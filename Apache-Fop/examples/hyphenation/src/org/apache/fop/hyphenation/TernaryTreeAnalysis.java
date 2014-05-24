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

/* $Id: TernaryTreeAnalysis.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.hyphenation;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides some useful methods to print the structure of a
 * TernaryTree object
 */
public class TernaryTreeAnalysis {

    /**
     * The TernaryTree object to analyse
     */
    protected TernaryTree tt;

    /**
     * @param tt
     *            the TernaryTree object
     */
    public TernaryTreeAnalysis(final TernaryTree tt) {
        this.tt = tt;
    }

    /**
     * Class representing a string of nodes in the tree representation of a
     * TernaryTree
     */
    public static class NodeString {

        /**
         * The node string being constructed
         */
        public StringBuffer string = new StringBuffer();

        /**
         * The indent of the node string
         */
        public int indent;

        /**
         * The list of branchpoints into the high direction
         */
        public List high = new ArrayList();

        /**
         * The list of branchpoints into the low direction
         */
        public List low = new ArrayList();

        /**
         * @param indent
         *            the indent of the nodestring
         */
        public NodeString(final int indent) {
            this.indent = indent;
            this.string.append("+");
        }

    }

    /**
     * Class representing a node of the TernaryTree object
     */
    protected class Node {

        /**
         * The index of the node
         */
        protected int index = 0;

        /**
         * The index of the high node
         */
        protected int high = 0;

        /**
         * The index of the high node
         */
        protected int low = 0;

        /**
         * The index of the equal node
         */
        protected int equal = 0;

        /**
         * The key following the node
         */
        protected String key = null;

        /**
         * True if this is a leaf node
         */
        protected boolean isLeafNode = false;

        /**
         * True if this is a packed node
         */
        protected boolean isPacked = false;

        /**
         * @param index
         *            the index of the node
         */
        protected Node(final int index) {
            this.index = index;
            if (TernaryTreeAnalysis.this.tt.sc[index] == 0) {
                this.isLeafNode = true;
            } else if (TernaryTreeAnalysis.this.tt.sc[index] == 0xFFFF) {
                this.isLeafNode = true;
                this.isPacked = true;
                this.key = readKey().toString();
            } else {
                this.key = new String(TernaryTreeAnalysis.this.tt.sc, index, 1);
                this.high = TernaryTreeAnalysis.this.tt.hi[index];
                this.low = TernaryTreeAnalysis.this.tt.lo[index];
                this.equal = TernaryTreeAnalysis.this.tt.eq[index];
            }
        }

        private StringBuffer readKey() {
            final StringBuffer s = new StringBuffer();
            int i = TernaryTreeAnalysis.this.tt.lo[this.index];
            char c = TernaryTreeAnalysis.this.tt.kv.get(i);
            for (; c != 0; c = TernaryTreeAnalysis.this.tt.kv.get(++i)) {
                s.append(c);
            }
            return s;
        }

        /**
         * Construct the string representation of the node
         * 
         * @return the string representing the node
         */
        public String toNodeString() {
            final StringBuffer s = new StringBuffer();
            if (this.isLeafNode) {
                s.append("-" + this.index);
                if (this.isPacked) {
                    s.append(",=>'" + this.key + "'");
                }
                s.append(",leaf");
            } else {
                s.append("-" + this.index + "--" + this.key + "-");
            }
            return s.toString();
        }

        /**
         * Construct the compact string representation of the node
         * 
         * @return the string representing the node
         */
        public String toCompactString() {
            final StringBuffer s = new StringBuffer();
            if (this.isLeafNode) {
                s.append("-" + this.index);
                if (this.isPacked) {
                    s.append(",=>'" + this.key + "'");
                }
                s.append(",leaf\n");
            } else {
                if (this.high != 0) {
                    s.append("(+-" + this.high + ")\n |\n");
                }
                s.append("-" + this.index + "- " + this.key + " (-"
                        + this.equal + ")\n");
                if (this.low != 0) {
                    s.append(" |\n(+-" + this.low + ")\n");
                }
            }
            return s.toString();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            final StringBuffer s = new StringBuffer();
            s.append("Node " + this.index + ":\n");
            if (this.isLeafNode) {
                if (this.isPacked) {
                    s.append("key: " + this.key + "\n");
                }
            } else {
                s.append("high: "
                        + (this.high == 0 ? "-" : String.valueOf(this.high))
                        + ", equal: " + this.equal + ", low: "
                        + (this.low == 0 ? "-" : String.valueOf(this.low))
                        + "\n");
                s.append("key: " + this.key + "\n");
            }
            return s.toString();
        }

    }

    /**
     * Construct the compact node representation of the TernaryTree object
     * 
     * @return the string representing the tree
     */
    public String toCompactNodes() {
        final StringBuffer s = new StringBuffer();
        for (int i = 1; i < this.tt.sc.length; ++i) {
            if (i != 1) {
                s.append("\n");
            }
            s.append(new Node(i).toCompactString());
        }
        return s.toString();
    }

    /**
     * Construct the node representation of the TernaryTree object
     * 
     * @return the string representing the tree
     */
    public String toNodes() {
        final StringBuffer s = new StringBuffer();
        for (int i = 1; i < this.tt.sc.length; ++i) {
            if (i != 1) {
                s.append("\n");
            }
            s.append(new Node(i).toString());
        }
        return s.toString();
    }

    private static StringBuffer toString(final char[] c) {
        final StringBuffer s = new StringBuffer();
        for (int i = 0; i < c.length; ++i) {
            s.append((int) c[i]);
            s.append(",");
        }
        return s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuffer s = new StringBuffer();

        s.append("hi: ");
        s.append(toString(this.tt.hi));
        s.append("\n");

        s.append("eq: ");
        s.append(toString(this.tt.eq));
        s.append("\n");

        s.append("lo: ");
        s.append(toString(this.tt.lo));
        s.append("\n");

        s.append("sc: ");
        for (int i = 0; i < this.tt.sc.length; ++i) {
            if (this.tt.sc[i] == 0) {
                s.append("-");
            } else if (this.tt.sc[i] == 0xFFFF) {
                s.append("^");
            } else {
                s.append(this.tt.sc[i]);
            }
        }
        s.append("\n");

        s.append("kv: ");
        for (int i = 0; i < this.tt.kv.length(); ++i) {
            if (this.tt.kv.get(i) == 0) {
                s.append("-");
            } else {
                s.append(this.tt.kv.get(i));
            }
        }
        s.append("\n");

        s.append("freenode: ");
        s.append((int) this.tt.freenode);
        s.append("\n");

        s.append("root: ");
        s.append((int) this.tt.root);
        s.append("\n");

        return s.toString();
    }

}
