//
//	@(#)JudgeImport.java	1.00	6/2002
//
//	Copyright 2002 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
//
//
//	This program is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//
//	This program is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//
//	You should have received a copy of the GNU General Public License
//	along with this program; if not, write to the Free Software
//	Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//	Or from http://www.gnu.org/
//
package dip.judge.parser;

import dip.judge.parser.AdjustmentParser.OwnerInfo;
import dip.judge.parser.DislodgedParser.DislodgedInfo;
import dip.judge.parser.TurnParser.Turn;
import dip.misc.Utils;
import dip.order.Build;
import dip.order.Disband;
import dip.order.Move;
import dip.order.NJudgeOrderParser.NJudgeOrder;
import dip.order.Order;
import dip.order.OrderException;
import dip.order.OrderFactory;
import dip.order.Orderable;
import dip.order.Remove;
import dip.order.ValidationOptions;
import dip.order.result.DislodgedResult;
import dip.order.result.OrderResult;
import dip.order.result.OrderResult.ResultType;
import dip.order.result.Result;
import dip.order.result.SubstitutedResult;
import dip.process.Adjustment;
import dip.process.Adjustment.AdjustmentInfoMap;
import dip.world.Location;
import dip.world.Phase;
import dip.world.Phase.PhaseType;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.RuleOptions;
import dip.world.RuleOptions.Option;
import dip.world.RuleOptions.OptionValue;
import dip.world.TurnState;
import dip.world.Unit;
import dip.world.Unit.Type;
import dip.world.VictoryConditions;
import dip.world.World;
import dip.world.WorldMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Processes an entire game history to create a world.
 * <p>
 * TODO:
 * <br>positioning units with orders that failed parsing (e.g., a move to Switzerland (swi))
 */
public final class JudgeImportHistory {
    private static final Logger LOG = LoggerFactory
            .getLogger(JudgeImportHistory.class);

    // constants
    private static final String STDADJ_MV_UNIT_DESTROYED = "STDADJ_MV_UNIT_DESTROYED";

    private static final String JIH_BAD_POSITION = "JP.import.badposition";
    private static final String JIH_NO_MOVEMENT_PHASE = "JP.history.nomovement";
    private static final String JIH_ORDER_PARSE_FAILURE = "JP.history.badorder";
    private static final String JIH_UNKNOWN_RESULT = "JP.history.unknownresult";
    private static final String JIH_NO_DISLODGED_MATCH = "JP.history.dislodgedmatchfail";
    private static final String JIH_INVALID_RETREAT = "JP.history.badretreat";
    private static final String JIH_BAD_LAST_PHASE = "JP.history.badlastphase";

    // parsing parameters
    /**
     * Regular expression for parsing what the Next phase is. This is only used to create the last
     * (final) phase.<p>
     * Capture groups: (1)Phase (2)Season (3)Year
     */
    public static final String PARSE_REGEX = "(?i)the\\snext\\sphase\\s.*will\\sbe\\s(\\p{Alpha}+)\\sfor\\s(\\p{Alpha}+)\\sof\\s((\\p{Digit}+))";
    public static final String END_FOF_GAME = "(?i)the game is over";
    public static final String START_POSITIONS = "(?i)Subject:\\s\\p{Alpha}+:\\p{Alnum}+\\s-\\s(\\p{Alpha})(\\p{Digit}+)(\\p{Alpha})";


    // instance variables
    private final WorldMap map;
    private final OrderFactory orderFactory;
    private final World world;
    private final JudgeParser jp;
    private final Position oldPosition;
    private final ValidationOptions valOpts;
    private List<HSCInfo> homeSCInfo;
    private boolean finalTurn;

    /**
     * Create a JudgeImportHistory
     *
     * @throws PatternSyntaxException
     */
    public JudgeImportHistory(final OrderFactory orderFactory,
                              final World world, final JudgeParser jp,
                              final Position oldPosition) throws IOException {
        this.orderFactory = orderFactory;
        this.world = world;
        this.jp = jp;
        this.oldPosition = oldPosition;
        map = world.getMap();

        // create a very strict validation object, loose seems to have some weird problems when importing.
        valOpts = new ValidationOptions();
        valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING,
                ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);

        processTurns();
    }// JudgeImportHistory()

    /**
     * Create a JudgeImportHistory and process a single turn
     *
     * @throws PatternSyntaxException
     */
    public JudgeImportHistory(final OrderFactory orderFactory,
                              final World world, final JudgeParser jp,
                              final Turn turn) throws IOException {
        this.orderFactory = orderFactory;
        this.world = world;
        this.jp = jp;
        oldPosition = world.getLastTurnState().getPosition();
        map = world.getMap();

        // create a very strict validation object, loose seems to have some weird problems when importing.
        valOpts = new ValidationOptions();
        valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING,
                ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);

        processSingleTurn(turn);
    }// JudgeImportHistory()

    /**
     * Returns the World, with TurnStates & Positions added as appropriate.
     */
    protected World getWorld() {
        return world;
    }// getWorld()


    /**
     * Processes the Turn data, starting with the first Movement phase. An exception is
     * thrown if no Movement phase exists.
     *
     * @throws PatternSyntaxException
     */
    private void processTurns() throws IOException {
        // break data up into turns
        final List<Turn> turns = new TurnParser(jp.getText()).getTurns();
        //System.out.println("# of turns: "+turns.length);

        // find first movement phase, if any
        int firstMovePhase = -1;
        for (int i = 0; i < turns.size(); i++) {
            if (turns.get(i).getPhase() != null) {
                if (turns.get(i).getPhase()
                        .getPhaseType() == PhaseType.MOVEMENT) {
                    firstMovePhase = i;
                    break;
                }
            }
        }

        // If we couldn't find the first movement phase... perhaps the game is just starting
        if (firstMovePhase == -1) {
            // Try to use the text info to create the game at its starting positions
            try {
                createStartingPositions();
                // Don't do the rest of this method, it will all fail.
                return;
            } catch (final IOException e) {
                throw new IOException(
                        Utils.getLocalString(JIH_NO_MOVEMENT_PHASE));
            }
        }

        //System.out.println("First move phase: "+firstMovePhase);

        // get home supply center information from the oldPosition object
        // and store it in HSCInfo object array, so that it can be set during each successive
        // turn.
        final List<HSCInfo> hscList = new ArrayList<>(50);
        for (final Province province : map.getProvinces()) {
            final Power power = oldPosition.getSupplyCenterHomePower(province)
                    .orElse(null);
            if (power != null) {
                hscList.add(new HSCInfo(province, power));
            }
        }
        homeSCInfo = hscList;

        // process all but the final phase
        for (int i = firstMovePhase; i < turns.size() - 1; i++) {
            //System.out.println("processing turn: "+i+"; phase = "+turns[i].getPhase());
            if (i == 0) {
                procTurn(turns.get(i), null, null, false);
            } else if (i == 1) {
                procTurn(turns.get(i), turns.get(i - 1), null, false);
            } else {
                procTurn(turns.get(i), turns.get(i - 1), turns.get(i - 2),
                        false);
            }
        }
        // process the last turn once more, but as the final turn, to allow proper positioning.
        finalTurn = true;
        if (turns.size() == 1) {
            procTurn(turns.get(turns.size() - 1), null, null, true);
        } else if (turns.size() == 2) {
            procTurn(turns.get(turns.size() - 1), turns.get(turns.size() - 2),
                    null, true);
        } else if (turns.size() >= 3) {
            procTurn(turns.get(turns.size() - 1), turns.get(turns.size() - 2),
                    turns.get(turns.size() - 3), true);
        }

        final Pattern endofgame = Pattern.compile(END_FOF_GAME);

        final Matcher e = endofgame
                .matcher(turns.get(turns.size() - 1).getText());

        if (e.find()) {
            // The imported game has ended
            // Reprocess the last turn, again, not as final, so it looks right for viewing.
            finalTurn = false;
            procTurn(turns.get(turns.size() - 1), turns.get(turns.size() - 2),
                    turns.get(turns.size() - 3), false);
            // Set the game as ended.
            final TurnState ts = world
                    .getTurnState(turns.get(turns.size() - 1).getPhase());
            final VictoryConditions vc = world.getVictoryConditions();
            final RuleOptions ruleOpts = world.getRuleOptions();
            final AdjustmentInfoMap adjMap = Adjustment
                    .getAdjustmentInfo(ts, ruleOpts,
                            world.getMap().getPowers());
            vc.evaluate(ts, adjMap);
            final List<Result> evalResults = ts.getResultList();
            evalResults.addAll(vc.getEvaluationResults());
            ts.setResultList(evalResults);
            ts.setEnded(true);
            ts.setResolved(true);
            world.setTurnState(ts);
        } else {

            // create last (un-resolved) turnstate
            makeLastTurnState(turns.get(turns.size() - 1));

            // reprocess the last turn, again, not as final, so it looks right for viewing.
            finalTurn = false;
            if (turns.size() == 1) {
                procTurn(turns.get(turns.size() - 1), null, null, false);
            } else if (turns.size() == 2) {
                procTurn(turns.get(turns.size() - 1),
                        turns.get(turns.size() - 2), null, false);
            } else if (turns.size() >= 3) {
                procTurn(turns.get(turns.size() - 1),
                        turns.get(turns.size() - 2),
                        turns.get(turns.size() - 3), false);
            }
        }

        // all phases have been processed; perform post-processing here.
    }// processTurns()

    /**
     * Processes a single turn.
     *
     * @throws PatternSyntaxException
     */
    private void processSingleTurn(final Turn turn) throws IOException {
        // get home supply center information from the oldPosition object
        // and store it in HSCInfo object array, so that it can be set during each successive
        // turn.
        final ArrayList<HSCInfo> hscList = new ArrayList<>(50);
        for (final Province province : map.getProvinces()) {
            final Power power = oldPosition.getSupplyCenterHomePower(province)
                    .orElse(null);
            if (power != null) {
                hscList.add(new HSCInfo(province, power));
            }
        }
        homeSCInfo = hscList;

        // process the turn
        procTurn(turn, null, null, false);
        // save new turn state
        final TurnState savedTS = world.getLastTurnState();

        // process the last turn once more, but as the final turn, to allow proper positioning.
        finalTurn = true;
        procTurn(turn, null, null, true);
        // create last (un-resolved) turnstate
        makeLastTurnState(turn);

        // inject the saved turnstate.
        world.setTurnState(savedTS);

        // TODO: do we have to check for victory conditions ?

    }// processSingleTurn()


    /**
     * Decides how to process the turn, based upon the phase information and past turns.
     * This is not the best way to process the turns, especially the adjustment phase,
     * but it works.
     */
    private void procTurn(final Turn turn, final Turn prevTurn,
                          final Turn thirdTurn,
                          final boolean positionPlacement) throws IOException {
        LOG.debug("JIH:procTurn():METHOD ENTRY");
        final Phase phase = turn.getPhase();
        if (phase != null) {
            final PhaseType phaseType = phase.getPhaseType();
            if (phaseType == PhaseType.MOVEMENT) {
                LOG.debug("JIH:procTurn():MOVEMENT START");
                procMove(turn, positionPlacement);
                LOG.debug("JIH:procTurn():MOVEMENT END");
            } else if (phaseType == PhaseType.RETREAT) {
                LOG.debug("JIH:procTurn():RETREAT START");
                    /*
                     * Set the proper positionPlacement value depending on if the turn being
					 * processed is the final turn. Set it back again when done. 
					 */
                if (finalTurn) {
                    procMove(prevTurn, positionPlacement);
                } else {
                    procMove(prevTurn, !positionPlacement);
                }

                procRetreat(turn, positionPlacement);

                if (finalTurn) {
                    procMove(prevTurn, !positionPlacement);
                } else {
                    procMove(prevTurn, positionPlacement);
                }
                LOG.debug("JIH:procTurn():RETREAT END");
            } else if (phaseType == PhaseType.ADJUSTMENT) {
                LOG.debug("JIH:procTurn():ADJUSTMENT START");
                PhaseType prevPhaseType = PhaseType.MOVEMENT; // dummy
                if (prevTurn != null) {
                    final Phase phase_p = prevTurn.getPhase();
                    prevPhaseType = phase_p.getPhaseType();
                }
                /*
                 * Much the same as above, set the proper positionPlacement value depending
				 * on the PhaseType and if the turn being processed is the final turn.
				 * Set it back again when done. 
				 */
                if (prevPhaseType == PhaseType.MOVEMENT) {
                    if (finalTurn) {
                        procMove(prevTurn, positionPlacement);
                    } else {
                        procMove(prevTurn, !positionPlacement);
                    }

                    procAdjust(turn, positionPlacement);

                    if (finalTurn) {
                        procMove(prevTurn, !positionPlacement);
                    } else {
                        procMove(prevTurn, positionPlacement);
                    }

                } else {

                    if (finalTurn) {
                        procMove(thirdTurn, positionPlacement);
                        procRetreat(prevTurn, positionPlacement);
                    } else {
                        procMove(thirdTurn, !positionPlacement);
                        procRetreat(prevTurn, !positionPlacement);
                    }

                    procAdjust(turn, positionPlacement);

                    if (finalTurn) {
                        procRetreat(prevTurn, !positionPlacement);
                        procMove(thirdTurn, !positionPlacement);
                    } else {
                        procRetreat(prevTurn, positionPlacement);
                        procMove(thirdTurn, positionPlacement);
                    }
                }
                LOG.debug("JIH:procTurn():ADJUSTMENT END");
            } else {
                throw new IllegalStateException("unknown phase type");
            }
        }
        LOG.debug("JIH:procTurn():METHOD EXIT");
    }// procTurn()


    /**
     * Creates a TurnState object with the correct Phase, Position, and World information,
     * including setting things such as home supply centers and what not.
     * <p>
     * This method ensures that TurnState objects are properly (and consistently) initialized.
     */
    private TurnState makeTurnState(final Turn turn,
                                    final boolean positionPlacement) {
        // does the turnstate already exist?
        // it could, if we are importing orders into an already-existing game.
        //
        LOG.debug("JIH::makeTurnState() {}", turn.getPhase());

        // TODO: we can import judge games, but we cannot import judge games
        // into existing games successfully.
        //
        final TurnState ts = new TurnState(turn.getPhase());
        ts.setWorld(world);
        ts.setPosition(new Position(world.getMap()));

        // note: we don't add the turnstate to the World object at this point (although we could), because
        // if a processing error occurs, we don't want a partial turnstate object in the World.

        // set Home Supply centers in position
        final Position pos = ts.getPosition();
        for (final HSCInfo aHomeSCInfo : homeSCInfo) {
            pos.setSupplyCenterHomePower(aHomeSCInfo.getProvince(),
                    aHomeSCInfo.getPower());
        }

        return ts;
    }// makeTurnState()


    /**
     * Old method
     */
    private void procMove(final Turn turn,
                          final boolean positionPlacement) throws IOException {
        procMove(turn, positionPlacement, false);
    }// procMove()


    /**
     * Process a Movement phase turn
     */
    private void procMove(final Turn turn, final boolean positionPlacement,
                          final boolean isRetreatMoveProcessing) throws IOException {
        LOG.debug("JIH::procMove():METHOD ENTRY");
        LOG.debug("  positionPlacement = {}", positionPlacement);
        LOG.debug("  isRetreatMoveProcessing = {}", isRetreatMoveProcessing);
        if (turn == null) {
            return;
        }

        // create TurnState
        LOG.debug("  Turn.getPhase() = {}", turn.getPhase());

        final TurnState ts = makeTurnState(turn, positionPlacement);
        final List<Result> results = ts.getResultList();

        LOG.debug("  Turnstate created. Phase: {}", ts.getPhase());

        // copy previous lastOccupier information into current turnstate.
        copyPreviousLastOccupierInfo(ts);

        // parse orders, and create orders for each unit
        final JudgeOrderParser jop = new JudgeOrderParser(map, orderFactory,
                turn.getText());
        final List<NJudgeOrder> nJudgeOrders = jop.getNJudgeOrders();

        // get Position. Remember, this position contains no units.
        final Position position = ts.getPosition();

        LOG.debug("  :Creating start positions; # NJudgeOrders = {}",
                nJudgeOrders.size());
        LOG.debug("  :getResultList().size() = {}", results.size());

        // create units from start position
        for (final NJudgeOrder njo : nJudgeOrders) {
            final Orderable order = njo.getOrder();
            if (order == null) {
                LOG.debug("JIH::procMove(): Null order; njo: {}", njo);
                throw new IOException(
                        "Internal error: null order in JudgeImportHistory::procMove()");
            }

            Location loc = order.getSource();
            final Type unitType = order.getSourceUnitType();
            final Power power = order.getPower();

            // validate location
            try {
                loc = loc.getValidated(unitType);
            } catch (final OrderException e) {
                LOG.debug("ERROR: {}", njo);
                LOG.debug("TURN: \n{}", turn.getText());
                throw new IOException(e);
            }

            // create unit, and add to Position
            // we may have to add the unit in a dislodged position.
            // We must first check for a 'dislodged' indicator.
            boolean isUnitDislodged = false;
            if (isRetreatMoveProcessing) {
                for (final Object o : njo.getResults()) {
                    final Result r = (Result) o;
                    if (r instanceof OrderResult) {
                        if (((OrderResult) r)
                                .getResultType() == ResultType.DISLODGED) {
                            isUnitDislodged = true;
                            break;
                        }
                    }
                }
            }

            final Unit unit = new Unit(order.getPower(), unitType);
            unit.setCoast(loc.getCoast());
            position.setLastOccupier(loc.getProvince(), power);

            if (isUnitDislodged) {
                position.setDislodgedUnit(loc.getProvince(), unit);
                LOG.debug("  :created dislodged unit: {} at {}", unit, loc);
            } else {
                position.setUnit(loc.getProvince(), unit);
                LOG.debug("  :created unit: {} at {}", unit, loc);
            }

            // if we found a Wing unit, make sure Wing units are enabled.
            checkAndEnableWings(unitType);
        }


        // now, validate all order objects from the parsed order
        // also create result objects
        // create positions from successful orders...
        // note that we only need to set the last occupier for changing (moving)
        // units, but we will do it for all units for consistency
        //
        // create orderMap, which maps powers to their respective order list
        final List<Power> powers = map.getPowers();

        LOG.debug("  :created power->order mapping");

        final Map<Power, LinkedList<Order>> orderMap = new HashMap<>(
                powers.size());
        for (final Power power1 : powers) {
            orderMap.put(power1, new LinkedList<>());
        }

        // process all orders
        final RuleOptions ruleOpts = world.getRuleOptions();

        for (final NJudgeOrder njo : nJudgeOrders) {
            final Order order = njo.getOrder();

            // first try to validate under strict settings; if fail, try
            // to validate under loose settings.
            try {
                order.validate(ts, valOpts, ruleOpts);

                final LinkedList<Order> list = orderMap.get(order.getPower());
                list.add(order);

                results.addAll(njo.getResults());

                LOG.debug("  order ok: {}", order);
            } catch (final OrderException e) {
                LOG.debug(
                        "  *** JIH::procMove():OrderException while processing order: {}",
                        order);
                LOG.debug("      error: {}", e);
                LOG.debug("      Retesting with loose validation");

                //Try loosening the validation object
                valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING,
                        ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE);

                /* Try the order once more.
                 * nJudge accepts illegal moves as valid as long as the syntax is valid.
                 * (Perhaps a few other things as well). jDip should accept these as well,
                 * even if the move is illegal.
                 */
                try {
                    order.validate(ts, valOpts, ruleOpts);

                    final List<Order> list = orderMap.get(order.getPower());
                    list.add(order);

                    results.addAll(njo.getResults());

                    LOG.debug("  order ok: {}", order);
                } catch (final OrderException e1) {
                    // create a general result indicating failure if an order could not be validated.
                    results.add(new Result(
                            Utils.getLocalString(JIH_ORDER_PARSE_FAILURE, order,
                                    e.getMessage())));
                    LOG.debug(
                            "JIH::procMove():OrderException (during validation): {}; {}",
                            order, e1.getMessage());
                    throw new IOException(
                            "Cannot validate order on second pass.\n" + e1
                                    .getMessage());
                }

                // Back to strict!
                valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING,
                        ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);
            }
        }

        LOG.debug("JIH::procMove():ORDER PARSING COMPLETE");

        // clear all units (dislodged or not) from the board
        for (final Province anUnitProv : position.getUnitProvinces()) {
            position.setUnit(anUnitProv, null);
        }
        for (final Province aDislProv : position.getDislodgedUnitProvinces()) {
            position.setUnit(aDislProv, null);
        }

        // now that all orders are parsed, and all units are cleared, put
        // unit in the proper place.
        for (final Object result1 : results) {
            final Result result = (Result) result1;
            if (result instanceof OrderResult) {
                final OrderResult ordResult = (OrderResult) result;
                final Orderable order = ordResult.getOrder();

                if (ordResult.getResultType() == ResultType.DISLODGED) {
                    // dislodged orders create a unit in the source province, marked as dislodged,
                    // unless it was destroyed; if so, it will be destroyed later. Mark as dislodged for now.
                    final Unit unit = new Unit(order.getPower(),
                            order.getSourceUnitType());
                    /*
                     * Check for the positionPlacement flag, if not, we need to position the units
                     * in their source places for VIEWING. Otherwise the units need to be
                     * in their destination place for copying.
                     */
                    if (positionPlacement) {
                        unit.setCoast(order.getSource().getCoast());
                        position.setDislodgedUnit(
                                order.getSource().getProvince(), unit);
                        LOG.debug("     unit dislodged: {}",
                                order.getSource().getProvince());
                    } else {
                        unit.setCoast(order.getSource().getCoast());
                        position.setUnit(order.getSource().getProvince(), unit);
                    }
                } else if (ordResult
                        .getResultType() == ResultType.SUCCESS && order instanceof Move) {
                    // successful moves create a unit in the destination province
                    final Move move = (Move) order;
                    final Unit unit = new Unit(move.getPower(),
                            move.getSourceUnitType());
                    /*
                     * Check for the positionPlacement flag, if not, we need to position the units
                     * in their source places for VIEWING. Otherwise the units need to be
                     * in their destination place for copying.
                     */
                    if (positionPlacement) {
                        unit.setCoast(move.getDest().getCoast());
                        position.setUnit(move.getDest().getProvince(), unit);
                        position.setLastOccupier(move.getDest().getProvince(),
                                move.getPower());
                    } else {
                        unit.setCoast(move.getSource().getCoast());
                        position.setUnit(move.getSource().getProvince(), unit);
                        position.setLastOccupier(move.getSource().getProvince(),
                                move.getPower());
                    }
                } else {
                    // all other orders create a non-dislodged unit in the source province
                    final Unit unit = new Unit(order.getPower(),
                            order.getSourceUnitType());
                    /*
                     * Only add a unit if there is not a unit currently there, this stops
                     * powers further down in alpha. order from overriding powers before
                     * them. Eg. England dislodged Germany will be overriding if this isn't here.
                     */
                    if (!position.hasUnit(order.getSource().getProvince())) {
                        unit.setCoast(order.getSource().getCoast());
                        position.setUnit(order.getSource().getProvince(), unit);
                        position.setLastOccupier(
                                order.getSource().getProvince(),
                                order.getPower());
                    }
                }
            }
        }

        // set orders in turnstate
        for (final Power power : powers) {
            ts.setOrders(power, orderMap.get(power));
        }

        // process dislodged unit info, to determine retreat paths
        // correct dislodged results are created here, and the old dislodged
        // results are removed
        final DislodgedParser dislodgedParser = new DislodgedParser(
                ts.getPhase(), turn.getText());
        makeDislodgedResults(ts.getPhase(), results, position,
                dislodgedParser.getDislodgedInfo(), positionPlacement);

        // process adjustment info ownership info (if any)
        //
        final AdjustmentParser adjParser = new AdjustmentParser(map,
                turn.getText());
        procAdjustmentBlock(adjParser.getOwnership(), ts, position);

        // check for elimination
        position.setEliminationStatus(map.getPowers());

        // set adjudicated flag
        ts.setResolved(true);

        // add to world
        world.setTurnState(ts);
        LOG.debug("JIH::procMove(): METHOD EXIT");
    }// procMove()


    /**
     * Process a Retreat phase turn
     */
    private void procRetreat(final Turn turn,
                             final boolean positionPlacement) throws IOException {
        LOG.debug("JIH::procRetreat(): METHOD START");
        if (turn == null) return;
        // create TurnState
        final TurnState ts = makeTurnState(turn, positionPlacement);
        final Position position = ts.getPosition();
        final List<Result> results = ts.getResultList();
        final RuleOptions ruleOpts = world.getRuleOptions();

        LOG.debug("  :procRetreat(): {}; positionPlacement: {}", ts.getPhase(),
                positionPlacement);

        // parse orders, and create orders for each unit
        final JudgeOrderParser jop = new JudgeOrderParser(map, orderFactory,
                turn.getText());
        final List<NJudgeOrder> nJudgeOrders = jop.getNJudgeOrders();

        // Copy previous phase positions
        copyPreviousPositions(ts);

        // process retreat orders (either moves or disbands)
        // if order failed, it counts as a disband
        // generate results
        // create units for all successfull move (retreat) orders in destination province
        // create orderMap, which maps powers to their respective order list
        final List<Power> powers = map.getPowers();
        final Map<Power, LinkedList<Order>> orderMap = new HashMap<>(
                powers.size());
        for (final Power power1 : powers) {
            orderMap.put(power1, new LinkedList<>());
        }

        // validate all parsed orders
        for (final NJudgeOrder njo : nJudgeOrders) {
            final Order order = njo.getOrder();
            if (order == null) {
                LOG.debug("JIH::procRetreat(): Null order; njo: {}", njo);
                throw new IOException(
                        "Internal error: null order in JudgeImportHistory::procRetreat()");
            }

            // if we found a Wing unit, make sure Wing units are enabled.
            checkAndEnableWings(order.getSourceUnitType());

            try {
                order.validate(ts, valOpts, ruleOpts);

                final Deque<Order> list = orderMap.get(order.getPower());
                list.add(order);

                results.addAll(njo.getResults());

                LOG.debug("  order ok: {}", order);
            } catch (final OrderException e) {
                results.add(new Result(
                        Utils.getLocalString(JIH_ORDER_PARSE_FAILURE, order,
                                e.getMessage())));
                LOG.debug(
                        "JIH::procMove():OrderException (during validation): {}; {}",
                        order, e.getMessage());
                throw new IOException(
                        "Cannot validate retreat order.\n" + e.getMessage(), e);
            }
        }

        // clear all dislodged units from board
        if (positionPlacement) {
            for (final Province aDislProv : position
                    .getDislodgedUnitProvinces()) {
                position.setDislodgedUnit(aDislProv, null);
            }
        }

        // now that all orders are parsed, and all units are cleared, put
        // unit in the proper place.
        //
        for (final Object result1 : results) {
            final Result result = (Result) result1;
            if (result instanceof OrderResult) {
                final OrderResult ordResult = (OrderResult) result;
                final Orderable order = ordResult.getOrder();

                // successful moves create a unit in the destination province
                // unsuccessful moves OR disbands create no unit
                if (order instanceof Move) {
                    // && ordResult.getResultType() == OrderResult.ResultType.SUCCESS)
                    // success: unit retreat to destination
                    final Move move = (Move) order;

                    final Unit unit = new Unit(move.getPower(),
                            move.getSourceUnitType());
                    /*
                     * Check for the positionPlacement flag, if not, we need to position the units
                     * in their source places for VIEWING. Otherwise the units need to be
                     * in their destination place for copying.
                     */
                    if (positionPlacement) {
                        unit.setCoast(move.getDest().getCoast());
                        position.setUnit(move.getDest().getProvince(), unit);
                        position.setLastOccupier(move.getSource().getProvince(),
                                move.getPower());
                    } else {
                        unit.setCoast(move.getSource().getCoast());
                        position.setDislodgedUnit(
                                move.getSource().getProvince(), unit);
                        position.setLastOccupier(move.getSource().getProvince(),
                                move.getPower());
                        LOG.debug("     unit dislodged: {}",
                                move.getSource().getProvince());
                    }
                } else if (order instanceof Disband) {
                    /*
                     * Check for the positionPlacement flag, if not, we need to position the units
                     * in their source places for VIEWING. Otherwise the units should not be drawn.
                     */
                    final Unit unit = new Unit(order.getPower(),
                            order.getSourceUnitType());
                    if (!positionPlacement) {
                        unit.setCoast(order.getSource().getCoast());
                        position.setDislodgedUnit(
                                order.getSource().getProvince(), unit);
                        LOG.debug("     unit dislodged: {}",
                                order.getSource().getProvince());
                    }
                }
            }
        }

        // set orders in turnstate
        for (final Power power : powers) {
            ts.setOrders(power, orderMap.get(power));
        }

        // process adjustment info ownership info (if any)
        final AdjustmentParser adjParser = new AdjustmentParser(map,
                turn.getText());
        procAdjustmentBlock(adjParser.getOwnership(), ts, position);

        // check for elimination
        ts.getPosition().setEliminationStatus(map.getPowers());

        // set adjudicated flag
        ts.setResolved(true);

        // add to world
        world.setTurnState(ts);
        LOG.debug("JIH::procRetreat(): METHOD EXIT");
    }// procRetreat()


    /**
     * Process an Adjustment phase turn
     */
    private void procAdjust(final Turn turn,
                            final boolean positionPlacement) throws IOException {
        if (turn == null) {
            return;
        }

        // create TurnState
        final TurnState ts = makeTurnState(turn, positionPlacement);
        final List<Result> results = ts.getResultList();
        final RuleOptions ruleOpts = world.getRuleOptions();

        LOG.debug("JIH::procAdjust(): {}", ts.getPhase());

        // parse orders, and create orders for each unit
        final JudgeOrderParser jop = new JudgeOrderParser(map, orderFactory,
                turn.getText());
        final List<NJudgeOrder> nJudgeOrders = jop.getNJudgeOrders();

        // Copy previous phase positions
        copyPreviousPositions(ts);

        // Copy previous SC info (we need proper ownership info before parsing orders)
        copyPreviousSCInfo(ts);

        // DEBUG: use Adjustment to check out WTF is going on
        /*
        System.out.println("dip.process.Adjustment.getAdjustmentInfo()");
		for(int i=0; i<map.getPowers().length; i++)
		{
			Power power = map.getPowers()[i];
			System.out.println("   for power: "+power+"; "+dip.process.Adjustment.getAdjustmentInfo(ts, power));
		}
		*/

        // process adjustment orders (either builds or removes)
        // create a unit, unless order failed
        {
            // get Position
            final Position position = ts.getPosition();

            // create orderMap, which maps powers to their respective order list
            final List<Power> powers = map.getPowers();
            final Map<Power, LinkedList<Order>> orderMap = new HashMap<>();
            for (final Power power1 : powers) {
                orderMap.put(power1, new LinkedList<>());
            }

            // parse all orders
            for (final NJudgeOrder njo : nJudgeOrders) {
                final Order order = njo.getOrder();

                // all adjustment orders produced by NJudgeOrderParser should
                // have only 1 result
                //
                if (njo.getResults().size() != 1) {
                    throw new IOException(
                            "Internal error: JIH:procAdjustments(): " + "getResults() != 1");
                }

                final Result result = njo.getResults().get(0);

                // if result is a substituted result, the player defaulted,
                // and the Judge inserted a Disband order
                //
                final boolean isDefaulted = result instanceof SubstitutedResult;

                if (order == null && !isDefaulted) {
                    // orders may be null; if they are, that is because
                    // it's a waive or unusable/pending order. These have
                    // results, but no associated order.
                    results.addAll(njo.getResults());
                } else {
                    // NOTE: everything in this block should use newOrder,
                    // not order, from here on!!
                    Order newOrder = order;

                    if (isDefaulted) {
                        newOrder = ((SubstitutedResult) result)
                                .getSubstitutedOrder();
                        assert newOrder != null;
                    }

                    // if we found a Wing unit, make sure Wing units are enabled.
                    checkAndEnableWings(newOrder.getSourceUnitType());

                    try {
                        newOrder.validate(ts, valOpts, ruleOpts);

                        if (!isDefaulted) {
                            final LinkedList<Order> list = orderMap
                                    .get(newOrder.getPower());
                            list.add(newOrder);
                        }

                        results.addAll(njo.getResults());

                        LOG.debug("  order ok: {}", newOrder);

                        // create or remove units
                        // as far as I know, orders are always successful.
                        //
                        if (newOrder instanceof Build) {
                            if (positionPlacement) {
                                final Unit unit = new Unit(newOrder.getPower(),
                                        newOrder.getSourceUnitType());
                                unit.setCoast(newOrder.getSource().getCoast());
                                position.setUnit(
                                        newOrder.getSource().getProvince(),
                                        unit);
                                position.setLastOccupier(
                                        newOrder.getSource().getProvince(),
                                        newOrder.getPower());
                            }
                        } else if (newOrder instanceof Remove) {
                            if (positionPlacement) {
                                position.setUnit(
                                        newOrder.getSource().getProvince(),
                                        null);
                            }
                        } else {
                            throw new IllegalStateException(
                                    "JIH::procAdjust(): type :" + newOrder + " not handled!");
                        }
                    } catch (final OrderException e) {
                        results.add(new Result(
                                Utils.getLocalString(JIH_ORDER_PARSE_FAILURE,
                                        newOrder, e.getMessage())));

                        LOG.debug(
                                "JIH::procAdjust():OrderException (during validation): ");
                        LOG.debug("     phase: {}", ts.getPhase());
                        LOG.debug("     order: {}", newOrder);
                        LOG.debug("     error: {}", e.getMessage());

                        throw new IOException(
                                "Cannot validate adjustment order.\n" + e
                                        .getMessage());
                    }
                }
            }

            // set orders in turnstate
            for (final Power power : powers) {
                ts.setOrders(power, orderMap.get(power));
            }
        }


        // check for elimination
        ts.getPosition().setEliminationStatus(map.getPowers());

        // set adjudicated flag
        ts.setResolved(true);

        // Since this is the adjustment phase, check for supply center change. Required for VictoryConditions
        // Otherwise, problems can arise and the game will end after importing due to no SC change.
        if (!positionPlacement) {
            TurnState previousTS = world.getPreviousTurnState(ts).get();
            while (previousTS.getPhase().getPhaseType() != PhaseType.MOVEMENT) {
                previousTS = world.getPreviousTurnState(previousTS).get();
            }
            //System.out.println(previousTS.getPhase());
            final Position oldPosition = previousTS.getPosition();
            final Position position = ts.getPosition();
            for (final Province province : position.getProvinces()) {
                if (province != null && province.hasSupplyCenter()) {
                    final Unit unit = position.getUnit(province).orElse(null);
                    if (unit != null) {
                        // nextPosition still contains old ownership information
                        final Power oldOwner = oldPosition
                                .getSupplyCenterOwner(province).orElse(null);
                        final Power newOwner = unit.getPower();
                        //System.out.println(oldOwner + " VS " + newOwner);

                        // change if ownership change, and not a wing unit
                        if (oldOwner != newOwner && unit
                                .getType() != Type.WING) {
                            // set owner-changed flag in TurnState [req'd for certain victory conditions]
                            ts.setSCOwnerChanged(true);
                        }
                    }
                }
            }
        }

        // add to world
        world.setTurnState(ts);
    }// procAdjust()


    /**
     * Clones all non-dislodged units from previous phase TurnState
     * and inserts them into the current turnstate.
     * <p>
     * We also copy non-dislodged units, unless the CURRENT turnstate is
     * an Adjustment phase
     */
    private void copyPreviousPositions(final TurnState current) {
        // get previous turnstate
        final TurnState previousTS = current.getWorld()
                .getPreviousTurnState(current).get();
        final boolean isCopyDislodged = current.getPhase()
                .getPhaseType() != PhaseType.ADJUSTMENT;

        // get position info
        final Position newPos = current.getPosition();
        final Position oldPos = previousTS == null ? oldPosition : previousTS
                .getPosition();

        LOG.debug("copyPreviousPositions() from: {}", oldPos);

        // clone!
        for (final Province p : map.getProvinces()) {
            final Unit unit = oldPos.getUnit(p).orElse(null);
            if (unit != null) {
                final Unit newUnit = unit.clone();
                newPos.setUnit(p, newUnit);
                LOG.debug("  cloned unit from/into: {} - {}", p,
                        unit.getPower());
            }

            final Unit unit0 = oldPos.getDislodgedUnit(p).orElse(null);
            if (isCopyDislodged && unit0 != null) {
                final Unit newUnit = unit0.clone();
                newPos.setDislodgedUnit(p, newUnit);
                LOG.debug("  cloned dislodged unit from/into: {} - {}", p,
                        unit0.getPower());
            }

            // clone any lastOccupied info as well.
            newPos.setLastOccupier(p, oldPos.getLastOccupier(p).orElse(null));
        }
    }// copyPreviousPositions()


    /**
     * Copies the previous TurnState (Position, really) home SC and SC info.
     * <p>
     * If no previous home supply center information is available (e.g.,
     * initial turn), the information from the initial board setup is
     * used.
     * <p>
     * This method should only be used if no AdjustmentInfo block has
     * been detected.
     */
    private void copyPreviousSCInfo(final TurnState current) {
        LOG.debug("copyPreviousSCInfo(): {}", current.getPhase());

        // get previous position information (or initial, if previous not available)
        final TurnState previousTS = current.getWorld()
                .getPreviousTurnState(current).get();
        final Position prevPos = previousTS == null ? oldPosition : previousTS
                .getPosition();

		/*
        if(previousTS != null)
		{
			LOG.debug("  Copying *previous* SC ownership info from: ", previousTS.getPhase());
		}
		else
		{
			LOG.debug("  !! Copying *previous* SC ownership info from: oldPosition");
			LOG.debug("  world has the following Turnstates: ");
			LOG.debug("  ", current.getWorld().getPhaseSet());
		}
		*/

        // current position
        final Position currentPos = current.getPosition();

        // copy!
        for (final Province province : map.getProvinces()) {
            Power power = prevPos.getSupplyCenterOwner(province).orElse(null);
            if (power != null) {
                //System.out.println("  SC @ "+provinces[i]+", owned by "+power);
                currentPos.setSupplyCenterOwner(province, power);
                LOG.debug("  set SC: {} owned by {}", province, power);
            }
            power = prevPos.getSupplyCenterHomePower(province).orElse(null);
            if (power != null) {
                currentPos.setSupplyCenterHomePower(province, power);
                LOG.debug("  set HSC: {} owned by {}", province, power);
            }
        }
    }// copyPreviousSCInfo()


    /**
     * Copies the Previous turnstate's lastOccupier information only
     */
    private void copyPreviousLastOccupierInfo(final TurnState current) {
        final TurnState previousTS = current.getWorld()
                .getPreviousTurnState(current).get();
        final Position newPos = current.getPosition();
        final Position oldPos = previousTS == null ? oldPosition : previousTS
                .getPosition();

        for (final Province p : map.getProvinces()) {
            // clone any lastOccupied info as well.
            newPos.setLastOccupier(p, oldPos.getLastOccupier(p).orElse(null));
        }
    }// copyPreviousLastOccupierInfo()


    /**
     * Processes a block of adjustment info; this can occur during a
     * Move or Retreat phase. Only the Supply Center ownership is used;
     * the adjustment values are ignored, since they can be computed
     * based upon ownership information.
     * <p>
     * If no SC owner info exists, copyPreviousSCInfo() is used to
     * supply the appropriate information.
     */
    private void procAdjustmentBlock(final List<OwnerInfo> ownerInfo,
                                     final TurnState ts,
                                     final Position position) throws IOException {
        LOG.debug("procAdjustmentBlock(): {}", ts.getPhase());
        if (ownerInfo.isEmpty()) {
            LOG.debug(
                    "   No adjustment block. Copying previous SC ownership info.");
            copyPreviousSCInfo(ts);
        } else {
            for (final OwnerInfo anOwnerInfo : ownerInfo) {
                final Power power = map
                        .getPowerMatching(anOwnerInfo.getPowerName())
                        .orElseThrow(() -> new IOException(String.format(
                                "Unregognized power \"%s\" in Ownership block.",
                                anOwnerInfo.getPowerName())));
                LOG.debug("   SC Owned by Power: {}", power);
                for (final String provName : anOwnerInfo.getProvinces()) {
                    final Province province = map.getProvinceMatching(provName)
                            .orElseThrow(() -> new IOException(String.format(
                                    "Unknown Province in SC Ownership block: %s",
                                    provName)));
                    LOG.debug("       {}", province);
                    position.setSupplyCenterOwner(province, power);
                }
            }
        }
    }// procAdjustmentBlock()


    /**
     * Creates correct dislodged results (with retreat information) by matching
     * DislodgedInfo with the previously generated Dislodged result.
     * <p>
     * Units with no retreat results are destroyed, and a message generated indicating so.
     * <p>
     * old Dislodged results are discarded.
     */
    private void makeDislodgedResults(final Phase phase,
                                      final List<Result> results,
                                      final Position position,
                                      final List<DislodgedInfo> dislodgedInfo,
                                      final boolean positionPlacement) throws IOException {
        LOG.debug("JIH::makeDislodgedResults() [{}]", phase);
        LOG.debug("  # results: {}", results.size());
        final ListIterator<Result> iter = results.listIterator();
        while (iter.hasNext()) {
            final Result result = iter.next();
            if (result instanceof OrderResult) {
                final OrderResult orderResult = (OrderResult) result;
                if (ResultType.DISLODGED == orderResult.getResultType()) {
                    LOG.debug("  failed order: {}", orderResult.getOrder());

                    List<String> retreatLocNames = null;
                    for (final DislodgedInfo aDislodgedInfo : dislodgedInfo) {
                        // find the province for this dislodgedInfo source
                        // remember, we use map.parseLocation() to auto-normalize coasts (see Coast.normalize())
                        final Location location = map
                                .parseLocation(aDislodgedInfo.getSourceName())
                                .orElse(null);
                        if (orderResult.getOrder().getSource()
                                .isProvinceEqual(location)) {
                            retreatLocNames = aDislodgedInfo
                                    .getRetreatLocationNames();
                            break;
                        }
                    }

                    // we didn't find a match!! note that, and don't delete old dislodged order
                    if (retreatLocNames == null) {
                        iter.add(new Result(
                                Utils.getLocalString(JIH_NO_DISLODGED_MATCH,
                                        orderResult.getOrder())));

                        final String message = "Could not match dislodged order: " + orderResult
                                .getOrder() + "; phase: " + phase;
                        LOG.debug(message);

                        // we are more strict with our errors
                        throw new IOException(message);
                    } else {
                        try {
                            // create objects from retreat location names
                            final Location[] retreatLocations = new Location[retreatLocNames
                                    .size()];
                            for (int i = 0; i < retreatLocNames.size(); i++) {
                                retreatLocations[i] = map
                                        .parseLocation(retreatLocNames.get(i))
                                        .orElse(null);
                                retreatLocations[i] = retreatLocations[i]
                                        .getValidated(orderResult.getOrder()
                                                .getSourceUnitType());
                            }

                            LOG.debug("    possible retreats: {}",
                                    Arrays.toString(retreatLocations));

                            // remove old dislodged result, replacing with the new dislodged result
                            iter.set(new DislodgedResult(orderResult.getOrder(),
                                    retreatLocations));

                            // if no retreat results, destroy unit
                            if (retreatLocations.length == 0) {
                                // destroy
                                final Province province = orderResult.getOrder()
                                        .getSource().getProvince();
                                final Unit unit;

								/*
                                 * Check for the positionPlacement flag. If so, go ahead and set the unit to the
								 * dislodged one. If not, the unit that is dislodged is not SHOWN as dislodged
								 * therefore get that one.
								 */
                                if (positionPlacement) {
                                    unit = position.getDislodgedUnit(province)
                                            .orElse(null);
                                } else {
                                    unit = position.getUnit(province)
                                            .orElse(null);
                                }

                                position.setDislodgedUnit(province, null);

                                // send result
                                iter.add(new Result(unit.getPower(),
                                        Utils.getLocalString(
                                                STDADJ_MV_UNIT_DESTROYED,
                                                unit.getType().getFullName(),
                                                province)));
                            }
                        } catch (final OrderException e) {
                            // couldn't validate!!
                            iter.add(new Result(
                                    Utils.getLocalString(JIH_INVALID_RETREAT,
                                            orderResult.getOrder())));
                            LOG.debug(
                                    "JIH::makeDislodgedResults(): exception: {}",
                                    orderResult.getOrder());
                            throw new IOException(e);
                        }
                    }
                }
            }
        }
    }// makeDislodgedResults()

    private void createStartingPositions() throws IOException {
        Phase phase = null;

        // determine the next phase by reading through the turn text.
        final Pattern pattern = Pattern.compile(START_POSITIONS);
        final Matcher m = pattern.matcher(jp.getText());

        if (m.find()) {
            final String sb = m.group(1) +
                    ' ' +
                    m.group(2) +
                    ' ' +
                    m.group(3);
            phase = Phase.parse(sb).orElse(null);
        }

        if (phase == null) {
            throw new IOException(Utils.getLocalString(JIH_BAD_LAST_PHASE));
        }

        // Create the new turnstate
        final TurnState ts = new TurnState(phase);
        ts.setWorld(world);
        ts.setPosition(new Position(world.getMap()));

        // set Home Supply centers in position
        final Position pos = oldPosition;
        for (int i = 0; i < oldPosition.getHomeSupplyCenters()
                .toArray(new Province[0]).length; i++) {
            pos.setSupplyCenterHomePower(oldPosition.getHomeSupplyCenters()
                    .toArray(new Province[0])[i], oldPosition
                    .getSupplyCenterHomePower(oldPosition.getHomeSupplyCenters()
                            .toArray(new Province[0])[i]).orElse(null));
        }

        // Copy previous phase positions
        copyPreviousPositions(ts);

        // Copy previous SC info (we need proper ownership info before parsing orders)
        copyPreviousSCInfo(ts);

        // check for elimination
        ts.getPosition().setEliminationStatus(map.getPowers());

        // add to World
        world.setTurnState(ts);
    }// createStartingPositions()

    /**
     * Creates the last TurnState, which is always ready for adjudication.
     * <p>
     * If parsing fails, no last turnstate will be created.
     */
    private void makeLastTurnState(final Turn lastTurn) throws IOException {
        Phase phase = null;

        // determine the next phase by reading through the turn text.
        final Pattern pattern = Pattern.compile(PARSE_REGEX);

        final Matcher m = pattern.matcher(lastTurn.getText());

        if (m.find()) {
            final String sb = m.group(1) +
                    ' ' +
                    m.group(2) +
                    ' ' +
                    m.group(3);
            phase = Phase.parse(sb).orElse(null);
        }

        if (phase == null) {
            throw new IOException(Utils.getLocalString(JIH_BAD_LAST_PHASE));
        }

        // Create the new turnstate
        final TurnState ts = new TurnState(phase);
        ts.setWorld(world);
        ts.setPosition(new Position(world.getMap()));

        // set Home Supply centers in position
        final Position pos = ts.getPosition();
        for (final HSCInfo aHomeSCInfo : homeSCInfo) {
            pos.setSupplyCenterHomePower(aHomeSCInfo.getProvince(),
                    aHomeSCInfo.getPower());
        }

        // Copy previous phase positions
        copyPreviousPositions(ts);

        // Copy previous SC info (we need proper ownership info before parsing orders)
        copyPreviousSCInfo(ts);

        // check for elimination
        ts.getPosition().setEliminationStatus(map.getPowers());

        // add to World
        world.setTurnState(ts);
    }// makeLastTurnState()


    /**
     * If a WING unit is detected, make sure we have the WING option
     * enabled; if it already is, do nothing.
     */
    private void checkAndEnableWings(final Type unitType) {
        if (Type.WING == unitType) {
            final RuleOptions ruleOpts = world.getRuleOptions();
            if (OptionValue.VALUE_WINGS_DISABLED == ruleOpts
                    .getOptionValue(Option.OPTION_WINGS)) {
                ruleOpts.setOption(Option.OPTION_WINGS,
                        OptionValue.VALUE_WINGS_ENABLED);
                world.setRuleOptions(ruleOpts);
            }
        }
    }// enableWings()


    /**
     * Home Supply Center information
     */
    private static class HSCInfo {
        private final Province province;
        private final Power power;

        private HSCInfo(final Province province, final Power power) {
            this.province = province;
            this.power = power;
        }// HSCInfo()

        public Province getProvince() {
            return province;
        }

        public Power getPower() {
            return power;
        }
    }// inner class HSCInfo

}// class JudgeImportHistory
