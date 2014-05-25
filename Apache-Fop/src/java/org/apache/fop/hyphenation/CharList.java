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

/* $Id: CharList.java 1311120 2012-04-08 23:48:11Z gadams $ */

package org.apache.fop.hyphenation;

import java.io.Serializable;

/**
 * <p>
 * This class implements a simple char vector with access to the underlying
 * array.
 * </p>
 *
 * <p>
 * This work was authored by Carlos Villegas (cav@uniscope.co.jp).
 * </p>
 */
public class CharList implements Cloneable, Serializable {

    private static final long serialVersionUID = 4263472982169004048L;

    /**
     * Capacity increment size
     */
    private static final int DEFAULT_BLOCK_SIZE = 2048;
    private int blockSize;

    /**
     * The encapsulated array
     */
    private char[] array;

    /**
     * Points to next free item
     */
    private int n;

    /**
     * Construct char vector instance with default block size.
     */
    public CharList() {
        this(DEFAULT_BLOCK_SIZE);
    }

    /**
     * Construct char vector instance.
     * 
     * @param capacity
     *            initial block size
     */
    public CharList(final int capacity) {
        if (capacity > 0) {
            this.blockSize = capacity;
        } else {
            this.blockSize = DEFAULT_BLOCK_SIZE;
        }
        this.array = new char[this.blockSize];
        this.n = 0;
    }

    /**
     * Construct char vector instance.
     * 
     * @param a
     *            char array to use
     */
    public CharList(final char[] a) {
        this.blockSize = DEFAULT_BLOCK_SIZE;
        this.array = a;
        this.n = a.length;
    }

    /**
     * Construct char vector instance.
     * 
     * @param a
     *            char array to use
     * @param capacity
     *            initial block size
     */
    public CharList(final char[] a, final int capacity) {
        if (capacity > 0) {
            this.blockSize = capacity;
        } else {
            this.blockSize = DEFAULT_BLOCK_SIZE;
        }
        this.array = a;
        this.n = a.length;
    }

    /**
     * Reset length of vector, but don't clear contents.
     */
    public void clear() {
        this.n = 0;
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final CharList cv = (CharList) super.clone();
        cv.array = this.array.clone();
        return cv;
    }

    /**
     * Obtain char vector array.
     * 
     * @return char array
     */
    public char[] getArray() {
        return this.array;
    }

    /**
     * Obtain number of items in array.
     * 
     * @return number of items
     */
    public int length() {
        return this.n;
    }

    /**
     * Obtain capacity of array.
     * 
     * @return current capacity of array
     */
    public int capacity() {
        return this.array.length;
    }

    /**
     * Pet char at index.
     * 
     * @param index
     *            the index
     * @param val
     *            a char
     */
    public void put(final int index, final char val) {
        this.array[index] = val;
    }

    /**
     * Get char at index.
     * 
     * @param index
     *            the index
     * @return a char
     */
    public char get(final int index) {
        return this.array[index];
    }

    /**
     * This is to implement memory allocation in the array. Like malloc().
     * 
     * @param size
     *            to allocate
     * @return previous length
     */
    public int alloc(final int size) {
        final int index = this.n;
        final int len = this.array.length;
        if (this.n + size >= len) {
            final char[] aux = new char[len + this.blockSize];
            System.arraycopy(this.array, 0, aux, 0, len);
            this.array = aux;
        }
        this.n += size;
        return index;
    }

    /**
     * Trim char vector to current length.
     */
    public void trimToSize() {
        if (this.n < this.array.length) {
            final char[] aux = new char[this.n];
            System.arraycopy(this.array, 0, aux, 0, this.n);
            this.array = aux;
        }
    }

}
