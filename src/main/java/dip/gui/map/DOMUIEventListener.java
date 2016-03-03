//
//  @(#)DOMUIEventListener.java			6/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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
package dip.gui.map;

import dip.world.Location;
import org.apache.batik.dom.events.DOMKeyEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.MouseEvent;
import org.w3c.dom.svg.SVGElement;


/**
 * Handles DOM UI Events, and dispatches the events (after some
 * decoding) to the DOMUIEventHandler that is set.
 * <p>
 * Currently this handles Mouse and Key events in the MapPanel. According to
 * http://www.w3.org/TR/2003/WD-DOM-Level-3-Events-20030221/DOM3-Events.txt,
 * the mac 'command' key maps to the meta key.
 */
public class DOMUIEventListener implements EventListener {
    /**
     * Left mouse button
     */
    public static final short BUTTON_LEFT = 0;
    /**
     * Right mouse button
     */
    public static final short BUTTON_RIGHT = 2;
    /**
     * Middle mouse button
     */
    public static final short BUTTON_MIDDLE = 1;


    private MapRenderer2 mapRenderer = null;
    private DOMUIEventHandler handler = null;

    /**
     * Create a DOMUIEventListener
     */
    public DOMUIEventListener() {
    }// DOMUIEventListener()

    /**
     * Set the MapRenderer
     */
    public void setMapRenderer(final MapRenderer2 mr) {
        this.mapRenderer = mr;
    }// setMapRenderer()

    /**
     * Define the (single) object that will receive DOM UI
     * events. Set to null to disable event reporting.
     */
    public void setDOMUIEventHandler(final DOMUIEventHandler handler) {
        this.handler = handler;
    }// setDOMUIEventHandler()

    /**
     * Handle Events; this method dispatches events to the appropriate
     * DOMUIEventHandler methods.
     */
    public void handleEvent(final Event evt) {
        if (handler == null || mapRenderer == null) {
            return;
        }

        // get ID for current element. If no ID is present, go up one level
        // the the parent element (usually a <g> element).
        final SVGElement element = (SVGElement) evt.getTarget();
        String id = element.getAttribute(SVGConstants.SVG_ID_ATTRIBUTE);
        if ("".equals(id)) {
            id = ((SVGElement) element.getParentNode())
                    .getAttribute(SVGConstants.SVG_ID_ATTRIBUTE);
        }

        // lookup Location (convert ID to Location); this may be null.
        final Location location = mapRenderer.getLocation(id);

        // dispatch events, as appropriate
        if (evt instanceof MouseEvent) {
            final MouseEvent me = (MouseEvent) evt;
            final String type = evt.getType();

			/* NOTE: if we enable this, also register event to DMR2
            if(type == SVGConstants.SVG_MOUSEMOVE_EVENT_TYPE)
			{
				handler.mouseMoved(me, location);
			}
			*/

            if (type == SVGConstants.SVG_EVENT_MOUSEOUT) {
                handler.mouseOut(me, location);
            } else if (type == SVGConstants.SVG_EVENT_MOUSEOVER) {
                handler.mouseOver(me, location);
            } else if (type == SVGConstants.SVG_EVENT_CLICK) {
                handler.mouseClicked(me, location);
            } else if (type == SVGConstants.SVG_MOUSEDOWN_EVENT_TYPE) {
                handler.mouseDown(me, location);
            } else if (type == SVGConstants.SVG_MOUSEUP_EVENT_TYPE) {
                handler.mouseUp(me, location);
            }
        } else if (evt instanceof DOMKeyEvent) {
            handler.keyPressed((DOMKeyEvent) evt, location);
        }

    }// handleEvent()


    /**
     * Method to decide if the mouse button is the Right mouse button, or,
     * command (meta) + left (default) mouse button.
     */
    public static boolean isRMBorMetaLMB(final MouseEvent me) {
        final short button = me.getButton();
        return ((button == BUTTON_RIGHT) || (button == BUTTON_LEFT && me
                .getMetaKey()));
    }// isRMBorMetaLMB()


    /**
     * Method to decide if the mouse button is the middle mouse button, or,
     * control + left (default) mouse button.
     */
    public static boolean isMMBorControlLMB(final MouseEvent me) {
        final short button = me.getButton();
        return ((button == BUTTON_MIDDLE) || (button == BUTTON_LEFT && me
                .getShiftKey()));
    }// isMMBorControlLMB()


}// DOMUIEventListener()
