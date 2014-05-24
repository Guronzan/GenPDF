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

/* $Id: HyphenationTreeAnalysis.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.hyphenation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.extern.slf4j.Slf4j;

/**
 * This class provides some useful methods to print the structure of a
 * HyphenationTree object
 */
@Slf4j
public class HyphenationTreeAnalysis extends TernaryTreeAnalysis {

    /**
     * The HyphenationTree object to analyse
     */
    protected HyphenationTree ht;

    /**
     * @param ht
     *            the HyphenationTree object
     */
    public HyphenationTreeAnalysis(final HyphenationTree ht) {
        super(ht);
        this.ht = ht;
    }

    /**
     * Class representing a node of the HyphenationTree object
     */
    protected class Node extends TernaryTreeAnalysis.Node {
        private String value = null;

        /**
         * @param index
         *            the index of the node
         */
        protected Node(final int index) {
            super(index);
            if (this.isLeafNode) {
                this.value = readValue().toString();
            }
        }

        private StringBuffer readValue() {
            final StringBuffer s = new StringBuffer();
            int i = HyphenationTreeAnalysis.this.ht.eq[this.index];
            byte v = HyphenationTreeAnalysis.this.ht.vspace.get(i);
            for (; v != 0; v = HyphenationTreeAnalysis.this.ht.vspace.get(++i)) {
                int c = (v >>> 4) - 1;
                s.append(c);
                c = v & 0x0f;
                if (c == 0) {
                    break;
                }
                c = c - 1;
                s.append(c);
            }
            return s;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.apache.fop.hyphenation.TernaryTreeAnalysis.Node#toNodeString()
         */
        @Override
        public String toNodeString() {
            if (this.isLeafNode) {
                final StringBuffer s = new StringBuffer();
                s.append("-" + this.index);
                if (this.isPacked) {
                    s.append(",=>'" + this.key + "'");
                }
                s.append("," + this.value);
                s.append(",leaf");
                return s.toString();
            } else {
                return super.toNodeString();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.apache.fop.hyphenation.TernaryTreeAnalysis.Node#toCompactString()
         */
        @Override
        public String toCompactString() {
            if (this.isLeafNode) {
                final StringBuffer s = new StringBuffer();
                s.append("-" + this.index);
                if (this.isPacked) {
                    s.append(",=>'" + this.key + "'");
                }
                s.append("," + this.value);
                s.append(",leaf\n");
                return s.toString();
            } else {
                return super.toCompactString();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            final StringBuffer s = new StringBuffer();
            s.append(super.toString());
            if (this.isLeafNode) {
                s.append("value: " + this.value + "\n");
            }
            return s.toString();
        }

    }

    private void addNode(final int nodeIndex, final List strings,
            final NodeString ns) {
        final int pos = ns.indent + ns.string.length() + 1;
        final Node n = new Node(nodeIndex);
        ns.string.append(n.toNodeString());
        if (n.high != 0) {
            ns.high.add(new Integer(pos));
            final NodeString highNs = new NodeString(pos);
            highNs.low.add(new Integer(pos));
            final int index = strings.indexOf(ns);
            strings.add(index, highNs);
            addNode(n.high, strings, highNs);
        }
        if (n.low != 0) {
            ns.low.add(new Integer(pos));
            final NodeString lowNs = new NodeString(pos);
            lowNs.high.add(new Integer(pos));
            final int index = strings.indexOf(ns);
            strings.add(index + 1, lowNs);
            addNode(n.low, strings, lowNs);
        }
        if (!n.isLeafNode) {
            addNode(n.equal, strings, ns);
        }

    }

    /**
     * Construct the tree representation of a list of node strings
     *
     * @param strings
     *            the list of node strings
     * @return the string representing the tree
     */
    public String toTree(final List strings) {
        final StringBuffer indentString = new StringBuffer();
        for (int j = indentString.length(); j < ((NodeString) strings.get(0)).indent; ++j) {
            indentString.append(' ');
        }
        final StringBuffer tree = new StringBuffer();
        for (int i = 0; i < strings.size(); ++i) {
            final NodeString ns = (NodeString) strings.get(i);
            if (indentString.length() > ns.indent) {
                indentString.setLength(ns.indent);
            } else {
                // should not happen
                for (int j = indentString.length(); j < ns.indent; ++j) {
                    indentString.append(' ');
                }
            }
            tree.append(indentString);
            tree.append(ns.string + "\n");

            if (i + 1 == strings.size()) {
                continue;
            }
            for (int j = 0; j < ns.low.size(); ++j) {
                final int pos = ((Integer) ns.low.get(j)).intValue();
                if (pos < indentString.length()) {
                    indentString.setCharAt(pos, '|');
                } else {
                    for (int k = indentString.length(); k < pos; ++k) {
                        indentString.append(' ');
                    }
                    indentString.append('|');
                }
            }
            tree.append(indentString + "\n");
        }

        return tree.toString();
    }

    /**
     * Construct the tree representation of the HyphenationTree object
     *
     * @return the string representing the tree
     */
    public String toTree() {
        final List strings = new ArrayList();
        final NodeString ns = new NodeString(0);
        strings.add(ns);
        addNode(1, strings, ns);
        return toTree(strings);
    }

    /**
     * Construct the compact node representation of the HyphenationTree object
     *
     * @return the string representing the tree
     */
    @Override
    public String toCompactNodes() {
        final StringBuffer s = new StringBuffer();
        for (int i = 1; i < this.ht.sc.length; ++i) {
            if (i != 1) {
                s.append("\n");
            }
            s.append(new Node(i).toCompactString());
        }
        return s.toString();
    }

    /**
     * Construct the node representation of the HyphenationTree object
     *
     * @return the string representing the tree
     */
    @Override
    public String toNodes() {
        final StringBuffer s = new StringBuffer();
        for (int i = 1; i < this.ht.sc.length; ++i) {
            if (i != 1) {
                s.append("\n");
            }
            s.append(new Node(i).toString());
        }
        return s.toString();
    }

    /**
     * Construct the printed representation of the HyphenationTree object
     *
     * @return the string representing the tree
     */
    @Override
    public String toString() {
        final StringBuffer s = new StringBuffer();

        s.append("classes: \n");
        s.append(new TernaryTreeAnalysis(this.ht.classmap).toString());

        s.append("\npatterns: \n");
        s.append(super.toString());
        s.append("vspace: ");
        for (int i = 0; i < this.ht.vspace.length(); ++i) {
            final byte v = this.ht.vspace.get(i);
            if (v == 0) {
                s.append("--");
            } else {
                int c = (v >>> 4) - 1;
                s.append(c);
                c = v & 0x0f;
                if (c == 0) {
                    s.append("-");
                } else {
                    c = c - 1;
                    s.append(c);
                }
            }
        }
        s.append("\n");

        return s.toString();
    }

    /**
     * Provide interactive access to a HyphenationTree object and its
     * representation methods
     *
     * @param args
     *            the arguments
     */
    public static void main(final String[] args) {
        HyphenationTree ht = null;
        HyphenationTreeAnalysis hta = null;
        int minCharCount = 2;
        final BufferedReader in = new BufferedReader(
                new java.io.InputStreamReader(System.in));
        while (true) {
            System.out
                    .print("l:\tload patterns from XML\n"
                            + "L:\tload patterns from serialized object\n"
                            + "s:\tset minimun character count\n"
                            + "w:\twrite hyphenation tree to object file\n"
                            + "p:\tprint hyphenation tree to stdout\n"
                            + "n:\tprint hyphenation tree nodes to stdout\n"
                            + "c:\tprint compact hyphenation tree nodes to stdout\n"
                            + "t:\tprint tree representation of hyphenation tree to stdout\n"
                            + "h:\thyphenate\n" + "f:\tfind pattern\n"
                            + "b:\tbenchmark\n" + "q:\tquit\n\n" + "Command:");
            try {
                String token = in.readLine().trim();
                if (token.equals("f")) {
                    System.out.print("Pattern: ");
                    token = in.readLine().trim();
                    log.info("Values: " + ht.findPattern(token));
                } else if (token.equals("s")) {
                    System.out.print("Minimum value: ");
                    token = in.readLine().trim();
                    minCharCount = Integer.parseInt(token);
                } else if (token.equals("l")) {
                    ht = new HyphenationTree();
                    hta = new HyphenationTreeAnalysis(ht);
                    System.out.print("XML file name: ");
                    token = in.readLine().trim();
                    try {
                        ht.loadPatterns(token);
                    } catch (final HyphenationException e) {
                        e.printStackTrace();
                    }
                } else if (token.equals("L")) {
                    ObjectInputStream ois = null;
                    System.out.print("Object file name: ");
                    token = in.readLine().trim();
                    try {
                        final String[] parts = token.split(":");
                        InputStream is = null;
                        if (parts.length == 1) {
                            is = new FileInputStream(token);
                        } else if (parts.length == 2) {
                            final ZipFile jar = new ZipFile(parts[0]);
                            final ZipEntry entry = new ZipEntry(
                                    jar.getEntry(parts[1]));
                            is = jar.getInputStream(entry);
                        }
                        ois = new ObjectInputStream(is);
                        ht = (HyphenationTree) ois.readObject();
                        hta = new HyphenationTreeAnalysis(ht);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (ois != null) {
                            try {
                                ois.close();
                            } catch (final IOException e) {
                                // ignore
                            }
                        }
                    }
                } else if (token.equals("w")) {
                    System.out.print("Object file name: ");
                    token = in.readLine().trim();
                    ObjectOutputStream oos = null;
                    try {
                        oos = new ObjectOutputStream(
                                new FileOutputStream(token));
                        oos.writeObject(ht);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (oos != null) {
                            try {
                                oos.flush();
                            } catch (final IOException e) {
                                // ignore
                            }
                            try {
                                oos.close();
                            } catch (final IOException e) {
                                // ignore
                            }
                        }
                    }
                } else if (token.equals("p")) {
                    System.out.print(hta);
                } else if (token.equals("n")) {
                    System.out.print(hta.toNodes());
                } else if (token.equals("c")) {
                    System.out.print(hta.toCompactNodes());
                } else if (token.equals("t")) {
                    System.out.print(hta.toTree());
                } else if (token.equals("h")) {
                    System.out.print("Word: ");
                    token = in.readLine().trim();
                    System.out.print("Hyphenation points: ");
                    log.info(ht.hyphenate(token, minCharCount, minCharCount)
                            .toString());
                } else if (token.equals("b")) {
                    if (ht == null) {
                        log.info("No patterns have been loaded.");
                        break;
                    }
                    System.out.print("Word list filename: ");
                    token = in.readLine().trim();
                    long starttime = 0;
                    int counter = 0;
                    try {
                        final BufferedReader reader = new BufferedReader(
                                new FileReader(token));
                        String line;

                        starttime = System.currentTimeMillis();
                        while ((line = reader.readLine()) != null) {
                            // System.out.print("\nline: ");
                            final Hyphenation hyp = ht.hyphenate(line,
                                    minCharCount, minCharCount);
                            if (hyp != null) {
                                final String hword = hyp.toString();
                                // log.info(line);
                                // log.info(hword);
                            } else {
                                // log.info("No hyphenation");
                            }
                            counter++;
                        }
                    } catch (final Exception ioe) {
                        log.info("Exception " + ioe);
                        ioe.printStackTrace();
                    }
                    final long endtime = System.currentTimeMillis();
                    final long result = endtime - starttime;
                    log.info(counter + " words in " + result
                            + " Milliseconds hyphenated");

                } else if (token.equals("q")) {
                    break;
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

    }

}
