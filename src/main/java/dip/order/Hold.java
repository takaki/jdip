/*
*  @(#)Hold.java	1.00	4/1/2002
*
*  Copyright 2002 Zachary DelProposto. All rights reserved.
*  Use is subject to license terms.
*/
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
import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;
import dip.world.Border;
import dip.world.Location;
import dip.world.Power;
import dip.world.RuleOptions;
import dip.world.TurnState;
import dip.world.Unit.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Hold order.
 */
public class Hold extends Order {
    private static final Logger LOG = LoggerFactory.getLogger(Hold.class);
    // il8n
    private static final String HOLD_FORMAT = "HOLD_FORMAT";

    // constants: names
    private static final String orderNameBrief = "H";
    private static final String orderNameFull = "Hold";
    private static final transient String orderFormatString = Utils
            .getLocalString(HOLD_FORMAT);


    /**
     * Creates a Hold order
     */
    protected Hold(final Power power, final Location src, final Type srcUnit) {
        super(power, src, srcUnit);
    }// Hold()

    /**
     * Creates a Hold order
     */
    protected Hold() {
    }// Hold()


    @Override
    public String getFullName() {
        return orderNameFull;
    }// getName()

    @Override
    public String getBriefName() {
        return orderNameBrief;
    }// getBriefName()


    // format-strings for orders
    @Override
    public String getDefaultFormat() {
        return orderFormatString;
    }// getFormatBrief()


    @Override
    public String toBriefString() {
        return String.format("%s %s", getBrief(), orderNameBrief);
    }// toBriefString()


    @Override
    public String toFullString() {
        return String.format("%s %s", getFull(), orderNameFull);
    }// toFullString()


    public boolean equals(final Object obj) {
        return obj instanceof Hold && super.equals(obj);
    }// equals()


    @Override
    public void validate(final TurnState state, final ValidationOptions valOpts,
                         final RuleOptions ruleOpts) throws OrderException {
        checkSeasonMovement(state, orderNameFull);
        checkPower(power, state,
                false);    // inactive powers can issue Hold orders
        super.validate(state, valOpts, ruleOpts);

        // validate Borders
        final Border border = src.getProvince()
                .getTransit(src, srcUnitType, state.getPhase(), getClass())
                .orElse(null);
        if (border != null) {
            throw new OrderException(
                    Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(),
                            border.getDescription()));
        }
    }// validate();


    /**
     * No verification is required for Hold orders.
     */
    @Override
    public void verify(final Adjudicator adjudicator) {
        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()


    /**
     * Dependencies for a Hold order:
     * <ol>
     * <li>Supports to this space
     * <li>Moves to this space
     * </ol>
     */
    @Override
    public void determineDependencies(final Adjudicator adjudicator) {
        addSupportsOfAndMovesToSource(adjudicator);
    }// determineDependencies()

    /**
     * Hold order evaluation logic.
     */
    @Override
    public void evaluate(final Adjudicator adjudicator) {
        LOG.debug("--- evaluate() dip.order.Hold ---");

        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        // calculate support
        thisOS.setDefMax(thisOS.getSupport(false));
        thisOS.setDefCertain(thisOS.getSupport(true));

        LOG.debug("   order: {}", this);
        LOG.debug("   initial evalstate: {}", thisOS.getEvalState());
        LOG.debug("     def-max: {}", thisOS.getDefMax());
        LOG.debug("    def-cert: {}", thisOS.getDefCertain());
        LOG.debug("  # supports: {}", thisOS.getDependentSupports().size());
        LOG.debug("  dislodged?: {}", thisOS.getDislodgedState());


        // if no moves against this order, we must succeed.
        // Otherwise, MOVE orders will determine if we are dislodged and thus fail.
        if (thisOS.getEvalState() == Tristate.UNCERTAIN && thisOS
                .getDependentMovesToSource().isEmpty()) {
            thisOS.setEvalState(Tristate.SUCCESS);
            thisOS.setDislodgedState(Tristate.NO);
        }
        LOG.debug("  final evalState: {}", thisOS.getEvalState());
    }// evaluate()

}// class Hold
