//
//  @(#)Variant.java	1.00	7/2002
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
package dip.world.variant.data;

import dip.misc.Utils;
import dip.world.Phase;
import dip.world.Power;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A Variant.
 */
@XmlRootElement(name = "VARIANT")
public class Variant implements Cloneable, Comparable<Variant> {
    // the arrays in general should not be null. They are defined as null initially
    // to make it more apparent should a field not be initialized properly.
    //
    @XmlAttribute(name = "name", required = true)
    private String name = "";
    @XmlAttribute(name = "default", required = true)
    private boolean isDefault;
    @XmlAttribute(name = "version", required = true)
    private float version;
    // @XmlAttribute(name="aliases")
    private List<String> aliases = Collections.emptyList();

    @XmlElement(name = "DESCRIPTION")
    private String description;
    @XmlElement(name = "VICTORYCONDITIONS", required = true)
    public VictoryConditions victoryConditions = new VictoryConditions();

    @XmlElement(name = "POWER")
    private List<Power> powers;

    @XmlElement(name = "SUPPLYCENTER")
    private List<SupplyCenter> supplyCenters;
    @XmlElement(name = "INITIALSTATE")
    private List<InitialState> istate;

    private List<ProvinceData> provinceData;
    private List<BorderData> borderData;

    @XmlElement(name = "MAP")
    private Map map = new Map();

    public static class Map {
        // <MAP adjacencyURI="std_adjacency.xml">
        // <MAP_GRAPHIC ref="standard" default="true"/>
        @XmlAttribute(name = "adjacencyURI")
        private URI adjacencyURI;
        @XmlElement(name = "MAP_GRAPHIC")
        private List<MapGraphic> mapGraphics;

    }

    @XmlElementWrapper(name = "RULEOPTIONS")
    @XmlElement(name = "RULEOPTION")
    private List<NameValuePair> roNVPs;

    // @XmlAttribute(name = "turn")
    // @XmlAttribute(name = "turn", required = true)
    //     <STARTINGTIME turn="Spring, 1914, Movement" allowBCYears="true"/>
    @XmlElement(name = "STARTINGTIME", required = true)
    private StartingTime startingTime = new StartingTime();

    @XmlRootElement
    public static class StartingTime {
        private Phase phase;
        @XmlAttribute(name = "allowBCYears")
        private boolean allowBCYears;

        @XmlAttribute(name = "turn")
        public void setPhase(String val) {
            phase = Phase.parse(val);
        }

        void afterUnmarshal(final Unmarshaller unmarshaller,
                            final Object parent) {
            if (phase.getYear() < 0) {
                allowBCYears = true;
            }
        }
    }

    @XmlRootElement
    public static class VictoryConditions {
        @XmlElement(name = "WINNING_SUPPLY_CENTERS")
        private IntWrapper vcNumSCForVictory = new IntWrapper();
        @XmlElement(name = "YEARS_WITHOUT_SC_CAPTURE")
        private IntWrapper vcMaxYearsNoSCChange = new IntWrapper();
        @XmlElement(name = "GAME_LENGTH")
        private IntWrapper vcMaxGameTimeYears = new IntWrapper();

        @XmlRootElement
        public static class IntWrapper {
            @XmlAttribute(name = "value", required = true)
            private int value;
        }

    }

    void afterUnmarshal(final Unmarshaller unmarshaller, final Object parent) {
//        System.out.println(victoryConditions);
//        System.out.println(victoryConditions.vcMaxGameTimeYears);
//        if (phase.getYear() < 0) {
//            allowBCYears = true;
//        }
    }

    /**
     * Class of Rule Option name/value pairs
     */
    @XmlRootElement
    public static class NameValuePair {
        @XmlAttribute
        private String name;
        @XmlAttribute
        private String vallue;
        private String value;


        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }// nested class NameValuePair


    /**
     * Construct a new Variant object
     */
    public Variant() {
    }

    /**
     * The name of the variant.
     */
    public String getName() {
        return name;
    }

    /**
     * The aliases (alternate names) of the variant. Never null.
     */
    public String[] getAliases() {
        return aliases.toArray(new String[aliases.size()]);
    }

    /**
     * Version of this variant
     */
    public float getVersion() {
        return version;
    }

    /**
     * Whether this is the default variant.
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Description for variant; this is typically HTML encoded.
     */
    public String getDescription() {
        return description;
    }

    /**
     * The starting time.
     */
    public Phase getStartingPhase() {
        return startingTime.phase;
    }

    /**
     * The starting InitialStates.
     */
    public InitialState[] getInitialStates() {
        return istate.toArray(new InitialState[istate.size()]);
    }

    /**
     * Returns Powers associated with this Variant.
     */
    public Power[] getPowers() {
        return powers.toArray(new Power[powers.size()]);
    }

    /**
     * Returns SupplyCenter objects
     */
    public SupplyCenter[] getSupplyCenters() {
        return supplyCenters.toArray(new SupplyCenter[supplyCenters.size()]);
    }

    /**
     * Victory Conditions: Number of Supply Centers required for victory.
     */
    public int getNumSCForVictory() {
        return victoryConditions.vcNumSCForVictory.value;
    }

    /**
     * Victory Conditions: Maximum years without a supply-center ownership change before game ends.
     */
    public int getMaxYearsNoSCChange() {
        return victoryConditions.vcMaxYearsNoSCChange.value;
    }

    /**
     * Victory Conditions: Maximum game duration, in years.
     */
    public int getMaxGameTimeYears() {
        return victoryConditions.vcMaxGameTimeYears.value;
    }

    /**
     * The mapGraphics associated with this Variant.
     */
    public MapGraphic[] getMapGraphics() {
        return map.mapGraphics.toArray(new MapGraphic[map.mapGraphics.size()]);
    }

    /**
     * The ProvinceData associated with this Variant
     */
    public ProvinceData[] getProvinceData() {
        return provinceData.toArray(new ProvinceData[provinceData.size()]);
    }

    /**
     * The RuleOptions (as name-value pairs) associated with this Variant
     */
    public NameValuePair[] getRuleOptionNVPs() {
        return roNVPs.toArray(new NameValuePair[roNVPs.size()]);
    }

    /**
     * Gets the BorderData associated with this Variant
     */
    public BorderData[] getBorderData() {
        return borderData.toArray(new BorderData[borderData.size()]);
    }

    /**
     * Gets if BC Years are allowed with this Variant
     */
    public boolean getBCYearsAllowed() {
        return startingTime.allowBCYears;
    }


    /**
     * Set the variant name.
     */
//    public void setName(final String value) {
//        name = value;
//    }

    /**
     * Set the alises. Null is not allowed.
     */
    @XmlAttribute(name = "aliases", required = true)
    public void setAliases(final String csv) {
        final String[] aliases = Utils.parseCSV(csv);
        if (aliases == null) {
            throw new IllegalArgumentException();
        }
        this.aliases = Arrays.asList(aliases);
    }

    /**
     * Set the version of this variant
     */
//    public void setVersion(final float value) {
//        version = value;
//    }

    /**
     * Set if this variant is the default variant.
     */
    public void setDefault(final boolean value) {
        isDefault = value;
    }

    /**
     * Set the description for this variant.
     */
//    public void setDescription(final String value) {
//        description = value;
//    }

    /**
     * Set the starting phase for this variant.
     */
//    @XmlElement(name = "STARTINGTIME")
//    @XmlAttribute(name = "turn", required = true)
    @XmlAttribute(name = "turn", required = true)
    public void setStartingPhase(final String value) {
        throw new IllegalArgumentException("setstartingPhase: " + value);
//        phase = Phase.parse(value);
//        if (phase == null) {
//            throw new IllegalArgumentException("setstartingPhase");
//        }
    }

    public void setStartingPhase(final Phase value) {
        startingTime.phase = value;
    }

    /**
     * Victory Conditions: Number of Supply Centers required for victory.
     */
    public void setNumSCForVictory(final int value) {
        victoryConditions.vcNumSCForVictory.value = value;
    }

    /**
     * Victory Conditions: Maximum years without a supply-center ownership change before game ends.
     */
    public void setMaxYearsNoSCChange(final int value) {
        victoryConditions.vcMaxYearsNoSCChange.value = value;
    }

    /**
     * Victory Conditions: Maximum game duration, in years.
     */
    public void setMaxGameTimeYears(final int value) {
        victoryConditions.vcMaxGameTimeYears.value = value;
    }

    /**
     * Sets the ProvinceData associated with this Variant
     */
    public void setProvinceData(final ProvinceData[] value) {
        provinceData = Arrays.asList(value);
    }

    /**
     * Sets the BorderData associated with this Variant
     */
    public void setBorderData(final BorderData[] value) {
        borderData = Arrays.asList(value);
    }

    /**
     * Sets whether BC years (negative years) are allowed
     */
    public void setBCYearsAllowed(final boolean value) {
        startingTime.allowBCYears = value;
    }


    /**
     * Sets the MapGraphics, from a List
     */
    public void setMapGraphics(final List mgList) {
        map.mapGraphics = new ArrayList<>(mgList);
    }// setPowers()

    /**
     * Sets the Powers, from a List
     */
    public void setPowers(final List powerList) {
        powers = new ArrayList<>(powerList);
    }// setPowers()

    /**
     * Sets the InitialStates, from a List
     */
    public void setInitialStates(final List stateList) {
        istate = new ArrayList<>(stateList);
    }// setInitialStates()

    /**
     * Sets the supply centers, from a List
     */
    public void setSupplyCenters(final List supplyCenterList) {
        supplyCenters = new ArrayList<>(supplyCenterList);
    }// setSupplyCenters()

    /**
     * Sets the RuleOptions (as a List of name-value pairs) associated with this Variant
     */
    public void setRuleOptionNVPs(final List nvpList) {
        roNVPs = new ArrayList<>(nvpList);
    }// setRuleOptionNVPs()


    /**
     * Changes the active/inactive state of a power. The number of values <b>must</b> equal the number of powers.
     */
    public void setActiveState(final boolean[] values) {
        if (values.length != powers.size()) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < powers.size(); i++) {
            if (powers.get(i).isActive() != values[i]) {
                // Powers are constant; we must create a new one.
                final Power old = powers.get(i);
                powers.set(i, new Power(old.getNames(), old.getAdjective(),
                        values[i]));
            }
        }
    }// setActiveState()


    /**
     * Compares based on Name
     */
    @Override
    public int compareTo(final Variant o) {
        return name.compareTo(o.name);
    }// compareTo()


    /**
     * Finds the MapGraphic by name; case insensitive.
     */
    public MapGraphic getMapGrapic(final String mgName) {
        if (map.mapGraphics != null) {
            return map.mapGraphics.stream()
                    .filter(mapGraphic -> mapGraphic.getName()
                            .equalsIgnoreCase(mgName)).findFirst().orElse(null);
        }
        return null;
    }// getVariant()


    /**
     * Gets the default MapGraphic; if there is no default, returns the first one.
     */
    public MapGraphic getDefaultMapGraphic() {
        MapGraphic mg = null;

        if (map.mapGraphics != null && map.mapGraphics.size() > 0) {
            mg = map.mapGraphics.stream().filter(MapGraphic::isDefault)
                    .findFirst().orElse(map.mapGraphics.get(0));
        }

        return mg;
    }// getDefaultMapGraphic()


    /**
     * Gets the arguments for an HTML description, suitable for insertion
     * inside an appropriately-marked HTML template (arguments are
     * surrounded by curly braces).
     * <p>
     * Arguments for the HTML template are:
     * <ol>
     * <li>Variant name</li>
     * <li>variant-description (note: may be in html)</li>
     * <li>Supply centers for victory</li>
     * <li>Starting season</li>
     * <li>Starting year</li>
     * <li>Starting phase</li>
     * <li>Powers (comma-separated list)</li>
     * <li>Number of Powers</li>
     * </ol>
     * 8 arguments are given in total.
     */
    public Object[] getHTMLSummaryArguments() {
        final Collection<Object> args = new ArrayList<>(8);
        args.add(name);
        args.add(description);
        args.add(String.valueOf(victoryConditions.vcNumSCForVictory.value));
        if (startingTime.phase == null) {
            args.add("{bad phase}");
            args.add("{bad phase}");
            args.add("{bad phase}");
        } else {
            args.add(startingTime.phase.getSeasonType());
            args.add(startingTime.phase.getYearType());
            args.add(startingTime.phase.getPhaseType());
        }
        // create list of powers
        args.add(powers.stream()
                .map(power -> power.isActive() ? power.getName() : String
                        .join("", "(", power.getName(), ")"))
                .collect(Collectors.joining(", ")));
        args.add(String.valueOf(powers.size()));

        return args.toArray(new Object[args.size()]);
    }// getHTMLSummaryArguments()


    /**
     * Creates a deep clone of all data EXCEPT InitialState / SupplyCenter data / Name / Description
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        // shallow clone
        final Variant variant = (Variant) super.clone();

        // deep clone
        //
        // phase
        if (startingTime.phase != null) {
            // cheap...
            variant.startingTime.phase = Phase
                    .parse(startingTime.phase.toString());
        }

        // powers
        if (powers != null) {
            variant.powers = powers.stream()
                    .map(thisPower -> new Power(thisPower.getNames(),
                            thisPower.getAdjective(), thisPower.isActive()))
                    .collect(Collectors.toList());
        }

        return variant;
    }// clone()

    /**
     * For debugging only!
     */
    @Override
    public String toString() {
        istate.stream().forEach(System.out::println);
        supplyCenters.stream().forEach(System.out::println);
        provinceData.stream().forEach(System.out::println);
        map.mapGraphics.stream().forEach(System.out::println);
        return String
                .join("", getClass().getName(), "[", "name=", name.toString(),
                        ",isDefault=", Boolean.toString(isDefault), "powers=",
                        powers.stream().map(power -> power.toString())
                                .collect(Collectors.joining(",")), ",phase=",
                        startingTime.phase.toString(), ",istate=",
                        ",supplyCenters=", ",provinceData=", "mapGraphics=",
                        ",vcNumSCForVictory=", Integer.toString(
                                victoryConditions.vcNumSCForVictory.value),
                        ",vcMaxGameTimeYears=", Integer.toString(
                                victoryConditions.vcMaxGameTimeYears.value),
                        ",vcMaxYearsNoSCChange=", Integer.toString(
                                victoryConditions.vcMaxYearsNoSCChange.value),
                        ",version=", Float.toString(version), "]");
    }// toString()
}// class Variant


