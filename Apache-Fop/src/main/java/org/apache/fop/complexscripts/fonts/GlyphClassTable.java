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

package org.apache.fop.complexscripts.fonts;

import java.util.Iterator;
import java.util.List;

// CSOFF: LineLengthCheck
// CSOFF: NoWhitespaceAfterCheck

/**
 * <p>
 * Base class implementation of glyph class table.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
public final class GlyphClassTable extends GlyphMappingTable implements
        GlyphClassMapping {

    /** empty mapping table */
    public static final int GLYPH_CLASS_TYPE_EMPTY = GLYPH_MAPPING_TYPE_EMPTY;

    /** mapped mapping table */
    public static final int GLYPH_CLASS_TYPE_MAPPED = GLYPH_MAPPING_TYPE_MAPPED;

    /** range based mapping table */
    public static final int GLYPH_CLASS_TYPE_RANGE = GLYPH_MAPPING_TYPE_RANGE;

    /** empty mapping table */
    public static final int GLYPH_CLASS_TYPE_COVERAGE_SET = 3;

    private final GlyphClassMapping cm;

    private GlyphClassTable(final GlyphClassMapping cm) {
        assert cm != null;
        assert cm instanceof GlyphMappingTable;
        this.cm = cm;
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return ((GlyphMappingTable) this.cm).getType();
    }

    /** {@inheritDoc} */
    @Override
    public List getEntries() {
        return ((GlyphMappingTable) this.cm).getEntries();
    }

    /** {@inheritDoc} */
    @Override
    public int getClassSize(final int set) {
        return this.cm.getClassSize(set);
    }

    /** {@inheritDoc} */
    @Override
    public int getClassIndex(final int gid, final int set) {
        return this.cm.getClassIndex(gid, set);
    }

    /**
     * Create glyph class table.
     * 
     * @param entries
     *            list of mapped or ranged class entries, or null or empty list
     * @return a new covera table instance
     */
    public static GlyphClassTable createClassTable(final List entries) {
        GlyphClassMapping cm;
        if (entries == null || entries.size() == 0) {
            cm = new EmptyClassTable(entries);
        } else if (isMappedClass(entries)) {
            cm = new MappedClassTable(entries);
        } else if (isRangeClass(entries)) {
            cm = new RangeClassTable(entries);
        } else if (isCoverageSetClass(entries)) {
            cm = new CoverageSetClassTable(entries);
        } else {
            cm = null;
        }
        assert cm != null : "unknown class type";
        return new GlyphClassTable(cm);
    }

    private static boolean isMappedClass(final List entries) {
        if (entries == null || entries.size() == 0) {
            return false;
        } else {
            for (final Iterator it = entries.iterator(); it.hasNext();) {
                final Object o = it.next();
                if (!(o instanceof Integer)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean isRangeClass(final List entries) {
        if (entries == null || entries.size() == 0) {
            return false;
        } else {
            for (final Iterator it = entries.iterator(); it.hasNext();) {
                final Object o = it.next();
                if (!(o instanceof MappingRange)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean isCoverageSetClass(final List entries) {
        if (entries == null || entries.size() == 0) {
            return false;
        } else {
            for (final Iterator it = entries.iterator(); it.hasNext();) {
                final Object o = it.next();
                if (!(o instanceof GlyphCoverageTable)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class EmptyClassTable extends
            GlyphMappingTable.EmptyMappingTable implements GlyphClassMapping {
        public EmptyClassTable(final List entries) {
            super(entries);
        }

        /** {@inheritDoc} */
        @Override
        public int getClassSize(final int set) {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public int getClassIndex(final int gid, final int set) {
            return -1;
        }
    }

    private static class MappedClassTable extends
            GlyphMappingTable.MappedMappingTable implements GlyphClassMapping {
        private int firstGlyph;
        private int[] gca;
        private int gcMax = -1;

        public MappedClassTable(final List entries) {
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            final List entries = new java.util.ArrayList();
            entries.add(Integer.valueOf(this.firstGlyph));
            if (this.gca != null) {
                for (int i = 0, n = this.gca.length; i < n; i++) {
                    entries.add(Integer.valueOf(this.gca[i]));
                }
            }
            return entries;
        }

        /** {@inheritDoc} */
        @Override
        public int getMappingSize() {
            return this.gcMax + 1;
        }

        /** {@inheritDoc} */
        @Override
        public int getMappedIndex(final int gid) {
            final int i = gid - this.firstGlyph;
            if (i >= 0 && i < this.gca.length) {
                return this.gca[i];
            } else {
                return -1;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int getClassSize(final int set) {
            return getMappingSize();
        }

        /** {@inheritDoc} */
        @Override
        public int getClassIndex(final int gid, final int set) {
            return getMappedIndex(gid);
        }

        private void populate(final List entries) {
            // obtain entries iterator
            final Iterator it = entries.iterator();
            // extract first glyph
            int firstGlyph = 0;
            if (it.hasNext()) {
                final Object o = it.next();
                if (o instanceof Integer) {
                    firstGlyph = ((Integer) o).intValue();
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entry, first entry must be Integer denoting first glyph value, but is: "
                                    + o);
                }
            }
            // extract glyph class array
            int i = 0;
            final int n = entries.size() - 1;
            int gcMax = -1;
            final int[] gca = new int[n];
            while (it.hasNext()) {
                final Object o = it.next();
                if (o instanceof Integer) {
                    final int gc = ((Integer) o).intValue();
                    gca[i++] = gc;
                    if (gc > gcMax) {
                        gcMax = gc;
                    }
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal mapping entry, must be Integer: " + o);
                }
            }
            assert i == n;
            assert this.gca == null;
            this.firstGlyph = firstGlyph;
            this.gca = gca;
            this.gcMax = gcMax;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{ firstGlyph = " + this.firstGlyph + ", classes = {");
            for (int i = 0, n = this.gca.length; i < n; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(Integer.toString(this.gca[i]));
            }
            sb.append("} }");
            return sb.toString();
        }
    }

    private static class RangeClassTable extends
            GlyphMappingTable.RangeMappingTable implements GlyphClassMapping {
        public RangeClassTable(final List entries) {
            super(entries);
        }

        /** {@inheritDoc} */
        @Override
        public int getMappedIndex(final int gid, final int s, final int m) {
            return m;
        }

        /** {@inheritDoc} */
        @Override
        public int getClassSize(final int set) {
            return getMappingSize();
        }

        /** {@inheritDoc} */
        @Override
        public int getClassIndex(final int gid, final int set) {
            return getMappedIndex(gid);
        }
    }

    private static class CoverageSetClassTable extends
            GlyphMappingTable.EmptyMappingTable implements GlyphClassMapping {
        public CoverageSetClassTable(final List entries) {
            throw new UnsupportedOperationException(
                    "coverage set class table not yet supported");
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GLYPH_CLASS_TYPE_COVERAGE_SET;
        }

        /** {@inheritDoc} */
        @Override
        public int getClassSize(final int set) {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public int getClassIndex(final int gid, final int set) {
            return -1;
        }
    }

}
