//
//  @(#)GUIHold.java	12/2002
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
import dip.order.Hold;
import dip.order.Orderable;
import dip.world.Location;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.Unit;
import dip.world.Unit.Type;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGGElement;
import org.w3c.dom.svg.SVGPolygonElement;

import java.awt.geom.Point2D;

/**
 * GUIOrder subclass of Hold order.
 */
public class GUIHold extends Hold implements GUIOrder {
    private static final Logger LOG = LoggerFactory.getLogger(GUIHold.class);
    // i18n keys

    // instance variables
    private static final transient int REQ_LOC = 1;
    private transient int currentLocNum;
    private transient int numSupports = -1;    // WARNING: this will become '0' when de-serialized; not -1
    private transient Point2D.Float failPt;
    private transient SVGGElement group;


    /**
     * Creates a GUIHold
     */
    protected GUIHold() {
    }// GUIHold()

    /**
     * Creates a GUIHold
     */
    protected GUIHold(final Power power, final Location source,
                      final Type sourceUnitType) {
        super(power, source, sourceUnitType);
    }// GUIHold()


    /**
     * This only accepts Hold orders. All others will throw an IllegalArgumentException.
     */
    @Override
    public void deriveFrom(final Orderable order) {
        if (!(order instanceof Hold)) {
            throw new IllegalArgumentException();
        }

        final Hold hold = (Hold) order;
        power = hold.getPower();
        src = hold.getSource();
        srcUnitType = hold.getSourceUnitType();

        // set completed
        currentLocNum = REQ_LOC;
    }// deriveFrom()

    @Override
    public boolean testLocation(final StateInfo stateInfo,
                                final Location location,
                                final StringBuffer sb) {
        sb.setLength(0);

        if (isComplete()) {
            sb.append(Utils.getLocalString(GUIOrder.COMPLETE, getFullName()));
            return false;
        }


        final Position position = stateInfo.getPosition();
        final Province province = location.getProvince();
        final Unit unit = position.getUnit(province).orElse(null);

        if (unit != null) {
            if (!stateInfo.canIssueOrder(unit.getPower())) {
                sb.append(Utils.getLocalString(GUIOrder.NOT_OWNER,
                        unit.getPower()));
                return false;
            }

            if (!GUIOrderUtils
                    .checkBorder(this, new Location(province, unit.getCoast()),
                            unit.getType(), stateInfo.getPhase(), sb)) {
                return false;
            }

            sb.append(Utils.getLocalString(GUIOrder.CLICK_TO_ISSUE,
                    getFullName()));
            return true;
        }

        // no unit in province
        sb.append(Utils.getLocalString(GUIOrder.NO_UNIT, getFullName()));
        return false;
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

        return true;
    }// clearLocations()

    @Override
    public boolean setLocation(final StateInfo stateInfo,
                               final Location location, final StringBuffer sb) {
        if (testLocation(stateInfo, location, sb)) {
            currentLocNum++;
            final Unit unit = stateInfo.getPosition()
                    .getUnit(location.getProvince()).orElse(null);
            src = new Location(location.getProvince(), unit.getCoast());
            power = unit.getPower();
            srcUnitType = unit.getType();

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
            LOG.debug("GUIHold: removeFromDOM(): group={}", group);
            final SVGGElement powerGroup = mapInfo
                    .getPowerSVGGElement(power, LAYER_TYPICAL);
            GUIOrderUtils.removeChild(powerGroup, group);
            group = null;
            numSupports = -1;
        }
    }// removeFromDOM()


    @Override
    public void updateDOM(final MapInfo mapInfo) {
        LOG.debug("GUIHold::updateDOM(): group,support: {}{}", group,
                numSupports);

        // if we are not displayable, we exit, after remove the order (if
        // it was created)
        if (!GUIOrderUtils.isDisplayable(power, mapInfo)) {
            LOG.debug("GUIHold::updateDOM(): not displayable.");
            removeFromDOM(mapInfo);
            return;
        }

        // determine if any change has occured. If no change has occured,
        // we will not change the DOM.
        //
        // check supports
        final int support = GUIOrderUtils
                .getMatchingSupportCount(mapInfo, src.getProvince(),
                        src.getProvince());
        if (numSupports == support && group != null) {
            LOG.debug(
                    "GUIHold::updateDOM(): no change. returning. calc support: {}",
                    support);
            return;    // no change
        }

        // we are only at this point if a change has occured.
        //
        numSupports = support;

        // if we've not yet been created, we will create; if we've
        // already been created, we must remove the existing elements
        // in our group
        if (group == null) {
            // create group
            LOG.debug("GUIHold::updateDOM(): creating group.");
            group = (SVGGElement) mapInfo.getDocument()
                    .createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI,
                            SVGConstants.SVG_G_TAG);

            mapInfo.getPowerSVGGElement(power, LAYER_TYPICAL)
                    .appendChild(group);
        } else {
            // remove group children
            LOG.debug("GUIHold::updateDOM(): removing group children.");
            GUIOrderUtils.deleteChildren(group);
        }

        // now, render the order
        //
        LOG.debug("GUIHold::updateDOM(): rendering order.");
        SVGElement element;

        // create hilight line
        final String cssStyle = mapInfo.getMapMetadata()
                .getOrderParamString(MapMetadata.EL_HOLD,
                        MapMetadata.ATT_HILIGHT_CLASS);
        if (!cssStyle.equalsIgnoreCase("none")) {
            final float offset = mapInfo.getMapMetadata()
                    .getOrderParamFloat(MapMetadata.EL_HOLD,
                            MapMetadata.ATT_HILIGHT_OFFSET);
            final float width = GUIOrderUtils
                    .getLineWidth(mapInfo, MapMetadata.EL_HOLD,
                            MapMetadata.ATT_SHADOW_WIDTHS, numSupports);

            element = drawOrder(mapInfo, offset);
            element.setAttributeNS(null,
                    SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE,
                    GUIOrderUtils.floatToString(width));

            GUIOrderUtils.makeHilight(element, mapInfo.getMapMetadata(),
                    MapMetadata.EL_HOLD);
            group.appendChild(element);
        }

        // create hold polygon
        final float width = GUIOrderUtils
                .getLineWidth(mapInfo, MapMetadata.EL_HOLD,
                        MapMetadata.ATT_WIDTHS, numSupports);

        element = drawOrder(mapInfo, 0);
        element.setAttributeNS(null, SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE,
                GUIOrderUtils.floatToString(width));

        GUIOrderUtils.makeStyled(element, mapInfo.getMapMetadata(),
                MapMetadata.EL_HOLD, power);
        group.appendChild(element);

        // draw 'failed' marker, if appropriate.
        if (!mapInfo.getTurnState().isOrderSuccessful(this)) {
            final SVGElement useElement = GUIOrderUtils
                    .createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
            group.appendChild(useElement);
        }
    }// updateDOM()


    private SVGElement drawOrder(final MapInfo mi, final float offset) {
        final MapMetadata mmd = mi.getMapMetadata();

        // attributes
        final Point2D.Float center = mmd
                .getUnitPt(src.getProvince(), src.getCoast());
        final float radius = mmd.getOrderRadius(MapMetadata.EL_HOLD,
                mi.getSymbolName(srcUnitType));

        // create the polygon
        final SVGElement elem = (SVGPolygonElement) mi.getDocument()
                .createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI,
                        SVGConstants.SVG_POLYGON_TAG);

        // add offset (if any) to elements
        center.x += offset;
        center.y += offset;

        final Point2D.Float[] pts = GUIOrderUtils.makeOctagon(center, radius);

        final StringBuffer sb = new StringBuffer(160);
        for (Point2D.Float pt : pts) {
            GUIOrderUtils.appendFloat(sb, pt.x);
            sb.append(',');
            GUIOrderUtils.appendFloat(sb, pt.y);
            sb.append(' ');
        }

        // create failure marker point, halfway between first & second points.
        failPt = GUIOrderUtils
                .getLineMidpoint(pts[0].x, pts[0].y, pts[1].x, pts[1].y);

        elem.setAttributeNS(null, SVGConstants.SVG_POINTS_ATTRIBUTE,
                sb.toString());
        elem.setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE,
                mmd.getOrderParamString(MapMetadata.EL_HOLD,
                        MapMetadata.ATT_STROKESTYLE));

        return elem;
    }// drawOrder()


    /**
     * We are dependent upon Support orders for proper rendering
     */
    @Override
    public boolean isDependent() {
        return true;
    }


}// class GUIHold
