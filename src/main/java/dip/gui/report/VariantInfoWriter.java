//
//  @(#)VariantInfoWriter.java		5/2004
//
//  Copyright 2004 Zachary DelProposto. All rights reserved.
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
package dip.gui.report;

import dip.gui.ClientFrame;
import dip.gui.dialog.TextViewer;
import dip.misc.Utils;
import dip.world.RuleOptions;
import dip.world.World;
import dip.world.variant.VariantManager;
import dip.world.variant.data.Variant;

import javax.swing.*;
import java.util.Iterator;
import java.util.Set;


/**
 * Writes information about the current Variant as HTML, and, also
 * displays the current rule settings (RuleOptions).
 */
public class VariantInfoWriter {
    // i18n constants
    private static final String TEMPLATE_LOCATION = "VariantInfoWriter.template.location";
    private static final String DIALOG_TITLE = "VariantInfoWriter.dialog.title";

    // instance fields
    private World world = null;


    /**
     * Gets information about the variant and current rule
     * options. Result is in HTML.
     */
    public static String variantInfoToHTML(final World w) {
        final VariantInfoWriter viw = new VariantInfoWriter(w);
        return viw.getInfoAsHTML();
    }// stateToHTML()


    /**
     * Returns the HTML-encoded variant information inside a dialog.
     */
    public static void displayDialog(final ClientFrame clientFrame,
                                     final World w) {
        final TextViewer tv = new TextViewer(clientFrame);
        tv.setEditable(false);
        tv.addSingleButton(tv.makeOKButton());
        tv.setTitle(Utils.getLocalString(DIALOG_TITLE));
        tv.setHeaderVisible(false);
        tv.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        tv.lazyLoadDisplayDialog(new TextViewer.TVRunnable() {
            public void run() {
                setText(variantInfoToHTML(w));
            }
        });
    }// displayDialog()


    /**
     * StateWriter constructor
     */
    private VariantInfoWriter(final World w) {
        this.world = w;
    }// VariantInfoWriter()


    /**
     * Writes info as HTML
     */
    private String getInfoAsHTML() {
        // get template
        String template = Utils
                .getText(Utils.getLocalString(TEMPLATE_LOCATION));
        if (template == null) {
            template = "ERROR: missing template resource " + Utils
                    .getLocalString(TEMPLATE_LOCATION);
        }

        // get selcted variant
        final World.VariantInfo vi = world.getVariantInfo();
        final Variant variant = new VariantManager()
                .getVariant(vi.getVariantName(), vi.getVariantVersion()).orElse(null);

        // get 8 main arguments
        final Object[] oldArgs = variant.getHTMLSummaryArguments().toArray(new Object[0]);

        // make extra space
        final Object[] newArgs = new Object[oldArgs.length + 1];
        System.arraycopy(oldArgs, 0, newArgs, 0, oldArgs.length);

        // make 9th argument: 'rules settings'
        final StringBuffer sb = new StringBuffer(256);

        final RuleOptions ro = world.getRuleOptions();
        final Set allOpts = ro.getAllOptions();
        final Iterator iter = allOpts.iterator();
        while (iter.hasNext()) {
            final RuleOptions.Option opt = (RuleOptions.Option) iter.next();
            final RuleOptions.OptionValue optVal = ro.getOptionValue(opt);

            sb.append("<p><b>");
            sb.append(opt.getNameI18N());
            sb.append(":</b> ");
            sb.append(optVal.getNameI18N());
            sb.append('\n');
        }

        newArgs[newArgs.length - 1] = sb.toString();

        // return text.
        return Utils.format(template, newArgs);
    }// getInfoAsHTML()


}// class VariantInfoWriter
