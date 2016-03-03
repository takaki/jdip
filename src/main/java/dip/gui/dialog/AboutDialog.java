//
//  @(#)AboutDialog.java	1.00	4/1/2002
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
package dip.gui.dialog;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;
import dip.gui.ClientFrame;
import dip.gui.swing.SwingWorker;
import dip.gui.swing.XJScrollPane;
import dip.misc.Log;
import dip.misc.Utils;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;


/**
 * About dialog
 * <p>
 * A cached copy is kept since this dialog takes about 600+ ms to construct.
 * The cached copy is created at startup. It really helps speed! (at the expense of memory...)
 */
public class AboutDialog extends HeaderDialog {
    // i18n constants
    public static final String TITLE = "AboutDialog.title";
    public static final String LOADING = "AboutDialog.loading";

    public static final String ABOUT_URL = "AboutDialog.location.about";
    public static final String LICENSE_URL = "AboutDialog.location.license";
    public static final String CREDITS_URL = "AboutDialog.location.credits";

    private static final String HEADER_LOCATION = "AboutDialog.location.header";
    private static final String TABLE_HEADERS[] = {Utils.getLocalString(
            "AboutDialog.header.property"), Utils.getLocalString(
            "AboutDialog.header.value")};

    private static final String TAB_INFO = "AboutDialog.tab.info";
    private static final String TAB_ABOUT = "AboutDialog.tab.about";
    private static final String TAB_LICENSE = "AboutDialog.tab.license";
    private static final String TAB_CREDITS = "AboutDialog.tab.credits";


    // constants
    private static final int SYS_BORDER = 10;

    // GUI
    private JTabbedPane tabPane;
    private JPanel aboutPanel;
    private JPanel systemPanel;
    private JPanel licensePanel;
    private JPanel creditPanel;

    // for speed
    private static AboutDialog dialogInstance = null;
    private static SwingWorker loader = null;


    /**
     * Create a cached copy of this dialog
     */
    public static synchronized void createCachedDialog(final JFrame parent) {
        if (dialogInstance == null) {
            if (loader == null) {
                loader = new SwingWorker() {
                    public Object construct() {
                        final long time = System.currentTimeMillis();
                        final AboutDialog ad = new AboutDialog(parent);
                        ad.pack();
                        ad.setSize(new Dimension(450, 575));
                        Log.printTimed(time,
                                "AboutDialog construct() complete: ");
                        return ad;
                    }// construct()
                };

                loader.start(Thread.MIN_PRIORITY);
            } else {
                Log.println("AboutDialog waiting...");
                dialogInstance = (AboutDialog) loader.get();
                loader = null;
            }
        }
    }// createCachedDialog()


    /**
     * Display the dialog
     */
    public static void displayDialog(final JFrame parent) {
        createCachedDialog(parent);    // if we haven't already
        Utils.centerInScreen(dialogInstance);
        dialogInstance.tabPane
                .setSelectedIndex(0);    // always reset to first tab
        dialogInstance.setVisible(true);
    }// displayDialog()


    /**
     * Creates the dialog
     */
    private AboutDialog(final JFrame parent) {
        super(parent, Utils.getLocalString(TITLE), true);
        makeTextPanels();
        makeSystemPanel();
        makeLicensePanel();

        tabPane = new JTabbedPane(JTabbedPane.TOP);
        tabPane.add(Utils.getLocalString(TAB_ABOUT), aboutPanel);
        tabPane.add(Utils.getLocalString(TAB_CREDITS), creditPanel);
        tabPane.add(Utils.getLocalString(TAB_LICENSE), licensePanel);
        tabPane.add(Utils.getLocalString(TAB_INFO), systemPanel);

        createDefaultContentBorder(tabPane);
        setHeaderText(Utils.getText(Utils.getLocalString(HEADER_LOCATION)));
        setContentPane(tabPane);
        addSingleButton(makeCloseButton());
    }// AboutDialog()


    /**
     * Create the About and Credits panels
     */
    private void makeTextPanels() {
        final Object[] args = {ClientFrame.getProgramName(), ClientFrame.getVersion()};
        aboutPanel = makeTextPanel(ABOUT_URL, args);

        creditPanel = makeTextPanel(CREDITS_URL, null);
    }// makeTextPanels()


    /**
     * Create the License tab
     */
    private void makeLicensePanel() {
        final JEditorPane textPanel = Utils.createTextLabel(LOADING, false);
        textPanel.setBorder(new EtchedBorder());

        final JScrollPane licenseScroller = new XJScrollPane(textPanel);
        licenseScroller.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        textPanel.setText(Utils.getText(Utils.getLocalString(LICENSE_URL)));
        textPanel.setCaretPosition(0);
        textPanel.repaint();
        licensePanel = new JPanel();

        final int[] w1 = {SYS_BORDER, 0, SYS_BORDER};
        final int[] h1 = {SYS_BORDER, 0, SYS_BORDER};

        final HIGLayout l1 = new HIGLayout(w1, h1);
        l1.setColumnWeight(2, 1);
        l1.setRowWeight(2, 1);
        licensePanel.setLayout(l1);

        final HIGConstraints c = new HIGConstraints();
        licensePanel.add(licenseScroller, c.rcwh(2, 2, 1, 1, "lrtb"));
        //licensePanel.add(new XJScrollPane(editPanel), c.rcwh(2,2,1,1,"lrtb"));
    }// makeAboutPanel()

    /**
     * Create the System tab
     */
    private void makeSystemPanel() {
        // create table model
        final DefaultTableModel tableModel = new DefaultTableModel(getSystemInfo(),
                TABLE_HEADERS) {
            public boolean isCellEditable(final int r, final int c) {
                return false;
            }
        };

        // create the table
        final JTable sysTable = new JTable(tableModel) {
            public boolean isFocusable() {
                return false;
            }
        };
        sysTable.setRowSelectionAllowed(false);
        sysTable.setColumnSelectionAllowed(false);
        sysTable.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        sysTable.setShowGrid(true);

        // create / layout the panel
        systemPanel = new JPanel();

        final int[] w1 = {SYS_BORDER, 0, SYS_BORDER};
        final int[] h1 = {SYS_BORDER, 0, SYS_BORDER};

        final HIGLayout l1 = new HIGLayout(w1, h1);
        l1.setColumnWeight(2, 1);
        l1.setRowWeight(2, 1);
        systemPanel.setLayout(l1);

        final HIGConstraints c = new HIGConstraints();
        systemPanel.add(new XJScrollPane(sysTable), c.rcwh(2, 2, 1, 1, "lrtb"));
    }// makeSystemPanel();


    /**
     * Get system properties. Returns an N x 2 array, suitable for a
     * JTable. e.g. String[n][x] where n = 0 to # properties, and x
     * = 0 (for property name) or 1 (property value).
     */
    private String[][] getSystemInfo() {
        final Properties p = System.getProperties();
        SortProp[] sortProps = null;

        synchronized (p) {
            // create 2 arrays, so that one can be sorted. We will combine these
            // into a 2D array later.
            sortProps = new SortProp[p.size()];

            final Enumeration enm = p.propertyNames();
            int idx = 0;
            while (enm.hasMoreElements()) {
                try {
                    final String propName = (String) enm.nextElement();
                    sortProps[idx] = new SortProp(propName,
                            p.getProperty(propName));
                    idx++;
                } catch (final ClassCastException e) {
                    // do nothing; just ignore property.
                }
            }
        }

        // sort arrays, & create 2D array.
        Arrays.sort(sortProps);

        final String[][] tableArray = new String[sortProps.length][];
        for (int i = 0; i < sortProps.length; i++) {
            tableArray[i] = new String[2];
            tableArray[i][0] = sortProps[i].getName();
            tableArray[i][1] = sortProps[i].getValue();
        }

        return tableArray;
    }// getSystemInfo()


    /**
     * inner class to create a sorted system property list
     */
    private class SortProp implements Comparable<SortProp> {
        private String name;
        private String value;

        public SortProp(final String name, final String value) {
            this.name = name;
            this.value = value;
        }// SortProp()

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public int compareTo(final SortProp obj) {
            return name.compareTo(((SortProp) obj).name);
        }// compareTo()
    }// inner class SortProp


    /**
     * Make text panel
     */
    private JPanel makeTextPanel(final String url, final Object[] args) {
        final JEditorPane editPanel = Utils.createTextLabel(true);

        String text = Utils.getText(Utils.getLocalString(url));

        if (text != null) {
            if (args != null) {
                text = Utils.format(text, args);
            }
        } else {
            text = "<html>Error: No text available.";
        }

        editPanel.setText(text);

        // create / layout the panel
        final JPanel panel = new JPanel();

        final int[] w1 = {SYS_BORDER, 0, SYS_BORDER};
        final int[] h1 = {SYS_BORDER, 0, SYS_BORDER};

        final HIGLayout l1 = new HIGLayout(w1, h1);
        l1.setColumnWeight(2, 1);
        l1.setRowWeight(2, 1);
        panel.setLayout(l1);

        final HIGConstraints c = new HIGConstraints();
        panel.add(editPanel, c.rcwh(2, 2, 1, 1, "lrtb"));
        return panel;
    }// makeTextPanel()

}// class AboutDialog
