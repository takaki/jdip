//
//  @(#)SupplyCenter.java	1.00	7/2002
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="SUPPLYCENTER")
public class SupplyCenter {

    @XmlAttribute(name = "province")
    private String province;
    @XmlAttribute(name = "homepower")
    private String powerName;
    @XmlAttribute(name = "owner")
    private String owner;

    /**
     * Get name of the home supply center; if "none" if none, "any" if any.
     */
    public String getHomePowerName() {
        return powerName;
    }

    /**
     * Sets the name of the home supply center.
     */
    public void setHomePowerName(final String value) {
        powerName = value;
    }


    /**
     * Get the province name of this supply center.
     */
    public String getProvinceName() {
        return province;
    }

    /**
     * Set the province name of this supply center.
     */
    public void setProvinceName(final String value) {
        province = value;
    }


    /**
     * Get the name of the Power that owns this supply center.
     */
    public String getOwnerName() {
        return owner;
    }

    /**
     * Set the name of the Power that owns this supply center.
     * <p>
     * "none" is acceptable, but "any" is not.
     */
    public void setOwnerName(final String value) {
        if ("any".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException();
        }

        owner = value;
    }// setOwnerName()

    /**
     * For debugging only!
     */
    @Override
    public String toString() {
        return String.join("", getClass().getName(), "[", "provinceName=",
                province, ",powerName=", powerName, ",ownerName=",
                owner, "]");
    }// toString()
}// nested class SupplyCenter

