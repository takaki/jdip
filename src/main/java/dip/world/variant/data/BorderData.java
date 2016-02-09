//
//  @(#)BorderData.java			11/2002
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

/**
 * Temporary holder for Border data after XML parsing.
 */
@XmlRootElement(name = "BORDER")
public final class BorderData {
    @XmlAttribute(name = "id")
    private String id;
    @XmlAttribute(name = "description")
    private String description;
    @XmlAttribute(name = "unitTypes")
    private String unitTypes;
    @XmlAttribute(name = "from")
    private String from;
    @XmlAttribute(name = "orderTypes")
    private String orderTypes;
    @XmlAttribute(name = "baseMoveModifier")
    private String baseMoveModifier;
    @XmlAttribute(name = "year")
    private String year;
    @XmlAttribute(name = "phase")
    private String phase;
    @XmlAttribute(name = "season")
    private String season;

    /**
     * Gets the Border ID
     */
    public String getID() {
        return id;
    }

    /**
     * Gets the Border description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the Border unit types
     */
    public String getUnitTypes() {
        return unitTypes;
    }

    /**
     * Gets the Border From locations
     */
    public String getFrom() {
        return from;
    }

    /**
     * Gets the Border Order types
     */
    public String getOrderTypes() {
        return orderTypes;
    }

    /**
     * Gets the Border Support modifier
     */
    public String getBaseMoveModifier() {
        return baseMoveModifier;
    }

    /**
     * Gets the Border Year
     */
    public String getYear() {
        return year;
    }

    /**
     * Gets the Border Phase
     */
    public String getPhase() {
        return phase;
    }

    /**
     * Gets the Border Season
     */
    public String getSeason() {
        return season;
    }


}// class BorderData
