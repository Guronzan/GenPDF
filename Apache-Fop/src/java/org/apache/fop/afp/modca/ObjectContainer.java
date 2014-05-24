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

/* $Id: ObjectContainer.java 1195952 2011-11-01 12:20:21Z phancock $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPObjectAreaInfo;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceLevel;
import org.apache.fop.afp.Factory;
import org.apache.fop.afp.modca.triplets.MappingOptionTriplet;
import org.apache.fop.afp.util.BinaryUtils;

/**
 * Object containers are MO:DCA objects that envelop and carry object data.
 */
public class ObjectContainer extends AbstractDataObject {

    /** the object container data maximum length */
    private static final int MAX_DATA_LEN = 32759;

    private byte[] data;

    /**
     * Main constructor
     *
     * @param factory
     *            the object factory
     * @param name
     *            the name of this object container
     */
    public ObjectContainer(final Factory factory, final String name) {
        super(factory, name);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeStart(final OutputStream os) throws IOException {
        final byte[] headerData = new byte[17];
        copySF(headerData, Type.BEGIN, Category.OBJECT_CONTAINER);

        // Set the total record length
        final int containerLen = headerData.length + getTripletDataLength() - 1;
        final byte[] len = BinaryUtils.convert(containerLen, 2);
        headerData[1] = len[0]; // Length byte 1
        headerData[2] = len[1]; // Length byte 2

        os.write(headerData);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final OutputStream os) throws IOException {
        super.writeContent(os); // write triplets and OEG

        // write OCDs
        final byte[] dataHeader = new byte[9];
        copySF(dataHeader, SF_CLASS, Type.DATA, Category.OBJECT_CONTAINER);
        final int lengthOffset = 1;

        if (this.data != null) {
            writeChunksToStream(this.data, dataHeader, lengthOffset,
                    MAX_DATA_LEN, os);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEnd(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.END, Category.OBJECT_CONTAINER);
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    public void setViewport(final AFPDataObjectInfo dataObjectInfo) {
        final AFPResourceInfo resourceInfo = dataObjectInfo.getResourceInfo();
        final AFPResourceLevel resourceLevel = resourceInfo.getLevel();

        // only need to set MCD and CDD when OC when is inlined (pre-2000 apps)
        if (resourceLevel.isInline()) {
            super.setViewport(dataObjectInfo);

            final MapContainerData mapContainerData = this.factory
                    .createMapContainerData(MappingOptionTriplet.SCALE_TO_FIT);
            getObjectEnvironmentGroup().setMapContainerData(mapContainerData);

            final int dataWidth = dataObjectInfo.getDataWidth();
            final int dataHeight = dataObjectInfo.getDataHeight();

            final AFPObjectAreaInfo objectAreaInfo = dataObjectInfo
                    .getObjectAreaInfo();
            final int widthRes = objectAreaInfo.getWidthRes();
            final int heightRes = objectAreaInfo.getHeightRes();

            final ContainerDataDescriptor containerDataDescriptor = this.factory
                    .createContainerDataDescriptor(dataWidth, dataHeight,
                            widthRes, heightRes);
            getObjectEnvironmentGroup().setDataDescriptor(
                    containerDataDescriptor);
        }
    }

    /**
     * Sets the data for the object container
     *
     * @param data
     *            a byte array
     */
    public void setData(final byte[] data) {
        this.data = data;
    }
}
