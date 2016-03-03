//
//  @(#)VictoryConditions.java		4/2002
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
import dip.order.result.Result;
import dip.process.Adjudicator;
import dip.process.Adjustment.AdjustmentInfo;
import dip.process.Adjustment.AdjustmentInfoMap;
import dip.world.Phase.PhaseType;
import dip.world.Phase.SeasonType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Establishes the conditions required to determine who wins a game, and contains
 * methods to evaluate if these condtions are met during adjudication.
 * <p>
 */
public class VictoryConditions implements Serializable {
    // il8n
    private static final String VC_MAX_GAME_TIME = "VC_MAX_GAME_TIME";
    private static final String VC_DRAW = "VC_DRAW";
    private static final String VC_WIN_SINGLE = "VC_WIN_SINGLE";
    private static final String VC_MAX_NO_SC_CHANGE = "VC_MAX_NO_SC_CHANGE";

    // class variables
    protected final int numSCForVictory;        // SCs required for victory; 0 if ignored
    protected final int maxYearsNoSCChange;        // max years w/o supply-center chaning hands; 0 if ignored
    protected final int maxGameTimeYears;        // max time, in years, a game may last
    protected final int initialYear;            // starting game year

    // transient variables
    protected transient List<Result> evalResults;

    /**
     * VictoryConditions constructor
     */
    public VictoryConditions(final int numSCForVictory,
                             final int maxYearsNoSCChange,
                             final int maxGameTimeYears,
                             final Phase initialPhase) {
        if (maxGameTimeYears < 0 || numSCForVictory < 0 || maxYearsNoSCChange < 0) {
            throw new IllegalArgumentException("arg: < 0; use 0 to disable");
        }

        Objects.requireNonNull(initialPhase);

        if (maxGameTimeYears == 0 && numSCForVictory == 0 && maxYearsNoSCChange == 0) {
            throw new IllegalArgumentException("no conditions set!");
        }

        this.numSCForVictory = numSCForVictory;
        this.maxYearsNoSCChange = maxYearsNoSCChange;
        this.maxGameTimeYears = maxGameTimeYears;
        initialYear = initialPhase.getYear();
    }// VictoryConditions()


    /**
     * Returns the Result(s) of evaluate(). This will return an empty list if
     * evaluate() has not been called or returned false.
     */
    public List<Result> getEvaluationResults() {
        return Collections.unmodifiableList(evalResults);
    }// getEvaluationResults()


    /**
     * Evaluates the victory conditions. Returns <code>true</code> if
     * victory has been achieved (via any condition).
     * <p>
     * This method defers to the
     * <code>evaluate(TurnState, Adjustment.AdjustmentInfoMap)</code> method.
     * <p>
     *
     * @param adjMap      adjustment map (as returned by Adjustment.getAdjustmentInfo())
     * @param adjudicator an Adjudicator object
     */
    public boolean evaluate(final Adjudicator adjudicator,
                            final AdjustmentInfoMap adjMap) {
        return evaluate(adjudicator.getTurnState(), adjMap);
    }// evaluate()


    /**
     * Evaluates the victory conditions. Returns <code>true</code> if
     * victory has been achieved (via any condition).
     * <p>
     *
     * @param adjMap    adjustment map (as returned by Adjustment.getAdjustmentInfo())
     * @param turnState the TurnState
     */
    public boolean evaluate(final TurnState turnState,
                            final AdjustmentInfoMap adjMap) {
        final Phase phase = turnState.getPhase();
        final int currentYear = phase.getYear();

        if (evalResults == null) {
            evalResults = new ArrayList<>(5);
        } else {
            evalResults.clear();
        }

        // create an array of AdjustmentInfo, indexed the same as the array of Powers,
        // from the passed HashMap
        final List<Power> powers = turnState.getWorld().getMap().getPowers();
        final List<AdjustmentInfo> adjInfo = powers.stream().map(adjMap::get)
                .collect(Collectors.toList());

        // check to see if we have exceeded the allocated time
        if (maxGameTimeYears > 0) {
            if (currentYear - initialYear + 1 >= maxGameTimeYears) {
                evalResults.add(new Result(null,
                        Utils.getLocalString(VC_MAX_GAME_TIME,
                                maxGameTimeYears)));
                evalResults.add(new Result(null, Utils.getLocalString(VC_DRAW,
                        getRemainingPowers(turnState, powers, adjInfo))));
                return true;
            }
        }


        // check for single-power victory (via controlling numSCForVictory supply centers)
        if (numSCForVictory > 0) {
            for (int i = 0; i < adjInfo.size(); i++) {
                if (adjInfo.get(i).getSupplyCenterCount() >= numSCForVictory) {
                    evalResults.add(new Result(null,
                            Utils.getLocalString(VC_WIN_SINGLE, powers.get(i),
                                    adjInfo.get(i).getSupplyCenterCount(),
                                    numSCForVictory)));
                    return true;
                }
            }
        }


        // check # of years w/o any supply centers captured
        // (if game exists for less than this #, do nothing).
        if (maxYearsNoSCChange > 0 && currentYear - initialYear >= maxYearsNoSCChange && !turnState
                .getSCOwnerChanged()) {
            // we first check the current turnstate. We do this because it may not yet have been
            // added to the World object (via setTurnState()). We assume it is a fall movement
            // or retreat phase as well.
            boolean overallSCChange = turnState.getSCOwnerChanged();

            final World world = turnState.getWorld();
            for (int year = currentYear - 1; year > currentYear - maxYearsNoSCChange; year--) {
                overallSCChange |= getIfSCChangeOccured(world, year);
            }

            if (!overallSCChange) {
                evalResults.add(new Result(null,
                        Utils.getLocalString(VC_MAX_NO_SC_CHANGE,
                                maxYearsNoSCChange)));
                evalResults.add(new Result(null, Utils.getLocalString(VC_DRAW,
                        getRemainingPowers(turnState, powers, adjInfo))));
                return true;
            }
        }

        return false;
    }// evaluate()


    /**
     * Given a year, finds the phase in which supply-center-changes could occur.
     * <p>
     * the adjudicator marks the NEXT phase, indicating that supply center changes
     * have occured. Thus, the next phase will be either RETREAT (if a change occured
     * during movement) or ADJUSTMENT (if a change occured during retreat)
     * <p>
     * where supply-center-changes could occur. We actually will check both;
     */
    private boolean getIfSCChangeOccured(final World world, final int year) {
        boolean value = false;

        final TurnState tsRetreat = world.getTurnState(
                new Phase(SeasonType.FALL, year, PhaseType.RETREAT));
        final TurnState tsAdjustment = world.getTurnState(
                new Phase(SeasonType.FALL, year, PhaseType.ADJUSTMENT));

        if (tsRetreat != null) {
            value |= tsRetreat.getSCOwnerChanged();
        }

        if (tsAdjustment != null) {
            value |= tsAdjustment.getSCOwnerChanged();
        }

        return value;
    }// getLastSCChangePhase()


    // creates a comma-seperated list of power names, if they are still in play
    private String getRemainingPowers(final TurnState turnState,
                                      final List<Power> powers,
                                      final List<AdjustmentInfo> adjInfo) {
        final Position pos = turnState.getPosition();
        // check for power elimination [note: check might be redundant...]
        // we need to check adjInfo because we check for victory BEFORE powers may
        // be eliminated in the adjustment phase.
        return IntStream.range(0, powers.size())
                .filter(i -> adjInfo.get(i).getSupplyCenterCount() > 0 && !pos
                        .isEliminated(powers.get(i))).mapToObj(powers::get)
                .map(Power::toString).collect(Collectors.joining(", "));
    }// getRemainingPowers()


}// class VictoryConditions
