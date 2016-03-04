//	
//	 @(#)Build.java		4/1/2002
//	
//	 Copyright 2002 Zachary DelProposto. All rights reserved.
//	 Use is subject to license terms.
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

import dip.misc.Log;
import dip.misc.Utils;
import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;
import dip.world.Border;
import dip.world.Coast;
import dip.world.Location;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.RuleOptions;
import dip.world.RuleOptions.Option;
import dip.world.RuleOptions.OptionValue;
import dip.world.TurnState;
import dip.world.Unit.Type;

/**
 * Implementation of the Build order.
 */

public class Build extends Order {
    // il8n constants
    private static final String BUILD_MULTICOAST = "BUILD_MULTICOAST";
    private static final String BUILD_HOME_SUPPLY = "BUILD_HOME_SUPPLY";
    private static final String BUILD_OWNED_SUPPLY = "BUILD_OWNED_SUPPLY";
    private static final String BUILD_EXISTINGUNIT = "BUILD_EXISTINGUNIT";
    private static final String BUILD_FORMAT = "BUILD_FORMAT";
    private static final String BUILD_WING_PROHIBITED = "BUILD_WING_PROHIBITED";


    // constants: names
    private static final String orderNameBrief = "B";
    private static final String orderNameFull = "Build";
    private static final transient String orderFormatString = Utils
            .getLocalString(BUILD_FORMAT);


    /**
     * Creates a Build order
     */
    protected Build(final Power power, final Location src, final Type srcUnit) {
        super(power, src, srcUnit);
    }// Build()

    /**
     * Creates a Build order
     */
    protected Build() {
    }// Build()

    @Override
    public String getFullName() {
        return orderNameFull;
    }// getName()

    @Override
    public String getBriefName() {
        return orderNameBrief;
    }// getBriefName()


    @Override
    public String getDefaultFormat() {
        return orderFormatString;
    }// getFormatBrief()


    @Override
    public String toBriefString() {
        final StringBuffer sb = new StringBuffer(
                String.format("%s: %s %s ", power, orderNameBrief,
                        srcUnitType.getShortName()));
        src.appendBrief(sb);

        return sb.toString();
    }// toBriefString()


    @Override
    public String toFullString() {
        final StringBuffer sb = new StringBuffer(
                String.format("%s: %s %s ", power, orderNameFull,
                        srcUnitType.getFullName()));
        src.appendFull(sb);

        return sb.toString();
    }// toFullString()


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Build) {
            if (super.equals(obj)) {
                return true;
            }
        }
        return false;
    }// equals()


    /**
     * Builds:
     * <p>
     * Builds are valid if:
     * <ol>
     * <li>supply center is owned by this power
     * <li>supply centers is home to this power
     * <li>province contains no unit
     * <li>can build unit of that type in said province
     * <li>if fleet specified, coast must be valid
     * <li>default to army if no type specified AND supply center is inland
     * otherwise build fails.
     * </ol>
     * <p>
     * Validation is different than that of most other orders, since there
     * is no unit present.
     * <p>
     * If builds not in home supply centers are allowed, restriction #2 above is waived.
     * <p>
     * WARNING: if the "build in any owned sc if one (or more) home sc is owned" rule is
     * in place, this method does NOT currently check and reject the order if no home
     * supply centers are owned. However, no units are allowed to be built in that case
     * anyway, so the adjustments should be 0.
     * <p>
     * NOTE: validate() cannot take care of state-dependent order problems (since validate()
     * cannot know what other orders have been submitted), such as if too many
     * build orders are submitted. verify() could, but currently does not.
     */
    @Override
    public void validate(final TurnState state, final ValidationOptions valOpts,
                         final RuleOptions ruleOpts) throws OrderException {
        checkSeasonAdjustment(state, orderNameFull);
        checkPower(power, state, true);

        final Province province = src.getProvince();
        final Position position = state.getPosition();

        // basic
        if (position.hasUnit(province)) {
            throw new OrderException(Utils.getLocalString(BUILD_EXISTINGUNIT));
        }
        if (power != position.getSupplyCenterOwner(province).orElse(null)) {
            throw new OrderException(
                    Utils.getLocalString(BUILD_OWNED_SUPPLY, power));
        }
        if (power != position.getSupplyCenterHomePower(province)
                .orElse(null) && ruleOpts.getOptionValue(
                Option.OPTION_BUILDS) == OptionValue.VALUE_BUILDS_HOME_ONLY) {
            throw new OrderException(Utils.getLocalString(BUILD_HOME_SUPPLY));
        }

        // undefined types assumed to be Army
        if (srcUnitType == Type.UNDEFINED) {
            srcUnitType = Type.ARMY;
        }

        // disallow wing units, if wing unit option prohibited
        if (srcUnitType == Type.WING && ruleOpts.getOptionValue(
                Option.OPTION_WINGS) == OptionValue.VALUE_WINGS_DISABLED) {
            throw new OrderException(
                    Utils.getLocalString(BUILD_WING_PROHIBITED));
        }

        src = src.getValidated(srcUnitType);

        // validate Borders
        final Border border = src.getProvince()
                .getTransit(src, srcUnitType, state.getPhase(), getClass())
                .orElse(null);
        if (border != null) {
            throw new OrderException(
                    Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(),
                            border.getDescription()));
        }

        // if coast is still undefined after validation, error!
        if (src.getCoast() == Coast.UNDEFINED) {
            throw new OrderException(Utils.getLocalString(BUILD_MULTICOAST));
        }

        // not much else to validate; adjudiator must take care of tricky situations.
        // such as too many or too few build orders
    }// validate()


    /**
     * Empty method: Build orders do not require verification.
     */
    @Override
    public void verify(final Adjudicator adjudicator) {
        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()

    /**
     * Empty method: Build orders do not require dependency determination.
     */
    @Override
    public void determineDependencies(final Adjudicator adjudicator) {
    }


    /**
     * Build orders are always successful.
     * <p>
     * If there are too few build orders, that is ok.
     * If there are too many build orders, extra build orders
     * must be discarded by the Adjustment adjudicator as appropriate.
     * <p>
     * Extra build orders are NOT considered in the evaluate() method here.
     */
    @Override
    public void evaluate(final Adjudicator adjudicator) {
        Log.println("--- evaluate() dip.order.Build ---");
        Log.println("   order: ", this);

        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            thisOS.setEvalState(Tristate.SUCCESS);
        }
    }// evaluate()


}// class Build


