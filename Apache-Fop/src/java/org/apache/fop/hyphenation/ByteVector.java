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

/* $Id: ByteVector.java 1297284 2012-03-05 23:29:29Z gadams $ */

package org.apache.fop.hyphenation;

import java.io.Serializable;

/**
 * <p>
 * This class implements a simple byte vector with access to the underlying
 * array.
 * </p>
 *
 * <p>
 * This work was authored by Carlos Villegas (cav@uniscope.co.jp).
 * </p>
 */
public class ByteVector implements Serializable {

    private static final long serialVersionUID = 1554572867863466772L;

    /**
     * Capacity increment size
     */
    private static final int DEFAULT_BLOCK_SIZE = 2048;
    private int blockSize;

    /**
     * The encapsulated array
     */
    private byte[] array;

    /**
     * Points to next free item
     */
    private int n;

    /**
     * Construct byte vector instance with default block size.
     */
    public ByteVector() {
        this(DEFAULT_BLOCK_SIZE);
    }

    /**
     * Construct byte vector instance.
     * 
     * @param capacity
     *            initial block size
     */
    public ByteVector(final int capacity) {
        if (capacity > 0) {
            this.blockSize = capacity;
        } else {
            this.blockSize = DEFAULT_BLOCK_SIZE;
        }
        this.array = new byte[this.blockSize];
        this.n = 0;
    }

    /**
     * Construct byte vector instance.
     * 
     * @param a
     *            byte array to use TODO should n should be initialized to
     *            a.length to be consistent with CharVector behavior? [GA]
     */
    public ByteVector(final byte[] a) {
        this.blockSize = DEFAULT_BLOCK_SIZE;
        this.array = a;
        this.n = 0;
    }

    /**
     * Construct byte vector instance.
     * 
     * @param a
     *            byte array to use
     * @param capacity
     *            initial block size TODO should n should be initialized to
     *            a.length to be consistent with CharVector behavior? [GA]
     */
    public ByteVector(final byte[] a, final int capacity) {
        if (capacity > 0) {
            this.blockSize = capacity;
        } else {
            this.blockSize = DEFAULT_BLOCK_SIZE;
        }
        this.array = a;
        this.n = 0;
    }

    /**
     * Obtain byte vector array.
     * 
     * @return byte array
     */
    public byte[] getArray() {
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
     * Pet byte at index.
     * 
     * @param index
     *            the index
     * @param val
     *            a byte
     */
    public void put(final int index, final byte val) {
        this.array[index] = val;
    }

    /**
     * Get byte at index.
     * 
     * @param index
     *            the index
     * @return a byte
     */
    public byte get(final int index) {
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
            final byte[] aux = new byte[len + this.blockSize];
            System.arraycopy(this.array, 0, aux, 0, len);
            this.array = aux;
        }
        this.n += size;
        return index;
    }

    /**
     * Trim byte vector to current length.
     */
    public void trimToSize() {
        if (this.n < this.array.length) {
            final byte[] aux = new byte[this.n];
            System.arraycopy(this.array, 0, aux, 0, this.n);
            this.array = aux;
        }
    }

}
