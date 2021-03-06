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

/* $Id: PDFRoot.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import org.apache.fop.util.LanguageTags;

/**
 * Class representing a Root (/Catalog) object.
 */
public class PDFRoot extends PDFDictionary {

    /**
     * Use no page mode setting, default
     */
    public static final int PAGEMODE_USENONE = 0;

    /**
     * Use outlines page mode to show bookmarks
     */
    public static final int PAGEMODE_USEOUTLINES = 1;

    /**
     * Use thumbs page mode to show thumbnail images
     */
    public static final int PAGEMODE_USETHUMBS = 2;

    /**
     * Full screen page mode
     */
    public static final int PAGEMODE_FULLSCREEN = 3;

    private static final PDFName[] PAGEMODE_NAMES = new PDFName[] {
            new PDFName("UseNone"), new PDFName("UseOutlines"),
            new PDFName("UseThumbs"), new PDFName("FullScreen"), };

    /**
     * create a Root (/Catalog) object. NOTE: The PDFRoot object must be created
     * before the PDF document is generated, but it is not assigned an object ID
     * until it is about to be written (immediately before the xref table as
     * part of the trailer). (mark-fop@inomial.com)
     *
     * @param objnum
     *            the object's number
     * @param pages
     *            the PDFPages object
     */
    public PDFRoot(final int objnum, final PDFPages pages) {
        super();
        setObjectNumber(objnum);
        put("Type", new PDFName("Catalog"));
        setRootPages(pages);
        setLanguage("x-unknown");
    }

    /** {@inheritDoc} */
    @Override
    public int output(final OutputStream stream) throws IOException {
        getDocument().getProfile().verifyTaggedPDF();
        return super.output(stream);
    }

    /**
     * Set the page mode for the PDF document.
     *
     * @param mode
     *            the page mode (one of PAGEMODE_*)
     */
    public void setPageMode(final int mode) {
        put("PageMode", PAGEMODE_NAMES[mode]);
    }

    /**
     * Returns the currently active /PageMode.
     * 
     * @return the /PageMode (one of PAGEMODE_*)
     */
    public int getPageMode() {
        final PDFName mode = (PDFName) get("PageMode");
        if (mode != null) {
            for (int i = 0; i < PAGEMODE_NAMES.length; i++) {
                if (PAGEMODE_NAMES[i].equals(mode)) {
                    return i;
                }
            }
            throw new IllegalStateException("Unknown /PageMode encountered: "
                    + mode);
        } else {
            return PAGEMODE_USENONE;
        }
    }

    /**
     * add a /Page object to the root /Pages object
     *
     * @param page
     *            the /Page object to add
     */
    public void addPage(final PDFPage page) {
        final PDFPages pages = getRootPages();
        pages.addPage(page);
    }

    /**
     * set the root /Pages object
     *
     * @param pages
     *            the /Pages object to set as root
     */
    public void setRootPages(final PDFPages pages) {
        put("Pages", pages.makeReference());
    }

    /**
     * Returns the /PageLabels object.
     * 
     * @return the /PageLabels object if set, null otherwise.
     * @since PDF 1.3
     */
    public PDFPages getRootPages() {
        final PDFReference ref = (PDFReference) get("Pages");
        return ref != null ? (PDFPages) ref.getObject() : null;
    }

    /**
     * Sets the /PageLabels object.
     * 
     * @param pageLabels
     *            the /PageLabels object
     */
    public void setPageLabels(final PDFPageLabels pageLabels) {
        put("PageLabels", pageLabels.makeReference());
    }

    /**
     * Returns the /PageLabels object.
     * 
     * @return the /PageLabels object if set, null otherwise.
     * @since PDF 1.3
     */
    public PDFPageLabels getPageLabels() {
        final PDFReference ref = (PDFReference) get("PageLabels");
        return ref != null ? (PDFPageLabels) ref.getObject() : null;
    }

    /**
     * Set the root outline for the PDF document.
     *
     * @param out
     *            the root PDF Outline
     */
    public void setRootOutline(final PDFOutline out) {
        put("Outlines", out.makeReference());

        // Set /PageMode to /UseOutlines by default if no other mode has been
        // set
        final PDFName mode = (PDFName) get("PageMode");
        if (mode == null) {
            setPageMode(PAGEMODE_USEOUTLINES);
        }
    }

    /**
     * Get the root PDF outline for the document.
     *
     * @return the root PDF Outline
     */
    public PDFOutline getRootOutline() {
        final PDFReference ref = (PDFReference) get("Outlines");
        return ref != null ? (PDFOutline) ref.getObject() : null;
    }

    /**
     * Set the /Names object.
     * 
     * @param names
     *            the Names object
     * @since PDF 1.2
     */
    public void setNames(final PDFNames names) {
        put("Names", names.makeReference());
    }

    /**
     * Returns the /Names object.
     * 
     * @return the Names object if set, null otherwise.
     * @since PDF 1.2
     */
    public PDFNames getNames() {
        final PDFReference ref = (PDFReference) get("Names");
        return ref != null ? (PDFNames) ref.getObject() : null;
    }

    /**
     * Set the optional Metadata object.
     * 
     * @param meta
     *            the Metadata object
     * @since PDF 1.4
     */
    public void setMetadata(final PDFMetadata meta) {
        if (getDocumentSafely().getPDFVersion().compareTo(Version.V1_4) >= 0) {
            put("Metadata", meta.makeReference());
        }
    }

    /**
     * Returns the /Metadata object
     * 
     * @return the /Metadata object if set, null otherwise.
     * @since PDF 1.4
     */
    public PDFMetadata getMetadata() {
        final PDFReference ref = (PDFReference) get("Metadata");
        return ref != null ? (PDFMetadata) ref.getObject() : null;
    }

    /**
     * Returns the /OutputIntents array.
     * 
     * @return the /OutputIntents array or null if it doesn't exist
     * @since PDF 1.4
     */
    public PDFArray getOutputIntents() {
        return (PDFArray) get("OutputIntents");
    }

    /**
     * Adds an OutputIntent to the PDF
     * 
     * @param outputIntent
     *            the OutputIntent dictionary
     * @since PDF 1.4
     */
    public void addOutputIntent(final PDFOutputIntent outputIntent) {
        if (getDocumentSafely().getPDFVersion().compareTo(Version.V1_4) >= 0) {
            PDFArray outputIntents = getOutputIntents();
            if (outputIntents == null) {
                outputIntents = new PDFArray(this);
                put("OutputIntents", outputIntents);
            }
            outputIntents.add(outputIntent);
        }
    }

    /**
     * Sets the "Version" entry. If this version is greater than that specified
     * in the header, this version takes precedence.
     *
     * @param version
     *            the PDF document version
     * @since PDF 1.4
     */
    void setVersion(final Version version) {
        put("Version", new PDFName(version.toString()));
    }

    /**
     * Returns the language identifier of the document.
     * 
     * @return the language identifier of the document (or null if not set or
     *         undefined)
     * @since PDF 1.4
     */
    public String getLanguage() {
        return (String) get("Lang");
    }

    /**
     * Sets the locale of the document.
     * 
     * @param locale
     *            the locale of the document.
     */
    public void setLanguage(final Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale must not be null");
        }
        setLanguage(LanguageTags.toLanguageTag(locale));
    }

    private void setLanguage(final String lang) {
        put("Lang", lang);
    }

    /**
     * Sets the StructTreeRoot object. Used for accessibility.
     * 
     * @param structTreeRoot
     *            of this document
     */
    public void setStructTreeRoot(final PDFStructTreeRoot structTreeRoot) {
        if (structTreeRoot == null) {
            throw new NullPointerException("structTreeRoot must not be null");
        }
        put("StructTreeRoot", structTreeRoot);
    }

    /**
     * Returns the StructTreeRoot object.
     * 
     * @return the structure tree root (or null if accessibility is not enabled)
     */
    public PDFStructTreeRoot getStructTreeRoot() {
        return (PDFStructTreeRoot) get("StructTreeRoot");
    }

    /**
     * Marks this document as conforming to the Tagged PDF conventions.
     */
    public void makeTagged() {
        final PDFDictionary dict = new PDFDictionary();
        dict.put("Marked", Boolean.TRUE);
        put("MarkInfo", dict); // new PDFMarkInfo()
    }

    /**
     * Returns the MarkInfo dictionary.
     * 
     * @return the MarkInfo dictionary (or null if it's not present)
     */
    public PDFDictionary getMarkInfo() {
        return (PDFDictionary) get("MarkInfo");
    }
}
