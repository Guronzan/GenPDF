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

/* $Id: PDFJavaScriptLaunchAction.java 1330451 2012-04-25 18:09:24Z vhennebert $ */

package org.apache.fop.pdf;

/**
 * PDF Action which executes some JavaScript code.
 * 
 * @since PDF 1.3
 */
public class PDFJavaScriptLaunchAction extends PDFAction {

    private final String script;

    /**
     * Creates a new /Launch action.
     * 
     * @param script
     *            the script to run when the launch action is triggered
     */
    public PDFJavaScriptLaunchAction(final String script) {
        this.script = script;
    }

    /** {@inheritDoc} */
    @Override
    public String getAction() {
        return referencePDF();
    }

    /** {@inheritDoc} */
    @Override
    public String toPDFString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append("<<\n/S /JavaScript\n/JS (");
        sb.append(this.script);
        sb.append(")\n>>");
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean contentEquals(final PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFJavaScriptLaunchAction)) {
            return false;
        }

        final PDFJavaScriptLaunchAction launch = (PDFJavaScriptLaunchAction) obj;

        if (!launch.script.toString().equals(this.script.toString())) {
            return false;
        }

        return true;
    }

}
