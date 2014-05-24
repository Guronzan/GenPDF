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

/* $Id: AFPStreamer.java 1050104 2010-12-16 19:17:21Z vhennebert $ */

package org.apache.fop.afp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.modca.ResourceGroup;
import org.apache.fop.afp.modca.StreamedResourceGroup;

/**
 * Manages the streaming of the AFP output
 */
@Slf4j
public class AFPStreamer implements Streamable {

    private static final String AFPDATASTREAM_TEMP_FILE_PREFIX = "AFPDataStream_";

    private static final int BUFFER_SIZE = 4096; // 4k writing buffer

    private static final String DEFAULT_EXTERNAL_RESOURCE_FILENAME = "resources.afp";

    private final Factory factory;

    /** A mapping of external resource destinations to resource groups */
    private final Map/* <String,AFPExternalResourceGroup> */pathResourceGroupMap = new java.util.HashMap/*
     * <
     * String
     * ,
     * AFPExternalResourceGroup
     * >
     */();

    private StreamedResourceGroup printFileResourceGroup;

    /** Sets the default resource group file path */
    private String defaultResourceGroupFilePath = DEFAULT_EXTERNAL_RESOURCE_FILENAME;

    private File tempFile;

    /** temporary document outputstream */
    private OutputStream documentOutputStream;

    /** the final outputstream */
    private OutputStream outputStream;

    private RandomAccessFile documentFile;

    private DataStream dataStream;

    /**
     * Main constructor
     *
     * @param factory
     *            a factory
     */
    public AFPStreamer(final Factory factory) {
        this.factory = factory;
    }

    /**
     * Creates a new DataStream
     *
     * @param paintingState
     *            the AFP painting state
     * @return a new {@link DataStream}
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred
     */
    public DataStream createDataStream(final AFPPaintingState paintingState)
            throws IOException {
        this.tempFile = File.createTempFile(AFPDATASTREAM_TEMP_FILE_PREFIX,
                null);
        this.documentFile = new RandomAccessFile(this.tempFile, "rw");
        this.documentOutputStream = new BufferedOutputStream(
                new FileOutputStream(this.documentFile.getFD()));
        this.dataStream = this.factory.createDataStream(paintingState,
                this.documentOutputStream);
        return this.dataStream;
    }

    /**
     * Sets the default resource group file path
     *
     * @param filePath
     *            the default resource group file path
     */
    public void setDefaultResourceGroupFilePath(final String filePath) {
        this.defaultResourceGroupFilePath = filePath;
    }

    /**
     * Returns the resource group for a given resource info
     *
     * @param level
     *            a resource level
     * @return a resource group for the given resource info
     */
    public ResourceGroup getResourceGroup(final AFPResourceLevel level) {
        ResourceGroup resourceGroup = null;
        if (level.isInline()) { // no resource group for inline level
            return null;
        }
        if (level.isExternal()) {
            String filePath = level.getExternalFilePath();
            if (filePath == null) {
                log.warn("No file path provided for external resource, using default.");
                filePath = this.defaultResourceGroupFilePath;
            }
            resourceGroup = (ResourceGroup) this.pathResourceGroupMap
                    .get(filePath);
            if (resourceGroup == null) {
                OutputStream os = null;
                try {
                    os = new BufferedOutputStream(
                            new FileOutputStream(filePath));
                } catch (final FileNotFoundException fnfe) {
                    log.error("Failed to create/open external resource group file '"
                            + filePath + "'");
                } finally {
                    if (os != null) {
                        resourceGroup = this.factory
                                .createStreamedResourceGroup(os);
                        this.pathResourceGroupMap.put(filePath, resourceGroup);
                    }
                }
            }
        } else if (level.isPrintFile()) {
            if (this.printFileResourceGroup == null) {
                // use final outputstream for print-file resource group
                this.printFileResourceGroup = this.factory
                        .createStreamedResourceGroup(this.outputStream);
            }
            resourceGroup = this.printFileResourceGroup;
        } else {
            // resource group in afp document datastream
            resourceGroup = this.dataStream.getResourceGroup(level);
        }
        return resourceGroup;
    }

    /**
     * Closes off the AFP stream writing the document stream
     *
     * @throws IOException
     *             if an an I/O exception of some sort has occurred
     */
    // write out any external resource groups
    public void close() throws IOException {
        final Iterator it = this.pathResourceGroupMap.values().iterator();
        while (it.hasNext()) {
            final StreamedResourceGroup resourceGroup = (StreamedResourceGroup) it
                    .next();
            resourceGroup.close();
        }

        // close any open print-file resource group
        if (this.printFileResourceGroup != null) {
            this.printFileResourceGroup.close();
        }

        // write out document
        writeToStream(this.outputStream);

        this.outputStream.close();

        if (this.documentOutputStream != null) {
            this.documentOutputStream.close();
        }

        if (this.documentFile != null) {
            this.documentFile.close();
        }
        // delete temporary file
        this.tempFile.delete();
    }

    /**
     * Sets the final outputstream
     *
     * @param outputStream
     *            an outputstream
     */
    public void setOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToStream(final OutputStream os) throws IOException {
        // long start = System.currentTimeMillis();
        final int len = (int) this.documentFile.length();
        final int numChunks = len / BUFFER_SIZE;
        final int remainingChunkSize = len % BUFFER_SIZE;
        byte[] buffer;

        this.documentFile.seek(0);
        if (numChunks > 0) {
            buffer = new byte[BUFFER_SIZE];
            for (int i = 0; i < numChunks; i++) {
                this.documentFile.read(buffer, 0, BUFFER_SIZE);
                os.write(buffer, 0, BUFFER_SIZE);
            }
        } else {
            buffer = new byte[remainingChunkSize];
        }
        if (remainingChunkSize > 0) {
            this.documentFile.read(buffer, 0, remainingChunkSize);
            os.write(buffer, 0, remainingChunkSize);
        }
        os.flush();
        // long end = System.currentTimeMillis();
        // log.debug("writing time " + (end - start) + "ms");
    }
}
