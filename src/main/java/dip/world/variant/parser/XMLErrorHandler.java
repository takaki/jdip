//
//  @(#)XMLErrorHandler.java	2/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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
package dip.world.variant.parser;

import dip.gui.dialog.ErrorDialog;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Objects;

/**
 * A simple error handler for the XML parsers.
 * <p>
 * Errors sent to an ErrorDialog
 */
public final class XMLErrorHandler implements ErrorHandler {
    /**
     * Create an XMLErrorHandler
     */
    public XMLErrorHandler() {
    }// XMLErrorHandler()


    /**
     * Handle a (recoverable) error
     */
    @Override
    public void error(final SAXParseException exception) {
        showError(exception, "Error");
    }// error()

    /**
     * Handle a non-recoverable error
     */
    @Override
    public void fatalError(final SAXParseException exception) {
        showError(exception, "Fatal Error");
    }// fatalError()

    /**
     * Handle a warning
     */
    @Override
    public void warning(final SAXParseException exception) {
        showError(exception, "Warning");
    }// warning()

    /**
     * Dialog method for error handling
     */
    protected void showError(final SAXParseException e, final String type) {
        ErrorDialog.displayGeneral(null, new SAXException(
                String.format("XML Validation %s:\n%s", type,
                        getLocationString(e))));
    }// showError()


    /**
     * Gets the error, nicely formatted
     */
    protected String getLocationString(final SAXParseException e) {
        final StringBuffer sb = new StringBuffer(256);
        String systemId = e.getSystemId();

        if (Objects.nonNull(systemId)) {
            final int index = systemId.lastIndexOf('/');

            if (index != -1) {
                systemId = systemId.substring(index + 1);
            }

            sb.append(systemId);
            sb.append(':');
        }

        sb.append("line ");
        sb.append(e.getLineNumber());
        sb.append(":col ");
        sb.append(e.getColumnNumber());
        sb.append(':');
        sb.append(e.getMessage());

        //e.printStackTrace();

        return sb.toString();
    }// getLocationString()


}// class XMLErrorHandler

