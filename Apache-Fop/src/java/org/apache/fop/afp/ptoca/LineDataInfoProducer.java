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

/* $Id: LineDataInfoProducer.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.afp.ptoca;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.AFPLineDataInfo;

/**
 * {@link PtocaProducer} implementation that interprets {@link AFPLineDataInfo}
 * objects.
 */
@Slf4j
public class LineDataInfoProducer implements PtocaProducer, PtocaConstants {

    private final AFPLineDataInfo lineDataInfo;

    /**
     * Main constructor.
     *
     * @param lineDataInfo
     *            the info object
     */
    public LineDataInfoProducer(final AFPLineDataInfo lineDataInfo) {
        this.lineDataInfo = lineDataInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void produce(final PtocaBuilder builder) throws IOException {
        builder.setTextOrientation(this.lineDataInfo.getRotation());
        final int x1 = ensurePositive(this.lineDataInfo.getX1());
        final int y1 = ensurePositive(this.lineDataInfo.getY1());
        builder.absoluteMoveBaseline(y1);
        builder.absoluteMoveInline(x1);
        builder.setExtendedTextColor(this.lineDataInfo.getColor());

        final int x2 = ensurePositive(this.lineDataInfo.getX2());
        final int y2 = ensurePositive(this.lineDataInfo.getY2());
        final int thickness = this.lineDataInfo.getThickness();
        if (y1 == y2) {
            builder.drawIaxisRule(x2 - x1, thickness);
        } else if (x1 == x2) {
            builder.drawBaxisRule(y2 - y1, thickness);
        } else {
            log.error("Invalid axis rule: unable to draw line");
            return;
        }
    }

    private static int ensurePositive(final int value) {
        if (value < 0) {
            return 0;
        }
        return value;
    }

}
