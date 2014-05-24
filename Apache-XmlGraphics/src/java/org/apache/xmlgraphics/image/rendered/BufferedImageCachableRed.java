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

/* $Id: BufferedImageCachableRed.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.rendered;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import org.apache.xmlgraphics.image.GraphicsUtil;

// CSOFF: NeedBraces
// CSOFF: WhitespaceAfter
// CSOFF: WhitespaceAround

/**
 * This implements CachableRed based on a BufferedImage. You can use this to
 * wrap a BufferedImage that you want to appear as a CachableRed. It essentially
 * ignores the dependency and dirty region methods.
 *
 * Originally authored by Thomas DeWeese.
 * 
 * @version $Id: BufferedImageCachableRed.java 1345683 2012-06-03 14:50:33Z
 *          gadams $
 */
public class BufferedImageCachableRed extends AbstractRed {
    // The bufferedImage that we wrap...
    BufferedImage bi;

    /**
     * Construct an instance of CachableRed around a BufferedImage.
     */
    public BufferedImageCachableRed(final BufferedImage bi) {
        super((CachableRed) null, new Rectangle(bi.getMinX(), bi.getMinY(),
                bi.getWidth(), bi.getHeight()), bi.getColorModel(), bi
                .getSampleModel(), bi.getMinX(), bi.getMinY(), null);

        this.bi = bi;
    }

    public BufferedImageCachableRed(final BufferedImage bi, final int xloc,
            final int yloc) {
        super((CachableRed) null, new Rectangle(xloc, yloc, bi.getWidth(),
                bi.getHeight()), bi.getColorModel(), bi.getSampleModel(), xloc,
                yloc, null);

        this.bi = bi;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(getMinX(), getMinY(), getWidth(), getHeight());
    }

    /**
     * fetch the bufferedImage from this node.
     */
     public BufferedImage getBufferedImage() {
        return this.bi;
     }

     @Override
     public Object getProperty(final String name) {
         return this.bi.getProperty(name);
     }

     @Override
     public String[] getPropertyNames() {
         return this.bi.getPropertyNames();
     }

     @Override
     public Raster getTile(final int tileX, final int tileY) {
         return this.bi.getTile(tileX, tileY);
     }

     @Override
     public Raster getData() {
         final Raster r = this.bi.getData();
         return r.createTranslatedChild(getMinX(), getMinY());
     }

     @Override
     public Raster getData(final Rectangle rect) {
         Rectangle r = (Rectangle) rect.clone();

         if (!r.intersects(getBounds())) {
             return null;
         }
         r = r.intersection(getBounds());
         r.translate(-getMinX(), -getMinY());

         final Raster ret = this.bi.getData(r);
         return ret.createTranslatedChild(ret.getMinX() + getMinX(),
                ret.getMinY() + getMinY());
     }

     @Override
     public WritableRaster copyData(final WritableRaster wr) {
         final WritableRaster wr2 = wr.createWritableTranslatedChild(
                wr.getMinX() - getMinX(), wr.getMinY() - getMinY());

         GraphicsUtil.copyData(this.bi.getRaster(), wr2);

         /*
         * This was the original code. This is _bad_ since it causes a multiply
         * and divide of the alpha channel to do the draw operation. I believe
         * that at some point I switched to drawImage in order to avoid some
         * issues with BufferedImage's copyData implementation but I can't
         * reproduce them now. Anyway I'm now using GraphicsUtil which should
         * generally be as fast if not faster...
         */
         /*
         * BufferedImage dest; dest = new BufferedImage(bi.getColorModel(),
         * wr.createWritableTranslatedChild(0,0),
         * bi.getColorModel().isAlphaPremultiplied(), null); java.awt.Graphics2D
         * g2d = dest.createGraphics(); g2d.drawImage(bi, null,
         * getMinX()-wr.getMinX(), getMinY()-wr.getMinY()); g2d.dispose();
         */
         return wr;
     }
}
