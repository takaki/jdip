//
//  @(#)ClientFrame.java	4/2002
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

import dip.gui.dialog.AboutDialog;
import dip.gui.dialog.ErrorDialog;
import dip.gui.dialog.FileDropTargetListener;
import dip.gui.dialog.MapPicker;
import dip.gui.dialog.MetadataDialog;
import dip.gui.dialog.MultiOrderEntry;
import dip.gui.dialog.SelectPhaseDialog;
import dip.gui.dialog.ValidationOptionsDialog;
import dip.gui.dialog.newgame.NewGameDialog;
import dip.gui.dialog.prefs.DisplayPreferencePanel;
import dip.gui.dialog.prefs.GeneralPreferencePanel;
import dip.gui.dialog.prefs.PreferenceDialog;
import dip.gui.map.MapMetadata;
import dip.gui.map.MapPanel;
import dip.gui.map.MapRenderer2;
import dip.gui.map.RenderCommandFactory.RenderCommand;
import dip.gui.order.GUIOrderFactory;
import dip.gui.report.OrderStatsWriter;
import dip.gui.report.ResultWriter;
import dip.gui.report.SCHistoryWriter;
import dip.gui.report.StateWriter;
import dip.gui.report.VariantInfoWriter;
import dip.gui.swing.XJFileChooser;
import dip.gui.undo.UndoRedoManager;
import dip.gui.undo.UndoResolve;
import dip.misc.Help;
import dip.misc.Help.HelpID;
import dip.misc.Log;
import dip.misc.Utils;
import dip.order.OrderFormatOptions;
import dip.order.Orderable;
import dip.order.ValidationOptions;
import dip.process.StdAdjudicator;
import dip.tool.Tool;
import dip.tool.ToolManager;
import dip.tool.ToolProxyImpl;
import dip.world.Phase;
import dip.world.Power;
import dip.world.TurnState;
import dip.world.World;
import dip.world.variant.VariantManager;
import jcmdline.BooleanParam;
import jcmdline.CmdLineHandler;
import jcmdline.FileParam;
import jcmdline.HelpCmdLineHandler;
import jcmdline.Parameter;
import jcmdline.StringParam;
import jcmdline.VersionCmdLineHandler;
import org.apache.batik.util.XMLResourceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.XMLReader;

import javax.swing.*;
import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Locale;

/**
 * The main class for starting the client... everything starts here.
 * <p>
 * Note that this is not required for non-gui functionality; see dip.misc.TestSuite
 * for an example of using the adjudicator classes without the GUI.
 * <p>
 * Currently, there is no "server".
 * <p>
 */
public class ClientFrame extends JFrame {
    private static final Logger LOG = LoggerFactory
            .getLogger(ClientFrame.class);

    // public property constants for PropertyChange events
    /**
     * Event indicating that a World object was created
     */
    public static final String EVT_WORLD_CREATED = "EVT_WORLD_CREATED";
    /**
     * Event indicating the World was destroyed (!)
     */
    public static final String EVT_WORLD_DESTROYED = "EVT_WORLD_DESTROYED";
    /**
     * Event indicating the <b>current</b> TurnState has changed.
     */
    public static final String EVT_TURNSTATE_CHANGED = "EVT_TURNSTATE_CHANGED";
    /**
     * Event indicating that the saved game state doesn't jive with the current state
     */
    public static final String EVT_MODIFIED_STATE = "EVT_MODIFIED_STATE";
    /**
     * Event indicating a TurnState was added
     */
    public static final String EVT_TURNSTATE_ADDED = "EVT_TURNSTATE_ADDED";
    /**
     * Event indicating a TurnState was removed
     */
    public static final String EVT_TURNSTATE_REMOVED = "EVT_TURNSTATE_REMOVED";
    /**
     * Event indicating a TurnState was resolved
     */
    public static final String EVT_TURNSTATE_RESOLVED = "EVT_TURNSTATE_RESOLVED";
    /**
     * Event indicating a mode change
     */
    public static final String EVT_MODE_CHANGED = "EVT_MODE_CHANGED";
    /**
     * Event indicating that order validation options have changed
     */
    public static final String EVT_VALOPTS_CHANGED = "EVT_VALOPTS_CHANGED";
    /**
     * Event indicating that MapMetadata object is ready
     */
    public static final String EVT_MMD_READY = "EVT_MMD_READY";

    // order events
    /**
     * Event indicating an order was created
     */
    public static final String EVT_ORDER_CREATED = "EVT_ORDER_CREATED";
    /**
     * Event indicating an order was deleted
     */
    public static final String EVT_ORDER_DELETED = "EVT_ORDER_DELETED";
    /**
     * Event indicating multiple orders were created
     */
    public static final String EVT_MULTIPLE_ORDERS_CREATED = "EVT_MULTIPLE_ORDERS_CREATED";
    /**
     * Event indicating multiple orders were deleted (cleared)
     */
    public static final String EVT_MULTIPLE_ORDERS_DELETED = "EVT_MULTIPLE_ORDERS_DELETED";

    // power events
    /**
     * Event indicating the Powers that may be displayed have changed.
     */
    public static final String EVT_DISPLAYABLE_POWERS_CHANGED = "EVT_DISPLAYABLE_POWERS_CHANGED";
    /**
     * Event indicating the Powers for which orders may be entered have changed.
     */
    public static final String EVT_ORDERABLE_POWERS_CHANGED = "EVT_ORDERABLE_POWERS_CHANGED";


    // modes [for EVT_MODE_CHANGED]
    /**
     * Order Mode: orders may be entered in this mode.
     */
    public static final String MODE_ORDER = "MODE_ORDER";
    /**
     * Edit Mode: units/ownership/etc. may be changed in this mode.
     */
    public static final String MODE_EDIT = "MODE_EDIT";
    /**
     * Review Mode: Orders from previous turns (or if game has ended) may be reviewed in this mode.
     */
    public static final String MODE_REVIEW = "MODE_REVIEW";
    /**
     * "None" Mode: no World object is active
     */
    public static final String MODE_NONE = "MODE_NONE";

    // private constants
    private static final String PROGRAM_NAME = Utils
            .getLocalString("PROGRAM_NAME");
    private static final int VERSION_MAJOR = 1;
    private static final int VERSION_MINOR = 7;
    private static final String KEY_VERSION_REVISION = "VERSION_REVISION";
    private static final String KEY_CURRENT_LANGUAGE = "CURRENT_LANGUAGE";


    // plugin directories
    private static final String VARIANT_DIR = "variants";
    private static final String TOOL_DIR = "plugins";
    private File variantDirPath = null;

    // instance variables
    private final JSplitPane splitPane;
    private MapPanel mapPanel = null;
    private ClientMenu clientMenu = null;
    private final PersistenceManager persistMan;
    private OrderDisplayPanel orderDisplayPanel = null;
    private OrderStatusPanel orderStatusPanel = null;
    private boolean isMMDSuppressed = false;
    private final PhaseSelector phaseSel;
    private StatusBar statusBar = null;

    private World world = null;                // loaded World (null if none)
    private TurnState turnState = null;        // current TurnState of above World

    private String currentMode = MODE_NONE;
    private GUIOrderFactory guiOrderFactory = null;            // default GUI order factory.
    private boolean isValidating = false;    // extra data validation when parsing?
    private boolean applyGUIEnhancements = true;
    private ValidationOptions valOpts = new ValidationOptions();
    private UndoRedoManager undoManager = null;
    private OrderFormatOptions orderFormatOptions = null;
    private MapMetadata mapMetadata = null;

    // power control instance variables
    private Power[] orderablePowers = new Power[0];        // powers for which orders may be entered
    private Power[] displayablePowers = new Power[0];    // powers for which orders may be displayed

    // for testing
    private final Object fireLock = new Object();


    /**
     * It all starts here ....
     */
    public static void main(final String[] args) {
        new ClientFrame(args);
    }// main()


    /**
     * Create a ClientFrame, the main screen for the GUI Client.
     */
    public ClientFrame(final String[] args) {
        super();
        final long ttime = System.currentTimeMillis();        // total time
        long dtime = ttime;                                // delta time

        // parse command-line args
        parseCmdLine(args);
        LOG.debug(Log.printDelta(dtime, "CF: arg parse time: "));
        dtime = System.currentTimeMillis();

        LOG.debug("   mem max: {}", Runtime.getRuntime().maxMemory());
        LOG.debug("   mem total: {}", Runtime.getRuntime().totalMemory());
        LOG.debug("   mem free: {}", Runtime.getRuntime().freeMemory());


        // set Batik XMLReader based on JAXP XMLReader.
        // this should work for JDK 1.5, 1.4, etc.
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            final XMLReader xmlReader = factory.newSAXParser().getXMLReader();
            XMLResourceDescriptor
                    .setXMLParserClassName(xmlReader.getClass().getName());
            LOG.debug("Batik XMLReader: {}",
                    XMLResourceDescriptor.getXMLParserClassName());
        } catch (final Exception e) {
            ErrorDialog.displayFatal(this, e);
        }


        LOG.debug("Applying GUI enhancements: {}", applyGUIEnhancements);

        if (applyGUIEnhancements) {
            // setup per-OS options
            if (Utils.isOSX()) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty(
                        "com.apple.mrj.application.apple.menu.about.name",
                        PROGRAM_NAME);
                System.setProperty("apple.awt.showGrowBox",
                        "true");    // may no longer need
                /*
                NOTE: brushed metal is disabled; a bug in the Cocoa implementation causes
				dialogs to behave strangely when this is enabled.
				//
				//System.setProperty("apple.awt.brushMetalLook", "true");
				*/
            }

            // replace bad-looking (metal, motif) LAFs with better-looking
            // ones.
            String lafClassName = UIManager.getSystemLookAndFeelClassName();
            assert lafClassName != null;

            if (Utils.isWindows()) {
                // higher-fidelity windows LAF
                lafClassName = "com.jgoodies.looks.windows.WindowsLookAndFeel";

                // enable this to use the Java (not windows system) default font.
                // UIManager.put("Application.useSystemFontSettings", Boolean.FALSE);
            } else if (!Utils.isOSX()) {
                // keep synth; switch if Motif / Metal
                if (lafClassName
                        .indexOf("MotifLookAndFeel") >= 0 || lafClassName
                        .indexOf("MetalLookAndFeel") >= 0) {
                    // good generic LAF
                    lafClassName = "com.jgoodies.looks.plastic.PlasticLookAndFeel";
                }
            }


            try {
                if (lafClassName.indexOf("jgoodies") >= 0) {
                    // for WebStart compatibility
                    UIManager.put("ClassLoader",
                            com.jgoodies.looks.LookUtils.class
                                    .getClassLoader());
                }
                LOG.debug(lafClassName);
                UIManager.setLookAndFeel(lafClassName);
            } catch (final Exception e) {
                // do nothing; swing will load default L&F
                LOG.debug(e.toString());
            }
        }

        LOG.debug(Log.printDelta(dtime, "CF: LAF setup time: "));
        dtime = System.currentTimeMillis();

        // set exception handler
        GUIExceptionHandler.registerHandler();


        // get the variant and tool directories.
        // do not change the variantDirPath if it was set
        // from the command line
        // use the preferred path, if set, and not overridden from command line
        variantDirPath = variantDirPath == null ? GeneralPreferencePanel
                .getVariantDir() : variantDirPath;
        File toolDirPath;
        if (System.getProperty("user.dir") == null) {
            variantDirPath = variantDirPath == null ? new File(".",
                    VARIANT_DIR) : variantDirPath;
            toolDirPath = new File(".", TOOL_DIR);
        } else {
            variantDirPath = variantDirPath == null ? new File(
                    System.getProperty("user.dir"),
                    VARIANT_DIR) : variantDirPath;
            toolDirPath = new File(System.getProperty("user.dir"), TOOL_DIR);
        }

        LOG.debug("Using variant directory: {}", variantDirPath);

        // parse variants
        initVariantManager();

        LOG.debug(Log.printDelta(dtime, "CF: variant setup time: "));
        dtime = System.currentTimeMillis();

        // init Tools
        ToolManager.init(new File[]{toolDirPath});
        final Tool[] tools = ToolManager.getTools();
        final ToolProxyImpl toolProxy = new ToolProxyImpl(this);
        for (Tool tool : tools) {
            tool.setToolProxy(toolProxy);
        }
        LOG.debug(Log.printDelta(dtime, "CF: tool setup time: "));
        dtime = System.currentTimeMillis();


        // set frame icon
        setIconImage(Utils.getImageIcon(Utils.FRAME_ICON).getImage());

        // init help system
        Help.init();
        LOG.debug(Log.printDelta(dtime, "CF: help init time: "));
        dtime = System.currentTimeMillis();

        // setup menu
        clientMenu = new ClientMenu(this);
        setJMenuBar(clientMenu.getJMenuBar());
        LOG.debug(Log.printDelta(dtime, "CF: menu setup time: "));
        dtime = System.currentTimeMillis();

        // init special filedialog class
        //
        // NOTE: JDK bug (?) can cause rare error at startup, due to Swing not being able
        // to get the dialog icons it needs. We delay init of this class as long as possible,
        // to see if that helps.
        XJFileChooser.init();

        // Cached dialogs [these dialogs appear slowly if not cached]
        NewGameDialog.createCachedDialog(this);

        AboutDialog.createCachedDialog(this);

        // persistence (must come after menus are defined)
        persistMan = new PersistenceManager(this);
        LOG.debug(Log.printDelta(dtime, "CF: PersistenceManager setup time: "));
        dtime = System.currentTimeMillis();

        // frame listener, handles JFrame close events
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                persistMan.exit();
            }
        });

        // default GUIOrderFactory
        // [in the future, variants may alter this]
        guiOrderFactory = new GUIOrderFactory();

        // setup drag-and-drop support
        new DropTarget(this, new CFDropTargetListener());
        LOG.debug(Log.printDelta(dtime, "CF: point A: "));
        dtime = System.currentTimeMillis();

        // create default split pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
        splitPane.setOneTouchExpandable(true);
        splitPane.setVisible(false);
        splitPane.setDividerSize(10);
        splitPane.setResizeWeight(1);
        LOG.debug(Log.printDelta(dtime, "CF: point B: "));
        dtime = System.currentTimeMillis();

        // create statusbar
        statusBar = new StatusBar();
        statusBar.setText(ClientFrame.PROGRAM_NAME + " " + getVersion());
        LOG.debug(Log.printDelta(dtime, "CF: point c: "));
        dtime = System.currentTimeMillis();

        // PhaseSelector
        phaseSel = new PhaseSelector(this);
        LOG.debug(Log.printDelta(dtime, "CF: point d: "));
        dtime = System.currentTimeMillis();

        // add mode listener for this object
        addPropertyChangeListener(new ModeListener());

        // set initial mode
        fireChangeMode(MODE_NONE);
        LOG.debug(Log.printDelta(dtime, "CF: point e: "));
        dtime = System.currentTimeMillis();
        // register menu listeners
        final MenuHandler mh = new MenuHandler();
        mh.registerMenuItems();

        // get default order formatting options
        orderFormatOptions = DisplayPreferencePanel.getOrderFormatOptions();
        LOG.debug(Log.printDelta(dtime, "CF: point f: "));
        dtime = System.currentTimeMillis();

        // setup layout
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        pack();
        GeneralPreferencePanel.getWindowSettings(this);
        setVisible(true);
        fireChangeMode(MODE_NONE);
        toFront();
        splash.destroy();
        LOG.debug(Log.printDelta(dtime, "CF: frame setup time: "));

        LOG.debug(Log.printTimed(ttime, "ClientFrame() startup time: "));
    }// ClientFrame()


    /**
     * Get the Menu component
     */
    public ClientMenu getClientMenu() {
        return clientMenu;
    }// getClientMenu()

    /**
     * Get the PersistenceManager component
     */
    public PersistenceManager getPM() {
        return persistMan;
    }// getPM()

    /**
     * Get the PhaseSelector component
     */
    public PhaseSelector getPhaseSelector() {
        return phaseSel;
    }// getPhaseSelector()

    /**
     * Get the OrderDisplayPanel component
     */
    public synchronized OrderDisplayPanel getOrderDisplayPanel() {
        return orderDisplayPanel;
    }// getOrderDisplayPanel()

    /**
     * Get the OrderStatusPanel component
     */
    public synchronized OrderStatusPanel getOrderStatusPanel() {
        return orderStatusPanel;
    }// getOrderStatusPanel()

    /**
     * Get the MapPanel component
     */
    public synchronized MapPanel getMapPanel() {
        return mapPanel;
    }// getMapPanel()

    /**
     * Get the StatusBar component
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }// getStatusBar()

    /**
     * Get the UndoRedoManager component
     */
    public synchronized UndoRedoManager getUndoRedoManager() {
        return undoManager;
    }// getUndoRedoManager()

    /**
     * Get the current order validation options settings.
     */
    public synchronized ValidationOptions getValidationOptions() {
        return valOpts;
    }

    /**
     * Set if we are suppressing MapMetadata placement errors.
     * This should only be set by Map Editors.
     */
    public void setMMDSuppressed(final boolean value) {
        isMMDSuppressed = value;
    }// setMMDSuppressed()

    /**
     * Returns if MapMetadata placement errors are suppressed.
     */
    public boolean isMMDSuppressed() {
        return isMMDSuppressed;
    }// isMMDSuppressed()


    /**
     * Get the user-specified Order Format Options (OFO)
     */
    public synchronized OrderFormatOptions getOFO() {
        return orderFormatOptions;
    }// getOFO()

    /**
     * Set the user-specified Order Format Options (OFO)
     */
    public synchronized void setOFO(final OrderFormatOptions value) {
        orderFormatOptions = value;
    }// setOFO()

    /**
     * Get MapMetadata (note: may be null)
     */
    public synchronized MapMetadata getMapMetadata() {
        return mapMetadata;
    }// getMapMetadata()


    /**
     * Returns the Powers for which orders may be entered.
     * If this value is changed, a EVT_ORDERABLE_POWERS_CHANGED
     * property event will be fired.
     * <p>
     * This is only applicable for phases in which orders may
     * be entered.
     */
    public Power[] getOrderablePowers() {
        synchronized (this) {
            return orderablePowers;
        }
    }// getOrderablePowers()


    /**
     * Returns the Powers which for which orders may be displayed.
     * If this value is changed, a EVT_DISPLAYABLE_POWERS_CHANGED
     * property event will be fired.
     * <p>
     * This is only applicable for phases in which orders may
     * be entered. For example, in Review mode, all orders
     * should be displayed.
     */
    public Power[] getDisplayablePowers() {
        synchronized (this) {
            return displayablePowers;
        }
    }// getDisplayablePowers()


    /**
     * Returns true if we should validate all data files. This
     * may or may not be applicable, depending upon the data file
     * format.
     */
    public boolean getValidating() {
        return isValidating;
    }// getValidating()

    /**
     * Returns the default OrderFactory for generating
     * new Orders. By default, GUIOrders are used, so
     * the default class should be a GUIOrderFactory or
     * derivative.
     */
    public GUIOrderFactory getGUIOrderFactory() {
        return guiOrderFactory;
    }// getGUIOrderFactory()


    /**
     * Set the OrderDisplayPanel
     */
    synchronized void setOrderDisplayPanel(final OrderDisplayPanel odp) {
        orderDisplayPanel = odp;
    }// setOrderDisplayPanel()


    /**
     * Set the OrderStatusPanel
     */
    synchronized void setOrderStatusPanel(final OrderStatusPanel osp) {
        orderStatusPanel = osp;
    }// setOrderStatusPanel()


    /**
     * Set the UndoRedoManager
     */
    synchronized void setUndoRedoManager(final UndoRedoManager urm) {
        undoManager = urm;
    }// setUndoRedoManager()

    /**
     * Set the MapPanel
     */
    synchronized void setMapPanel(final MapPanel mp) {
        mapPanel = mp;
    }// setMapPanel()


    /**
     * Get the JSplitPane component
     */
    public JSplitPane getJSplitPane() {
        return splitPane;
    }// getJSplitPane()


    /**
     * Returns the current World, or null if no World is loaded.
     */
    public synchronized World getWorld() {
        return world;
    }// getWorld()


    /**
     * Sets the current World. Uses the GameSetup
     * object to perform additional, as-needed,
     * game setup. A Null argument is not permitted.
     */
    public synchronized void createWorld(final World w) {
        // safety check
        if (w == null) {
            throw new IllegalArgumentException();
        }

        // disable menu-input
        // remove old GUI components
        // and cleanup GUI / etc components
        destroyWorld();

        // set the world
        world = w;

        // setup the world. NOTE: GUIGameSetup objects absolutely
        // must fire a WorldCreated event and a TurnstateChanged
        // event.
        final GUIGameSetup ggs = (GUIGameSetup) world.getGameSetup();
        ggs.setup(this, world);

        // validate something here
        validate();
    }// createWorld()


    /**
     * Destroy the World (! use with caution).
     */
    public synchronized void destroyWorld() {
        fireChangeMode(MODE_NONE);
        clientMenu.setEnabled(ClientMenu.EDIT_EDIT_MODE, false);

        // NOTE: splitPane.removeAll() has seriously bad (and weird)
        // side effects when used (especially if components are null).
        // we will use this more laborious but more deterministic
        // code. (if not used, the right-most panel will take up
        // all the space, and the splitpane divider is not drawn)
        //
        Component c = splitPane.getLeftComponent();
        if (c != null) {
            splitPane.remove(c);
        }

        c = splitPane.getRightComponent();
        if (c != null) {
            splitPane.remove(c);
        }

        splitPane.setVisible(false);

        // cleanup
        if (orderStatusPanel != null) {
            orderStatusPanel.close();
            orderStatusPanel = null;
        }

        if (orderDisplayPanel != null) {
            orderDisplayPanel.close();
            orderDisplayPanel = null;
        }

        if (mapPanel != null) {
            mapPanel.close();
            mapPanel = null;
        }

        undoManager = null;
        world = null;
        turnState = null;

        // inform everybody
        fireWorldDestroyed();

        validate();
    }// destroyWorld()


    /**
     * Fired when the World object was created.
     * This is typically only sent by the GUIGameSetup object.
     */
    protected void fireWorldCreated(final World w) {
        checkNotNull(w);
        synchronized (fireLock) {
            firePropertyChange(EVT_WORLD_CREATED, null, w);
        }
    }// fireWorldCreated()


    /**
     * Fired when the World object was destroyed
     */
    private void fireWorldDestroyed() {
        synchronized (fireLock) {
            firePropertyChange(EVT_WORLD_DESTROYED, getWorld(), null);
        }
    }// fireWorldDestroyed()


    /**
     * Indicates to listeners what the current TurnState is.
     * It is upto the Listener (such as OrderDisplayPanel and MapPanel) to
     * actually update the GUI.
     * <p>
     * <b>Note: The OLD event value (TurnState) will always be null for this
     * event; otherwise 'update' events will not fire.</b>
     * <p>
     * null is not acceptable.
     */
    public void fireTurnstateChanged(final TurnState ts) {
        checkNotNull(ts);
        synchronized (fireLock) {
            firePropertyChange(EVT_TURNSTATE_CHANGED, null, ts);
        }
    }// fireTurnstateChanged()


    /**
     * Indicates that the given TurnState has been Resolved
     * (adjudicated). May not be null.
     * <p>
     * <b>Note: The OLD event value (TurnState) will always be null for this
     * event; otherwise 'update' events will not fire.</b>
     */
    public void fireTurnstateResolved(final TurnState ts) {
        checkNotNull(ts);
        synchronized (fireLock) {
            firePropertyChange(EVT_TURNSTATE_RESOLVED, null, ts);
        }
    }// fireTurnstateResolved()


    /**
     * Fired when the MapMetadata object is ready, or if
     * it is not ready (null), such as when a map is reloaded.
     */
    public void fireMMDReady(final MapMetadata mmd) {
        firePropertyChange(EVT_MMD_READY, null, mmd);
    }// fireTurnstateChanged()


    /**
     * Returns the current TurnState. <p>
     * Returns null if no World (or current TurnState) exists.
     */
    public synchronized TurnState getTurnState() {
        return turnState;
    }// getTurnState()


    /**
     * Fired when an order was created
     */
    public final void fireOrderCreated(final Orderable newOrder) {
        synchronized (fireLock) {
            checkNotNull(newOrder);
            firePropertyChange(EVT_ORDER_CREATED, null, newOrder);
        }
    }// fireOrderCreated()

    /**
     * Fired when an order was deleted
     */
    public final void fireOrderDeleted(final Orderable deletedOrder) {
        synchronized (fireLock) {
            checkNotNull(deletedOrder);
            firePropertyChange(EVT_ORDER_DELETED, deletedOrder, null);
        }
    }// fireOrderDeleted()

    /**
     * Fired when multiple orders were created
     */
    public final void fireMultipleOrdersCreated(
            final Orderable[] createdOrders) {
        synchronized (fireLock) {
            checkNotNull(createdOrders);
            firePropertyChange(EVT_MULTIPLE_ORDERS_CREATED, null,
                    createdOrders);
        }
    }// fireMultipleOrdersCreated()

    /**
     * Fired when multiple orders were deleted
     */
    public final void fireMultipleOrdersDeleted(
            final Orderable[] deletedOrders) {
        synchronized (fireLock) {
            checkNotNull(deletedOrders);
            firePropertyChange(EVT_MULTIPLE_ORDERS_DELETED, deletedOrders,
                    null);
        }
    }// fireMultipleOrdersDeleted()

    /**
     * Fired when displayed orders have changed
     */
    public final void fireDisplayablePowersChanged(final Power[] oldPowers,
                                                   final Power[] newPowers) {
        synchronized (fireLock) {
            checkNotNull(newPowers);
            checkNotNull(oldPowers);
            firePropertyChange(EVT_DISPLAYABLE_POWERS_CHANGED, oldPowers,
                    newPowers);
        }
    }// fireDisplayablePowersChanged()

    /**
     * Fired when Powers for which orders may be entered have changed
     */
    public final void fireOrderablePowersChanged(final Power[] oldPowers,
                                                 final Power[] newPowers) {
        synchronized (fireLock) {
            checkNotNull(newPowers);
            checkNotNull(oldPowers);
            firePropertyChange(EVT_ORDERABLE_POWERS_CHANGED, oldPowers,
                    newPowers);
        }
    }// fireOrderablePowersChanged()

    /**
     * Fired if we have modified the World in such a way it
     * is no longer reflected in its saved state.
     */
    public final void fireStateModified() {
        synchronized (fireLock) {
            firePropertyChange(EVT_MODIFIED_STATE, false, true);
        }
    }// fireStateModified()

    /**
     * Fired if we have added a turnstate
     */
    public final void fireTurnStateAdded(final TurnState newTS) {
        synchronized (fireLock) {
            checkNotNull(newTS);
            firePropertyChange(EVT_TURNSTATE_ADDED, null, newTS);
        }
    }// fireTurnStateAdded()

    /**
     * Fired if we have removed a turnstate
     */
    public final void fireTurnStateRemoved() {
        synchronized (fireLock) {
            firePropertyChange(EVT_TURNSTATE_REMOVED, null, null);
        }
    }// fireTurnStateRemoved()

    /**
     * Fired if we change the order Validation Options. New/Old options sent.
     */
    public final void fireValidationOptionsChanged(
            final ValidationOptions oldOpts, final ValidationOptions newOpts) {
        checkNotNull(oldOpts);
        checkNotNull(newOpts);
        firePropertyChange(EVT_VALOPTS_CHANGED, oldOpts, newOpts);
    }// fireValidationOptionsChanged()


    /**
     * Change the operating mode for this ClientFrame.<br>
     */
    public final synchronized void fireChangeMode(final String newMode) {
        if (newMode != MODE_ORDER && newMode != MODE_REVIEW && newMode != MODE_EDIT && newMode != MODE_NONE) {
            throw new IllegalArgumentException("bad mode constant");
        }

        currentMode = newMode;
        firePropertyChange(EVT_MODE_CHANGED, null, newMode);
    }// fireChangeMode()


    /**
     * Prints the currently registered listeners to stdout. For debugging only.
     */
    public void dbgPrintListeners() {
        final PropertyChangeListener[] pcls = getPropertyChangeListeners();
        System.out.println("ClientFrame listeners: " + pcls.length);
        for (PropertyChangeListener pcl : pcls) {
            System.out.println("     " + pcl.getClass().getName());
        }
    }// dbgPrintListeners()


    /**
     * Returns the Client version; format: <br>
     * major.minor.revision (language)
     */
    public static String getVersion() {
        final String revision = Utils.getLocalStringNoEx(KEY_VERSION_REVISION);
        final String language = Utils.getLocalStringNoEx(KEY_CURRENT_LANGUAGE);

        return String.format("%s.%d%s%s", String.valueOf(VERSION_MAJOR),
                VERSION_MINOR, revision != null ? '.' + revision : "",
                language != null ? " (" + language + ")" : "");
    }// getVersion()


    /**
     * Returns the Client major version
     */
    public static int getVersionMajor() {
        return VERSION_MAJOR;
    }

    /**
     * Returns the Client minor version
     */
    public static int getVersionMinor() {
        return VERSION_MINOR;
    }

    /**
     * Returns the Client program name.
     */
    public static String getProgramName() {
        return PROGRAM_NAME;
    }


    /**
     * DropTarget listener that allows ClientFrame to respond to
     * drag events.
     */
    private class CFDropTargetListener extends FileDropTargetListener {
        @Override
        public void processDroppedFiles(final File[] files) {
            for (File file : files) {
                if (files.length >= 0) {
                    final World world = persistMan
                            .acceptDrag(files[0], getWorld());
                    if (world != null) {
                        world.setGameSetup(new DefaultGUIGameSetup());
                        createWorld(world);
                    }
                    persistMan.updateTitle();
                }
            }
        }// processDroppedFiles()
    }// inner class CFDropTargetListener


    /**
     * Gets the current mode. Use a fireChangeMode() to set the current mode.
     */
    public synchronized String getMode() {
        // return a new reference, so that noone can change the mode
        // without using fireChangeMode()
        final String mode = currentMode;
        return mode;
    }// getMode()


    /**
     * Property Listener for listening to Settings Changes
     */
    private class ModeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            final String evtName = evt.getPropertyName();

            if (evtName == EVT_MODE_CHANGED) {
                final String newMode = (String) evt.getNewValue();

                if (newMode == MODE_NONE) {
                    statusBar.clearModeText();
                } else if (newMode == MODE_REVIEW) {
                    statusBar.setModeText(
                            Utils.getLocalString("ClientFrame.mode.review"));

                    if (getTurnState().isEnded()) {
                        statusBar.setModeText(
                                Utils.getLocalString("ClientFrame.mode.ended"));
                    }
                } else if (newMode == MODE_EDIT) {
                    statusBar.setModeText(
                            Utils.getLocalString("ClientFrame.mode.edit"));
                } else if (newMode == MODE_ORDER) {
                    statusBar.setModeText(
                            Utils.getLocalString("ClientFrame.mode.order"));
                } else {
                    throw new IllegalStateException("invalid mode: " + newMode);
                }
            } else if (evtName == EVT_TURNSTATE_CHANGED) {
                synchronized (ClientFrame.this) {
                    turnState = (TurnState) evt.getNewValue();

                    if (turnState.isEnded() || turnState.isResolved()) {
                        fireChangeMode(MODE_REVIEW);
                    } else {
                        fireChangeMode(MODE_ORDER);
                    }
                }
            } else if (evtName == EVT_DISPLAYABLE_POWERS_CHANGED) {
                synchronized (ClientFrame.this) {
                    displayablePowers = (Power[]) evt.getNewValue();
                }
            } else if (evtName == EVT_ORDERABLE_POWERS_CHANGED) {
                synchronized (ClientFrame.this) {
                    orderablePowers = (Power[]) evt.getNewValue();
                }
            } else if (evtName == EVT_MMD_READY) {
                synchronized (ClientFrame.this) {
                    mapMetadata = (MapMetadata) evt.getNewValue();
                }
            }
        }
    }// class ModeListener


    /**
     * Setup command-line options and parse the command line.
     */
    private void parseCmdLine(final String[] args) {
        // parameterized options
        final StringParam argLocale = new StringParam("lang",
                "force language to the specified ISO-639 2-letter type (e.g., \"de\", \"en\", \"fr\")",
                2, 2, true, false);

        final FileParam argLogFile = new FileParam("log",
                "writes logging information to file or stdout [if \"stdout\" specified]",
                FileParam.NO_ATTRIBUTES, StringParam.OPTIONAL,
                FileParam.SINGLE_VALUED);

        final FileParam argVariantPath = new FileParam("variantpath",
                "load variant plugins from specified directory",
                FileParam.IS_DIR & FileParam.IS_READABLE & FileParam.EXISTS,
                FileParam.OPTIONAL, FileParam.SINGLE_VALUED);

        // boolean options
        final BooleanParam validateOpt = new BooleanParam("validate",
                "validate XML and SVG data files");

        final BooleanParam splashOpt = new BooleanParam("nosplash",
                "do not show splash screen");

        // validate option
        final BooleanParam defaultGUI = new BooleanParam("defaultgui",
                "do not apply GUI enhancements");

        // verbose help text
        final String helpText = " ";

        // main command line handler
        final CmdLineHandler cl = new VersionCmdLineHandler(getVersion(),
                new HelpCmdLineHandler(helpText, "jdip",
                        "Adjudicator and Game Manager for multiplayer diplomacy-based strategy games",
                        // options
                        new Parameter[]{argLocale, argLogFile, argVariantPath, validateOpt, splashOpt, defaultGUI},
                        // arguments [left on command line]
                        new Parameter[]{}));

        // parse command line
        cl.parse(args);

        // if Locale has been set, use it.
        if (argLocale.isSet()) {
            final Locale locale = new Locale(
                    argLocale.getValue().toLowerCase());
            System.out.println(
                    "Using Language: " + locale.getLanguage() + " [" + locale
                            .getDisplayLanguage() + "]");
            Utils.loadLocale(locale);
        }

        // set variant path if given
        if (argVariantPath.isSet()) {
            variantDirPath = argVariantPath.getFile();
        }

        // do logging

        // set flags
        isValidating = validateOpt.isTrue();
        applyGUIEnhancements = !defaultGUI.isTrue();
    }// parseCmdLine()


    /**
     * Check if an argument is null; throw IllegalArgumentException if so
     */
    private void checkNotNull(final Object arg) {
        if (arg == null) {
            throw new IllegalArgumentException("null argument!");
        }
    }// checkNotNull()


    /**
     * Resolve orders. Requires an OrderDisplayPanel to be present.
     */
    public void resolveOrders() {
        if (orderDisplayPanel != null) {
            final TurnState resolvedTurnState = getTurnState();
            final StdAdjudicator stdJudge = new StdAdjudicator(
                    getGUIOrderFactory(), resolvedTurnState);
            stdJudge.setStatReporting(true);        // report order statistics
            stdJudge.setPowerOrderChecking(true);    // check for cheats & bugs
            stdJudge.process();
            fireStateModified();

            // this may be null, if the game has been won
            final TurnState newTurnState = stdJudge.getNextTurnState();

            if (newTurnState != null) {
                world.setTurnState(newTurnState);
                fireTurnStateAdded(newTurnState);
            }

            // create Undo result
            undoManager.addEdit(
                    new UndoResolve(undoManager, getTurnState(), newTurnState));

            // simplify undoable actions
            undoManager.simplify();

            // see if game has ended; if so, show a dialog & change mode
            if (getTurnState().isEnded() || newTurnState != null && newTurnState
                    .isEnded()) {
                Utils.popupInfo(ClientFrame.this,
                        Utils.getLocalString("ClientFrame.ended.dialog.title"),
                        Utils.getText(Utils.getLocalString(
                                "ClientFrame.ended.dialog.text")));

                fireChangeMode(MODE_REVIEW);
            }

            // show results (if desired)
            if (GeneralPreferencePanel.getShowResolutionResults()) {
                final TurnState priorTS = getWorld()
                        .getPreviousTurnState(newTurnState).get();
                ResultWriter.displayDialog(ClientFrame.this, priorTS, getOFO());
            }

            fireTurnstateResolved(resolvedTurnState);

            if (newTurnState != null) {
                fireTurnstateChanged(newTurnState);
            }
        }
    }// resolveOrders()


    /**
     * Private class to handle menu events,
     * without exposing public methods to other classes.
     * Furthermore, we can directly call no-arg methods in
     * final helper classes (PhaseSelector, PersistanceManager).
     */
    private class MenuHandler {
        // inner state
        private String oldEditMode = null;

        /**
         * Register the menu items
         */
        public void registerMenuItems() {
            // file
            clientMenu.setActionMethod(ClientMenu.FILE_NEW_STD, this,
                    "onFileNewStd");

            clientMenu.setActionMethod(ClientMenu.FILE_NEW_F2F, this,
                    "onFileNewF2F");
            clientMenu
                    .setActionMethod(ClientMenu.FILE_OPEN, this, "onFileOpen");
            clientMenu
                    .setActionMethod(ClientMenu.FILE_SAVE, persistMan, "save");
            clientMenu.setActionMethod(ClientMenu.FILE_SAVEAS, persistMan,
                    "saveAs");
            clientMenu.setActionMethod(ClientMenu.FILE_SAVETO, persistMan,
                    "saveTo");
            clientMenu.setActionMethod(ClientMenu.FILE_IMPORT_FILE, this,
                    "onFileImport");
            clientMenu.setActionMethod(ClientMenu.FILE_IMPORT_FLOC, this,
                    "onFileImportFloc");
            clientMenu
                    .setActionMethod(ClientMenu.FILE_EXIT, persistMan, "exit");

            // edit
            clientMenu
                    .setActionMethod(ClientMenu.EDIT_UNDO, this, "onEditUndo");
            clientMenu
                    .setActionMethod(ClientMenu.EDIT_REDO, this, "onEditRedo");
            clientMenu.setActionMethod(ClientMenu.EDIT_SELECT_ALL, this,
                    "onEditSelectAll");
            clientMenu.setActionMethod(ClientMenu.EDIT_SELECT_NONE, this,
                    "onEditSelectNone");
            clientMenu.setActionMethod(ClientMenu.EDIT_DELETE, this,
                    "onEditDelete");
            clientMenu.setActionMethod(ClientMenu.EDIT_CLEAR_ALL, this,
                    "onEditClearAll");
            clientMenu.setActionMethod(ClientMenu.EDIT_EDIT_MODE, this,
                    "onEditEditMode");
            clientMenu.setActionMethod(ClientMenu.EDIT_METADATA, this,
                    "onEditMetadata");
            clientMenu.setActionMethod(ClientMenu.EDIT_PREFERENCES, this,
                    "onEditPreferences");

            // orders
            clientMenu.setActionMethod(ClientMenu.ORDERS_VAL_OPTIONS, this,
                    "onOrdersValOpts");
            clientMenu.setActionMethod(ClientMenu.ORDERS_REVALIDATE, this,
                    "onOrdersRevalidate");
            clientMenu.setActionMethod(ClientMenu.ORDERS_MULTI_INPUT, this,
                    "onOrdersMultiInput");
            clientMenu.setActionMethod(ClientMenu.ORDERS_RESOLVE, this,
                    "onOrdersResolve");

            // history
            clientMenu.setActionMethod(ClientMenu.HISTORY_PREVIOUS, phaseSel,
                    "previous");
            clientMenu
                    .setActionMethod(ClientMenu.HISTORY_NEXT, phaseSel, "next");
            clientMenu.setActionMethod(ClientMenu.HISTORY_INITIAL, phaseSel,
                    "first");
            clientMenu
                    .setActionMethod(ClientMenu.HISTORY_LAST, phaseSel, "last");
            clientMenu.setActionMethod(ClientMenu.HISTORY_SELECT, this,
                    "onHistorySelect");

            // view
            clientMenu.setActionMethod(ClientMenu.VIEW_NAMES_NONE, this,
                    "onViewNamesNone");
            clientMenu.setActionMethod(ClientMenu.VIEW_NAMES_SHORT, this,
                    "onViewNamesShort");
            clientMenu.setActionMethod(ClientMenu.VIEW_NAMES_FULL, this,
                    "onViewNamesFull");
            clientMenu.setActionMethod(ClientMenu.VIEW_SUPPLY_CENTERS, this,
                    "onViewSC");
            clientMenu.setActionMethod(ClientMenu.VIEW_UNITS, this,
                    "onViewUnits");
            clientMenu.setActionMethod(ClientMenu.VIEW_DISLODGED_UNITS, this,
                    "onViewDislodged");
            clientMenu.setActionMethod(ClientMenu.VIEW_UNORDERED, this,
                    "onViewUnordered");
            clientMenu.setActionMethod(ClientMenu.VIEW_INFLUENCE, this,
                    "onViewInfluence");
            clientMenu.setActionMethod(ClientMenu.VIEW_SELECT_MAP, this,
                    "onViewSelectMap");
            clientMenu.setActionMethod(ClientMenu.VIEW_SHOW_MAP, this,
                    "onViewShowMap");


            // reports
            clientMenu.setActionMethod(ClientMenu.REPORTS_RESULTS, this,
                    "onReportsResults");
            clientMenu
                    .setActionMethod(ClientMenu.REPORTS_PREVIOUS_RESULTS, this,
                            "onReportsPreviousResults");
            clientMenu.setActionMethod(ClientMenu.REPORTS_STATUS, this,
                    "onReportsStatus");
            clientMenu.setActionMethod(ClientMenu.REPORTS_SC_HISTORY, this,
                    "onReportsSCHistory");
            clientMenu.setActionMethod(ClientMenu.REPORTS_ORDER_STATS, this,
                    "onReportsOrderStats");
            clientMenu.setActionMethod(ClientMenu.REPORTS_MAP_INFO, this,
                    "onReportsMapInfo");

            // help
            clientMenu.setActionMethod(ClientMenu.HELP_ABOUT, this,
                    "onHelpAbout");
            Help.enableHelpOnButton(
                    clientMenu.getMenuItem(ClientMenu.HELP_CONTENTS),
                    HelpID.Contents);
        }// registerMenuItems()

        // file
        //
        public void onFileNewStd() {
            final World world = persistMan.newGame();
            if (world != null) {
                createWorld(world);
                persistMan.updateTitle();
            }
        }// onFileNewStd()

        public void onFileNewF2F() {
            final World world = persistMan.newF2FGame();
            if (world != null) {
                createWorld(world);
                persistMan.updateTitle();
            }
        }// onFileNewStd()

        public void onFileOpen() {
            final World world = persistMan.open();
            if (world != null) {
                createWorld(world);
            }
        }

        public void onFileImport() {
            final World world = persistMan.importJudge(getWorld());
            if (world != null) {
                world.setGameSetup(new DefaultGUIGameSetup());
                createWorld(world);
            }
            persistMan.updateTitle();
        }

        public void onFileImportFloc() {
            final World world = persistMan.importFloc();
            if (world != null) {
                world.setGameSetup(new DefaultGUIGameSetup());
                createWorld(world);
                persistMan.updateTitle();
            }
        }


        // edit
        //
        public void onEditUndo() {
            if (orderDisplayPanel != null) {
                undoManager.undo();
            }
        }

        public void onEditRedo() {
            if (orderDisplayPanel != null) {
                undoManager.redo();
            }
        }

        public void onEditSelectAll() {
            if (orderDisplayPanel != null) {
                orderDisplayPanel.selectAll();
            }
        }

        public void onEditSelectNone() {
            if (orderDisplayPanel != null) {
                orderDisplayPanel.selectNone();
            }
        }

        public void onEditDelete() {
            if (orderDisplayPanel != null) {
                orderDisplayPanel.removeSelected();
            }
        }

        public void onEditClearAll() {
            if (orderDisplayPanel != null) {
                orderDisplayPanel.removeAllOrders(true);
            }
        }

        public void onEditEditMode() {
            if (clientMenu.getSelected(ClientMenu.EDIT_EDIT_MODE)) {
                oldEditMode = getMode();
                fireChangeMode(MODE_EDIT);
            } else {
                // check and see if any powers were eliminated after edit
                getTurnState().getPosition()
                        .setEliminationStatus(world.getMap().getPowers());
                fireChangeMode(oldEditMode);
            }
        }

        public void onEditMetadata() {
            if (orderDisplayPanel != null) {
                MetadataDialog.displayDialog(ClientFrame.this);
            }
        }

        public void onEditPreferences() {
            PreferenceDialog.displayDialog(ClientFrame.this);
        }

        // orders
        //
        public void onOrdersValOpts() {
            if (getOrderDisplayPanel() != null) {
                final ValidationOptions newOpts = ValidationOptionsDialog
                        .displayDialog(ClientFrame.this, valOpts);
                fireValidationOptionsChanged(valOpts, newOpts);
                valOpts = newOpts;
            }
        }

        public void onOrdersRevalidate() {
            if (getOrderDisplayPanel() != null) {
                getOrderDisplayPanel().revalidateAllOrders();
            }
        }

        public void onOrdersMultiInput() {
            if (getWorld() != null) {
                MultiOrderEntry.displayDialog(ClientFrame.this, getWorld());
            }
        }

        public void onOrdersResolve() {
            resolveOrders();
        }// onOrdersResolve()

        // history
        //
        public void onHistorySelect() {
            if (orderDisplayPanel != null) {
                final Phase phase = SelectPhaseDialog
                        .displayDialog(ClientFrame.this);
                if (phase != null) {
                    fireTurnstateChanged(world.getTurnState(phase));
                }
            }
        }

        // view
        //
        public void onViewNamesNone() {
            if (mapPanel != null) {
                final RenderCommand rc = mapPanel.getRenderCommandFactory()
                        .createRCSetLabel(mapPanel.getMapRenderer(),
                                MapRenderer2.VALUE_LABELS_NONE);
                execRenderCommand(rc);
            }
        }

        public void onViewNamesShort() {
            if (mapPanel != null) {
                final RenderCommand rc = mapPanel.getRenderCommandFactory()
                        .createRCSetLabel(mapPanel.getMapRenderer(),
                                MapRenderer2.VALUE_LABELS_BRIEF);
                execRenderCommand(rc);
            }
        }

        public void onViewNamesFull() {
            if (mapPanel != null) {
                final RenderCommand rc = mapPanel.getRenderCommandFactory()
                        .createRCSetLabel(mapPanel.getMapRenderer(),
                                MapRenderer2.VALUE_LABELS_FULL);
                execRenderCommand(rc);
            }
        }

        public void onViewSC() {
            if (mapPanel != null) {
                final boolean value = clientMenu
                        .getSelected(ClientMenu.VIEW_SUPPLY_CENTERS);
                final RenderCommand rc = mapPanel.getRenderCommandFactory()
                        .createRCSetDisplaySC(mapPanel.getMapRenderer(), value);
                execRenderCommand(rc);
            }
        }

        public void onViewUnits() {
            if (mapPanel != null) {
                final boolean value = clientMenu
                        .getSelected(ClientMenu.VIEW_UNITS);
                final RenderCommand rc = mapPanel.getRenderCommandFactory()
                        .createRCSetDisplayUnits(mapPanel.getMapRenderer(),
                                value);
                execRenderCommand(rc);
            }
        }

        public void onViewDislodged() {
            if (mapPanel != null) {
                final boolean value = clientMenu
                        .getSelected(ClientMenu.VIEW_DISLODGED_UNITS);
                final RenderCommand rc = mapPanel.getRenderCommandFactory()
                        .createRCSetDisplayDislodgedUnits(
                                mapPanel.getMapRenderer(), value);
                execRenderCommand(rc);
            }
        }

        // VIEW_ORDERS is handled internally by ClientMenu

        public void onViewUnordered() {
            if (mapPanel != null) {
                final boolean value = clientMenu
                        .getSelected(ClientMenu.VIEW_UNORDERED);
                final RenderCommand rc = mapPanel.getRenderCommandFactory()
                        .createRCSetDisplayUnordered(mapPanel.getMapRenderer(),
                                value);
                execRenderCommand(rc);
            }
        }

        public void onViewInfluence() {
            if (mapPanel != null) {
                final boolean value = clientMenu
                        .getSelected(ClientMenu.VIEW_INFLUENCE);
                final RenderCommand rc = mapPanel.getRenderCommandFactory()
                        .createRCSetInfluenceMode(mapPanel.getMapRenderer(),
                                value);
                execRenderCommand(rc);
            }
        }

        public void onViewSelectMap() {
            if (world != null) {
                MapPicker.displayDialog(ClientFrame.this, world);
            }
        }

        public void onViewShowMap() {
            if (mapPanel != null) {
                final boolean value = clientMenu
                        .getSelected(ClientMenu.VIEW_SHOW_MAP);
                final RenderCommand rc = mapPanel.getRenderCommandFactory()
                        .createRCShowMap(mapPanel.getMapRenderer(), value);
                execRenderCommand(rc);
            }
        }


        // reports
        //
        public void onReportsResults() {
            ResultWriter
                    .displayDialog(ClientFrame.this, getTurnState(), getOFO());
        }

        public void onReportsPreviousResults() {
            // getPreviousTurnState() should not return null, if this item is enabled.
            if (mapPanel != null) {
                final TurnState ts = getTurnState();
                ResultWriter.displayDialog(ClientFrame.this,
                        world.getPreviousTurnState(ts).get(), getOFO());
            }
        }

        public void onReportsSCHistory() {
            SCHistoryWriter.displayDialog(ClientFrame.this, getWorld());
        }

        public void onReportsStatus() {
            StateWriter.displayDialog(ClientFrame.this, getTurnState());
        }


        public void onReportsOrderStats() {
            OrderStatsWriter
                    .displayDialog(ClientFrame.this, getWorld(), getOFO());
        }

        public void onReportsMapInfo() {
            if (getWorld() != null) {
                VariantInfoWriter.displayDialog(ClientFrame.this, getWorld());
            }
        }

        // help
        //
        public void onHelpAbout() {
            AboutDialog.displayDialog(ClientFrame.this);
        }


        /**
         * Helper method for View methods
         */
        private void execRenderCommand(final RenderCommand rc) {
            final MapRenderer2 mr2 = mapPanel.getMapRenderer();
            mr2.execRenderCommand(rc);
        }// execRenderCommand

    }// inner class MenuHandler()


    /**
     * Handle variant parsing.... and initialize the variant manager
     */
    private void initVariantManager() {
//        try {
        new VariantManager(); // FIXME
//        } catch (dip.world.variant.NoVariantsException e) {
//            // display informative message, as a popup
//            Utils.popupError(null, Utils.getLocalString(
//                    "ClientFrame.error.novariants.dialog.title"), Utils.getText(
//                    Utils.getLocalString(
//                            "ClientFrame.error.novariants.dialog.text.location")));
//
//            // give the user a chance to set the variant dir path
//            final File file = GeneralPreferencePanel.setVariantDir(null, true);
//            if (file == null) {
//                ErrorDialog.displayFatal(this, e);
//            } else {
//                variantDirPath = file;
//                initVariantManager();
//            }
//        }
    }// initVariantManager()

}// class ClientFrame

