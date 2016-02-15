//
//  @(#)Unit.java		4/2002
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

import dip.misc.Utils;

import javax.xml.bind.annotation.XmlEnumValue;
import java.io.Serializable;
import java.util.Objects;


/**
 * A Unit is an object that has an owner (power), a coast location, and a Type
 * describing that unit.
 * <p>
 * Units are placed in Provinces.
 * <p>
 * <b>This object is not immutable!</b>
 */

public class Unit implements Serializable, Cloneable {
    // instance variables
    protected final Type type;
    protected final Power owner;
    protected Coast coast = Coast.UNDEFINED;


    /**
     * Creates a new Unit
     */
    public Unit(final Power power, final Type unitType) {
        if (power == null || unitType == null) {
            throw new IllegalArgumentException("null arguments not permitted");
        }

        if (unitType == Type.UNDEFINED) {
            throw new IllegalArgumentException(
                    "cannot create a unit with undefined type");
        }

        owner = power;
        type = unitType;
    }// Unit()


    /**
     * For Cloning: *NO* arguments are checked.
     */
    private Unit(final Power power, final Type unitType, final Coast coast) {
        owner = power;
        type = unitType;
        this.coast = coast;
    }// Unit()


    /**
     * Set the coast of a unit.
     */
    public void setCoast(final Coast coast) {
        if (coast == null) {
            throw new IllegalArgumentException("null coast");
        }

        this.coast = coast;
    }// setCoast()


    /**
     * Get the Coast where this Unit is located
     */
    public Coast getCoast() {
        return coast;
    }

    /**
     * Get the Power who controls this Unit
     */
    public Power getPower() {
        return owner;
    }

    /**
     * Get the Type of unit (e.g., Army or Fleet)
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns if two Units are equivalent.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Unit) {
            final Unit unit = (Unit) obj;
            return unit.type == type && unit.owner == owner && unit.coast == coast;
        }

        return false;
    }// equals()

    /**
     * Returns a Clone of the unit. Note that this is not a
     * strict implementation of clone(); a constructor is
     * invoked for performance reasons.
     */
    @Override
    public Unit clone() {
        return new Unit(owner, type, coast);
    }// clone()


    /**
     * Displays internal object values. For debugging use only!
     */
    @Override
    public String toString() {
        return String.join("", "Unit:[type=" +
                type +
                ",power=" +
                owner +
                ",coast=" +
                coast +
                "]");
    }// toString()

    @Override
    public int hashCode() {
        return Objects.hash(type, owner, coast);
    }


    /**
     * A Type is the class of unit, for example, Army or Fleet.
     * <p>
     * Type constans should be used; new Type objects should not be created
     * unless the game concepts are being extended.
     */
    public enum Type {
        /**
         * Constant representing an Army
         */
        @XmlEnumValue("army")
        ARMY("army"),
        /**
         * Constant representing a Fleet
         */
        @XmlEnumValue("fleet")
        FLEET("fleet"),
        /**
         * Constant representing a Wing
         */
        @XmlEnumValue("wing")
        WING("wing"),
        /**
         * Constant representing an unknown type
         */
        UNDEFINED("undefined");

        // internal i18n key constants
        private static final String UNIT_TYPE_PREFIX = "unit.type.";
        private static final String UNIT_TYPE_BRIEF_SUFFIX = ".brief";
        private static final String UNIT_TYPE_ARTICLE_SUFFIX = ".witharticle";

        // so, for an army (brief name), the key would be:
        // UNIT_TYPE_PREFIX + NAME_ARMY + UNIT_TYPE_BRIEF_SUFFIX

        private final transient String name;
        private final transient String shortName;
        private final transient String nameWithArticle;


        /**
         * Create a new Type
         */
        Type(final String internalName) {
            name = Utils.getLocalString(UNIT_TYPE_PREFIX + internalName);
            shortName = Utils.getLocalString(UNIT_TYPE_PREFIX +
                    internalName + UNIT_TYPE_BRIEF_SUFFIX);
            nameWithArticle = Utils.getLocalString(UNIT_TYPE_PREFIX +
                    internalName + UNIT_TYPE_ARTICLE_SUFFIX);
        }// Type()

        /**
         * Get the full name of this type (e.g., 'Army')
         */
        public String getFullName() {
            return name;
        }// getName()

        /**
         * Get the short name of this type (e.g., 'A')
         */
        public String getShortName() {
            return shortName;
        }// getShortName();

        /**
         * Get the short name
         */
        @Override
        public String toString() {
            return shortName;
        }// toString()

        /**
         * Get the full name, including an article
         */
        public String getFullNameWithArticle() {
            return nameWithArticle;
        }// getFullNameWithArticle()

		/*
            equals():
			
			We use Object.equals(), which just does a test of 
			referential equality. 
			
		*/


        /**
         * Returns a type constant corresponding to the input.
         * Case insensitive. This will parse localized names,
         * AS WELL AS the standard English names. So, for
         * English names (and all other languages):
         * <pre>
         * 		null -> Type.UNDEFINED
         * 		'f' or 'fleet' -> Type.FLEET
         * 		'a' or 'army' -> Type.ARMY
         * 		'w' or 'wing' -> Type.WING
         * 		any other -> null
         * 	</pre>
         */
        public static Type parse(final String text) {
            if (text == null) {
                return UNDEFINED;
            }

            final String input = text.toLowerCase().trim();
            if (Objects.equals(input, ARMY.shortName) || Objects
                    .equals(input, ARMY.name)) {
                return ARMY;
            }
            if (Objects.equals(input, FLEET.shortName) || Objects
                    .equals(input, ARMY.name)) {
                return FLEET;
            }
            if (Objects.equals(input, WING.shortName) || Objects
                    .equals(input, ARMY.name)) {
                return WING;
            }

            // test against standard English names after trying
            // localized names.
            //
            switch (input) {
                case "a":
                case "army":
                    return ARMY;
                case "f":
                case "fleet":
                    return FLEET;
                case "w":
                case "wing":
                    return WING;
                default:
                    break;
            }

            return null;
        }// parse()

    }// inner class Type


}// class Unit
