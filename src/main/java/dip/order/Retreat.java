//  
//  @(#)Retreat.java	4/2002
//  
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;

import dip.misc.Utils;
import dip.order.result.OrderResult.ResultType;
import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.RetreatChecker;
import dip.process.Tristate;
import dip.world.Location;
import dip.world.Position;
import dip.world.Power;
import dip.world.RuleOptions;
import dip.world.TurnState;
import dip.world.Unit;
import dip.world.Unit.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the Retreat order.
 * <p>
 * Note that a Retreat order is a Move order. Retreat orders are issued
 * instead of Move orders during the Retreat phase, by OrderParser.
 * <p>
 * Convoy options are not valid in Retreat orders.
 */

public class Retreat extends Move {
    private static final Logger LOG = LoggerFactory.getLogger(Retreat.class);
    // il8n constants
    private static final String RETREAT_SRC_EQ_DEST = "RETREAT_SRC_EQ_DEST";
    private static final String RETREAT_CANNOT = "RETREAT_CANNOT";
    private static final String RETREAT_FAIL_DPB = "RETREAT_FAIL_DPB";
    private static final String RETREAT_FAIL_MULTIPLE = "RETREAT_FAIL_MULTIPLE";

    private static final String orderNameFull = "Retreat";    // brief order name is still 'M'


    /**
     * Creates a Retreat order
     */
    protected Retreat(final Power power, final Location src,
                      final Type srcUnitType, final Location dest) {
        super(power, src, srcUnitType, dest, false);
    }// Move()

    /**
     * Creates a Retreat order
     */
    protected Retreat() {
    }// Retreat()

    /**
     * Retreats are never convoyed; this will always return false.
     */
    public boolean isByConvoy() {
        return false;
    }// isByConvoy()


    @Override
    public String getFullName() {
        return orderNameFull;
    }// getFullName()


    public boolean equals(final Object obj) {
        if (obj instanceof Retreat) {
            final Retreat retreat = (Retreat) obj;
            if (super.equals(retreat) && dest.equals(retreat.dest)) {
                return true;
            }
        }
        return false;
    }// equals()


    @Override
    public void validate(final TurnState state, final ValidationOptions valOpts,
                         final RuleOptions ruleOpts) throws OrderException {
        // step 0
        checkSeasonRetreat(state, getFullName());
        checkPower(power, state, true);


        // 1
        final Position position = state.getPosition();
        final Unit unit = position.getDislodgedUnit(src.getProvince())
                .orElse(null);
        validate(valOpts, unit);

        if (valOpts.getOption(ValidationOptions.KEY_GLOBAL_PARSING)
                .equals(ValidationOptions.VALUE_GLOBAL_PARSING_STRICT)) {
            // 2
            if (src.isProvinceEqual(dest)) {
                throw new OrderException(
                        Utils.getLocalString(RETREAT_SRC_EQ_DEST));
            }

            // validate Borders
            if (src.getProvince()
                    .getTransit(src, srcUnitType, state.getPhase(), getClass())
                    .isPresent()) {
                throw new OrderException(
                        Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(),
                                src.getProvince().getTransit(src, srcUnitType,
                                        state.getPhase(), getClass())
                                        .orElse(null).getDescription()));
            }

            // 3: validate destination [must be a legal move] This is important, because
            // the coast of the destination needs to be known (if unit is a fleet).
            // otherwise, RetreatChecker.isValid() won't work.
            dest = dest.getValidatedWithMove(srcUnitType, src);

            // check that we can transit into destination (check borders)
            if (dest.getProvince()
                    .getTransit(dest, srcUnitType, state.getPhase(), getClass())
                    .isPresent()) {
                throw new OrderException(
                        Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(),
                                dest.getProvince().getTransit(dest, srcUnitType,
                                        state.getPhase(), getClass())
                                        .orElse(null).getDescription()));
            }

            // 4
            final RetreatChecker rc = new RetreatChecker(state);
            if (!rc.isValid(src, dest)) {
                throw new OrderException(
                        Utils.getLocalString(RETREAT_CANNOT, dest));
            }
        }
    }// validate()


    /**
     * No verification is performed.
     */
    @Override
    public void verify(final Adjudicator adjudicator) {
        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()


    /**
     * Retreat orders are only dependent on other
     * retreat orders that are moving to the same destination.
     */
    @Override
    public void determineDependencies(final Adjudicator adjudicator) {
        // add moves to destination space, and supports of this space
        final List<OrderState> orderStates = adjudicator.getOrderStates();
        final List<OrderState> depMTDest = orderStates.stream()
                .filter(dependentOS -> {
                    final Orderable order = dependentOS.getOrder();

                    if (order instanceof Retreat && order != this) {
                        final Retreat retreat = (Retreat) order;

                        if (retreat.getDest().isProvinceEqual(getDest())) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());

        // set dependent moves to destination
        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setDependentMovesToDestination(depMTDest);
    }// determineDependencies()


    /**
     * If a retreat is valid, it will be successfull unless
     * there exists one or more retreats to the same destination.
     */
    @Override
    public void evaluate(final Adjudicator adjudicator) {
        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        // while Retreat orders cannot be supported, we DO need to determine
        // if we are retrating across a difficult passable border.
        // We only need to set 'max' support. For the typical case (base move
        // modifier == -1), this will return "1" unless over a DPB (base move
        // modifier -1) which will return "0".
        //
        thisOS.setRetreatStrength(thisOS.getSupport(false));

        LOG.debug("--- evaluate() dip.order.Retreat ---");
        LOG.debug("   order: {}", this);
        LOG.debug("   retreat strength: [dpb check]: {}",
                thisOS.getRetreatStrength());
        LOG.debug("   # moves to dest: {}",
                thisOS.getDependentMovesToDestination().size());

        // if other retreats to destination, we will succeed; otherwise, we will
        // probably fail.
        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            final List<OrderState> depMovesToDest = thisOS
                    .getDependentMovesToDestination();

            if (depMovesToDest.isEmpty()) {
                // typical case
                LOG.debug("    SUCCESS!");
                thisOS.setEvalState(Tristate.SUCCESS);
            } else {
                // Modified for Difficult Passsable Border (see DATC 16-dec-03 10.K)
                // But must differentiate DPB from multiple retreats to same location.
                //
                // Furthermore, we need to account for the possibility of 3 retreats
                // to 1 dest, where 2 retreats are over a normal border and 1 is over
                // a DPB; that would cause all retreats to fail.
                //
                // Thus, if we are <= strength of others, we fail. Otherwise we are >, and
                // successful.
                //
                // This logic *MUST* be run in an evaluation loop.
                //
                Tristate evalResult = Tristate.UNCERTAIN;
                boolean isStrongerThanAllOthers = false;

                for (final OrderState depMoveOS : depMovesToDest) {

                    if (depMoveOS.isRetreatStrengthSet()) {
                        if (thisOS.getRetreatStrength() < depMoveOS
                                .getRetreatStrength()) {
                            // only can be less when considering DPBs
                            LOG.debug("    FAILURE! (<){}",
                                    depMoveOS.getOrder());
                            evalResult = Tristate.FAILURE;
                            adjudicator.addResult(thisOS, ResultType.FAILURE,
                                    Utils.getLocalString(RETREAT_FAIL_DPB));
                            isStrongerThanAllOthers = false;
                            break;
                        } else if (thisOS.getRetreatStrength() == depMoveOS
                                .getRetreatStrength()) {
                            // the usual case
                            LOG.debug("    FAILURE! (==){}",
                                    depMoveOS.getOrder());
                            evalResult = Tristate.FAILURE;
                            adjudicator.addResult(thisOS, ResultType.FAILURE,
                                    Utils.getLocalString(
                                            RETREAT_FAIL_MULTIPLE));
                            isStrongerThanAllOthers = false;
                            break;
                        } else {// >
                            // we *may* be stronger than all others
                            LOG.debug(
                                    "    We may be stronger than all others...");
                            isStrongerThanAllOthers = true;
                        }
                    } else {
                        // remain uncertain. Dependent orderstate not yet evaluated.
                        LOG.debug("    Uncertain: {} not yet evaluated.",
                                depMoveOS.getOrder());
                        isStrongerThanAllOthers = false;    // we don't know yet.
                        evalResult = Tristate.UNCERTAIN;
                    }
                }// for()

                // if we are stronger than all others, we will succeed.
                if (isStrongerThanAllOthers) {
                    LOG.debug("    SUCCESS! [stronger than all others]");
                    evalResult = Tristate.UNCERTAIN;
                }

                thisOS.setEvalState(evalResult);
            }
        }

        LOG.debug("    final evalState() = {}", thisOS.getEvalState());
    }// evaluate()

}// class Retreat
