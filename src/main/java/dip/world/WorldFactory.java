//
//  @(#)WorldFactory.java		4/2002
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
package dip.world;

import dip.misc.Log;
import dip.misc.Utils;
import dip.order.OrderException;
import dip.world.Province.Adjacency;
import dip.world.Unit.Type;
import dip.world.variant.data.BorderData;
import dip.world.variant.data.ProvinceData;
import dip.world.variant.data.Variant;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * A WorldFactory creates World objects from XML map data.
 */
public class WorldFactory {
    // il8n
    private static final String WF_PROV_NON_UNIQUE = "WF_PROV_NON_UNIQUE";
    private static final String WF_PROV_MISMATCH = "WF_PROV_MISMATCH";
    //private static final String WF_VARIANT_NOTFOUND = "WF_VARIANT_NOTFOUND";
    private static final String WF_BAD_STARTINGTIME = "WF_BAD_STARTINGTIME";
    private static final String WF_BAD_SC_PROVINCE = "WF_BAD_SC_PROVINCE";
    private static final String WF_BAD_SC_HOMEPOWER = "WF_BAD_SC_HOMEPOWER";
    private static final String WF_BAD_SC_OWNER = "WF_BAD_SC_OWNER";
    private static final String WF_BAD_IS_POWER = "WF_BAD_IS_POWER";
    private static final String WF_BAD_IS_PROVINCE = "WF_BAD_IS_PROVINCE";
    private static final String WF_BAD_IS_UNIT_LOC = "WF_BAD_IS_UNIT_LOC";
    private static final String WF_BAD_IS_UNIT = "WF_BAD_IS_UNIT";
    private static final String WF_ADJ_BAD_TYPE = "WF_ADJ_BAD_TYPE";
    private static final String WF_ADJ_BAD_PROVINCE = "WF_ADJ_BAD_PROVINCE";
    private static final String WF_ADJ_INVALID = "WF_ADJ_INVALID";
    //private static final String WF_BAD_BUILDOPTION = "WF_BADBUILDOPTION";
    private static final String WF_BAD_BORDER_NAME = "WF_BAD_BORDER_NAME";
    private static final String WF_BAD_BORDER_LOCATION = "WF_BAD_BORDER_LOCATION";


    // class variables
    private static final WorldFactory instance = new WorldFactory();


    private WorldFactory() {
    }// WorldFactory()


    /**
     * Get an instance of the WorldFactory
     */
    public static synchronized WorldFactory getInstance() {
        return instance;
    }// getInstance()


    /**
     * Generates a World given the supplied Variant information
     */
    public static World createWorld(final Variant variant) {
        Objects.requireNonNull(variant);

        Log.println("WorldFactory.createWorld(): " + variant.getName());

        // gather all province data, and create provinces
        final List<ProvinceData> provinceDataArray = variant.getProvinceData();
        final List<Province> provinces = IntStream
                .range(0, provinceDataArray.size()).mapToObj(i -> {
                    final ProvinceData provinceData = provinceDataArray.get(i);
                    // get short names
                    final List<String> shortNames = provinceData
                            .getShortNames();

                    // create Province object
                    return new Province(provinceData.getFullName(), shortNames,
                            i, provinceData.getConvoyableCoast());
                }).collect(Collectors.toList());

        // verify uniqueness of names
        final Map<String, Province> provNameMap = new TreeMap<>(
                String.CASE_INSENSITIVE_ORDER);    // mapping of names->provinces
        provinces.stream().forEach(province -> {
            final String fullname = province.getFullName();
            final List<String> shortNames = province.getShortNames();
            if (!isUnique(provNameMap, fullname, shortNames)) {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_PROV_NON_UNIQUE,
                                province.getFullName()));
            }
            provNameMap.put(fullname, province);
            shortNames.stream()
                    .forEach(shortname -> provNameMap.put(shortname, province));
        });

        // gather all adjacency data
        // parse adjacency data for all provinces
        // keep a list of the locations parsed below
        provinceDataArray.stream().forEach(provinceData -> {
            final List<String> adjProvinceTypes = provinceData
                    .getAdjacentProvinceTypes();
            final List<String> adjProvinceNames = provinceData
                    .getAdjacentProvinceNames();

            if (adjProvinceTypes.size() != adjProvinceNames.size()) {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_PROV_MISMATCH));
            }

            // get the Province to which this adjacency data refers
            final Province province = provNameMap
                    .get(provinceData.getFullName());

            // get the Adjacency data structure from the Province
            final Adjacency adjacency = province.getAdjacency();

            // parse adjacency data, then set it for this province
            IntStream.range(0, adjProvinceTypes.size()).forEach(adjIdx -> {
                // get the coast type.
                final Coast coast = Coast.parse(adjProvinceTypes.get(adjIdx));

                // parse provinces, making locations for each
                // provinces must be seperated by " " or "," or ";" or ":"
                final String input = adjProvinceNames.get(adjIdx).trim()
                        .toLowerCase();
                // add data to adjacency table after unwrapping collection
                adjacency.setLocations(coast,
                        Arrays.stream(input.split("[ ,;:\t\n\r]+"))
                                .map(st -> makeLocation(provNameMap, st, coast))
                                .collect(Collectors.toList()));
            });


            // validate adjacency data
            if (!adjacency.validate(province)) {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_ADJ_INVALID,
                                provinceData.getFullName()));
            }

            // create wing coast
            adjacency.createWingCoasts();
        });

        // Process BorderData. This requires the Provinces to be known and
        // successfully parsed. They are mapped to the ID name, stored in the borderMap.
        final Map<String, Border> borderMap;
        try {
            borderMap = variant.getBorderData().stream().collect(Collectors
                    .toMap(BorderData::getID,
                            bd -> new Border(bd.getID(), bd.getDescription(),
                                    bd.getUnitTypes(),
                                    makeBorderLocations(bd.getFrom(),
                                            provNameMap), bd.getOrderTypes(),
                                    bd.getBaseMoveModifier(), bd.getSeason(),
                                    bd.getPhase(), bd.getYear())));
        } catch (final InvalidBorderException ibe) {
            throw new InvalidWorldException(ibe);
        }

        // set the Border data (if any) for each province.
        provinceDataArray.stream().forEach(aProvinceDataArray -> {
            final Province province = provNameMap
                    .get(aProvinceDataArray.getFullName());

            province.setBorders(
                    aProvinceDataArray.getBorders().stream().map(borderName -> {
                        final Border border = borderMap.get(borderName);
                        if (border == null) {
                            throw new InvalidWorldException(
                                    Utils.getLocalString(WF_BAD_BORDER_NAME,
                                            province.getShortName(),
                                            borderName));
                        }
                        return border;
                    }).collect(Collectors.toList()));
        });

        // Now that we know the variant, we know the powers, and can
        // create the Map.
        final WorldMap map = new WorldMap(variant.getPowers(), provinces);

        // create the World object as well, now that we have the Map
        final World world = new World(map);

        // create the Position object, as we will need it for various game state
        final Position pos = new Position(map);

        // define supply centers
        variant.getSupplyCenters().stream().forEach(supplyCenter -> {
            final Province province = map
                    .getProvince(supplyCenter.getProvinceName());
            if (province == null) {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_BAD_SC_PROVINCE,
                                supplyCenter.getProvinceName()));
            }

            province.setSupplyCenter(true);

            final String hpName = supplyCenter.getHomePowerName();
            if (!"none".equalsIgnoreCase(hpName)) {
                final Power power = map.getPower(hpName);
                if (power == null) {
                    throw new InvalidWorldException(
                            Utils.getLocalString(WF_BAD_SC_HOMEPOWER, hpName));
                }

                pos.setSupplyCenterHomePower(province, power);
            }

            // define current owner of supply center, if any
            final String scOwner = supplyCenter.getOwnerName();
            if (!"none".equalsIgnoreCase(scOwner)) {
                final Power power = map.getPower(scOwner);
                if (power == null) {
                    throw new InvalidWorldException(
                            Utils.getLocalString(WF_BAD_SC_OWNER, scOwner));
                }

                pos.setSupplyCenterOwner(province, power);
            }
        });

        // set initial state [derived from INITIALSTATE elements in XML file]
        variant.getInitialStates().stream().forEach(initState -> {
            // a province and power is required, no matter what, unless
            // we are ONLY setting the supply center (which we do above)

            final Province province = map
                    .getProvinceMatching(initState.getProvinceName())
                    .orElseThrow(() -> new InvalidWorldException(
                            Utils.getLocalString(WF_BAD_IS_PROVINCE)));

            final Type unitType = initState.getUnitType();

            if (unitType == null) {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_BAD_IS_UNIT,
                                initState.getProvinceName()));
            }
            // create unit in province, if location is valid
            final Coast coast = initState.getCoast();

            final Unit unit = new Unit(
                    map.getPowerMatching(initState.getPowerName()).orElseThrow(
                            () -> new InvalidWorldException(
                                    Utils.getLocalString(WF_BAD_IS_POWER))),
                    unitType);
            final Location location = new Location(province, coast);
            try {
                unit.setCoast(location.getValidatedSetup(unitType).getCoast());
                pos.setUnit(province, unit);

                // set 'lastOccupier' for unit
                pos.setLastOccupier(province, unit.getPower());
            } catch (final OrderException e) {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_BAD_IS_UNIT_LOC,
                                initState.getProvinceName(), e.getMessage()),
                        e);
            }
        });

        // create initial turn state based on starting game time
        final Phase phase = variant.getStartingPhase();
        if (phase == null) {
            throw new InvalidWorldException(
                    Utils.getLocalString(WF_BAD_STARTINGTIME));
        }
        world.setVictoryConditions(
                new VictoryConditions(variant.getNumSCForVictory(),
                        variant.getMaxYearsNoSCChange(),
                        variant.getMaxGameTimeYears(), phase));

        // set TurnState / Map / complete World creation.
        final TurnState turnState = new TurnState(phase);
        turnState.setPosition(pos);
        turnState.setWorld(world);
        world.setTurnState(turnState);

        return world;
    }// makeWorld()


    /**
     * Makes a Border location. This uses the already-generated Provinces and Adjacency data,
     * which help error checking. It also will create "undefined" coasts by default. If the
     * coast does not exist for a Province (but is not Undefined) then this will create an
     * Exception.
     * <p>
     * Input is a space and/or comma-seperated list.
     * <p>
     * This will return null if there are no border locations, instead of
     * a zero-length array.
     */
    private static List<Location> makeBorderLocations(final String in,
                                                      final Map<String, Province> provNameMap) {
        return Arrays.stream(in.trim().split("[;, ]+")).map(tok -> new Location(
                provNameMap.get(Coast.getProvinceName(tok)), Coast.parse(tok)))
                .collect(Collectors.toList());
    }// makeBorderLocation()


    /**
     * Parses the Adjacency data and converts it into the Location objects
     * e.g.:
     * <p>
     * The default coast is corrected as follows:
     * <br>	'mv' : stays 'mv' (Coast.LAND)
     * <br>	'xc' : stays 'xc' (Coast.SINGLE)
     * <br>	'nc', 'wc', 'ec', 'sc' : converted to 'xc' (Coast.SINGLE)
     * <p>
     * The default coast is over-ridden if a coast is specified
     * after a ref. thus given: &lt;ADJACENCY type="xc" refs="xxx yyy/sc"/&gt;
     * "yyy" will have a coast of "sc" (Coast.SOUTH)
     * <p>
     * The reason is because look at the following (for bulgaria)
     * <pre>
     * 	&lt;PROVINCE shortname="bul" fullname="Bulgaria"&gt;
     * 		&lt;ADJACENCY type="mv" refs="gre con ser rum" /&gt;
     * 		&lt;ADJACENCY type="ec" refs="con bla rum" /&gt;
     * 		&lt;ADJACENCY type="sc" refs="gre aeg con" /&gt;
     * 	&lt;/PROVINCE&gt;
     * </pre>
     * If we do not convert directional coasts (nc/ec/wc/sc) to (xc),
     * bul/ec would then be linked to con/ec, bla/ec, rum/ec which
     * do not even exist.
     */
    private static Location makeLocation(
            final Map<String, Province> provNameMap, final String name,
            final Coast theDefaultCoast) {
        if (theDefaultCoast == Coast.UNDEFINED) {
            throw new InvalidWorldException(
                    Utils.getLocalString(WF_ADJ_BAD_TYPE, name));
        }

        final Coast defaultCoast = theDefaultCoast == Coast.NORTH ||
                theDefaultCoast == Coast.WEST ||
                theDefaultCoast == Coast.SOUTH ||
                theDefaultCoast == Coast.EAST ? Coast.SINGLE : theDefaultCoast;

        Coast coast = Coast.parse(name);

        if (coast == Coast.UNDEFINED) {
            coast = defaultCoast;
        }

        // name lookup
        final String provinceName = Coast.getProvinceName(name);
        final Province province = provNameMap.get(provinceName);
        if (province == null) {
            throw new InvalidWorldException(
                    Utils.getLocalString(WF_ADJ_BAD_PROVINCE, name,
                            provinceName, defaultCoast));
        }

        // create Location
        return new Location(province, coast);
    }// makeLocation()


    // verify all names are unique. (hasn't yet been added to the map)
    private static boolean isUnique(final Map<String, Province> provNameMap,
                                    final String fullname,
                                    final Collection<String> shortnames) {
        return !(provNameMap.containsKey(fullname) || shortnames.stream()
                .anyMatch(shortname -> provNameMap.containsKey(shortname)));
    }// isUnique()

}// class MapFactory

