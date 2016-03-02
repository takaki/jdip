//
//  @(#)RuleOptions.java		10/2002
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
import dip.world.variant.data.Variant;
import dip.world.variant.data.Variant.NameValuePair;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * RuleOptions is an object for storing Options and OptionValues that
 * describe rule variants.
 * <p>
 * Internationalization notes:
 * <p>
 * <pre>
 * OptionValue:
 * getNameI18N()				gets internationalized name (key is getName())
 * getDescriptionI18N()		gets internationalized description (name + "_description")
 *
 * Option:
 * getNameI18N()				gets internationalized name
 * getDescriptionI18N()		gets internationalized description (name + "_description")
 *
 *
 * </pre>
 */
public final class RuleOptions implements Serializable {
    // internal constnats
    private static final String DESCRIPTION = "_description";

    // il8n constants
    private static final String RO_BAD_OPTIONVALUE = "RuleOpts.parser.badoptionvalue";
    private static final String RO_BAD_NVP = "RuleOpts.parser.badnvp";

    // DO NOT change the names of these!!
    // pre-defined option values that are shared between multiple options
    /**
     * TRUE (Boolean) OptionValue
     */
    public static final OptionValue VALUE_TRUE = new OptionValue(
            "OptionValue.true");
    /**
     * FALSE (Boolean) OptionValue
     */
    public static final OptionValue VALUE_FALSE = new OptionValue(
            "OptionValue.false");

    // DO NOT change the names of these!!
    // defined options, and their values
    // BUILD options
    public static final OptionValue VALUE_BUILDS_HOME_ONLY = new OptionValue(
            "OptionValue.home-only");
    public static final OptionValue VALUE_BUILDS_ANY_OWNED = new OptionValue(
            "OptionValue.any-owned");
    public static final OptionValue VALUE_BUILDS_ANY_IF_HOME_OWNED = new OptionValue(
            "OptionValue.any-if-home-owned");
    public static final Option OPTION_BUILDS = new Option("Option.builds",
            VALUE_BUILDS_HOME_ONLY,
            Arrays.asList(VALUE_BUILDS_HOME_ONLY, VALUE_BUILDS_ANY_OWNED,
                    VALUE_BUILDS_ANY_IF_HOME_OWNED));


    public static final OptionValue VALUE_WINGS_ENABLED = new OptionValue(
            "OptionValue.wings-enabled");
    public static final OptionValue VALUE_WINGS_DISABLED = new OptionValue(
            "OptionValue.wings-disabled");
    public static final Option OPTION_WINGS = new Option("Option.wings",
            VALUE_WINGS_DISABLED,
            Arrays.asList(VALUE_WINGS_ENABLED, VALUE_WINGS_DISABLED));


    public static final OptionValue VALUE_PATHS_EXPLICIT = new OptionValue(
            "OptionValue.explicit-paths");
    public static final OptionValue VALUE_PATHS_IMPLICIT = new OptionValue(
            "OptionValue.implicit-paths");
    public static final OptionValue VALUE_PATHS_EITHER = new OptionValue(
            "OptionValue.either-path");
    public static final Option OPTION_CONVOYED_MOVES = new Option(
            "Option.move.convoyed", VALUE_PATHS_EITHER,
            Arrays.asList(VALUE_PATHS_EITHER, VALUE_PATHS_EXPLICIT,
                    VALUE_PATHS_IMPLICIT));


    // array of default options, that are always set for every variant.
    private static final Option[] DEFAULT_RULE_OPTIONS = {OPTION_BUILDS, OPTION_WINGS, OPTION_CONVOYED_MOVES};

    // NOTE: we must include all options / optionvalues in these arrays
    // OptionList -- for serialization/deserialization
    private static final Option[] ALL_OPTIONS = {OPTION_BUILDS, OPTION_WINGS, OPTION_CONVOYED_MOVES};

    // OptionValue -- for serialization/deserialization
    private static final OptionValue[] ALL_OPTIONVALUES = {VALUE_BUILDS_HOME_ONLY, VALUE_BUILDS_ANY_OWNED, VALUE_BUILDS_ANY_IF_HOME_OWNED, VALUE_WINGS_ENABLED, VALUE_WINGS_DISABLED, VALUE_PATHS_EXPLICIT, VALUE_PATHS_IMPLICIT, VALUE_PATHS_EITHER};


    // instance variables
    private final Map<Option, OptionValue> optionMap;


    /**
     * An Option is created for each type of Rule that may have more than
     * one allowable option. The name of each Option must be unique.
     */
    public static final class Option implements Serializable {
        // instance variables
        private final String name;
        private final List<OptionValue> allowed;
        private final OptionValue defaultValue;

        /**
         * Create an Option.
         */
        public Option(final String name, final OptionValue defaultValue,
                      final List<OptionValue> allowed) {
            Objects.requireNonNull(defaultValue);
            Objects.requireNonNull(name);
            Objects.requireNonNull(allowed);

            this.name = name;
            this.defaultValue = defaultValue;
            this.allowed = new ArrayList<>(allowed);
        }// Option()

        /**
         * Returns the Option name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the default Option value.
         */
        public OptionValue getDefault() {
            return defaultValue;
        }

        /**
         * Returns the allowed Option values.
         */
        public List<OptionValue> getAllowed() {
            return Collections.unmodifiableList(allowed);
        }

        /**
         * Checks if the given optionValue is allowed.
         */
        public boolean isAllowed(final OptionValue optionValue) {
            return allowed.stream()
                    .anyMatch(anAllowed -> (optionValue == anAllowed));
        }// isAllowed()

        /**
         * Gets the internationalized ("display") version of the name.
         */
        public String getNameI18N() {
            return Utils.getLocalString(name);
        }

        /**
         * Gets the internationalized ("display") version of the description.
         */
        public String getDescriptionI18N() {
            return Utils.getLocalString(name + DESCRIPTION);
        }

        /**
         * Checks if the given OptionValue is permitted; if so, returns true.
         */
        public boolean checkValue(final OptionValue value) {
            return allowed.stream()
                    .anyMatch(anAllowed -> anAllowed.equals(value));
        }// checkValue()


        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Option) {
                final Option o = (Option) obj;
                return name.equals(o.name);
            }
            return false;
        }// equals()


        @Override
        public int hashCode() {
            return name.hashCode();
        }// hashCode()

        private Object readResolve() throws ObjectStreamException {
            // slow but easy
            for (final Option option : ALL_OPTIONS) {
                if (name.equals(option.name)) {
                    return option;
                }
            }

            throw new InvalidObjectException(
                    "RuleOptions: ALL_OPTIONS internal error");
        }// readResolve()

        /**
         * For debugging only
         */
        @Override
        public String toString() {
            return name;
        }
    }// nested class Option


    /**
     * OptionValues are the pre-defined values that an Option may have.
     * <p>
     * OptionValue names need not be unique, and may be shared between
     * options.
     */
    public static final class OptionValue implements Serializable {
        // instance variables
        private final String name;

        /**
         * Create an OptionValue.
         */
        public OptionValue(final String name) {
            Objects.requireNonNull(name);
            this.name = name;
        }// OptionValue()

        /**
         * Returns the OptionValue name.
         */
        public String getName() {
            return name;
        }// getName()

        /**
         * Gets the internationalized ("display") version of the name.
         */
        public String getNameI18N() {
            return Utils.getLocalString(name);
        }

        /**
         * Gets the internationalized ("display") version of the description.
         */
        public String getDescriptionI18N() {
            return Utils.getLocalString(name + DESCRIPTION);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof OptionValue) {
                OptionValue o = (OptionValue) obj;
                return name.equals(o.name);
            }
            return false;
        }// equals()

        @Override
        public int hashCode() {
            return name.hashCode();
        }// hashCode()


        private Object readResolve() throws ObjectStreamException {
            // slow but easy
            for (OptionValue ALL_OPTIONVALUE : ALL_OPTIONVALUES) {
                if (name.equals(ALL_OPTIONVALUE.name)) {
                    return ALL_OPTIONVALUE;
                }
            }

            throw new InvalidObjectException(
                    "RuleOptions: ALL_OPTIONVALUES internal error");
        }// readResolve()

        /**
         * For debugging only
         */
        @Override
        public String toString() {
            return name;
        }
    }// nested class OptionValue()


    /**
     * Creates a new RuleOptions object, which stores various Rule options.
     */
    public RuleOptions() {
        optionMap = new HashMap<>(31);
    }// RuleOptions()


    /**
     * Sets the OptionValue for an Option.
     * <p>
     * Null Options or OptionValues are not permitted. If an invalid OptionValue
     * is given, an IllegalArgumentException is thrown.
     */
    public void setOption(final Option option, final OptionValue value) {
        Objects.requireNonNull(option);
        Objects.requireNonNull(value);

        if (!option.checkValue(value)) {
            throw new IllegalArgumentException(
                    "invalid OptionValue for Option");
        }

        optionMap.put(option, value);
    }// setOption()


    /**
     * Obtains the value for an Option. If the Option is not found, or its OptionValue
     * not set, the default OptionValue is returned.
     * <p>
     * A null Option is not permitted.
     */
    public OptionValue getOptionValue(final Option option) {
        Objects.requireNonNull(option);

        final OptionValue value = optionMap.get(option);
        if (value == null) {
            return option.getDefault();
        }

        return value;
    }// getOption()


    /**
     * Returns a Set of all Options.
     */
    public Set<Option> getAllOptions() {
        return optionMap.keySet();
    }// getAllOptions()


    /**
     * For debugging only; print the rule options
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(256);
        sb.append(getClass().getName());
        sb.append('\n');

        final Set<Option> set = getAllOptions();
        final Iterator<Option> iter = set.iterator();
        while (iter.hasNext()) {
            final Option opt = iter.next();
            final OptionValue ov = getOptionValue(opt);
            sb.append("  ");
            sb.append(opt);
            sb.append(" : ");
            sb.append(ov);
            sb.append('\n');
        }

        return sb.toString();
    }// toString()


    /**
     * Create a RuleOptions from a Variant.
     * <p>
     * An InvalidWorldException is thrown if the passed data
     * is invalid.
     */
    public static RuleOptions createFromVariant(
            final Variant variant) throws InvalidWorldException {
        // create ruleoptions
        // set rule options
        final RuleOptions ruleOpts = new RuleOptions();

        // set default rule options
        for (Option DEFAULT_RULE_OPTION : DEFAULT_RULE_OPTIONS) {
            ruleOpts.setOption(DEFAULT_RULE_OPTION,
                    DEFAULT_RULE_OPTION.getDefault());
        }

        // this class
        final Class clazz = ruleOpts.getClass();

        // look up all name-value pairs via reflection.
        final List<NameValuePair> nvps = variant.getRuleOptionNVPs();
        for (NameValuePair nvp : nvps) {
            final Option option;
            final OptionValue optionValue;

            // first, check the name
            try {
                Field field = clazz.getField(nvp.getName());
                option = (Option) field.get(null);

                field = clazz.getField(nvp.getValue());
                optionValue = (OptionValue) field.get(null);
            } catch (final Exception e) {
                throw new InvalidWorldException(
                        Utils.getLocalString(RO_BAD_NVP, nvp.getName(),
                                nvp.getValue(), e.getMessage()));
            }


            // ensure that optionValue is valid for option
            if (!option.isAllowed(optionValue)) {
                throw new InvalidWorldException(
                        Utils.getLocalString(RO_BAD_OPTIONVALUE, nvp.getValue(),
                                nvp.getName()));
            }

            // set option
            ruleOpts.setOption(option, optionValue);
        }

        // done.
        return ruleOpts;
    }// createFromVariant()


}// class RuleOptions
