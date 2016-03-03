//
// 	@(#)OrderResult.java		4/2002
//
// 	Copyright 2002 Zachary DelProposto. All rights reserved.
// 	Use is subject to license terms.
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

import dip.order.Orderable;

import java.util.Objects;

/**
 * A message sent to a specific Power that refers to a specific order.
 * The message is classified according to ResultType (see for details).
 * <p>
 * More than one OrderResult may exist for a single order.
 */
public class OrderResult extends Result {
    // instance fields
    /**
     * The ResultType
     */
    protected ResultType resultType;
    /**
     * The Order to which this Result refers
     */
    protected Orderable order;


    /**
     * no-arg constructor for subclasses
     */
    protected OrderResult() {
    }// OrderResult()

    /**
     * Create an OrderResult with the given Order and Message.
     * A null order is not permissable.
     */
    public OrderResult(final Orderable order, final String message) {
        this(order, ResultType.TEXT, message);
    }// OrderResult()


    /**
     * Create an OrderResult with the given Order, ResultType, and Message.
     * A null Order or ResultType is not permissable.
     */
    public OrderResult(final Orderable order, final ResultType type,
                       final String message) {
        super(order.getPower(), message);
        Objects.requireNonNull(type);
        Objects.requireNonNull(order);

        resultType = type;
        this.order = order;
    }// OrderResult()

    /**
     * Get the ResultType. Never returns null.
     */
    public ResultType getResultType() {
        return resultType;
    }// getResultType()

    /**
     * Get the Order. Never return null.
     */
    public Orderable getOrder() {
        return order;
    }// getOrder()

    /**
     * For debugging
     */
    public String toString() {
        final StringBuffer sb = new StringBuffer(180);
        sb.append(power);
        sb.append(": [");
        sb.append(resultType);
        sb.append("] [order: ");
        sb.append(order);
        sb.append("] ");
        sb.append(message);
        return sb.toString();
    }// toString()


    /**
     * Compare in the following order:
     * <ol>
     * <li>Power (null first)
     * <li>Orderable source province	[null first]
     * <li>ResultType	[never null]
     * <li>message [never null, but may be empty]
     * </ol>
     * <p>
     * If power is null, it will be first in ascending order.
     * If message may be empty, but never is null.
     */
    @Override
    public int compareTo(final Object o) {
        if (o instanceof OrderResult) {
            final OrderResult result = (OrderResult) o;

            // 1: compare powers
            int compareResult = 0;
            if (result.power == null && power == null) {
                compareResult = 0;
            } else if (power == null && result.power != null) {
                return -1;
            } else if (power != null && result.power == null) {
                return +1;
            } else {
                // if these are equal, could be 0
                compareResult = power.compareTo(result.power);
            }

            if (compareResult != 0) {
                return compareResult;
            }

            // 2: compare Order Source province
            // null orders come first.
            if (order == null && result.order != null) {
                return -1;
            } else if (order != null && result.order == null) {
                return +1;
            } else if (order != null && result.order != null) {
                // neither are null
                compareResult = order.getSource().getProvince()
                        .compareTo(result.order.getSource().getProvince());
                if (compareResult != 0) {
                    return compareResult;
                }
            }

            // 3: compare ResultType
            compareResult = resultType.compareTo(result.resultType);
            if (compareResult != 0) {
                return compareResult;
            }

            // 4: compare message
            return message.compareTo(result.message);
        } else {
            return super.compareTo(o);
        }
    }// compareTo()


    /**
     * Type-Safe enumerated categories of OrderResults.
     */
    // key constants
    private static final String KEY_VALIDATION_FAILURE = "VALIDATION_FAILURE";
    private static final String KEY_SUCCESS = "SUCCESS";
    private static final String KEY_FAILURE = "FAILURE";
    private static final String KEY_DISLODGED = "DISLODGED";
    private static final String KEY_CONVOY_PATH_TAKEN = "CONVOY_PATH_TAKEN";
    private static final String KEY_TEXT = "TEXT";
    private static final String KEY_SUBSTITUTED = "SUBSTITUTED";

    public enum ResultType {
        /**
         * ResultType indicating that order validation failed
         */
        VALIDATION_FAILURE(KEY_VALIDATION_FAILURE, 10),
        /**
         * ResultType indicating the order was successful
         */
        SUCCESS(KEY_SUCCESS, 20),
        /**
         * ResultType indicating the order has failed
         */
        FAILURE(KEY_FAILURE, 30),
        /**
         * ResultType indicating the order's source unit has been dislodged
         */
        DISLODGED(KEY_DISLODGED, 40),
        /**
         * ResultType indicating what convoy path a convoyed unit used
         */
        CONVOY_PATH_TAKEN(KEY_CONVOY_PATH_TAKEN, 50),
        /**
         * ResultType for a general (not otherwise specified) message
         */
        // text message only
        TEXT(KEY_TEXT, 60),
        /**
         * ResultType indicating that the order was substituted with another order
         */
        SUBSTITUTED(KEY_SUBSTITUTED, 70);


        // instance variables
        private final String key;
        private final int ordering;

        ResultType(final String key, final int ordering) {
            Objects.requireNonNull(key);

            this.key = key;
            this.ordering = ordering;
        }// ResultType()


        /**
         * For debugging: return the name
         */
        @Override
        public String toString() {
            return key;
        }// toString()


    }// nested class ResultType

}// class OrderResult
