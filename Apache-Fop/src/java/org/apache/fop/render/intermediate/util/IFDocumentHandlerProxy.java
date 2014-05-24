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

/* $Id: IFDocumentHandlerProxy.java 1306814 2012-03-29 12:46:52Z phancock $ */

package org.apache.fop.render.intermediate.util;

import java.awt.Dimension;
import java.util.Locale;

import javax.xml.transform.Result;

import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFDocumentNavigationHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFPainter;

/**
 * This class is a simple proxy that delegates all method calls to another
 * {@link IFDocumentHandler} instance.
 */
public class IFDocumentHandlerProxy implements IFDocumentHandler {

    /** the delegate IFDocumentHandler */
    protected IFDocumentHandler delegate;

    /**
     * Creates a new proxy instance.
     * 
     * @param delegate
     *            the delegate instance
     */
    public IFDocumentHandlerProxy(final IFDocumentHandler delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsPagesOutOfOrder() {
        return this.delegate.supportsPagesOutOfOrder();
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return this.delegate.getMimeType();
    }

    /** {@inheritDoc} */
    @Override
    public void setContext(final IFContext context) {
        this.delegate.setContext(context);
    }

    /** {@inheritDoc} */
    @Override
    public IFContext getContext() {
        return this.delegate.getContext();
    }

    /** {@inheritDoc} */
    @Override
    public FontInfo getFontInfo() {
        return this.delegate.getFontInfo();
    }

    /** {@inheritDoc} */
    @Override
    public void setFontInfo(final FontInfo fontInfo) {
        this.delegate.setFontInfo(fontInfo);
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultFontInfo(final FontInfo fontInfo) {
        this.delegate.setDefaultFontInfo(fontInfo);
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandlerConfigurator getConfigurator() {
        return this.delegate.getConfigurator();
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentNavigationHandler getDocumentNavigationHandler() {
        return this.delegate.getDocumentNavigationHandler();
    }

    /** {@inheritDoc} */
    @Override
    public StructureTreeEventHandler getStructureTreeEventHandler() {
        return this.delegate.getStructureTreeEventHandler();
    }

    /** {@inheritDoc} */
    @Override
    public void setResult(final Result result) throws IFException {
        this.delegate.setResult(result);
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws IFException {
        this.delegate.startDocument();
    }

    /** {@inheritDoc} */
    @Override
    public void setDocumentLocale(final Locale locale) {
        this.delegate.setDocumentLocale(locale);

    }

    /** {@inheritDoc} */
    @Override
    public void startDocumentHeader() throws IFException {
        this.delegate.startDocumentHeader();
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentHeader() throws IFException {
        this.delegate.endDocumentHeader();
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final String id) throws IFException {
        this.delegate.startPageSequence(id);
    }

    /** {@inheritDoc} */
    @Override
    public void startPage(final int index, final String name,
            final String pageMasterName, final Dimension size)
            throws IFException {
        this.delegate.startPage(index, name, pageMasterName, size);
    }

    /** {@inheritDoc} */
    @Override
    public void startPageHeader() throws IFException {
        this.delegate.startPageHeader();
    }

    /** {@inheritDoc} */
    @Override
    public void endPageHeader() throws IFException {
        this.delegate.endPageHeader();
    }

    /** {@inheritDoc} */
    @Override
    public IFPainter startPageContent() throws IFException {
        return this.delegate.startPageContent();
    }

    /** {@inheritDoc} */
    @Override
    public void endPageContent() throws IFException {
        this.delegate.endPageContent();
    }

    /** {@inheritDoc} */
    @Override
    public void startPageTrailer() throws IFException {
        this.delegate.startPageTrailer();
    }

    /** {@inheritDoc} */
    @Override
    public void endPageTrailer() throws IFException {
        this.delegate.endPageTrailer();
    }

    /** {@inheritDoc} */
    @Override
    public void endPage() throws IFException {
        this.delegate.endPage();
    }

    /** {@inheritDoc} */
    @Override
    public void endPageSequence() throws IFException {
        this.delegate.endPageSequence();
    }

    /** {@inheritDoc} */
    @Override
    public void startDocumentTrailer() throws IFException {
        this.delegate.startDocumentTrailer();
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentTrailer() throws IFException {
        this.delegate.endDocumentTrailer();
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws IFException {
        this.delegate.endDocument();
    }

    /** {@inheritDoc} */
    @Override
    public void handleExtensionObject(final Object extension)
            throws IFException {
        this.delegate.handleExtensionObject(extension);
    }

}
