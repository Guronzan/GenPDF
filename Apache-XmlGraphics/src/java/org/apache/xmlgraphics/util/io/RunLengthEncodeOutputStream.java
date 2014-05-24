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

/* $Id: RunLengthEncodeOutputStream.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.util.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class applies a RunLengthEncode filter to the stream.
 *
 * @version $Id: RunLengthEncodeOutputStream.java 1345683 2012-06-03 14:50:33Z
 *          gadams $
 *
 *          Originally authored by Stephen Wolke.
 */
public class RunLengthEncodeOutputStream extends FilterOutputStream implements
        Finalizable {

    private static final int MAX_SEQUENCE_COUNT = 127;
    private static final int END_OF_DATA = 128;
    private static final int BYTE_MAX = 256;

    private static final int NOT_IDENTIFY_SEQUENCE = 0;
    private static final int START_SEQUENCE = 1;
    private static final int IN_SEQUENCE = 2;
    private static final int NOT_IN_SEQUENCE = 3;

    private int runCount = 0;
    private int isSequence = NOT_IDENTIFY_SEQUENCE;
    private final byte[] runBuffer = new byte[MAX_SEQUENCE_COUNT + 1];

    /** @see java.io.FilterOutputStream **/
    public RunLengthEncodeOutputStream(final OutputStream out) {
        super(out);
    }

    /** @see java.io.FilterOutputStream **/
    public void write(final byte b) throws java.io.IOException {
        this.runBuffer[this.runCount] = b;

        switch (this.runCount) {
        case 0:
            this.runCount = 0;
            this.isSequence = NOT_IDENTIFY_SEQUENCE;
            this.runCount++;
            break;
        case 1:
            if (this.runBuffer[this.runCount] != this.runBuffer[this.runCount - 1]) {
                this.isSequence = NOT_IN_SEQUENCE;
            }
            this.runCount++;
            break;
        case 2:
            if (this.runBuffer[this.runCount] != this.runBuffer[this.runCount - 1]) {
                this.isSequence = NOT_IN_SEQUENCE;
            } else {
                if (this.isSequence == NOT_IN_SEQUENCE) {
                    this.isSequence = START_SEQUENCE;
                } else {
                    this.isSequence = IN_SEQUENCE;
                }
            }
            this.runCount++;
            break;
        case MAX_SEQUENCE_COUNT:
            if (this.isSequence == IN_SEQUENCE) {
                this.out.write(BYTE_MAX - (MAX_SEQUENCE_COUNT - 1));
                this.out.write(this.runBuffer[this.runCount - 1]);
                this.runBuffer[0] = this.runBuffer[this.runCount];
                this.runCount = 1;
            } else {
                this.out.write(MAX_SEQUENCE_COUNT);
                this.out.write(this.runBuffer, 0, this.runCount + 1);
                this.runCount = 0;
            }
            this.isSequence = NOT_IDENTIFY_SEQUENCE;
            break;
        default:
            switch (this.isSequence) {
            case NOT_IN_SEQUENCE:
                if (this.runBuffer[this.runCount] == this.runBuffer[this.runCount - 1]) {
                    this.isSequence = START_SEQUENCE;
                }
                this.runCount++;
                break;
            case START_SEQUENCE:
                if (this.runBuffer[this.runCount] == this.runBuffer[this.runCount - 1]) {
                    this.out.write(this.runCount - 3);
                    this.out.write(this.runBuffer, 0, this.runCount - 2);
                    this.runBuffer[0] = this.runBuffer[this.runCount];
                    this.runBuffer[1] = this.runBuffer[this.runCount];
                    this.runBuffer[2] = this.runBuffer[this.runCount];
                    this.runCount = 3;
                    this.isSequence = IN_SEQUENCE;
                    break;
                } else {
                    this.isSequence = NOT_IN_SEQUENCE;
                    this.runCount++;
                    break;
                }
            case IN_SEQUENCE:
            default:
                if (this.runBuffer[this.runCount] != this.runBuffer[this.runCount - 1]) {
                    this.out.write(BYTE_MAX - (this.runCount - 1));
                    this.out.write(this.runBuffer[this.runCount - 1]);
                    this.runBuffer[0] = this.runBuffer[this.runCount];
                    this.runCount = 1;
                    this.isSequence = NOT_IDENTIFY_SEQUENCE;
                    break;
                }
                this.runCount++;
                break;
            }
        }
    }

    /** @see java.io.FilterOutputStream **/
    @Override
    public void write(final byte[] b) throws IOException {

        for (int i = 0; i < b.length; i++) {
            this.write(b[i]);
        }
    }

    /** @see java.io.FilterOutputStream **/
    @Override
    public void write(final byte[] b, final int off, final int len)
            throws IOException {

        for (int i = 0; i < len; i++) {
            this.write(b[off + i]);
        }
    }

    /** @see Finalizable **/
    @Override
    public void finalizeStream() throws IOException {
        switch (this.isSequence) {
        case IN_SEQUENCE:
            this.out.write(BYTE_MAX - (this.runCount - 1));
            this.out.write(this.runBuffer[this.runCount - 1]);
            break;
        default:
            this.out.write(this.runCount - 1);
            this.out.write(this.runBuffer, 0, this.runCount);
        }

        this.out.write(END_OF_DATA);

        flush();
        if (this.out instanceof Finalizable) {
            ((Finalizable) this.out).finalizeStream();
        }
    }

    /** @see java.io.FilterOutputStream **/
    @Override
    public void close() throws IOException {
        finalizeStream();
        super.close();
    }

}
