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
package org.apache.fop.render.ps;

import java.io.IOException;

import org.apache.fop.render.intermediate.IFState;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.verification.VerificationMode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PSPainterTestCase {

    private PSDocumentHandler docHandler;
    private PSPainter psPainter;
    private PSGenerator gen;
    private IFState state;

    @Before
    public void setup() {
        this.docHandler = new PSDocumentHandler();
        this.gen = mock(PSGenerator.class);
        this.docHandler.gen = this.gen;
        this.state = IFState.create();
        this.psPainter = new PSPainter(this.docHandler, this.state);
    }

    @Test
    public void testNonZeroFontSize() throws IOException {
        testFontSize(6, times(1));
    }

    @Test
    public void testZeroFontSize() throws IOException {
        testFontSize(0, never());
    }

    private void testFontSize(final int fontSize, final VerificationMode test)
            throws IOException {
        this.state.setFontSize(fontSize);
        try {
            this.psPainter.drawText(10, 10, 2, 2, null, "Test");
        } catch (final Exception ex) {
            // Expected
        }
        verify(this.gen, test).useColor(this.state.getTextColor());
    }
}
