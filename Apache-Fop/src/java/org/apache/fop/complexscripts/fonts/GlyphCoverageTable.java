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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

// CSOFF: LineLengthCheck
// CSOFF: InnerAssignmentCheck
// CSOFF: NoWhitespaceAfterCheck

/**
 * <p>
 * .Base class implementation of glyph coverage table.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
@Slf4j
public final class GlyphCoverageTable extends GlyphMappingTable implements
GlyphCoverageMapping {

    /** empty mapping table */
    public static final int GLYPH_COVERAGE_TYPE_EMPTY = GLYPH_MAPPING_TYPE_EMPTY;

    /** mapped mapping table */
    public static final int GLYPH_COVERAGE_TYPE_MAPPED = GLYPH_MAPPING_TYPE_MAPPED;

    /** range based mapping table */
    public static final int GLYPH_COVERAGE_TYPE_RANGE = GLYPH_MAPPING_TYPE_RANGE;

    private final GlyphCoverageMapping cm;

    private GlyphCoverageTable(final GlyphCoverageMapping cm) {
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
    public int getCoverageSize() {
        return this.cm.getCoverageSize();
    }

    /** {@inheritDoc} */
    @Override
    public int getCoverageIndex(final int gid) {
        return this.cm.getCoverageIndex(gid);
    }

    /**
     * Create glyph coverage table.
     *
     * @param entries
     *            list of mapped or ranged coverage entries, or null or empty
     *            list
     * @return a new covera table instance
     */
    public static GlyphCoverageTable createCoverageTable(final List entries) {
        GlyphCoverageMapping cm;
        if (entries == null || entries.size() == 0) {
            cm = new EmptyCoverageTable(entries);
        } else if (isMappedCoverage(entries)) {
            cm = new MappedCoverageTable(entries);
        } else if (isRangeCoverage(entries)) {
            cm = new RangeCoverageTable(entries);
        } else {
            cm = null;
        }
        assert cm != null : "unknown coverage type";
        return new GlyphCoverageTable(cm);
    }

    private static boolean isMappedCoverage(final List entries) {
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

    private static boolean isRangeCoverage(final List entries) {
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

    private static class EmptyCoverageTable extends
    GlyphMappingTable.EmptyMappingTable implements GlyphCoverageMapping {
        public EmptyCoverageTable(final List entries) {
            super(entries);
        }

        /** {@inheritDoc} */
        @Override
        public int getCoverageSize() {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public int getCoverageIndex(final int gid) {
            return -1;
        }
    }

    private static class MappedCoverageTable extends
    GlyphMappingTable.MappedMappingTable implements
    GlyphCoverageMapping {
        private int[] map;

        public MappedCoverageTable(final List entries) {
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            final List entries = new java.util.ArrayList();
            if (this.map != null) {
                for (final int element : this.map) {
                    entries.add(Integer.valueOf(element));
                }
            }
            return entries;
        }

        /** {@inheritDoc} */
        @Override
        public int getMappingSize() {
            return this.map != null ? this.map.length : 0;
        }

        @Override
        public int getMappedIndex(final int gid) {
            int i;
            if ((i = Arrays.binarySearch(this.map, gid)) >= 0) {
                return i;
            } else {
                return -1;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int getCoverageSize() {
            return getMappingSize();
        }

        /** {@inheritDoc} */
        @Override
        public int getCoverageIndex(final int gid) {
            return getMappedIndex(gid);
        }

        private void populate(final List entries) {
            int i = 0;
            int skipped = 0;
            final int n = entries.size();
            int gidMax = -1;
            final int[] map = new int[n];
            for (final Iterator it = entries.iterator(); it.hasNext();) {
                final Object o = it.next();
                if (o instanceof Integer) {
                    final int gid = ((Integer) o).intValue();
                    if (gid >= 0 && gid < 65536) {
                        if (gid > gidMax) {
                            map[i++] = gidMax = gid;
                        } else {
                            log.info("ignoring out of order or duplicate glyph index: "
                                    + gid);
                            skipped++;
                        }
                    } else {
                        throw new AdvancedTypographicTableFormatException(
                                "illegal glyph index: " + gid);
                    }
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal coverage entry, must be Integer: " + o);
                }
            }
            assert i + skipped == n;
            assert this.map == null;
            this.map = map;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append('{');
            for (int i = 0, n = this.map.length; i < n; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(Integer.toString(this.map[i]));
            }
            sb.append('}');
            return sb.toString();
        }
    }

    private static class RangeCoverageTable extends
    GlyphMappingTable.RangeMappingTable implements GlyphCoverageMapping {
        public RangeCoverageTable(final List entries) {
            super(entries);
        }

        /** {@inheritDoc} */
        @Override
        public int getMappedIndex(final int gid, final int s, final int m) {
            return m + gid - s;
        }

        /** {@inheritDoc} */
        @Override
        public int getCoverageSize() {
            return getMappingSize();
        }

        /** {@inheritDoc} */
        @Override
        public int getCoverageIndex(final int gid) {
            return getMappedIndex(gid);
        }
    }

}
