//	
//	@(#)ValidationOptions.java		4/2002
//	
//	Copyright 2002 Zachary DelProposto. All rights reserved.
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
package dip.order;

import dip.misc.Utils;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * Controls how orders are validated.
 * <p>
 * This is expandable and adaptable, both for GUI use
 * and for future non-standard adjudicator use.
 * <p>
 * Currently, there are 2 levels; LOOSE and STRICT.
 */

public class ValidationOptions implements Serializable, Cloneable {
    // Contants: never internationalized.

    // global: parsing strictness
    // NOTE: for il8n:
    //		NAME = key for il8n name
    //		NAME_description = description
    public static final String KEY_GLOBAL_PARSING = "KEY_GLOBAL_PARSING";
    public static final String VALUE_GLOBAL_PARSING_STRICT = "VALUE_GLOBAL_PARSING_STRICT";
    public static final String VALUE_GLOBAL_PARSING_LOOSE = "VALUE_GLOBAL_PARSING_LOOSE";
    public static final Option OPTION_GLOBAL_PARSING = new Option(
            KEY_GLOBAL_PARSING,
            new String[]{VALUE_GLOBAL_PARSING_STRICT, VALUE_GLOBAL_PARSING_LOOSE},
            VALUE_GLOBAL_PARSING_STRICT);


    // internal: list of all options (in base class)
    protected static final Option[] _OPTIONS = {OPTION_GLOBAL_PARSING};
    private static final String DESCRIPTION = "_description";


    // instance variables
    protected Hashtable<String, Object> map = new Hashtable<>(5);
    protected Option[] options = null;            // subclasses should modify as appropriate in constructor


    // constructor: init's all values to default.
    public ValidationOptions() {
        options = _OPTIONS;
        clearOptions();
    }// ValidationOptions()

    /**
     * for clone():
     * <p>
     * The hashtable is cloned, using hashtable.clone(). Althoug this is a shallow
     * copy, keys are OK to be shallow, and so are values, since all values are constants
     * anyway. The only important thing is that a new hashtable is made, that
     * is similar to the old hashtable.
     * <p>
     * <p>
     * only data in the hashtable changes between objects for a given validationOptions
     */
    public Object clone() throws CloneNotSupportedException {
        final ValidationOptions vopt = (ValidationOptions) super.clone();
        vopt.map = (Hashtable<String, Object>) this.map.clone();
        return vopt;
    }// clone()


    // Query methods
    // get options descriptions, internationilzed; used by GUI to
    // display settings.
    public DescriptiveOption[] getOptions() {
        final DescriptiveOption[] dopts = new DescriptiveOption[options.length];
        for (int i = 0; i < dopts.length; i++) {
            dopts[i] = new DescriptiveOption(options[i]);
            final DescriptiveOption opt = dopts[i];    // current option

            opt.setDisplayName(Utils.getLocalString(opt.getKey()));
            opt.setDescription(
                    Utils.getLocalString(opt.getKey() + DESCRIPTION));

            final String[] optionValues = opt.getValues();
            final int nOpts = optionValues.length;
            final String[] names = new String[nOpts];
            final String[] descriptions = new String[nOpts];
            for (int j = 0; j < nOpts; j++) {
                names[j] = Utils.getLocalString(optionValues[j]);
                descriptions[j] = Utils
                        .getLocalString(optionValues[j] + DESCRIPTION);
            }
            opt.setDisplayValues(names);
            opt.setValueDescriptions(descriptions);
        }

        return dopts;
    }// getOptions()


    // Set/Get methods
    public void setOption(final String key, final Object value) {
        map.put(key, value);
    }// setOption()

    public Object getOption(final String key) {
        return map.get(key);
    }// getOption()

    public boolean isOption(final String key, final Object value) {
        return value.equals(map.get(key));
    }// isOption()

    // set all options to default.
    public final void clearOptions() {
        map.clear();
        for (Option option : options) {
            map.put(option.getKey(), option.getDefaultValue());
        }
    }// clearOptions()


    // exposed classes
    // 'current' value of an option is in hashtable
    // perhaps these should be renamed: OptionInfo, then
    public static class Option {
        private final String key;                // actual name
        private final String values[];            // allowable values (to set)
        private final String defaultValue;        // default value

        protected Option(final String key, final String[] values, final String defaultValue) {
            this.key = key;
            this.values = values;
            this.defaultValue = defaultValue;
        }// Option

        public String getKey() {
            return key;
        }

        public String[] getValues() {
            return values;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }// class Option

    public static class DescriptiveOption extends Option {
        private String displayName;            // il8n name
        private String description;            // il8n description of name
        private String displayValues[];        // il8n value name
        private String valueDescriptions[]; // il8n value description (optional)

        protected DescriptiveOption(final ValidationOptions.Option option) {
            super(option.key, option.values, option.defaultValue);
        }// DescriptiveOption

        protected void setDisplayName(final String value) {
            displayName = value;
        }

        protected void setDescription(final String value) {
            description = value;
        }

        protected void setDisplayValues(final String[] value) {
            if (value.length != getValues().length) {
                throw new IllegalArgumentException(
                        "Number of value names != Number of values");
            }

            displayValues = value;
        }// setDisplayValues()

        protected void setValueDescriptions(final String[] value) {
            if (value.length != getValues().length) {
                throw new IllegalArgumentException(
                        "Number of value descriptions != Number of values");
            }

            valueDescriptions = value;
        }// setValueDescriptions()

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String[] getDisplayValues() {
            return displayValues;
        }

        public String[] getValueDescriptions() {
            return valueDescriptions;
        }
    }// inner class DescriptiveOption


}// class ValidationOptions
