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

/* $Id$ */

package org.apache.fop.traits;

/**
 * This class provides a reusable implementation of the WritingModeTraitsSetter
 * interface.
 */
public class WritingModeTraits implements WritingModeTraitsSetter {

    private Direction inlineProgressionDirection;
    private Direction blockProgressionDirection;
    private Direction columnProgressionDirection;
    private Direction rowProgressionDirection;
    private Direction shiftDirection;
    private WritingMode writingMode;

    /**
     * Default writing mode traits constructor.
     */
    public WritingModeTraits() {
        this(WritingMode.LR_TB);
    }

    /**
     * Construct writing mode traits using the specified writing mode.
     * 
     * @param writingMode
     *            a writing mode traits object
     */
    public WritingModeTraits(final WritingMode writingMode) {
        assignWritingModeTraits(writingMode);
    }

    /**
     * @return the "inline-progression-direction" trait.
     */
    @Override
    public Direction getInlineProgressionDirection() {
        return this.inlineProgressionDirection;
    }

    /**
     * @param direction
     *            the "inline-progression-direction" trait.
     */
    @Override
    public void setInlineProgressionDirection(final Direction direction) {
        this.inlineProgressionDirection = direction;
    }

    /**
     * @return the "block-progression-direction" trait.
     */
    @Override
    public Direction getBlockProgressionDirection() {
        return this.blockProgressionDirection;
    }

    /**
     * @param direction
     *            the "block-progression-direction" trait.
     */
    @Override
    public void setBlockProgressionDirection(final Direction direction) {
        this.blockProgressionDirection = direction;
    }

    /**
     * @return the "column-progression-direction" trait.
     */
    @Override
    public Direction getColumnProgressionDirection() {
        return this.columnProgressionDirection;
    }

    /**
     * @param direction
     *            the "column-progression-direction" trait.
     */
    @Override
    public void setColumnProgressionDirection(final Direction direction) {
        this.columnProgressionDirection = direction;
    }

    /**
     * @return the "row-progression-direction" trait.
     */
    @Override
    public Direction getRowProgressionDirection() {
        return this.rowProgressionDirection;
    }

    /**
     * @param direction
     *            the "row-progression-direction" trait.
     */
    @Override
    public void setRowProgressionDirection(final Direction direction) {
        this.rowProgressionDirection = direction;
    }

    /**
     * @return the "shift-direction" trait.
     */
    @Override
    public Direction getShiftDirection() {
        return this.shiftDirection;
    }

    /**
     * @param direction
     *            the "shift-direction" trait.
     */
    @Override
    public void setShiftDirection(final Direction direction) {
        this.shiftDirection = direction;
    }

    /**
     * @return the "writing-mode" trait.
     */
    @Override
    public WritingMode getWritingMode() {
        return this.writingMode;
    }

    /**
     * @param writingMode
     *            the "writing-mode" trait.
     */
    @Override
    public void setWritingMode(final WritingMode writingMode) {
        this.writingMode = writingMode;
    }

    /**
     * @param writingMode
     *            the "writing-mode" trait.
     */
    @Override
    public void assignWritingModeTraits(final WritingMode writingMode) {
        writingMode.assignWritingModeTraits(this);
    }

    /**
     * Helper function to find the writing mode traits getter (if any) that
     * applies for a given FO node.
     * 
     * @param fn
     *            the node to start searching from
     * @return the applicable writing mode traits getter, or null if none
     *         applies
     */
    public static WritingModeTraitsGetter getWritingModeTraitsGetter(
            final org.apache.fop.fo.FONode fn) {
        for (org.apache.fop.fo.FONode n = fn; n != null; n = n.getParent()) {
            if (n instanceof WritingModeTraitsGetter) {
                return (WritingModeTraitsGetter) n;
            }
        }
        return null;
    }

}
