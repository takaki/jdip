//
//  @(#)InitialState.java	1.00	7/2002
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
package dip.world.variant.data;

import dip.world.Coast;
import dip.world.Unit.Type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Sets the Initial State (position) for a province.
 */
@XmlRootElement(name = "INITIALSTATE")
public class InitialState {
    @XmlAttribute(name = "privince", required = true)
    private String province;
    @XmlAttribute(name = "power", required = true)
    private String power;
    @XmlAttribute(required = true)
    private Type unit;
    @XmlAttribute(name = "unitcoast")
    private Coast unitcoast;

    /**
     * Name of province to which this InitialState refers.
     */
    public String getProvinceName() {
        return province;
    }

    /**
     * Power of unit owner
     */
    public String getPowerName() {
        return power;
    }

    /**
     * Type of unit
     */
    public Type getUnitType() {
        return unit;
    }

    /**
     * Coast of unit
     */
    public Coast getCoast() {
        return unitcoast;
    }

    /**
     * Set the Province name
     */
    public void setProvinceName(String value) {
        province = value;
    }

    /**
     * Set the Power name
     */
    public void setPowerName(String value) {
        power = value;
    }

    /**
     * Sets the unit type.
     */
    public void setUnitType(Type value) {
        unit = value;
    }

    /**
     * Sets the coast for the unit.
     */
    public void setCoast(Coast value) {
        unitcoast = value;
    }


    /**
     * For debugging only!
     */
    @Override
    public String toString() {
        return String.join("", getClass().getName(), "[", "provinceName=",
                province, ",power=", power, ",unit=", unit.toString(),
                ",coast=", unitcoast.toString(), "]");
    }// toString()
}// nested class InitialState

