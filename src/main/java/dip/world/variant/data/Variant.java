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

import dip.world.Phase;
import dip.world.Power;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A Variant.
 */
public class Variant implements Cloneable, Comparable<Variant> {
    // the arrays in general should not be null. They are defined as null initially
    // to make it more apparent should a field not be initialized properly.
    //
    private String name;
    private boolean isDefault;
    private String description;
    private List<Power> powers;
    private Phase phase;
    private List<InitialState> istate;
    private List<SupplyCenter> supplyCenters;
    private List<ProvinceData> provinceData;
    private int vcNumSCForVictory;
    private int vcMaxYearsNoSCChange;
    private int vcMaxGameTimeYears;
    private List<MapGraphic> mapGraphics;
    private float version;
    private List<NameValuePair> roNVPs;
    private List<BorderData> borderData;
    private boolean allowBCYears;
    private List<String> aliases = Collections.emptyList();

    /**
     * Class of Rule Option name/value pairs
     */
    public static class NameValuePair {
        private final Pair<String, String> pair;

        /**
         * Create a NameValuePair. Neither name or value may be null.
         */
        public NameValuePair(final String name, final String value) {
            if (name == null || value == null) {
                throw new IllegalArgumentException();
            }
            pair = new Pair<>(name, value);
        }// NameValuePair()

        /**
         * Return the Name
         */
        public String getName() {
            return pair.getKey();
        }

        /**
         * Return the Value
         */
        public String getValue() {
            return pair.getValue();
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
        return phase;
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
        return vcNumSCForVictory;
    }

    /**
     * Victory Conditions: Maximum years without a supply-center ownership change before game ends.
     */
    public int getMaxYearsNoSCChange() {
        return vcMaxYearsNoSCChange;
    }

    /**
     * Victory Conditions: Maximum game duration, in years.
     */
    public int getMaxGameTimeYears() {
        return vcMaxGameTimeYears;
    }

    /**
     * The mapGraphics associated with this Variant.
     */
    public MapGraphic[] getMapGraphics() {
        return mapGraphics.toArray(new MapGraphic[mapGraphics.size()]);
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
        return allowBCYears;
    }


    /**
     * Set the variant name.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Set the alises. Null is not allowed.
     */
    public void setAliases(final String[] aliases) {
        if (aliases == null) {
            throw new IllegalArgumentException();
        }
        this.aliases = Arrays.asList(aliases);
    }

    /**
     * Set the version of this variant
     */
    public void setVersion(final float value) {
        version = value;
    }

    /**
     * Set if this variant is the default variant.
     */
    public void setDefault(final boolean value) {
        isDefault = value;
    }

    /**
     * Set the description for this variant.
     */
    public void setDescription(final String value) {
        description = value;
    }

    /**
     * Set the starting phase for this variant.
     */
    public void setStartingPhase(final Phase value) {
        phase = value;
    }

    /**
     * Victory Conditions: Number of Supply Centers required for victory.
     */
    public void setNumSCForVictory(final int value) {
        vcNumSCForVictory = value;
    }

    /**
     * Victory Conditions: Maximum years without a supply-center ownership change before game ends.
     */
    public void setMaxYearsNoSCChange(final int value) {
        vcMaxYearsNoSCChange = value;
    }

    /**
     * Victory Conditions: Maximum game duration, in years.
     */
    public void setMaxGameTimeYears(final int value) {
        vcMaxGameTimeYears = value;
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
        allowBCYears = value;
    }


    /**
     * Sets the MapGraphics, from a List
     */
    public void setMapGraphics(final List mgList) {
        mapGraphics = new ArrayList<>(mgList);
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
        if (mapGraphics != null) {
            return mapGraphics.stream()
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

        if (mapGraphics != null && mapGraphics.size() > 0) {
            mg = mapGraphics.stream().filter(MapGraphic::isDefault).findFirst()
                    .orElse(mapGraphics.get(0));
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
        args.add(String.valueOf(vcNumSCForVictory));
        if (phase == null) {
            args.add("{bad phase}");
            args.add("{bad phase}");
            args.add("{bad phase}");
        } else {
            args.add(phase.getSeasonType());
            args.add(phase.getYearType());
            args.add(phase.getPhaseType());
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
        if (phase != null) {
            // cheap...
            variant.phase = Phase.parse(phase.toString());
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
        mapGraphics.stream().forEach(System.out::println);
        return String
                .join("", getClass().getName(), "[", "name=", name.toString(),
                        ",isDefault=", Boolean.toString(isDefault), "powers=",
                        powers.stream().map(power -> power.toString())
                                .collect(Collectors.joining(",")), ",phase=",
                        phase.toString(), ",istate=", ",supplyCenters=",
                        ",provinceData=", "mapGraphics=", ",vcNumSCForVictory=",
                        Integer.toString(vcNumSCForVictory),
                        ",vcMaxGameTimeYears=",
                        Integer.toString(vcMaxGameTimeYears),
                        ",vcMaxYearsNoSCChange=",
                        Integer.toString(vcMaxYearsNoSCChange), ",version=",
                        Float.toString(version), "]");
    }// toString()
}// class Variant


