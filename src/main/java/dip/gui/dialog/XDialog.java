//
//  @(#)XDialog.java	1.00	4/1/2002
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

import dip.gui.AbstractCFPListener;
import dip.gui.ClientFrame;
import dip.misc.Help;
import dip.misc.Utils;
import dip.world.World;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Extended JDialog
 * <p>
 * Features:
 * <ol>
 * <li>Automatically disposes dialog if close-button pressed, although
 * this behavior can be changed by over-riding close()</li>
 * <li>Closes dialog if ESC pressed (calls close())</li>
 * <li>Internationalized button text, by default</li>
 * <li>Button constants</li>
 * <li>Help support</li>
 * <li>If non-modal, automatically closes the dialog when the World object
 * changes. This behavior can be modified by subclassing the
 * <code>worldChanged()</code> method.</li>
 * </ol>
 * <p>
 * To add a default button, use JRootPane.setDefaultButton(); note that if a
 * text panel/field is present then this will not work. If a read-only (non-editable)
 * text component is present, it can be sub-classed to avoid receiving any keyboard
 * input (override isFocusable()). See TextViewer for an example.
 */
public class XDialog extends JDialog {
    // common dialog constants
    /**
     * Internationalized button text for "OK"
     */
    public static final String TEXT_OK = Utils
            .getLocalString("XDialog.button.ok");
    /**
     * Internationalized button text for "Cancel"
     */
    public static final String TEXT_CANCEL = Utils
            .getLocalString("XDialog.button.cancel");
    /**
     * Internationalized button text for "Close"
     */
    public static final String TEXT_CLOSE = Utils
            .getLocalString("XDialog.button.close");
    /**
     * Internationalized button text for "Accept"
     */
    public static final String TEXT_ACCEPT = Utils
            .getLocalString("XDialog.button.accept");


    // instance vars
    private AbstractCFPListener cfl = null;


    /**
     * Create an XDialog
     */
    public XDialog() {
        super();
    }// XDialog()

    /**
     * Create an XDialog
     */
    public XDialog(final Frame owner) {
        super(owner);
    }// UniverseJDialog()

    /**
     * Create an XDialog
     */
    public XDialog(final Frame owner, final String title) {
        super(owner, title);
    }// XDialog()

    /**
     * Create an XDialog
     */
    public XDialog(final Frame owner, final boolean modal) {
        super(owner, modal);
    }// XDialog()

    /**
     * Create an XDialog
     */
    public XDialog(final Frame owner, final String title, final boolean modal) {
        super(owner, title, modal);
    }// XDialog()

    /**
     * Create an XDialog
     */
    public XDialog(final Dialog owner) {
        super(owner);
    }// UniverseJDialog()

    /**
     * Create an XDialog
     */
    public XDialog(final Dialog owner, final String title) {
        super(owner, title);
    }// XDialog()

    /**
     * Create an XDialog
     */
    public XDialog(final Dialog owner, final boolean modal) {
        super(owner, modal);
    }// XDialog()

    /**
     * Create an XDialog
     */
    public XDialog(final Dialog owner, final String title, final boolean modal) {
        super(owner, title, modal);
    }// XDialog()


    /**
     * Called when closing. By default, calls dispose().
     */
    protected void close() {
        dispose();
    }// close()


    /**
     * Dialog setup, including adding Window-Close listener
     */
    @Override
    protected void dialogInit() {
        super.dialogInit();

        // install world-change listener (if appropriate)
        setupCFL();

        // install close/close-button handling
        super.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                close();
            }
        });
    }// dialogInit()


    /**
     * Adds the ESC key listener
     */
    @Override
    protected JRootPane createRootPane() {
        final JRootPane rootPane = super.createRootPane();

        // install ESC key checking.
        final ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                close();
            }
        };

        final KeyStroke strokeESC = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

        rootPane.registerKeyboardAction(actionListener, strokeESC,
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        return rootPane;
    }// createRootPane()


    /**
     * Throws an IllegalArgumentException()
     */
    @Override
    public void setDefaultCloseOperation(final int operation) {
        throw new IllegalArgumentException("override close() instead");
    }// setDefaultCloseOperation()


    /**
     * Set the HelpID (see dip.misc.Help).  If non-null, sets the Window-Level
     * help for this dialog.
     */
    public void setHelpID(final Help.HelpID helpID) {
        Help.enableDialogHelp(this, helpID);
    }// setHelpID()


    @Override
    public void dispose() {
        if (cfl != null) {
            ((JComponent) getParent()).removePropertyChangeListener(cfl);
            cfl = null;
        }

        super.dispose();
    }// dispose()


    /**
     * For non-modal dialogs, that are an instance of ClientFrame, add
     * a ClientFrame property listener to catch world created/destroyed
     * events. Our listener will call worldChanged().
     */
    private void setupCFL() // CFL = client frame listener
    {
        if (getParent() instanceof ClientFrame && !isModal()) {
            final ClientFrame cf = (ClientFrame) getParent();
            cf.addPropertyChangeListener(new AbstractCFPListener() {
                @Override
                public void actionWorldCreated(final World w) {
                    worldChanged();
                }

                @Override
                public void actionWorldDestroyed(final World w) {
                    worldChanged();
                }
            });
        }
    }// setupCFL()


    /**
     * Called for non-modal dialogs with a ClientFrame parent, when a
     * World object has been deleted or created. By default, this calls
     * close().
     */
    protected void worldChanged() {
        close();
    }// worldChanged()

}// class XDialog

