//
// 	@(#)Convoy.java		4/2002
//
// 	Copyright 2002 Zachary DelProposto. All rights reserved.
// 	Use is subject to license terms.
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
import dip.order.result.OrderResult.ResultType;
import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;
import dip.world.Border;
import dip.world.Location;
import dip.world.Path;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.RuleOptions;
import dip.world.TurnState;
import dip.world.Unit;
import dip.world.Unit.Type;

import java.util.Optional;

/**
 * Implementation of the Convoy order.
 */

public class Convoy extends Order {
    // il8n constants
    private static final String CONVOY_SEA_FLEETS = "CONVOY_SEA_FLEETS";
    private static final String CONVOY_ONLY_ARMIES = "CONVOY_ONLY_ARMIES";
    private static final String CONVOY_NO_ROUTE = "CONVOY_NO_ROUTE";
    private static final String CONVOY_VER_NOMOVE = "CONVOY_VER_NOMOVE";
    private static final String CONVOY_FORMAT = "CONVOY_FORMAT";
    private static final String CONVOY_SELF_ILLEGAL = "CONVOY_SELF_ILLEGAL";
    private static final String CONVOY_TO_SAME_PROVINCE = "CONVOY_TO_SAME_PROVINCE";

    // constants: names
    private static final String orderNameBrief = "C";
    private static final String orderNameFull = "Convoy";
    private static final transient String orderFormatString = Utils
            .getLocalString(CONVOY_FORMAT);

    // instance variables
    protected Location convoySrc;
    protected Location convoyDest;
    protected Type convoyUnitType;
    protected Power convoyPower;

    /**
     * Creates a Convoy order
     */
    protected Convoy(final Power power, final Location src, final Type srcUnit,
                     final Location convoySrc, final Power convoyPower,
                     final Type convoyUnitType, final Location convoyDest) {
        super(power, src, srcUnit);

        if (convoySrc == null || convoyUnitType == null || convoyDest == null) {
            throw new IllegalArgumentException("null argument(s)");
        }

        this.convoySrc = convoySrc;
        this.convoyUnitType = convoyUnitType;
        this.convoyPower = convoyPower;
        this.convoyDest = convoyDest;
    }// Convoy()


    /**
     * Creates a Convoy order
     */
    protected Convoy() {
    }// Convoy()


    /**
     * Returns the Location of the Unit to be Convoyed
     */
    public Location getConvoySrc() {
        return convoySrc;
    }

    /**
     * Returns the Unit Type of the Unit to be Convoyed
     * <b>Warning:</b> this can be null, if no unit type was set, and
     * no strict validation was performed (via <code>validate()</code>).
     */
    public Type getConvoyUnitType() {
        return convoyUnitType;
    }

    /**
     * Returns the Power of the Unit we are Convoying.
     * <b>Warning:</b> this can be null, if no unit type was set, and
     * no strict validation was performed (via <code>validate()</code>).
     * <p>
     * <b>Important Note:</b> This also may be null only when a saved game
     * from 1.5.1 or prior versions are loaded into a recent version,
     * since prior versions did not support this field.
     */
    public Power getConvoyedPower() {
        return convoyPower;
    }


    /**
     * Returns the Location of the Convoy destination
     */
    public Location getConvoyDest() {
        return convoyDest;
    }


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
        final StringBuffer sb = new StringBuffer(64);

        appendBrief(sb);
        sb.append(' ');
        sb.append(orderNameBrief);
        sb.append(' ');
        sb.append(convoyUnitType.getShortName());
        sb.append(' ');
        convoySrc.appendBrief(sb);
        sb.append('-');
        convoyDest.appendBrief(sb);

        return sb.toString();
    }// toBriefString()


    @Override
    public String toFullString() {
        final StringBuffer sb = new StringBuffer(128);

        appendFull(sb);
        sb.append(' ');
        sb.append(orderNameFull);
        sb.append(' ');
        sb.append(convoyUnitType.getFullName());
        sb.append(' ');
        convoySrc.appendFull(sb);
        sb.append(" -> ");
        convoyDest.appendFull(sb);

        return sb.toString();
    }// toFullString()


    public boolean equals(final Object obj) {
        if (obj instanceof Convoy) {
            final Convoy convoy = (Convoy) obj;
            if (super.equals(convoy) && convoySrc
                    .equals(convoy.convoySrc) && convoyUnitType == convoy.convoyUnitType && convoyDest
                    .equals(convoy.convoyDest)) {
                return true;
            }
        }
        return false;
    }// equals()


    @Override
    public void validate(final TurnState state, final ValidationOptions valOpts,
                         final RuleOptions ruleOpts) throws OrderException {
        // v.0: 	check phase, basic validation
        checkSeasonMovement(state, orderNameFull);
        checkPower(power, state, true);
        super.validate(state, valOpts, ruleOpts);

        if (valOpts.getOption(ValidationOptions.KEY_GLOBAL_PARSING)
                .equals(ValidationOptions.VALUE_GLOBAL_PARSING_STRICT)) {
            final Position position = state.getPosition();
            final Province srcProvince = src.getProvince();

            // v.1: src unit type must be a fleet, in a body of water
            // OR in a convoyable coast.
            if (srcUnitType != Type.FLEET || !srcProvince
                    .isSea() && !srcProvince.isConvoyableCoast()) {
                throw new OrderException(
                        Utils.getLocalString(CONVOY_SEA_FLEETS));
            }

            // validate Borders
            final Optional<Border> border = src.getProvince()
                    .getTransit(src, srcUnitType, state.getPhase(), getClass());
            if (border.isPresent()) {
                throw new OrderException(
                        Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(),
                                border.get().getDescription()));
            }

            // v.2: 	a) type-match unit type with current state, and unit must exist
            // 		b) unit type must be ARMY
            final Unit convoyUnit = position.getUnit(convoySrc.getProvince())
                    .orElse(null);
            convoyUnitType = getValidatedUnitType(convoySrc.getProvince(),
                    convoyUnitType, convoyUnit);
            if (convoyUnitType != Type.ARMY) {
                throw new OrderException(
                        Utils.getLocalString(CONVOY_ONLY_ARMIES));
            }

            // v.3.a: validate locations: convoySrc & convoyDest
            convoySrc = convoySrc
                    .getValidatedAndDerived(convoyUnitType, convoyUnit);
            convoyDest = convoyDest.getValidated(convoyUnitType);

            // v.3.b: convoying to self (if we are in a convoyable coast) is illegal!
            if (srcProvince.isConvoyableCoast() && src
                    .isProvinceEqual(convoyDest)) {
                throw new OrderException(
                        Utils.getLocalString(CONVOY_SELF_ILLEGAL));
            }

            // v.3.c: origin/destination of convoy must not be same province.
            if (convoySrc.isProvinceEqual(convoyDest)) {
                throw new OrderException(
                        Utils.getLocalString(CONVOY_TO_SAME_PROVINCE));
            }

            // v.4:	a *theoretical* convoy route must exist between
            //		convoySrc and convoyDest
            final Path path = new Path(position);
            if (!path.isPossibleConvoyRoute(convoySrc, convoyDest)) {
                throw new OrderException(Utils.getLocalString(CONVOY_NO_ROUTE,
                        convoySrc.toLongString(), convoyDest.toLongString()));
            }

            // validate Borders
            final Border border1 = convoySrc.getProvince()
                    .getTransit(convoySrc, convoyUnitType, state.getPhase(),
                            getClass()).orElse(null);
            if (border1 != null) {
                throw new OrderException(
                        Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(),
                                border1.getDescription()));
            }

            final Border border0 = convoyDest.getProvince()
                    .getTransit(convoyDest, convoyUnitType, state.getPhase(),
                            getClass()).orElse(null);
            if (border0 != null) {
                throw new OrderException(
                        Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(),
                                border0.getDescription()));
            }
        }
    }// validate();


    /**
     * Checks for matching Move orders.
     */
    @Override
    public void verify(final Adjudicator adjudicator) {
        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            // check for a matching move order.
            //
            // note that the move must have its isByConvoy() flag set, so we don't
            // kidnap armies that prefer not to be convoyed.
            boolean foundMatchingMove = false;

            final OrderState matchingOS = adjudicator
                    .findOrderStateBySrc(getConvoySrc());
            if (matchingOS != null) {
                if (matchingOS.getOrder() instanceof Move) {
                    final Move convoyedMove = (Move) matchingOS.getOrder();

                    // check that Move has been verified; if it has not,
                    // we should just immediately verify it (though we could
                    // wait for the adjudicator to do so).
                    if (!matchingOS.isVerified()) {
                        convoyedMove.verify(adjudicator);

                        // but if it doesn't verify, then we have a
                        // dependency-error.
                        if (!matchingOS.isVerified()) {
                            throw new IllegalStateException(
                                    "Verify dependency error.");
                        }
                    }

                    if (convoyedMove.isConvoying() && getConvoyDest()
                            .isProvinceEqual(convoyedMove.getDest())) {
                        foundMatchingMove = true;
                    }
                }
            }

            if (!foundMatchingMove) {
                thisOS.setEvalState(Tristate.FAILURE);
                adjudicator.addResult(thisOS, ResultType.FAILURE,
                        Utils.getLocalString(CONVOY_VER_NOMOVE));
            }
        }

        thisOS.setVerified(true);
    }// verify()

    /**
     * Dependencies for a Convoy order are:
     * <ol>
     * <li>Moves to this space (to determine dislodgement)
     * <li>Supports to this space (only considered if attacked, to prevent dislodgement)
     * </ol>
     */
    @Override
    public void determineDependencies(final Adjudicator adjudicator) {
        addSupportsOfAndMovesToSource(adjudicator);
    }// determineDependencies()


    /**
     * Convoy order evaluation logic
     */
    @Override
    public void evaluate(final Adjudicator adjudicator) {
        Log.println("--- evaluate() dip.order.Convoy ---");

        final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        // calculate support
        thisOS.setDefMax(thisOS.getSupport(false));
        thisOS.setDefCertain(thisOS.getSupport(true));

        if (Log.isLogging()) {
            Log.println("   order: ", this);
            Log.println("   initial evalstate: ", thisOS.getEvalState());
            Log.println("     def-max: ", thisOS.getDefMax());
            Log.println("    def-cert: ", thisOS.getDefCertain());
            Log.println("  # supports: ", thisOS.getDependentSupports().size());
        }

        // determine evaluation state. This is important for Convoy orders, since
        // moves depend upon them. If we cannot determine, we will remain uncertain.
        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            // if for some reason we were dislodged, but not marked as failure,
            // mark as failure.
            if (thisOS.getDislodgedState() == Tristate.YES) {
                thisOS.setEvalState(Tristate.FAILURE);
                return;
            }

            // we will also succeed if there are *no* moves against us, or if all the
            // moves against us have failed.
            final boolean isSuccess = thisOS.getDependentMovesToSource()
                    .stream().allMatch(aDepMovesToSrc -> aDepMovesToSrc
                            .getEvalState() == Tristate.FAILURE);
            if (isSuccess) {
                thisOS.setEvalState(Tristate.SUCCESS);
                thisOS.setDislodgedState(Tristate.NO);
            }
        }

        Log.println("  final evalState: ", thisOS.getEvalState());
    }// evaluate()

}// class Convoy
