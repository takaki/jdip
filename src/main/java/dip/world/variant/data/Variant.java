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
import dip.world.variant.parser.XMLVariantParser.AdjCache;
import org.xml.sax.SAXException;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A Variant.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "VARIANT")
public final class Variant implements Cloneable, Comparable<Variant> {
    // the arrays in general should not be null. They are defined as null initially
    // to make it more apparent should a field not be initialized properly.
    //
    private String name = "";
    @XmlAttribute(name = "default", required = true)
    private boolean isDefault;

    @XmlAttribute(name = "version", required = true)
    private String version;

    private List<String> aliases = Collections.emptyList();

    private String description;
    @XmlElement(name = "VICTORYCONDITIONS", required = true)
    public VictoryConditions victoryConditions = new VictoryConditions();

    @XmlElement(name = "POWER")
    private List<Power> powers;

    @XmlElement(name = "SUPPLYCENTER")
    private List<SupplyCenter> supplyCenters;
    @XmlElement(name = "INITIALSTATE")
    private List<InitialState> istate;

    @XmlElement(name = "MAP")
    private Map map = new Map();

    @XmlElementWrapper(name = "RULEOPTIONS")
    @XmlElement(name = "RULEOPTION")
    private List<NameValuePair> roNVPs;

    @XmlElement(name = "STARTINGTIME", required = true)
    private StartingTime startingTime = new StartingTime();

    private URL baseURL;

    public void setBaseURL(final URL baseURL) {
        this.baseURL = baseURL;
    }

    public static class Map {
        @XmlAttribute(name = "adjacencyURI")
        private URI adjacencyURI;
        @XmlElement(name = "MAP_GRAPHIC")
        private List<MapGraphic> mapGraphics;
    }

    @XmlRootElement
    public static class StartingTime {
        private Phase phase;
        @XmlAttribute(name = "allowBCYears")
        private boolean allowBCYears;

        @XmlAttribute(name = "turn")
        public void setPhase(final String val) {
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

    void afterUnmarshal(final Unmarshaller unmarshaller,
                        final Object parent) throws IOException, SAXException {
        // TODO: Remove this
    }


    /**
     * Class of Rule Option name/value pairs
     */
    @XmlRootElement
    public static class NameValuePair {
        @XmlAttribute
        private String name;
        @XmlAttribute
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
    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

    /**
     * Version of this variant
     */
    public VersionNumber getVersion() {
        return VersionNumber.parse(version);
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
    public List<InitialState> getInitialStates() {
        return Collections.unmodifiableList(istate);
    }

    /**
     * Returns Powers associated with this Variant.
     */
    public List<Power> getPowers() {
        return Collections.unmodifiableList(powers);
    }

    /**
     * Returns SupplyCenter objects
     */
    public List<SupplyCenter> getSupplyCenters() {
        return Collections.unmodifiableList(supplyCenters);
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
    public List<MapGraphic> getMapGraphics() {
        return map.mapGraphics;
    }

    /**
     * The ProvinceData associated with this Variant
     */
    public List<ProvinceData> getProvinceData() {
        try {
            final URL url = new URL(baseURL, map.adjacencyURI.toString());
            return AdjCache.getProvinceData(url);// TODO: remove AdjCache
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }

    }

    /**
     * The RuleOptions (as name-value pairs) associated with this Variant
     */
    public List<NameValuePair> getRuleOptionNVPs() {
        return Collections.unmodifiableList(roNVPs);
    }

    /**
     * Gets the BorderData associated with this Variant
     */
    public List<BorderData> getBorderData() {
        try {
            final URL url = new URL(baseURL, map.adjacencyURI.toString());
            return AdjCache.getBorderData(url); // TODO: remove AdjCache
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
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
    @XmlAttribute(name = "name", required = true)
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

    @XmlAttribute(name = "aliases", required = true)
    public void setAliases(final String csv) {
        final String[] aliases = Utils.parseCSV(csv);
        setAliases(aliases);
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
    @XmlElement(name = "DESCRIPTION")
    public void setDescription(final String value) {
        description = value;
    }

    /**
     * Set the starting phase for this variant.
     */
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
     * Sets whether BC years (negative years) are allowed
     */
    public void setBCYearsAllowed(final boolean value) {
        startingTime.allowBCYears = value;
    }


    /**
     * Sets the Powers, from a List
     */
    public void setPowers(final List<Power> powerList) {
        powers = new ArrayList<>(powerList);
    }// setPowers()


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
    public Optional<MapGraphic> getMapGrapic(final String mgName) {
        if (map.mapGraphics != null) {
            return map.mapGraphics.stream()
                    .filter(mapGraphic -> mapGraphic.getName()
                            .equalsIgnoreCase(mgName)).findFirst();
        }
        return null;
    }// getVariant()


    /**
     * Gets the default MapGraphic; if there is no default, returns the first one.
     */
    public MapGraphic getDefaultMapGraphic() {
        return map.mapGraphics != null ? map.mapGraphics.stream()
                .filter(MapGraphic::isDefault).findFirst()
                .orElse(map.mapGraphics.get(0)) : null;

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
    public Collection<Object> getHTMLSummaryArguments() {
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

        return args;
    }// getHTMLSummaryArguments()


    /**
     * Creates a deep clone of all data EXCEPT InitialState / SupplyCenter data / Name / Description
     */
    @Override
    public Variant clone() throws CloneNotSupportedException {
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
        return String
                .join("", getClass().getName(), "[", "name=", name.toString(),
                        ",isDefault=", Boolean.toString(isDefault), "powers=",
                        powers.stream().map(Power::toString)
                                .collect(Collectors.joining(",")), ",,phase=",
                        startingTime.phase.toString(), ",istate=",
                        ",supplyCenters=", ",provinceData=", "mapGraphics=",
                        ",vcNumSCForVictory=", Integer.toString(
                                victoryConditions.vcNumSCForVictory.value),
                        ",vcMaxGameTimeYears=", Integer.toString(
                                victoryConditions.vcMaxGameTimeYears.value),
                        ",vcMaxYearsNoSCChange=", Integer.toString(
                                victoryConditions.vcMaxYearsNoSCChange.value),
                        ",version=", version, "]");
    }// toString()
}// class Variant


