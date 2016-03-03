//
//  @(#)DefaultMapRenderer2.java	5/2003
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
package dip.gui.map;

import dip.gui.ClientMenu;
import dip.gui.map.RenderCommandFactory.RenderCommand;
import dip.gui.order.GUIOrder;
import dip.gui.order.GUIOrder.MapInfo;
import dip.misc.Log;
import dip.order.Orderable;
import dip.world.Coast;
import dip.world.Location;
import dip.world.Phase;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.TurnState;
import dip.world.Unit;
import dip.world.WorldMap;
import dip.world.variant.data.SymbolPack;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.util.RunnableQueue;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Default Rendering logic.
 * <p>
 * <p>
 * <b>Debugging Hint:</b> To see if the DOM has been altered, it can be 'dumped' by
 * using File | Export Map As... | SVG and looking at the SVG output.
 */
public class DefaultMapRenderer2 extends MapRenderer2 {
    // Symbol Names
    //
    /**
     * Army symbol ID
     */
    public static final String SYMBOL_ARMY = "Army";
    /**
     * Fleet symbol ID
     */
    public static final String SYMBOL_FLEET = "Fleet";
    /**
     * Dislodged Army symbol ID
     */
    public static final String SYMBOL_DISLODGED_ARMY = "DislodgedArmy";
    /**
     * Dislodged Fleet symbol ID
     */
    public static final String SYMBOL_DISLODGED_FLEET = "DislodgedFleet";
    /**
     * Supply Center symbol ID
     */
    public static final String SYMBOL_SC = "SupplyCenter";
    /**
     * Wing symbol ID
     */
    public static final String SYMBOL_WING = "Wing";
    /**
     * Dislodged Wing symbol ID
     */
    public static final String SYMBOL_DISLODGED_WING = "DislodgedWing";
    /**
     * Failed Order symbol ID
     */
    public static final String SYMBOL_FAILEDORDER = "FailedOrder";
    /**
     * Build marker symbol ID
     */
    public static final String SYMBOL_BUILDUNIT = "BuildUnit";
    /**
     * Remove (and disband) symbol ID
     */
    public static final String SYMBOL_REMOVEUNIT = "RemoveUnit";
    /**
     * Waived Build symbol ID
     */
    public static final String SYMBOL_WAIVEDBUILD = "WaivedBuild";

    /**
     * Symbol List
     */
    public static final String[] SYMBOLS = {SYMBOL_ARMY, SYMBOL_FLEET, SYMBOL_DISLODGED_ARMY, SYMBOL_DISLODGED_FLEET, SYMBOL_WING, SYMBOL_DISLODGED_WING, SYMBOL_SC, SYMBOL_FAILEDORDER, SYMBOL_BUILDUNIT, SYMBOL_REMOVEUNIT, SYMBOL_WAIVEDBUILD};


    /**
     * Layer: Map
     */
    public static final String LAYER_MAP = "MapLayer";
    /**
     * Layer: Supply Center
     */
    protected static final String LAYER_SC = "SupplyCenterLayer";
    /**
     * Layer: Orders
     */
    protected static final String LAYER_ORDERS = "OrderLayer";
    /**
     * Layer: Highest (z=0) Orders
     */
    protected static final String HIGHEST_ORDER_LAYER = "HighestOrderLayer";
    /**
     * Layer: Units
     */
    protected static final String LAYER_UNITS = "UnitLayer";
    /**
     * Layer: Dislodged Units
     */
    protected static final String LAYER_DISLODGED_UNITS = "DislodgedUnitLayer";
    /**
     * Layer: region definitions for mouse
     */
    protected static final String LAYER_MOUSE = "MouseLayer";
    /**
     * Label Layer: Abbreviated
     */
    public static final String LABEL_LAYER_BRIEF = "BriefLabelLayer";
    /**
     * Label Layer: Full
     */
    public static final String LABEL_LAYER_FULL = "FullLabelLayer";

    /**
     * All Label layers
     */
    protected static final String[] LABEL_LAYERS = {LABEL_LAYER_BRIEF, LABEL_LAYER_FULL};

    /**
     * All Layers
     */
    protected static final String[] LAYERS = {LAYER_MAP, LAYER_SC, LAYER_ORDERS, HIGHEST_ORDER_LAYER, LAYER_UNITS, LAYER_DISLODGED_UNITS, LABEL_LAYER_BRIEF, LABEL_LAYER_FULL, LAYER_MOUSE};


    // layers for Z-ordering
    private static final String LAYER_1 = "Layer1";
    private static final String LAYER_2 = "Layer2";
    private static final String[] Z_LAYER_NAMES = {HIGHEST_ORDER_LAYER, LAYER_1, LAYER_2};


    protected static final String NOPOWER = "nopower";
    protected static final String SC_NOPOWER = "scnopower";
    protected static final String UNORDERED = "unordered";

    // instance variables
    protected final Map<Province, Tracker> trackerMap;            // for rendering units & dislodged units; keyed by Province
    protected final HashMap<String, SVGGElement> layerMap;        // layers to which we render; keyed by LAYER; includes label layers
    private final HashMap<Object, Object> renderSettings;    // control rendering options.
    private final HashMap<String, Location> locMap;            // maps multicoastal province ids -> Location objects for multicoastal provinces
    private final HashMap[] powerOrderMap;
    private HashMap oldRenderSettings;        // old render settings

    private final WorldMap worldMap;    // World Map reference

    private TurnState turnState = null;                    // current TurnState
    private final Province[] provinces;
    private final Power[] powers;
    private Position position = null;                    // current Position
    private MapMetadata mapMeta = null;
    private DOMUIEventListener domEventListener = null;
    private boolean isDislodgedPhase = false;            // true if we are in Phase.RETREAT
    private static final DMR2RenderCommandFactory rcf;    // default render command factory instance.
    private final SymbolPack symbolPack;

    static {
        rcf = new DMR2RenderCommandFactory();
    }

    /**
     * Creates a DefaultMapRenderer object
     */
    public DefaultMapRenderer2(final MapPanel mp, final SymbolPack sp) throws MapException {
        super(mp);
        this.symbolPack = sp;
        Log.printTimed(mapPanel.startTime, "DMR2 constructor start");

        // init variables
        worldMap = mapPanel.getClientFrame().getWorld().getMap();
        provinces = worldMap.getProvinces().toArray(new Province[0]);
        powers = mapPanel.getClientFrame().getWorld().getMap().getPowers()
                .toArray(new Power[0]);

        // setup object maps
        trackerMap = new HashMap<>(113);
        renderSettings = new HashMap<>(11);
        layerMap = new HashMap<>(11);
        locMap = new HashMap<>(17);

        // power order hashmap (now with z-axis) setup
        powerOrderMap = new HashMap[Z_LAYER_NAMES.length];
        for (int i = 0; i < powerOrderMap.length; i++) {
            powerOrderMap[i] = new HashMap<>(11);
        }

        // set default render settings
        renderSettings.put(KEY_SHOW_MAP, Boolean.TRUE);
        renderSettings.put(KEY_SHOW_SUPPLY_CENTERS, Boolean.TRUE);
        renderSettings.put(KEY_SHOW_UNITS, Boolean.TRUE);
        renderSettings.put(KEY_SHOW_DISLODGED_UNITS, Boolean.TRUE);
        renderSettings.put(KEY_SHOW_ORDERS_FOR_POWERS, powers);
        renderSettings.put(KEY_SHOW_UNORDERED, Boolean.FALSE);
        renderSettings.put(KEY_INFLUENCE_MODE, Boolean.FALSE);
        renderSettings.put(KEY_LABELS, VALUE_LABELS_NONE);

        // get map metadata
        mapMeta = new MapMetadata(mapPanel, sp,
                mapPanel.getClientFrame().isMMDSuppressed());

        // tell others that mmd has been parsed and is ready
        mapPanel.getClientFrame().fireMMDReady(mapMeta);

        // get and check symbols & rendering layers
        checkSymbols();
        mapLayers();

        // add mouse listeners to the MouseLayer
        // add key listeners
        domEventListener = mapPanel.getDOMUIEventListener();
        domEventListener.setMapRenderer(this);
        validateAndSetupMouseRegions();

        // Root SVG element listeners
        // one is for general key events, and the other, for 'null' locations
        // (some maps may have 'null' space).
        doc.getRootElement()
                .addEventListener(SVGConstants.SVG_KEYPRESS_EVENT_TYPE,
                        domEventListener, false);    // note: false
        doc.getRootElement().addEventListener(SVGConstants.SVG_EVENT_CLICK,
                domEventListener, true);
        doc.getRootElement().addEventListener(SVGConstants.SVG_EVENT_MOUSEOUT,
                domEventListener, true);
        doc.getRootElement().addEventListener(SVGConstants.SVG_EVENT_MOUSEOVER,
                domEventListener, true);

        // Dragging stuff
        doc.getRootElement()
                .addEventListener(SVGConstants.SVG_MOUSEDOWN_EVENT_TYPE,
                        domEventListener, true);
        doc.getRootElement()
                .addEventListener(SVGConstants.SVG_MOUSEUP_EVENT_TYPE,
                        domEventListener, true);

        // create a complete set of Tracker objects for all provinces
        for (Province province : provinces) {
            trackerMap.put(province, new Tracker());
        }

        // add province hilites to Tracker object
        addProvinceHilitesToTracker();

        // create the per-power order layers
        createDOMOrderTree();

        // add all SC to the map. The SC are never removed or added, however,
        // their attributes (attribute class) may change.
        // after adding to DOM, the element is added to the Tracker object.
        final RunnableQueue rq = getRunnableQueue();
        if (rq != null) {
            rq.invokeLater(new Runnable() {
                public void run() {
                    synchronized (trackerMap) {
                        for (Province province : provinces) {
                            if (province.hasSupplyCenter()) {
                                // create element
                                final SVGElement element = makeSCUse(province,
                                        null);

                                // add element to tracker
                                final Tracker tracker = (Tracker) trackerMap
                                        .get(province);
                                tracker.setSCElement(element);

                                // add to DOM
                                final SVGElement parent = (SVGElement) layerMap
                                        .get(LAYER_SC);
                                parent.appendChild(element);
                            }
                        }
                    }
                }// run()
            });
        }

        Log.printTimed(mapPanel.startTime, "DMR2 constructor end");
    }// DefaultMapRenderer()


    /**
     * Returns a DMR2RenderCommandFactory
     */
    public RenderCommandFactory getRenderCommandFactory() {
        return rcf;
    }// getRenderCommandFactory()


    /**
     * Close the MapRenderer, releasing all resources.
     * <p>
     * WARNING: render events must not be processed after or
     * during a call to this method.
     */
    public void close() {
        // super cleanup
        super.close();

        // remove Root SVG element listeners
        doc.getRootElement()
                .removeEventListener(SVGConstants.SVG_KEYPRESS_EVENT_TYPE,
                        domEventListener, false);

        // Remove other mouse/key listeners
        final SVGElement[] mouseElements = SVGUtils
                .idFinderSVG(layerMap.get(LAYER_MOUSE));
        for (SVGElement mouseElement : mouseElements) {
            if (mouseElement instanceof EventTarget) {
                // add mouse listeners
                final EventTarget et = (EventTarget) mouseElement;
                et.removeEventListener(SVGConstants.SVG_EVENT_CLICK,
                        domEventListener, false);
                et.removeEventListener(SVGConstants.SVG_EVENT_MOUSEOUT,
                        domEventListener, false);
                et.removeEventListener(SVGConstants.SVG_EVENT_MOUSEOVER,
                        domEventListener, false);
            }
        }

        // clear maps
        synchronized (trackerMap) {
            trackerMap.clear();
        }

        layerMap.clear();
        renderSettings.clear();
        locMap.clear();

        // clear metadata
        mapMeta.close();
    }// close()


    /**
     * Gets the MapMetadata object
     */
    public MapMetadata getMapMetadata() {
        return mapMeta;
    }// getMapMetadata()

    /**
     * Get a mapped layer
     */
    public SVGGElement getLayer(final String key) {
        return layerMap.get(key);
    }// getLayer()

    /**
     * Called when an order has been added to the order list
     */
    protected void orderCreated(final GUIOrder order) {
        execRenderCommand(new RenderCommand(this) {
            public void execute() {
                Log.println("DMR2: orderCreated(): ", order);

                final MapInfo mapInfo = new DMRMapInfo(turnState);
                order.updateDOM(mapInfo);

                unsyncUpdateDependentOrders(new GUIOrder[]{order});
                unsyncUpdateProvince(order.getSource().getProvince());
            }// execute()
        });
    }// orderAdded()


    /**
     * Called when an order has been deleted from the order list
     */
    protected void orderDeleted(final GUIOrder order) {
        execRenderCommand(new RenderCommand(this) {
            public void execute() {
                Log.println("DMR2: orderDeleted(): ", order);

                final MapInfo mapInfo = new DMRMapInfo(turnState);
                order.removeFromDOM(mapInfo);

                unsyncUpdateDependentOrders(null);
                unsyncUpdateProvince(order.getSource().getProvince());
            }// execute()
        });
    }// orderDeleted()

    /**
     * Called when multiple orders have been added from the order list
     */
    protected void multipleOrdersCreated(final GUIOrder[] orders) {
        execRenderCommand(new RenderCommand(this) {
            public void execute() {
                Log.println("DMR2: multipleOrdersCreated(): ", orders);
                final MapInfo mapInfo = new DMRMapInfo(turnState);

                // render orders and update provinces
                for (GUIOrder order : orders) {
                    order.updateDOM(mapInfo);
                    unsyncUpdateProvince(order.getSource().getProvince());
                }

                // update dependent orders
                unsyncUpdateDependentOrders(orders);
            }// execute()
        });
    }// multipleOrdersCreated()

    /**
     * Called when multiple orders have been deleted from the order list
     */
    protected void multipleOrdersDeleted(final GUIOrder[] orders) {
        execRenderCommand(new RenderCommand(this) {
            public void execute() {
                Log.println("DMR2: multipleOrdersDeleted(): ", orders);
                final MapInfo mapInfo = new DMRMapInfo(turnState);

                // render orders and update provinces
                for (GUIOrder order : orders) {
                    order.removeFromDOM(mapInfo);
                    unsyncUpdateProvince(order.getSource().getProvince());
                }

                // update dependent orders
                unsyncUpdateDependentOrders(null);
            }// execute()
        });
    }// multipleOrdersDeleted()


    /**
     * Called when the displayable powers have changed
     */
    protected void displayablePowersChanged(final Power[] diplayPowers) {
        execRenderCommand(new RenderCommand(this) {
            public void execute() {
                Log.println("DMR2: displayablePowersChanged()");

                // update all orders
                unsyncUpdateAllOrders();
            }// execute()
        });
    }// displayablePowersChanged()


    /**
     * Sets the current TurnState object for the renderer. This should
     * only operate within a run() method... if the turnState is changed
     * while another run() method is activated, bad things can happen.
     */
    protected void setTurnState(final TurnState ts) {
        if (ts == null || ts.getPosition() == null) {
            throw new IllegalArgumentException("null turnstate or position");
        }

        // destroy all orders in old turnstate
        // before changing
        if (turnState != null) {
            unsyncDestroyAllOrders();
        }

        // change turnstate.
        turnState = ts;
        position = ts.getPosition();
        isDislodgedPhase = (ts.getPhase()
                .getPhaseType() == Phase.PhaseType.RETREAT);
    }// setTurnState()


    /**
     * Get a map rendering setting
     */
    public Object getRenderSetting(final Object key) {
        synchronized (renderSettings) {
            return renderSettings.get(key);
        }
    }// getRenderSetting()


    /**
     * Internally set a Render Setting
     */
    protected void setRenderSetting(final Object key, final Object value) {
        synchronized (renderSettings) {
            renderSettings.put(key, value);
        }
    }// setRenderSetting()


    /**
     * Get the Symbol Name for the given unit type
     */
    public String getSymbolName(final Unit.Type unitType) {
        if (unitType == Unit.Type.ARMY) {
            return DefaultMapRenderer2.SYMBOL_ARMY;
        } else if (unitType == Unit.Type.FLEET) {
            return DefaultMapRenderer2.SYMBOL_FLEET;
        } else if (unitType == Unit.Type.WING) {
            return DefaultMapRenderer2.SYMBOL_WING;
        } else {
            throw new IllegalStateException(
                    "DMR2: Unit Type: " + unitType + " SVG symbol ID unknown");
        }
    }// getSymbolName()


    /**
     * Gets the location that corresponds to a given string id<br>Assumes ID is lowercase!
     */
    public Location getLocation(final String id) {
        final Province province = worldMap.getProvince(id);
        if (province != null) {
            return new Location(province, Coast.UNDEFINED);
        }

        return locMap.get(id);
    }// getLocation()


    /**
     * Creates SVG G elements (one for each power) under the OrderLayer
     * SVG G element layer. Maps each Power to the SVGGElement that
     * corresponds, in powerOrderMap.
     */
    private void createDOMOrderTree() {
        final RunnableQueue rq = getRunnableQueue();
        if (rq != null) {
            rq.invokeLater(new Runnable() {
                public void run() {
                    SVGGElement orderLayer = layerMap
                            .get(LAYER_ORDERS);

                    for (int z = (powerOrderMap.length - 1); z >= 0; z--) {
                        // determine which order layer we should use.
                        if (z == 0) {
                            // special case: this has its own explicit group in the SVG file
                            orderLayer = layerMap
                                    .get(HIGHEST_ORDER_LAYER);
                        } else {
                            // typical case
                            // these occur under the "OrderLayer" group
                            // Note that we must create the elements in reverse order, because
                            // lower z-orders (closer to viewer) must be rendered after (later)
                            // higher z orders
                            //

                            final SVGGElement parentLayer = layerMap
                                    .get(LAYER_ORDERS);

                            // create order layer under ORDER_LAYERS layer (e.g., id="Layer1", or id="Layer2")
                            orderLayer = (SVGGElement) doc.createElementNS(
                                    SVGDOMImplementation.SVG_NAMESPACE_URI,
                                    SVGConstants.SVG_G_TAG);
                            orderLayer.setAttributeNS(null,
                                    SVGConstants.SVG_ID_ATTRIBUTE,
                                    Z_LAYER_NAMES[z]);
                            parentLayer.appendChild(orderLayer);

                            // now put this into the layer map, so it can be retrieved later
                            layerMap.put(Z_LAYER_NAMES[z], orderLayer);
                        }

                        // create an order layer for each power. append the z order ID
                        for (Power power : powers) {
                            final SVGGElement gElement = (SVGGElement) doc
                                    .createElementNS(
                                            SVGDOMImplementation.SVG_NAMESPACE_URI,
                                            SVGConstants.SVG_G_TAG);
                            // make layer name (needs to be unique)
                            final StringBuffer sb = new StringBuffer(32);
                            sb.append(getPowerName(power));
                            sb.append('_');
                            sb.append(String.valueOf(z));

                            gElement.setAttributeNS(null,
                                    SVGConstants.SVG_ID_ATTRIBUTE,
                                    sb.toString());
                            orderLayer.appendChild(gElement);
                            powerOrderMap[z].put(power, gElement);
                        }
                    }
                }
            });
        }
    }// createDOMOrderTree()


    /**
     * Makes sure that all orders have been removed from the order
     * tree and have no associated SVGElement.
     */
    protected void unsyncDestroyAllOrders() {
        Log.println("DMR2::unsyncDestroyAllOrders()");
        final MapInfo mapInfo = new DMRMapInfo(turnState);
        final Iterator iter = turnState.getAllOrders().iterator();
        while (iter.hasNext()) {
            final GUIOrder order = (GUIOrder) iter.next();
            order.removeFromDOM(mapInfo);
        }
    }// unsyncDestroyAllOrders()


    /**
     * Get the Power order SVGGElement (group) for
     * then given z-order. z : [0,2]
     */
    private SVGGElement getPowerSVGGElement(final Power p, final int z) {
        return (SVGGElement) powerOrderMap[z].get(p);
    }// getPowerSVGGElement()


    /**
     * Sets the Visibility (CSS visibility) for a Power's orders.
     * This manipulates all layers for a given power.
     */
    private void setPowerOrderVisibility(final Power p, final boolean isVisible) {
        for (int i = 0; i < powerOrderMap.length; i++) {
            setElementVisibility(getPowerSVGGElement(p, i), isVisible);
        }
    }// setPowerOrderVisibility()

    /**
     * Uses the KEY_SHOW_ORDERS_FOR_POWERS value to
     * determine which orders should be displayed.
     */
    protected void unsyncSetVisiblePowers() {
        final Power[] displayedPowers = (Power[]) getRenderSetting(
                KEY_SHOW_ORDERS_FOR_POWERS);

        // displayedPowers contains the powers that are visible.
        // go thru all powers, setting the visibility
        for (Power power : powers) {
            boolean isVisible = false;
            for (Power displayedPower : displayedPowers) {
                if (power == displayedPower) {
                    isVisible = true;
                    break;
                }
            }

            // set visibility
            setPowerOrderVisibility(power, isVisible);
        }
    }// unsyncSetVisiblePowers()


    /**
     * Removes ALL orders, for all powers, in all layers.
     * Power layer G parent elements remain unaltered.
     */
    protected void unsyncRemoveAllOrdersFromDOM() {
        for (int z = 0; z < powerOrderMap.length; z++) {
            for (Power power : powers) {
                final SVGGElement powerNode = getPowerSVGGElement(power, z);

                Node child = powerNode.getFirstChild();
                while (child != null) {
                    powerNode.removeChild(child);
                    child = powerNode.getFirstChild();
                }
            }
        }
    }// unsyncRemoveAllOrdersFromDOM()


    /**
     * Implements Influence mode. Saves and restores old mapRenderer settings.
     */
    protected void unsyncSetInfluenceMode(final boolean value) {
        //Log.println("unsyncSetInfluenceMode(): ", String.valueOf(value));

        // disable or enable certain View menu items
        final ClientMenu cm = mapPanel.getClientFrame().getClientMenu();

        // save and set, or restore, previous MapRenderer2
        if (value) {
            // ENTERING influence mode
            //
            if (oldRenderSettings != null) {
                throw new IllegalStateException("already in influence mode!");
            }

            // disable menu items early
            cm.setEnabled(ClientMenu.VIEW_ORDERS, !value);
            cm.setEnabled(ClientMenu.VIEW_UNITS, !value);
            cm.setEnabled(ClientMenu.VIEW_DISLODGED_UNITS, !value);
            cm.setEnabled(ClientMenu.VIEW_SUPPLY_CENTERS, !value);
            cm.setEnabled(ClientMenu.VIEW_UNORDERED, !value);
            cm.setEnabled(ClientMenu.VIEW_SHOW_MAP, !value);


            // now clear the render settings
            synchronized (renderSettings) {
                // copy old render settings
                oldRenderSettings = (HashMap) renderSettings.clone();

                // if 'show unordered' was enabled, we must first disable it.
                if (oldRenderSettings
                        .get(MapRenderer2.KEY_SHOW_UNORDERED) == Boolean.TRUE) {
                    renderSettings.put(MapRenderer2.KEY_SHOW_UNORDERED,
                            Boolean.FALSE);
                }

                renderSettings.clear();
                renderSettings.put(MapRenderer2.KEY_SHOW_ORDERS_FOR_POWERS,
                        new Power[0]);
            }

            // hide layers we don't want (units, orders, sc)
            SVGElement elLayer = layerMap.get(LAYER_SC);
            setElementVisibility(elLayer, false);

            elLayer = layerMap.get(LAYER_UNITS);
            setElementVisibility(elLayer, false);

            elLayer = layerMap.get(LAYER_DISLODGED_UNITS);
            setElementVisibility(elLayer, false);

            elLayer = layerMap
                    .get(LAYER_MAP);    // always show map in influence mode
            setElementVisibility(elLayer, true);

            final Power[] visiblePowers = (Power[]) oldRenderSettings
                    .get(MapRenderer2.KEY_SHOW_ORDERS_FOR_POWERS);
            for (Power visiblePower : visiblePowers) {
                setPowerOrderVisibility(visiblePower, false);
            }

            // reset renderSetting influence state, since we just cleared it
            // this must be set for unsyncUpdateProvince to work correctly
            synchronized (renderSettings) {
                renderSettings
                        .put(MapRenderer2.KEY_INFLUENCE_MODE, Boolean.TRUE);
            }

            // update province CSS values
            for (Province province : provinces) {
                unsyncUpdateProvince(province);
            }
        } else {
            // EXITING influence mode
            //
            if (oldRenderSettings == null) {
                throw new IllegalStateException("not in influence mode!");
            }

            // reset renderSetting influence state, since we just cleared it
            // this must be set for unsyncUpdateProvince to work correctly
            // we also want to reset the KEY_INFLUENCE_MODE
            synchronized (renderSettings) {
                final Iterator iter = oldRenderSettings.entrySet().iterator();
                while (iter.hasNext()) {
                    final Map.Entry me = (Map.Entry) iter.next();
                    renderSettings.put(me.getKey(), me.getValue());
                }

                renderSettings
                        .put(MapRenderer2.KEY_INFLUENCE_MODE, Boolean.FALSE);
            }

            // update province CSS values
            // this also takes care of:
            //		KEY_SHOW_UNORDERED
            // which require the updating of all province (via unsyncUpdateAllProvinces())
            unsyncUpdateAllProvinces();

            // activate render settings
            // we must cover all other keys, that we cleared when we entered influence mode:
            // 		KEY_SHOW_UNITS
            // 		KEY_SHOW_DISLODGED_UNITS
            // 		KEY_SHOW_ORDERS_FOR_POWERS
            // 		LAYER_SC
            //
            setElementVisibility(layerMap.get(LAYER_UNITS),
                    ((Boolean) getRenderSetting(KEY_SHOW_UNITS))
                            .booleanValue());
            setElementVisibility(
                    (SVGElement) layerMap.get(LAYER_DISLODGED_UNITS),
                    ((Boolean) getRenderSetting(KEY_SHOW_DISLODGED_UNITS))
                            .booleanValue());
            setElementVisibility(layerMap.get(LAYER_SC),
                    ((Boolean) getRenderSetting(KEY_SHOW_SUPPLY_CENTERS))
                            .booleanValue());
            unsyncSetVisiblePowers();    // takes care of KEY_SHOW_ORDERS_FOR_POWERS
            setElementVisibility(layerMap.get(LAYER_MAP),
                    ((Boolean) getRenderSetting(KEY_SHOW_MAP)).booleanValue());

            // destroy old render settings
            oldRenderSettings = null;

            // enable menu items [late]
            cm.setEnabled(ClientMenu.VIEW_ORDERS, !value);
            cm.setEnabled(ClientMenu.VIEW_UNITS, !value);
            cm.setEnabled(ClientMenu.VIEW_DISLODGED_UNITS, !value);
            cm.setEnabled(ClientMenu.VIEW_SUPPLY_CENTERS, !value);
            cm.setEnabled(ClientMenu.VIEW_UNORDERED, !value);
            cm.setEnabled(ClientMenu.VIEW_SHOW_MAP, !value);
        }
    }// unsyncSetInfluenceMode()


    /**
     * Find orders that are dependent, and call their updateDOM()
     * method again for possible re-rendering. This is called whenever
     * an order is added, deleted, or changed. If an order has just
     * been added or changed, it's update() method is not called.
     * <p>
     */
    protected void unsyncUpdateDependentOrders(final GUIOrder[] addedOrders) {
        //Log.println("unsyncUpdateDependentOrders() : ", addedOrder);

        // get ALL orders
        final MapInfo mapInfo = new DMRMapInfo(turnState);
        final Iterator iter = turnState.getAllOrders().iterator();
        while (iter.hasNext()) {
            final GUIOrder order = (GUIOrder) iter.next();

            if (order.isDependent()) {
                if (addedOrders != null) {
                    // do not update if we are in the addedOrders branch
                    for (GUIOrder addedOrder : addedOrders) {
                        if (order == addedOrder) {
                            break;
                        }
                    }
                }

                // update!
                order.updateDOM(mapInfo);
            }
        }
    }// unsyncUpdateDependentOrders()


    /**
     * Refresh and/or re-render all orders
     */
    protected void unsyncRecreateAllOrders() {
        // get ALL orders
        final MapInfo mapInfo = new DMRMapInfo(turnState);
        final Iterator iter = turnState.getAllOrders().iterator();
        while (iter.hasNext()) {
            final GUIOrder order = (GUIOrder) iter.next();
            order.updateDOM(mapInfo);
        }
    }// unsyncRecreateAllOrders()


    /**
     * Sends an update message to ALL orders, regardless of their dependency status.
     */
    private void unsyncUpdateAllOrders() {
        // get ALL orders
        final MapInfo mapInfo = new DMRMapInfo(turnState);
        final Iterator iter = turnState.getAllOrders().iterator();
        while (iter.hasNext()) {
            final GUIOrder order = (GUIOrder) iter.next();
            order.updateDOM(mapInfo);
        }
    }// unsyncUpdateAllOrders()


    /**
     * Unsynchronized updater: Renders ALL provinces but NOT orders
     */
    protected void unsyncUpdateAllProvinces() {
        for (final Province province : provinces) {
            final Tracker tracker = (Tracker) trackerMap.get(province);
            unsyncUpdateProvince(tracker, province, false);
        }
    }// unsyncUpdateAllProvincesAndOrders()


    /**
     * Unsynchronized province updater, used in both update methods.
     * We must synchronize around this because this method
     * will alter the DOM.
     */
    protected void unsyncUpdateProvince(final Province province,
                                        final boolean forceUpdate) {
        final Tracker tracker = trackerMap.get(province);
        unsyncUpdateProvince(tracker, province, forceUpdate);
    }// unsyncUpdateProvince()


    /**
     * Convenience method: non-forced.
     */
    protected void unsyncUpdateProvince(final Province province) {
        unsyncUpdateProvince(province, false);
    }// unsyncUpdateProvince()

    /**
     * Unsynchronized province updater, used in both update methods.
     * We must synchronize around this because this method
     * will alter the DOM. If the 'force' flag is true, we will update
     * the unit information EVEN IF it hasn't changed.
     */
    protected void unsyncUpdateProvince(final Tracker tracker, final Province province,
                                        final boolean force) {
        if (tracker == null) {
            // avoid NPE when in mid-render and batik exits
            return;
        }

        Unit posUnit = position.getUnit(province).orElse(null);
        if (tracker.getUnit() != posUnit || force) {
            changeUnitInDOM(posUnit, tracker, province, false);
            tracker.setUnit(posUnit);
        }

        posUnit = position.getDislodgedUnit(province).orElse(null);
        if (tracker.getDislodgedUnit() != posUnit || force) {
            changeUnitInDOM(posUnit, tracker, province, true);
            tracker.setDislodgedUnit(posUnit);
        }

        // set province hiliting based upon current render settings
        final SVGElement provinceGroupElement = tracker.getProvinceHiliteElement();
        if (provinceGroupElement != null) {
            // if we are in 'influence mode', we hilite provinces differently
            if (renderSettings
                    .get(MapRenderer2.KEY_INFLUENCE_MODE) == Boolean.TRUE) {
                // we are in influence mode
                //
                // we only hilite provinces that have a lastOccupier set and are NOT sea provinces
                if (position.getLastOccupier(province) != null && province
                        .isLand()) {
                    setCSSIfChanged(provinceGroupElement,
                            tracker.getPowerCSSClass(
                                    position.getLastOccupier(province)
                                            .orElse(null)));
                } else {
                    // use default province CSS styling, if not already
                    setCSSIfChanged(provinceGroupElement,
                            tracker.getOriginalProvinceCSS());
                }
            } else {
                // we are NOT in influence mode
                //
                if (renderSettings
                        .get(MapRenderer2.KEY_SHOW_UNORDERED) == Boolean.TRUE && (getPhaseApropriateUnit(
                        province) != null) && !isOrdered(province)) {
                    // we are unordered!
                    // unordered CSS style takes precedence over any existing style.
                    setCSSIfChanged(provinceGroupElement, UNORDERED);
                } else {
                    if (province.hasSupplyCenter()) {
                        // get supply center owner
                        final Power power = position.getSupplyCenterOwner(province)
                                .orElse(null);

                        // note:
                        // if we are not showing province SC (supply center) hilites, then
                        // we will just use the original CSS.
                        if (renderSettings
                                .get(MapRenderer2.KEY_SHOW_SUPPLY_CENTERS) == Boolean.TRUE) {
                            setCSSIfChanged(provinceGroupElement,
                                    tracker.getPowerCSSClass(power));
                        } else {
                            setCSSIfChanged(provinceGroupElement,
                                    tracker.getOriginalProvinceCSS());
                        }

                        // supply center hilites (always available, but may always be same color)
                        // if power is null, default is 'scnopower'.
                        // these may be 'hidden' or 'visible' depending upon the Render settings for the above key.
                        setCSSIfChanged(tracker.getSCElement(),
                                getSCCSSClass(power));
                    } else {
                        // set to original CSS style; no special hiliting here.
                        setCSSIfChanged(provinceGroupElement,
                                tracker.getOriginalProvinceCSS());
                    }
                }
            }
        }// if(provinceGroupElement != null)
    }// unsyncUpdateProvince()


    /**
     * Updates an SC element with new position information.
     * No change made to CSS.
     */
    protected void unsyncUpdateSC(final Province province) {
        final Tracker tracker = (Tracker) trackerMap.get(province);
        final SVGElement scEl = tracker.getSCElement();
        if (scEl != null) {
            final Point2D.Float pos = mapMeta.getSCPt(province);
            scEl.setAttributeNS(null, SVGConstants.SVG_X_ATTRIBUTE,
                    String.valueOf(pos.x));
            scEl.setAttributeNS(null, SVGConstants.SVG_Y_ATTRIBUTE,
                    String.valueOf(pos.y));
        }
    }// unsyncUpdateSC()


    /**
     * Changes a Unit in the DOM
     */
    private void changeUnitInDOM(final Unit posUnit, final Tracker tracker,
                                 final Province province, final boolean isDislodged) {
        // make tracker unit mirror posUnit
        //
        SVGElement newElement = null;

        // get old element (dislodged or normal)
        final SVGElement oldElement = (isDislodged) ? tracker
                .getDislodgedUnitElement() : tracker.getUnitElement();

        // remove, add, or replace as appropriate
        if (posUnit == null && oldElement == null) {
            // case 0: do nothing
            return;
        } else if (posUnit == null && oldElement != null) {
            // case 1: new unit null, old unit not null: delete old unit element
            oldElement.getParentNode().removeChild(oldElement);
        } else if (oldElement == null && posUnit != null) {
            // case 2: new unit not null, old unit null: create new element, then add
            newElement = makeUnitUse(posUnit, province, isDislodged);
            final SVGElement layer = ((isDislodged) ? layerMap
                    .get(LAYER_DISLODGED_UNITS) : layerMap.get(LAYER_UNITS));
            layer.appendChild(newElement);
        } else {
            // case 3: neither unit null, but different; create new element, then replace
            newElement = makeUnitUse(posUnit, province, isDislodged);
            oldElement.getParentNode().replaceChild(newElement, oldElement);
        }

        // set the tracker unit
        if (isDislodged) {
            tracker.setDislodgedUnit(newElement, posUnit);
        } else {
            tracker.setUnit(newElement, posUnit);
        }
    }// changeUnitInDOM


    /**
     * Creates a Unit of the given type / owner color,  via a <use> symbol, in the right place
     */
    private SVGElement makeUnitUse(final Unit u, final Province province,
                                   final boolean isDislodged) {
        // determine symbol ID
        String symbolID = null;
        if (u.getType().equals(Unit.Type.FLEET)) {
            symbolID = (isDislodged) ? SYMBOL_DISLODGED_FLEET : SYMBOL_FLEET;
        } else if (u.getType().equals(Unit.Type.ARMY)) {
            symbolID = (isDislodged) ? SYMBOL_DISLODGED_ARMY : SYMBOL_ARMY;
        } else if (u.getType().equals(Unit.Type.WING)) {
            symbolID = (isDislodged) ? SYMBOL_DISLODGED_WING : SYMBOL_WING;
        } else {
            throw new IllegalArgumentException(
                    "undefined or unknown unit type");
        }

        // get symbol size data
        final MapMetadata.SymbolSize symbolSize = mapMeta.getSymbolSize(symbolID);
        assert (symbolSize != null);

        // get the rectangle coordinates
        final Coast coast = u.getCoast();
        final Point2D.Float pos = (isDislodged) ? mapMeta
                .getDislodgedUnitPt(province, coast) : mapMeta
                .getUnitPt(province, coast);
        final float x = pos.x;
        final float y = pos.y;
        return SVGUtils.createUseElement(doc, symbolID, null,
                getUnitCSSClass(u.getPower()), x, y, symbolSize);
    }// makeUnitUse()


    /**
     * Returns the CSS class for power fills.
     * If the power starts with a number, the a capital X is prepended.
     */
    private String getUnitCSSClass(final Power power) {
        final StringBuffer sb = new StringBuffer(power.getName().length() + 4);
        sb.append("unit");
        sb.append(getPowerName(power));
        return sb.toString();
    }// getUnitCSSClass()


    /**
     * Returns the CSS class for Supply Center fills. Returns SC_NOPOWER if power is null.
     * If the power starts with a number, the a capital X is prepended.
     */
    private String getSCCSSClass(final Power power) {
        if (power == null) {
            return SC_NOPOWER;
        }

        final StringBuffer sb = new StringBuffer(power.getName().length() + 2);
        sb.append("sc");
        sb.append(getPowerName(power));
        return sb.toString();
    }// getSCCSSClass()


    /**
     * Creates the power name, and prepends an "X" if power name starts with a digit.
     * Does not accept null arguments.
     */
    private String getPowerName(final Power power) {
        final String name = power.getName().toLowerCase();

        if (Character.isDigit(name.charAt(0))) {
            final StringBuffer sb = new StringBuffer(name.length() + 1);
            sb.append('X');
            sb.append(name);
            return sb.toString();
        }

        return name;
    }// getPowerName()


    /**
     * Creates a Supply Center via a &lt;use&gt; symbol, in the right place.
     * Power may be null.
     */
    private SVGElement makeSCUse(final Province province, final Power power) {
        final Point2D.Float pos = mapMeta.getSCPt(province);
        final MapMetadata.SymbolSize symbolSize = mapMeta.getSymbolSize(SYMBOL_SC);
        return SVGUtils
                .createUseElement(doc, SYMBOL_SC, null, getSCCSSClass(power),
                        pos.x, pos.y, symbolSize);
    }// makeSCUse()


    /**
     * Set the visibility of an element
     */
    protected void setElementVisibility(final SVGElement element, final boolean value) {
        // optimization: if no change, make no change to the DOM.
        final String oldValue = element
                .getAttributeNS(null, CSSConstants.CSS_VISIBILITY_PROPERTY);

        if (value) {
            if (!oldValue.equals(CSSConstants.CSS_VISIBLE_VALUE)) {
                element.setAttributeNS(null,
                        CSSConstants.CSS_VISIBILITY_PROPERTY,
                        CSSConstants.CSS_VISIBLE_VALUE);
            }
        } else {
            if (!oldValue.equals(CSSConstants.CSS_HIDDEN_VALUE)) {
                element.setAttributeNS(null,
                        CSSConstants.CSS_VISIBILITY_PROPERTY,
                        CSSConstants.CSS_HIDDEN_VALUE);
            }
        }
    }// setElementVisibility()


    /**
     * Ensures that all required Symbol elements are present in the SVG file,
     * and that MapMetadata has appropriate symbol sizing information for all
     * symbols.
     */
    private void checkSymbols() throws MapException {
        // check symbol presence
        final Map map = SVGUtils
                .tagFinderSVG(Arrays.asList(SYMBOLS), doc.getRootElement());
        for (String SYMBOL1 : SYMBOLS) {
            if (map.get(SYMBOL1) == null) {
                throw new MapException(
                        "Missing required <symbol> or <g> element with id=\"" + SYMBOL1 + "\".");
            }
        }

        // check MMD
        for (String SYMBOL : SYMBOLS) {
            if (mapMeta.getSymbolSize(SYMBOL) == null) {
                throw new MapException(
                        "Missing required <jdipNS:SYMBOLSIZE> element for symbol name \"" + SYMBOL + "\"");
            }
        }

    }// checkSymbols()


    /**
     * ensures that we extract all the layer info into the layerMap
     */
    private void mapLayers() throws MapException {
        SVGUtils.tagFinderSVG(layerMap, Arrays.asList(LAYERS),
                doc.getRootElement());
        for (String LAYER : LAYERS) {
            if (layerMap.get(LAYER) == null) {
                throw new MapException(
                        "Missing required layer (<g> element) with id=\"" + LAYER + "\".");
            }
        }
    }// mapLayers()


    /**
     * Ensure that a region exists for each province. Regions are elements that can be
     * assigned a mouse listeners [G, RECT, closed PATH, etc.].
     * <p>
     * This is critical! If this fails, GUI order input / region detection cannot work.
     * <p>
     * This also sets up a special lookup table for multicoastal provinces.
     */
    private void validateAndSetupMouseRegions() throws MapException {
        final SVGElement[] mouseElements = SVGUtils
                .idFinderSVG((SVGElement) layerMap.get(LAYER_MOUSE));

        for (SVGElement mouseElement : mouseElements) {
            // get id, which must be a province with or without a coast
            final String id = mouseElement
                    .getAttribute(SVGConstants.SVG_ID_ATTRIBUTE);

            // parse ID; determine if there is a coast.
            final String provinceID = Coast.getProvinceName(id);
            final Coast coast = Coast.parse(id);
            final Province province = worldMap.getProvince(provinceID);

            if (province == null) {
                throw new MapException(
                        "Province \"" + provinceID + "\" in " + LAYER_MOUSE + " is invalid.");
            }

            // can we even target this element??
            if (mouseElement instanceof EventTarget) {
                // map the location, but only if the coast is defined.
                if (coast != Coast.UNDEFINED) {
                    locMap.put(id.toLowerCase(), new Location(province, coast));
                }
            } else {
                throw new MapException(
                        LAYER_MOUSE + "element: " + mouseElement + " cannot be targetted by mouse events.");
            }
        }
    }// validateAndSetupMouseRegions()


    /**
     * Looks for groups with an ID prefaced by an underscore
     * If that which follows the underscore is a province ID,
     * it is added to the tracker objects.
     * <p>
     * All SVGElements with underscore-prefaced province IDs will
     * be rendered using the region coloring CSS styles. If no province
     * element is found, it will not be so colored.
     */
    private void addProvinceHilitesToTracker() {
        // Make a list of all possible provinces with underscores
        final ArrayList<String> uscoreProvList = new ArrayList<>(125);    // stores underscore-preceded names
        final ArrayList<Province> lookupProvList = new ArrayList<>(125);    // stores corresponding Province
        for (Province province : provinces) {
            final String[] shortNames = province.getShortNames()
                    .toArray(new String[0]);
            for (String shortName : shortNames) {
                uscoreProvList.add('_' + shortName);
                lookupProvList.add(province);
            }
        }

        // try to find as many of the above names as possible
        final Map map = SVGUtils
                .tagFinderSVG(uscoreProvList, doc.getRootElement(), true);

        // safety check
        assert (uscoreProvList.size() == lookupProvList.size());

        // go through the map and add non-null objects to the tracker.
        for (int i = 0; i < lookupProvList.size(); i++) {
            final SVGElement element = (SVGElement) map.get(uscoreProvList.get(i));
            if (element != null) {
                final Tracker tracker = trackerMap
                        .get(lookupProvList.get(i));
                tracker.setProvinceHiliteElement(element);
            }
        }
    }// addProvinceHilitesToTracker()


    /**
     * Gets the appropriate Unit for a phase. This gets the non-dislodged unit
     * during Movement and Adjustment phases, and the dislodged unit during the
     * Retreat phase.
     */
    private Unit getPhaseApropriateUnit(final Province p) {
        return (isDislodgedPhase) ? position.getDislodgedUnit(p)
                .orElse(null) : position.getUnit(p).orElse(null);
    }// getPhaseAppropriateUnit()


    /**
     * Sets a CSS style on an element, but only if it is different
     * from the existing CSS style. Returns true if a change was made.
     * A null css value is not allowed.
     */
    private boolean setCSSIfChanged(final SVGElement el, final String css) {
        final String oldCSS = el
                .getAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE);

        if (!css.equals(oldCSS)) {
            el.setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, css);
            return true;
        }

        return false;
    }// setCSSIfChanged()


    /**
     * Searches the TurnState to see if the given province has an order.
     * <p>
     * This is made faster by first determining if a unit is present;
     * we then look at the orders for the power of that unit (phase
     * appropriate). This cuts the searching time.
     */
    private boolean isOrdered(final Province province) {
        final Unit unit = getPhaseApropriateUnit(province);
        if (unit != null) {
            final List<dip.order.Order> list = turnState.getOrders(unit.getPower());
            final Iterator<dip.order.Order> iter = list.iterator();
            while (iter.hasNext()) {
                final Orderable order = iter.next();
                if (order.getSource().isProvinceEqual(province)) {
                    return true;
                }
            }
        }

        return false;
    }// isOrdered()


    /**
     * Keeps track of the DOM Elements and other info to monitor changes; ONE (and only one)
     * Tracker object will exist for each Province with any items to be rendered (SC, units, etc.)
     */
    protected class Tracker {
        // elements (to avoid traversing SVG DOM)
        private SVGElement elUnit = null;
        private SVGElement elDislodgedUnit = null;

        // things to monitor change
        private Unit unit = null;
        private Unit dislodgedUnit = null;

        // associated SVGElement of province, to determine if a hilite should be rendered
        // really, only provinces with supply centers need be rendered in this manner
        private SVGElement provHilite = null;

        // Supply Center SVGElement. Provinces without supply centers
        // will not have one.
        private SVGElement scElement = null;

        // original Province CSS style(s) if any.
        // if multiple styles, they will be separated by spaces
        private String provOriginalCSS = null;

        /**
         * Create a Tracker object
         */
        public Tracker() {
        }

        public SVGElement getUnitElement() {
            return elUnit;
        }

        public SVGElement getDislodgedUnitElement() {
            return elDislodgedUnit;
        }


        public Unit getUnit() {
            return unit;
        }

        public Unit getDislodgedUnit() {
            return dislodgedUnit;
        }

        public void setUnit(final Unit u) {
            unit = u;
        }

        public void setDislodgedUnit(final Unit u) {
            dislodgedUnit = u;
        }


        public void setProvinceHiliteElement(final SVGElement el) {
            provHilite = el;

            if (provHilite != null) {
                provOriginalCSS = provHilite
                        .getAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE);
            }
        }// setProvinceHiliteElement()


        public SVGElement getProvinceHiliteElement() {
            return provHilite;
        }

        /**
         * Returns original province CSS style(s), or null, derived when setProvinceHiliteElement() was called.
         */
        public String getOriginalProvinceCSS() {
            return provOriginalCSS;
        }

        /**
         * Returns the CSS class for the power, or, the original CSS defined in the
         * SVG file (if available) if power is null. If no original CSS exists, then,
         * no CSS value is returned.
         */
        public String getPowerCSSClass(final Power power) {
            if (power == null) {
                return getOriginalProvinceCSS();
            }

            return getPowerName(power);
        }// getPowerCSSClass()

        public void setSCElement(final SVGElement el) {
            scElement = el;
        }

        public SVGElement getSCElement() {
            return scElement;
        }


        public void setUnit(final SVGElement el, final Unit unit) {
            elUnit = el;
            this.unit = unit;
        }// setUnit()

        public void setDislodgedUnit(final SVGElement el, final Unit unit) {
            elDislodgedUnit = el;
            dislodgedUnit = unit;
        }// setDislodgedUnit()


        /**
         * For debugging only
         */
        public String toString() {
            final StringBuffer sb = new StringBuffer(128);
            sb.append("elUnit=");
            sb.append(elUnit);
            sb.append(",unit=");
            sb.append(unit);
            sb.append(']');
            return sb.toString();
        }// toString()

    }// inner class Tracker


    /**
     * Implicit class for MapInfo interface
     */
    protected class DMRMapInfo extends GUIOrder.MapInfo {
        public DMRMapInfo(final TurnState ts) {
            super(ts);
        }// DMRMapInfo()

        public MapMetadata getMapMetadata() {
            return mapMeta;
        }

        public String getPowerCSS(final Power power) {
            return DefaultMapRenderer2.this.getPowerName(power);
        }

        public String getUnitCSS(final Power power) {
            return DefaultMapRenderer2.this.getUnitCSSClass(power);
        }

        public String getSymbolName(final Unit.Type unitType) {
            return DefaultMapRenderer2.this.getSymbolName(unitType);
        }

        public SVGDocument getDocument() {
            return doc;
        }

        public Power[] getDisplayablePowers() {
            final Power[] powers = super.getDisplayablePowers();
            if (powers == null) {
                return mapPanel.getClientFrame().getDisplayablePowers();
            }

            return powers;
        }// getDisplayablePowers()

        public SVGGElement getPowerSVGGElement(final Power p, final int z) {
            return DefaultMapRenderer2.this.getPowerSVGGElement(p, z);
        }
    }// nested class DMRMapInfo


}// class DefaultMapRenderer
