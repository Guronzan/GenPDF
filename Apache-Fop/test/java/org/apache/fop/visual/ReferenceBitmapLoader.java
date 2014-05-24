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

/* $Id: ReferenceBitmapLoader.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.visual;

import java.awt.image.BufferedImage;
import java.io.File;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * BitmapProducer implementation that simply loads preproduced reference bitmaps
 * from a certain directory.
 * <p>
 * Here's what the configuration element looks like for the class:
 * <p>
 *
 * <pre>
 * <producer classname="org.apache.fop.visual.ReferenceBitmapLoader">
 *   <directory>C:/Temp/ref-bitmaps</directory>
 * </producer>
 * </pre>
 */
@Slf4j
public class ReferenceBitmapLoader extends AbstractBitmapProducer implements
Configurable {

    private File bitmapDirectory;

    /** @see org.apache.avalon.framework.configuration.Configurable */
    @Override
    public void configure(final Configuration cfg)
            throws ConfigurationException {
        this.bitmapDirectory = new File(cfg.getChild("directory")
                .getValue(null));
        if (!this.bitmapDirectory.exists()) {
            throw new ConfigurationException("Directory could not be found: "
                    + this.bitmapDirectory);
        }
    }

    /** @see org.apache.fop.visual.BitmapProducer */
    @Override
    public BufferedImage produce(final File src, final int index,
            final ProducerContext context) {
        try {
            final File bitmap = new File(this.bitmapDirectory, src.getName()
                    + ".png");
            if (bitmap.exists()) {
                return BitmapComparator.getImage(bitmap);
            } else {
                return null;
            }
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
