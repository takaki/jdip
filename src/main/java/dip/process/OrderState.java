//
//  @(#)OrderState.java	1.00	4/1/2002
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

import dip.order.Move;
import dip.order.Order;
import dip.order.Orderable;
import dip.order.Support;
import dip.world.Location;
import dip.world.Power;
import dip.world.Province;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * OrderState objects keep track of the state of an order, during adjudication.
 * OrderStates are not designed to be serialized.
 * <p>
 * Each OrderState represents a decision. All OrderStates initially start out with an evaluation state
 * of Tristate.UNCERTAIN. Once a decision has been made, based upon the internal state, the evaluation
 * state is finalized (Tristate.SUCCESS or Tristate.FAILURE). Ince finalized, a decision can never be reversed.
 * Irreversibility is enforced by the OrderState object.
 * <p>
 * _max values only become smaller.<br>
 * _certain values only become larger.<br>
 * <p>
 * defense: the strength to stay in the same position (hold strength)	<br>
 * attack: the strength required to dislodge a defender from it's position; a separate value
 * is computed for "self" attacks.
 * <p>
 * Dependent lists include OrderStates representing:
 * <ul>
 * <li> Support</li>
 * <li> Moves to Destination province</li>
 * <li> Moves to this (source) province</li>
 * </ul>
 * <p>
 * In Java code, OrderState variables typically have the suffix "OS".
 * <p>
 * NOTE: <b>When debugging, assertions should be enabled.</b>
 * <p>
 * DESIGN NOTE: this class is currently marked final but may not be in future versions.
 */
public final class OrderState {
    /**
     * NOTE: 'max' values start at an arbitrarily high value (something that is so large
     * that it could never occur in a game) and move down. Note that setting the value
     * to Integer.MAX_VALUE is NOT a good idea, because if we add two max values together
     * (e.g., when calculating support), we will overflow (to negative values)
     * So, we will use our own MAX_VALUE, which is still quite high.
     */
    public static final int MAX_VALUE = 9999;

    /**
     * MINIMUM value for a strength. Only used for Retreat Strength.
     */
    public static final int MIN_VALUE = -9999;


    private Orderable order;


    private int defense_max = MAX_VALUE;
    private int defense_certain = 1;                    // always can defend at 1

    private int attack_max = MAX_VALUE;
    private int attack_certain;                    // can't always can attack at 1. E.g DPB

    private int selfsupport_atk_max = MAX_VALUE;
    private int selfsupport_atk_certain;            // we may not be self-supporting, so start at 0

    private int retreatStr = MIN_VALUE;                // retreat strength (for DPB support)

    private boolean isCircular;
    private boolean isLegal = true;                    // assume order is legal

    private Tristate evalState = Tristate.UNCERTAIN;
    private Tristate dislodged = Tristate.NO;                // not-dislodged is default

    private List<OrderState> dependentSelfSupports = Collections
            .emptyList();    // only contains Support orders
    private List<OrderState> dependentSupports = Collections
            .emptyList();    // only contains Support orders
    private List<OrderState> dependentMovesToSource = Collections
            .emptyList();    // only contains Move orders
    private List<OrderState> dependentMovesToDestination = Collections
            .emptyList();    // only contains Move orders

    private OrderState headToHead;        // if it's a head-to-head move
    private OrderState dislodgedBy;    // orderstate which dislodged this unit
    private boolean foundConvoyPath;    // if move found a convoy path
    private boolean isVerified;            // has this order been verified() yet?


    /**
     * Create an OrderState. This is protected, because only subclasses of
     * this and adjudicators should be able to create new OrderState objects,
     * although other classes can definately use them.
     */
    protected OrderState(final Orderable order) {
        Objects.requireNonNull(order);

        this.order = order;
    }// OrderState()


    // GET methods

    /**
     * Get the Order for this OrderState.
     */
    public Orderable getOrder() {
        return order;
    }

    /**
     * Get if we are part of a circular movement chain.
     */
    public boolean isCircular() {
        return isCircular;
    }

    /**
     * Get the evaluation state.
     */
    public Tristate getEvalState() {
        return evalState;
    }

    /**
     * Get dislodged state.
     */
    public Tristate getDislodgedState() {
        return dislodged;
    }

    /**
     * Get maximum defense value
     */
    public int getDefMax() {
        return defense_max;
    }

    /**
     * Get certain defense value
     */
    public int getDefCertain() {
        return defense_certain;
    }

    /**
     * Get maximum attack value
     */
    public int getAtkMax() {
        return attack_max;
    }

    /**
     * Get certain attack value
     */
    public int getAtkCertain() {
        return attack_certain;
    }

    /**
     * Get maximum self-support attack value
     */
    public int getAtkSelfSupportMax() {
        return selfsupport_atk_max;
    }

    /**
     * Get certain self-support attack value.
     */
    public int getAtkSelfSupportCertain() {
        return selfsupport_atk_certain;
    }

    /**
     * Determines if this is part of a head-to-head Move.
     */
    public boolean isHeadToHead() {
        return headToHead != null;
    }

    /**
     * Get the order that we are in a head-to-head Move with.
     */
    public OrderState getHeadToHead() {
        return headToHead;
    }

    /**
     * Get the order legality (default is true).
     */
    public boolean isLegal() {
        return isLegal;
    }

    /**
     * Get the dislodger, if any.
     */
    public OrderState getDislodger() {
        return dislodgedBy;
    }

    /**
     * Get if we have found a convoy path.
     */
    public boolean hasFoundConvoyPath() {
        return foundConvoyPath;
    }

    /**
     * Get retreat strength
     */
    public int getRetreatStrength() {
        return retreatStr;
    }

    /**
     * Indicates if the retreat strength has been set.
     */
    public boolean isRetreatStrengthSet() {
        return retreatStr != MIN_VALUE;
    }

    /**
     * Indicate if Order has been Verified
     */
    public boolean isVerified() {
        return isVerified;
    }

    /**
     * Gets the dependent Support orders for this order
     */
    public List<OrderState> getDependentSupports() {
        return Collections.unmodifiableList(dependentSupports);
    }

    /**
     * Get the Move orders that are moving to the Source Location of this order.
     */
    public List<OrderState> getDependentMovesToSource() {
        return Collections.unmodifiableList(dependentMovesToSource);
    }

    /**
     * Get the Move orders that are moving to the Destination Location of this order.
     */
    public List<OrderState> getDependentMovesToDestination() {
        return Collections.unmodifiableList(dependentMovesToDestination);
    }

    /**
     * Gets the dependent self-Support orders for this order
     */
    public List<OrderState> getDependentSelfSupports() {
        return Collections.unmodifiableList(dependentSelfSupports);
    }

    /**
     * Set the Order. NOTE: this may be eliminated/deprecated, as it must be used with extreme care.
     */
    protected void setOrder(final Order value) {
        order = value;
    }    // consider eliminating

    /**
     * Set if this is part of a chain of circular movements.
     */
    public void setCircular(final boolean value) {
        isCircular = value;
    }

    /**
     * Set the dislodged state.
     */
    public void setDislodgedState(final Tristate value) {
        dislodged = value;
    }

    /**
     * Set the maximum defense.
     */
    public void setDefMax(final int value) {
        defense_max = value;
    }

    /**
     * Set the certain defense.
     */
    public void setDefCertain(final int value) {
        defense_certain = value;
    }

    /**
     * Set the maximum attack value.
     */
    public void setAtkMax(final int value) {
        attack_max = value;
    }

    /**
     * Set the certain attack value.
     */
    public void setAtkCertain(final int value) {
        attack_certain = value;
    }

    /**
     * Set the attack max including self-support
     */
    public void setAtkSelfSupportMax(final int value) {
        selfsupport_atk_max = value;
    }

    /**
     * Set the attack certain including self-support
     */
    public void setAtkSelfSupportCertain(final int value) {
        selfsupport_atk_certain = value;
    }

    /**
     * Set if we have found a convoy path
     */
    public void setFoundConvoyPath(final boolean value) {
        foundConvoyPath = value;
    }

    /**
     * Set the retreat strength
     */
    public void setRetreatStrength(final int value) {
        retreatStr = value;
    }

    /**
     * Sets if an Order is legal. By default, orders are legal.
     */
    public void setLegal(final boolean value) {
        isLegal = value;
    }

    /**
     * Set if an order has been verified. Once set to true, cannot be set to false.
     */
    public void setVerified(final boolean value) {
        if (!value && isVerified) {
            throw new IllegalStateException(
                    "setVerified() is irreversible, once set to true.");
        }

        isVerified = value;
    }// setVerified()

    /**
     * Set the evaluation state. Note that once set (e.g., value != UNCERTAIN),
     * it cannot be altered.
     */
    public void setEvalState(final Tristate value) {
        // ENSURE irreversibility
        if (evalState != Tristate.UNCERTAIN) {
            throw new IllegalStateException(
                    "EvalState is irreversible, once set.");
        }

        evalState = value;
    }// setEvalState()

    /**
     * if move is a head-to-head move, set which move we are moving head-to-head against here.
     */
    public void setHeadToHead(final OrderState os) {
        if (os != null && !(os.order instanceof Move)) {
            throw new IllegalArgumentException(
                    "h2h orderstate must be set with a Move order");
        }
        headToHead = os;
    }// setHeadToHead()

    /**
     * Set the dislodger. We do some sanity checks; this can only
     * be set if dislodged == MAYBE or YES. Furthermore, dislodger
     * must be a Move order.
     */
    public void setDislodger(final OrderState os) {
        assert os.order instanceof Move && dislodged != Tristate.NO;
        dislodgedBy = os;
    }// setDislodger()


    /**
     * Adds the given list, which must only contain Dependent Support OrderStates.
     * <p>
     * If assertions are enabled, the list is verified to contain only Support
     * OrderStates
     */
    public void setDependentSupports(final List<OrderState> osList) {
        assert verifyListSupport(osList);
        dependentSupports = new ArrayList<>(osList);
    }// setDependentSupports()

    /**
     * Criteria:
     * <ol>
     * <li>Support order
     * <li>Supported MOVE (not any other) [supportedSrc != supportedDest]
     * <li>unit in supportedDest must be present
     * <li>power of unit in supported dest == power of support order
     * </ol>
     * <b>NOTE:</b> OrderState only checks criteria #1 and #2, and only if
     * asserts are enabled.
     */
    public void setDependentSelfSupports(final List<OrderState> osList) {
        assert verifyListSelfSupport(osList);
        dependentSelfSupports = new ArrayList<>(osList);
    }// addDependentSupport()


    /**
     * Adds a List of the Dependent Move Orderstates to the Source Province of this Orderstate.
     * <p>If asserts are enabled, all OrderStates are verified to contain only Move orders.
     */
    public void setDependentMovesToSource(final List<OrderState> osList) {
        assert verifyListMove(osList);
        dependentMovesToSource = new ArrayList<>(osList);
    }// addDependentMoveToSource()


    /**
     * Adds a List of the Dependent Move Orderstates to the Destination Province of this Orderstate
     * <p>If asserts are enabled, all OrderStates are verified to contain only Move orders.
     */
    public void setDependentMovesToDestination(final List<OrderState> osList) {
        assert verifyListMove(osList);
        dependentMovesToDestination = new ArrayList<>(osList);
    }// addDependentMoveToDestination()


    /**
     * Convenicent method: get the order source Location
     */
    public Location getSource() {
        return order.getSource();
    }

    /**
     * Convenicent method: get the order source Province
     */
    public Province getSourceProvince() {
        return getSource().getProvince();
    }

    /**
     * Convenicent method: get the order Power
     */
    public Power getPower() {
        return order.getPower();
    }

    /**
     * For each support in the dependent list, the total support is
     * determined in the following manner:
     * <ul>
     * <li>if isCertain == true:
     * <ul>
     * <li>+1 if support evalState == TriSTate.SUCCESS
     * <li>+0 if evalState is UNCERTAIN or FAILURE
     * </ul>
     * <p>
     * <li>if isCertain == false;	[calculates 'max' support]
     * <ul>
     * <li>+1 if support evalState == SUCCESS or UNCERTAIN
     * <li>+0 if support evalState == FAILURE
     * </ul>
     * </ul>
     * <p>
     * <b>Note:</b><br>
     * It is assumed that all support is 'appropriate'; and that invalid supports have
     * already been marked as such (e.g., if order is Move A-B, and we have to supports:
     * support 1: C sup A-B, and support 2: C sup A) that support 2 (illegal for a Move order)
     * is marked FAILURE with it's evalstate.
     * <p>
     * If there is NO support, we return 1 (since all units have a default strength of 1),
     * unless the base move modifier (due to a difficult passable border) changes this amount.
     */
    public int getSupport(final boolean isCertain) {
        // Determine support, but based on the fact that provinces could have special borders
        final int mod;

        // Just figuring out how to get each border, and what its BaseMoveModifier is.
        if (order instanceof Move) {
            final Move order = (Move) this.order;
            mod = order.getDest().getProvince()
                    .getBaseMoveModifier(order.getSource());
        } else if (order instanceof Support) {
            final Support order = (Support) this.order;
            mod = order.getSupportedDest().getProvince()
                    .getBaseMoveModifier(order.getSource());
            // If the order is a convoy, hold,etc there is not BaseMoveModifier
        } else {
            mod = 0;
        }

        // Get the strength
        final int strength = getSupport(isCertain, dependentSupports, 1 + mod);
        // if it is 0 or above, great! If not, make it 0!
        return strength >= 0 ? strength : 0;
    }// getSupport()


    /**
     * Similar to getSupport(), but, determines self-support (if any).
     * If there is no self support, returns '0'.
     */
    public int getSelfSupport(final boolean isCertain) {
        return getSupport(isCertain, dependentSelfSupports, 0);
    }// getSelfSupport()


    /**
     * Helper method, used by other getSupport() and getSelfSupport() methods.
     */
    private static int getSupport(final boolean isCertain,
                                  final List<OrderState> supportList,
                                  final int defaultStrength) {
        int strength = defaultStrength;
        strength += supportList.stream()
                .filter(os -> os.evalState == Tristate.SUCCESS || !isCertain && os.evalState == Tristate.UNCERTAIN)
                .count();
        assert strength >= 0;    // negative strength is not allowed!
        return strength;
    }// getSupport()


    /**
     * Verifies that given list ONLY contains Move orderstates
     */
    private static boolean verifyListMove(final List<OrderState> list) {
        return list.stream().allMatch(os -> (os.order instanceof Move));

    }// verifyListMove()


    /**
     * Verifies that given list ONLY contains Support orderstates
     */
    private static boolean verifyListSupport(final List<OrderState> list) {
        return list.stream().allMatch(os -> (os.order instanceof Support));
    }// verifyListSupport()


    /**
     * Verifies that given list ONLY contains Self Support orderstates
     */
    private static boolean verifyListSelfSupport(final List<OrderState> list) {
        return list.stream().allMatch(
                os -> os.order instanceof Support && !((Support) os.order)
                        .isSupportingHold());

    }// verifyListSelfSupport()

}// class OrderState


