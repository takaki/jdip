//
//  @(#)GeneralPreferencePanel.java		4/2002
//
//  Copyright 2002-2004 Zachary DelProposto. All rights reserved.
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

package dip.gui.dialog.prefs;

import com.l2fprod.common.swing.JDirectoryChooser;
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;
import dip.gui.ClientFrame;
import dip.gui.OrderDisplayPanel;
import dip.gui.map.MapRenderer2;
import dip.gui.swing.AssocJComboBox;
import dip.gui.swing.AssocJComboBox.AssociatedObj;
import dip.gui.swing.XJFileChooser;
import dip.misc.LRUCache;
import dip.misc.SharedPrefs;
import dip.misc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * General preferences.
 * <p>
 * Static methods are included to access (in a controlled manner) preference-controlled
 * functionality.
 */
public class GeneralPreferencePanel extends PreferencePanel {
    private static final Logger LOG = LoggerFactory.getLogger(
            GeneralPreferencePanel.class);
    // constants
    public static final int BORDER = 10;

    // preference nodes keys
    public static final String NODE_SAVE_WINDOW_SETTINGS = "saveWindowSettings";
    public static final String NODE_DEFAULT_GAME_DIR = "defaultGameDir";
    public static final String NODE_SHOW_RESOLUTION_RESULTS = "showResolutionResults";

    // recent files (w/o #) [if enabled]
    public static final String NODE_RECENT_FILE = "lastFile";

    // window settings [if enabled]
    public static final String NODE_WINDOW_X = "windowX";
    public static final String NODE_WINDOW_Y = "windowY";
    public static final String NODE_WINDOW_WIDTH = "windowWidth";
    public static final String NODE_WINDOW_HEIGHT = "windowHeight";

    // order sort preference
    public static final String NODE_ORDER_SORTING = "orders.list.sorting";
    public static final String NODE_ORDER_SORTING_REVERSE = "orders.list.sorting.reversed";    // boolean

    // map label preference
    public static final String NODE_MAP_LABEL_LEVEL = "map.label.level";

    // variant dir
    public static final String NODE_VARIANT_DIR = "variant.dir";

    // # of recent files to save
    private static final int NUM_RECENT_FILES = 5;

    // LRU cache of files
    private static LRUCache fileCache = null;
    private static Collator collator = null;

    // UI Elements
    private JCheckBox saveWindowSettings;                // save window settings (position, size)
    private JTextField saveDir;                        // default save-files directory
    private JButton browseSaveDir;                    // browse button for setting directory
    private JButton clearMRU;                        // clears most-recently-used file list

    private JCheckBox reverseSort;    // reverse the order sort direction
    private AssocJComboBox orderSorting;    // order sorting type
    private AssocJComboBox mapLabels;        // map label level

    private JCheckBox showResolution;

    private ClientFrame cf = null;

    // UI text i18n constants
    private static final String TAB_NAME = "GPP.tabname";
    private static final String GPP_SAVE_WINDOW_POS = "GPP.save_window_pos";
    private static final String GPP_SAVE_DIR_TEXT = "GPP.save_dir_text";
    private static final String GPP_SAVE_DIR_BUTTON = "GPP.save_dir_button";

    private static final String GPP_SHOW_RESOLUTION_RESULTS = "GPP.show_resolution_results";
    private static final String GPP_CLEAR_MRU_BUTTON = "GPP.clearmru.button";
    private static final String GPP_CLEAR_MRU_TEXT = "GPP.clearmru.text";

    private static final String GPP_MAP_LABEL_PREFIX = "GPP.map.label.level.";
    private static final String GPP_ORDER_SORT_PREFIX = "GPP.order.sort.type.";
    private static final String GPP_MAP_LABEL = "GPP.map.label.text";
    private static final String GPP_MAP_LABEL_NOTE = "GPP.map.label.text.note";
    private static final String GPP_ORDER_SORT_LABEL = "GPP.order.sort.label";
    private static final String GPP_ORDER_SORT_DIRECTION = "GPP.order.sort.direction";


    private static final String DIALOG_TITLE = "GPP.filedialog.title";
    private static final String SELECT_VARIANT_DIALOG_TITLE = "GPP.selectvariantdir.title";


    public GeneralPreferencePanel(final ClientFrame cf) {
        this.cf = cf;

        // create UI elements
        saveWindowSettings = new JCheckBox(
                Utils.getLocalString(GPP_SAVE_WINDOW_POS));

        showResolution = new JCheckBox(
                Utils.getLocalString(GPP_SHOW_RESOLUTION_RESULTS));

        saveDir = new JTextField();
        saveDir.setEditable(false);
        browseSaveDir = new JButton(Utils.getLocalString(GPP_SAVE_DIR_BUTTON));
        browseSaveDir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                directoryBrowse();
            }// actionPerformed()
        });


        clearMRU = new JButton(Utils.getLocalString(GPP_CLEAR_MRU_BUTTON));
        clearMRU.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                clearFileList();
                cf.getClientMenu().updateRecentFiles();
            }// actionPerformed()
        });

        // setup associative arrays for comboboxes
        String[] arr = new String[]{MapRenderer2.VALUE_LABELS_NONE, MapRenderer2.VALUE_LABELS_BRIEF, MapRenderer2.VALUE_LABELS_FULL};

        AssociatedObj[] assocObjs = AssociatedObj
                .createAssociatedObjects(arr, GPP_MAP_LABEL_PREFIX,
                        MapRenderer2.VALUE_LABELS_NONE, true);
        mapLabels = new AssocJComboBox(assocObjs);


        // order sorting :: implement
        arr = new String[]{OrderDisplayPanel.SORT_POWER, OrderDisplayPanel.SORT_PROVINCE, OrderDisplayPanel.SORT_UNIT, OrderDisplayPanel.SORT_ORDER};

        final String[] arr2 = new String[]{Utils.getLocalString(
                OrderDisplayPanel.LABEL_SORT_POWER), Utils.getLocalString(
                OrderDisplayPanel.LABEL_SORT_PROVINCE), Utils.getLocalString(
                OrderDisplayPanel.LABEL_SORT_UNIT), Utils.getLocalString(
                OrderDisplayPanel.LABEL_SORT_ORDER)};

        assocObjs = AssociatedObj
                .createAssociatedObjects(arr, arr2,
                        OrderDisplayPanel.SORT_POWER, true);
        orderSorting = new AssocJComboBox(assocObjs);

        reverseSort = new JCheckBox(
                Utils.getLocalString(GPP_ORDER_SORT_DIRECTION));

        // update components
        getSettings();

        // mini-panels
        final JPanel clrPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        clrPanel.add(new JLabel(Utils.getLocalString(GPP_CLEAR_MRU_TEXT)));
        clrPanel.add(Box.createHorizontalStrut(5));
        clrPanel.add(clearMRU);

        final JPanel orderP = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        orderP.add(new JLabel(Utils.getLocalString(GPP_ORDER_SORT_LABEL)));
        orderP.add(Box.createHorizontalStrut(5));
        orderP.add(orderSorting);
        orderP.add(Box.createHorizontalStrut(10));
        orderP.add(reverseSort);

        final JPanel mapP = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        mapP.add(new JLabel(Utils.getLocalString(GPP_MAP_LABEL)));
        mapP.add(Box.createHorizontalStrut(5));
        mapP.add(mapLabels);

        // layout
        final int[] h1 = {BORDER, 0, 8, 0, 8, 0, 8, 0, 3, 0, 7, 36,    // upto row 12
                0, 3, 0, 10, 0, 8, 0, BORDER};
        final int[] w1 = {BORDER, 10, 0, 5, 0, BORDER};

        final HIGLayout l1 = new HIGLayout(w1, h1);
        l1.setColumnWeight(3, 1);
        l1.setRowWeight(19, 1);
        setLayout(l1);


        final HIGConstraints c = new HIGConstraints();
        add(saveWindowSettings, c.rcwh(2, 2, 4, 1, "l"));
        add(showResolution, c.rcwh(4, 2, 4, 1, "l"));

        add(orderP, c.rcwh(6, 2, 4, 1, "l"));

        add(mapP, c.rcwh(8, 2, 4, 1, "l"));
        add(new JLabel(Utils.getLocalString(GPP_MAP_LABEL_NOTE)),
                c.rcwh(10, 3, 3, 1, "ltb"));

        // separator bar (12)
        add(new JSeparator(), c.rcwh(12, 2, 4, 1, "lr"));

        // save dir
        add(new JLabel(Utils.getLocalString(GPP_SAVE_DIR_TEXT)),
                c.rcwh(13, 2, 4, 1, "l"));
        add(saveDir, c.rcwh(15, 3, 1, 1, "lr"));
        add(browseSaveDir, c.rcwh(15, 5, 1, 1, "lrtb"));

        // clear MRU
        add(clrPanel, c.rcwh(17, 2, 4, 1, "l"));

    }// GeneralPreferencePanel()


    private void directoryBrowse() {
        // setup
        final JDirectoryChooser chooser = new JDirectoryChooser();
        chooser.setMultiSelectionEnabled(false);

        // set directory
        final String path = saveDir.getText();
        if (!"".equals(path)) {
            chooser.setCurrentDirectory(new File(path));
        }

        chooser.setDialogTitle(Utils.getLocalString(DIALOG_TITLE));
        final int choice = chooser.showDialog(cf,
                Utils.getLocalString(XJFileChooser.BTN_DIR_SELECT));
        if (choice == JDirectoryChooser.APPROVE_OPTION) {
            saveDir.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }// directoryBrowse()


    @Override
    public void apply() {
        final Preferences prefs = SharedPrefs.getUserNode();

        // apply settings
        prefs.putBoolean(NODE_SAVE_WINDOW_SETTINGS,
                saveWindowSettings.isSelected());
        prefs.put(NODE_DEFAULT_GAME_DIR, saveDir.getText());
        prefs.putBoolean(NODE_SHOW_RESOLUTION_RESULTS,
                showResolution.isSelected());

        // map settings
        prefs.put(NODE_MAP_LABEL_LEVEL, (String) mapLabels.getSelectedValue());

        // order settings
        prefs.putBoolean(NODE_ORDER_SORTING_REVERSE, reverseSort.isSelected());
        prefs.put(NODE_ORDER_SORTING, (String) orderSorting.getSelectedValue());

        try {
            prefs.flush();
        } catch (final BackingStoreException bse) {
        }
    }// apply()


    @Override
    public void cancel() {
        // do nothing
    }// cancel()


    @Override
    public void setDefault() {
        saveWindowSettings.setSelected(false);
        showResolution.setSelected(false);
        saveDir.setText("");

        reverseSort.setSelected(false);
        orderSorting.reset();
        mapLabels.reset();
    }// applyDefault()


    @Override
    public String getName() {
        return Utils.getLocalString(TAB_NAME);
    }// getName()


    private void getSettings() {
        final Preferences prefs = SharedPrefs.getUserNode();
        try {
            prefs.sync();
        } catch (final BackingStoreException bse) {
        }

        saveWindowSettings.setSelected(
                prefs.getBoolean(NODE_SAVE_WINDOW_SETTINGS, false));
        showResolution.setSelected(
                prefs.getBoolean(NODE_SHOW_RESOLUTION_RESULTS, true));
        saveDir.setText(prefs.get(NODE_DEFAULT_GAME_DIR, ""));

        reverseSort.setSelected(
                prefs.getBoolean(NODE_ORDER_SORTING_REVERSE, false));

        // get MapLabels setting
        final String mlSetting = MapRenderer2
                .parseLabelValue(prefs.get(NODE_MAP_LABEL_LEVEL, null),
                        MapRenderer2.VALUE_LABELS_NONE);
        assert mlSetting != null;
        mapLabels.setSelectedItem(mlSetting);

        // get order sorting setting
        final String osSetting = OrderDisplayPanel
                .parseSortValue(prefs.get(NODE_ORDER_SORTING, null),
                        OrderDisplayPanel.SORT_PROVINCE);
        assert mlSetting != null;
        orderSorting.setSelectedItem(osSetting);
    }// getSettings()


    /**
     * Get the Map label-level setting, as set by the User in
     * Preferences. Never returns null.
     */
    public static String getMapLabelSetting() {
        final Preferences prefs = SharedPrefs.getUserNode();
        final String mlSetting = MapRenderer2
                .parseLabelValue(prefs.get(NODE_MAP_LABEL_LEVEL, null),
                        MapRenderer2.VALUE_LABELS_NONE);
        assert mlSetting != null;
        return mlSetting;
    }// getMapLabelSetting()

    /**
     * Get the order-sorting direction, as set by the user.
     * Returns <code>true</code> if sort direction is reversed.
     */
    public static boolean getOrderSortReverse() {
        final Preferences prefs = SharedPrefs.getUserNode();
        return prefs.getBoolean(NODE_ORDER_SORTING_REVERSE, false);
    }// getOrderSortReverse()


    /**
     * Get the order-sorting mode, as set by the user.
     * Never returns null.
     */
    public static String getOrderSortMode() {
        final Preferences prefs = SharedPrefs.getUserNode();
        final String osSetting = OrderDisplayPanel
                .parseSortValue(prefs.get(NODE_ORDER_SORTING, null),
                        OrderDisplayPanel.SORT_PROVINCE);
        return osSetting;
    }// getOrderSortMode()


    public static void getWindowSettings(final Component c) {
        final Preferences prefs = SharedPrefs.getUserNode();

        if (prefs.getBoolean(NODE_SAVE_WINDOW_SETTINGS, false)) {
            final int x = prefs.getInt(NODE_WINDOW_X, 50);
            final int y = prefs.getInt(NODE_WINDOW_Y, 50);
            final int w = prefs.getInt(NODE_WINDOW_WIDTH, 600);
            final int h = prefs.getInt(NODE_WINDOW_HEIGHT, 400);
            c.setSize(w, h);
            c.setLocation(x, y);
        } else {
            c.setSize(Utils.getScreenSize(0.85f));
            Utils.centerInScreen(c);
        }
    }// getWindowSettings()


    public static void saveWindowSettings(final Component c) {
        final Preferences prefs = SharedPrefs.getUserNode();

        if (prefs.getBoolean(NODE_SAVE_WINDOW_SETTINGS, false)) {
            prefs.putInt(NODE_WINDOW_X, c.getX());
            prefs.putInt(NODE_WINDOW_Y, c.getY());
            prefs.putInt(NODE_WINDOW_WIDTH, c.getWidth());
            prefs.putInt(NODE_WINDOW_HEIGHT, c.getHeight());
        }
        try {
            prefs.flush();
        } catch (final BackingStoreException bse) {
        }
    }// saveWindowSettings()


    // sort before returning; case insensitive
    // CALL THIS the first time
    public static String[] getRecentFileNamesFromPrefs() {
        if (fileCache == null) {
            // load from preferences
            final Preferences prefs = SharedPrefs.getUserNode();

            // get files
            final ArrayList al = new ArrayList(NUM_RECENT_FILES);
            for (int i = 0; i < NUM_RECENT_FILES; i++) {
                final String s = prefs.get(NODE_RECENT_FILE + i, "");
                if (s != null && !s.isEmpty()) {
                    // do NOT add file if it doesn't exist.
                    final File file = new File(s);
                    if (file.exists()) {
                        al.add(file);
                    }
                }
            }

            // add to cache & create a String of just the name
            fileCache = new LRUCache(NUM_RECENT_FILES);
            final String[] s = new String[al.size()];
            for (int i = 0; i < al.size(); i++) {
                final File file = (File) al.get(i);
                fileCache.put(file.getName(), file);
                s[i] = file.getName();
            }

            // sort [note; should use CollationKey if sorting more items...]
            if (collator == null) {
                collator = Collator.getInstance();
                collator.setStrength(Collator.PRIMARY);        // ignore case
            }
            Arrays.sort(s, collator);
            return s;
        }

        return new String[0];
    }// getRecentFileNamesFromPrefs()

    // this may return null!!
    public static File getFileFromName(final String name) {
        if (fileCache != null) {
            return (File) fileCache.get(name);
        }
        return null;
    }// getFileFromName()


    // CALL THIS most of the time; use to update the names for the menu
    public static String[] getRecentFileNamesFromCache() {
        if (fileCache != null) {
            // load from cache; sort and check for file existence before returning
            final ArrayList names = new ArrayList(NUM_RECENT_FILES);
            final Iterator iter = fileCache.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry mapEntry = (Entry) iter.next();
                final File file = (File) mapEntry.getValue();
                if (file.exists()) {
                    names.add(mapEntry.getKey());
                }
            }

            final String[] fileNames = (String[]) names
                    .toArray(new String[names.size()]);
            Arrays.sort(fileNames, collator);
            return fileNames;
        }


        return new String[0];
    }// getRecentFilesNames()


    /**
     * This method updates the recent file name preferences.
     * <p>
     * It should be called every time a file is opened, and
     * every time "Save As" is called.
     * <p>
     * Don't forget to update menus as well, after calling this
     * method.
     */
    public static void setRecentFileName(final File file) {
        if (fileCache != null) {
            final String name = file.getName();
            if (fileCache.containsKey(name)) {
                fileCache.get(name);    // update access order (if entry exists)
            } else {
                fileCache.put(name, file);
            }
            saveRecentFileNames();
        }
    }// setRecentFileName()


    /**
     * Clears the MRU file list. Does not update menu.
     */
    public static void clearFileList() {
        if (fileCache != null) {
            fileCache.clear();
            final Preferences prefs = SharedPrefs.getUserNode();
            for (int i = 0; i < NUM_RECENT_FILES; i++) {
                prefs.remove(NODE_RECENT_FILE + i);
            }

            try {
                prefs.flush();
            } catch (final BackingStoreException bse) {
            }
        }
    }// clearFileList()


    /**
     * Saves the recent file names
     */
    private static void saveRecentFileNames() {
        if (fileCache != null) {
            // get node
            final Preferences prefs = SharedPrefs.getUserNode();

            // use an iterator to preerve access-order.
            // save in reverse-order
            int idx = NUM_RECENT_FILES - 1;
            final Iterator iter = fileCache.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry mapEntry = (Entry) iter.next();
                final File file = (File) mapEntry.getValue();

                prefs.put(NODE_RECENT_FILE + idx,
                        file.getPath());
                idx--;
            }

            // delete any empty entries.
            while (idx > 0) {
                prefs.remove(NODE_RECENT_FILE + idx);
                idx--;
            }

            try {
                prefs.flush();
            } catch (final BackingStoreException bse) {
            }
        }
    }// saveRecentFileNames()


    /**
     * Returns the default save-game directory, or an empty directory ("") if none.
     */
    public static File getDefaultGameDir() {
        final Preferences prefs = SharedPrefs.getUserNode();
        return new File(prefs.get(NODE_DEFAULT_GAME_DIR, ""));
    }// getDefaultGameDir()


    /**
     * Returns if we should automatically bring up the resolution results dialog
     */
    public static boolean getShowResolutionResults() {
        return getSetting(NODE_SHOW_RESOLUTION_RESULTS, true);
    }


    /**
     * Returns a given setting from preferences
     */
    private static boolean getSetting(final String key, final boolean defaultValue) {
        final Preferences prefs = SharedPrefs.getUserNode();
        try {
            prefs.sync();
        } catch (final BackingStoreException bse) {
        }
        return prefs.getBoolean(key, defaultValue);
    }// getSetting()


    /**
     * Gets the user-set variant directory, if set. If not set, returns null.
     * This will also return null if the variant directory is not actually a
     * directory.
     */
    public static File getVariantDir() {
        final Preferences prefs = SharedPrefs.getUserNode();
        File file = null;
        final String text = prefs.get(NODE_VARIANT_DIR, null);
        if (text != null) {
            file = new File(text);
            if (!file.isDirectory()) {
                LOG.debug("GPP.getVariantDir(): not a directory : {}", text);
                return null;
            }
        }

        return file;
    }// getVariantDir()

    /**
     * Sets the variant directory. This is a GUI-based set method, and can
     * be used outside of the preference panel. This will return the selected
     * file. Note that if 'immediateCommit' is true, the returned File will be
     * written immediately to the preferences backing store (this is useful
     * when not used within the Preference dialog, but does not allow for
     * the undo operation).
     * <p>
     * This will return null if no file was selected.
     */
    public static File setVariantDir(final JFrame frame, final boolean immediateCommit) {
        final JDirectoryChooser chooser = new JDirectoryChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(
                Utils.getLocalString(SELECT_VARIANT_DIALOG_TITLE));

        File file = null;
        final int choice = chooser.showDialog(frame,
                Utils.getLocalString(XJFileChooser.BTN_DIR_SELECT));
        if (choice == JDirectoryChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile().getAbsoluteFile();
            if (immediateCommit && file != null) {
                final Preferences prefs = SharedPrefs.getUserNode();
                prefs.put(NODE_VARIANT_DIR, file.toString());
                try {
                    prefs.sync();
                } catch (final BackingStoreException bse) {
                }
            }
        }

        return file;
    }// setVariantDir()

}// class GeneralPreferencePanel
