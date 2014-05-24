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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectOutputStream;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.complexscripts.fonts.GlyphPositioningTable;
import org.apache.fop.complexscripts.fonts.GlyphSubstitutionTable;
import org.apache.fop.complexscripts.fonts.ttx.TTXFile;
import org.apache.fop.complexscripts.util.GlyphSequence;

/**
 * Tests for functionality related to the arabic script.
 */
@Slf4j
public class GenerateArabicTestData implements ArabicTestConstants {

    public static void main(final String[] args) {
        boolean compile = false;
        boolean help = false;
        for (final String a : args) {
            if (a.equals("-c")) {
                compile = true;
            }
            if (a.equals("-?")) {
                help = true;
            }
        }
        if (help) {
            help();
        } else if (compile) {
            compile();
        }
    }

    private static void help() {
        final StringBuffer sb = new StringBuffer();
        sb.append("org.apache.fop.complexscripts.arabic.ArabicTestCase");
        sb.append(" [-compile]");
        sb.append(" [-?]");
        log.info(sb.toString());
    }

    private static void compile() {
        for (final String sfn : srcFiles) {
            try {
                final String spn = srcFilesDir + File.separator + sfn + "."
                        + WF_FILE_SRC_EXT;
                compile(WF_FILE_SCRIPT, WF_FILE_LANGUAGE, spn);
            } catch (final Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void compile(final String script, final String language,
            final String spn) {
        int fno = 0;
        for (final String tfn : ttxFonts) {
            final TTXFile tf = TTXFile.getFromCache(ttxFontsDir
                    + File.separator + tfn);
            assert tf != null;
            final List data = compile(script, language, spn, tfn, tf);
            output(makeDataPathName(spn, fno++), data);
        }
    }

    private static List compile(final String script, final String language,
            final String spn, final String tfn, final TTXFile tf) {
        final List<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[] { script, language, spn, tfn });
        final GlyphSubstitutionTable gsub = tf.getGSUB();
        final GlyphPositioningTable gpos = tf.getGPOS();
        final int[] widths = tf.getWidths();
        if (gsub != null && gpos != null) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(spn);
                if (fis != null) {
                    final LineNumberReader lr = new LineNumberReader(
                            new InputStreamReader(fis, Charset.forName("UTF-8")));
                    String wf;
                    while ((wf = lr.readLine()) != null) {
                        final GlyphSequence igs = tf.mapCharsToGlyphs(wf);
                        final GlyphSequence ogs = gsub.substitute(igs, script,
                                language);
                        int[][] paa = new int[ogs.getGlyphCount()][4];
                        if (!gpos.position(ogs, script, language, 1000, widths,
                                paa)) {
                            paa = null;
                        }
                        data.add(new Object[] { wf, getGlyphs(igs),
                                getGlyphs(ogs), paa });
                    }
                    lr.close();
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
        } else {
            assert gsub != null;
            assert gpos != null;
        }
        System.err.println("compiled " + (data.size() - 1)
                + " word forms using font " + tfn);
        return data;
    }

    private static int[] getGlyphs(final GlyphSequence gs) {
        final IntBuffer gb = gs.getGlyphs();
        final int[] ga = new int[gb.limit()];
        gb.rewind();
        gb.get(ga);
        return ga;
    }

    private static String makeDataPathName(final String spn, final int fno) {
        final File f = new File(spn);
        return datFilesDir + File.separator + stripExtension(f.getName())
                + "-f" + fno + "." + WF_FILE_DAT_EXT;
    }

    private static String stripExtension(final String s) {
        final int i = s.lastIndexOf('.');
        if (i >= 0) {
            return s.substring(0, i);
        } else {
            return s;
        }
    }

    private static void output(final String dpn, final List<Object[]> data) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dpn);
            if (fos != null) {
                final ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(data);
                oos.close();
            }
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (final Exception e) {
                }
            }
        }
    }

}
