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

/* $Id: GraphicsEndProlog.java 1095744 2011-04-21 15:08:29Z jeremias $ */

package org.apache.fop.afp.goca;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The end of a prolog.
 */
public class GraphicsEndProlog extends AbstractGraphicsDrawingOrder {

    /** {@inheritDoc} */
    @Override
    public void writeToStream(final OutputStream os) throws IOException {
        final byte[] data = new byte[] { getOrderCode(), // GEPROL order code
                0x00, // Reserved
        };
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    public int getDataLength() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "GraphicsEndProlog (GEPROL)";
    }

    /** {@inheritDoc} */
    @Override
    byte getOrderCode() {
        return 0x3E;
    }
}
