//
//  @(#)OrderStatusPanel.java		5/2003
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

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;
import dip.gui.swing.XJPanel;
import dip.misc.Utils;
import dip.order.Orderable;
import dip.world.Phase;
import dip.world.TurnState;
import dip.world.World;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * OrderStatusPanel: contains a label that displays the current phase,
 * if a game is loaded. Also displays (when appropriate) a text field
 * where the user may enter orders in text format.
 */
public class OrderStatusPanel extends XJPanel {
    // i18n constnats
    private final static String LABEL_ORDER = "OP.label.order";
    private final static String EMPTY = "";

    // instance variables
    private JLabel orderFieldLabel;
    private JLabel phase;
    private JTextField orderField;
    private OSPPropertyListener propListener = null;
    private ClientFrame cf = null;


    /**
     * Creates an OrderStatusPanel object.
     */
    public OrderStatusPanel(final ClientFrame clientFrame) {
        cf = clientFrame;

        // setup labels
        phase = new JLabel(EMPTY);
        orderFieldLabel = new JLabel(Utils.getLocalString(LABEL_ORDER));


        // setup text field
        orderField = new dip.gui.swing.XJTextField();
        orderField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final String text = orderField.getText();

                if (text.equals(EMPTY)) {
                    return;
                }

                // add an order; if no error occured, clear
                // the textfield
                if (cf.getOrderDisplayPanel() != null) {
                    if (cf.getOrderDisplayPanel().addOrder(text, true)) {
                        orderField.setText(EMPTY);
                        orderField.repaint();
                    }
                }
            }
        });

        // setup propety listener
        propListener = new OSPPropertyListener();
        cf.addPropertyChangeListener(propListener);

        // do layout
        makeLayout();
    }// OrderStatusPanel()


    /**
     * Performs any cleanup.
     */
    public void close() {
        cf.removePropertyChangeListener(propListener);
    }// close()


    /**
     * Sets the text in the order text field.
     * <p>
     * Note that this does not parse the text;
     * however this text is "live", and the user may edit it.
     */
    public void setOrderText(final String value) {
        orderField.setText(value);
    }// setOrderText()


    /**
     * Clears the order TextField of any text
     */
    public void clearOrderText() {
        orderField.setText(EMPTY);
    }// clearOrderText()


    /**
     * Property change event listener
     */
    private class OSPPropertyListener extends AbstractCFPListener {
        @Override
        public void actionOrderCreated(final Orderable order) {
            clearOrderText();
        }

        @Override
        public void actionOrderDeleted(final Orderable order) {
            clearOrderText();
        }

        @Override
        public void actionOrdersCreated(final Orderable[] orders) {
            clearOrderText();
        }

        @Override
        public void actionOrdersDeleted(final Orderable[] orders) {
            clearOrderText();
        }

        @Override
        public void actionModeChanged(final String mode) {
            if (mode == ClientFrame.MODE_ORDER) {
                orderField.setVisible(true);
                orderFieldLabel.setVisible(true);
            } else {
                orderField.setVisible(false);
                orderFieldLabel.setVisible(false);
            }
        }// actionModeChanged()

        @Override
        public void actionTurnstateChanged(final TurnState turnState) {
            final Phase tsPhase = turnState.getPhase();

            // set game time
            String sb = "<html><h2>" +
                    tsPhase.toString() +
                    "</h2></html>";
            phase.setText(sb);
        }// actionTurnstateChanged()


        @Override
        public void actionWorldCreated(final World w) {
            phase.setText(EMPTY);
        }

        @Override
        public void actionWorldDestroyed(final World w) {
            phase.setText(EMPTY);
        }
    }// inner class OSPPropertyListener

    /**
     * Layout components
     */
    private void makeLayout() {
        // start layout
        final int[] w1 = {0, 5, 0};
        final int[] h1 = {5, 0, 25, 0, 10};

        final HIGLayout hl = new HIGLayout(w1, h1);
        hl.setColumnWeight(3, 1);
        hl.setRowWeight(2, 1);
        setLayout(hl);

        final HIGConstraints c = new HIGConstraints();

        add(phase, c.rcwh(2, 1, 3, 1, "lr"));
        add(orderFieldLabel, c.rc(4, 1, "l"));
        add(orderField, c.rc(4, 3, "lr"));
    }// makeLayout()

}// class OrderStatusPanel
