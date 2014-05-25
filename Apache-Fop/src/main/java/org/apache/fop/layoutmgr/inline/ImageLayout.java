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

/* $Id: ImageLayout.java 1326053 2012-04-14 04:02:37Z gadams $ */

package org.apache.fop.layoutmgr.inline;

import java.awt.Dimension;
import java.awt.Rectangle;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.GraphicsProperties;
import org.apache.fop.fo.properties.LengthRangeProperty;

/**
 * Helper class which calculates the size and position in the viewport of an
 * image.
 */
@Slf4j
public class ImageLayout implements Constants {

    // Input
    private final GraphicsProperties props;
    private final PercentBaseContext percentBaseContext;
    private final Dimension intrinsicSize;

    // Output
    private Rectangle placement;
    private final Dimension viewportSize = new Dimension(-1, -1);
    private boolean clip;

    /**
     * Main constructor
     *
     * @param props
     *            the properties for the image
     * @param percentBaseContext
     *            the context object for percentage calculations
     * @param intrinsicSize
     *            the image's intrinsic size
     */
    public ImageLayout(final GraphicsProperties props,
            final PercentBaseContext percentBaseContext,
            final Dimension intrinsicSize) {
        this.props = props;
        this.percentBaseContext = percentBaseContext;
        this.intrinsicSize = intrinsicSize;

        doLayout();
    }

    /**
     * Does the actual calculations for the image.
     */
    protected void doLayout() {
        Length len;

        int bpd = -1;
        int ipd = -1;

        len = this.props.getBlockProgressionDimension()
                .getOptimum(this.percentBaseContext).getLength();
        if (len.getEnum() != EN_AUTO) {
            bpd = len.getValue(this.percentBaseContext);
        }
        len = this.props.getBlockProgressionDimension()
                .getMinimum(this.percentBaseContext).getLength();
        if (bpd == -1 && len.getEnum() != EN_AUTO) {
            // Establish minimum viewport size
            bpd = len.getValue(this.percentBaseContext);
        }

        len = this.props.getInlineProgressionDimension()
                .getOptimum(this.percentBaseContext).getLength();
        if (len.getEnum() != EN_AUTO) {
            ipd = len.getValue(this.percentBaseContext);
        }
        len = this.props.getInlineProgressionDimension()
                .getMinimum(this.percentBaseContext).getLength();
        if (ipd == -1 && len.getEnum() != EN_AUTO) {
            // Establish minimum viewport size
            ipd = len.getValue(this.percentBaseContext);
        }

        // if auto then use the intrinsic size of the content scaled
        // to the content-height and content-width
        boolean constrainIntrinsicSize = false;
        int cwidth = -1;
        int cheight = -1;
        len = this.props.getContentWidth();
        if (len.getEnum() != EN_AUTO) {
            switch (len.getEnum()) {
            case EN_SCALE_TO_FIT:
                if (ipd != -1) {
                    cwidth = ipd;
                }
                constrainIntrinsicSize = true;
                break;
            case EN_SCALE_DOWN_TO_FIT:
                if (ipd != -1 && this.intrinsicSize.width > ipd) {
                    cwidth = ipd;
                }
                constrainIntrinsicSize = true;
                break;
            case EN_SCALE_UP_TO_FIT:
                if (ipd != -1 && this.intrinsicSize.width < ipd) {
                    cwidth = ipd;
                }
                constrainIntrinsicSize = true;
                break;
            default:
                cwidth = len.getValue(this.percentBaseContext);
            }
        }
        len = this.props.getContentHeight();
        if (len.getEnum() != EN_AUTO) {
            switch (len.getEnum()) {
            case EN_SCALE_TO_FIT:
                if (bpd != -1) {
                    cheight = bpd;
                }
                constrainIntrinsicSize = true;
                break;
            case EN_SCALE_DOWN_TO_FIT:
                if (bpd != -1 && this.intrinsicSize.height > bpd) {
                    cheight = bpd;
                }
                constrainIntrinsicSize = true;
                break;
            case EN_SCALE_UP_TO_FIT:
                if (bpd != -1 && this.intrinsicSize.height < bpd) {
                    cheight = bpd;
                }
                constrainIntrinsicSize = true;
                break;
            default:
                cheight = len.getValue(this.percentBaseContext);
            }
        }

        Dimension constrainedIntrinsicSize;
        if (constrainIntrinsicSize) {
            constrainedIntrinsicSize = constrain(this.intrinsicSize);
        } else {
            constrainedIntrinsicSize = this.intrinsicSize;
        }

        // Derive content extents where not explicit
        final Dimension adjustedDim = adjustContentSize(cwidth, cheight,
                constrainedIntrinsicSize);
        cwidth = adjustedDim.width;
        cheight = adjustedDim.height;

        // Adjust viewport if not explicit
        if (ipd == -1) {
            ipd = constrainExtent(cwidth,
                    this.props.getInlineProgressionDimension(),
                    this.props.getContentWidth());
        }
        if (bpd == -1) {
            bpd = constrainExtent(cheight,
                    this.props.getBlockProgressionDimension(),
                    this.props.getContentHeight());
        }

        this.clip = false;
        final int overflow = this.props.getOverflow();
        if (overflow == EN_HIDDEN) {
            this.clip = true;
        } else if (overflow == EN_ERROR_IF_OVERFLOW) {
            if (cwidth > ipd || cheight > bpd) {
                // TODO Don't use logging to report error!
                log.error("Object overflows the viewport: clipping");
            }
            this.clip = true;
        }

        final int xoffset = computeXOffset(ipd, cwidth);
        final int yoffset = computeYOffset(bpd, cheight);

        // Build calculation results
        this.viewportSize.setSize(ipd, bpd);
        this.placement = new Rectangle(xoffset, yoffset, cwidth, cheight);
    }

    private int constrainExtent(int extent, final LengthRangeProperty range,
            final Length contextExtent) {
        final boolean mayScaleUp = contextExtent.getEnum() != EN_SCALE_DOWN_TO_FIT;
        final boolean mayScaleDown = contextExtent.getEnum() != EN_SCALE_UP_TO_FIT;
        Length len;
        len = range.getMaximum(this.percentBaseContext).getLength();
        if (len.getEnum() != EN_AUTO) {
            final int max = len.getValue(this.percentBaseContext);
            if (max != -1 && mayScaleDown) {
                extent = Math.min(extent, max);
            }
        }
        len = range.getMinimum(this.percentBaseContext).getLength();
        if (len.getEnum() != EN_AUTO) {
            final int min = len.getValue(this.percentBaseContext);
            if (min != -1 && mayScaleUp) {
                extent = Math.max(extent, min);
            }
        }
        return extent;
    }

    private Dimension constrain(final Dimension size) {
        final Dimension adjusted = new Dimension(size);
        final int effWidth = constrainExtent(size.width,
                this.props.getInlineProgressionDimension(),
                this.props.getContentWidth());
        final int effHeight = constrainExtent(size.height,
                this.props.getBlockProgressionDimension(),
                this.props.getContentHeight());
        final int scaling = this.props.getScaling();
        if (scaling == EN_UNIFORM) {
            final double rat1 = (double) effWidth / size.width;
            final double rat2 = (double) effHeight / size.height;
            if (rat1 < rat2) {
                adjusted.width = effWidth;
                adjusted.height = (int) (rat1 * size.height);
            } else if (rat1 > rat2) {
                adjusted.width = (int) (rat2 * size.width);
                adjusted.height = effHeight;
            } else {
                adjusted.width = effWidth;
                adjusted.height = effHeight;
            }
        } else {
            adjusted.width = effWidth;
            adjusted.height = effHeight;
        }
        return adjusted;
    }

    private Dimension adjustContentSize(final int cwidth, final int cheight,
            final Dimension defaultSize) {
        final Dimension dim = new Dimension(cwidth, cheight);
        final int scaling = this.props.getScaling();
        if (scaling == EN_UNIFORM || cwidth == -1 || cheight == -1) {
            if (cwidth == -1 && cheight == -1) {
                dim.width = defaultSize.width;
                dim.height = defaultSize.height;
            } else if (cwidth == -1) {
                if (defaultSize.height == 0) {
                    dim.width = 0;
                } else {
                    dim.width = (int) (defaultSize.width * (double) cheight / defaultSize.height);
                }
            } else if (cheight == -1) {
                if (defaultSize.width == 0) {
                    dim.height = 0;
                } else {
                    dim.height = (int) (defaultSize.height * (double) cwidth / defaultSize.width);
                }
            } else {
                // adjust the larger
                if (defaultSize.width == 0 || defaultSize.height == 0) {
                    dim.width = 0;
                    dim.height = 0;
                } else {
                    final double rat1 = (double) cwidth / defaultSize.width;
                    final double rat2 = (double) cheight / defaultSize.height;
                    if (rat1 < rat2) {
                        // reduce height
                        dim.height = (int) (rat1 * defaultSize.height);
                    } else if (rat1 > rat2) {
                        dim.width = (int) (rat2 * defaultSize.width);
                    }
                }
            }
        }
        return dim;
    }

    /**
     * Given the ipd and the content width calculates the required x offset
     * based on the text-align property
     *
     * @param ipd
     *            the inline-progression-dimension of the object
     * @param cwidth
     *            the calculated content width of the object
     * @return the X offset
     */
    public int computeXOffset(final int ipd, final int cwidth) {
        int xoffset = 0;
        switch (this.props.getTextAlign()) {
        case EN_CENTER:
            xoffset = (ipd - cwidth) / 2;
            break;
        case EN_END:
            xoffset = ipd - cwidth;
            break;
        case EN_START:
            break;
        case EN_JUSTIFY:
        default:
            break;
        }
        return xoffset;
    }

    /**
     * Given the bpd and the content height calculates the required y offset
     * based on the display-align property
     *
     * @param bpd
     *            the block-progression-dimension of the object
     * @param cheight
     *            the calculated content height of the object
     * @return the Y offset
     */
    public int computeYOffset(final int bpd, final int cheight) {
        int yoffset = 0;
        switch (this.props.getDisplayAlign()) {
        case EN_BEFORE:
            break;
        case EN_AFTER:
            yoffset = bpd - cheight;
            break;
        case EN_CENTER:
            yoffset = (bpd - cheight) / 2;
            break;
        case EN_AUTO:
        default:
            break;
        }
        return yoffset;
    }

    /**
     * Returns the placement of the image inside the viewport.
     *
     * @return the placement of the image inside the viewport (coordinates in
     *         millipoints)
     */
    public Rectangle getPlacement() {
        return this.placement;
    }

    /**
     * Returns the size of the image's viewport.
     *
     * @return the viewport size (in millipoints)
     */
    public Dimension getViewportSize() {
        return this.viewportSize;
    }

    /**
     * Returns the size of the image's intrinsic (natural) size.
     *
     * @return the intrinsic size (in millipoints)
     */
    public Dimension getIntrinsicSize() {
        return this.intrinsicSize;
    }

    /**
     * Indicates whether the image is clipped.
     *
     * @return true if the image shall be clipped
     */
    public boolean isClipped() {
        return this.clip;
    }

}
