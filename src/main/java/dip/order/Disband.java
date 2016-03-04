/*
*  @(#)Disband.java	1.00	4/1/2002
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

import dip.misc.Log;
import dip.misc.Utils;
import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;
import dip.world.*;
import dip.world.Unit.Type;

/**
 * Implementation of the Disband order.
 */
public class Disband extends Order {
    // il8n
    private static final String DISBAND_FORMAT = "DISBAND_FORMAT";

    // constants: names
    private static final String orderNameBrief = "D";
    private static final String orderNameFull = "Disband";
    private static final transient String orderFormatString = Utils
            .getLocalString(DISBAND_FORMAT);


    /**
     * Creates a Disband order
     */
    protected Disband(final Power power, final Location src, final Type srcUnit) {
        super(power, src, srcUnit);
    }// Disband()

    /**
     * Creates a Disband order
     */
    protected Disband() {
        super();
    }// Disband()

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
        final StringBuffer sb = new StringBuffer(64);

        super.appendBrief(sb);
        sb.append(' ');
        sb.append(orderNameBrief);

        return sb.toString();
    }// toBriefString()


    @Override
    public String toFullString() {
        final StringBuffer sb = new StringBuffer(128);

        super.appendFull(sb);
        sb.append(' ');
        sb.append(orderNameFull);

        return sb.toString();
    }// toFullString()


    public boolean equals(final Object obj) {
        if (obj instanceof Disband) {
            if (super.equals(obj)) {
                return true;
            }
        }
        return false;
    }// equals()


    @Override
    public void validate(final TurnState state, final ValidationOptions valOpts,
                         final RuleOptions ruleOpts) throws OrderException {
        // step 0
        checkSeasonRetreat(state, orderNameFull);
        checkPower(power, state, false);    // inactive units can disband!

        // step 1
        final Position position = state.getPosition();
        final Unit unit = position.getDislodgedUnit(src.getProvince()).orElse(null);
        super.validate(valOpts, unit);
    }// validate()


    /**
     * Disband orders do not require verification.
     */
    @Override
    public void verify(final Adjudicator adjudicator) {
        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()

    /**
     * Empty method: Disband orders do not require dependency determination.
     */
    @Override
    public void determineDependencies(final Adjudicator adjudicator) {
    }


    /**
     * Disband orders are always successful.
     */
    @Override
    public void evaluate(final Adjudicator adjudicator) {
        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        Log.println("--- evaluate() dip.order.Disband ---");
        Log.println("   order: ", this);

        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            thisOS.setEvalState(Tristate.SUCCESS);
        }

        Log.println("   result: ", thisOS.getEvalState());
    }// evaluate()

}// class Disband
