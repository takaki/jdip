/*
*  @(#)Remove.java	1.00	4/1/2002
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
import dip.world.Location;
import dip.world.Power;
import dip.world.RuleOptions;
import dip.world.TurnState;
import dip.world.Unit.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Remove order.
 */
public class Remove extends Order {
    private static final Logger LOG = LoggerFactory.getLogger(Remove.class);
    // il8n constants
    private static final String REMOVE_FORMAT = "REMOVE_FORMAT";

    // constants: names
    private static final String orderNameBrief = "R";
    private static final String orderNameFull = "Remove";
    private static final transient String orderFormatString = Utils
            .getLocalString(REMOVE_FORMAT);


    /**
     * Creates a Remove order
     */
    protected Remove(final Power power, final Location src, final Type srcUnit) {
        super(power, src, srcUnit);
    }// Remove()

    /**
     * Creates a Remove order
     */
    protected Remove() {
        super();
    }// Remove()

    @Override
    public String getFullName() {
        return orderNameFull;
    }// getName()

    @Override
    public String getBriefName() {
        return orderNameBrief;
    }// getBriefName()


    // order formatting
    @Override
    public String getDefaultFormat() {
        return orderFormatString;
    }// getFormatBrief()


    @Override
    public String toBriefString() {
        String sb = power +
                ": " +
                orderNameBrief +
                ' ' +
                srcUnitType.getShortName() +
                ' ' +
                src.getBrief();

        return sb;
    }// toBriefString()


    @Override
    public String toFullString() {
        String sb = power +
                ": " +
                orderNameFull +
                ' ' +
                srcUnitType.getFullName() +
                ' ' +
                src.getFull();

        return sb;
    }// toFullString()


    public boolean equals(final Object obj) {
        if (obj instanceof Remove) {
            if (super.equals(obj)) {
                return true;
            }
        }
        return false;
    }// equals()


    @Override
    public void validate(final TurnState state, final ValidationOptions valOpts,
                         final RuleOptions ruleOpts) throws OrderException {
        checkSeasonAdjustment(state, orderNameFull);
        super.validate(state, valOpts, ruleOpts);
        checkPower(power, state, false);

        // not much else to validate; adjudiator must take care of tricky situations.
    }// validate()


    /**
     * Empty method: Remove orders do not require verification.
     */
    @Override
    public void verify(final Adjudicator adjudicator) {
        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()

    /**
     * Empty method: Remove orders do not require dependency determination.
     */
    @Override
    public void determineDependencies(final Adjudicator adjudicator) {
    }


    /**
     * Remove orders are always successful.
     * <p>
     * Note that too many (or two few) remove orders may be given; this
     * must be handled by the adjustment adjudicator.
     */
    @Override
    public void evaluate(final Adjudicator adjudicator) {
        LOG.debug("--- evaluate() dip.order.Disband ---");
        LOG.debug("   order: {}", this);

        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            thisOS.setEvalState(Tristate.SUCCESS);
        }
    }// evaluate()


}// class Remove
