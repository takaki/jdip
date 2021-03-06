//
//  @(#)GUIRetreat.java		12/2002
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

import dip.gui.map.MapMetadata;
import dip.misc.Utils;
import dip.order.Orderable;
import dip.order.Retreat;
import dip.order.ValidationOptions;
import dip.process.RetreatChecker;
import dip.world.Location;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.Unit;
import dip.world.Unit.Type;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGGElement;
import org.w3c.dom.svg.SVGLineElement;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * GUIOrder subclass of Retreat order.
 */
public class GUIRetreat extends Retreat implements GUIOrder {
    // i18n keys
    private final static String UNIT_MUST_DISBAND = "GUIRetreat.must_disband";
    private final static String CLICK_TO_SET_DEST = "GUIRetreat.set_dest";
    private final static String CANNOT_RETREAT_HERE = "GUIRetreat.bad_dest";
    private final static String VALID_RETREAT_LOCS = "GUIRetreat.valid_locs";

    // instance variables
    private transient static final int REQ_LOC = 2;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;


    /**
     * Creates a GUIRetreat
     */
    protected GUIRetreat() {
        super();
    }// GUIRetreat()

    /**
     * Creates a GUIRetreat
     */
    protected GUIRetreat(final Power power, final Location source, final Type sourceUnitType,
                         final Location dest) {
        super(power, source, sourceUnitType, dest);
    }// GUIRetreat()

    /**
     * This only accepts Retreat orders. All others will throw an IllegalArgumentException.
     */
    @Override
    public void deriveFrom(final Orderable order) {
        if (!(order instanceof Retreat)) {
            throw new IllegalArgumentException();
        }

        final Retreat retreat = (Retreat) order;
        power = retreat.getPower();
        src = retreat.getSource();
        srcUnitType = retreat.getSourceUnitType();
        dest = retreat.getDest();

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

        if (currentLocNum == 0) {
            // set Retreat source; remember, we are using the *dislodged* unit.
            //
            // we require a dislodged unit present. We will check unit ownership too, if appropriate
            final Unit unit = position.getDislodgedUnit(province).orElse(null);
            if (unit != null) {
                if (!stateInfo.canIssueOrder(unit.getPower())) {
                    sb.append(Utils.getLocalString(GUIOrder.NOT_OWNER,
                            unit.getPower()));
                    return false;
                }

                // determine valid retreat locations
                final RetreatChecker rc = stateInfo.getRetreatChecker();
                final List<Location> retreatLocs = rc.getValidLocations(
                        new Location(province, unit.getCoast()));

                // if we have no valid retreat locations, inform that we must disband
                if (retreatLocs.isEmpty()) {
                    sb.append(Utils.getLocalString(UNIT_MUST_DISBAND));
                    return false;
                }

                // check borders
                if (!GUIOrderUtils.checkBorder(this,
                        new Location(province, unit.getCoast()), unit.getType(),
                        stateInfo.getPhase(), sb)) {
                    return false;
                }

                // we can retreat. Inform user of our retreat options.
                sb.append(Utils.getLocalString(GUIOrder.CLICK_TO_ISSUE,
                        getFullName()));
                sb.append(getRetLocText(retreatLocs));
                return true;
            }

            // no *dislodged* unit in province
            sb.append(Utils.getLocalString(GUIOrder.NO_DISLODGED_UNIT,
                    getFullName()));
            return false;
        } else if (currentLocNum == 1) {
            // set retreat destination
            // - If we are not validating, any destination is acceptable (even source)
            // - If we are validating, we check that the retreat is adjacent or a possible convoy
            //		route exists.
            //
            if (stateInfo.getValidationOptions()
                    .getOption(ValidationOptions.KEY_GLOBAL_PARSING)
                    .equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE)) {
                // lenient parsing enabled; we'll take anything!
                sb.append(Utils.getLocalString(CLICK_TO_SET_DEST));
                return true;
            }

            // strict parsing is enabled. We are more selective.
            // check destination against possible retreat locations.
            final RetreatChecker rc = stateInfo.getRetreatChecker();
            final List<Location> retreatLocs = rc
                    .getValidLocations(getSource());

            for (Location retreatLoc : retreatLocs) {
                if (retreatLoc.getProvince() == province) {
                    sb.append(Utils.getLocalString(CLICK_TO_SET_DEST));
                    return true;
                }
            }

            // check borders
            if (!GUIOrderUtils.checkBorder(this, location, srcUnitType,
                    stateInfo.getPhase(), sb)) {
                return false;
            }

            // not a valid retreat destination.
            sb.append(Utils.getLocalString(CANNOT_RETREAT_HERE));
            sb.append(getRetLocText(retreatLocs));
            return false;
        } else {
            // should not occur.
            throw new IllegalStateException();
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
        srcUnitType = null;
        dest = null;

        return true;
    }// clearLocations()


    @Override
    public boolean setLocation(final StateInfo stateInfo, final Location location,
                               final StringBuffer sb) {
        if (isComplete()) {
            return false;
        }

        if (testLocation(stateInfo, location, sb)) {
            if (currentLocNum == 0) {
                final Unit unit = stateInfo.getPosition()
                        .getDislodgedUnit(location.getProvince()).orElse(null);
                src = new Location(location.getProvince(), unit.getCoast());
                power = unit.getPower();
                srcUnitType = unit.getType();
                currentLocNum++;
                return true;
            } else if (currentLocNum == 1) {
                dest = new Location(location.getProvince(),
                        location.getCoast());

                sb.setLength(0);
                sb.append(
                        Utils.getLocalString(GUIOrder.COMPLETE, getFullName()));
                currentLocNum++;
                return true;
            }
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
     * Always throws an IllegalArgumentException
     */
    @Override
    public void setParam(final Parameter param, final Object value) {
        throw new IllegalArgumentException();
    }

    /**
     * Always throws an IllegalArgumentException
     */
    @Override
    public Object getParam(final Parameter param) {
        throw new IllegalArgumentException();
    }


    @Override
    public void removeFromDOM(final MapInfo mapInfo) {
        if (group != null) {
            final SVGGElement powerGroup = mapInfo
                    .getPowerSVGGElement(power, LAYER_TYPICAL);
            GUIOrderUtils.removeChild(powerGroup, group);
            group = null;
        }
    }// removeFromDOM()


    /**
     * Draws a line with an arrow. Unlife a Move, we are not dependent.
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

            mapInfo.getPowerSVGGElement(power, LAYER_TYPICAL)
                    .appendChild(group);
        } else {
            // remove group children
            GUIOrderUtils.deleteChildren(group);
        }

        // now, render the order
        //
        SVGElement element;

        // create hilight line
        final String cssStyle = mapInfo.getMapMetadata()
                .getOrderParamString(MapMetadata.EL_RETREAT,
                        MapMetadata.ATT_HILIGHT_CLASS);
        if (!cssStyle.equalsIgnoreCase("none")) {
            final float offset = mapInfo.getMapMetadata()
                    .getOrderParamFloat(MapMetadata.EL_RETREAT,
                            MapMetadata.ATT_HILIGHT_OFFSET);
            element = drawOrder(mapInfo, offset, false);
            GUIOrderUtils.makeHilight(element, mapInfo.getMapMetadata(),
                    MapMetadata.EL_RETREAT);
            group.appendChild(element);
        }

        // create real line
        element = drawOrder(mapInfo, 0, true);
        GUIOrderUtils.makeStyled(element, mapInfo.getMapMetadata(),
                MapMetadata.EL_RETREAT, power);
        group.appendChild(element);

        // draw 'failed' marker, if appropriate.
        if (!mapInfo.getTurnState().isOrderSuccessful(this)) {
            final SVGElement useElement = GUIOrderUtils
                    .createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
            group.appendChild(useElement);
        }
    }// updateDOM()


    private SVGElement drawOrder(final MapInfo mapInfo, final float offset,
                                 final boolean addMarker) {
        final MapMetadata mmd = mapInfo.getMapMetadata();
        final Point2D.Float ptFrom = mmd
                .getDislodgedUnitPt(src.getProvince(), src.getCoast());
        final Point2D.Float ptTo = mmd.getUnitPt(dest.getProvince(), dest.getCoast());

        // respect radius, if there is a unit present in destination.
        Point2D.Float newPtTo = ptTo;
        final Position position = mapInfo.getTurnState().getPosition();
        if (position.hasUnit(dest.getProvince())) {
            final Type destUnitType = position.getUnit(dest.getProvince())
                    .orElse(null).getType();
            final float r = mmd.getOrderRadius(MapMetadata.EL_RETREAT,
                    mapInfo.getSymbolName(destUnitType));
            newPtTo = GUIOrderUtils.getLineCircleIntersection(ptFrom.x + offset,
                    ptFrom.y + offset, ptTo.x + offset, ptTo.y + offset,
                    ptTo.x + offset, ptTo.y + offset, r);
        }

        // calculate (but don't yet use) failPt
        failPt = GUIOrderUtils
                .getLineMidpoint(ptFrom.x, ptFrom.y, newPtTo.x, newPtTo.y);

        final SVGLineElement line = (SVGLineElement) mapInfo.getDocument()
                .createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI,
                        SVGConstants.SVG_LINE_TAG);

        line.setAttributeNS(null, SVGConstants.SVG_X1_ATTRIBUTE,
                GUIOrderUtils.floatToString(ptFrom.x + offset));
        line.setAttributeNS(null, SVGConstants.SVG_Y1_ATTRIBUTE,
                GUIOrderUtils.floatToString(ptFrom.y + offset));
        line.setAttributeNS(null, SVGConstants.SVG_X2_ATTRIBUTE,
                GUIOrderUtils.floatToString(newPtTo.x + offset));
        line.setAttributeNS(null, SVGConstants.SVG_Y2_ATTRIBUTE,
                GUIOrderUtils.floatToString(newPtTo.y + offset));

        // style
        line.setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE,
                mmd.getOrderParamString(MapMetadata.EL_RETREAT,
                        MapMetadata.ATT_STROKESTYLE));

        // marker
        if (addMarker || offset != 0.0f) {
            GUIOrderUtils.addMarker(line, mmd, MapMetadata.EL_RETREAT);
        }

        // end
        return line;
    }// drawOrder()


    @Override
    public boolean isDependent() {
        return false;
    }


    /**
     * Generate text message containing valid retreat locations (if any). Assumes non-zero retreatLocs length.
     */
    private String getRetLocText(final List<Location> retreatLocs) {
        if (retreatLocs.isEmpty()) {
            throw new IllegalStateException();
        } else {
            final StringBuffer tmp = new StringBuffer(64);
            tmp.append(Utils.getLocalString(VALID_RETREAT_LOCS));

            tmp.append(retreatLocs.get(0).getProvince().getShortName());
            for (int i = 1; i < retreatLocs.size(); i++) {
                tmp.append(", ");
                tmp.append(retreatLocs.get(i).getProvince().getShortName());
            }

            tmp.append('.');

            return tmp.toString();
        }
    }// getRetLocText()


}// class GUIRetreat
