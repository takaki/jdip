//
//  @(#)Phase.java		4/2002
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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A Phase object represents when a turn takes place, and contains the
 * year, game phase (PhaseType), and Season information.
 * <p>
 * Phase objects are mutable and comparable.
 * <p>
 * PhaseType and SeasonType objects may be compared with referential equality.
 * (For example, "Phase.getSeasonType() == SeasonType.SPRING")
 */
public class Phase implements Serializable, Comparable<Phase> {
    // internal constants: describes ordering of phases
    // Setup is independent of this ordering.
    // ordering: (for a given year)
    //		spring movement, spring retreat, fall movement, fall retreat, fall adjustment
    // both these constants correspond, and must have equal sizes
    private static final List<SeasonType> ORDER_SEASON = Arrays
            .asList(SeasonType.SPRING, SeasonType.SPRING, SeasonType.FALL,
                    SeasonType.FALL, SeasonType.FALL);

    private static final List<PhaseType> ORDER_PHASE = Arrays
            .asList(PhaseType.MOVEMENT, PhaseType.RETREAT, PhaseType.MOVEMENT,
                    PhaseType.RETREAT, PhaseType.ADJUSTMENT);

    // formatter to always 4-digit format a year
    private static final DecimalFormat YEAR_FORMAT = new DecimalFormat("0000");


    // instance variables
    protected final SeasonType seasonType;
    protected final YearType yearType;
    protected final PhaseType phaseType;
    private transient int orderIdx;                // set by readResolve() when object de-serialized


    /**
     * Create a new Phase.
     */
    public Phase(final SeasonType seasonType, final int year,
                 final PhaseType phaseType) {
        this(seasonType, new YearType(year), phaseType);
    }// Phase()


    /**
     * Create a new Phase.
     */
    public Phase(final SeasonType seasonType, final YearType yearType,
                 final PhaseType phaseType) {
        Objects.requireNonNull(seasonType);
        Objects.requireNonNull(yearType);
        Objects.requireNonNull(phaseType);

        orderIdx = deriveOrderIdx(seasonType, phaseType);
        if (orderIdx == -1) {
            throw new IllegalArgumentException(
                    "invalid seasontype/phasetype combination");
        }

        this.seasonType = seasonType;
        this.yearType = yearType;
        this.phaseType = phaseType;
    }// Phase()

    /**
     * Create a new Phase, given a known index
     */
    protected Phase(final YearType yt, final int idx) {
        orderIdx = idx;
        yearType = yt;
        phaseType = ORDER_PHASE.get(idx);
        seasonType = ORDER_SEASON.get(idx);
    }// Phase()


    /**
     * Returns the year
     */
    public int getYear() {
        return yearType.getYear();
    }

    /**
     * Returns the YearType
     */
    public YearType getYearType() {
        return yearType;
    }

    /**
     * Returns the PhaseType
     */
    public PhaseType getPhaseType() {
        return phaseType;
    }

    /**
     * Returns the SeasonType
     */
    public SeasonType getSeasonType() {
        return seasonType;
    }

    /**
     * Displays as a short String (e.g., F1902R)
     */
    public String getBriefName() {
        return String.join("", seasonType.getBriefName(),
                YEAR_FORMAT.format(yearType.getYear()),
                phaseType.getBriefName());
    }// getBriefName()

    /**
     * Displays the phase as a String
     */
    @Override
    public String toString() {
        return String
                .join("", String.valueOf(seasonType), ", ", yearType.toString(),
                        " (", phaseType.toString(), ")");
    }// toString()

    /**
     * Returns true if the two phases are equivalent.
     */
    @Override
    public boolean equals(final Object obj) {
        final Phase phase = (Phase) obj;
        return yearType.equals(phase.yearType) && seasonType
                .equals(phase.seasonType) && phaseType.equals(phase.phaseType);
    }// equals()

    /**
     * Compares the Phase to the given Phase object. Returns a negative, zero, or
     * positive integer depending if the given Phase is less than, equal, or
     * greater than (temporally) to this Phase.
     */
    @Override
    public int compareTo(final Phase obj) {
        final Phase phase = obj;
        int result = 0;

        // year is dominant
        result = yearType.compareTo(phase.yearType);
        if (result != 0) {
            return result;
        }

        // then season
        result = seasonType.compareTo(phase.seasonType);
        if (result != 0) {
            return result;
        }

        // finally, phase type.
        return phaseType.compareTo(phase.phaseType);
    }// compareTo()


    /**
     * Get the phase that would be after to the current phase
     */
    public Phase getNext() {
        // advance the phase index by one, UNLESS we are over; then
        // advance the year and reset.
        final int idx = (orderIdx + 1) % ORDER_SEASON.size();
        final YearType yt = idx == 0 ? yearType.getNext() : yearType;

        return new Phase(yt, idx);
    }// getNext()


    /**
     * Get the phase that would come before the current phase
     */
    public Phase getPrevious() {
        final int idx = orderIdx - 1;
        return new Phase(idx < 0 ? yearType.getPrevious() : yearType,
                Math.floorMod(idx, ORDER_SEASON.size()));
    }// getPrevious()


    /**
     * given season/phase, derive the order index. If we cannot, our index is -1.
     */
    private int deriveOrderIdx(final SeasonType st, final PhaseType pt) {
        return IntStream.range(0, ORDER_SEASON.size())
                .filter(i -> ORDER_SEASON.get(i) == st && ORDER_PHASE.get(i) == pt)
                .findFirst().orElse(-1);

    }// deriveOrderIdx()


    /**
     * Determines if this phase is valid. Not all PhaseType and
     * SeasonType combinations are valid.
     */
    public static boolean isValid(final SeasonType st, final PhaseType pt) {
        return IntStream.range(0, ORDER_SEASON.size())
                .anyMatch(i -> ORDER_SEASON.get(i) == st && ORDER_PHASE.get(i) == pt);
    }// isValid()


    /**
     * Determines the Phase from a String.
     * <p>
     * Expects input in the following form(s):
     * <p>
     * Season, Year (Phase)<br>
     * Season, Year [Phase]<br>
     * SYYYYP  as a single 6-character token, e.g., F1900M = Fall 1900, Movement<br>
     * <p>
     * Whitespace: space, comma, colon, semicolon, [], (), tab, newline, return, quotes
     * <p>
     * The order is not important. If the combination is not valid (via isValid()), or if
     * any Phase component cannot be parsed, a null value is returned. Note that this is very
     * forgiving, but it does not allow any non-word tokens between what we look for.
     */
    public static Optional<Phase> parse(final String in) {
        // special case: 6 char token (commonly seen in Judge input)
        // 'bc' years aren't allowed in 6 char tokens.
        if (in.length() == 6) {
            // parse season & phase
            final Optional<SeasonType> seasonType = SeasonType
                    .parse(in.substring(0, 1));
            final Optional<YearType> yearType = YearType
                    .parse(in.substring(1, 5));
            final Optional<PhaseType> phaseType = PhaseType
                    .parse(in.substring(5, 6));
            return seasonType.flatMap(season -> yearType.flatMap(
                    year -> phaseType.flatMap(phase -> Optional
                            .of(new Phase(season, year, phase)))));
        } else {
            // case conversion
            final String lcIn = in.toLowerCase();

            // our token list (should be 3 or 4; whitespace/punctuation is ignored)

            // get all tokens, ignoring ANY whitespace or punctuation; StringTokenizer is ideal for this
            final Collection<String> tokList = Arrays
                    .asList(lcIn.split("[ ,:;\\[\\](){}\\-_|/\\\"\'\t\n\r]+"));

            // not enough tokens (we need at least 3)
            if (tokList.size() < 3) {
                return Optional.empty();
            }

            // parse until we run out of things to parse
            final Optional<SeasonType> seasonType = tokList.stream()
                    .map(SeasonType::parse).flatMap(
                            season -> season.map(Stream::of)
                                    .orElse(Stream.empty())).findFirst();
            final Optional<YearType> yearType = tokList.stream()
                    .map(YearType::parse).flatMap(
                            year -> year.map(Stream::of).orElse(Stream.empty()))
                    .findFirst();
            final Optional<PhaseType> phaseType = tokList.stream()
                    .map(PhaseType::parse).flatMap(
                            phase -> phase.map(Stream::of)
                                    .orElse(Stream.empty())).findFirst();

            return seasonType.flatMap(season -> yearType
                    .flatMap(year -> phaseType.flatMap(phase -> {
                        // check season-phase validity
                        if (!isValid(season, phase)) {
                            return Optional.empty();
                        }
                        // 'bc' token may be 'loose'. If so, we need to find it, as the
                        // YearType parser was fed only a single token (no whitespace)
                        // e.g., "1083 BC" won't be parsed right, but "1083bc" will be.
                        final YearType yt = (lcIn.contains("bc") || lcIn
                                .contains("b.c.")) && year
                                .getYear() > 0 ? new YearType(
                                -year.getYear()) : year;

                        return Optional.of(new Phase(season, yt, phase));
                    })));
        }

    }// parse()


    /**
     * Returns a String array, in order, of valid season/phase combinations.
     * <p>
     * E.g.: Spring Move, or Spring Adjustment, etc.
     */
    public static List<String> getAllSeasonPhaseCombos() {
        return IntStream.range(0, ORDER_SEASON.size()).mapToObj(i -> String
                .join(" ", ORDER_SEASON.get(i).toString(),
                        ORDER_PHASE.get(i).toString()))
                .collect(Collectors.toList());
    }// getAllSeasonPhaseCombos()


    /**
     * Reconstitute a Phase object
     */
    protected Object readResolve() throws ObjectStreamException {
        orderIdx = deriveOrderIdx(seasonType, phaseType);
        return this;
    }// readResolve()


    /**
     * Represents seasons
     * <p>
     * SeasonType constants should be used, rather than creating new SeasonType objects.
     */
    public enum SeasonType {
        // Season Type Constants
        /**
         * Spring season
         */
        SPRING("SEASONTYPE_SPRING"),
        /**
         * Fall season
         */
        FALL("SEASONTYPE_FALL");

        /**
         * SeasonType array
         * <b>Warning: this should not be mutated.</b>
         */
        // always-accepted english constants for SeasonTypes
        private static final String CONST_SPRING = "SPRING";
        private static final String CONST_FALL = "FALL";
        private static final String CONST_SUMMER = "SUMMER";
        private static final String CONST_WINTER = "WINTER";

        // internal constants
        private static final String IL8N_SPRING = "SEASONTYPE_SPRING";
        private static final String IL8N_FALL = "SEASONTYPE_FALL";


        private final transient String displayName;


        /**
         * Creates a new SeasonType
         */
        SeasonType(final String il8nKey) {
            displayName = Utils.getLocalString(il8nKey);
        }// SeasonType()


        /**
         * Return the name of this season
         */
        @Override
        public String toString() {
            return displayName;
        }// toString()


        /**
         * Brief name of a Season (e.g., [F]all, [S]pring)
         */
        public String getBriefName() {
            switch (this) {
                case SPRING:
                    return "S";
                case FALL:
                    return "F";
            }

            return "?";
        }// getBriefName()


        /**
         * Get the next season
         */
        public SeasonType getNext() {
            switch (this) {
                case SPRING:
                    return FALL;
                case FALL:
                    return SPRING;
            }
            throw new IllegalArgumentException(
                    String.format("Unknown SeasonType: %s", this));

        }// getNext()


        /**
         * Get the previous season
         */
        public SeasonType getPrevious() {
            switch (this) {
                case SPRING:
                    return FALL;
                case FALL:
                    return SPRING;
            }
            throw new IllegalArgumentException(
                    String.format("Unknown SeasonType: %s", this));
        }// getPrevious()


        /**
         * Parse input to determine season; return null if input cannot be parsed
         * into a known SeasonType constant.
         * <p>
         * Note: SUMMER and WINTER are converted to Spring and Fall, respectively.
         */
        public static Optional<SeasonType> parse(final String in) {
            // short cases (1 letter); not i18n'd
            if (in.length() == 1) {
                switch (in.toLowerCase()) {
                    case "s":
                        return Optional.of(SPRING);
                    case "f":
                    case "w":
                        return Optional.of(FALL);
                    default:
                        return Optional.empty();
                }
            }

            // typical cases
            switch (in.toUpperCase()) {
                case CONST_SPRING:
                    return Optional.of(SPRING);
                case CONST_FALL:
                    return Optional.of(FALL);
                case CONST_SUMMER:
                    return Optional.of(SPRING);
                case CONST_WINTER:
                    return Optional.of(FALL);
                default:
                    break;
            }

            // il8n cases
            if (in.equalsIgnoreCase(Utils.getLocalString(IL8N_SPRING))) {
                return Optional.of(SPRING);
            }
            if (in.equalsIgnoreCase(Utils.getLocalString(IL8N_FALL))) {
                return Optional.of(FALL);
            }

            return Optional.empty();
        }// parse()


    }// nested class SeasonType


    /**
     * PhaseTypes represent game phases. For example, MOVEMENT or RETREAT phases.
     * <p>
     * PhaseType constants should be used instead of creating new PhaseType objects.
     */
    public enum PhaseType {
        // PhaseType Constants
        /**
         * Adjustment PhaseType
         */
        ADJUSTMENT("adjustment", "PHASETYPE_ADJUSTMENT"),
        /**
         * Movement PhaseType
         */
        MOVEMENT("movement", "PHASETYPE_MOVEMENT"),
        /**
         * Retreat PhaseType
         */
        RETREAT("retreat", "PHASETYPE_RETREAT");

        // always-accepted english constants for phase types
        // these MUST be in lower case
        private static final String CONST_ADJUSTMENT = "adjustment";
        private static final String CONST_MOVEMENT = "movement";
        private static final String CONST_RETREAT = "retreat";

        // internal constants
        private static final String IL8N_ADJUSTMENT = "PHASETYPE_ADJUSTMENT";
        private static final String IL8N_MOVEMENT = "PHASETYPE_MOVEMENT";
        private static final String IL8N_RETREAT = "PHASETYPE_RETREAT";


        // instance variables
        private final String displayName;
        private final String constName;

        /**
         * Create a new PhaseType
         */
        PhaseType(final String cName, final String il8nKey) {
            constName = cName;
            displayName = Utils.getLocalString(il8nKey);
        }// PhaseType()

        /**
         * Get the name of a phase
         */
        @Override
        public String toString() {
            return displayName;
        }// toString()

        /**
         * Brief name of a Phase (e.g., [B]uild, [R]etreat [M]ove
         */
        public String getBriefName() {
            switch (this) {
                case ADJUSTMENT:
                    return "B";
                case RETREAT:
                    return "R";
                case MOVEMENT:
                    return "M";
            }

            // unknown!
            return "?";
        }// getBriefName()


        /**
         * Get the next PhaseType, in sequence.
         */
        public PhaseType getNext() {
            switch (this) {
                case ADJUSTMENT:
                    return MOVEMENT;
                case RETREAT:
                    return ADJUSTMENT;
                case MOVEMENT:
                    return RETREAT;
            }
            throw new IllegalArgumentException(
                    String.format("Unknown PhaseType: %s", this));
        }// getNext()


        /**
         * Get the previous PhaseType, in sequence.
         */
        public PhaseType getPrevious() {
            switch (this) {
                case ADJUSTMENT:
                    return RETREAT;
                case RETREAT:
                    return MOVEMENT;
                case MOVEMENT:
                    return ADJUSTMENT;
            }
            throw new IllegalArgumentException(
                    String.format("Unknown PhaseType: %s", this));
        }// getPrevious()


        /**
         * Returns the appropriate PhaseType constant representing
         * the input, or null.
         * <p>
         * Plurals are allowable on constants, but not in il8n versions.
         */
        public static Optional<PhaseType> parse(final String in) {
            // short cases (1 letter); not i18n'd
            if (in.length() == 1) {
                switch (in.toLowerCase()) {
                    case "m":
                        return Optional.of(MOVEMENT);
                    case "a":
                    case "b":
                        return Optional.of(ADJUSTMENT);
                    case "r":
                        return Optional.of(RETREAT);
                    default:
                        return Optional.empty();
                }
            }

            // typical cases; use 'startsWith'
            if (in.startsWith(CONST_ADJUSTMENT)) {
                return Optional.of(ADJUSTMENT);
            }
            if (in.startsWith(CONST_MOVEMENT)) {
                return Optional.of(MOVEMENT);
            }
            if (in.startsWith(CONST_RETREAT)) {
                return Optional.of(RETREAT);
            }

            // il8n cases
            if (in.equalsIgnoreCase(Utils.getLocalString(IL8N_ADJUSTMENT))) {
                return Optional.of(ADJUSTMENT);
            }
            if (in.equalsIgnoreCase(Utils.getLocalString(IL8N_MOVEMENT))) {
                return Optional.of(MOVEMENT);
            }
            if (in.equalsIgnoreCase(Utils.getLocalString(IL8N_RETREAT))) {
                return Optional.of(RETREAT);
            }

            return Optional.empty();
        }// parse()

    }// nested class PhaseType


    /**
     * YearType is used to represent the Year
     * <p>
     * A YearType is used because we now support negative years ("BC")
     * and need to appropriately advance, parse, and format these years.
     * <p>
     * A YearType is an immutable object.
     */
    public static class YearType implements Serializable, Comparable<YearType> {
        // instance fields
        protected final int year;


        /**
         * Create a new YearType
         */
        public YearType(final int value) {
            if (value == 0) {
                throw new IllegalArgumentException("Year 0 not valid");
            }

            year = value;
        }// YearType()


        /**
         * Get the name of a year.
         */
        @Override
        public String toString() {
            if (year >= 1000) {
                return Integer.toString(year);
            } else if (year > 0) {
                // explicitly add "AD"
                return String.join("", String.valueOf(year), " AD");
            } else {
                return String.join("", Integer.toString(-year), " BC");
            }
        }// toString()


        /**
         * Gets the year. This will return a negative number if it is a BC year.
         */
        public int getYear() {
            return year;
        }// getYear()

        /**
         * Returns the hashcode
         */
        @Override
        public int hashCode() {
            return year;
        }// hashCode()

        /**
         * Returns <code>true</code> if YearTYpe objects are equivalent
         */
        @Override
        public boolean equals(final Object obj) {
            return obj == this || obj instanceof YearType && year == ((YearType) obj).year;

        }// equals()

        /**
         * Temporally compares YearType objects
         */
        @Override
        public int compareTo(final YearType obj) {
            if (year > obj.year) {
                return 1;
            }
            if (year < obj.year) {
                return -1;
            }
            return 0;
        }// compareTo()


        /**
         * Get the next YearType, in sequence
         */
        public YearType getNext() {
            // 1BC -> 1AD, otherwise, just add 1
            return year == -1 ? new YearType(1) : new YearType(year + 1);
        }// getNext()


        /**
         * Get the previous YearType, in sequence.
         */
        public YearType getPrevious() {
            // 1 AD -> 1 BC, otherwise, just subtract 1
            return year == 1 ? new YearType(-1) : new YearType(year - 1);
        }// getPrevious()


        /**
         * Returns the appropriate YearType constant representing
         * the input, or null.
         * <p>
         * 0 is not a valid year<br>
         * Negative years are interpreted as BC<br>
         * The modifier "BC" following a year is valid<br>
         * A negative year with the BC modifier is still a BC year<br>
         * Periods are NOT allowed in "BC"<br>
         * The modifier BC must be in lower case<br>
         */
        public static Optional<YearType> parse(final String input) {
            String in = input;
            final int idx = in.indexOf("bc");
            boolean isBC = false;
            if (idx >= 1) {
                isBC = true;
                in = in.substring(0, idx);
            }

            int y;
            try {
                y = Integer.parseInt(in.trim());
            } catch (final NumberFormatException e) {
                return Optional.empty();
            }

            if (y == 0) {
                return Optional.empty();
            }
            if (y > 0 && isBC) {
                y = -y;
            }

            return Optional.of(new YearType(y));
        }// parse()

    }// nested class YearType

}// class Phase
