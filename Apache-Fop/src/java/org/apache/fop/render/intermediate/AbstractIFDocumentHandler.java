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

/* $Id: AbstractIFDocumentHandler.java 1296496 2012-03-02 22:19:46Z gadams $ */

package org.apache.fop.render.intermediate;

import java.util.Locale;

import org.apache.fop.accessibility.DummyStructureTreeEventHandler;
import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.apps.FOUserAgent;

/**
 * Abstract base class for {@link IFDocumentHandler} implementations.
 */
public abstract class AbstractIFDocumentHandler implements IFDocumentHandler {

    private IFContext ifContext;

    /**
     * Default constructor.
     */
    public AbstractIFDocumentHandler() {
    }

    /** {@inheritDoc} */
    @Override
    public void setContext(final IFContext context) {
        this.ifContext = context;
    }

    /** {@inheritDoc} */
    @Override
    public IFContext getContext() {
        return this.ifContext;
    }

    /**
     * Returns the associated user agent.
     * 
     * @return the user agent
     */
    public FOUserAgent getUserAgent() {
        return getContext().getUserAgent();
    }

    /** {@inheritDoc} */
    @Override
    public StructureTreeEventHandler getStructureTreeEventHandler() {
        return DummyStructureTreeEventHandler.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentNavigationHandler getDocumentNavigationHandler() {
        return null; // By default, this is not supported
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws IFException {
        if (getUserAgent() == null) {
            throw new IllegalStateException(
                    "User agent must be set before starting document generation");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setDocumentLocale(final Locale locale) {
    }

    /** {@inheritDoc} */
    @Override
    public void startDocumentHeader() throws IFException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentHeader() throws IFException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void startDocumentTrailer() throws IFException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentTrailer() throws IFException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void startPageHeader() throws IFException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void endPageHeader() throws IFException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void startPageTrailer() throws IFException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void endPageTrailer() throws IFException {
        // nop
    }
}
