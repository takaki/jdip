//
//  @(#)NGDStartOptions.java	4/2002
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
import dip.gui.dialog.newgame.NewGameDialog.NGDTabPane;
import dip.gui.swing.GradientJLabel;
import dip.misc.Utils;
import dip.world.Phase;
import dip.world.variant.data.Variant;

import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Defines the game starting options for the New Game dialog.
 * <p>
 * This allows the user to alter any game parameters before the
 * game starts.
 * <p>
 * Reset button allows reversion to defaults.
 */
public class NGDStartOptions extends JPanel implements NGDTabPane {
    // constants
    private static final int BORDER = 5;
    private static final String TAB_NAME = "NGDoptions.tab.name";
    private static final String BUTTON_RESET = "NGDoptions.button_reset";
    private static final String LABEL_TIME = "NGDoptions.label_time";
    private static final String LABEL_VC = "NGDoptions.label_vc";
    private static final String LABEL_VC_SC_REQ = "NGDoptions.label_vc_sc_req";
    private static final String LABEL_VC_SC_NOCHANGE = "NGDoptions.label_vc_sc_nochange";
    private static final String LABEL_VC_DURATION = "NGDoptions.label_vc_duration";
    private static final String INTRO_TEXT_LOCATION = "NGDoptions.location_intro_text";

    // instance variables
    private Variant current = null;    // current variant
    private Variant original = null;

    // GUI controls
    private JComboBox phaseBox;
    private BCSpinner year;
    private JSpinner vcSC;
    private JSpinner vcSCChange;
    private JSpinner vcDuration;
    private JButton reset;
    private JEditorPane introText;


    /**
     * Create the StartOptions panel for the New Game dialog
     */
    public NGDStartOptions() {
        // create GUI controls
        phaseBox = new JComboBox();
        phaseBox.setEditable(false);

        year = new BCSpinner(1, 1, 9999);
        vcSC = makeCustomSpinner(true);
        vcSCChange = makeCustomSpinner(true);
        vcDuration = makeCustomSpinner(true);

        reset = new JButton(Utils.getLocalString(BUTTON_RESET));
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (original != null) {
                    resetData();
                }
            }
        });


        introText = Utils.createTextLabel(true);
        introText.setText(
                Utils.getText(Utils.getLocalString(INTRO_TEXT_LOCATION)));
        introText.setMargin(new Insets(10, 0, 10, 0));

        makeLayout();
    }// NGDStartOptions()


    /**
     * Gets the (possibly) modified Variant
     */
    public synchronized Variant getVariant() {
        // set game time
        String sb = String.valueOf(phaseBox.getSelectedItem()) +
                ' ' +
                year.getValue();
        current.setStartingPhase(Phase.parse(sb).orElse(null));

        // set victory conditions
        current.setNumSCForVictory(getSpinnerValue(vcSC));
        current.setMaxYearsNoSCChange(getSpinnerValue(vcSCChange));
        current.setMaxGameTimeYears(getSpinnerValue(vcDuration));

        return current;
    }// getVariant()


    /**
     * Enables & Disables controls on this panel
     */
    @Override
    public void setEnabled(final boolean value) {
        phaseBox.setEnabled(value);
        year.setEnabled(value);
        vcSC.setEnabled(value);
        vcSCChange.setEnabled(value);
        vcDuration.setEnabled(value);
        reset.setEnabled(value);
        introText.setEnabled(value);
        super.setEnabled(value);
    }// setEnabled()


    /**
     * Set data from originally passed reference
     */
    private void resetData() {
        final Phase phase = original.getStartingPhase();

        // set combo box
        phaseBox.removeAllItems();
        final String[] combos = Phase.getAllSeasonPhaseCombos().toArray(new String[0]);
        String protoType = combos[0];
        for (String combo : combos) {
            phaseBox.addItem(combo);
            protoType = combo.length() > protoType
                    .length() ? combo : protoType;
        }

        phaseBox.setPrototypeDisplayValue(protoType + "M");

        // set currently selected
        phaseBox.setSelectedItem(
                phase.getSeasonType() + " " + phase.getPhaseType());

        // set if we can display 'bc' years
        if (original.getBCYearsAllowed()) {
            year.setMinimum(-9999);    // 9999 BC is earliest
        } else {
            year.setMinimum(1);    // no earlier than 1 AD
        }

        // set year
        year.setValue(phase.getYear());

        // set spinners
        setSpinner(vcSC, original.getNumSCForVictory(), 1,
                original.getSupplyCenters().size());
        setSpinner(vcSCChange, original.getMaxYearsNoSCChange(), 0, 9999);
        setSpinner(vcDuration, original.getMaxGameTimeYears(), 0, 9999);
    }// resetData()


    /**
     * Layout the panel
     */
    private void makeLayout() {
        final int[] w1 = {BORDER, 15, 0, 0, BORDER};
        final int[] h1 = {BORDER, 0, 10, 0, 5, 0, 20, 0, 5, 0, 5, 0, 5, 0, 5, 0, 0, 5, 0, BORDER};

        // create 'minipanels': these put labels and spinners horizontally adjacent
        final JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timePanel.add(phaseBox);
        timePanel.add(year);

        final JPanel vc1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        vc1.add(vcSC);
        vc1.add(new JLabel(Utils.getLocalString(LABEL_VC_SC_REQ)));

        final JPanel vc2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        vc2.add(vcSCChange);
        vc2.add(new JLabel(Utils.getLocalString(LABEL_VC_SC_NOCHANGE)));

        final JPanel vc3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        vc3.add(vcDuration);
        vc3.add(new JLabel(Utils.getLocalString(LABEL_VC_DURATION)));


        // higlayout
        final HIGLayout l1 = new HIGLayout(w1, h1);
        l1.setColumnWeight(4, 1);
        l1.setRowWeight(16, 1);

        setLayout(l1);
        final HIGConstraints c = new HIGConstraints();

        add(introText, c.rcwh(2, 2, 3, 1, "lrtb"));

        add(new GradientJLabel(Utils.getLocalString(LABEL_TIME)),
                c.rcwh(4, 2, 2, 1, "lr"));
        add(timePanel, c.rc(6, 3, "l"));

        add(new GradientJLabel(Utils.getLocalString(LABEL_VC)),
                c.rcwh(8, 2, 2, 1, "lr"));
        add(vc1, c.rc(10, 3, "l"));
        add(vc2, c.rc(12, 3, "l"));
        add(vc3, c.rc(14, 3, "l"));

        add(new JSeparator(), c.rcwh(17, 2, 3, 1, "lr"));

        add(reset, c.rcwh(19, 2, 2, 1, "l"));
    }// makeLayout()


    /**
     * Creates a custom spinner. Custom spinners have no commas in the numbers,
     * and cannot be negative. 0 is acceptable. Maximum is set to 9999 by default.
     * <p>
     * useSeparator: determine if grouping separator is used for numbers.
     */
    private JSpinner makeCustomSpinner(final boolean useSeparator) {
        final SpinnerNumberModel model = new SpinnerNumberModel(0, 0, 9999, 1);
        final JSpinner spinner = new JSpinner(model);
        new NumberEditor(spinner, "0000");

        if (!useSeparator) {
            // remove comma (grouping separator)
            ((NumberEditor) spinner.getEditor()).getFormat()
                    .setGroupingUsed(false);
        }

        return spinner;
    }// makeCustomSpinner()

    /**
     * Conveniently sets spinner values & handles any details
     */
    private void setSpinner(final JSpinner spinner, final int value, final int min, final int max) {
        final SpinnerNumberModel snn = (SpinnerNumberModel) spinner.getModel();
        snn.setMinimum(new Integer(min));
        snn.setMaximum(new Integer(max));
        snn.setValue(new Integer(value));
    }// setSpinner()

    /**
     * Convenience method: get int from spinner
     */
    private int getSpinnerValue(final JSpinner spinner) {
        return ((Integer) spinner.getValue()).intValue();
    }// getSpinnerValue()

    /**
     * Get the tab name.
     */
    @Override
    public String getTabName() {
        return Utils.getLocalString(TAB_NAME);
    }// getTabName()

    /**
     * The Variant has Changed.
     */
    @Override
    public void variantChanged(final Variant variant) {
        // store passed reference, but modify the cloned version
        original = variant;
        try {
            current = (Variant) variant.clone();
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e.getMessage());
        }

        resetData();
        revalidate();
    }// variantChanged()

    /**
     * The Enabled status has Changed.
     */
    @Override
    public void enablingChanged(final boolean enabled) {
        setEnabled(enabled);
    }// enablingChanged()

}// class NGDStartOptions
