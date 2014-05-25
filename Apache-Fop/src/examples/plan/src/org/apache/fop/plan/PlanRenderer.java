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

/* $Id: PlanRenderer.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.plan;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PlanRenderer {

    private static final String SVG_NAMESPACE = SVGDOMImplementation.SVG_NAMESPACE_URI;

    private String fontFamily = "sansserif";
    private float fontSize = 12;
    private String type = "";
    private String lang = "";
    private String country = "";
    private String variant = "";
    private float width;
    private float height;
    private float topEdge;
    private float rightEdge;
    private final HashMap hints = new HashMap();

    private String[] colours;
    private String[] darkcolours;

    public void setFontInfo(final String fam, final float si) {
        this.fontFamily = fam;
        this.fontSize = si;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    protected float toFloat(final String str) {
        return Float.parseFloat(str);
    }

    public Document createSVGDocument(final Document planDoc) {

        final Element svgRoot = planDoc.getDocumentElement();

        this.width = toFloat(svgRoot.getAttribute("width"));
        this.height = toFloat(svgRoot.getAttribute("height"));
        this.type = svgRoot.getAttribute("type");
        this.lang = svgRoot.getAttribute("lang");
        this.country = svgRoot.getAttribute("country");
        this.variant = svgRoot.getAttribute("variant");
        final String style = svgRoot.getAttribute("style");
        parseStyle(style);

        final Locale locale = new Locale(this.lang, this.country == null ? ""
                : this.country, this.variant == null ? "" : this.variant);

        final String start = svgRoot.getAttribute("start");
        final String end = svgRoot.getAttribute("end");
        final Date sd = getDate(start, locale);
        final Date ed = getDate(end, locale);

        String title = "";
        EventList data = null;
        final NodeList childs = svgRoot.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            final Node obj = childs.item(i);
            final String nname = obj.getNodeName();
            if (nname.equals("title")) {
                title = ((Element) obj).getFirstChild().getNodeValue();
            } else if (nname.equals("events")) {
                data = getEvents((Element) obj, locale);
            }
        }

        final SimplePlanDrawer planDrawer = new SimplePlanDrawer();
        planDrawer.setStartDate(sd);
        planDrawer.setEndDate(ed);
        this.hints.put(PlanHints.FONT_FAMILY, this.fontFamily);
        this.hints.put(PlanHints.FONT_SIZE, new Float(this.fontSize));
        this.hints.put(PlanHints.LOCALE, locale);
        final Document doc = planDrawer.createDocument(data, this.width,
                this.height, this.hints);
        return doc;
    }

    protected void parseStyle(final String style) {
        this.hints.put(PlanHints.PLAN_BORDER, new Boolean(true));
        this.hints.put(PlanHints.FONT_FAMILY, this.fontFamily);
        this.hints.put(PlanHints.FONT_SIZE, new Float(this.fontSize));
        this.hints.put(PlanHints.LABEL_FONT_SIZE, new Float(this.fontSize));
        this.hints.put(PlanHints.LABEL_FONT, this.fontFamily);
        this.hints.put(PlanHints.LABEL_TYPE, "textOnly");

        final StringTokenizer st = new StringTokenizer(style, ";");
        while (st.hasMoreTokens()) {
            final String pair = st.nextToken().trim();
            final int index = pair.indexOf(":");
            final String name = pair.substring(0, index).trim();
            final String val = pair.substring(index + 1).trim();
            if (name.equals(PlanHints.PLAN_BORDER)) {
                this.hints.put(name, Boolean.valueOf(val));
            } else if (name.equals(PlanHints.FONT_SIZE)) {
                this.hints.put(name, Float.valueOf(val));
            } else if (name.equals(PlanHints.LABEL_FONT_SIZE)) {
                this.hints.put(name, Float.valueOf(val));
            } else {
                this.hints.put(name, val);
            }
        }
    }

    public ActionInfo getActionInfo(final Element ele, final Locale locale) {
        final String t = ele.getAttribute("type");

        final NodeList childs = ele.getChildNodes();
        final ActionInfo data = new ActionInfo();
        if (t.equals("milestone")) {
            data.setType(ActionInfo.MILESTONE);
        } else if (t.equals("task")) {
            data.setType(ActionInfo.TASK);
        } else if (t.equals("grouping")) {
            data.setType(ActionInfo.GROUPING);
        } else {
            throw new IllegalArgumentException("Unknown action type: " + t);
        }

        for (int i = 0; i < childs.getLength(); i++) {
            final Node obj = childs.item(i);
            final String nname = obj.getNodeName();
            if (nname.equals("label")) {
                final String dat = ((Element) obj).getFirstChild()
                        .getNodeValue();
                data.setLabel(dat);
            } else if (nname.equals("owner")) {
                final String dat = ((Element) obj).getFirstChild()
                        .getNodeValue();
                data.setOwner(dat);
            } else if (nname.equals("startdate")) {
                final Date dat = getDate((Element) obj, locale);
                data.setStartDate(dat);
            } else if (nname.equals("enddate")) {
                final Date dat = getDate((Element) obj, locale);
                data.setEndDate(dat);
            }
        }
        return data;
    }

    public EventList getEvents(final Element ele, final Locale locale) {
        final EventList data = new EventList();
        final NodeList childs = ele.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            final Node obj = childs.item(i);
            if (obj.getNodeName().equals("group")) {
                final GroupInfo dat = getGroupInfo((Element) obj, locale);
                data.addGroupInfo(dat);
            }
        }
        return data;
    }

    public GroupInfo getGroupInfo(final Element ele, final Locale locale) {
        final NodeList childs = ele.getChildNodes();
        final GroupInfo data = new GroupInfo(ele.getAttribute("name"));
        for (int i = 0; i < childs.getLength(); i++) {
            final Node obj = childs.item(i);
            if (obj.getNodeName().equals("action")) {
                final ActionInfo dat = getActionInfo((Element) obj, locale);
                data.addActionInfo(dat);
            }
        }
        return data;
    }

    public Date getDate(final Element ele, final Locale locale) {
        final String label = ele.getFirstChild().getNodeValue();
        return getDate(label, locale);
    }

    public Date getDate(final String label, final Locale locale) {
        final Calendar cal = Calendar.getInstance(locale);

        String str;
        str = label.substring(0, 4);
        int intVal = Integer.valueOf(str).intValue();
        cal.set(Calendar.YEAR, intVal);

        str = label.substring(4, 6);
        intVal = Integer.valueOf(str).intValue();
        cal.set(Calendar.MONTH, intVal - 1);

        str = label.substring(6, 8);
        intVal = Integer.valueOf(str).intValue();
        cal.set(Calendar.DATE, intVal);
        return cal.getTime();
    }
}
