/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: TableEventProducer.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.fo.flow.table;

import org.apache.fop.events.EventBroadcaster;
import org.apache.fop.events.EventProducer;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.expr.PropertyException;
import org.xml.sax.Locator;

/**
 * Event producer interface for table-specific XSL-FO validation messages.
 */
public interface TableEventProducer extends EventProducer {

    /** Provider class for the event producer. */
    static final class Provider {

        private Provider() {
        }

        /**
         * Returns an event producer.
         * 
         * @param broadcaster
         *            the event broadcaster to use
         * @return the event producer
         */
        public static TableEventProducer get(final EventBroadcaster broadcaster) {
            return (TableEventProducer) broadcaster
                    .getEventProducerFor(TableEventProducer.class);
        }
    }

    /**
     * A value other than "auto" has been specified on fo:table.
     * 
     * @param source
     *            the event source
     * @param loc
     *            the location of the error or null
     * @event.severity WARN
     */
    void nonAutoBPDOnTable(final Object source, final Locator loc);

    /**
     * Padding on fo:table is ignored if the collapsing border model is active.
     * 
     * @param source
     *            the event source
     * @param loc
     *            the location of the error or null
     * @event.severity WARN
     */
    void noTablePaddingWithCollapsingBorderModel(final Object source,
            final Locator loc);

    /**
     * No mixing of table-rows and table-cells is allowed for direct children of
     * table-body.
     * 
     * @param source
     *            the event source
     * @param elementName
     *            the name of the context node
     * @param loc
     *            the location of the error or null
     * @throws ValidationException
     *             the validation error provoked by the method call
     * @event.severity FATAL
     */
    void noMixRowsAndCells(final Object source, final String elementName,
            final Locator loc) throws ValidationException;

    /**
     * The table-footer was found after the table-body. FOP cannot recover with
     * collapsed border model.
     * 
     * @param source
     *            the event source
     * @param elementName
     *            the name of the context node
     * @param loc
     *            the location of the error or null
     * @throws ValidationException
     *             the validation error provoked by the method call
     * @event.severity FATAL
     */
    void footerOrderCannotRecover(final Object source,
            final String elementName, final Locator loc)
            throws ValidationException;

    /**
     * starts-row/ends-row for fo:table-cells non-applicable for children of an
     * fo:table-row
     * 
     * @param source
     *            the event source
     * @param loc
     *            the location of the error or null
     * @event.severity WARN
     */
    void startEndRowUnderTableRowWarning(final Object source, final Locator loc);

    /**
     * Column-number or number of cells in the row overflows the number of
     * fo:table-column specified for the table.
     * 
     * @param source
     *            the event source
     * @param loc
     *            the location of the error or null
     * @throws ValidationException
     *             the validation error provoked by the method call
     * @event.severity FATAL
     */
    void tooManyCells(final Object source, final Locator loc)
            throws ValidationException;

    /**
     * Property value must be 1 or bigger.
     * 
     * @param source
     *            the event source
     * @param propName
     *            the property name
     * @param actualValue
     *            the actual value
     * @param loc
     *            the location of the error or null
     * @throws PropertyException
     *             the property error provoked by the method call
     * @event.severity FATAL
     */
    void valueMustBeBiggerGtEqOne(final Object source, final String propName,
            final int actualValue, final Locator loc) throws PropertyException;

    /**
     * table-layout=\"fixed\" and column-width unspecified => falling back to
     * proportional-column-width(1)
     * 
     * @param source
     *            the event source
     * @param loc
     *            the location of the error or null
     * @event.severity WARN
     */
    void warnImplicitColumns(final Object source, final Locator loc);

    /**
     * padding-* properties are not applicable.
     * 
     * @param source
     *            the event source
     * @param elementName
     *            the name of the context node
     * @param loc
     *            the location of the error or null
     * @event.severity WARN
     */
    void paddingNotApplicable(final Object source, final String elementName,
            final Locator loc);

    /**
     * Cell overlap.
     * 
     * @param source
     *            the event source
     * @param elementName
     *            the name of the context node
     * @param column
     *            the column index of the overlapping cell
     * @param loc
     *            the location of the error or null
     * @throws PropertyException
     *             the property error provoked by the method call
     * @event.severity FATAL
     */
    void cellOverlap(final Object source, final String elementName,
            final int column, final Locator loc) throws PropertyException;

    /**
     * @param source
     *            the event source
     * @param elementName
     *            the name of the context node
     * @param propValue
     *            the user-specified value of the column-number property
     * @param columnNumber
     *            the generated value for the column-number property
     * @param loc
     *            the location of the error or null
     * @event.severity WARN
     */
    void forceNextColumnNumber(final Object source, final String elementName,
            final Number propValue, final int columnNumber, final Locator loc);

    /**
     * Break ignored due to row spanning.
     * 
     * @param source
     *            the event source
     * @param elementName
     *            the name of the context node
     * @param breakBefore
     *            true for "break-before", false for "break-after"
     * @param loc
     *            the location of the error or null
     * @event.severity WARN
     */
    void breakIgnoredDueToRowSpanning(final Object source,
            final String elementName, final boolean breakBefore,
            final Locator loc);

}
