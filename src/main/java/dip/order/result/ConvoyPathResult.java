//	
//	@(#)ConvoyPathResult.java	12/2003
//	
//	Copyright 2003 Zachary DelProposto. All rights reserved.
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
package dip.order.result;

import dip.misc.Utils;
import dip.order.OrderFormat;
import dip.order.OrderFormatOptions;
import dip.order.Orderable;
import dip.world.Province;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An OrderResult that contains the path taken by a successfully
 * convoyed Move. It has the result type of CONVOY_PATH_TAKEN.
 */
public class ConvoyPathResult extends OrderResult {
    // i18n
    private static final String KEY_MESSAGE = "ConvoyPathResult.message";
    private static final String KEY_ARROW = "ConvoyPathResult.arrow";

    // instance fields
    private Province[] convoyPath;


    /**
     * Create a ConvoyPathResult
     */
    public ConvoyPathResult(final Orderable order, final List<Province> path) {
        this(order, path.toArray(new Province[path.size()]));
    }// ConvoyPathResult()

    /**
     * Create a ConvoyPathResult
     */
    public ConvoyPathResult(final Orderable order,
                            final Province[] convoyPath) {
        if (convoyPath == null || convoyPath.length < 3) {
            throw new IllegalArgumentException("bad path (null or length < 3)");
        }

        power = order.getPower();
        message = null;
        this.order = order;
        resultType = ResultType.CONVOY_PATH_TAKEN;
        this.convoyPath = convoyPath;
    }// ConvoyPathResult()


    /**
     * Creates an appropriate internationalized text message given the
     * convoy path.
     */
    @Override
    public String getMessage(final OrderFormatOptions ofo) {
        /*
        arguments:        			{0}	: convoy path taken.
		*/
        // create path list
        final String arrow = Utils.getLocalString(KEY_ARROW);
        // return formatted message
        return Utils.getLocalString(KEY_MESSAGE,
                Arrays.stream(convoyPath).map(c -> OrderFormat.format(ofo, c))
                        .collect(Collectors.joining(arrow)));
    }// getMessage()


    /**
     * Primarily for debugging.
     */
    @Override
    public String toString() {
        // add convoy path
        return String.format("%s convoy path: %s", super.toString(),
                Arrays.stream(convoyPath).map(Province::getShortName)
                        .collect(Collectors.joining("-")));
    }// toString()

}// class ConvoyPathResult
