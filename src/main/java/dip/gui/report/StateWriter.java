//
//  @(#)StateWriter.java		6/2003
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
package dip.gui.report;

import dip.gui.ClientFrame;
import dip.gui.dialog.TextViewer;
import dip.gui.dialog.TextViewer.TVRunnable;
import dip.misc.Help;
import dip.misc.Help.HelpID;
import dip.misc.Utils;
import dip.order.Order;
import dip.order.OrderFormatOptions;
import dip.process.Adjustment;
import dip.process.Adjustment.AdjustmentInfo;
import dip.process.Adjustment.AdjustmentInfoMap;
import dip.world.Phase;
import dip.world.Phase.PhaseType;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.TurnState;
import dip.world.Unit;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Writes the current game state as HTML.
 * <p>
 * Includes:
 * <ol>
 * <li>Current Phase
 * <li>Current Unit Positions
 * <li>Current Orders
 * <li>Current Supply Center owners
 * <li>Current Adjustments <b>or</b> Current Retreats
 * </ol>
 */
public class StateWriter {
    // i18n constants
    private static final String HTML_TEMPLATE = "StateWriter.template";
    private static final String MSG_NO_ORDERS_SUBMITTED = "StateWriter.order.notsubmitted";
    private static final String MSG_POWER_ELIMINATED = "StateWriter.order.eliminated";
    private static final String MSG_UNAVAILABLE = "StateWriter.order.unavailable";
    private static final String MSG_NONE = "StateWriter.quantity.none";
    private static final String DISLODGED_HEADER_TEXT = "StateWriter.header.dislodged";
    private static final String ADJUSTMENT_HEADER_TEXT = "StateWriter.header.adjustment";
    private static final String NO_DISLODGED_UNITS = "StateWriter.dislodged.none";
    private static final String ADJ_BUILD_TEXT = "StateWriter.adjustment.text.build";
    private static final String ADJ_REMOVE_TEXT = "StateWriter.adjustment.text.remove";
    private static final String ADJ_BASIC_TEXT = "StateWriter.adjustment.text";
    private static final String ADJ_NOCHANGE_TEXT = "StateWriter.adjustment.text.nochange";
    private static final String ADJ_BLOCKED_BUILD_TEXT = "StateWriter.adjustment.text.blockedbuilds";
    private static final String SC_NUM = "StateWriter.sc.number";
    private static final String ORD_TOO_FEW = "StateWriter.order.toofew";

    // i18n dialog constants
    private static final String DIALOG_TITLE = "StateWriter.dialog.title";


    // instance constants
    private final List<Power> displayablePowers;
    private final TurnState turnState;
    private final List<Power> allPowers;
    private final Map<Power, LinkedList<String>> powerMap;
    private final AdjustmentInfoMap adjMap;
    private final OrderFormatOptions ofo;


    /**
     * Displays a summary of the current game state as HTML.
     * Obeys the displayablePowers setting (obtained from
     * ClientFrame). If no ClientFrame supplied, all displayable
     * powers are shown.
     */
    public static String stateToHTML(final ClientFrame cf, final TurnState ts) {
        final StateWriter sw = new StateWriter(cf, ts);
        return sw.getStateAsHTML();
    }// stateToHTML()


    /**
     * Returns the HTML-encoded current state inside a dialog.
     */
    public static void displayDialog(final ClientFrame clientFrame,
                                     final TurnState ts) {
        String title = Utils.getLocalString(DIALOG_TITLE) +
                ": " +
                ts.getPhase();

        final TextViewer tv = new TextViewer(clientFrame);
        tv.setEditable(false);
        tv.addSingleButton(tv.makeOKButton());
        tv.setTitle(title);
        tv.setHelpID(HelpID.Dialog_StatusReport);
        tv.setHeaderVisible(false);
        tv.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        tv.lazyLoadDisplayDialog(new TVRunnable() {
            @Override
            public void run() {
                setText(stateToHTML(clientFrame, ts));
            }
        });
    }// displayDialog()


    /**
     * StateWriter constructor
     */
    private StateWriter(final ClientFrame cf, final TurnState ts) {
        assert cf != null;
        turnState = ts;
        allPowers = ts.getWorld().getMap().getPowers();
        displayablePowers = cf == null ? allPowers : Arrays
                .asList(cf.getDisplayablePowers());
        powerMap = getUnitsByPower();
        adjMap = Adjustment.getAdjustmentInfo(turnState,
                turnState.getWorld().getRuleOptions(), allPowers);
        ofo = cf.getOFO();
    }// StateWriter()


    /**
     * Write state as HTML
     */
    private String getStateAsHTML() {
        // get template
        final String templateText = Utils
                .getText(Utils.getLocalString(HTML_TEMPLATE));

        // write retreat or adjustment information, if appropriate.
        String header = "";
        String info = "";

        if (turnState.getPhase().getPhaseType() == PhaseType.RETREAT) {
            header = Utils.getLocalString(DISLODGED_HEADER_TEXT);
            info = getDislodgedInfo();
        } else if (turnState.getPhase()
                .getPhaseType() == PhaseType.ADJUSTMENT) {
            header = Utils.getLocalString(ADJUSTMENT_HEADER_TEXT);
            info = getAdjustmentInfo();
        }


        // get template objects
        final Object[] templateData = new Object[]{turnState.getPhase(),    // {0} : Phase
                getUnitLocationTable(),    // {1} : Unit Location Table
                getOrders(),            // {2} : Orders, by power. Non-displayed powers listed as "unknown"
                getSCInfo(),            // {3} : Supply Center Information
                header,                    // {4} : Dislodged/Adjustment header (or empty)
                info                    // {5} : Dislodged/Adjustment info (or empty)
        };

        // format into template
        return Utils.format(templateText, templateData);
    }// getState()

    /**
     * Unit Location Table<p>
     * The positions of all units -- for all powers -- are always available.
     * A light highlight color for the rows is applied for readability.
     */
    private String getUnitLocationTable() {
        final StringBuffer sb = new StringBuffer(1024);
        int nRows = -1;    // max # of rows

        sb.append("<table cellpadding=\"3\" cellspacing\"3\">");

        // column headers (the power name)
        sb.append("<tr>");
        for (int i = 0; i < allPowers.size(); i++) {
            // odd columns have bgcolor highlights
            if ((i & 1) == 0) {
                sb.append("<th bgcolor=\"F0F8FF\">");
            } else {
                sb.append("<th>");
            }

            sb.append("<u>");
            sb.append(allPowers.get(i));
            sb.append("</u></th>");

            // determine the maximum number of rows we will have (not including
            // the power name)
            final List<String> list = powerMap.get(allPowers.get(i));
            if (list.size() > nRows) {
                nRows = list.size();
            }
        }
        sb.append("</tr>");

        // column data (unit locations)
        for (int i = 0; i < nRows; i++) {
            sb.append("<tr>");

            for (int j = 0; j < allPowers.size(); j++) {
                // odd columns have bg color
                if ((j & 1) == 0) {
                    sb.append("<td bgcolor=\"F0F8FF\">");
                } else {
                    sb.append("<td>");
                }

                final List<String> list = powerMap.get(allPowers.get(j));
                if (i < list.size()) {
                    sb.append(list.get(i));
                }

                sb.append("</td>");
            }

            sb.append("</tr>");
        }


        sb.append("</table>");

        return sb.toString();
    }// getUnitLocationTable()


    /**
     * Write order information. Doesn't show orders for
     * powers that are hidden (not displayable)
     */
    private String getOrders() {
        final StringBuffer sb = new StringBuffer(2048);
        final Position position = turnState.getPosition();

        for (Power allPower : allPowers) {
            // print power name
            sb.append("<div class=\"indent1cm\"><b>");
            sb.append(allPower);
            sb.append("</b></div>");

            // if power is not displayable, mention that.
            boolean canShow = false;
            for (Power displayablePower : displayablePowers) {
                if (allPower == displayablePower) {
                    canShow = true;
                    break;
                }
            }

            sb.append("<div class=\"indent2cm\">");
            if (canShow) {
                // print submission/elimination information
                final List orders = turnState.getOrders(allPower);
                if (!orders.isEmpty()) {
                    final Iterator iter = orders.iterator();
                    while (iter.hasNext()) {
                        final Order order = (Order) iter.next();
                        sb.append(order.toFormattedString(ofo));
                        sb.append("<br>\n");
                    }

                    // but do we have orders for all units?
                    // indicate if we do not.
                    // this is phase dependent
                    final AdjustmentInfo adjInfo = adjMap
                            .get(allPower);
                    int diff = 0;
                    if (turnState.getPhase()
                            .getPhaseType() == PhaseType.RETREAT) {
                        diff = adjInfo.getDislodgedUnitCount() - orders.size();
                    } else if (turnState.getPhase()
                            .getPhaseType() == PhaseType.ADJUSTMENT) {
                        diff = Math.abs(adjInfo.getAdjustmentAmount()) - orders
                                .size();
                    } else if (turnState.getPhase()
                            .getPhaseType() == PhaseType.MOVEMENT) {
                        diff = adjInfo.getUnitCount() - orders.size();
                    }

                    if (diff > 0) {
                        sb.append(Utils.getLocalString(ORD_TOO_FEW,
                                new Integer(diff)));
                        sb.append("<br>\n");
                    }

                } else {
                    // if no orders are submitted, we must mention that, unless power
                    // has been eliminated....
                    if (position.isEliminated(allPower)) {
                        sb.append(Utils.getLocalString(MSG_POWER_ELIMINATED));
                    } else {
                        sb.append(
                                Utils.getLocalString(MSG_NO_ORDERS_SUBMITTED));
                    }

                    sb.append("<br>\n");
                }
            } else {
                // (not available), unless eliminated
                if (position.isEliminated(allPower)) {
                    sb.append(Utils.getLocalString(MSG_POWER_ELIMINATED));
                } else {
                    sb.append(Utils.getLocalString(MSG_UNAVAILABLE));
                }

                sb.append("<br>\n");
            }

            sb.append("</div>");
        }

        return sb.toString();
    }// getOrders()


    /**
     * Write SC ownership information
     */
    private String getSCInfo() {
        final StringBuffer sb = new StringBuffer(1024);
        sb.append("<div class=\"indent1cm\">");

        final Position position = turnState.getPosition();

        // we're going to do this the slow, but simple way
        for (Power allPower : allPowers) {
            // create a sorted list of owned supply centers for this power.
            final Province[] ownedSCs = position.getOwnedSupplyCenters(allPower)
                    .toArray(new Province[0]);
            Arrays.sort(ownedSCs);

            // print the power name
            sb.append("<b>");
            sb.append(allPower);
            sb.append(":</b> ");

            // print out the provinces
            if (ownedSCs.length > 0) {
                for (Province ownedSC : ownedSCs) {
                    sb.append(ownedSC.getFullName());
                    sb.append(", ");
                }

                // delete the last ", "
                sb.delete(sb.length() - 2, sb.length());

                sb.append("  ");
                sb.append(Utils.getLocalString(SC_NUM,
                        new Integer(ownedSCs.length)));
            } else {
                sb.append(Utils.getLocalString(MSG_NONE));
            }

            sb.append('.');
            sb.append("<br>\n");
        }

        sb.append("</div>");

        return sb.toString();
    }// getSCInfo()


    /**
     * Write dislodged unit information.
     */
    private String getDislodgedInfo() {
        // write dislodged units / powers. if none are
        // dislodged, indicate. Not super-efficient
        // ordered by powers (like SC ownership)
        boolean anyDislodged = false;
        final Position position = turnState.getPosition();
        final StringBuffer sb = new StringBuffer(1024);
        sb.append("<div class=\"indent1cm\">");

        for (Power allPower : allPowers) {
            final Province[] dislodged = position
                    .getDislodgedUnitProvinces(allPower)
                    .toArray(new Province[0]);
            if (dislodged.length > 0) {
                anyDislodged = true;

                // print power name
                sb.append("<b>");
                sb.append(allPower);
                sb.append(":</b> ");


                // print unit information, for each unit.
                // comma-separate.
                for (int z = 0; z < dislodged.length - 1; z++) {
                    final Unit unit = position.getDislodgedUnit(dislodged[z])
                            .orElse(null);

                    sb.append(' ');
                    sb.append(unit.getType().getFullName());
                    sb.append(' ');
                    sb.append(dislodged[z].getFullName());
                    sb.append(',');
                }

                // print last (no comma afterwards)
                final Unit unit = position
                        .getDislodgedUnit(dislodged[dislodged.length - 1])
                        .orElse(null);
                sb.append(' ');
                sb.append(unit.getType().getFullName());
                sb.append(' ');
                sb.append(dislodged[dislodged.length - 1].getFullName());

                // finish the line.
                sb.append('.');
                sb.append("<br>\n");
            }
        }

        if (!anyDislodged) {
            sb.append(Utils.getLocalString(NO_DISLODGED_UNITS));
        }

        sb.append("</div>");

        return sb.toString();
    }// getDislodgedInfo()


    /**
     * Write adjustment information.
     */
    private String getAdjustmentInfo() {
        final StringBuffer sb = new StringBuffer(1024);
        sb.append("<div class=\"indent1cm\">");

        // format using format string
        // many args...
        for (Power allPower : allPowers) {
            final AdjustmentInfo adjInfo = adjMap.get(allPower);


            // determine build/remove/nochange text, and blocked builds
            String adjustmentText;            // never null after below
            String blockedBuildMessage = "";        // empty if no builds are blocked

            final int adjAmount = adjInfo.getAdjustmentAmount();
            if (adjAmount > 0) {
                adjustmentText = Utils.getLocalString(ADJ_BUILD_TEXT,
                        new Integer(Math.abs(adjAmount)));
            } else if (adjAmount < 0) {
                adjustmentText = Utils.getLocalString(ADJ_REMOVE_TEXT,
                        new Integer(Math.abs(adjAmount)));
            } else {
                adjustmentText = Utils.getLocalString(ADJ_NOCHANGE_TEXT);
            }

            // blocked builds?
            final int shouldBuild = adjInfo.getSupplyCenterCount() - adjInfo
                    .getUnitCount();
            if (adjAmount >= 0 && shouldBuild > adjAmount) {
                blockedBuildMessage = Utils
                        .getLocalString(ADJ_BLOCKED_BUILD_TEXT,
                                new Integer(shouldBuild - adjAmount));
            }

            final Object[] args = new Object[]{allPower,            // {0} : Power
                    new Integer(
                            adjInfo.getSupplyCenterCount()),    // {1} : # SC (including home SC) controlled
                    new Integer(
                            adjInfo.getUnitCount()),            // {2} : # units controlled
                    adjustmentText,            // {3} : build or remove (or no change) message
                    blockedBuildMessage,    // {4} : misc text (blocked builds), or empty
            };

            sb.append(Utils.getLocalString(ADJ_BASIC_TEXT, args));
            sb.append("<br>\n");
        }

        sb.append("</div>");
        return sb.toString();
    }// getAdjustmentInfo()


    /**
     * Returns a Map of Power=>(List of Unit location names)
     * Dislodged units are underlined. Abbreviations
     * for province names are always used.
     */
    private Map<Power, LinkedList<String>> getUnitsByPower() {
        final Map<Power, LinkedList<String>> pmap = new HashMap<>();
        for (Power allPower1 : allPowers) {
            pmap.put(allPower1, new LinkedList<>());
        }

        final Position position = turnState.getPosition();
        final Province[] provinces = position.getProvinces().toArray(new Province[0]);
        for (final Province province : provinces) {
            if (position.hasUnit(province)) {
                final Unit unit = position.getUnit(province).orElse(null);
                final List<String> uList = pmap.get(unit.getPower());
                String sb = unit.getType().getShortName() +
                        ' ' +
                        province.getShortName();
                uList.add(sb);
            }

            if (position.hasDislodgedUnit(province)) {
                // dislodged units are underlined
                final Unit unit = position.getDislodgedUnit(province)
                        .orElse(null);
                final List<String> uList = pmap.get(unit.getPower());
                String sb = "<u>" +
                        unit.getType().getShortName() +
                        "</u> <u>" +
                        province.getShortName() +
                        "</u>";
                uList.add(sb);
            }

        }

        // sort the lists.
        for (Power allPower : allPowers) {
            final List<String> list = pmap.get(allPower);
            Collections.sort(list);
        }

        return pmap;
    }// getUnitsByPower()


}// class StateWriter()

