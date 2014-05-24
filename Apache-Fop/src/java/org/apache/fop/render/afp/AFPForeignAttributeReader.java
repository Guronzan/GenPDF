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

/* $Id: AFPForeignAttributeReader.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.render.afp;

import java.io.File;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceLevel;
import org.apache.fop.render.afp.extensions.AFPElementMapping;
import org.apache.xmlgraphics.util.QName;

/**
 * Parses any AFP foreign attributes
 */
@Slf4j
public class AFPForeignAttributeReader {

    /** the resource-name attribute */
    public static final QName RESOURCE_NAME = new QName(
            AFPElementMapping.NAMESPACE, "afp:resource-name");

    /** the resource-level attribute */
    public static final QName RESOURCE_LEVEL = new QName(
            AFPElementMapping.NAMESPACE, "afp:resource-level");

    /** the resource-group-file attribute */
    public static final QName RESOURCE_GROUP_FILE = new QName(
            AFPElementMapping.NAMESPACE, "afp:resource-group-file");

    /**
     * Main constructor
     */
    public AFPForeignAttributeReader() {
    }

    /**
     * Returns the resource information
     *
     * @param foreignAttributes
     *            the foreign attributes
     * @return the resource information
     */
    public AFPResourceInfo getResourceInfo(
            final Map/* <QName, String> */foreignAttributes) {
        final AFPResourceInfo resourceInfo = new AFPResourceInfo();
        if (foreignAttributes != null && !foreignAttributes.isEmpty()) {
            final String resourceName = (String) foreignAttributes
                    .get(RESOURCE_NAME);
            if (resourceName != null) {
                resourceInfo.setName(resourceName);
            }
            final AFPResourceLevel level = getResourceLevel(foreignAttributes);
            if (level != null) {
                resourceInfo.setLevel(level);
            }
        }
        return resourceInfo;
    }

    /**
     * Returns the resource level
     *
     * @param foreignAttributes
     *            the foreign attributes
     * @return the resource level
     */
    public AFPResourceLevel getResourceLevel(
            final Map/* <QName, String> */foreignAttributes) {
        AFPResourceLevel resourceLevel = null;
        if (foreignAttributes != null && !foreignAttributes.isEmpty()) {
            if (foreignAttributes.containsKey(RESOURCE_LEVEL)) {
                final String levelString = (String) foreignAttributes
                        .get(RESOURCE_LEVEL);
                resourceLevel = AFPResourceLevel.valueOf(levelString);
                // if external get resource group file attributes
                if (resourceLevel != null && resourceLevel.isExternal()) {
                    final String resourceGroupFile = (String) foreignAttributes
                            .get(RESOURCE_GROUP_FILE);
                    if (resourceGroupFile == null) {
                        final String msg = RESOURCE_GROUP_FILE
                                + " not specified";
                        log.error(msg);
                        throw new UnsupportedOperationException(msg);
                    }
                    final File resourceExternalGroupFile = new File(
                            resourceGroupFile);
                    final SecurityManager security = System
                            .getSecurityManager();
                    try {
                        if (security != null) {
                            security.checkWrite(resourceExternalGroupFile
                                    .getPath());
                        }
                    } catch (final SecurityException ex) {
                        final String msg = "unable to gain write access to external resource file: "
                                + resourceGroupFile;
                        log.error(msg);
                    }

                    try {
                        final boolean exists = resourceExternalGroupFile
                                .exists();
                        if (exists) {
                            log.warn("overwriting external resource file: "
                                    + resourceGroupFile);
                        }
                        resourceLevel.setExternalFilePath(resourceGroupFile);
                    } catch (final SecurityException ex) {
                        final String msg = "unable to gain read access to external resource file: "
                                + resourceGroupFile;
                        log.error(msg);
                    }
                }
            }
        }
        return resourceLevel;
    }
}
