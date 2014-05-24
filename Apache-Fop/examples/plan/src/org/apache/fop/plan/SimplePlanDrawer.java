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

/* $Id: SimplePlanDrawer.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.plan;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.fop.svg.SVGUtilities;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simple plan drawer implementation.
 */
public class SimplePlanDrawer implements PlanDrawer {

    private static final String SVG_NAMESPACE = SVGDOMImplementation.SVG_NAMESPACE_URI;

    private float fontSize;
    private HashMap hints;
    private java.awt.Font font = null;
    private boolean bord = false;
    private final float lSpace = 15;
    private float width;
    private float height;
    private float topEdge;
    private float rightEdge;

    private String[] colours;
    private String[] darkcolours;

    private Date startDate;
    private Date endDate;

    /**
     * Sets the start date.
     *
     * @param sd
     *            start date
     */
    public void setStartDate(final Date sd) {
        this.startDate = sd;
    }

    /**
     * Sets the end date.
     *
     * @param ed
     *            end date
     */
    public void setEndDate(final Date ed) {
        this.endDate = ed;
    }

    /**
     * @see org.apache.fop.plan.PlanDrawer#createDocument(EventList, float,
     *      float, HashMap)
     */
    @Override
    public Document createDocument(final EventList data, final float w,
            final float h, final HashMap hints) {
        this.width = w;
        this.height = h;
        this.hints = hints;
        this.fontSize = ((Float) hints.get(PlanHints.FONT_SIZE)).floatValue();
        this.bord = ((Boolean) hints.get(PlanHints.PLAN_BORDER)).booleanValue();

        final String title = "";

        final DOMImplementation impl = SVGDOMImplementation
                .getDOMImplementation();
        final Document doc = impl.createDocument(SVG_NAMESPACE, "svg", null);

        final Element svgRoot = doc.getDocumentElement();
        svgRoot.setAttributeNS(null, "width", Float.toString(this.width));
        svgRoot.setAttributeNS(null, "height", Float.toString(this.height));
        svgRoot.setAttributeNS(
                null,
                "viewBox",
                "0 0 " + Float.toString(this.width) + " "
                        + Float.toString(this.height));
        svgRoot.setAttributeNS(null, "style", "font-size:" + 8
                + ";font-family:" + hints.get(PlanHints.FONT_FAMILY));

        this.font = new java.awt.Font(
                (String) hints.get(PlanHints.FONT_FAMILY), java.awt.Font.PLAIN,
                (int) this.fontSize);

        if (this.bord) {
            final Element border = SVGUtilities.createRect(doc, 0, 0,
                    this.width, this.height);
            border.setAttributeNS(null, "style", "stroke:black;fill:none");
            svgRoot.appendChild(border);
        }

        final float strwidth = SVGUtilities.getStringWidth(title, this.font);

        Element text;
        float pos = (float) (80 - strwidth / 2.0);
        if (pos < 5) {
            pos = 5;
        }
        text = SVGUtilities.createText(doc, pos, 18, title);
        text.setAttributeNS(null, "style", "font-size:14");
        svgRoot.appendChild(text);

        this.topEdge = SVGUtilities.getStringHeight(title, this.font) + 5;

        // add the actual pie chart
        addPlan(doc, svgRoot, data);
        // addLegend(doc, svgRoot, data);

        return doc;
    }

    protected void addPlan(final Document doc, final Element svgRoot,
            final EventList data) {
        final Date currentDate = new Date();

        Date lastWeek = this.startDate;
        Date future = this.endDate;
        final Calendar lw = Calendar.getInstance();
        if (lastWeek == null || future == null) {
            final int dow = lw.get(Calendar.DAY_OF_WEEK);
            lw.add(Calendar.DATE, -dow - 6);
            lastWeek = lw.getTime();
            lw.add(Calendar.DATE, 5 * 7);
            future = lw.getTime();
        }
        final long totalDays = (future.getTime() - lastWeek.getTime() + 43200000) / 86400000;
        lw.setTime(lastWeek);
        final int startDay = lw.get(Calendar.DAY_OF_WEEK);

        final float graphTop = this.topEdge;
        Element g;
        Element line;
        line = SVGUtilities.createLine(doc, 0, this.topEdge, this.width,
                this.topEdge);
        line.setAttributeNS(null, "style", "fill:none;stroke:black");
        svgRoot.appendChild(line);

        final Element clip1 = SVGUtilities.createClip(
                doc,
                SVGUtilities.createPath(doc, "m0 0l126 0l0 " + this.height
                        + "l-126 0z"), "clip1");
        final Element clip2 = SVGUtilities.createClip(
                doc,
                SVGUtilities.createPath(doc, "m130 0l66 0l0 " + this.height
                        + "l-66 0z"), "clip2");
        final Element clip3 = SVGUtilities.createClip(
                doc,
                SVGUtilities.createPath(doc, "m200 0l" + (this.width - 200)
                        + " 0l0 " + this.height + "l-" + (this.width - 200)
                        + " 0z"), "clip3");
        svgRoot.appendChild(clip1);
        svgRoot.appendChild(clip2);
        svgRoot.appendChild(clip3);

        final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        Element text;
        text = SVGUtilities.createText(doc, 201, this.topEdge - 1,
                df.format(lastWeek));
        svgRoot.appendChild(text);

        text = SVGUtilities.createText(doc, this.width, this.topEdge - 1,
                df.format(future));
        text.setAttributeNS(null, "text-anchor", "end");
        svgRoot.appendChild(text);

        line = SVGUtilities
                .createLine(doc, 128, this.topEdge, 128, this.height);
        line.setAttributeNS(null, "style", "fill:none;stroke:rgb(150,150,150)");
        svgRoot.appendChild(line);
        int offset = 0;
        for (int count = startDay; count < startDay + totalDays - 1; count++) {
            offset++;
            if (count % 7 == 0 || count % 7 == 1) {
                final Element rect = SVGUtilities.createRect(doc, 200
                        + (offset - 1) * (this.width - 200) / (totalDays - 2),
                        (float) (this.topEdge + 0.5), (this.width - 200)
                        / (totalDays - 3), this.height - 1
                        - this.topEdge);
                rect.setAttributeNS(null, "style",
                        "stroke:none;fill:rgb(230,230,230)");
                svgRoot.appendChild(rect);
            }
            line = SVGUtilities.createLine(doc, 200 + (offset - 1)
                    * (this.width - 200) / (totalDays - 2),
                    (float) (this.topEdge + 0.5), 200 + (offset - 1)
                    * (this.width - 200) / (totalDays - 2),
                    (float) (this.height - 0.5));
            line.setAttributeNS(null, "style",
                    "fill:none;stroke:rgb(200,200,200)");
            svgRoot.appendChild(line);
        }

        for (int count = 0; count < data.getSize(); count++) {
            final GroupInfo gi = data.getGroupInfo(count);
            g = SVGUtilities.createG(doc);
            text = SVGUtilities.createText(doc, 1, this.topEdge + 12,
                    gi.getName());
            text.setAttributeNS(null, "style", "clip-path:url(#clip1)");
            g.appendChild(text);
            if (count > 0) {
                line = SVGUtilities.createLine(doc, 0, this.topEdge + 2,
                        this.width, this.topEdge + 2);
                line.setAttributeNS(null, "style",
                        "fill:none;stroke:rgb(100,100,100)");
                g.appendChild(line);
            }

            final float lastTop = this.topEdge;
            this.topEdge += 14;
            boolean showing = false;
            for (int count1 = 0; count1 < gi.getSize(); count1++) {
                final ActionInfo act = gi.getActionInfo(count1);
                final String name = act.getOwner();
                final String label = act.getLabel();
                text = SVGUtilities
                        .createText(doc, 8, this.topEdge + 12, label);
                text.setAttributeNS(null, "style", "clip-path:url(#clip1)");
                g.appendChild(text);

                text = SVGUtilities.createText(doc, 130, this.topEdge + 12,
                        name);
                text.setAttributeNS(null, "style", "clip-path:url(#clip2)");
                g.appendChild(text);
                final int type = act.getType();
                final Date start = act.getStartDate();
                final Date end = act.getEndDate();
                if (end.after(lastWeek) && start.before(future)) {
                    showing = true;
                    final int left = 200;
                    final int right = 500;

                    final int daysToStart = (int) ((start.getTime()
                            - lastWeek.getTime() + 43200000) / 86400000);
                    final int days = (int) ((end.getTime() - start.getTime() + 43200000) / 86400000);
                    final int daysFromEnd = (int) ((future.getTime()
                            - end.getTime() + 43200000) / 86400000);
                    Element taskGraphic;
                    switch (type) {
                    case ActionInfo.TASK:
                        taskGraphic = SVGUtilities.createRect(doc, left
                                + daysToStart * 300 / (totalDays - 2),
                                this.topEdge + 2, days * 300 / (totalDays - 2),
                                10);
                        taskGraphic
                        .setAttributeNS(null, "style",
                                "stroke:black;fill:blue;stroke-width:1;clip-path:url(#clip3)");
                        g.appendChild(taskGraphic);
                        break;
                    case ActionInfo.MILESTONE:
                        taskGraphic = SVGUtilities
                        .createPath(doc, "m "
                                + (left + daysToStart * 300
                                        / (totalDays - 2) - 6) + " "
                                        + (this.topEdge + 6) + "l6 6l6-6l-6-6z");
                        taskGraphic
                        .setAttributeNS(null, "style",
                                "stroke:black;fill:black;stroke-width:1;clip-path:url(#clip3)");
                        g.appendChild(taskGraphic);
                        text = SVGUtilities.createText(doc, left + daysToStart
                                * 300 / (totalDays - 2) + 8, this.topEdge + 9,
                                df.format(start));
                        g.appendChild(text);

                        break;
                    case ActionInfo.GROUPING:
                        taskGraphic = SVGUtilities.createPath(doc,
                                "m "
                                        + (left + daysToStart * 300
                                                / (totalDays - 2) - 6) + " "
                                                + (this.topEdge + 6) + "l6 -6l" + days
                                                * 300 / (totalDays - 2)
                                                + " 0l6 6l-6 6l-4-4l"
                                                + -(days * 300 / (totalDays - 2) - 8)
                                                + " 0l-4 4l-6-6z");
                        taskGraphic
                        .setAttributeNS(null, "style",
                                "stroke:black;fill:black;stroke-width:1;clip-path:url(#clip3)");
                        g.appendChild(taskGraphic);
                        break;
                    default:
                        break;
                    }
                }

                this.topEdge += 14;
            }
            if (showing) {
                svgRoot.appendChild(g);
            } else {
                this.topEdge = lastTop;
            }
        }
        final int currentDays = (int) ((currentDate.getTime()
                - lastWeek.getTime() + 43200000) / 86400000);

        text = SVGUtilities.createText(doc,
                (float) (200 + (currentDays + 0.5) * 300 / 35), graphTop - 1,
                df.format(currentDate));
        text.setAttributeNS(null, "text-anchor", "middle");
        text.setAttributeNS(null, "style", "stroke:rgb(100,100,100)");
        svgRoot.appendChild(text);

        line = SVGUtilities.createLine(doc,
                (float) (200 + (currentDays + 0.5) * 300 / 35), graphTop,
                (float) (200 + (currentDays + 0.5) * 300 / 35), this.height);
        line.setAttributeNS(null, "style",
                "fill:none;stroke:rgb(200,50,50);stroke-dasharray:5,5");
        svgRoot.appendChild(line);

    }
}
