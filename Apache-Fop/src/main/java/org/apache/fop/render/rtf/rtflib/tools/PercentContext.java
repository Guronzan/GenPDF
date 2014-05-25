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

/* $Id: PercentContext.java 1311081 2012-04-08 20:18:15Z gadams $ */

package org.apache.fop.render.rtf.rtflib.tools;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.pagination.PageSequence;

/**
 * <p>
 * PercentBaseContext implementation to track base widths for percentage
 * calculations.
 * </p>
 */
@Slf4j
public class PercentContext implements PercentBaseContext {

    /** Map containing the FObj and its width */
    private final Map lengthMap = new java.util.HashMap();

    /** Map containing the Tables and their table units */
    private final Map tableUnitMap = new java.util.HashMap();

    /** Variable to check if a base width is set */
    private boolean baseWidthSet = false;

    /**
     * Returns the available width for a specific FObj
     *
     * @param lengthBase
     *            lengthBase not used
     * @param fobj
     *            the FObj
     * @return Available Width
     */
    @Override
    public int getBaseLength(final int lengthBase, FObj fobj) {
        if (fobj == null) {
            return 0;
        }

        // Special handler for TableColumn width specifications, needs to be
        // relative to the parent!
        if (fobj instanceof TableColumn && fobj.getParent() instanceof FObj) {
            fobj = (FObj) fobj.getParent();
        }

        switch (lengthBase) {
        case LengthBase.CONTAINING_BLOCK_WIDTH:
        case LengthBase.PARENT_AREA_WIDTH:
        case LengthBase.CONTAINING_REFAREA_WIDTH:
            Object width = this.lengthMap.get(fobj);
            if (width != null) {
                return Integer.parseInt(width.toString());
            } else if (fobj.getParent() != null) {
                // If the object itself has no width the parent width will be
                // used
                // because it is the base width of this object
                width = this.lengthMap.get(fobj.getParent());
                if (width != null) {
                    return Integer.parseInt(width.toString());
                }
            }
            return 0;
        case LengthBase.TABLE_UNITS:
            Object unit = this.tableUnitMap.get(fobj);
            if (unit != null) {
                return ((Integer) unit).intValue();
            } else if (fobj.getParent() != null) {
                // If the object itself has no width the parent width will be
                // used
                unit = this.tableUnitMap.get(fobj.getParent());
                if (unit != null) {
                    return ((Integer) unit).intValue();
                }
            }
            return 0;
        default:
            log.error("Unsupported base type for LengthBase:" + lengthBase,
                    new Exception("Unsupported base type for LengthBase:"
                            + lengthBase));
            return 0;
        }
    }

    /**
     * Elements having a width property can call this function if their width is
     * calculated in RTFHandler
     *
     * @param fobj
     *            the FObj
     * @param width
     *            width of the FObj (in millipoints)
     */
    public void setDimension(final FObj fobj, final int width) {
        // TODO ACCEPT only objects above for setting a width
        if (fobj instanceof PageSequence) {
            this.baseWidthSet = true;
        }
        // width in mpt
        this.lengthMap.put(fobj, new Integer(width));
    }

    /**
     * Records the calculated table unit for a given table.
     *
     * @param table
     *            the table for which the table unit is set
     * @param tableUnit
     *            the table unit value (in millipoints)
     */
    public void setTableUnit(final Table table, final int tableUnit) {
        this.tableUnitMap.put(table, new Integer(tableUnit));
    }

    /**
     * Searches for the parent object of fobj.
     */
    private Integer findParent(final FONode fobj) {
        if (fobj.getRoot() != fobj) {
            if (this.lengthMap.containsKey(fobj)) {
                return new Integer(this.lengthMap.get(fobj).toString());
            } else {
                return findParent(fobj.getParent());
            }
        } else {
            log.error("Base Value for element " + fobj.getName() + " not found");
            return new Integer(-1);
        }
    }

    /**
     * Elements willing to use this context have to register themselves by
     * calling this function.
     *
     * @param fobj
     *            the FObj
     */
    public void setDimension(final FObj fobj) {
        if (this.baseWidthSet) {
            final Integer width = findParent(fobj.getParent());
            if (width.intValue() != -1) {
                this.lengthMap.put(fobj, width);
            }
        }
    }
}
