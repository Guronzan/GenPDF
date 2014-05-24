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

/* $Id$ */

package org.apache.fop.complexscripts.scripts.arabic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.IntBuffer;
import java.util.List;

import org.apache.fop.complexscripts.fonts.GlyphPositioningTable;
import org.apache.fop.complexscripts.fonts.GlyphSubstitutionTable;
import org.apache.fop.complexscripts.fonts.ttx.TTXFile;
import org.apache.fop.complexscripts.util.GlyphSequence;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for functionality related to the arabic script.
 */
public class ArabicTestCase implements ArabicTestConstants {

    @Test
    public void testArabicWordForms() {
        for (final String sfn : srcFiles) {
            try {
                processWordForms(new File(datFilesDir));
            } catch (final Exception e) {
                fail(e.getMessage());
            }
        }
    }

    private void processWordForms(final File dfd) {
        final String[] files = listWordFormFiles(dfd);
        for (final String fn : files) {
            final File dff = new File(dfd, fn);
            processWordForms(dff.getAbsolutePath());
        }
    }

    private String[] listWordFormFiles(final File dfd) {
        return dfd.list(new FilenameFilter() {
            @Override
            public boolean accept(final File f, final String name) {
                return hasPrefixFrom(name, srcFiles)
                        && hasExtension(name, WF_FILE_DAT_EXT);
            }

            private boolean hasPrefixFrom(final String name,
                    final String[] prefixes) {
                for (final String p : prefixes) {
                    if (name.startsWith(p)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean hasExtension(final String name,
                    final String extension) {
                return name.endsWith("." + extension);
            }
        });
    }

    private void processWordForms(final String dpn) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(dpn);
            if (fis != null) {
                final ObjectInputStream ois = new ObjectInputStream(fis);
                final List<Object[]> data = (List<Object[]>) ois.readObject();
                if (data != null) {
                    processWordForms(data);
                }
                ois.close();
            }
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (final Exception e) {
                }
            }
        }
    }

    private void processWordForms(final List<Object[]> data) {
        assert data != null;
        assert data.size() > 0;
        String script = null;
        String language = null;
        String tfn = null;
        TTXFile tf = null;
        GlyphSubstitutionTable gsub = null;
        GlyphPositioningTable gpos = null;
        int[] widths = null;
        for (final Object[] d : data) {
            if (script == null) {
                assert d.length >= 4;
                script = (String) d[0];
                language = (String) d[1];
                tfn = (String) d[3];
                tf = TTXFile.getFromCache(ttxFontsDir + File.separator + tfn);
                assertTrue(tf != null);
                gsub = tf.getGSUB();
                assertTrue(gsub != null);
                gpos = tf.getGPOS();
                assertTrue(gpos != null);
                widths = tf.getWidths();
                assertTrue(widths != null);
            } else {
                assert tf != null;
                assert gsub != null;
                assert gpos != null;
                assert tfn != null;
                assert d.length >= 4;
                final String wf = (String) d[0];
                final int[] iga = (int[]) d[1];
                final int[] oga = (int[]) d[2];
                final int[][] paa = (int[][]) d[3];
                final GlyphSequence tigs = tf.mapCharsToGlyphs(wf);
                assertSameGlyphs(iga, getGlyphs(tigs), "input glyphs", wf, tfn);
                final GlyphSequence togs = gsub.substitute(tigs, script,
                        language);
                assertSameGlyphs(oga, getGlyphs(togs), "output glyphs", wf, tfn);
                final int[][] tpaa = new int[togs.getGlyphCount()][4];
                if (gpos.position(togs, script, language, 1000, widths, tpaa)) {
                    assertSameAdjustments(paa, tpaa, wf, tfn);
                } else if (paa != null) {
                    assertEquals("unequal adjustment count, word form(" + wf
                            + "), font (" + tfn + ")", paa.length, 0);
                }
            }
        }
    }

    private void assertSameGlyphs(final int[] expected, final int[] actual,
            final String label, final String wf, final String tfn) {
        assertEquals(label + ": unequal glyph count, word form(" + wf
                + "), font (" + tfn + ")", expected.length, actual.length);
        for (int i = 0, n = expected.length; i < n; i++) {
            final int e = expected[i];
            final int a = actual[i];
            assertEquals(label + ": unequal glyphs[" + i + "], word form(" + wf
                    + "), font (" + tfn + ")", e, a);
        }
    }

    private void assertSameAdjustments(final int[][] expected,
            final int[][] actual, final String wf, final String tfn) {
        assertEquals("unequal adjustment count, word form(" + wf + "), font ("
                + tfn + ")", expected.length, actual.length);
        for (int i = 0, n = expected.length; i < n; i++) {
            final int[] ea = expected[i];
            final int[] aa = actual[i];
            assertEquals("bad adjustments length, word form(" + wf
                    + "), font (" + tfn + ")", ea.length, aa.length);
            for (int k = 0; k < 4; k++) {
                final int e = ea[k];
                final int a = aa[k];
                assertEquals("unequal adjustment[" + i + "][" + k
                        + "], word form(" + wf + "), font (" + tfn + ")", e, a);
            }
        }
    }

    private static int[] getGlyphs(final GlyphSequence gs) {
        final IntBuffer gb = gs.getGlyphs();
        final int[] ga = new int[gb.limit()];
        gb.rewind();
        gb.get(ga);
        return ga;
    }

}
