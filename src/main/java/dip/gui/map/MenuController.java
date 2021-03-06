//
//  @(#)MenuController.java		4/2002
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

import dip.gui.ClientMenu;
import dip.gui.map.MapPanelSVGAction.ExportJPG;
import dip.gui.map.MapPanelSVGAction.ExportPDF;
import dip.gui.map.MapPanelSVGAction.ExportPNG;
import dip.gui.map.MapPanelSVGAction.ExportSVG;
import dip.gui.map.MapPanelSVGAction.Print;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * Controls the interaction between the menu (ClientMenu) and
 * the Map display (MapPanel).
 * <p>
 */
final class MenuController {
    private final MapPanel mapPanel;


    /**
     * Creates a MenuController
     */
    public MenuController(final MapPanel mapPanel) {
        this.mapPanel = mapPanel;

        // Print / Export
        registerExportItems();
        setExportingEnabled(true);

        // other stuff...
    }// MenuController()


    /**
     * Cleanup / should be called by MapPanel.close() to reset Menu state.
     */
    public void close() {
        setExportingEnabled(false);
    }// close()


    /**
     * enable / disable Print & Export menu items
     */
    private void setExportingEnabled(final boolean value) {
        final ClientMenu cm = mapPanel.getClientFrame().getClientMenu();
        cm.setEnabled(ClientMenu.FILE_PRINT, value);
        cm.setEnabled(ClientMenu.FILE_EXPORT_JPG, value);
        cm.setEnabled(ClientMenu.FILE_EXPORT_PNG, value);
        cm.setEnabled(ClientMenu.FILE_EXPORT_SVG, value);
        cm.setEnabled(ClientMenu.FILE_EXPORT_PDF, value);
    }// setExportingEnabled()


    /**
     * register us with Print & Export menu items
     */
    private void registerExportItems() {
        // remove any pre-existing listeners for these items
        final ClientMenu cm = mapPanel.getClientFrame().getClientMenu();

        JMenuItem mi = cm.getMenuItem(ClientMenu.FILE_PRINT);
        removeActionListeners(mi);
        mi.addActionListener(new Print(mapPanel));

        mi = cm.getMenuItem(ClientMenu.FILE_EXPORT_JPG);
        removeActionListeners(mi);
        mi.addActionListener(new ExportJPG(mapPanel));

        mi = cm.getMenuItem(ClientMenu.FILE_EXPORT_PNG);
        removeActionListeners(mi);
        mi.addActionListener(new ExportPNG(mapPanel));

        mi = cm.getMenuItem(ClientMenu.FILE_EXPORT_PDF);
        removeActionListeners(mi);
        mi.addActionListener(new ExportPDF(mapPanel));

        mi = cm.getMenuItem(ClientMenu.FILE_EXPORT_SVG);
        removeActionListeners(mi);
        mi.addActionListener(new ExportSVG(mapPanel));
    }// registerExportItems()


    /**
     * Removes all action listeners associated with an AbstractButton
     */
    private void removeActionListeners(final AbstractButton ab) {
        final ActionListener[] al = ab.getActionListeners();
        for (ActionListener anAl : al) {
            ab.removeActionListener(anAl);
        }
    }// removeActionListeners()


}// class MenuController
