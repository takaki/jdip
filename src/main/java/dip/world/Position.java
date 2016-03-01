//
//  @(#)Position.java		2/2003
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

import dip.world.Unit.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores all the mutable (state) information for a given TurnState.
 * Immutable data is retained in the Power/Province/etc. objects; only
 * mutable data is stored here.
 * <p>
 * This object can be cloned, and should be cloned, when creating a new
 * Position based upon previous Position data. Several clone methods are
 * available, each optimized for speed and cloning requirements.
 * <p>
 * WARNING: this code is not MT (Multithread) safe!
 * <p>
 * This class is heavily optimized, as adjudicator performance is highly dependent
 * upon the performance of this class.
 * <p>
 * The clone() methods are not strictly implemented; they call a constructor
 * to assist in cloning rather than call super.clone(). This is done for
 * performance reasons.
 */
public final class Position implements Serializable, Cloneable {
    // size constants; these should be prime
    private static final int POWER_SIZE = 17;

    // instance variables
    private final Map<Power, PowerData> powerMap = new HashMap<>(POWER_SIZE);
    private final List<ProvinceData> provArray;
    private final dip.world.Map map;
    private transient Province[] tmpProvArray;


    public Position(final dip.world.Map map) {
        this.map = map;
        provArray = new ArrayList<>(
                Arrays.asList(new ProvinceData[map.getProvinces().length]));
    }// Position()


    /**
     * The Number of Provinces in this Position
     */
    public int size() {
        return provArray.size();
    }// size()


    /**
     * Convenience method: Returns an array of Provinces
     */
    public Province[] getProvinces() {
        return map.getProvinces();
    }// getProvinces()


    /**
     * Returns true if this Power has been eliminated. False by default.
     */
    public boolean isEliminated(final Power power) {
        final PowerData pd = powerMap.get(power);
        if (pd != null) {
            return pd.isEliminated();
        }
        return false;
    }// isEliminated()


    /**
     * Set whether this Power has been eliminated.
     */
    public void setEliminated(final Power power, final boolean value) {
        final PowerData pd = getPowerData(power);
        pd.setEliminated(value);
    }// setEliminated()


    /**
     * Scans the Position; sets/unsets elimination depending upon if a given
     * Power has any units (including dislodged units) or supply centers on the map
     */
    public void setEliminationStatus(final Power[] powers) {
        final HashMap<Power, Object> pmap = new HashMap<>(19);
        for (final Power power1 : powers) {
            pmap.put(power1, null);
        }

        for (final ProvinceData pd : provArray) {
            Power power = null;

            if (pd != null) {
                Unit unit = pd.getUnit();
                if (unit != null)        // first check non-dislodged units
                {
                    power = unit.getPower();
                    if (pmap.get(power) == null) {
                        pmap.put(power, new Object());
                    }
                }

                // then see if there's a dislodged unit
                unit = pd.getDislodgedUnit();
                if (unit != null) {
                    power = unit.getPower();
                    if (pmap.get(power) == null) {
                        pmap.put(power, new Object());
                    }
                }

                // finally, see if we own a supply center
                power = pd.getSCOwner();
                if (power != null) {
                    if (pmap.get(power) == null) {
                        pmap.put(power, new Object());
                    }
                }
            }
        }

        for (final Power power : powers) {
            if (pmap.get(power) == null) {
                setEliminated(power, true);
            } else {
                setEliminated(power, false);
            }
        }
    }// setEliminationStatus()


    /**
     * Set the owner of the supply center.
     */
    public void setSupplyCenterOwner(final Province province,
                                     final Power power) {
        final ProvinceData pd = getProvinceData(province);
        pd.setSCOwner(power);
    }// setSupplyCenterOwner()


    /**
     * Set the owner of a home supply center.
     */
    public void setSupplyCenterHomePower(final Province province,
                                         final Power power) {
        final ProvinceData pd = getProvinceData(province);
        pd.setSCHomePower(power);
    }// setSupplyCenterHomePower()


    /**
     * Determine if this Province contains a supply center
     */
    public boolean hasSupplyCenterOwner(final Province province) {
        final ProvinceData pd = provArray.get(province.getIndex());
        if (pd != null) {
            return pd.isSCOwned();
        }
        return false;
    }// hasSupplyCenterOwner()


    /**
     * Determine if this Province contains a Home supply center
     */
    public boolean isSupplyCenterAHome(final Province province) {
        final ProvinceData pd = provArray.get(province.getIndex());
        if (pd != null) {
            return pd.isSCAHome();
        }
        return false;
    }// isSupplyCenterAHome()


    /**
     * Get the home power of the supply center; null if no supply center or home power
     */
    public Optional<Power> getSupplyCenterHomePower(final Province province) {
        final ProvinceData pd = provArray.get(province.getIndex());
        if (pd != null) {
            return Optional.ofNullable(pd.getSCHomePower());
        }
        return Optional.empty();
    }// getSupplyCenterHomePower()


    /**
     * Get the owner of the supply center; null if no owner or no supply center.
     */
    public Optional<Power> getSupplyCenterOwner(final Province province) {
        final ProvinceData pd = provArray.get(province.getIndex());
        if (pd != null) {
            return Optional.ofNullable(pd.getSCOwner());
        }
        return Optional.empty();
    }// getSupplyCenterOwner()


    // non-dislodged unit

    /**
     * Set the unit contained in this province; null to eliminate an existing unit.
     */
    public void setUnit(final Province province, final Unit unit) {
        final ProvinceData pd = getProvinceData(province);
        pd.setUnit(unit);
    }// setUnit()

    /**
     * Determines if there is a unit present in this province.
     */
    public boolean hasUnit(final Province province) {
        final ProvinceData pd = provArray.get(province.getIndex());
        if (pd != null) {
            return pd.hasUnit();
        }
        return false;
    }// hasUnit()

    /**
     * Get the unit contained in this Province. Returns null if no unit exists.
     */
    public Optional<Unit> getUnit(final Province province) {
        final ProvinceData pd = provArray.get(province.getIndex());
        if (pd != null) {
            return Optional.ofNullable(pd.getUnit());
        }
        return Optional.empty();
    }// getUnit()


    /**
     * Test if the given type of unit is contained in this Province.
     */
    public boolean hasUnit(final Province province, final Type unitType) {
        final Unit unit = getUnit(province).orElse(null);
        if (unit != null) {
            return unit.getType().equals(unitType);
        }
        return false;
    }// hasUnit()

    /**
     * Test if the given type of unit is contained in this Province.
     */
    public boolean hasDislodgedUnit(final Province province,
                                    final Type unitType) {
        final Unit unit = getDislodgedUnit(province).orElse(null);
        if (unit != null) {
            return unit.getType().equals(unitType);
        }
        return false;
    }// hasDislodgedUnit()


    // dislodged unit

    /**
     * Set the dislodged unit contained in this province; null to eliminate an existing unit.
     */
    public void setDislodgedUnit(final Province province, final Unit unit) {
        final ProvinceData pd = getProvinceData(province);
        pd.setDislodgedUnit(unit);
    }// setDislodgedUnit()


    /**
     * Get the dislodged unit in this Province. Returns null if no dislodged unit exists.
     */
    public Optional<Unit> getDislodgedUnit(final Province province) {
        final ProvinceData pd = provArray.get(province.getIndex());
        if (pd != null) {
            return Optional.ofNullable(pd.getDislodgedUnit());
        }
        return Optional.empty();
    }// getDislodgedUnit()


    // last occupier

    /**
     * Sets the Power that last occupied a given space. Note that this
     * is not intended to be used for Supply Center ownership (which only
     * changes in the Fall season); use setSupplyCenterOwner() instead.
     */
    public void setLastOccupier(final Province province, final Power power) {
        final ProvinceData pd = getProvinceData(province);
        pd.setLastOccupier(power);
    }// setLastOccupier()


    /**
     * Returns the Power that last occupied a given space. Note that this
     * is not intended to be used for Supply Center ownership (which only
     * changes in the Fall season); use getSupplyCenterOwner() instead.
     */
    public Optional<Power> getLastOccupier(final Province province) {
        final ProvinceData pd = provArray.get(province.getIndex());
        if (pd != null) {
            return Optional.ofNullable(pd.getLastOccupier());
        }
        return Optional.empty();
    }// getLastOccupier()


    /**
     * Determines if there is a dislodged unit present in this province.
     */
    public boolean hasDislodgedUnit(final Province province) {
        final ProvinceData pd = provArray.get(province.getIndex());
        if (pd != null) {
            return pd.hasDislodgedUnit();
        }
        return false;
    }// hasDislodgedUnit()


    /**
     * Returns an array of provinces with non-dislodged units
     */
    public Province[] getUnitProvinces() {
        makeTmpProvArray();

        int arrSize = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null && pd.hasUnit()) {
                tmpProvArray[arrSize] = map.reverseIndex(i);
                arrSize++;
            }
        }

        final Province[] p = new Province[arrSize];
        System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
        return p;
    }// getUnitProvinces()


    /**
     * Returns an array of provinces with dislodged units
     */
    public Province[] getDislodgedUnitProvinces() {
        makeTmpProvArray();

        int arrSize = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null && pd.hasDislodgedUnit()) {
                tmpProvArray[arrSize] = map.reverseIndex(i);
                arrSize++;
            }
        }

        final Province[] p = new Province[arrSize];
        System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
        return p;
    }// getDislodgedUnitProvinces()


    /**
     * Returns the number of provinces with non-dislodged units
     */
    public int getUnitCount() {
        int count = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null && pd.hasUnit()) {
                count++;
            }
        }

        return count;
    }// getUnitCount()


    /**
     * Returns the number of provinces with dislodged units
     */
    public int getDislodgedUnitCount() {
        int count = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null && pd.hasDislodgedUnit()) {
                count++;
            }
        }

        return count;
    }// getDislodgedUnitCount()


    /**
     * Returns an array of provinces with home supply centers
     */
    public Province[] getHomeSupplyCenters() {
        makeTmpProvArray();

        int arrSize = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null && pd.isSCAHome()) {
                tmpProvArray[arrSize] = map.reverseIndex(i);
                arrSize++;
            }
        }

        final Province[] p = new Province[arrSize];
        System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
        return p;
    }// getHomeSupplyCenters()


    /**
     * Returns an Array of the Home Supply Centers for a given power (whether or not they are owned by that power)
     */
    public Province[] getHomeSupplyCenters(final Power power) {
        makeTmpProvArray();

        int arrSize = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null && pd.getSCHomePower() == power) {
                tmpProvArray[arrSize] = map.reverseIndex(i);
                arrSize++;
            }
        }

        final Province[] p = new Province[arrSize];
        System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
        return p;
    }// getHomeSupplyCenters()


    /**
     * Determines if a Power has at least one owned Home Supply Center.
     * <p>
     * An owned home supply center need not have a unit present.
     */
    public boolean hasAnOwnedHomeSC(final Power power) {
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null && pd.getSCHomePower() == power && pd
                    .getSCOwner() == power) {
                return true;
            }
        }

        return false;
    }// hasAnOwnedHomeSC()


    /**
     * Returns an Array of the owned Supply Centers for a given Power (whether or not they are home supply centers)
     */
    public Province[] getOwnedSupplyCenters(final Power power) {
        makeTmpProvArray();

        int arrSize = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null && pd.getSCOwner() == power) {
                tmpProvArray[arrSize] = map.reverseIndex(i);
                arrSize++;
            }
        }

        final Province[] p = new Province[arrSize];
        System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
        return p;
    }// getOwnedSupplyCenters()


    /**
     * Returns an array of provinces with owned supply centers
     */
    public Province[] getOwnedSupplyCenters() {
        makeTmpProvArray();

        int arrSize = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null && pd.isSCOwned()) {
                tmpProvArray[arrSize] = map.reverseIndex(i);
                arrSize++;
            }
        }

        final Province[] p = new Province[arrSize];
        System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
        return p;
    }// getOwnedSupplyCenters()


    /**
     * Deep clone of the contents of this Position.
     */
    @Override
    public Object clone() {
        final Position pos = new Position(map);

        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);

            if (pd != null) {
                pos.provArray.set(i, pd.normClone());
            }
        }

        final Iterator<Power> iter = powerMap.keySet().iterator();
        while (iter.hasNext()) {
            final Power key = iter.next();
            final PowerData pd = powerMap.get(key);

            pos.powerMap.put(key, pd.normClone());
        }

        return pos;
    }// clone()

    /**
     * Deep clone of everything *except* dislodged & non-dislodged units;
     * (e.g., SC ownership, Power Info, etc.)
     */
    public Position cloneExceptUnits() {
        final Position pos = new Position(map);

        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);

            if (pd != null) {
                pos.provArray.set(i, pd.cloneExceptUnits());
            }
        }

        final Iterator<Power> iter = powerMap.keySet().iterator();
        while (iter.hasNext()) {
            final Power key = iter.next();
            final PowerData pd = powerMap.get(key);

            pos.powerMap.put(key, pd.normClone());
        }

        return pos;
    }// cloneExceptUnits()


    /**
     * Deep clone of everything <b>except</b> dislodged units.
     */
    public Position cloneExceptDislodged() {
        final Position pos = new Position(map);

        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null) {
                pos.provArray.set(i, pd.cloneExceptDislodged());
            }
        }

        final Iterator<Power> iter = powerMap.keySet().iterator();
        while (iter.hasNext()) {
            final Power key = iter.next();
            final PowerData pd = powerMap.get(key);

            pos.powerMap.put(key, pd.normClone());
        }

        return pos;
    }// cloneExceptDislodged()


    /**
     * Gets all the Provinces with non-dislodged
     * Units for a particular power.
     */
    public Province[] getUnitProvinces(final Power power) {
        makeTmpProvArray();

        int arrSize = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null) {
                final Unit unit = pd.getUnit();
                if (unit != null && unit.getPower() == power) {
                    tmpProvArray[arrSize] = map.reverseIndex(i);
                    arrSize++;
                }
            }
        }

        final Province[] p = new Province[arrSize];
        System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
        return p;
    }// getUnitProvinces()


    /**
     * Gets all the Provinces with dislodged
     * Units for a particular power.
     */
    public Province[] getDislodgedUnitProvinces(final Power power) {
        makeTmpProvArray();

        int arrSize = 0;
        for (int i = 0; i < provArray.size(); i++) {
            final ProvinceData pd = provArray.get(i);
            if (pd != null) {
                final Unit unit = pd.getDislodgedUnit();
                if (unit != null && unit.getPower() == power) {
                    tmpProvArray[arrSize] = map.reverseIndex(i);
                    arrSize++;
                }
            }
        }

        final Province[] p = new Province[arrSize];
        System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
        return p;
    }// getDislodgedUnitProvinces()


    /**
     * Call this method FIRST before any set(); thus if ProvinceData
     * does not exist, we will add one to the map.
     */
    private ProvinceData getProvinceData(final Province province) {
        final int idx = province.getIndex();
        ProvinceData pd = provArray.get(idx);
        if (pd == null) {
            pd = new ProvinceData();
            provArray.set(idx, pd);
        }

        return pd;
    }// getProvinceData()

    /**
     * Same type of functionality as getProvinceData() but for PowerData objects
     */
    private PowerData getPowerData(final Power power) {
        PowerData pd = powerMap.get(power);
        if (pd == null) {
            pd = new PowerData();
            powerMap.put(power, pd);
        }
        return pd;
    }// getPowerData()


    /**
     * All mutable Province data is kept here
     */
    private final class ProvinceData implements Serializable {
        // instance variables
        private Unit unit;
        private Unit dislodgedUnit;
        private Power SCOwner;
        private Power SCHomePower;
        private Power lastOccupier;

        // unit set/get
        public boolean hasUnit() {
            return unit != null;
        }

        public boolean hasDislodgedUnit() {
            return dislodgedUnit != null;
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


        // SC set/get
        public boolean isSCAHome() {
            return SCHomePower != null;
        }

        public boolean isSCOwned() {
            return SCOwner != null;
        }

        public void setSCOwner(final Power p) {
            SCOwner = p;
        }

        public void setSCHomePower(final Power p) {
            SCHomePower = p;
        }

        public Power getSCOwner() {
            return SCOwner;
        }

        public Power getSCHomePower() {
            return SCHomePower;
        }

        // occupier set/get
        public Power getLastOccupier() {
            return lastOccupier;
        }

        public void setLastOccupier(final Power p) {
            lastOccupier = p;
        }


        // normal clone
        public ProvinceData normClone() {
            final ProvinceData pd = new ProvinceData();

            // deep copy unit information
            if (unit != null) {
                pd.unit = unit.clone();
            }

            if (dislodgedUnit != null) {
                pd.dislodgedUnit = dislodgedUnit.clone();
            }

            // shallow copy Powers [Power is immutable]
            pd.SCOwner = SCOwner;
            pd.SCHomePower = SCHomePower;
            pd.lastOccupier = lastOccupier;

            return pd;
        }// normClone()


        /**
         * Returns null if no non-ownership information exists
         */
        public ProvinceData cloneExceptUnits() {
            // don't create an object if there is no ownership info.
            // this also compacts the Position map!
            if (SCOwner == null && SCHomePower == null && lastOccupier == null) {
                return null;
            }

            // create a ProvinceData object
            final ProvinceData pd = new ProvinceData();

            // shallow copy Power [Power is immutable]
            pd.SCOwner = SCOwner;
            pd.SCHomePower = SCHomePower;
            pd.lastOccupier = lastOccupier;

            return pd;
        }// cloneExceptUnits()

        /**
         * Returns null if no ownership info/unit exists
         */
        public ProvinceData cloneExceptDislodged() {
            // don't create an object if there is no ownership info.
            // this also compacts the Position map!
            if (SCOwner == null && SCHomePower == null && unit == null && lastOccupier == null) {
                return null;
            }

            // create a ProvinceData object
            final ProvinceData pd = new ProvinceData();

            // shallow copy Power [Power is immutable]
            pd.SCOwner = SCOwner;
            pd.SCHomePower = SCHomePower;
            pd.lastOccupier = lastOccupier;

            // deep copy unit
            if (unit != null) {
                pd.unit = unit.clone();
            }

            return pd;
        }// cloneExceptUnits()


    }// inner class ProvinceData


    /**
     * All mutable Power data is kept here
     */
    private final class PowerData implements Serializable {
        // instance variables
        private boolean isEliminated;


        public PowerData() {
        }// PowerData()

        public boolean isEliminated() {
            return isEliminated;
        }

        public void setEliminated(final boolean value) {
            isEliminated = value;
        }

        public PowerData normClone() {
            final PowerData pd = new PowerData();
            pd.isEliminated = isEliminated;
            return pd;
        }// normClone()
    }// inner class PowerData


    /**
     * Copying references into a large temporary array, then creating an array
     * and copying the temp array into the correctly-sized array via
     * System.arraycopy() is about twice as fast as using an ArrayList, and
     * almost twice as fast as double-iterating (one to gauge the array size,
     * and one to copy data).
     * <p>
     * This method creates a sufficiently large array to hold temporary data.
     */
    private void makeTmpProvArray() {
        if (tmpProvArray == null) {
            tmpProvArray = new Province[provArray.size()];
        }
    }// makeTmpProvArray()
}// class Position

