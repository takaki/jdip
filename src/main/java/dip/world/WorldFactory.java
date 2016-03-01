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
import dip.world.variant.data.InitialState;
import dip.world.variant.data.ProvinceData;
import dip.world.variant.data.SupplyCenter;
import dip.world.variant.data.Variant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;


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
    private static final String WF_BAD_VC = "WF_BAD_VC";
    private static final String WF_ADJ_BAD_TYPE = "WF_ADJ_BAD_TYPE";
    private static final String WF_ADJ_BAD_PROVINCE = "WF_ADJ_BAD_PROVINCE";
    private static final String WF_ADJ_INVALID = "WF_ADJ_INVALID";
    //private static final String WF_BAD_BUILDOPTION = "WF_BADBUILDOPTION";
    private static final String WF_BAD_BORDER_NAME = "WF_BAD_BORDER_NAME";
    private static final String WF_BAD_BORDER_LOCATION = "WF_BAD_BORDER_LOCATION";


    // class variables
    private static WorldFactory instance = new WorldFactory();


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
    public static World createWorld(
            final Variant variant) throws InvalidWorldException {
        Objects.requireNonNull(variant);

        Log.println("WorldFactory.createWorld(): " + variant.getName());

        final List<Province> provinces = new ArrayList<>(100);
        final HashMap<String, Province> provNameMap = new HashMap<>();    // mapping of names->provinces

        // gather all province data, and create provinces
        final List<ProvinceData> provinceDataArray = variant.getProvinceData();
        for (int i = 0; i < provinceDataArray.size(); i++) {
            final ProvinceData provinceData = provinceDataArray.get(i);

            // get short names
            final List<String> shortNames = provinceData.getShortNames();

            // verify uniqueness of names
            if (!isUnique(provNameMap, provinceData.getFullName(),
                    shortNames)) {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_PROV_NON_UNIQUE,
                                provinceData.getFullName()));
            }

            // create Province object
            final Province province = new Province(provinceData.getFullName(),
                    shortNames, i, provinceData.getConvoyableCoast());

            // add Province data to list
            provinces.add(province);

            // add Province names (all) to our name->province map
            provNameMap.put(province.getFullName().toLowerCase(), province);
            final String[] lcProvNames = province.getShortNames();
            for (final String lcProvName : lcProvNames) {
                provNameMap.put(lcProvName.toLowerCase(), province);
            }
        }

        // gather all adjacency data
        // parse adjacency data for all provinces
        // keep a list of the locations parsed below
        final ArrayList<Location> locationList = new ArrayList<>(16);
        for (final ProvinceData provinceData : provinceDataArray) {
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
                    .get(provinceData.getFullName().toLowerCase());

            // get the Adjacency data structure from the Province
            final Adjacency adjacency = province.getAdjacency();

            // parse adjacency data, then set it for this province
            for (int adjIdx = 0; adjIdx < adjProvinceTypes.size(); adjIdx++) {
                // get the coast type.
                final Coast coast = Coast.parse(adjProvinceTypes.get(adjIdx));

                // clear the location list (we re-use it)
                locationList.clear();

                // parse provinces, making locations for each
                // provinces must be seperated by " " or "," or ";" or ":"
                final String input = adjProvinceNames.get(adjIdx).trim()
                        .toLowerCase();
                final StringTokenizer st = new StringTokenizer(input,
                        " ,;:\t\n\r", false);
                while (st.hasMoreTokens()) {
                    // makeLocation() will change the coast, as needed, and verify the province
                    final Location location = makeLocation(provNameMap,
                            st.nextToken(), coast);
                    locationList.add(location);
                }

                // add data to adjacency table after unwrapping collection
                final Location[] locations = locationList
                        .toArray(new Location[locationList.size()]);
                adjacency.setLocations(coast, locations);
            }


            // validate adjacency data
            if (!adjacency.validate(province)) {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_ADJ_INVALID,
                                provinceData.getFullName()));
            }

            // create wing coast
            adjacency.createWingCoasts();
        }

        // Process BorderData. This requires the Provinces to be known and
        // successfully parsed. They are mapped to the ID name, stored in the borderMap.
        final java.util.Map<String, Border> borderMap = new HashMap<>(11);
        try {
            final List<BorderData> borderDataArray = variant.getBorderData();
            for (final BorderData bd : borderDataArray) {
                final Location[] fromLocs = makeBorderLocations(bd.getFrom(),
                        provNameMap);

                final Border border = new Border(bd.getID(),
                        bd.getDescription(), bd.getUnitTypes(), fromLocs,
                        bd.getOrderTypes(), bd.getBaseMoveModifier(),
                        bd.getSeason(), bd.getPhase(), bd.getYear());

                borderMap.put(bd.getID(), border);
            }
        } catch (final InvalidBorderException ibe) {
            throw new InvalidWorldException(ibe.getMessage());
        }

        // set the Border data (if any) for each province.
        final ArrayList<Border> list = new ArrayList<>(10);

        for (final ProvinceData aProvinceDataArray : provinceDataArray) {
            list.clear();
            final ProvinceData provinceData = aProvinceDataArray;
            final Province province = provNameMap
                    .get(provinceData.getFullName().toLowerCase());

            final List<String> borderNames = provinceData.getBorders();
            for (final String borderName : borderNames) {
                final Border border = borderMap.get(borderName);
                if (border == null) {
                    throw new InvalidWorldException(
                            Utils.getLocalString(WF_BAD_BORDER_NAME,
                                    province.getShortName(), borderName));
                }

                list.add(border);
            }

            if (!list.isEmpty()) {
                province.setBorders(list.toArray(new Border[list.size()]));
            }
        }

        // Now that we know the variant, we know the powers, and can
        // create the Map.
        final WorldMap map = new WorldMap(variant.getPowers().toArray(new Power[0]),
                provinces.toArray(new Province[provinces.size()]));

        // create the World object as well, now that we have the Map
        final World world = new World(map);

        // set variables to null that we don't need (just a safety check)
        borderMap.clear();

        // create initial turn state based on starting game time
        final Phase phase = variant.getStartingPhase();
        if (phase == null) {
            throw new InvalidWorldException(
                    Utils.getLocalString(WF_BAD_STARTINGTIME));
        }

        // create the Position object, as we will need it for various game state
        final Position pos = new Position(map);

        // define supply centers
        final List<SupplyCenter> supplyCenters = variant.getSupplyCenters();
        for (final SupplyCenter supplyCenter : supplyCenters) {
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
        }


        // set initial state [derived from INITIALSTATE elements in XML file]
        final List<InitialState> initStates = variant.getInitialStates();
        for (final InitialState initState : initStates) {
            // a province and power is required, no matter what, unless
            // we are ONLY setting the supply center (which we do above)
            final Power power = map.getPowerMatching(initState.getPowerName())
                    .orElseThrow(() -> new InvalidWorldException(
                            Utils.getLocalString(WF_BAD_IS_POWER)));

            final Province province = map
                    .getProvinceMatching(initState.getProvinceName())
                    .orElseThrow(() -> new InvalidWorldException(
                            Utils.getLocalString(WF_BAD_IS_PROVINCE)));

            final Type unitType = initState.getUnitType();

            if (unitType != null) {
                // create unit in province, if location is valid
                final Coast coast = initState.getCoast();

                final Unit unit = new Unit(power, unitType);
                Location location = new Location(province, coast);
                try {
                    location = location.getValidatedSetup(unitType);
                    unit.setCoast(location.getCoast());
                    pos.setUnit(province, unit);

                    // set 'lastOccupier' for unit
                    pos.setLastOccupier(province, unit.getPower());
                } catch (final OrderException e) {
                    throw new InvalidWorldException(
                            Utils.getLocalString(WF_BAD_IS_UNIT_LOC,
                                    initState.getProvinceName(),
                                    e.getMessage()));
                }
            } else {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_BAD_IS_UNIT,
                                initState.getProvinceName()));
            }
        }


        // set the victory conditions
        // make sure we have at least one victory condition!
        if (variant.getNumSCForVictory() <= 0 && variant
                .getMaxYearsNoSCChange() <= 0 && variant
                .getMaxGameTimeYears() <= 0) {
            throw new InvalidWorldException(Utils.getLocalString(WF_BAD_VC));
        }

        final VictoryConditions vc = new VictoryConditions(
                variant.getNumSCForVictory(), variant.getMaxYearsNoSCChange(),
                variant.getMaxGameTimeYears(), phase);
        world.setVictoryConditions(vc);

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
    private static Location[] makeBorderLocations(final String in,
                                                  final java.util.Map<String, Province> provNameMap) throws InvalidWorldException {
        final ArrayList<Location> al = new ArrayList<>(6);

        final StringTokenizer st = new StringTokenizer(in.trim(), ";, ");
        while (st.hasMoreTokens()) {
            final String tok = st.nextToken();

            final Coast coast = Coast.parse(tok);
            final Province province = provNameMap
                    .get(Coast.getProvinceName(tok).toLowerCase());
            if (province == null) {
                throw new InvalidWorldException(
                        Utils.getLocalString(WF_BAD_BORDER_LOCATION, tok));
            }

            al.add(new Location(province, coast));
        }

        return al.toArray(new Location[al.size()]);
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
            final java.util.Map<String, Province> provNameMap,
            final String name,
            final Coast theDefaultCoast) throws InvalidWorldException {
        Coast defaultCoast = theDefaultCoast;

        if (defaultCoast == Coast.UNDEFINED) {
            throw new InvalidWorldException(
                    Utils.getLocalString(WF_ADJ_BAD_TYPE, name));
        }
        if (defaultCoast == Coast.NORTH || defaultCoast == Coast.WEST || defaultCoast == Coast.SOUTH || defaultCoast == Coast.EAST) {
            defaultCoast = Coast.SINGLE;
        }

        Coast coast = Coast.parse(name);
        final String provinceName = Coast.getProvinceName(name);

        if (coast == Coast.UNDEFINED) {
            coast = defaultCoast;
        }

        // name lookup
        final Province province = provNameMap.get(provinceName.toLowerCase());
        if (province == null) {
            throw new InvalidWorldException(
                    Utils.getLocalString(WF_ADJ_BAD_PROVINCE, name,
                            provinceName, defaultCoast));
        }

        // create Location
        return new Location(province, coast);
    }// makeLocation()


    // verify all names are unique. (hasn't yet been added to the map)
    private static boolean isUnique(
            final java.util.Map<String, Province> provNameMap,
            final String fullname, final Iterable<String> shortnames) {
        if (provNameMap.get(fullname.toLowerCase()) != null) {
            return false;
        }

        for (final String shortname : shortnames) {
            if (provNameMap.get(shortname.toLowerCase()) != null) {
                return false;
            }
        }

        return true;
    }// isUnique()

}// class MapFactory

