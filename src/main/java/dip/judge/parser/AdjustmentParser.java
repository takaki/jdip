//
//  @(#)AdjustmentParser.java	1.00	6/2002
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

import com.codepoetics.protonpack.StreamUtils;
import dip.world.Power;
import dip.world.WorldMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses the Adjustment information block.
 * <br>
 * This includes both supply center ownership, and build/removes.
 */
public class AdjustmentParser {

    /**
     * Header text to look for
     */
    public static final String HEADER_REGEX = "(?i)ownership of supply centers:";

    /**
     * Adjustment regex
     * Capture groups: 1:power 2:# supply centers 3:#units 4:# to build or remove
     * This is always fed a trimmed string, and assumed that it is always on one line. But we search a whole block.
     * case-insensitive. Power names: alphanumeric + "-" and "_" supported.
     * Parsed on a per-line basis.
     */
    public static final String ADJUST_REGEX = "(?i)([\\p{Alnum}\\-_]*):\\D*(\\d+)\\D+(\\d+)\\D+((\\d+)).*\\.";


    // INSTANCE VARIABLES
    private final WorldMap map;

    private final List<OwnerInfo> ownerList;
    private final Pattern regexAdjust;

    private final List<OwnerInfo> ownerInfo;
    private final List<AdjustInfo> adjustInfo;

    /**
     * Creates a AdjustmentParser object, which parses the given input for an Ownership and Adjustment info blocks
     */
    public AdjustmentParser(final WorldMap map,
                            final String input) throws IOException {
        Objects.requireNonNull(map);
        Objects.requireNonNull(input);

        this.map = map;
        // create lists
        ownerList = new LinkedList<>();
        final List<AdjustInfo> adjustList = new LinkedList<>();

        // create patterns
        regexAdjust = Pattern.compile(ADJUST_REGEX);

        // Create HEADER_REGEX pattern
        final Pattern header = Pattern.compile(HEADER_REGEX);

        // search for HEADER_REGEX
        // create a block of text
        final BufferedReader br = new BufferedReader(new StringReader(input));

        String line = br.readLine();
        while (line != null) {
            final Matcher m = header.matcher(line);
            if (m.lookingAt()) {
                parseOwnerBlock(ParserUtils.parseBlock(br));


//
                adjustList.addAll(StreamUtils.takeWhile(
                        Arrays.stream(ParserUtils.parseBlock(br).split("\\n"))
                                .map(regexAdjust::matcher), Matcher::find)
                        .map(ml -> new AdjustInfo(ml.group(1),
                                Integer.parseInt(ml.group(2)),
                                Integer.parseInt(ml.group(3)),
                                Integer.parseInt(ml.group(4))))
                        .collect(Collectors.toList()));
                break;
            }

            line = br.readLine();
        }


        // create the output array
        ownerInfo = new ArrayList<>(ownerList);
        adjustInfo = new ArrayList<>(adjustList);

        // cleanup
        br.close();
        ownerList.clear();
        adjustList.clear();
    }// AdjustmentParser()


    /**
     * Returns an array of OwnerInfo objects; this never returns null.
     */
    public List<OwnerInfo> getOwnership() {
        return Collections.unmodifiableList(ownerInfo);
    }// getOwnership()

    /**
     * Returns an array of AdjustInfo objects; this never returns null.
     */
    public List<AdjustInfo> getAdjustments() {
        return Collections.unmodifiableList(adjustInfo);
    }// getAdjustments()


    /**
     * An OwnerInfo object is created for each power.
     * <p>
     */
    public static final class OwnerInfo {
        private final String power;
        private final List<String> locations;

        /**
         * Create a OwnerInfo object
         */
        public OwnerInfo(final String power, final List<String> locations) {
            this.power = power;
            this.locations = new ArrayList<>(locations);
        }// OwnerInfo()

        /**
         * Name of the Power
         */
        public String getPowerName() {
            return power;
        }

        /**
         * Names of provinces with owned supply centers
         */
        public List<String> getProvinces() {
            return Collections.unmodifiableList(locations);
        }

        /**
         * String output for debugging; may change between versions.
         */
        @Override
        public String toString() {
            return String.format("OwnerInfo[power=%s, locations=%s]", power,
                    String.join(",", locations));
        }// toString()
    }// nested class OwnerInfo


    /**
     * An AdjustInfo object is created for each power, and contains adjustment information
     * <p>
     */
    public static final class AdjustInfo {
        private final String power;
        private final int numSC;
        private final int numUnits;
        private final int toBuildOrRemove;

        /**
         * Create an AdjustInfo object
         */
        public AdjustInfo(final String power, final int numSC,
                          final int numUnits, final int toBuildOrRemove) {
            Objects.requireNonNull(power);
            if (numSC < 0 || numUnits < 0 || toBuildOrRemove < 0) {
                throw new IllegalArgumentException("bad arguments");
            }

            this.power = power;
            this.numSC = numSC;
            this.numUnits = numUnits;
            this.toBuildOrRemove = toBuildOrRemove;
        }// AdjustInfo()

        /**
         * Name of the Power
         */
        public String getPowerName() {
            return power;
        }

        /**
         * Current number of supply centers
         */
        public int getNumSupplyCenters() {
            return numSC;
        }

        /**
         * Current number of units
         */
        public int getNumUnits() {
            return numUnits;
        }

        /**
         * Number of units to build or remove
         */
        public int getNumBuildOrRemove() {
            return toBuildOrRemove;
        }

        /**
         * String output for debugging; may change between versions.
         */
        @Override
        public String toString() {
            return String
                    .format("AdjustInfo[power=%s, SC=%d, units=%d, change=%d]",
                            power, numSC, numUnits, toBuildOrRemove);
        }// toString()
    }// nested class AdjustInfo


    /**
     * Given a trimmed block, determines ownership
     */
    private void parseOwnerBlock(final String text) throws IOException {
        // map of Powers to StringBuffers
        final Map<Power, StringBuffer> pmap = new HashMap<>();

        // parse and re-formulate
        // into a new string
        //
        Power currentPower = null;
        ;
        for (final String tok : text.split("[ \f\t\n\r]+")) {
            if (tok.equalsIgnoreCase("unowned:")) {
                // we don't process unowned SC yet. I'm not sure that
                // all judges support this??
                currentPower = null;
            } else if (tok.endsWith(":")) {
                // should be a Power
                //
                final Power p = map
                        .getPower(tok.substring(0, tok.length() - 1));
                if (p == null) {
                    throw new IOException(
                            "Adjustment Block: Power " + tok + " not recognized.");
                }

                // toss into the map
                currentPower = p;
                pmap.put(p, new StringBuffer());
            } else {
                if (currentPower != null) {
                    final StringBuffer sb = pmap.get(currentPower);
                    sb.append(tok);
                    sb.append(" ");
                }
            }
        }

        // now, iterate through the powers
        // parse the province
        // there may be a "." on the end of some
        // which should be eliminated.
        //
        for (final Power allPower : map.getPowers()) {
            final StringBuffer sb = pmap.get(allPower);
            if (sb != null) {
                // clean up province tokens
                // remove parentheses (put on blockaded SC in games with Wings)
                // create OwnerInfo
                ownerList.add(new OwnerInfo(allPower.getName(),
                        Arrays.stream(sb.toString().split("[\\,]"))
                                .map(String::trim)
                                .map(prov -> prov.endsWith(".") || prov
                                        .endsWith(")") ? prov
                                        .substring(0, prov.length() - 1) : prov)
                                .map(prov -> prov.startsWith("(") ? prov
                                        .substring(1) : prov)
                                .collect(Collectors.toList())));
            }
        }
    }// parseOwnerBlock()


}// class AdjustmentParser
