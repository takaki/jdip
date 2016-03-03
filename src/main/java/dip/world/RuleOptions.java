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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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


    // array of default options, that are always set for every variant.
    private static final List<Option> DEFAULT_RULE_OPTIONS = Arrays
            .asList(Option.OPTION_BUILDS, Option.OPTION_WINGS,
                    Option.OPTION_CONVOYED_MOVES);

    // instance variables
    private final Map<Option, OptionValue> optionMap;

    /**
     * An Option is created for each type of Rule that may have more than
     * one allowable option. The name of each Option must be unique.
     */
    public enum Option {
        // DO NOT change the names of these!!
        // defined options, and their values
        // BUILD options
        OPTION_BUILDS("Option.builds", OptionValue.VALUE_BUILDS_HOME_ONLY,
                Arrays.asList(OptionValue.VALUE_BUILDS_HOME_ONLY,
                        OptionValue.VALUE_BUILDS_ANY_OWNED,
                        OptionValue.VALUE_BUILDS_ANY_IF_HOME_OWNED)),

        OPTION_WINGS("Option.wings", OptionValue.VALUE_WINGS_DISABLED,
                Arrays.asList(OptionValue.VALUE_WINGS_ENABLED,
                        OptionValue.VALUE_WINGS_DISABLED)),

        OPTION_CONVOYED_MOVES("Option.move.convoyed",
                OptionValue.VALUE_PATHS_EITHER,
                Arrays.asList(OptionValue.VALUE_PATHS_EITHER,
                        OptionValue.VALUE_PATHS_EXPLICIT,
                        OptionValue.VALUE_PATHS_IMPLICIT));


        // instance variables
        private final String name;
        private final List<OptionValue> allowed;
        private final OptionValue defaultValue;

        /**
         * Create an Option.
         */
        Option(final String name, final OptionValue defaultValue,
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
         * Checks if the given value is allowed.
         */
        public boolean isAllowed(final OptionValue value) {
            return allowed.stream().anyMatch(anAllowed -> (anAllowed == value));
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
    public enum OptionValue {
        // DO NOT change the names of these!!
        // pre-defined option values that are shared between multiple options

        /**
         * TRUE (Boolean) OptionValue
         */
        VALUE_TRUE("OptionValue.true"),
        /**
         * FALSE (Boolean) OptionValue
         */
        VALUE_FALSE("OptionValue.false"),

        // DO NOT change the names of these!!
        // defined options, and their values
        // BUILD options
        VALUE_BUILDS_HOME_ONLY("OptionValue.home-only"),
        VALUE_BUILDS_ANY_OWNED("OptionValue.any-owned"),
        VALUE_BUILDS_ANY_IF_HOME_OWNED("OptionValue.any-if-home-owned"),


        VALUE_WINGS_ENABLED("OptionValue.wings-enabled"),
        VALUE_WINGS_DISABLED("OptionValue.wings-disabled"),


        VALUE_PATHS_EXPLICIT("OptionValue.explicit-paths"),
        VALUE_PATHS_IMPLICIT("OptionValue.implicit-paths"),
        VALUE_PATHS_EITHER("OptionValue.either-path");


        // instance variables
        private final String name;

        /**
         * Create an OptionValue.
         */
        OptionValue(final String name) {
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
        optionMap = new EnumMap<>(Option.class);
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

        if (!option.isAllowed(value)) {
            throw new InvalidWorldException(
                    Utils.getLocalString(RO_BAD_OPTIONVALUE, value, option));
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
        return optionMap.getOrDefault(option, option.getDefault());
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
        return getClass().getName() +
                '\n' +
                getAllOptions().stream().map(opt -> {
                    final OptionValue ov = getOptionValue(opt);
                    return String.join("", "  ", opt.toString(), " : ",
                            ov.toString());
                }).collect(Collectors.joining("\n"));
    }// toString()


    /**
     * Create a RuleOptions from a Variant.
     * <p>
     * An InvalidWorldException is thrown if the passed data
     * is invalid.
     */
    public static RuleOptions createFromVariant(final Variant variant) {
        // create ruleoptions
        // set rule options
        final RuleOptions ruleOpts = new RuleOptions();

        // set default rule options
        DEFAULT_RULE_OPTIONS.stream().forEach(
                option -> ruleOpts.setOption(option, option.getDefault()));

        // look up all name-value pairs via reflection.
        variant.getRuleOptionNVPs().stream().forEach(nvp -> {
            // first, check the name
            final Option option = Option.valueOf(nvp.getName());
            final OptionValue optionValue = OptionValue.valueOf(nvp.getValue());

            // set option
            ruleOpts.setOption(option, optionValue);
        });

        // done.
        return ruleOpts;
    }// createFromVariant()


}// class RuleOptions
