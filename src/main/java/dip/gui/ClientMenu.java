//
//  @(#)ClientMenu.java		4/2002
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
package dip.gui;

import dip.gui.dialog.ErrorDialog;
import dip.gui.dialog.prefs.GeneralPreferencePanel;
import dip.gui.map.MapRenderer2;
import dip.misc.Utils;
import dip.tool.Tool;
import dip.tool.ToolManager;
import dip.world.Power;
import dip.world.World;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Implements many menu methods, and constructs the menus.
 * <p>
 * This is fully internationlizable (il8n) via the il8n.properties file
 */
public class ClientMenu {
    private static final String BLANK_ICON = "common/icons/16x16/blank.gif";

    // MENU CONSTANTS
    //
    // main menus
    public static final Item FILE = new Item("FILE");
    public static final Item EDIT = new Item("EDIT");
    public static final Item ORDERS = new Item("ORDERS");
    public static final Item HISTORY = new Item("HISTORY");
    public static final Item VIEW = new Item("VIEW");
    public static final Item REPORTS = new Item("REPORTS");
    public static final Item TOOLS = new Item("TOOLS");
    public static final Item HELP = new Item("HELP");

    // File menu
    public static final Item FILE_NEW = new Item("FILE_NEW");
    public static final Item FILE_NEW_STD = new Item("FILE_NEW_STD");
    public static final Item FILE_NEW_F2F = new Item("FILE_NEW_F2F");
    public static final Item FILE_NEW_NET = new Item("FILE_NEW_NET");

    public static final Item FILE_OPEN = new Item("FILE_OPEN");
    public static final Item FILE_RECENT = new Item("FILE_RECENT");
    public static final Item FILE_SAVE = new Item("FILE_SAVE");
    public static final Item FILE_SAVEAS = new Item("FILE_SAVEAS");
    public static final Item FILE_SAVETO = new Item("FILE_SAVETO");
    public static final Item FILE_IMPORT_FILE = new Item("FILE_IMPORT_FILE");
    public static final Item FILE_IMPORT_FLOC = new Item("FILE_IMPORT_FLOC");
    public static final Item FILE_EXPORT = new Item("FILE_EXPORT");
    public static final Item FILE_PRINT = new Item("FILE_PRINT");
    public static final Item FILE_EXIT = new Item("FILE_EXIT");

    public static final Item FILE_EXPORT_JPG = new Item("FILE_EXPORT_JPG");
    public static final Item FILE_EXPORT_PNG = new Item("FILE_EXPORT_PNG");
    public static final Item FILE_EXPORT_SVG = new Item("FILE_EXPORT_SVG");
    public static final Item FILE_EXPORT_PDF = new Item("FILE_EXPORT_PDF");


    // Edit menu
    public static final Item EDIT_UNDO = new Item("EDIT_UNDO");
    public static final Item EDIT_REDO = new Item("EDIT_REDO");
    public static final Item EDIT_SELECT_ALL = new Item("EDIT_SELECT_ALL");
    public static final Item EDIT_SELECT_NONE = new Item("EDIT_SELECT_NONE");
    public static final Item EDIT_DELETE = new Item("EDIT_DELETE");
    public static final Item EDIT_CLEAR_ALL = new Item("EDIT_CLEAR_ALL");
    public static final Item EDIT_EDIT_MODE = new Item("EDIT_EDIT_MODE");
    public static final Item EDIT_PREFERENCES = new Item("EDIT_PREFERENCES");
    public static final Item EDIT_METADATA = new Item("EDIT_METADATA");


    // Orders menu
    public static final Item ORDERS_VAL_OPTIONS = new Item(
            "ORDERS_VAL_OPTIONS");
    public static final Item ORDERS_REVALIDATE = new Item("ORDERS_REVALIDATE");
    public static final Item ORDERS_MULTI_INPUT = new Item(
            "ORDERS_MULTI_INPUT");
    public static final Item ORDERS_RESOLVE = new Item("ORDERS_RESOLVE");

    // History menu
    public static final Item HISTORY_PREVIOUS = new Item("HISTORY_PREVIOUS");
    public static final Item HISTORY_NEXT = new Item("HISTORY_NEXT");
    public static final Item HISTORY_INITIAL = new Item("HISTORY_INITIAL");
    public static final Item HISTORY_LAST = new Item("HISTORY_LAST");
    public static final Item HISTORY_SELECT = new Item("HISTORY_SELECT");

    // View menu
    public static final Item VIEW_NAMES = new Item(
            "VIEW_NAMES");            // submenu
    public static final Item VIEW_NAMES_NONE = new Item(
            "VIEW_NAMES_NONE");        // rb
    public static final Item VIEW_NAMES_SHORT = new Item(
            "VIEW_NAMES_SHORT");        // rb
    public static final Item VIEW_NAMES_FULL = new Item(
            "VIEW_NAMES_FULL");        // rb
    public static final Item VIEW_ORDERS = new Item(
            "VIEW_ORDERS");            // cb
    public static final Item VIEW_UNITS = new Item(
            "VIEW_UNITS");            // cb
    public static final Item VIEW_DISLODGED_UNITS = new Item(
            "VIEW_DISLODGED_UNITS");    // cb
    public static final Item VIEW_SUPPLY_CENTERS = new Item(
            "VIEW_SUPPLY_CENTERS");    // cb
    public static final Item VIEW_ONLY_SELECTED = new Item(
            "VIEW_ONLY_SELECTED");    // cb
    public static final Item VIEW_UNORDERED = new Item(
            "VIEW_UNORDERED");        // cb
    public static final Item VIEW_INFLUENCE = new Item(
            "VIEW_INFLUENCE");        // cb
    public static final Item VIEW_SELECT_MAP = new Item("VIEW_SELECT_MAP");
    public static final Item VIEW_SHOW_MAP = new Item(
            "VIEW_SHOW_MAP");        // cb

    // Reports menu
    public static final Item REPORTS_RESULTS = new Item("REPORTS_RESULTS");
    public static final Item REPORTS_PREVIOUS_RESULTS = new Item(
            "REPORTS_PREVIOUS_RESULTS");
    public static final Item REPORTS_STATUS = new Item("REPORTS_STATUS");
    public static final Item REPORTS_SC_HISTORY = new Item(
            "REPORTS_SC_HISTORY");
    public static final Item REPORTS_ORDER_STATS = new Item(
            "REPORTS_ORDER_STATS");
    public static final Item REPORTS_MAP_INFO = new Item("REPORTS_MAP_INFO");

    // Tools menu
    // NOTE: Items are created by the Tools themselves

    // Help menu
    public static final Item HELP_ABOUT = new Item("HELP_ABOUT");
    public static final Item HELP_CONTENTS = new Item("HELP_CONTENTS");

    // View:Orders submenu
    private static final Item VIEW_ORDERS_ALLPOWERS = new Item(
            "VIEW_ORDERS_ALLPOWERS");
    private static final Item VIEW_ORDERS_NOPOWERS = new Item(
            "VIEW_ORDERS_NOPOWERS");

    // Blank Icon
    private static final Icon blankIcon = Utils.getIcon(BLANK_ICON);

    // instance variables
    private JMenuBar menuBar;
    private HashMap menuMap;
    private ClientFrame clientFrame = null;
    //private static Font menuFont = new Font("SansSerif", Font.PLAIN, 12);

    // recent file menu fields
    private static final int RI_INSERT_POINT = 12;    // insertion point (index) for recent files
    private int numItems = 0;
    private RecentFileListener rfListener = null;

    // view-orders menu fields
    private JMenu orderMenu = null;
    private Power[] powers = null;
    private OrderMenuListener orderMenuListener = null;


    /**
     * Inner class that describes and obtains menu item info from the resource file.
     */
    public static class Item {
        // mneumonic: highlited letter in menu item text
        // accelerator: e.g., ^S for "Save"
        private String name = null;
        private int mnemonic = -1;
        private KeyStroke accelerator = null;
        private Icon icon = null;
        private static final Icon blank = Utils.getIcon(BLANK_ICON);
        private static final int MASK = Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMask();

        /**
         * Create a MenuItem definition from the properties file
         * Mac OS X: for accelerator keys, "control" will be changed
         * to use the platform default (Command).
         */
        public Item(final String name) {
            // read from il8n file
            this.name = Utils.getLocalString(name);

            String text = Utils.getLocalStringNoEx(name + "_mnemonic");
            if (text != null) {
                final KeyStroke ks = KeyStroke.getKeyStroke(text);
                this.mnemonic = ks.getKeyCode();
            }

            text = Utils.getLocalStringNoEx(name + "_accelerator");
            if (text != null) {
                this.accelerator = KeyStroke.getKeyStroke(text);
                if (Utils.isOSX()) {
                    // substitute COMMAND for CTRL modifier.
                    //
                    if ((accelerator
                            .getModifiers() & InputEvent.CTRL_MASK) > 0) {
                        accelerator = KeyStroke
                                .getKeyStroke(accelerator.getKeyCode(), MASK);
                    }
                }
            }

            text = Utils.getLocalStringNoEx(name + "_icon");
            this.icon = (text == null) ? blank : Utils.getIcon(text);
        }// Item()

        /**
         * Get item name
         */
        public String getName() {
            return name;
        }

        /**
         * Get item mnemonic
         */
        public int getMnemonic() {
            return mnemonic;
        }

        /**
         * Get item accelerator
         */
        public KeyStroke getAccelerator() {
            return accelerator;
        }

        /**
         * Get item icon
         */
        public Icon getIcon() {
            return icon;
        }

        /**
         * Create a JMenuItem with the Name, Mnemonic, Accelerator,
         * and Icon for a given MenuItem. If indent is false, and
         * there is no icon assigned, no 'blank' icon is used.
         */
        public JMenuItem makeMenuItem(final boolean indent) {
            final JMenuItem menuItem = new JMenuItem(getName());
            if (indent) {
                menuItem.setIcon(getIcon());
            }
            menuItem.setMnemonic(getMnemonic());
            menuItem.setAccelerator(getAccelerator());
            return menuItem;
        }// makeMenuItem()
    }// inner class Item


    // constructor
    public ClientMenu(final ClientFrame parent) {
        this.clientFrame = parent;

        // create menu bar
        menuBar = new JMenuBar();
        menuMap = new HashMap(31);

        // create menus
        JMenu menu = null;

        JMenu subMenu = null;

        // File
        menu = makeMenu(FILE);

        subMenu = makeMenu(FILE_NEW, true);
        subMenu.add(makeMenuItem(FILE_NEW_STD, false));
        subMenu.add(makeMenuItem(FILE_NEW_F2F, false));
        //subMenu.add(makeMenuItem(FILE_NEW_NET, false));

        menu.add(
                subMenu);                                // 1 [number to index for MRU file list]
        menu.add(makeMenuItem(FILE_OPEN));
        menu.add(makeMenuItem(FILE_SAVE));
        menu.add(makeMenuItem(FILE_SAVEAS));
        menu.add(makeMenuItem(FILE_SAVETO));            // 5
        menu.add(new JSeparator());                        // 6

        menu.add(makeMenuItem(FILE_IMPORT_FLOC));
        menu.add(makeMenuItem(FILE_IMPORT_FILE));        // 8

        subMenu = makeMenu(FILE_EXPORT, true);
        subMenu.add(makeMenuItem(FILE_EXPORT_JPG, false));
        subMenu.add(makeMenuItem(FILE_EXPORT_PNG, false));
        subMenu.add(makeMenuItem(FILE_EXPORT_PDF, false));
        subMenu.add(makeMenuItem(FILE_EXPORT_SVG, false));
        menu.add(subMenu);                                // 9
        menu.add(new JSeparator());
        menu.add(makeMenuItem(FILE_PRINT));                // 11
        menu.add(new JSeparator());                        // 12

        createRecentFileList(menu);        // 13th item on menu

        menu.add(makeMenuItem(FILE_EXIT));
        menuBar.add(menu);

        // Edit
        menu = makeMenu(EDIT);
        menu.add(makeMenuItem(EDIT_UNDO));
        menu.add(makeMenuItem(EDIT_REDO));
        menu.add(new JSeparator());
        menu.add(makeCBMenuItem(EDIT_EDIT_MODE, false, false));
        menu.add(new JSeparator());
        menu.add(makeMenuItem(EDIT_PREFERENCES));
        menuBar.add(menu);

        // Orders
        menu = makeMenu(ORDERS);
        menu.add(makeMenuItem(EDIT_SELECT_ALL));
        menu.add(makeMenuItem(EDIT_SELECT_NONE));
        menu.add(new JSeparator());
        menu.add(makeMenuItem(EDIT_DELETE));
        menu.add(makeMenuItem(EDIT_CLEAR_ALL));
        menu.add(new JSeparator());
        menu.add(makeMenuItem(ORDERS_VAL_OPTIONS));
        menu.add(makeMenuItem(ORDERS_REVALIDATE));
        menu.add(new JSeparator());
        menu.add(makeMenuItem(ORDERS_MULTI_INPUT));
        menu.add(new JSeparator());
        menu.add(makeMenuItem(ORDERS_RESOLVE));
        menuBar.add(menu);

        // History
        menu = makeMenu(HISTORY);
        menu.add(makeMenuItem(HISTORY_PREVIOUS));
        menu.add(makeMenuItem(HISTORY_NEXT));
        menu.add(new JSeparator());
        menu.add(makeMenuItem(HISTORY_INITIAL, true));
        menu.add(makeMenuItem(HISTORY_LAST, true));
        menu.add(new JSeparator());
        menu.add(makeMenuItem(HISTORY_SELECT, true));
        menuBar.add(menu);

        // View
        menu = makeMenu(VIEW);

        final ButtonGroup nbg = new ButtonGroup();
        subMenu = makeMenu(VIEW_NAMES, true);
        subMenu.add(makeRBMenuItem(VIEW_NAMES_NONE, nbg, true, false));
        subMenu.add(makeRBMenuItem(VIEW_NAMES_SHORT, nbg, false, false));
        subMenu.add(makeRBMenuItem(VIEW_NAMES_FULL, nbg, false, false));
        menu.add(subMenu);

        // VIEW_ORDERS is a submenu
        orderMenu = makeMenu(VIEW_ORDERS, true);
        menu.add(orderMenu);
        menu.add(makeCBMenuItem(VIEW_UNITS, true, false));
        menu.add(makeCBMenuItem(VIEW_DISLODGED_UNITS, true, false));
        menu.add(makeCBMenuItem(VIEW_SUPPLY_CENTERS, true, false));
        menu.add(makeCBMenuItem(VIEW_UNORDERED, false, false));
        menu.add(makeCBMenuItem(VIEW_SHOW_MAP, true, false));
        menu.add(makeCBMenuItem(VIEW_INFLUENCE, false, false));
        menu.add(new JSeparator());
        menu.add(makeMenuItem(VIEW_SELECT_MAP, true));
        menuBar.add(menu);


        // Reports
        menu = makeMenu(REPORTS);
        menu.add(makeMenuItem(REPORTS_RESULTS, false));
        menu.add(makeMenuItem(REPORTS_PREVIOUS_RESULTS, false));
        menu.add(makeMenuItem(REPORTS_STATUS, false));
        menu.add(makeMenuItem(REPORTS_SC_HISTORY, false));
        menu.add(makeMenuItem(REPORTS_ORDER_STATS, false));
        menu.add(makeMenuItem(REPORTS_MAP_INFO, false));
        menu.add(new JSeparator());
        menu.add(makeMenuItem(EDIT_METADATA, false));
        menuBar.add(menu);


        // add Tools menu, if there are any Tools
        if (ToolManager.getTools().length > 0) {
            menu = makeMenu(TOOLS);
            makeToolMenu(menu);
            menuBar.add(menu);
        }

        // Help
        menu = makeMenu(HELP);
        menu.add(makeMenuItem(HELP_ABOUT));
        menu.add(makeMenuItem(HELP_CONTENTS));
        menuBar.add(menu);

        // add change listener
        clientFrame.addPropertyChangeListener(new ModeListener());
    }// ClientMenu()


    /**
     * Make a Menu (or submenu) from an Item object.
     */
    public JMenu makeMenu(final Item item) {
        return makeMenu(item, false);
    }// makeMenu()

    /**
     * Make a Menu (or submenu) from an Item object, indenting if required
     */
    public JMenu makeMenu(final Item item, final boolean indent) {
        final JMenu menu = new JMenu(item.getName());
        //menu.setFont(menuFont);

        if (indent) {
            menu.setIcon(item.getIcon());
        }

        menu.setMnemonic(item.getMnemonic());
        menuMap.put(item, menu);
        return menu;
    }// makeMenu()

    /**
     * Make a menu item from an Item object
     */
    public JMenuItem makeMenuItem(final Item item) {
        return makeMenuItem(item, true);
    }// makeMenuItem()

    /**
     * Make a menu item from an Item object, indenting if required
     */
    public JMenuItem makeMenuItem(final Item item, final boolean indent) {
        final JMenuItem menuItem = new JMenuItem(item.getName());
        //menuItem.setFont(menuFont);

        if (indent) {
            menuItem.setIcon(item.getIcon());
        }

        menuItem.setMnemonic(item.getMnemonic());
        menuItem.setAccelerator(item.getAccelerator());
        menuMap.put(item, menuItem);
        return menuItem;
    }// makeMenuItem()


    /**
     * Make a JCheckBox menu item
     */
    public JCheckBoxMenuItem makeCBMenuItem(final Item item, final boolean defaultState,
                                            final boolean indent) {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(item.getName(),
                defaultState);
        //menuItem.setFont(menuFont);
        menuItem.setMnemonic(item.getMnemonic());
        menuItem.setAccelerator(item.getAccelerator());
        if (indent) {
            menuItem.setIcon(item.getIcon());
        }
        menuMap.put(item, menuItem);
        return menuItem;
    }// makeCBMenuItem()


    /**
     * Make a JRadioButton menu item
     */
    private JRadioButtonMenuItem makeRBMenuItem(final Item item, final ButtonGroup bg,
                                                final boolean defaultState,
                                                final boolean indent) {
        final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(item.getName(),
                defaultState);
        bg.add(menuItem);
        //menuItem.setFont(menuFont);
        menuItem.setMnemonic(item.getMnemonic());
        menuItem.setAccelerator(item.getAccelerator());
        if (indent) {
            menuItem.setIcon(item.getIcon());
        }
        menuMap.put(item, menuItem);
        return menuItem;
    }// makeCBMenuItem()


    /**
     * Get a JMenuItem given an Item (usually a specified constant)
     */
    public JMenuItem getMenuItem(final Item item) {
        final JMenuItem menuItem = (JMenuItem) menuMap.get(item);
        if (menuItem != null) {
            return menuItem;
        }
        throw new IllegalArgumentException("Undefined or null Item");
    }// getMenuItem()

    /**
     * Via Power
     */
    private JMenuItem getMenuItem(final Power power) {
        final JMenuItem menuItem = (JMenuItem) menuMap.get(power);
        if (menuItem != null) {
            return menuItem;
        }
        throw new IllegalArgumentException("Undefined or null Item");
    }// getMenuItem()

    /**
     * Get the JMenuBar
     */
    public JMenuBar getJMenuBar() {
        return menuBar;
    }// getJMenuBar()


    public boolean isEnabled(final Item item) {
        final JMenuItem menuItem = getMenuItem(item);
        return menuItem.isEnabled();
    }// isEnabled()

    // NOTE: this WILL NOT WORK on RB menu items!!
    public void setEnabled(final Item item, final boolean value) {
        final JMenuItem menuItem = getMenuItem(item);
        menuItem.setEnabled(value);
    }// setEnabled()


    public boolean isVisible(final Item item) {
        final JMenuItem menuItem = getMenuItem(item);
        return menuItem.isVisible();
    }// isVisible()

    public void setVisible(final Item item, final boolean value) {
        final JMenuItem menuItem = getMenuItem(item);
        menuItem.setVisible(value);
    }// setVisible()


    // WARNING: this could break il8n
    public void setText(final Item item, final String text) {
        final JMenuItem menuItem = getMenuItem(item);
        menuItem.setText(text);
    }// setText()


    // 'checkbox' state
    public boolean getSelected(final Item item) {
        final JCheckBoxMenuItem cbMenuItem = (JCheckBoxMenuItem) getMenuItem(item);
        return cbMenuItem.getState();
    }// getSelected()

    // for internal use only
    private boolean getSelected(final Power power) {
        final JCheckBoxMenuItem cbMenuItem = (JCheckBoxMenuItem) getMenuItem(power);
        return cbMenuItem.getState();
    }// getSelected()

    public void setSelected(final Item item, final boolean value) {
        ((JCheckBoxMenuItem) getMenuItem(item)).setState(value);
    }// setSelected()

    // for internal use only
    public void setSelected(final Power power, final boolean value) {
        ((JCheckBoxMenuItem) getMenuItem(power)).setState(value);
    }// setSelected()


    /**
     * Updates the recent file menu, if enabled.
     */
    public void updateRecentFiles() {
        final JMenu fileMenu = (JMenu) menuMap.get(FILE);

        // remove all items inserted, decouple from listeners
        final JMenuItem[] toRemove = new JMenuItem[numItems];
        for (int i = 0; i < numItems; i++) {
            toRemove[i] = fileMenu.getItem(i + RI_INSERT_POINT);
            toRemove[i].removeActionListener(rfListener);
        }

        // now delete
        for (int i = 0; i < toRemove.length; i++) {
            fileMenu.remove(toRemove[i]);
        }


        // create new items
        createItemsFromArray(fileMenu,
                GeneralPreferencePanel.getRecentFileNamesFromCache());
    }// updateRecentFiles()

    /**
     * Creates a series of files, with a JSeperator above, if enabled.
     * Item names have a number preceding, which is the mnemonic.
     * Items are indexed by the mnemonic.
     */
    private void createRecentFileList(final JMenu menu) {
        rfListener = new RecentFileListener();

        final int nItems = createItemsFromArray(menu,
                GeneralPreferencePanel.getRecentFileNamesFromPrefs());

        // add bottom separator
        if (nItems > 0) {
            menu.add(new JSeparator());
        }
    }// createRecentFileList()


    /**
     * Creates items from a string array; sets numItems, too. Returns the number of items created.
     */
    private int createItemsFromArray(final JMenu menu, final String[] array) {
        // we only handle single-digits; otherwise substring() in RecentFileListner.actionPerformed()
        // could fail. Although > 9 would be too many recent files anyway. But this is a safety check.
        if (array.length > 9) {
            throw new IllegalArgumentException("array too long");
        }

        numItems = array.length;
        for (int i = 0; i < array.length; i++) {
            final String mnemonic = String.valueOf(i + 1);
            final StringBuffer sb = new StringBuffer(32);
            sb.append(mnemonic);
            sb.append(' ');
            sb.append(array[i]);

            final JMenuItem menuItem = new JMenuItem(sb.toString());
            menuItem.setMnemonic(KeyStroke.getKeyStroke(mnemonic).getKeyCode());
            menuItem.setIcon(blankIcon);
            menuItem.addActionListener(rfListener);

            menu.add(menuItem, RI_INSERT_POINT + i);
        }


        return numItems;
    }// createItemsFromArray()


    private class RecentFileListener implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            final JMenuItem jmi = (JMenuItem) e.getSource();
            final File file = GeneralPreferencePanel
                    .getFileFromName(jmi.getText().substring(2));
            if (file != null) {
                // attempt to open file, with PersistenceManager
                final World world = clientFrame.getPM().open(file);
                if (world != null) {
                    clientFrame.createWorld(world);
                }
            }
        }// actionPerformed()
    }// inner class RecentFileListener


    /**
     * Make the Tools menu. This is only called if at least 1 tool exists.
     */
    private void makeToolMenu(final JMenu menu) {
        final Tool[] tools = ToolManager.getTools();
        for (int i = 0; i < tools.length; i++) {
            menu.add(tools[i].registerJMenuItem());
        }
    }// makeToolMenu()


    /**
     * This is a specialized item group that is used internally and
     * by OrderPanel.java.
     */
    public void setEditItemsEnabled(final boolean value) {
        setEnabled(EDIT_SELECT_ALL, value);
        setEnabled(EDIT_SELECT_NONE, value);
        setEnabled(EDIT_DELETE, value);
        setEnabled(EDIT_CLEAR_ALL, value);
    }// setEditEnabled()


    /**
     * View menu rendering options
     * <p>
     * This is a specialized item group that is enabled/disabled
     * automatically by a mode change. However, there are/may be
     * instances when items may need to be disabled despite the
     * particular mode setting.
     */
    public void setViewRenderItemsEnabled(final boolean value) {
        setEnabled(VIEW_NAMES, value);
        setEnabled(VIEW_ORDERS, value);
        setEnabled(VIEW_UNITS, value);
        setEnabled(VIEW_DISLODGED_UNITS, value);
        setEnabled(VIEW_SUPPLY_CENTERS, value);
        setEnabled(VIEW_UNORDERED, value);
        setEnabled(VIEW_SHOW_MAP, value);
        setEnabled(VIEW_INFLUENCE, value);
        setEnabled(VIEW_SELECT_MAP, value);
    }// setViewRenderItemsEnabled()


    /**
     * Set the View->Names-> menu option to "None", or whatever was set in
     * Preferences. This is needed after loading a new game because the map
     * does not show names.
     */
    public void setViewNames() {
        // get user pref setting
        final String defaultLabelLevel = GeneralPreferencePanel
                .getMapLabelSetting();

        if (MapRenderer2.VALUE_LABELS_BRIEF.equals(defaultLabelLevel)) {
            ((JRadioButtonMenuItem) getMenuItem(VIEW_NAMES_SHORT))
                    .setSelected(true);
        } else if (MapRenderer2.VALUE_LABELS_FULL.equals(defaultLabelLevel)) {
            ((JRadioButtonMenuItem) getMenuItem(VIEW_NAMES_FULL))
                    .setSelected(true);
        } else {
            ((JRadioButtonMenuItem) getMenuItem(VIEW_NAMES_NONE))
                    .setSelected(true);
        }
    }// setViewNames()


    /**
     * MODE_NONE menus<br>
     * This occurs when no World is open.
     */
    private void setModeNone() {
        // File:
        setEnabled(FILE_SAVE, false);
        setEnabled(FILE_SAVEAS, false);
        setEnabled(FILE_SAVETO, false);
        setEnabled(ClientMenu.FILE_PRINT, false);
        setEnabled(ClientMenu.FILE_EXPORT_JPG, false);
        setEnabled(ClientMenu.FILE_EXPORT_PNG, false);
        setEnabled(ClientMenu.FILE_EXPORT_PDF, false);
        setEnabled(ClientMenu.FILE_EXPORT_SVG, false);

        // Edit:
        refreshUndoRedo();    // EDIT_UNDO and EDIT_REDO
        setEditItemsEnabled(false);
        setEnabled(ClientMenu.EDIT_EDIT_MODE, false);
        setEnabled(EDIT_METADATA, false);

        // Orders:
        setEnabled(ClientMenu.ORDERS_VAL_OPTIONS, false);
        setEnabled(ClientMenu.ORDERS_REVALIDATE, false);
        setEnabled(ClientMenu.ORDERS_MULTI_INPUT, false);
        setEnabled(ClientMenu.ORDERS_RESOLVE, false);

        // History menu: Controlled by PhaseSelector.java

        // View:
        setEnabled(VIEW_NAMES, false);
        setEnabled(VIEW_ORDERS, false);
        setEnabled(VIEW_UNITS, false);
        setEnabled(VIEW_DISLODGED_UNITS, false);
        setEnabled(VIEW_SUPPLY_CENTERS, false);
        setEnabled(VIEW_UNORDERED, false);
        setEnabled(VIEW_SHOW_MAP, false);
        setEnabled(VIEW_INFLUENCE, false);
        setEnabled(VIEW_SELECT_MAP, false);

        // Reports
        setEnabled(REPORTS_RESULTS, false);
        setEnabled(REPORTS_PREVIOUS_RESULTS, false);
        setEnabled(REPORTS_STATUS, false);
        setEnabled(REPORTS_SC_HISTORY, false);
        setEnabled(REPORTS_ORDER_STATS, false);
        setEnabled(REPORTS_MAP_INFO, false);
    }// setModeNone()


    /**
     * MODE_ORDER menus<br>
     * This is the mode for un-resolved turnstates, where orders
     * may be entered.
     */
    private void setModeOrder() {
        // File:
        setEnabled(FILE_SAVE, true);
        setEnabled(FILE_SAVEAS, true);
        setEnabled(FILE_SAVETO, true);
        setEnabled(ClientMenu.FILE_PRINT, true);
        setEnabled(ClientMenu.FILE_EXPORT_JPG, true);
        setEnabled(ClientMenu.FILE_EXPORT_PNG, true);
        setEnabled(ClientMenu.FILE_EXPORT_PDF, true);
        setEnabled(ClientMenu.FILE_EXPORT_SVG, true);

        // Edit:
        refreshUndoRedo();    // EDIT_UNDO and EDIT_REDO
        setEditItemsEnabled(true);
        setEnabled(ClientMenu.EDIT_EDIT_MODE, true);
        setEnabled(EDIT_METADATA, true);

        // Orders:
        setEnabled(ClientMenu.ORDERS_VAL_OPTIONS, true);
        setEnabled(ClientMenu.ORDERS_REVALIDATE, true);
        setEnabled(ClientMenu.ORDERS_MULTI_INPUT, true);
        setEnabled(ClientMenu.ORDERS_RESOLVE, true);

        // History menu: Controlled by PhaseSelector.java

        // View:
        setEnabled(VIEW_NAMES, true);
        setEnabled(VIEW_ORDERS, true);
        setEnabled(VIEW_UNITS, true);
        setEnabled(VIEW_DISLODGED_UNITS, true);
        setEnabled(VIEW_SUPPLY_CENTERS, true);
        setEnabled(VIEW_UNORDERED, true);
        setEnabled(VIEW_SHOW_MAP, true);
        setEnabled(VIEW_INFLUENCE, true);
        setEnabled(VIEW_SELECT_MAP, true);

        // Reports:
        // REPORTS_RESULTS controlled by PhaseSelector.java
        // REPORTS_PREVIOUS_RESULTS controlled by PhaseSelector.java
        setEnabled(REPORTS_STATUS, true);
        setEnabled(REPORTS_SC_HISTORY, true);
        setEnabled(REPORTS_ORDER_STATS, true);
        setEnabled(REPORTS_MAP_INFO, true);
    }// setModeOrder()

    /**
     * MODE_REVIEW menus<br>
     * This mode is when the turnstate is resolved, and orders can
     * be seen but new orders cannot be entered.
     */
    private void setModeReview() {
        // File:
        setEnabled(FILE_SAVE, true);
        setEnabled(FILE_SAVEAS, true);
        setEnabled(FILE_SAVETO, true);
        setEnabled(ClientMenu.FILE_PRINT, true);
        setEnabled(ClientMenu.FILE_EXPORT_JPG, true);
        setEnabled(ClientMenu.FILE_EXPORT_PNG, true);
        setEnabled(ClientMenu.FILE_EXPORT_PDF, true);
        setEnabled(ClientMenu.FILE_EXPORT_SVG, true);

        // Edit:
        refreshUndoRedo();    // EDIT_UNDO and EDIT_REDO
        setEditItemsEnabled(false);
        setEnabled(ClientMenu.EDIT_EDIT_MODE, false);
        setEnabled(EDIT_METADATA, true);

        // Orders:
        setEnabled(ClientMenu.ORDERS_VAL_OPTIONS, false);
        setEnabled(ClientMenu.ORDERS_REVALIDATE, false);
        setEnabled(ClientMenu.ORDERS_MULTI_INPUT, false);
        setEnabled(ClientMenu.ORDERS_RESOLVE, false);

        // History menu: Controlled by PhaseSelector.java

        // View:
        setEnabled(VIEW_NAMES, true);
        setEnabled(VIEW_ORDERS, true);
        setEnabled(VIEW_UNITS, true);
        setEnabled(VIEW_DISLODGED_UNITS, true);
        setEnabled(VIEW_SUPPLY_CENTERS, true);
        setEnabled(VIEW_UNORDERED, true);
        setEnabled(VIEW_SHOW_MAP, true);
        setEnabled(VIEW_INFLUENCE, true);
        setEnabled(VIEW_SELECT_MAP, true);

        // Reports:
        // REPORTS_RESULTS controlled by PhaseSelector.java
        // REPORTS_PREVIOUS_RESULTS controlled by PhaseSelector.java
        setEnabled(REPORTS_STATUS, true);
        setEnabled(REPORTS_SC_HISTORY, true);
        setEnabled(REPORTS_ORDER_STATS, true);
        setEnabled(REPORTS_MAP_INFO, true);
    }// setModeReview()

    /**
     * MODE_EDIT menus<br>
     * This mode is when we are in edit mode
     */
    private void setModeEdit() {
        // File:
        setEnabled(FILE_SAVE, true);
        setEnabled(FILE_SAVEAS, true);
        setEnabled(FILE_SAVETO, true);
        setEnabled(ClientMenu.FILE_PRINT, true);
        setEnabled(ClientMenu.FILE_EXPORT_JPG, true);
        setEnabled(ClientMenu.FILE_EXPORT_PNG, true);
        setEnabled(ClientMenu.FILE_EXPORT_PDF, true);
        setEnabled(ClientMenu.FILE_EXPORT_SVG, true);

        // edit menu: leave preferences / edit mode / metadata alone
        // leave metadata alone, and disable all others.
        refreshUndoRedo();    // EDIT_UNDO and EDIT_REDO
        setEnabled(EDIT_METADATA, true);
        setEnabled(EDIT_METADATA, true);
        setEnabled(EDIT_SELECT_ALL, false);
        setEnabled(EDIT_SELECT_NONE, false);
        setEnabled(EDIT_DELETE, false);
        setEnabled(EDIT_CLEAR_ALL, false);

        // Orders: order menu does not apply in edit mode
        setEnabled(ClientMenu.ORDERS_VAL_OPTIONS, false);
        setEnabled(ClientMenu.ORDERS_REVALIDATE, false);
        setEnabled(ClientMenu.ORDERS_MULTI_INPUT, false);
        setEnabled(ClientMenu.ORDERS_RESOLVE, false);

        // History menu: Controlled by PhaseSelector.java

        // View:
        setEnabled(VIEW_NAMES, true);
        setEnabled(VIEW_ORDERS, true);
        setEnabled(VIEW_UNITS, true);
        setEnabled(VIEW_DISLODGED_UNITS, true);
        setEnabled(VIEW_SUPPLY_CENTERS, true);
        setEnabled(VIEW_UNORDERED, true);
        setEnabled(VIEW_SHOW_MAP, true);
        setEnabled(VIEW_INFLUENCE, true);
        setEnabled(VIEW_SELECT_MAP, true);

        // Reports:
        // REPORTS_RESULTS controlled by PhaseSelector.java
        // REPORTS_PREVIOUS_RESULTS controlled by PhaseSelector.java
        setEnabled(REPORTS_STATUS, true);
        setEnabled(REPORTS_SC_HISTORY, true);
        setEnabled(REPORTS_ORDER_STATS, true);
        setEnabled(REPORTS_MAP_INFO, true);
    }// setModeEdit()


    /**
     * Updates the Power menu.
     */
    private void updatePowers(final Power[] newPowers) {
        // check
        if (newPowers == null) {
            throw new IllegalArgumentException("null powers");
        }

        // remove old powers (if any) from menu map
        if (powers != null) {
            for (int i = 0; i < powers.length; i++) {
                menuMap.remove(powers[i]);
            }
        }

        // set our new powers
        // first, alphabatize
        powers = newPowers;
        Arrays.sort(powers);

        // create orderMenuListener if we haven't
        if (orderMenuListener == null) {
            orderMenuListener = new OrderMenuListener();
        }

        // remove all item's listeners
        for (int i = 0; i < orderMenu.getItemCount(); i++) {
            final JMenuItem jmi = orderMenu.getItem(i);
            if (jmi != null)    // separators are returned as 'null'
            {
                jmi.removeActionListener(orderMenuListener);
            }
        }

        // remove all items
        orderMenu.removeAll();

        // create 'all' and 'none' items
        orderMenu.add(makeMenuItem(VIEW_ORDERS_ALLPOWERS, false));
        orderMenu.add(makeMenuItem(VIEW_ORDERS_NOPOWERS, false));

        // add spacer
        orderMenu.add(new JSeparator());

        // add listeners to all & none items
        getMenuItem(VIEW_ORDERS_ALLPOWERS).addActionListener(orderMenuListener);
        getMenuItem(VIEW_ORDERS_NOPOWERS).addActionListener(orderMenuListener);

        // create power items
        final int startAccel = KeyEvent.VK_F1;    // NOTE: this is, technically, dangerous
        final int maxAccel = 12;                    // only go upto VK_F12 (pc/mac/unix usually have 12 Fn keys)
        for (int i = 0; i < powers.length; i++) {
            final String mnemonic = String.valueOf(i + 1);
            final StringBuffer sb = new StringBuffer(32);
            sb.append(mnemonic);
            sb.append(' ');
            sb.append(powers[i].getName());
            final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(sb.toString(),
                    true);
            //menuItem.setFont(menuFont);

            // mnemonics only go upto 9 (1-9)
            if (i < 9) {
                menuItem.setMnemonic(
                        KeyStroke.getKeyStroke(mnemonic).getKeyCode());
            }

            menuItem.addActionListener(orderMenuListener);

            // accel only go from F1-F12
            if (i < maxAccel) {
                menuItem.setAccelerator(KeyStroke
                        .getKeyStroke((i + startAccel), InputEvent.CTRL_MASK));
            }

            orderMenu.add(menuItem);
            menuMap.put(powers[i],
                    menuItem);    // atypical; for internal use only
        }
    }// updatePowers()


    /**
     * Gets which powers are selected for drawing. Never returns null.
     */
    public Power[] getOrderDrawingPowers() {
        // some powers are selected. determine which.
        final ArrayList<Power> list = new ArrayList<>(powers.length);

        for (int i = 0; i < powers.length; i++) {
            if (getSelected(powers[i])) {
                list.add(powers[i]);
            }
        }

        return (Power[]) list.toArray(new Power[list.size()]);
    }// getOrderDrawingPowers()


    /**
     * Order menu listener
     */
    private class OrderMenuListener implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            final JMenuItem jmi = (JMenuItem) e.getSource();

            if (jmi == getMenuItem(ClientMenu.VIEW_ORDERS_ALLPOWERS)) {
                for (int i = 0; i < powers.length; i++) {
                    setSelected(powers[i], true);
                }
            } else if (jmi == getMenuItem(ClientMenu.VIEW_ORDERS_NOPOWERS)) {
                for (int i = 0; i < powers.length; i++) {
                    setSelected(powers[i], false);
                }
            }


            // change order rendering settings
            if (clientFrame.getMapPanel() != null) {
                final Power[] visiblePowers = getOrderDrawingPowers();
                final MapRenderer2 mr2 = clientFrame.getMapPanel().getMapRenderer();
                mr2.execRenderCommand(mr2.getRenderCommandFactory()
                        .createRCSetPowerOrdersDisplayed(mr2, visiblePowers));
            }
        }// actionPerformed()
    }// inner class OrderMenuListener


    /**
     * Mode Change Listener.
     */
    private class ModeListener extends AbstractCFPListener {
        public void actionWorldCreated(final World w) {
            final Power[] thePowers = w.getMap().getPowers().toArray(new Power[0]);
            updatePowers(thePowers);
        }// actionWorldCreated()

        public void actionModeChanged(final String mode) {
            if (mode == ClientFrame.MODE_NONE) {
                setModeNone();
            } else if (mode == ClientFrame.MODE_ORDER) {
                setModeOrder();
            } else if (mode == ClientFrame.MODE_REVIEW) {
                setModeReview();
            } else if (mode == ClientFrame.MODE_EDIT) {
                setModeEdit();
            }
        }// actionModeChanged()
    }// inner class ModeListener


    /**
     * Set the given method (of the given target class) to be the
     * only recipient of ActionEvents for the desired menu item.
     * All methods <b>must</b> have zero arguments.
     * <p>
     * Null arguments are not permitted.
     */
    public void setActionMethod(final Item item, final Object target, final String methodName) {
        // disallow null args
        if (target == null || methodName == null || item == null) {
            System.err.println("setActionMethod()");
            System.err.println("  item: " + item);
            System.err.println("  target: " + target);
            System.err.println("  methodName: " + methodName);
            throw new IllegalArgumentException("Null argument(s)");
        }

        // find our item
        final JMenuItem menuItem = getMenuItem(item);

        // get our method
        Method method = null;
        try {
            method = target.getClass().getMethod(methodName, null);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "NoSuchMethodException: " + methodName);
        }


        // remove any existing listeners
        final ActionListener[] listeners = menuItem.getActionListeners();
        for (int i = 0; i < listeners.length; i++) {
            menuItem.removeActionListener(listeners[i]);
        }

        // add our new action listener, which will invoke (via reflection)
        // the passed method.
        menuItem.addActionListener(new ReflexiveActionListener(target, method));
    }// setActionMethod()


    /**
     * Generic Menu Action event, using Reflection
     */
    private class ReflexiveActionListener implements ActionListener {
        private final Object target;
        private final Method targetMethod;

        /**
         * Create a ReflexiveActionListener
         */
        public ReflexiveActionListener(final Object target, final Method targetMethod) {
            this.target = target;
            this.targetMethod = targetMethod;
        }// ReflexiveActionListener

        /**
         * Call the target method
         */
        public void actionPerformed(final ActionEvent evt) {
            try {
                targetMethod.invoke(target, null);
            } catch (final InvocationTargetException e) {
                debugOut(e);
                if (e.getCause() != null) {
                    ErrorDialog.displaySerious(null, e);
                    e.getCause().printStackTrace();
                }
            } catch (final IllegalAccessException e2) {
                debugOut(e2);
                throw new IllegalStateException(
                        "Menu: IllegalAccessException: " + e2.getMessage());
            }
        }// actionPerformed()

        /**
         * debugging
         */
        private void debugOut(final Exception e) {
            System.out.println("ReflexiveActionListener:");
            System.out.println("   target: " + target);
            System.out.println("   targetMethod: " + targetMethod);
            System.out.println("   cause: " + e.getCause());
        }
    }// inner class ReflexiveActionListener


    /**
     * Refresh the Undo and Redo items, but take into account
     * if UndoRedoManager is null; if so, then the items are
     * always disabled.
     */
    private void refreshUndoRedo() {
        if (clientFrame.getUndoRedoManager() != null) {
            clientFrame.getUndoRedoManager()
                    .refreshMenu();    // EDIT_UNDO and EDIT_REDO
        } else {
            setEnabled(ClientMenu.EDIT_UNDO, false);
            setEnabled(ClientMenu.EDIT_REDO, false);
        }
    }// refreshUndoRedo()


}// class ClientMenu
