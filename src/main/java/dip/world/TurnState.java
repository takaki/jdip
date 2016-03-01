//
//  @(#)TurnState.java		4/2002
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
package dip.world;

import dip.order.Orderable;
import dip.order.result.OrderResult;
import dip.order.result.OrderResult.ResultType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * A TurnState represents a snapshot of the game for the given Phase.
 * The essential components of a TurnState are, then:
 * <ul>
 * <li>A Phase object (represents the TurnState in time)
 * <li>A Position object (stores the current unit and powers state)
 * <li>Orders for the units
 * <li>Order Results (if the TurnState has been resolved)
 * </ul>
 * <p>
 * Note that we store Map (Province) adjacency data seperately from
 * the Map (it's in the World object), so Map objects are re-constituted
 * from a list of Provinces and Powers. This occurs behind-the-scenes when
 * a World (or TurnState) object is deserialized.
 * <p>
 * This object is NOT SYNCHRONIZED and therefore not inherently threadsafe.
 * <p>
 * Also note that when a List of orders is obtained for a power, we do not
 * check that the list contains only orders for that power. (e.g., are
 * non-power orders 'snuck in' for a given power)
 */
public class TurnState implements Serializable {
    // instance variables (we serialize all of this)
    private Phase phase;
    private List resultList;                // order results, post-adjudication
    private Map<Power, List> orderMap;                // Map of power=>orders
    private boolean isSCOwnerChanged;        // 'true' if any supply centers changed ownership
    private Position position;                // Position data (majority of game state)
    private transient World world;                // makes it easier when we just pass a turnstate
    private boolean isEnded;                // true if game over (won, draw, etc.)
    private boolean isResolved;                // true if phase has been adjudicated
    private transient Map<Orderable, Boolean> resultMap;        // transient result map


    /**
     * Creates a TurnState object.
     */
    protected TurnState() {
    }// TurnState()


    /**
     * Creates a TurnState object.
     */
    public TurnState(final Phase phase) {
        Objects.requireNonNull(phase);
        this.phase = phase;
        resultList = new ArrayList(80);
        orderMap = new HashMap<>(29);
    }// TurnState()

    /**
     * Set the World object associated with this TurnState.
     * A <code>null</code> World is not permitted.
     */
    public void setWorld(final World world) {
        Objects.requireNonNull(world);
        this.world = world;
    }// setWorld()


    /**
     * Gets the World object associated with this TurnState. Never should be null.
     */
    public World getWorld() {
        return world;
    }// getWorld()

    /**
     * Returns the current Phase
     */
    public Phase getPhase() {
        return phase;
    }// getTurnInfo()


    /**
     * This should be used with the utmost care. Null Phases are not allowed.
     */
    public void setPhase(final Phase phase) {
        Objects.requireNonNull(phase);
        this.phase = phase;
    }// setPhase()


    /**
     * Gets the Position data for this TurnState
     */
    public Position getPosition() {
        return position;
    }// getPosition()


    /**
     * Sets the Position data for this TurnState
     */
    public void setPosition(final Position position) {
        Objects.requireNonNull(position);
        this.position = position;
    }// setPosition()


    /**
     * Returns the result list
     */
    public List getResultList() {
        return resultList;
    }// getResultList()


    /**
     * Sets the Result list, erasing any previously existing result list.
     */
    public void setResultList(final List list) {
        Objects.requireNonNull(list);
        resultList = new ArrayList<>(list);
    }// setResultList()


    /**
     * A flag indicating if, after adjudication, any supply centers
     * have changed ownership.
     */
    public boolean getSCOwnerChanged() {
        return isSCOwnerChanged;
    }// getSCOwnerChanged()


    /**
     * Sets the flag indicating if, after adjudication, any supply centers
     * have changed ownership.
     */
    public void setSCOwnerChanged(final boolean value) {
        isSCOwnerChanged = value;
    }// setSCOwnerChanged()


    /**
     * Returns a List of orders for all powers.
     * <p>
     * Manipulations to this list will not be reflected in the TurnState object.
     */
    public List<Object> getAllOrders() {
        final List<Object> list = new ArrayList<>(75);

        final Iterator<Entry<Power, List>> esIter = orderMap.entrySet()
                .iterator();
        while (esIter.hasNext()) {
            final Entry<Power, List> mapEntry = esIter.next();
            final List orders = mapEntry.getValue();

            final Iterator ordIter = orders.iterator();
            while (ordIter.hasNext()) {
                list.add(ordIter.next());
            }
        }

        return list;
    }// getOrderList()


    /**
     * Clear all Orders, for all Powers.
     */
    public void clearAllOrders() {
        orderMap.clear();
    }// clearAllOrders()


    /**
     * Returns the List of orders for a given Power.
     * <p>
     * Note that modifications to the returned order List will be reflected
     * in the TurnState.
     */
    public List getOrders(final Power power) {
        Objects.requireNonNull(power);

        List orderList = orderMap.get(power);
        if (orderList == null) {
            orderList = new ArrayList(15);
            orderMap.put(power, orderList);
        }

        return orderList;
    }// getOrders()

    /**
     * Sets the orders for the given Power, deleting any existing orders for the power
     */
    public void setOrders(final Power power, final List list) {
        Objects.requireNonNull(power);
        Objects.requireNonNull(list);

        orderMap.put(power, list);
    }// setOrders()

    /**
     * Set if game has ended for any reason
     */
    public void setEnded(final boolean value) {
        isEnded = value;
    }

    /**
     * Returns <code>true</code> if game has ended
     */
    public boolean isEnded() {
        return isEnded;
    }

    /**
     * Set if the turn has been adjudicated.
     */
    public void setResolved(final boolean value) {
        isResolved = value;
    }

    /**
     * Returns the turn has been adjudicated
     */
    public boolean isResolved() {
        return isResolved;
    }

    /**
     * Returns if an order has failed, based on results. Note that
     * this only applies once the turnstate has been resolved. If
     * the TurnState is not resolved, this will always return true.
     */
    public boolean isOrderSuccessful(final Orderable o) {
        if (!isResolved) {
            return true;
        }

        if (resultMap == null) {
            resultMap = new HashMap<>(53);
            final Iterator iter = getResultList().iterator();
            while (iter.hasNext()) {
                final Object obj = iter.next();
                if (obj instanceof OrderResult) {
                    final OrderResult ordRes = (OrderResult) obj;

                    // we only map SUCCESSFULL orders.
                    if (ordRes.getResultType() == ResultType.SUCCESS) {
                        resultMap.put(ordRes.getOrder(), Boolean.TRUE);
                    }
                }
            }
        }

        return resultMap.get(o) == Boolean.TRUE;

    }// isFailedOrder()

}// class TurnState
