//
//  @(#)JudgeImport.java	1.00	6/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
//
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/
//
package dip.judge.parser;

import dip.judge.parser.AdjustmentParser.OwnerInfo;
import dip.judge.parser.PositionParser.PositionInfo;
import dip.judge.parser.TurnParser.Turn;
import dip.misc.Log;
import dip.misc.Utils;
import dip.order.OrderException;
import dip.order.OrderFactory;
import dip.world.InvalidWorldException;
import dip.world.Location;
import dip.world.Phase;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.RuleOptions;
import dip.world.RuleOptions.Option;
import dip.world.RuleOptions.OptionValue;
import dip.world.TurnState;
import dip.world.Unit;
import dip.world.Unit.Type;
import dip.world.World;
import dip.world.World.VariantInfo;
import dip.world.WorldFactory;
import dip.world.WorldMap;
import dip.world.metadata.GameMetadata;
import dip.world.metadata.PlayerMetadata;
import dip.world.variant.VariantManager;
import dip.world.variant.data.Variant;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Imports text or reads a file, that is a Judge history file or listing.
 * A World object is created from that file, if successful.
 * <p>
 * Todo:<p>
 * <br>
 * <br>
 */
public final class JudgeImport {
    // resource constants
    private static final String JI_VARIANT_NOTFOUND = "JP.import.novariant";
    private static final String JI_NO_SUPPLY_INFO = "JP.import.nosupplyinfo";
    private static final String JI_NO_UNIT_INFO = "JP.import.nounitinfo";
    private static final String JI_UNKNOWN_POWER = "JP.import.powernotfound";
    private static final String JI_UNKNOWN_PROVINCE = "JP.import.provincenotfound";
    private static final String JI_BAD_POSITION = "JP.import.badposition";
    private static final String JI_UNKNOWN_TYPE = "JP.import.unknowntype";

    // import result constants
    public static final String JI_RESULT_NEWWORLD = "JP.import.newworld";
    public static final String JI_RESULT_TRYREWIND = "JP.import.tryrewind";
    public static final String JI_RESULT_LOADOTHER = "JP.import.loadother";
    public static final String JI_RESULT_THISWORLD = "JP.import.thisworld";

    // Instance variables
    private OrderFactory orderFactory;
    private JudgeParser jp;
    private World world;
    private World currentWorld;
    private String importResult = JI_RESULT_NEWWORLD;
    private String gameInfo; // e.g. "Game: test  Judge: USCA  Variant: Standard S1901M"

    /**
     * Creates a JudgeImport object from a File
     */
    public JudgeImport(final OrderFactory orderFactory, final File file,
                       final World currentWorld) throws IOException {
        this(orderFactory, new FileReader(file), currentWorld);
    }// JudgeImport()

    /**
     * Creates a JudgeImport object from a String
     */
    public JudgeImport(final OrderFactory orderFactory,
                       final String input) throws IOException {
        this(orderFactory, new StringReader(input),
                null); // TODO: submit a currentWorld
    }// JudgeImport()


    /**
     * Creates a JudgeImport object from a generic Reader
     */
    public JudgeImport(final OrderFactory orderFactory, final Reader reader,
                       final World currentWorld) throws IOException {
        this.orderFactory = orderFactory;
        this.currentWorld = currentWorld;
        jp = new JudgeParser(orderFactory, reader);
        procJudgeInput();
    }// JudgeImport()


    /**
     * Returns the World object after successful parsing, or null if unsuccessfull.
     */
    public World getWorld() {
        return world;
    }// getWorld()

    /**
     * Returns if the creator of this JudgeImport object needs to create a new world,
     * or if the current world is modified.
     */
    public String getResult() {
        return importResult;
    }

    public String getGameInfo() {
        return gameInfo;
    }

    /**
     * Processing that is common to both History and Listing files
     */
    private void procJudgeInput() throws IOException {
        // determine if we can load the variant
        final Variant variant = new VariantManager()
                .getVariant(jp.getVariantName(), VariantManager.VERSION_NEWEST)
                .orElse(null);
        if (variant == null) {
            throw new IOException(Utils.getLocalString(JI_VARIANT_NOTFOUND,
                    jp.getVariantName()));
        }

        // create the world
        try {
            world = WorldFactory.createWorld(variant);

            // essential! create the default rules
            world.setRuleOptions(RuleOptions.createFromVariant(variant));

        } catch (final InvalidWorldException e) {// FIXME: 16/03/04 catch RuntimeException
            throw new IOException(e);
        }

        // set the 'explicit convoy' rule option (all nJudge games require this)
        Log.println("JudgeImport: RuleOptions.VALUE_PATHS_EXPLICIT set");
        final RuleOptions ruleOpts = world.getRuleOptions();
        ruleOpts.setOption(Option.OPTION_CONVOYED_MOVES,
                OptionValue.VALUE_PATHS_EXPLICIT);
        world.setRuleOptions(ruleOpts);

        // eliminate all existing TurnStates; we will create our own from parsed values
        // we need the Position, though, since it has home-supply-center information
        Position position = null;
        for (final Phase phase : world.getPhaseSet()) {
            final TurnState ts = world.getTurnState(phase);
            if (position == null) {
                position = ts.getPosition();
            }
            world.removeTurnState(ts);
        }


        // set essential world data (variant name, map graphic to use)
        final VariantInfo variantInfo = world.getVariantInfo();
        variantInfo.setVariantName(variant.getName());
        variantInfo.setVariantVersion(variant.getVersion());
        variantInfo.setMapName(
                variant.getDefaultMapGraphic().orElse(null).getName());

        // set general metadata
        final GameMetadata gmd = world.getGameMetadata();
        gmd.setJudgeName(jp.getJudgeName());
        gmd.setGameName(jp.getGameName());

        // set player metadata (email address)
        final String[] pPowerNames = jp.getPlayerPowerNames();
        final String[] pPowerEmail = jp.getPlayerEmails();

        final WorldMap map = world.getMap();
        for (int i = 0; i < pPowerNames.length; i++) {
            final Power power = map.getPowerMatching(pPowerNames[i])
                    .orElse(null);
            if (power != null) {
                final PlayerMetadata pmd = world.getPlayerMetadata(power);
                pmd.setEmailAddresses(
                        Collections.singletonList(pPowerEmail[i]));
            } else if (pPowerNames[i].equalsIgnoreCase("master")) {
                gmd.setModeratorEmail(pPowerEmail[i]);
            }
        }

        // activate listing or history parsing
        if (Objects.equals(jp.getType(), JudgeParser.JP_TYPE_LISTING)) {
            procListing(position);
        } else if (Objects.equals(jp.getType(), JudgeParser.JP_TYPE_HISTORY)) {
            final JudgeImportHistory jih = new JudgeImportHistory(orderFactory,
                    world, jp, position);
            world = jih.getWorld();
        } else if (Objects.equals(jp.getType(), JudgeParser.JP_TYPE_RESULTS)) {
            procResults(jp, variant.getName());
        } else if (Objects
                .equals(jp.getType(), JudgeParser.JP_TYPE_GAMESTART)) {
            jp.prependText("Subject: " + jp.getJudgeName() + ":" + jp
                    .getGameName() + " - " +
                    jp.getPhase().getBriefName() + " Game Starting\n");
            final JudgeImportHistory jih = new JudgeImportHistory(orderFactory,
                    world, jp, position);
            world = jih.getWorld();
        } else {
            // unknown judge input
            throw new IOException(Utils.getLocalString(JI_UNKNOWN_TYPE));
        }
    }// procJudgeInput()

    /**
     * Process a Game Result
     */
    private void procResults(final JudgeParser jp,
                             final String variantName) throws IOException {
        // set game info
        gameInfo = "Judge: " + jp.getJudgeName() + "  Game: " + jp
                .getGameName() + "  Variant: " + variantName +
                ", " + jp.getPhase();
        // check, if currentWorld matches judge, game, variant and phase of these results
        if (currentWorld == null) {
            importResult = JI_RESULT_LOADOTHER;
            return;
        }

        final GameMetadata gmd = currentWorld.getGameMetadata();

        if (gmd.getJudgeName() == null || !gmd.getJudgeName()
                .equalsIgnoreCase(jp.getJudgeName()) ||
                gmd.getGameName() == null || !gmd.getGameName()
                .equalsIgnoreCase(jp.getGameName()) ||
                !currentWorld.getVariantInfo().getVariantName()
                        .equalsIgnoreCase(variantName)) {
            // wrong game
            importResult = JI_RESULT_LOADOTHER;
            return;
        }
        // right game, check phase
        if (currentWorld.getLastTurnState().getPhase()
                .compareTo(jp.getPhase()) != 0) {
            if (currentWorld.getLastTurnState().getPhase()
                    .compareTo(jp.getPhase()) > 0) {
                importResult = JI_RESULT_TRYREWIND;
            } else {
                importResult = JI_RESULT_LOADOTHER;
            }
            return;
        }

        final Turn turn = new Turn();
        turn.setPhase(jp.getPhase());
        turn.setText(jp.getText());
        final JudgeImportHistory jih = new JudgeImportHistory(orderFactory,
                currentWorld, jp, turn);
        importResult = JI_RESULT_THISWORLD;
    }

    /**
     * Process a Game Listing
     */
    private void procListing(final Position oldPosition) throws IOException {
        // Remember, for listings, we use jp.getText(), not jp.getInitialText()
        //
        //
        // parse position information
        final PositionParser pp = new PositionParser(jp.getText());
        final Phase phase = pp.getPhase();
        final PositionInfo[] posInfo = pp.getPositionInfo();

        // parse ownership / adjustment information
        final AdjustmentParser ap = new AdjustmentParser(world.getMap(),
                jp.getText());
        final OwnerInfo[] ownerInfo = ap.getOwnership();

        // ERROR if no positions, or no owner information.
        if (posInfo.length == 0) {
            throw new IOException(Utils.getLocalString(JI_NO_UNIT_INFO));
        }

        if (ownerInfo.length == 0) {
            throw new IOException(Utils.getLocalString(JI_NO_SUPPLY_INFO));
        }


        // Create a TurnState
        final TurnState ts = new TurnState(phase);
        ts.setWorld(world);

        // Create position information, and add to TurnState
        final Position position = new Position(world.getMap());
        ts.setPosition(position);

        // get world map information
        final WorldMap map = world.getMap();

        // reset home supply centers
        final List<Province> provinces = map.getProvinces();
        for (final Province province1 : provinces) {
            final Power power = oldPosition.getSupplyCenterHomePower(province1)
                    .orElse(null);
            if (power != null) {
                position.setSupplyCenterHomePower(province1, power);
            }
        }

        // set SC ownership information
        for (final OwnerInfo anOwnerInfo : ownerInfo) {
            final Power power = map.getPowerMatching(anOwnerInfo.getPowerName())
                    .orElse(null);
            if (power == null) {
                throw new IOException(Utils.getLocalString(JI_UNKNOWN_POWER,
                        anOwnerInfo.getPowerName()));
            }

            final String[] ownedProvNames = anOwnerInfo.getProvinces();
            for (final String ownedProvName : ownedProvNames) {
                final Province province = map.getProvinceMatching(ownedProvName)
                        .orElse(null);
                if (province == null) {
                    throw new IOException(
                            Utils.getLocalString(JI_UNKNOWN_PROVINCE,
                                    anOwnerInfo.getPowerName()));
                }

                position.setSupplyCenterOwner(province, power);
            }
        }

        // create units & positions on the map
        for (final PositionInfo aPosInfo : posInfo) {
            final Power power = map.getPowerMatching(aPosInfo.getPowerName())
                    .orElse(null);
            final Type unitType = Type.parse(aPosInfo.getUnitName());
            Location location = map.parseLocation(aPosInfo.getLocationName())
                    .orElse(null);

            // check
            if (power == null || location == null || unitType == Type.UNDEFINED) {
                throw new IOException(Utils.getLocalString(JI_BAD_POSITION,
                        aPosInfo.getPowerName(), aPosInfo.getUnitName(),
                        aPosInfo.getLocationName()));
            }

            // validate location
            try {
                location = location.getValidated(unitType);
            } catch (final OrderException e) {
                throw new IOException(e);
            }

            // create unit, and add to Position
            final Unit unit = new Unit(power, unitType);
            unit.setCoast(location.getCoast());
            position.setUnit(location.getProvince(), unit);
        }

        // although we parse adjustment info, we should not require it. We can just
        // detect supply-center differences.

        // add TurnState to World.
        world.setTurnState(ts);
    }// procListing()


}// class JudgeImport
