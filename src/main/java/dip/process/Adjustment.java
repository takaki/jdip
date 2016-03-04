//
//  @(#)Adjustment.java	1.00	4/1/2002
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
package dip.process;

import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.RuleOptions;
import dip.world.RuleOptions.Option;
import dip.world.RuleOptions.OptionValue;
import dip.world.TurnState;
import dip.world.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Calculates Adjustments (how many units a power may build or must remove).
 */
public final class Adjustment {


    private Adjustment() {
    }// Adjustment()

    /**
     * Determines AdjustmentInfo for a given power
     * <p>
     * Note that this will work during any phase.
     */
    public static AdjustmentInfo getAdjustmentInfo(final TurnState turnState,
                                                   final RuleOptions ruleOpts,
                                                   final Power power) {
        Objects.requireNonNull(power);
        Objects.requireNonNull(turnState);
        final AdjustmentInfo ai = new AdjustmentInfo(ruleOpts);

        final Position position = turnState.getPosition();
        final List<Province> provinces = position.getProvinces();
        ai.numUnits += provinces.stream()
                .filter(province -> position.getUnit(province)
                        .filter(u -> Objects.equals(u.getPower(), power))
                        .isPresent()).count();

        ai.numDislodgedUnits += provinces.stream()
                .filter(province -> position.getDislodgedUnit(province)
                        .filter(unit -> Objects.equals(unit.getPower(), power))
                        .isPresent()).count();
        ai.numSC += provinces.stream()
                .filter(province -> position.getSupplyCenterOwner(province)
                        .filter(p -> Objects.equals(p, power)).isPresent())
                .count();
        ai.numHSC += provinces.stream()
                .filter(province -> position.getSupplyCenterOwner(province)
                        .filter(p -> Objects.equals(p, power))
                        .map(p0 -> position.getSupplyCenterHomePower(province)
                                .filter(p -> Objects.equals(p, power)))
                        .isPresent()).count();
        return ai;
    }// getAdjustmentInfo()


    /**
     * Determines AdjustmentInfo for all Powers;
     * <p>
     * Note that this will work during any phase.
     * <p>
     * Results are returned as an AdjustmentInfoMap
     */
    public static AdjustmentInfoMap getAdjustmentInfo(final TurnState turnState,
                                                      final RuleOptions ruleOpts,
                                                      final List<Power> powers) {
        Objects.requireNonNull(powers);
        Objects.requireNonNull(turnState);

        // setup AdjustmentInfoMap
        final AdjustmentInfoMap adjMap = new AdjustmentInfoMap();
        for (final Power power1 : powers) {
            adjMap.put(power1, new AdjustmentInfo(ruleOpts));
        }

        // Iterate for all Powers
        final Position position = turnState.getPosition();
        final List<Province> provinces = position.getProvinces();

        for (final Province province : provinces) {
            final boolean hasUnit;

            // tally units
            final Unit unit = position.getUnit(province).orElse(null);
            if (unit != null) {
                adjMap.get(unit.getPower()).numUnits++;
                hasUnit = true;
            } else {
                hasUnit = false;
            }

            final Unit orElse = position.getDislodgedUnit(province)
                    .orElse(null);
            if (orElse != null) {
                adjMap.get(orElse.getPower()).numDislodgedUnits++;
            }

            // tally supply centers
            final Power power = position.getSupplyCenterOwner(province)
                    .orElse(null);
            if (power != null) {
                adjMap.get(power).numSC++;

                if (hasUnit) {
                    adjMap.get(power).numOccSC++;
                }


                final Power anElse = position.getSupplyCenterHomePower(province)
                        .orElse(null);
                if (anElse != null) {
                    adjMap.get(anElse).numHSC++;

                    if (hasUnit) {
                        adjMap.get(anElse).numOccHSC++;
                    }
                }
            }
        }

        return adjMap;
    }// getAdjustmentInfo()


    /**
     * Class containing various information about Adjustments, for a given Power
     */
    public static class AdjustmentInfo {
        // these private fields may still be set by enclosing class
        //
        private int numUnits;        // # of units total
        private int numSC;        // # of owned supply centers
        private int numHSC;        // # of owned home supply centers
        private int numOccHSC;    // # of occupied owned home supply centers
        private int numOccSC;        // # of occupied non-home supply centers
        private int numDislodgedUnits;    // # of dislodged units

        private int adj;            // the adjustment amount, as determined by calculate()
        private boolean isCalc;    // if we have been calculated
        private final RuleOptions ruleOpts; // for calculating


        /**
         * Construct an AdjustmentInfo object
         */
        protected AdjustmentInfo(final RuleOptions ruleOpts) {
            Objects.requireNonNull(ruleOpts);

            this.ruleOpts = ruleOpts;
        }// AdjustmentInfo()


        /**
         * # of units to adjust (+/0/-) from current unit count.
         * <p>
         * If units cannot be built, because supply center is occupied,
         * that is taken into account
         */
        public int getAdjustmentAmount() {
            checkCalc();
            return adj;
        }// getAdjustmentAmount()

        /**
         * Force calculation of adjustment amount, and adjust supply-center
         * information according to Build Options
         * <p>
         * IT IS VITAL for this method to be called after values are set, and
         * before any values are read back.
         * <p>
         * There should be no effect if this method is called multiple times,
         * as long as RuleOptions do NOT change in between callings.
         */
        private void calculate() {
            final OptionValue buildOpt = ruleOpts
                    .getOptionValue(Option.OPTION_BUILDS);

            assert numOccSC <= numSC;

            if (buildOpt == OptionValue.VALUE_BUILDS_HOME_ONLY) {
                // Adjustment = number of SC gained. But, if we have gained more adjustments
                // than we have home supply centers to build on, those builds are discarded.
                // Or, if some are occupied, those builds are discarded.
                // e.g.:
                // 		3 builds, 3 empty owned home supply centers: adjustments: +3
                // 		3 builds, 2 empty owned home supply centers: adjustments: +2
                adj = numSC - numUnits;
                adj = adj > numHSC - numOccHSC ? numHSC - numOccHSC : adj;
            } else if (buildOpt == OptionValue.VALUE_BUILDS_ANY_OWNED) {
                // We can build in any owned supply center. Effectively, then,
                // ALL owned supply centers are home supply centers.
                numHSC = numSC;
                adj = numSC - numUnits;
                adj = adj > numSC - numOccSC ? numSC - numOccSC : adj;
            } else if (buildOpt == OptionValue.VALUE_BUILDS_ANY_IF_HOME_OWNED) {
                // We can build in any supply center, if at least ONE home supply
                // center is owned.
                adj = numSC - numUnits;
                adj = adj > 0 && numHSC < 1 ? 0 : adj;
                adj = adj > numSC - numOccSC ? numSC - numOccSC : adj;
            } else {
                // should not occur
                throw new IllegalStateException();
            }

            isCalc = true;
        }// calculate()

        /**
         * # of units for this power
         */
        public int getUnitCount() {
            checkCalc();
            return numUnits;
        }

        /**
         * # of dislodged units for this power
         */
        public int getDislodgedUnitCount() {
            checkCalc();
            return numDislodgedUnits;
        }

        /**
         * # of supply centers for this power (includes home supply centers)
         */
        public int getSupplyCenterCount() {
            checkCalc();
            return numSC;
        }

        /**
         * # of home supply centers
         */
        public int getHomeSupplyCenterCount() {
            checkCalc();
            return numHSC;
        }

        /**
         * mostly for debugging
         */
        @Override
        public String toString() {
            return String
                    .format("[AdjustmentInfo: units=%d; supplycenters=%d; home supplycenters=%d; adjustment=%d]",
                            numUnits, numSC, numHSC, getAdjustmentAmount());
        }// toString()


        /**
         * Checks if we have been calculated.
         */
        private void checkCalc() {
            if (!isCalc) {
                calculate();
            }
        }// checkCalc()


    }// nested class AdjustmentInfo


    /**
     * Aggregation of HashMap that contains only AdjustmentInfo objects,
     * mapped by Power.
     */
    public static final class AdjustmentInfoMap {
        private final Map<Power, AdjustmentInfo> map;

        /**
         * Create an AdjustmentInfoMap
         */
        public AdjustmentInfoMap() {
            map = new HashMap<>(13);
        }// AdjustmentInfoMap()

        /**
         * Create an AdjustmentInfoMap
         */
        public AdjustmentInfoMap(final int size) {
            map = new HashMap<>(size);
        }// AdjustmentInfoMap()

        /**
         * Set AdjustmentInfo for a power.
         */
        private void put(final Power power, final AdjustmentInfo ai) {
            map.put(power, ai);
        }// put()

        /**
         * Gets AdjustmentInfo for a power.
         */
        public AdjustmentInfo get(final Power power) {
            return map.get(power);
        }// get()

        /**
         * Clears all information from this object.
         */
        public void clear() {
            map.clear();
        }// clear()

    }// nested class AdjustmentInfoMap

}// class Adjustment
