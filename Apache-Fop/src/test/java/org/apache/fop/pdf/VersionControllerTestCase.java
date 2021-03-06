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

/* $Id: VersionControllerTestCase.java 1302518 2012-03-19 15:56:19Z vhennebert $ */

package org.apache.fop.pdf;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * A test class for {@link VersionController}.
 */
public class VersionControllerTestCase {

    private PDFDocument doc;

    @Before
    public void setUp() {
        this.doc = new PDFDocument("test");
    }

    @Test
    public void testGetVersion() {
        // These do the same thing
        for (final Version version : Version.values()) {
            if (version.compareTo(Version.V1_4) >= 0) {
                final VersionController fixedVC = VersionController
                        .getFixedVersionController(version);
                assertEquals(version, fixedVC.getPDFVersion());
            }

            final VersionController dynamicVC = VersionController
                    .getDynamicVersionController(version, this.doc);
            assertEquals(version, dynamicVC.getPDFVersion());
        }
    }

    /**
     * Tests that the setter methods work at setting the underlying version.
     * Here there is a disparity between the two objects, the fixed version will
     * throw an exception if the setter is invoked. The dynamic version will
     * allow the version to be changed, if the new version is greater than the
     * version already set.
     */
    @Test
    public void testSetVersion() {
        // Create every type of expected PDFVersion
        for (final Version originalVersion : Version.values()) {
            // Compare against every type of Version
            for (final Version setVersion : Version.values()) {
                testDynamicController(originalVersion, setVersion);
                testFixedController(originalVersion, setVersion);
            }

        }
    }

    /**
     * The fixed implementation will throw an exception if an attempt is made to
     * change its version.
     *
     * @param originalVersion
     *            the version given to the constructor when PDFVersion
     *            instantiated
     * @param setVersion
     *            the version being set
     */
    private void testFixedController(final Version originalVersion,
            final Version setVersion) {
        if (originalVersion.compareTo(Version.V1_4) >= 0) {
            final VersionController fixedVC = VersionController
                    .getFixedVersionController(originalVersion);
            try {
                fixedVC.setPDFVersion(setVersion);
                fail("The FixedVersionController should throw an exception if an attempt to change "
                        + "the version is made");
            } catch (final IllegalStateException e) {
                // PASS
            }
            // Changes are NOT allowed, the original version is immutable
            assertEquals(originalVersion, fixedVC.getPDFVersion());
            // The document version is NEVER changed
            assertEquals(Version.V1_4, this.doc.getPDFVersion());
            // the /Version parameter shouldn't be present in the document
            // catalog
            assertNull(this.doc.getRoot().get("Version"));
        } else {
            try {
                VersionController.getFixedVersionController(originalVersion);
                fail("Versions < 1.4 aren't allowed.");
            } catch (final IllegalArgumentException e) {
                // PASS
            }
        }
    }

    /**
     * The dynamic implementation allows the version to be changed. However, the
     * version given in the constructor will be the version set in the header of
     * the PDF document. Any change to the version will then be made in the
     * document catalog.
     *
     * @param originalVersion
     *            the version given to the constructor when PDFVersion
     *            instantiated
     * @param setVersion
     *            the version being set
     */
    private void testDynamicController(final Version originalVersion,
            final Version setVersion) {
        final VersionController testSubj = VersionController
                .getDynamicVersionController(originalVersion, this.doc);
        testSubj.setPDFVersion(setVersion);
        final PDFName nameVersion = new PDFName(setVersion.toString());

        if (originalVersion.compareTo(setVersion) < 0) {
            versionShouldChange(setVersion, testSubj, nameVersion);
        } else {
            versionShouldNotChange(originalVersion, testSubj);
        }
        this.doc.getRoot().put("Version", null);
    }

    private void versionShouldNotChange(final Version originalVersion,
            final VersionController testSubj) {
        assertEquals(originalVersion, testSubj.getPDFVersion());
        assertNull(this.doc.getRoot().get("Version"));
    }

    private void versionShouldChange(final Version setVersion,
            final VersionController testSubj, final PDFName nameVersion) {
        assertEquals(setVersion, testSubj.getPDFVersion());
        assertEquals(nameVersion.toString(), this.doc.getRoot().get("Version")
                .toString());
    }
}
