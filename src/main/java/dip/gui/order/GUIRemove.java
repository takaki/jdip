//
//  @(#)GUIRemove.java	12/2002
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
import dip.gui.map.SVGUtils;
import dip.misc.Utils;
import dip.order.Orderable;
import dip.order.Remove;
import dip.process.Adjustment;
import dip.world.*;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;

/**
 * GUIOrder implementation of Remove order.
 */
public class GUIRemove extends Remove implements GUIOrder {
    // i18n
    public static final String NO_UNITS_TO_REMOVE = "GUIRemove.no_removes";

    // instance variables
    private transient final static int REQ_LOC = 1;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;


    /**
     * Creates a GUIRemove
     */
    protected GUIRemove() {
        super();
    }// GUIRemove()

    /**
     * Creates a GUIRemove
     */
    protected GUIRemove(final Power power, final Location source,
                        final Unit.Type sourceUnitType) {
        super(power, source, sourceUnitType);
    }// GUIRemove()


    /**
     * This only accepts Remove orders. All others will throw an IllegalArgumentException.
     */
    @Override
    public void deriveFrom(final Orderable order) {
        if (!(order instanceof Remove)) {
            throw new IllegalArgumentException();
        }

        final Remove remove = (Remove) order;
        power = remove.getPower();
        src = remove.getSource();
        srcUnitType = remove.getSourceUnitType();

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
        final Unit unit = position.getUnit(province).orElse(null);

        if (unit != null) {
            if (!stateInfo.canIssueOrder(unit.getPower())) {
                sb.append(Utils.getLocalString(GUIOrder.NOT_OWNER,
                        unit.getPower()));
                return false;
            }

            // check that we actually have units to remove
            final Adjustment.AdjustmentInfo adjInfo = stateInfo.getAdjustmenInfoMap()
                    .get(unit.getPower());
            if (adjInfo.getAdjustmentAmount() < 0) {
                sb.append(Utils.getLocalString(GUIOrder.CLICK_TO_ISSUE,
                        getFullName()));
                return true;
            }

            // no units (for this power) to remove
            sb.append(
                    Utils.getLocalString(NO_UNITS_TO_REMOVE, unit.getPower()));
            return false;
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
    public boolean setLocation(final StateInfo stateInfo, final Location location,
                               final StringBuffer sb) {
        if (testLocation(stateInfo, location, sb)) {
            currentLocNum++;

            final Unit unit = stateInfo.getPosition().getUnit(location.getProvince()).orElse(null);
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
            final SVGGElement powerGroup = mapInfo
                    .getPowerSVGGElement(power, LAYER_HIGHEST);
            GUIOrderUtils.removeChild(powerGroup, group);
            group = null;
        }
    }// removeFromDOM()


    /**
     * Draws a circle with an X in it
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
        // unlike typical orders (move, hold, etc.)
        // we use a symbol; thus we don't have to draw highlight and shadow svg elements.
        //
        group.appendChild(drawOrder(mapInfo));

        // draw 'failed' marker, if appropriate.
        if (!mapInfo.getTurnState().isOrderSuccessful(this)) {
            final SVGElement useElement = GUIOrderUtils
                    .createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
            group.appendChild(useElement);
        }
    }// updateDOM()

    private SVGElement drawOrder(final MapInfo mapInfo) {
        final MapMetadata mmd = mapInfo.getMapMetadata();
        // Note: Remove orders use unit; Disband orders use the dislodged unit coordinates.
        final Point2D.Float srcPt = mmd.getUnitPt(src.getProvince(), src.getCoast());
        final float radius = mmd.getOrderRadius(MapMetadata.EL_REMOVE,
                mapInfo.getSymbolName(srcUnitType));

        // calculate (but don't yet use) failPt
        // it will be on the right-hand part of the circle (r,0)
        failPt = new Point2D.Float(srcPt.x + radius, srcPt.y);

        // get symbolsize
        final MapMetadata.SymbolSize symbolSize = mmd
                .getSymbolSize(DefaultMapRenderer2.SYMBOL_REMOVEUNIT);

        // create RemoveUnit symbol via a USE element
        final SVGElement useElement = SVGUtils.createUseElement(mapInfo.getDocument(),
                "#" + DefaultMapRenderer2.SYMBOL_REMOVEUNIT, null,    // no ID
                null,    // no special style
                srcPt.x, srcPt.y, symbolSize);

        // add to parent
        return useElement;
    }// drawOrder()

    @Override
    public boolean isDependent() {
        return false;
    }


}// class GUIRemove
