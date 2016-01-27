//
//  @(#)GUIGameSetup.java		6/2003
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
package dip.gui;

import dip.gui.map.MapPanel;
import dip.gui.undo.UndoRedoManager;
import dip.world.World;

import javax.swing.*;
import java.awt.*;

/**
 * The Default GameSetup. This is used when we are not in face-
 * to-face or a network mode. All powers may have their orders
 * entered and displayed. The last turnstate is always made the
 * current turnstate.
 */
public class DefaultGUIGameSetup implements GUIGameSetup {

    /**
     * Setup the game.
     */
    public void setup(ClientFrame cf, World world) {
        // create right-panel components
        OrderDisplayPanel odp = new OrderDisplayPanel(cf);
        OrderStatusPanel osp = new OrderStatusPanel(cf);

        cf.setOrderDisplayPanel(odp);
        cf.setOrderStatusPanel(osp);

        // right-panel layout
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPanel.add(osp, BorderLayout.NORTH);
        rightPanel.add(odp, BorderLayout.CENTER);
        cf.getJSplitPane().setRightComponent(rightPanel);

        // setup map panel (left-panel)
        MapPanel mp = new MapPanel(cf);
        cf.setMapPanel(mp);
        cf.getJSplitPane().setLeftComponent(mp);

        // restore or create the undo/redo manager
        UndoRedoManager urm = world.getUndoRedoManager();
        if (urm == null) {
            urm = new UndoRedoManager(cf, odp);
            world.setUndoRedoManager(urm);
        } else {
            urm.setClientFrame(cf);
            urm.setOrderDisplayPanel(odp);
        }

        cf.setUndoRedoManager(urm);

        cf.getJSplitPane().setVisible(true);

        // inform everybody about the World
        cf.fireWorldCreated(world);

        // set turnstate and powers
        cf.fireDisplayablePowersChanged(cf.getDisplayablePowers(),
                world.getMap().getPowers());
        cf.fireOrderablePowersChanged(cf.getOrderablePowers(),
                world.getMap().getPowers());
        cf.fireTurnstateChanged(world.getLastTurnState());
    }// setup()


    /**
     * We do not need to save any data.
     */
    public void save(ClientFrame cf) {
    }


}// class DefaultGUIGameSetup
