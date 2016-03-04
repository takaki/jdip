//
//  @(#)ControlBar.java		4/2002
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
package dip.gui.map;

import dip.world.Location;
import org.apache.batik.dom.events.DOMKeyEvent;
import org.w3c.dom.events.MouseEvent;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;


/**
 * All ControlBars must extend this Control bar.
 * Implements all DOMUIEventHandler methods with empty methods.
 */
public abstract class ControlBar extends JToolBar implements DOMUIEventHandler {
    protected final MapPanel mapPanel;


    /**
     * Create a ControlBar
     */
    public ControlBar(final MapPanel mp) {
        super();
        setMargin(new Insets(5, 5, 5, 5));
        setFloatable(false);
        setRollover(true);
        // TODO: we need just a 'top' border, no bottom border, and even a top
        // border doesn't look right when the scrollbar is in place.
        //setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        setBorder(new LineBorder(getBackground().darker()));
        mapPanel = mp;
    }// ControlBar()


    /**
     * Key Pressed event. Does Nothing by default.
     */
    @Override
    public void keyPressed(final DOMKeyEvent ke, final Location loc) {
    }

    /**
     * Mouse Over event: Mouse over a province. Does Nothing by default.
     */
    @Override
    public void mouseOver(final MouseEvent me, final Location loc) {
    }

    /**
     * Mouse Out event: Mouse out of a province. Does Nothing by default.
     */
    @Override
    public void mouseOut(final MouseEvent me, final Location loc) {
    }

    /**
     * Mouse clicked. Does Nothing by default.
     */
    @Override
    public void mouseClicked(final MouseEvent me, final Location loc) {
    }

    /**
     * Mouse button pressed. Does Nothing by default.
     */
    @Override
    public void mouseDown(final MouseEvent me, final Location loc) {
    }

    /**
     * Mouse button released. Does Nothing by default.
     */
    @Override
    public void mouseUp(final MouseEvent me, final Location loc) {
    }

}// class ControlBar	

