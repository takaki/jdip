//
//  @(#)NGDVariantSelect.java	4/2002
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
package dip.gui.dialog.newgame;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;
import dip.gui.ClientFrame;
import dip.gui.dialog.ErrorDialog;
import dip.gui.swing.GradientJLabel;
import dip.gui.swing.XJScrollPane;
import dip.misc.Utils;
import dip.world.InvalidWorldException;
import dip.world.RuleOptions;
import dip.world.World;
import dip.world.WorldFactory;
import dip.world.variant.VariantManager;
import dip.world.variant.data.MapGraphic;
import dip.world.variant.data.SymbolPack;
import dip.world.variant.data.Variant;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Panel for New Game Dialog that allows selection of a map / variant / map graphic.
 * <p>
 */
public class NGDVariantSelect extends JPanel implements NewGameDialog.NGDTabPane {
    // i18n constants
    private static final String TAB_NAME = "NGDvariant.tab.name";
    private static final String LOADING_TEXT = "NGDvariant.text.loading";
    private static final String INITIAL_TEXT = "NGDvariant.text.initial";
    private static final String LABEL_LIST = "NGDvariant.label.list";
    private static final String LABEL_DESCRIPTION = "NGDvariant.label.description";
    private static final String TEMPLATE_LOCATION = "NGDvariant.location.template";

    // misc constants
    private static final int BORDER = 5;


    // instance variables
    private ClientFrame parent;
    private NewGameDialog ngd;
    private boolean isLoading = false;

    private DefaultListModel listModel;
    private JList variantList;
    private JEditorPane textPanel;
    private String description;
    private Variant defaultVariant = null;

    /**
     * Create a Variant Selection panel
     */
    protected NGDVariantSelect(final ClientFrame parent, final NewGameDialog ngd) {
        this.parent = parent;
        this.ngd = ngd;

        // textPanel setup
        textPanel = Utils
                .createTextLabel(Utils.getLocalString(LOADING_TEXT), false,
                        true);
        textPanel.setContentType("text/html");

        // get description template HTML; requires textPanel to be setup first
        description = Utils.getText(Utils.getLocalString(TEMPLATE_LOCATION));
        if (description == null) {
            description = "ERROR: missing template resource " + Utils
                    .getLocalString(TEMPLATE_LOCATION);
        }

        // variant list (based on map)
        listModel = new DefaultListModel();
        makeVariantList();
        variantList = new JList(listModel);
        variantList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        variantList.setPrototypeCellValue("MMMMMMMMMMMMMM");
        variantList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                doVariantListSelection();
            }
        });


        // create layout
        makeLayout();

        // NOTE: to properly scroll to the correctly selected item,
        // the JList must be in a scrollpane. Thus, makeLayout() must come
        // prior to this code.
        if (!listModel.isEmpty()) {
            // search and find the (last) default
            // if no default exists, select the first variant.
            final Variant[] variants = new VariantManager().getVariants()
                    .toArray(new Variant[0]);
            defaultVariant = null;
            for (Variant variant : variants) {
                if (variant.isDefault()) {
                    defaultVariant = variant;
                }
            }

            if (defaultVariant != null) {
                variantList.setSelectedValue(defaultVariant.getName(), true);
            } else {
                variantList.setSelectedIndex(0);
            }
        }
    }// NewGameDialog()


    /**
     * Gets the initial (default) selected variant.
     */
    Variant getDefaultVariant() {
        return defaultVariant;
    }// getDefaultVariant()


    /**
     * Returns the newly created World, or null, based upon the user's selections
     */
    protected World getWorld() {
        // check parameters
        final int idx = variantList.getSelectedIndex();
        if (isLoading || idx < 0) {
            return null;
        }

        // create world, based on selected variant
        World world = null;
        try {
            final WorldFactory wf = WorldFactory.getInstance();
            final Variant variant = ngd.getStartOptionsPanel().getVariant();
            world = WorldFactory.createWorld(variant);

            // set basic variant parameters
            final World.VariantInfo variantInfo = world.getVariantInfo();
            variantInfo.setVariantName(variant.getName());
            variantInfo.setVariantVersion(variant.getVersion());

            // set map/symbols (from Map-Symbol panel)
            final NGDMapAndUnits ngdMAU = ngd.getMAUPanel();

            final SymbolPack sp = ngdMAU.getSelectedSymbolPack();
            variantInfo.setSymbolPackName(sp.getName());
            variantInfo.setSymbolPackVersion(sp.getVersion());

            final MapGraphic mg = ngdMAU.getSelectedMap();
            variantInfo.setMapName(mg.getName());

            // set RuleOptions
            final RuleOptions ruleOpts = ngd.getRuleOptionsPanel().getRuleOptions();
            world.setRuleOptions(ruleOpts);
        } catch (final InvalidWorldException iwe) {
            world = null;
            ErrorDialog.displayGeneral(parent, iwe);
        }

        return world;
    }// getWorld()


    /**
     * Initializes the variant list
     */
    private void makeVariantList() {
        final Variant[] variants = new VariantManager().getVariants()
                .toArray(new Variant[0]);
        listModel.clear();
        for (Variant variant : variants) {
            listModel.addElement(variant.getName());
        }
    }// makeVariantList()

    void ensureSelectionIsVisible() {
        variantList.ensureIndexIsVisible(variantList.getSelectedIndex());
    }// ensureSelectionIsVisible()


    /**
     * Handles list selections
     */
    void doVariantListSelection() {
        // this prevents various layout-manager exceptions
        if (!isVisible()) {
            return;
        }

        final int idx = variantList.getSelectedIndex();
        if (idx < 0) {
            textPanel.setText(Utils.getLocalString(INITIAL_TEXT));
            ngd.getStartOptionsPanel().setEnabled(false);
            return;
        } else {
            variantList.ensureIndexIsVisible(idx);
        }

        final Variant selectedVariant = new VariantManager().getVariants()
                .get(idx);

        // set text, depending upon selection
        //
        textPanel.setText(Utils.format(description,
                selectedVariant.getHTMLSummaryArguments()
                        .toArray(new Object[0])));
        textPanel.setCaretPosition(0);    // scroll to top
        textPanel.repaint();

        ngd.setTabsVariant(selectedVariant);
        ngd.setTabsEnabled(true);
    }// doVariantListSelection()


    /**
     * Get the tab name.
     */
    public String getTabName() {
        return Utils.getLocalString(TAB_NAME);
    }// getTabName()

    /**
     * The Variant has Changed.
     */
    public void variantChanged(final Variant variant) {
        // DO NOTHING
    }// variantChanged()

    /**
     * The Enabled status has Changed. We do nothing for this tab.
     */
    public void enablingChanged(final boolean enabled) {
        // DO NOTHING
    }// enablingChanged()


    /**
     * Layout the panel
     */
    private void makeLayout() {
        final JScrollPane descSP = new XJScrollPane(textPanel);
        descSP.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        final JScrollPane varSP = new XJScrollPane(variantList);
        varSP.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        final int[] w1 = {BORDER, 0, 15, 0, BORDER};    // cols
        final int[] h1 = {2 * BORDER, 0, 5, 0, BORDER};    // rows

        final HIGLayout l1 = new HIGLayout(w1, h1);
        l1.setColumnWeight(4, 1);
        l1.setRowWeight(4, 1);

        setLayout(l1);
        final HIGConstraints c = new HIGConstraints();

        add(new GradientJLabel(Utils.getLocalString(LABEL_LIST)),
                c.rcwh(2, 2, 1, 1, "lr"));
        add(varSP, c.rcwh(4, 2, 1, 1, "lrtb"));

        add(new GradientJLabel(Utils.getLocalString(LABEL_DESCRIPTION)),
                c.rcwh(2, 4, 1, 1, "lr"));
        add(descSP, c.rcwh(4, 4, 1, 1, "lrtb"));
    }// makeLayout()

}// class NGDVariantSelect

