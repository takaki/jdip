//
//  @(#)GUIBuild.java	12/2002
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
package dip.gui.order;

import dip.gui.map.DefaultMapRenderer2;
import dip.gui.map.MapMetadata;
import dip.gui.map.MapMetadata.SymbolSize;
import dip.gui.map.SVGUtils;
import dip.misc.Utils;
import dip.order.Build;
import dip.order.Orderable;
import dip.process.Adjustment;
import dip.process.Adjustment.AdjustmentInfo;
import dip.world.*;
import dip.world.RuleOptions.Option;
import dip.world.RuleOptions.OptionValue;
import dip.world.Unit.Type;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;

/**
 * GUIOrder implementation of Build order.
 */
public class GUIBuild extends Build implements GUIOrder {
    // BuildParameter constants
    /**
     * Required. Used to set build Unit.Type. Associated value must be a Unit.Type
     */
    public transient static final BuildParameter BUILD_UNIT = new BuildParameter(
            "BUILD_UNIT");

    // i18n keys
    private static final String BUILD_FLEET_OK = "GUIBuild.ok.fleet";
    private static final String BUILD_ARMY_OK = "GUIBuild.ok.army";
    private static final String BUILD_WING_OK = "GUIBuild.ok.wing";
    private static final String NOBUILD_FLEET_LANDLOCKED = "GUIBuild.bad.fleet.landlocked";
    private static final String NOBUILD_NO_ARMY_IN_SEA = "GUIBuild.bad.army_in_sea";
    private static final String NOBUILD_NO_UNIT_SELECTED = "GUIBuild.bad.no_unit_selected";
    private static final String NOBUILD_MUST_BE_AN_OWNED_SC = "GUIBuild.bad.must_own_sc";
    private static final String NOBUILD_NOT_OWNED_HOME_SC = "GUIBuild.bad.now_owned_home_sc";
    private static final String NOBUILD_NEED_ONE_OWNED_SC = "GUIBuild.bad.need_one_owned_sc";
    private static final String NOBUILD_NO_BUILDS_AVAILABLE = "GUIBuild.bad.no_builds_available";
    private static final String NOBUILD_SC_NOT_CONTROLLED = "GUIBuild.bad.sc_not_controlled";
    private static final String NOBUILD_UNIT_PRESENT = "GUIBuild.bad.unit_already_present";
    private static final String NOBUILD_UNOWNED_SC = "GUIBuild.bad.unowned_sc";

    // instance variables
    private transient final static int REQ_LOC = 1;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;

    /**
     * Creates a GUIBuild
     */
    protected GUIBuild() {
        super();
    }// GUIBuild()

    /**
     * Creates a GUIBuild
     */
    protected GUIBuild(final Power power, final Location source, final Type sourceUnitType) {
        super(power, source, sourceUnitType);
    }// GUIBuild()


    /**
     * This only accepts Build orders. All others will throw an IllegalArgumentException.
     */
    @Override
    public void deriveFrom(final Orderable order) {
        if (!(order instanceof Build)) {
            throw new IllegalArgumentException();
        }

        final Build build = (Build) order;
        power = build.getPower();
        src = build.getSource();
        srcUnitType = build.getSourceUnitType();

        // set completed
        currentLocNum = REQ_LOC;
    }// deriveFrom()


    @Override
    public boolean testLocation(final StateInfo stateInfo, final Location location,
                                final StringBuffer sb) {
        sb.setLength(0);

        if (isComplete()) {
            sb.append(Utils.getLocalString(GUIOrder.COMPLETE, getFullName()));
            return false;
        }


        final Position position = stateInfo.getPosition();
        final Province province = location.getProvince();

        if (province.hasSupplyCenter()) {
            final Power SCOwner = position.getSupplyCenterOwner(province).orElse(null);

            // general screening, applicable to all build options
            //
            if (SCOwner == null) {
                sb.append(Utils.getLocalString(NOBUILD_UNOWNED_SC));
                return false;
            }

            if (position.hasUnit(province)) {
                sb.append(Utils.getLocalString(NOBUILD_UNIT_PRESENT));
                return false;
            }

            if (!stateInfo.canIssueOrder(SCOwner)) {
                sb.append(Utils.getLocalString(NOBUILD_SC_NOT_CONTROLLED));
                return false;
            }

            // indicate if we have no builds available
            //
            final AdjustmentInfo adjInfo = stateInfo.getAdjustmenInfoMap()
                    .get(SCOwner);
            if (adjInfo.getAdjustmentAmount() <= 0) {
                sb.append(Utils.getLocalString(NOBUILD_NO_BUILDS_AVAILABLE,
                        SCOwner.getName()));
                return false;
            }


            // build-option-specific, based upon RuleOptions
            //
            final RuleOptions ruleOpts = stateInfo.getRuleOptions();
            if (ruleOpts.getOptionValue(
                    Option.OPTION_BUILDS) == OptionValue.VALUE_BUILDS_ANY_OWNED) {
                return checkBuildUnit(stateInfo, province, location, sb);
            } else if (ruleOpts.getOptionValue(
                    Option.OPTION_BUILDS) == OptionValue.VALUE_BUILDS_ANY_IF_HOME_OWNED) {
                // check if we have ONE owned home supply center before buidling
                // in a non-home supply center.
                //
                if (SCOwner != position
                        .getSupplyCenterHomePower(province).orElse(null) && !position
                        .hasAnOwnedHomeSC(SCOwner)) {
                    sb.append(Utils.getLocalString(NOBUILD_NEED_ONE_OWNED_SC));
                    return false;    // failed
                }

                // we (probably) can build here
                return checkBuildUnit(stateInfo, province, location, sb);
            } else {
                // build only in owned HOME supply centers
                //
                if (SCOwner == position.getSupplyCenterHomePower(province).orElse(null)) {
                    // we (probably) can build here
                    return checkBuildUnit(stateInfo, province, location, sb);
                }

                // build failure.
                sb.append(Utils.getLocalString(NOBUILD_NOT_OWNED_HOME_SC));
                return false;
            }
        } else {
            sb.append(Utils.getLocalString(NOBUILD_MUST_BE_AN_OWNED_SC));
            return false;
        }

        // NO return here: thus we must appropriately exit within an if/else block above.
    }// testLocation()


    @Override
    public boolean clearLocations() {
        if (isComplete()) {
            return false;
        }

        currentLocNum = 0;
        power = null;
        src = null;
        //srcUnitType: not cleared

        return true;
    }// clearLocations()


    @Override
    public boolean setLocation(final StateInfo stateInfo, final Location location,
                               final StringBuffer sb) {
        if (testLocation(stateInfo, location, sb)) {
            currentLocNum++;

            src = new Location(location.getProvince(), location.getCoast());
            power = stateInfo.getPosition()
                    .getSupplyCenterOwner(location.getProvince()).orElse(null);

            // srcUnitType: already defined
            assert srcUnitType != null;

            sb.setLength(0);
            sb.append(Utils.getLocalString(GUIOrder.COMPLETE, getFullName()));
            return true;
        }

        return false;
    }// setLocation()


    @Override
    public boolean isComplete() {
        assert currentLocNum <= getNumRequiredLocations();
        return currentLocNum == getNumRequiredLocations();
    }// isComplete()

    @Override
    public int getNumRequiredLocations() {
        return REQ_LOC;
    }

    @Override
    public int getCurrentLocationNum() {
        return currentLocNum;
    }

    /**
     * Used to set what type of Unit we are building. Value must be a Unit.Type
     */
    @Override
    public void setParam(final Parameter param, final Object value) {
        if (param != BUILD_UNIT || !(value instanceof Type)) {
            throw new IllegalArgumentException();
        }

        srcUnitType = (Type) value;
    }// setParam()


    /**
     * Used to set what type of Unit we are building.
     */
    @Override
    public Object getParam(final Parameter param) {
        if (param != BUILD_UNIT) {
            throw new IllegalArgumentException();
        }

        return srcUnitType;
    }// getParam()


    @Override
    public void removeFromDOM(final MapInfo mapInfo) {
        if (group != null) {
            final SVGGElement powerGroup = mapInfo
                    .getPowerSVGGElement(power, LAYER_HIGHEST);
            GUIOrderUtils.removeChild(powerGroup, group);
            group = null;
        }
    }// removeFromDOM()


    /**
     * Places a unit in the desired area.
     */
    @Override
    public void updateDOM(final MapInfo mapInfo) {
        // if we are not displayable, we exit, after remove the order (if
        // it was created)
        if (!GUIOrderUtils.isDisplayable(power, mapInfo)) {
            removeFromDOM(mapInfo);
            return;
        }

        // determine if any change has occured. If no change has occured,
        // we will not change the DOM.
        //
        // we have nothing (yet) to check for change; isDependent() == false.
        // so just return if we have not been drawn.
        if (group != null) {
            return;
        }

        // there has been a change, if we are at this point.
        //

        // if we've not yet been created, we will create; if we've
        // already been created, we must remove the existing elements
        // in our group
        if (group == null) {
            // create group
            group = (SVGGElement) mapInfo.getDocument()
                    .createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI,
                            SVGConstants.SVG_G_TAG);

            mapInfo.getPowerSVGGElement(power, LAYER_HIGHEST)
                    .appendChild(group);
        } else {
            // remove group children
            GUIOrderUtils.deleteChildren(group);
        }

        // now, render the order
        // (no highlight + shadow required here)
        // no offset required
        //
        final SVGElement[] elements = drawOrder(mapInfo);
        for (SVGElement element : elements) {
            group.appendChild(element);
        }

        // draw 'failed' marker, if appropriate.
        if (!mapInfo.getTurnState().isOrderSuccessful(this)) {
            final SVGElement useElement = GUIOrderUtils
                    .createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
            group.appendChild(useElement);
        }
    }// updateDOM()


    private SVGElement[] drawOrder(final MapInfo mapInfo) {
        final MapMetadata mmd = mapInfo.getMapMetadata();
        final Point2D.Float center = mmd.getUnitPt(src.getProvince(), src.getCoast());

        // get 'integer' and float data
        final float radius = mmd.getOrderRadius(MapMetadata.EL_BUILD,
                mapInfo.getSymbolName(srcUnitType));

        // calculate failPt.
        failPt = new Point2D.Float(center.x + radius, center.y - radius);


        // A BUILD consists of the Built Unit ontop of a BuildUnit symbol
        // elements added in drawing order (thus BuildUnit must come first,
        // otherwise the unit built will be obscured by the BuilUnit symbol).
        //
        final SVGElement[] elements = new SVGElement[2];

        // BuildUnit symbol
        SymbolSize symbolSize = mmd
                .getSymbolSize(DefaultMapRenderer2.SYMBOL_BUILDUNIT);

        elements[0] = SVGUtils.createUseElement(mapInfo.getDocument(),
                "#" + DefaultMapRenderer2.SYMBOL_BUILDUNIT, null,    // no ID
                null,    // no special style
                center.x, center.y, symbolSize);

        // Unit symbol
        final String symbolName = mapInfo.getSymbolName(srcUnitType);
        symbolSize = mmd.getSymbolSize(symbolName);

        elements[1] = SVGUtils
                .createUseElement(mapInfo.getDocument(), "#" + symbolName, null,
                        // no ID
                        mapInfo.getUnitCSS(power),    // unit style
                        center.x, center.y, symbolSize);

        return elements;
    }// drawOrder()


    @Override
    public boolean isDependent() {
        return false;
    }


    /**
     * All build options require that appropriate units are built in
     * appropriate places (e.g., armies can't go in the water, fleets
     * can't go in landlocked provinces, fleets must have coasts specified
     * if required, etc.). This method takes care of that.
     * <p>
     * returns false if we cannot build here.
     */
    private boolean checkBuildUnit(final StateInfo stateInfo, final Province province,
                                   final Location loc, final StringBuffer sb) {
        if (srcUnitType == null) {
            sb.append(Utils.getLocalString(NOBUILD_NO_UNIT_SELECTED));
            return false;
        }

        if (srcUnitType == Type.ARMY) {
            if (province.isSea()) {
                sb.append(Utils.getLocalString(NOBUILD_NO_ARMY_IN_SEA));
                return false;
            } else {
                // check borders
                if (!GUIOrderUtils.checkBorder(this, loc, srcUnitType,
                        stateInfo.getPhase(), sb)) {
                    return false;
                }

                sb.append(Utils.getLocalString(BUILD_ARMY_OK));
                return true;
            }
        } else if (srcUnitType == Type.FLEET) {
            if (province.isLandLocked()) {
                sb.append(Utils.getLocalString(NOBUILD_FLEET_LANDLOCKED));
                return false;
            } else {
                // check borders
                if (!GUIOrderUtils.checkBorder(this, loc, srcUnitType,
                        stateInfo.getPhase(), sb)) {
                    return false;
                }

                sb.append(Utils.getLocalString(BUILD_FLEET_OK));
                return true;
            }
        } else if (srcUnitType == Type.WING) {
            // check borders
            if (!GUIOrderUtils
                    .checkBorder(this, loc, srcUnitType, stateInfo.getPhase(),
                            sb)) {
                return false;
            }

            sb.append(Utils.getLocalString(BUILD_WING_OK));
            return true;
        }

        // this should not occur.
        sb.append("** Unknown unit type! **");
        return false;
    }// checkBuildUnit()


    /**
     * Typesafe Enumerated Parameter class for setting
     * required Build parameters.
     */
    protected static class BuildParameter extends Parameter {
        /**
         * Creates a BuildParameter
         */
        public BuildParameter(final String name) {
            super(name);
        }// BuildParameter()
    }// nested class BuildParameter


}// class GUIBuild
