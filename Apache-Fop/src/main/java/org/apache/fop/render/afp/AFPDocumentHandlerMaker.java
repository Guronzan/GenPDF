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

/* $Id: AFPDocumentHandlerMaker.java 779046 2009-05-27 08:16:36Z cbowditch $ */

package org.apache.fop.render.afp;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.render.intermediate.AbstractIFDocumentHandlerMaker;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;

/**
 * Intermediate format document handler factory for AFP output.
 */
public class AFPDocumentHandlerMaker extends AbstractIFDocumentHandlerMaker {

    private static final String[] MIMES = new String[] {
        MimeConstants.MIME_AFP, MimeConstants.MIME_AFP_ALT };

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandler makeIFDocumentHandler(final FOUserAgent ua) {
        final AFPDocumentHandler handler = new AFPDocumentHandler();
        handler.setContext(new IFContext(ua));
        return handler;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsOutputStream() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getSupportedMimeTypes() {
        return MIMES;
    }

}