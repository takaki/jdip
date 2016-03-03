//
//  @(#)Coast.java		4/2002
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

import javax.xml.bind.annotation.XmlEnumValue;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coasts are essential to determining connectivity between Provinces.
 * <p>
 * Coast constants should be used.
 */

public enum Coast {
    // perhaps make it "?c" ??
    UNDEFINED("Undefined", "?", 0),

    /**
     * Constant indicating Wing coast (for Wing movement)
     */
    @XmlEnumValue("wx")
    WING("Wing", "wx", 1),
    /**
     * Constant indicating no coast (Army movement)
     */
    @XmlEnumValue("mv")
    NONE("None", "mv", 2),
    /**
     * Constant indicating a single Coast (for fleets in coastal land areas, or sea-only provinces)
     */
    @XmlEnumValue("xc")
    SINGLE("Single", "xc", 3),
    /**
     * Constant indicating North Coast
     */
    @XmlEnumValue("nc")
    NORTH("North Coast", "nc", 4),
    /**
     * Constant indicating South Coast
     */
    @XmlEnumValue("sc")
    SOUTH("South Coast", "sc", 5),
    /**
     * Constant indicating West Coast
     */
    @XmlEnumValue("sc")
    WEST("West Coast", "wc", 6),
    /**
     * Constant indicating East Coast
     */
    @XmlEnumValue("ec")
    EAST("East Coast", "ec", 7);


    // transient data
    private static transient List<Pattern> patterns = Arrays.asList(
            // match /xx, -xx, \xx coasts; also takes care of periods.
            // also matches /x; will not match /xxx (or -xxx)
            Pattern.compile(
                    "\\s*[\\-\\\\/](\\p{Alnum}\\.?)(\\p{Alnum}\\.?)\\b"),
            //
            // match parenthetical coasts.
            //patterns[1] = Pattern.compile("\\s*\\([^\\p{Alnum}]*(\\p{Alnum})[^\\p{Alnum}]*(\\p{Alnum})[^)]*\\)");
            Pattern.compile("\\s*\\(([.[^)]]*)(\\))\\s*"));



	/* To be used in the future .... parsing to accomodate
    private static final String NW_FULL 		= "Northwest Coast";
	private static final String NE_FULL 		= "Northeast Coast";
	private static final String SW_FULL 		= "Southwest Coast";
	private static final String SE_FULL 		= "Southeast Coast";
	private static final String NW_ABBREV		= "nw";
	private static final String NE_ABBREV		= "ne";
	private static final String SW_ABBREV		= "sw";
	private static final String SE_ABBREV		= "se";
	*/


    // constants
    /**
     * Alias for Coast.WING
     */
    public static final Coast TOUCHING = WING;
    /**
     * Alias for Coast.NONE
     */
    public static final Coast LAND = NONE;
    /**
     * Alias for Coast.SINGLE
     */
    public static final Coast SEA = SINGLE;

    // index-to-coast array
    private static final List<Coast> IDX_ARRAY = Arrays
            .asList(UNDEFINED, WING, NONE, SINGLE, NORTH, SOUTH, WEST, EAST);


    /**
     * Array of Coasts that are not typically displayed
     * <b>Warning: this should not be mutated.</b>
     */
    private static final Set<Coast> NOT_DISPLAYED = EnumSet
            .of(NONE, SINGLE, UNDEFINED, WING);

    /**
     * Array of the 6 main coast types (NONE, SINGLE, NORTH, SOUTH, WEST, EAST)
     * <b>Warning: this should not be mutated.</b>
     */
    public static final Set<Coast> ALL_COASTS = EnumSet
            .of(NONE, SINGLE, NORTH, SOUTH, WEST, EAST);
    /**
     * Array of sea coasts (SINGLE, NORTH, SOUTH, WEST, EAST)
     * <b>Warning: this should not be mutated.</b>
     */
    public static final Set<Coast> ANY_SEA = EnumSet
            .of(SINGLE, NORTH, SOUTH, WEST, EAST);
    /**
     * Array of directional coasts (NORTH, SOUTH, WEST, EAST)
     * <b>Warning: this should not be mutated.</b>
     */
    public static final Set<Coast> ANY_DIRECTIONAL = EnumSet
            .of(NORTH, SOUTH, WEST, EAST);


    // class variables
    private final String name;
    private final String abbreviation;
    private final int index;

    /**
     * Constructs a Coast
     */
    Coast(final String name, final String abbreviation, final int index) {

        if (index < 0) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.abbreviation = abbreviation;
        this.index = index;
    }// Coast()


    /**
     * Returns the full name (long name) of a coast; e.g., "North Coast"
     */
    public String getName() {
        return name;
    }// getName()

    /**
     * Returns the abbreviated coast name (e.g., "nc")
     */
    public String getAbbreviation() {
        return abbreviation;
    }// getAbbreviation()

    /**
     * Gets the index of a Coast. Indices are &gt;= 0.
     */
    public int getIndex() {
        return index;
    }// getIndex()


    /**
     * Returns the full name of the coast
     */
    public String toString() {
        return name;
    }// toString()

    /**
     * Returns if this Coast is typically displayed
     */
    public static boolean isDisplayable(final Coast coast) {
        return !NOT_DISPLAYED.contains(coast);
    }// isDisplayable()

    /**
     * Parses the coast from a text token.
     * <p>
     * Given a text token such as "spa-sc" or "spa/nc",
     * Returns the Coast constant. Coasts must begin with
     * a '/', '-', or '\'; parenthetical notation e.g., "(nc)"
     * is not supported.
     * <p>
     * This method never returns null; for nonexistent or
     * unparsable coasts, Coast.UNDEFINED is returned.
     * <p>
     */
    public static Coast parse(final String text) {
        final String input = text.toLowerCase().trim();

        // check if it is just a coast (2-letter) or
        // part of a province name. If we don't check
        // for -/\, then we could be processing part
        // of a province name
        if (input.length() >= 3) {
            final char c = input.charAt(input.length() - 3);
            if (c != '-' && c != '/' && c != '\\') {
                return UNDEFINED;
            }
        }

        if (input.endsWith("nc")) {
            return NORTH;
        } else if (input.endsWith("sc")) {
            return SOUTH;
        } else if (input.endsWith("wc")) {
            return WEST;
        } else if (input.endsWith("ec")) {
            return EAST;
        } else if (input.endsWith("xc")) {
            return SINGLE;
        } else if (input.endsWith("mv")) {
            return LAND;
        }

        return UNDEFINED;
    }// parse()


    /**
     * Returns the Province name upto the first Coast seperator
     * character ('-', '/', or '\'); Parentheses are not supported.
     */
    public static String getProvinceName(final String input) {
        if (input.length() > 3) {
            final int idx = input.length() - 3;
            final char c = input.charAt(idx);
            if (c == '-' || c == '/' || c == '\\') {
                return input.substring(0, idx);
            }
        }
        return input;
    }// getProvinceName()


    /**
     * Normalizes coasts to standard format "/xx".
     * <p>
     * The following applies:
     * <pre>
     * a) input must be lower-case
     * b) normalizes:
     * axy     where a = "/" "\" or "-"
     * x       where x = any alphanumeric [but is later checked]; a "." may follow
     * y       where y = "c" or (if x="m", "v"); a "." my follow
     * c) parenthetical coasts
     * coalesces preceding spaces (before parenthesis), so
     * "stp(sc)", "stp( sc)", "stp(.s.c.)", "stp (sc)", and "stp    (sc)" all would become "stp/sc"
     * coast depends upon FIRST character
     * stp(qoieru)    ==> invalid!
     * 	</pre>
     * <p>
     * An OrderException is thrown if the coast is not recognized. The OrderException will contain
     * the invalid coast text only.
     * <p>
     * Bug note: the following "xxx-n.c." will be converted to "xxx-nc ." Note the extra period.
     */
    public static Optional<String> normalize(final String input)  {
        // start matching.
        String matchInput = input;
        for (final Pattern pattern : patterns) {
            final Matcher m = pattern.matcher(matchInput);
            final StringBuffer sb = new StringBuffer(matchInput.length());

            boolean result = m.find();
            while (result) {
                if (m.groupCount() == 2) {
                    // catch empty group "()"
                    if (m.group(1).length() == 0) {
                        return Optional.empty();
                    }

                    final char c1 = m.group(1).charAt(0);
                    final char c2 = m.group(2).charAt(0);

                    //System.out.println("1: "+m.group(1)+";  2: "+m.group(2));

                    if (c2 == ')') {
                        final String group1 = superTrim(m.group(1));

                        // test 'full name' and abbreviated coasts inside parentheses
                        if (group1.startsWith("north") || "nc".equals(group1)) {
                            m.appendReplacement(sb, "/nc ");
                        } else if (group1.startsWith("south") || "sc"
                                .equals(group1)) {
                            m.appendReplacement(sb, "/sc ");
                        } else if (group1.startsWith("west") || "wc"
                                .equals(group1)) {
                            m.appendReplacement(sb, "/wc ");
                        } else if (group1.startsWith("east") || "ec"
                                .equals(group1)) {
                            m.appendReplacement(sb, "/ec ");
                        } else if ("mv".equals(group1)) {
                            m.appendReplacement(sb, "/mv ");
                        } else if ("xc".equals(group1)) {
                            m.appendReplacement(sb, "/xc ");
                        }
                    } else if (c2 == 'c' && (c1 == 'n' || c1 == 's' || c1 == 'w' || c1 == 'e' || c1 == 'x') || c1 == 'm' && c2 == 'v') {
                        final StringBuffer rep = new StringBuffer(4);
                        rep.append('/');
                        rep.append(c1);
                        rep.append(c2);
                        rep.append(
                                ' ');    // space added afterwards--essential!
                        m.appendReplacement(sb, rep.toString());
                    } else {
                        Optional.empty();
                    }
                } else {
                    Optional.empty();
                }

                result = m.find();
            }

            m.appendTail(sb);
            matchInput = sb.toString();
        }

        return Optional.of(matchInput.trim());
    }// normalize()

    /**
     * Trims the following characters before, within, and after a given string.
     * <br>
     * space, tab, '.'
     */
    private static String superTrim(final String in) {
        return in.replaceAll("\\.*\\s*\\t*", "");
    }// superTrim()


    /**
     * Returns <code>true</code> if coast is one of
     * Coast.NORTH, Coast.SOUTH, Coast.WEST, or Coast.EAST
     */
    public boolean isDirectional() {
        return ANY_DIRECTIONAL.contains(this);
    }// isDirectionalCoast()


}// class Coast()
