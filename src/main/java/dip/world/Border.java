//
//  @(#)Border.java		10/2002
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

import dip.misc.Utils;
import dip.order.Order;
import dip.world.Phase.PhaseType;
import dip.world.Phase.SeasonType;
import dip.world.Unit.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A Border limits movement or support between 2 provinces.
 * <p>
 * A Border object is immutable.
 * <p>
 * The DTD for a Border object is:<br>
 * <code>
 * &lt;!ATTLIST BORDER	<br>
 * id ID #REQUIRED<br>
 * description CDATA #REQUIRED<br>
 * from CDATA #IMPLIED<br>
 * unitTypes CDATA #IMPLIED<br>
 * orderTypes CDATA #IMPLIED<br>
 * year CDATA #IMPLIED<br>
 * season CDATA #IMPLIED<br>
 * phase CDATA #IMPLIED<br>
 * baseMoveModifier CDATA #IMPLIED<br>
 * &gt;
 * </code>
 * <p>
 * Therefore, all fields are optional except for "id" and "description".
 * If a field is not specified, it is assumed to apply to all types.
 * Therefore: a border with a unitType of "Army" and year of "1900, 2000" would
 * prohibit Armies from passing during the 1900 to 2000 years. However, if the
 * unitType was omitted, no unit could pass (Army, Wing, or Fleet) from 1900 to
 * 2000 years.
 * <p>
 * The exception to this is the "from" field. If a "from" field is present,
 * the other criteria apply ONLY if "from" matches.
 * <p>
 * All specified items (except baseMoveModifier and from), thus unitTypes/
 * orderTypes/year/season/phase) must match for the Border to prohibit
 * crossing.
 * <p>
 * Borders apply to ANY crossing; this includes Movement as well as Support.
 * <p>
 * <b>Field Values:</b>
 * <ul>
 * <li><b>id: </b>The unique ID that identified this Border. These
 * IDs are used in subsequent PROVINCE definitions. Therefore,
 * a Border can be used multiple times.</li>
 * <li><b>description: </b>If the border prohibits movement, this
 * text message is displayed.</li>
 * <li><b>from: </b>The locations from which units are coming that this
 * applies to. Optional. See above.</li>
 * <li><b>unitTypes: </b> The unit types to which this applies.
 * Optional. </li>
 * <li><b>orderTypes: </b> The order types (e.g., dip.order.Move) to which
 * this applies. Optional. </li>
 * <li><b>year: </b> The years for which this applies. TWO year values
 * must be specified; the first is the minimum, the second is the maximum.
 * Alternatively, the phrase "odd" (for odd years) or "even" (for even
 * years) may be used. Both minimum and maximum are inclusive.
 * Thus to specify a single year: "2000, 2000"; a range: "1900, 2000";
 * even years: "even". Optional. </li>
 * <li><b>season: </b> The seasons (e.g., "Fall", "Spring") to
 * which this applies. Optional. </li>
 * <li><b>phase: </b>The phases (e.g., "Movement", "Retreat") to
 * to which this applies. Note that the Adjustment phase is not
 * allowed, as adjustments (adding/removing units) do not ocucr
 * via borders. Optional. </li>
 * <li><b>baseMoveModifier: </b> An optional modifier of Move strength.
 * This can be positive or negative. If not specified, it is assumed
 * to be 0. If <b>from</b> is specified, this will only apply to
 * the specified locations. Optional. </li>
 * </ul>
 */
public class Border implements Serializable {
    /**
     * Constant indicating year was omitted
     */
    private static final int YEAR_NOT_SPECIFIED = 0;
    /**
     * Constant indicating year is ranged
     */
    private static final int YEAR_SPECIFIED = 1;
    /**
     * Constant indicating that the transit allowed only during odd years
     */
    private static final int YEAR_ODD = 2;
    /**
     * Constant indicating that the transit allowed only during even years
     */
    private static final int YEAR_EVEN = 3;


    private static final String TOK_YEAR_ODD = "odd";
    private static final String TOK_YEAR_EVEN = "even";


    // instance fields
    private final List<Location> from;        // location(s) from which this transit limit applies;
    // if null, applies to all 'from' locations.
    // may specify coasts; if coast not defined, any coast used

    private final List<SeasonType> seasons;    // if null, applies to all seasons
    private final List<PhaseType> phases;        // if null, applies to all phases
    private final List<Type> unitTypes;    // if null, applies to all unit types
    private final String description; // description
    private final List<Class<? extends Order>> orderClasses;    // if null, applies to all order types
    private int yearMin;
    private int yearMax;
    private int yearModifier = YEAR_NOT_SPECIFIED;    // if not specified, this is the result

    // not determinants in canTransit()
    private final int baseMoveModifier;    // support modifier (defaults to 0)
    private final String id;            // identifying name

    /**
     * Constructor. The String arguments are parsed; if they are not valid,
     * an InvalidBorderException will be thrown. It is not recommended that
     * null arguments are given. Instead, use empty strings or public constants
     * where appropriate.
     * <p>
     * The from Locations may be null, if that field is empty.
     *
     * @throws InvalidBorderException   if any arguments are invalid.
     * @throws IllegalArgumentException if id, description, or prohibited is null
     */
    public Border(final String id, final String description, final String units,
                  final Location[] from, final String orders,
                  final String baseMoveModifier, final String season,
                  final String phase,
                  final String year) throws InvalidBorderException {
        if (id == null || description == null || units == null || from == null || orders == null || season == null || phase == null || year == null) {
            throw new IllegalArgumentException("null argument");
        }

        // set id. This is used by error messages, so must be set early.
        this.id = id;

        // parse allowed orderClasses via order classes; must specify package [case sensitive]
        // e.g.: dip.order.Move
        // these may be separated by spaces or commas (or both)
        orderClasses = parseOrders(orders);

        // parse unitTypes; must specify package [case sensitive]
        // e.g.: ARMY; must be a declared unit constant in dip.world.Unit
        unitTypes = parseUnitTypes(units);

        seasons = parseProhibitedSeasons(season);
        phases = parseProhibitedPhases(phase);
        parseYear(year);


        this.baseMoveModifier = parseBaseMoveModifier(baseMoveModifier);

        // fields we don't need to parse
        this.from = Arrays.asList(from);
        this.description = description;

    }// Border()


    /**
     * Parses the prohibited SeasonTypes (uses Phase.SeasonTypes.parse())
     */
    private List<SeasonType> parseProhibitedSeasons(
            final String in) throws InvalidBorderException {

        final List<SeasonType> list = new ArrayList<>();
        for (final String st : in.split("[, ]+")) {
            final String tok = st.trim();
            final SeasonType season = SeasonType.parse(tok).orElse(null);
            if (season == null) {
                throw new InvalidBorderException(
                        "Border " + id + ": season \"" + tok + "\" is not recognized.");
            }
            list.add(season);
        }
        return list.isEmpty() ? null : list;
    }// parseProhibitedSeasons()

    /**
     * Parses the prohibited PhaseTypes (uses Phase.PhaseType.parse())
     */
    private List<PhaseType> parseProhibitedPhases(
            final String in) throws InvalidBorderException {
        final List<PhaseType> list = new ArrayList<>();
        for (final String st : in.split("[, ]+")) {
            final String tok = st.trim();
            final PhaseType phase = PhaseType.parse(tok).orElse(null);
            if (phase == null || PhaseType.ADJUSTMENT.equals(phase)) {
                throw new InvalidBorderException(
                        "Border " + id + ": phase \"" + tok + "\" is not allowed or recognized.");
            }

            list.add(phase);
        }

        return list.isEmpty() ? null : list;
    }// parseProhibitedPhases()


    /**
     * Parses the year value (integer)
     * Expecting:
     * ####, ####	(min/max)
     * odd
     * even
     */

    private void parseYear(final String in) throws InvalidBorderException {
        if (in == null) {
            throw new IllegalArgumentException();
        }

        yearMin = Integer.MIN_VALUE;
        yearMax = Integer.MAX_VALUE;
        yearModifier = YEAR_SPECIFIED;

        // empty case
        final String text = in.trim();
        if (text.isEmpty()) {
            yearModifier = YEAR_NOT_SPECIFIED;
        } else {
            final String[] tokens = in.split("[, \t]+");
            String value1 = null;
            String value2 = null;

            if (tokens.length > 0) {
                value1 = tokens[0];
            }

            if (tokens.length > 1) {
                value2 = tokens[1];
            }

            if (tokens.length > 2 || value1 == null) {
                throw new InvalidBorderException(
                        Utils.getLocalString("Border.error.badyear", id,
                                "Too few / too many year tokens."));
            }

            if (TOK_YEAR_ODD.equalsIgnoreCase(value1)) {
                yearModifier = YEAR_ODD;
                if (value2 != null) {
                    throw new InvalidBorderException(
                            Utils.getLocalString("Border.error.badyear", id,
                                    "Cannot specify even/odd + year"));
                }
            } else if (TOK_YEAR_EVEN.equalsIgnoreCase(value1)) {
                yearModifier = YEAR_EVEN;
                if (value2 != null) {
                    throw new InvalidBorderException(
                            Utils.getLocalString("Border.error.badyear", id,
                                    "Cannot specify even/odd + year"));
                }
            } else {
                try {
                    yearMin = Integer.parseInt(value1);
                    yearMax = Integer.parseInt(value2);

                    if (yearMin > yearMax) {
                        throw new NumberFormatException();
                    }
                } catch (final NumberFormatException ignored) {
                    throw new InvalidBorderException(
                            Utils.getLocalString("Border.error.badyear", id,
                                    "Minimum and Maximum year values not specified or illegal."));
                }
            }
        }
    }// parseYear()


    /**
     * Parses the unit types
     */
    private List<Type> parseUnitTypes(final String in) {
        final List<Type> list = Arrays.stream(in.split("[, ]+"))
                .map(Type::parse).collect(Collectors.toList());
        return list.isEmpty() ? null : list;
    }// parseUnitTypes()


    /**
     * Parses the order types
     */
    private List<Class<? extends Order>> parseOrders(
            final String in) throws InvalidBorderException {
        final List<Class<? extends Order>> classes = parseClasses2Objs(in,
                "dip.order.Order");
        return classes.isEmpty() ? null : classes;
    }// parseOrders()


    /**
     * Internal parser helper method
     */
    private List<Class<? extends Order>> parseClasses2Objs(final String in,
                                                           final String superClassName) throws InvalidBorderException {
        Class<? extends Order> superClass = null;
        try {
            superClass = Class.forName(superClassName).asSubclass(Order.class);
        } catch (final ClassNotFoundException e) {
            throw new InvalidBorderException(
                    Utils.getLocalString("Border.error.internal",
                            "parseClasses2Objs()", e.getMessage()));
        }

        final List<Class<? extends Order>> list = new ArrayList<>(10);
        for (final String st : in.split("[, ]+")) {
            Class<? extends Order> cls;

            try {
                cls = Class.forName(st).asSubclass(Order.class);
            } catch (final ClassNotFoundException ignored) {
                throw new InvalidBorderException(
                        Utils.getLocalString("Border.error.badclass", id, st));
            }

            if (!superClass.isAssignableFrom(cls)) {
                throw new InvalidBorderException(
                        Utils.getLocalString("Border.error.badderivation", id,
                                cls.getName(), superClass.getName()));
            }

            list.add(cls);
        }

        return list;
    }// parseClasses2Objs()


    /**
     * Parses the base move modifier. If string is empty, defaults to 0.
     * The format is just a positive or negative (or 0) integer.
     */
    private int parseBaseMoveModifier(
            final String in) throws InvalidBorderException {
        final String in1 = in.trim();
        try {
            return in1.isEmpty() ? 0 : Integer.parseInt(in1);
        } catch (final NumberFormatException ignored) {
            throw new InvalidBorderException(
                    Utils.getLocalString("Border.error.badmovemod", id, in1));
        }

    }// parseBaseMoveModifier()


    /**
     * Determines if a unit can transit from a location to this location.
     * <p>
     * Convenience method for more verbose canTransit() method. No arguments may
     * be null.
     */
    public boolean canTransit(final Phase phase, final Order order) {
        return canTransit(order.getSource(), order.getSourceUnitType(), phase,
                order.getClass());
    }// canTransit()


    /**
     * Determines if a unit can transit from a location to this location.
     * <p>
     * All defined border attributes have to match to prohibit border transit.
     * <p>
     * Null arguments are not permitted.
     */
    public boolean canTransit(final Location fromLoc, final Type unit,
                              final Phase phase, final Class orderClass) {
        /*
        System.out.println("border: "+id);
		System.out.println("  "+fromLoc.getProvince()+":"+fromLoc.getCoast()+", "+phase);
		*/

        // check from
        int nResults = 0;
        int failResults = 0;
        boolean fromMatched = from.stream()
                .anyMatch(aFrom -> aFrom.equalsLoosely(fromLoc));

        // we only apply criteria if 'from' was not specified, or
        // from was specified, and it matches.
        if (fromMatched) {
            // check unit type
            if (unitTypes != null) {
                nResults++;
                failResults += unitTypes.stream()
                        .anyMatch(unitType -> unitType == unit) ? 1 : 0;
            }

            // check order
            if (orderClasses != null) {
                nResults++;
                failResults += orderClasses.stream().anyMatch(
                        orderClass1 -> Objects
                                .equals(orderClass, orderClass1)) ? 1 : 0;
            }

            // check phase (season, phase, and year)
            if (seasons != null) {
                nResults++;
                failResults += seasons.stream().anyMatch(
                        season -> phase.getSeasonType().equals(season)) ? 1 : 0;
            }

            if (phases != null) {
                nResults++;
                failResults += phases.stream().anyMatch(
                        phase1 -> phase.getPhaseType().equals(phase1)) ? 1 : 0;
            }

            // we always check the year
            if (yearModifier != YEAR_NOT_SPECIFIED) {
                nResults++;
                final int theYear = phase.getYear();
                switch (yearModifier) {
                    case YEAR_ODD:
                        failResults += (theYear & 1) == 1 ? 1 : 0;
                        break;
                    case YEAR_EVEN:
                        failResults += (theYear & 1) == 1 ? 0 : 1;
                        break;
                    default:
                        failResults += yearMin <= theYear && theYear <= yearMax ? 1 : 0;
                        break;
                }
            }
        }

		/*
        System.out.println("  fromMatched: "+fromMatched);
		System.out.println("  nResults: "+nResults);
		System.out.println("  failResults: "+failResults);
		*/

        // only return 'false' if EVERYTHING has failed, or,
        // nothing was tested
        assert failResults <= nResults;
        return failResults < nResults || nResults == 0;
    }// canTransit()


    /**
     * Gets the base move modifier. Requires a non-null from location.
     */
    public int getBaseMoveModifier(final Location moveFrom) {
        // if not from the given location, no change in support.
        return from == null ? baseMoveModifier : from.stream()
                .filter(aFrom -> aFrom.equalsLoosely(moveFrom))
                .map(aFrom -> baseMoveModifier).findFirst().orElse(0);
    }// getBaseMoveModifier()


    /**
     * Returns the description
     */
    public String getDescription() {
        return description;
    }// getDescription()

}// class Border
