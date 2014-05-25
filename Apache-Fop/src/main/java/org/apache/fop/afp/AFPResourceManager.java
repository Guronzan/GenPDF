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

/* $Id: AFPResourceManager.java 1195952 2011-11-01 12:20:21Z phancock $ */

package org.apache.fop.afp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.afp.fonts.AFPFont;
import org.apache.fop.afp.fonts.CharacterSet;
import org.apache.fop.afp.modca.AbstractNamedAFPObject;
import org.apache.fop.afp.modca.AbstractPageObject;
import org.apache.fop.afp.modca.IncludeObject;
import org.apache.fop.afp.modca.IncludedResourceObject;
import org.apache.fop.afp.modca.PageSegment;
import org.apache.fop.afp.modca.Registry;
import org.apache.fop.afp.modca.ResourceGroup;
import org.apache.fop.afp.modca.ResourceObject;
import org.apache.fop.afp.util.AFPResourceUtil;
import org.apache.fop.afp.util.ResourceAccessor;

/**
 * Manages the creation and storage of document resources
 */
@Slf4j
public class AFPResourceManager {

    /** The AFP datastream (document tree) */
    private DataStream dataStream;

    /** Resource creation factory */
    private final Factory factory;

    private final AFPStreamer streamer;

    private final AFPDataObjectFactory dataObjectFactory;

    /** Maintain a reference count of instream objects for referencing purposes */
    private int instreamObjectCount = 0;

    /** a mapping of resourceInfo --> include name */
    private final Map<AFPResourceInfo, String> includeNameMap = new java.util.HashMap<AFPResourceInfo, String>();

    /** a mapping of resourceInfo --> page segment name */
    private final Map<AFPResourceInfo, String> pageSegmentMap = new java.util.HashMap<AFPResourceInfo, String>();

    private final AFPResourceLevelDefaults resourceLevelDefaults = new AFPResourceLevelDefaults();

    /**
     * Main constructor
     */
    public AFPResourceManager() {
        this.factory = new Factory();
        this.streamer = new AFPStreamer(this.factory);
        this.dataObjectFactory = new AFPDataObjectFactory(this.factory);
    }

    /**
     * Sets the outputstream
     *
     * @param paintingState
     *            the AFP painting state
     * @param outputStream
     *            the outputstream
     * @return a new AFP DataStream
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred
     */
    public DataStream createDataStream(final AFPPaintingState paintingState,
            final OutputStream outputStream) throws IOException {
        this.dataStream = this.streamer.createDataStream(paintingState);
        this.streamer.setOutputStream(outputStream);
        return this.dataStream;
    }

    /**
     * Returns the AFP DataStream
     *
     * @return the AFP DataStream
     */
    public DataStream getDataStream() {
        return this.dataStream;
    }

    /**
     * Tells the streamer to write
     *
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred.
     */
    public void writeToStream() throws IOException {
        this.streamer.close();
    }

    /**
     * Sets the default resource group file path
     *
     * @param filePath
     *            the default resource group file path
     */

    public void setDefaultResourceGroupFilePath(final String filePath) {
        this.streamer.setDefaultResourceGroupFilePath(filePath);
    }

    /**
     * Tries to create an include of a data object that has been previously
     * added to the AFP data stream. If no such object was available, the method
     * returns false which serves as a signal that the object has to be created.
     *
     * @param dataObjectInfo
     *            the data object info
     * @return true if the inclusion succeeded, false if the object was not
     *         available
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred.
     */
    public boolean tryIncludeObject(final AFPDataObjectInfo dataObjectInfo)
            throws IOException {
        final AFPResourceInfo resourceInfo = dataObjectInfo.getResourceInfo();
        updateResourceInfoUri(resourceInfo);

        String objectName = this.includeNameMap.get(resourceInfo);
        if (objectName != null) {
            // an existing data resource so reference it by adding an include to
            // the current page
            includeObject(dataObjectInfo, objectName);
            return true;
        }

        objectName = this.pageSegmentMap.get(resourceInfo);
        if (objectName != null) {
            // an existing data resource so reference it by adding an include to
            // the current page
            includePageSegment(dataObjectInfo, objectName);
            return true;
        }
        return false;
    }

    /**
     * Creates a new data object in the AFP datastream
     *
     * @param dataObjectInfo
     *            the data object info
     *
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred.
     */
    public void createObject(final AFPDataObjectInfo dataObjectInfo)
            throws IOException {
        if (tryIncludeObject(dataObjectInfo)) {
            // Object has already been produced and is available by inclusion,
            // so return early.
            return;
        }

        AbstractNamedAFPObject namedObj = null;
        final AFPResourceInfo resourceInfo = dataObjectInfo.getResourceInfo();

        boolean useInclude = true;
        Registry.ObjectType objectType = null;

        // new resource so create
        if (dataObjectInfo instanceof AFPImageObjectInfo) {
            final AFPImageObjectInfo imageObjectInfo = (AFPImageObjectInfo) dataObjectInfo;
            namedObj = this.dataObjectFactory.createImage(imageObjectInfo);
        } else if (dataObjectInfo instanceof AFPGraphicsObjectInfo) {
            final AFPGraphicsObjectInfo graphicsObjectInfo = (AFPGraphicsObjectInfo) dataObjectInfo;
            namedObj = this.dataObjectFactory.createGraphic(graphicsObjectInfo);
        } else {
            // natively embedded data object
            namedObj = this.dataObjectFactory
                    .createObjectContainer(dataObjectInfo);
            objectType = dataObjectInfo.getObjectType();
            useInclude = objectType != null && objectType.isIncludable();
        }

        final AFPResourceLevel resourceLevel = resourceInfo.getLevel();
        final ResourceGroup resourceGroup = this.streamer
                .getResourceGroup(resourceLevel);

        useInclude &= resourceGroup != null;
        if (useInclude) {
            final boolean usePageSegment = dataObjectInfo.isCreatePageSegment();

            // if it is to reside within a resource group at print-file or
            // external level
            if (resourceLevel.isPrintFile() || resourceLevel.isExternal()) {
                if (usePageSegment) {
                    final String pageSegmentName = "S10"
                            + namedObj.getName().substring(3);
                    namedObj.setName(pageSegmentName);
                    final PageSegment seg = new PageSegment(pageSegmentName);
                    seg.addObject(namedObj);
                    namedObj = seg;
                }

                // wrap newly created data object in a resource object
                namedObj = this.dataObjectFactory.createResource(namedObj,
                        resourceInfo, objectType);
            }

            // add data object into its resource group destination
            resourceGroup.addObject(namedObj);

            // create the include object
            final String objectName = namedObj.getName();
            if (usePageSegment) {
                includePageSegment(dataObjectInfo, objectName);
                this.pageSegmentMap.put(resourceInfo, objectName);
            } else {
                includeObject(dataObjectInfo, objectName);
                // record mapping of resource info to data object resource name
                this.includeNameMap.put(resourceInfo, objectName);
            }
        } else {
            // not to be included so inline data object directly into the
            // current page
            this.dataStream.getCurrentPage().addObject(namedObj);
        }
    }

    private void updateResourceInfoUri(final AFPResourceInfo resourceInfo) {
        String uri = resourceInfo.getUri();
        if (uri == null) {
            uri = "/";
        }
        // if this is an instream data object adjust the uri to ensure that its
        // unique
        if (uri.endsWith("/")) {
            uri += "#" + (++this.instreamObjectCount);
            resourceInfo.setUri(uri);
        }
    }

    private void includeObject(final AFPDataObjectInfo dataObjectInfo,
            final String objectName) {
        final IncludeObject includeObject = this.dataObjectFactory
                .createInclude(objectName, dataObjectInfo);
        this.dataStream.getCurrentPage().addObject(includeObject);
    }

    /**
     * Handles font embedding. If a font is embeddable and has not already been
     * embedded it will be.
     *
     * @param afpFont
     *            the AFP font to be checked for embedding
     * @param charSet
     *            the associated character set
     * @throws IOException
     *             if there's a problem while embedding the external resources
     */
    public void embedFont(final AFPFont afpFont, final CharacterSet charSet)
            throws IOException {
        if (afpFont.isEmbeddable()) {
            // Embed fonts (char sets and code pages)
            if (charSet.getResourceAccessor() != null) {
                final ResourceAccessor accessor = charSet.getResourceAccessor();
                createIncludedResource(charSet.getName(), accessor,
                        ResourceObject.TYPE_FONT_CHARACTER_SET);
                createIncludedResource(charSet.getCodePage(), accessor,
                        ResourceObject.TYPE_CODE_PAGE);
            }
        }
    }

    private void includePageSegment(final AFPDataObjectInfo dataObjectInfo,
            final String pageSegmentName) {
        final int x = dataObjectInfo.getObjectAreaInfo().getX();
        final int y = dataObjectInfo.getObjectAreaInfo().getY();
        final AbstractPageObject currentPage = this.dataStream.getCurrentPage();
        final boolean createHardPageSegments = true;
        currentPage.createIncludePageSegment(pageSegmentName, x, y,
                createHardPageSegments);
    }

    /**
     * Creates an included resource object by loading the contained object from
     * a file.
     *
     * @param resourceName
     *            the name of the resource
     * @param accessor
     *            resource accessor to access the resource with
     * @param resourceObjectType
     *            the resource object type ({@link ResourceObject}.*)
     * @throws IOException
     *             if an I/O error occurs while loading the resource
     */
    public void createIncludedResource(final String resourceName,
            final ResourceAccessor accessor, final byte resourceObjectType)
                    throws IOException {
        URI uri;
        try {
            uri = new URI(resourceName.trim());
        } catch (final URISyntaxException e) {
            throw new IOException("Could not create URI from resource name: "
                    + resourceName + " (" + e.getMessage() + ")");
        }

        createIncludedResource(resourceName, uri, accessor, resourceObjectType);
    }

    /**
     * Creates an included resource object by loading the contained object from
     * a file.
     *
     * @param resourceName
     *            the name of the resource
     * @param uri
     *            the URI for the resource
     * @param accessor
     *            resource accessor to access the resource with
     * @param resourceObjectType
     *            the resource object type ({@link ResourceObject}.*)
     * @throws IOException
     *             if an I/O error occurs while loading the resource
     */
    public void createIncludedResource(final String resourceName,
            final URI uri, final ResourceAccessor accessor,
            final byte resourceObjectType) throws IOException {
        final AFPResourceLevel resourceLevel = new AFPResourceLevel(
                AFPResourceLevel.PRINT_FILE);

        final AFPResourceInfo resourceInfo = new AFPResourceInfo();
        resourceInfo.setLevel(resourceLevel);
        resourceInfo.setName(resourceName);
        resourceInfo.setUri(uri.toASCIIString());

        final String objectName = this.includeNameMap.get(resourceInfo);
        if (objectName == null) {
            if (log.isDebugEnabled()) {
                log.debug("Adding included resource: " + resourceName);
            }
            final IncludedResourceObject resourceContent = new IncludedResourceObject(
                    resourceName, accessor, uri);

            final ResourceObject resourceObject = this.factory
                    .createResource(resourceName);
            resourceObject.setDataObject(resourceContent);
            resourceObject.setType(resourceObjectType);

            final ResourceGroup resourceGroup = this.streamer
                    .getResourceGroup(resourceLevel);
            resourceGroup.addObject(resourceObject);
            // record mapping of resource info to data object resource name
            this.includeNameMap.put(resourceInfo, resourceName);
        } else {
            // skip, already created
        }
    }

    /**
     * Creates an included resource extracting the named resource from an
     * external source.
     *
     * @param resourceName
     *            the name of the resource
     * @param uri
     *            the URI for the resource
     * @param accessor
     *            resource accessor to access the resource with
     * @throws IOException
     *             if an I/O error occurs while loading the resource
     */
    public void createIncludedResourceFromExternal(final String resourceName,
            final URI uri, final ResourceAccessor accessor) throws IOException {

        final AFPResourceLevel resourceLevel = new AFPResourceLevel(
                AFPResourceLevel.PRINT_FILE);

        final AFPResourceInfo resourceInfo = new AFPResourceInfo();
        resourceInfo.setLevel(resourceLevel);
        resourceInfo.setName(resourceName);
        resourceInfo.setUri(uri.toASCIIString());

        final String resource = this.includeNameMap.get(resourceInfo);
        if (resource == null) {

            final ResourceGroup resourceGroup = this.streamer
                    .getResourceGroup(resourceLevel);

            // resourceObject delegates write commands to copyNamedResource()
            // The included resource may already be wrapped in a resource object
            final AbstractNamedAFPObject resourceObject = new AbstractNamedAFPObject(
                    null) {

                @Override
                protected void writeContent(final OutputStream os)
                        throws IOException {
                    InputStream inputStream = null;
                    try {
                        inputStream = accessor.createInputStream(uri);
                        final BufferedInputStream bin = new BufferedInputStream(
                                inputStream);
                        AFPResourceUtil
                        .copyNamedResource(resourceName, bin, os);
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                    }
                }

                // bypass super.writeStart
                @Override
                protected void writeStart(final OutputStream os)
                        throws IOException {
                }

                // bypass super.writeEnd
                @Override
                protected void writeEnd(final OutputStream os)
                        throws IOException {
                }
            };

            resourceGroup.addObject(resourceObject);

            this.includeNameMap.put(resourceInfo, resourceName);

        }
    }

    /**
     * Sets resource level defaults. The existing defaults over merged with the
     * ones passed in as parameter.
     *
     * @param defaults
     *            the new defaults
     */
    public void setResourceLevelDefaults(final AFPResourceLevelDefaults defaults) {
        this.resourceLevelDefaults.mergeFrom(defaults);
    }

    /**
     * Returns the resource level defaults in use with this resource manager.
     *
     * @return the resource level defaults
     */
    public AFPResourceLevelDefaults getResourceLevelDefaults() {
        return this.resourceLevelDefaults;
    }

}
