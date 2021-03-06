//	
//	@(#)DislodgedResult.java	5/2003
//	
//	Copyright 2003 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order.result;

import dip.misc.Utils;
import dip.order.OrderFormat;
import dip.order.OrderFormatOptions;
import dip.order.Orderable;
import dip.world.Location;
import dip.world.Province;

import java.util.Objects;


/**
 * Similar to an OrderResult, but allows the <b>optional</b> specification of:
 * <ul>
 * <li>valid Locations to which a unit may retreat (if any)</li>
 * <li>the unit that dislodged this unit</li>
 * <li>the attack and defense strengths</li>
 * </ul>
 */
public class DislodgedResult extends OrderResult {
    // instance fields
    private Location[] retreatLocations;
    private Province dislodger;
    private int atkStrength = -1;
    private int defStrength = -1;


    public DislodgedResult(final Orderable order,
                           final Location[] retreatLocations) {
        this(order, null, retreatLocations);
    }// DislodgedResult()


    public DislodgedResult(final Orderable order, final String message,
                           final Location[] retreatLocations) {
        Objects.requireNonNull(order);

        power = order.getPower();
        this.message = message;
        this.order = order;
        resultType = ResultType.DISLODGED;
        this.retreatLocations = retreatLocations;
    }// DislodgedResult()


    /**
     * Returns the valid retreat locations, if set. If no retreat
     * locations have been defined, this will return null.
     */
    public Location[] getRetreatLocations() {
        return retreatLocations;
    }// getRetreatLocations()

    /**
     * Returns the attack strength, or -1 if it has not
     * been set.
     */
    public int getAttackStrength() {
        return atkStrength;
    }// getAttackStrength()


    /**
     * Returns the defense strength, or -1 if it has not
     * been set.
     */
    public int getDefenseStrength() {
        return defStrength;
    }// getDefenseStrength()


    /**
     * Returns the dislodging units location, or null if it has not
     * been set.
     */
    public Province getDislodger() {
        return dislodger;
    }// getDislodger()


    /**
     * Set the attack strength. A value of -1 indicates
     * that this has not been set.
     */
    public void setAttackStrength(final int value) {
        if (value < -1) {
            throw new IllegalArgumentException();
        }

        atkStrength = value;
    }// setAttackStrength()


    /**
     * Set the defense strength. A value of -1 indicates
     * that this has not been set.
     */
    public void setDefenseStrength(final int value) {
        if (value < -1) {
            throw new IllegalArgumentException();
        }

        defStrength = value;
    }// setDefenseStrength()


    /**
     * Set the dislodger. A value of <code>null</code>
     * indicates that this has not been set.
     */
    public void setDislodger(final Province value) {
        dislodger = value;
    }// setDislodger()


    /**
     * Creates an appropriate internationalized text message given the
     * set and unset parameters.
     */
    @Override
    public String getMessage(final OrderFormatOptions ofo) {
        /*
        0 : province not specified
		1 : province specified
		
		{0} : dislodge province yes/no (1/0)
		{1} : dislodge province
		{2} : atk
		{3} : def
		{4} : retreats number (-1, 0, or >0)
		{5} : retreats (comma-separated)
		*/

        // create formated dislodged present (if any)
        String fmtDislodger = null;
        if (dislodger != null) {
            fmtDislodger = OrderFormat.format(ofo, dislodger);
        }

        // create retreat list
        final StringBuffer retreats = new StringBuffer(128);
        if (retreatLocations != null) {
            for (int i = 0; i < retreatLocations.length; i++) {
                retreats.append(' ');

                retreats.append(OrderFormat.format(ofo, retreatLocations[i]));

                if (i < retreatLocations.length - 1) {
                    retreats.append(',');
                }
            }
        }

        // create messageformat arguments
        final Object[] args = {dislodger == null ? 0 : 1,    // {0}; 0 if no province specified
                fmtDislodger,                                                // {1}
                atkStrength,                                    // {2}
                defStrength,                                    // {3}
                retreatLocations == null ? -1 : retreatLocations.length,  // {4}
                retreats.toString() // {5}
        };

        // return formatted message
        return Utils.getLocalString("DislodgedResult.message", args);
    }// getMessage()


    /**
     * Primarily for debugging.
     */
    public String toString() {
        final StringBuffer sb = new StringBuffer(256);
        sb.append(super.toString());

        // add retreats
        sb.append(" retreat locations:");
        if (retreatLocations == null) {
            sb.append(" null");
        } else if (retreatLocations.length == 0) {
            sb.append(" none");
        } else {
            for (final Location retreatLocation : retreatLocations) {
                sb.append(' ');
                sb.append(retreatLocation.getBrief());
            }
        }

        // add dislodged info
        sb.append(". Dislodged from ");
        sb.append(dislodger);
        sb.append(' ');
        sb.append(atkStrength);
        sb.append(':');
        sb.append(defStrength);
        sb.append('.');

        return sb.toString();
    }// toString()


}// class DislodgedResult
