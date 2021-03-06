//
//  @(#)DMR2RenderCommandFactory.java		5/2003
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

import dip.world.Power;
import dip.world.Province;
import dip.world.TurnState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.svg.SVGElement;

public class DMR2RenderCommandFactory extends RenderCommandFactory {
    private static final Logger LOG = LoggerFactory.getLogger(
            DMR2RenderCommandFactory.class);

    /** */
    @Override
    public RenderCommandFactory.RCRenderAll createRCRenderAll(final MapRenderer2 mr) {
        return new RCRenderAll(mr);
    }// RCRenderAll()

    /** */
    @Override
    public RenderCommandFactory.RCSetTurnstate createRCSetTurnstate(
            final MapRenderer2 mr, final TurnState ts) {
        return new RCSetTurnstate(mr, ts);
    }// RCSetTurnstate()

    /**
     * Updates the province, based on the current Position object
     * <p>
     * This typically will change units, dislodged units, and supply center
     * information for the given Province. It can add or remove items.
     * <p>
     * Essentially, it synchronizes the visual state with the current state
     * of the Province.
     */
    @Override
    public RenderCommandFactory.RCRenderProvince createRCRenderProvince(
            final MapRenderer2 mr, final Province province) {
        return new RCRenderProvince(mr, province);
    }// RCRenderProvince()

    /** */
    @Override
    public RenderCommandFactory.RCSetLabel createRCSetLabel(final MapRenderer2 mr,
                                                            final Object labelValue) {
        return new RCSetLabel(mr, labelValue);
    }// RCSetLabel()

    /** */
    @Override
    public RenderCommandFactory.RCSetDisplaySC createRCSetDisplaySC(
            final MapRenderer2 mr, final boolean value) {
        return new RCSetDisplaySC(mr, value);
    }// RCSetDisplaySC()

    /** */
    @Override
    public RenderCommandFactory.RCSetDisplayUnits createRCSetDisplayUnits(
            final MapRenderer2 mr, final boolean value) {
        return new RCSetDisplayUnits(mr, value);
    }// RCSetDisplayUnits()

    /** */
    @Override
    public RenderCommandFactory.RCSetDisplayDislodgedUnits createRCSetDisplayDislodgedUnits(
            final MapRenderer2 mr, final boolean value) {
        return new RCSetDisplayDislodgedUnits(mr, value);
    }// RCSetDisplayDislodgedUnits()

    /** */
    @Override
    public RenderCommandFactory.RCSetDisplayUnordered createRCSetDisplayUnordered(
            final MapRenderer2 mr, final boolean value) {
        return new RCSetDisplayUnordered(mr, value);
    }// RCSetDisplayUnordered()

    /** */
    @Override
    public RenderCommandFactory.RCSetInfluenceMode createRCSetInfluenceMode(
            final MapRenderer2 mr, final boolean value) {
        return new RCSetInfluenceMode(mr, value);
    }// RCSetInfluenceMode()

    /** */
    @Override
    public RenderCommandFactory.RCSetPowerOrdersDisplayed createRCSetPowerOrdersDisplayed(
            final MapRenderer2 mr, final Power[] displayedPowers) {
        return new RCSetPowerOrdersDisplayed(mr, displayedPowers);
    }// RCSetPowerOrdersDisplayed()

    /** */
    @Override
    public RenderCommandFactory.RCShowMap createRCShowMap(final MapRenderer2 mr,
                                                          final boolean value) {
        return new RCShowMap(mr, value);
    }// RCSetPowerOrdersDisplayed()


    /**
     * Force re-rendering of all orders (erase then update). The Turnstate better have been set.
     */
    public RenderCommand createRCRenderAllForced(final MapRenderer2 mr) {
        return new RenderCommand(mr) {
            @Override
            public void execute() {
                LOG.debug("DMR2RCF::createRCRenderAllForced()");
                final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;

                // destroy the existing orders
                dmr2.unsyncDestroyAllOrders();

                // for each province, update the province and orders.
                dmr2.unsyncRecreateAllOrders();
                dmr2.unsyncUpdateAllProvinces();
                LOG.debug("  DMR2RCF::createRCRenderAllForced() complete.");
            }// execute()
        };
    }// RCRenderProvince()

    /**
     * Force-update the unit or dislodged unit information
     */
    public RenderCommand createRCRenderProvinceForced(final MapRenderer2 mr,
                                                      final Province province) {
        return new RenderCommand(mr) {
            @Override
            public void execute() {
                LOG.debug("DMR2RCF::createRCRenderProvinceForced(): {}",
                        province);
                final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
                dmr2.unsyncUpdateProvince(province, true);
            }// execute()
        };
    }// RCRenderProvince()


    /**
     * Update a Supply Center (position)
     */
    public RenderCommand createRCUpdateSC(final MapRenderer2 mr,
                                          final Province province) {
        return new RenderCommand(mr) {
            @Override
            public void execute() {
                LOG.debug("DMR2RCF::createRCUpdateSC(): {}", province);
                final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
                dmr2.unsyncUpdateSC(province);
            }// execute()
        };
    }// RCRenderProvince()


    /**
     * Render (Refresh) the entire map
     */
    protected static class RCRenderAll extends RenderCommandFactory.RCRenderAll {
        public RCRenderAll(final MapRenderer2 dmr2) {
            super(dmr2);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCRenderAll()");
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;

            // for each province, update the province and orders.
            // TODO: consider destroying all orders before rendering
            // (slower, but less error-prone??)
            // see RCRenderAllForced for details
            dmr2.unsyncRecreateAllOrders();
            dmr2.unsyncUpdateAllProvinces();
            LOG.debug("  DMR2RCF::RCRenderAll() complete.");
        }// execute()
    }// nested class RCRenderAll


    /**
     * Render the entire map
     */
    protected static class RCSetTurnstate extends RenderCommandFactory.RCSetTurnstate {
        public RCSetTurnstate(final MapRenderer2 dmr2, final TurnState ts) {
            super(dmr2, ts);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCSetTurnstate()");
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;

            // set the TurnState
            dmr2.setTurnState(ts);

            // remove all orders from the DOM
            dmr2.unsyncRemoveAllOrdersFromDOM();
        }// execute()
    }// nested class RCSetTurnstate


    /**
     * Render a particular Province
     */
    protected static class RCRenderProvince extends RenderCommandFactory.RCRenderProvince {
        public RCRenderProvince(final MapRenderer2 dmr2, final Province province) {
            super(dmr2, province);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCRenderProvince(): {}", province);
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
            dmr2.unsyncUpdateProvince(province);
        }// execute()
    }// nested class RCRenderProvince

    /**
     * Change how labels are displayed
     */
    protected static class RCSetLabel extends RenderCommandFactory.RCSetLabel {
        public RCSetLabel(final MapRenderer2 dmr2, final Object value) {
            super(dmr2, value);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCSetLabel(): {}", labelValue);
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;

            Object newLabelValue = labelValue;
            final MapMetadata mmd = dmr2.getMapMetadata();

            // not all maps have all label settings. All will have
            // NONE, but not all will have BRIEF and FULL. So, degrade
            // gracefully based upon map metadata.
            // full->brief->none is how we degrade.
            //
            if (newLabelValue == MapRenderer2.VALUE_LABELS_FULL && !mmd
                    .getDisplayParamBoolean(MapMetadata.ATT_LABELS_FULL,
                            false)) {
                newLabelValue = MapRenderer2.VALUE_LABELS_BRIEF;
                LOG.debug("  degrading label to: {}", newLabelValue);
            }

            if (newLabelValue == MapRenderer2.VALUE_LABELS_BRIEF && !mmd
                    .getDisplayParamBoolean(MapMetadata.ATT_LABELS_BRIEF,
                            false)) {
                newLabelValue = MapRenderer2.VALUE_LABELS_NONE;
                LOG.debug("  degrading label to: {}", newLabelValue);
            }

            final SVGElement elBrief = dmr2.layerMap
                    .get(DefaultMapRenderer2.LABEL_LAYER_BRIEF);
            final SVGElement elFull = dmr2.layerMap
                    .get(DefaultMapRenderer2.LABEL_LAYER_FULL);

            if (newLabelValue == MapRenderer2.VALUE_LABELS_NONE) {
                dmr2.setElementVisibility(elBrief, false);
                dmr2.setElementVisibility(elFull, false);
            } else if (newLabelValue == MapRenderer2.VALUE_LABELS_BRIEF) {
                dmr2.setElementVisibility(elBrief, true);
                dmr2.setElementVisibility(elFull, false);
            } else if (newLabelValue == MapRenderer2.VALUE_LABELS_FULL) {
                dmr2.setElementVisibility(elBrief, false);
                dmr2.setElementVisibility(elFull, true);
            }

            dmr2.setRenderSetting(MapRenderer2.KEY_LABELS, labelValue);
        }// execute()
    }// nested class RCSetLabel


    /** */
    protected static class RCSetDisplaySC extends RenderCommandFactory.RCSetDisplaySC {
        public RCSetDisplaySC(final MapRenderer2 dmr2, final boolean value) {
            super(dmr2, value);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCSetDisplaySC()");
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
            dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_SUPPLY_CENTERS,
                    Boolean.valueOf(value));
            dmr2.setElementVisibility(dmr2.layerMap
                    .get(DefaultMapRenderer2.LAYER_SC), value);
            dmr2.unsyncUpdateAllProvinces();    // update provinces, but not orders
        }// execute()
    }// nested class RCSetDisplaySC

    /** */
    protected static class RCSetDisplayUnits extends RenderCommandFactory.RCSetDisplayUnits {
        public RCSetDisplayUnits(final MapRenderer2 dmr2, final boolean value) {
            super(dmr2, value);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCSetDisplayUnits()");
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
            dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_UNITS,
                    Boolean.valueOf(value));
            dmr2.setElementVisibility(dmr2.layerMap
                    .get(DefaultMapRenderer2.LAYER_UNITS), value);
        }// execute()
    }// nested class RCSetDisplayUnits

    /** */
    protected static class RCSetDisplayDislodgedUnits extends RenderCommandFactory.RCSetDisplayDislodgedUnits {
        public RCSetDisplayDislodgedUnits(final MapRenderer2 dmr2, final boolean value) {
            super(dmr2, value);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCSetDisplayDislodgedUnits()");
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
            dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_DISLODGED_UNITS,
                    Boolean.valueOf(value));
            dmr2.setElementVisibility((SVGElement) dmr2.layerMap
                    .get(DefaultMapRenderer2.LAYER_DISLODGED_UNITS), value);
        }// execute()
    }// nested class RCSetDisplayDislodgedUnits

    /** */
    protected static class RCSetDisplayUnordered extends RenderCommandFactory.RCSetDisplayUnordered {
        public RCSetDisplayUnordered(final MapRenderer2 dmr2, final boolean value) {
            super(dmr2, value);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCSetDisplayUnordered()");
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
            dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_UNORDERED,
                    Boolean.valueOf(value));
            dmr2.unsyncUpdateAllProvinces();    // update provinces, but not orders
        }// execute()
    }// nested class RCSetDisplayUnordered

    /** */
    protected static class RCSetInfluenceMode extends RenderCommandFactory.RCSetInfluenceMode {
        public RCSetInfluenceMode(final MapRenderer2 dmr2, final boolean value) {
            super(dmr2, value);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCSetInfluenceMode()");
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
            dmr2.unsyncSetInfluenceMode(value);
        }// execute()
    }// nested class RCSetInfluenceMode

    /**  */
    protected static class RCShowMap extends RenderCommandFactory.RCShowMap {
        public RCShowMap(final MapRenderer2 dmr2, final boolean value) {
            super(dmr2, value);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCShowMap()");
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
            dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_MAP,
                    Boolean.valueOf(value));
            final SVGElement mapLayer = (SVGElement) dmr2.layerMap
                    .get(DefaultMapRenderer2.LAYER_MAP);
            assert mapLayer != null;
            dmr2.setElementVisibility(mapLayer, value);
        }// execute()
    }// nested class RCShowMap


    /** */
    protected static class RCSetPowerOrdersDisplayed extends RenderCommandFactory.RCSetPowerOrdersDisplayed {
        public RCSetPowerOrdersDisplayed(final MapRenderer2 dmr2, final Power[] powers) {
            super(dmr2, powers);
        }

        @Override
        public void execute() {
            LOG.debug("DMR2RCF::RCSetPowerOrdersDisplayed()");
            final DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
            dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_ORDERS_FOR_POWERS,
                    displayedPowers);
            dmr2.unsyncSetVisiblePowers();
        }// execute()
    }// nested class RCSetPowerOrdersDisplayed


}// class RenderCommandFactory

	
