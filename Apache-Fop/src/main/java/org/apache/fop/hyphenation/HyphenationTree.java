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

/* $Id: HyphenationTree.java 1297284 2012-03-05 23:29:29Z gadams $ */

package org.apache.fop.hyphenation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;

import org.xml.sax.InputSource;

/**
 * <p>
 * This tree structure stores the hyphenation patterns in an efficient way for
 * fast lookup. It provides the provides the method to hyphenate a word.
 * </p>
 *
 * <p>
 * This work was authored by Carlos Villegas (cav@uniscope.co.jp).
 * </p>
 */
@Slf4j
public class HyphenationTree extends TernaryTree implements PatternConsumer {

    private static final long serialVersionUID = -7842107987915665573L;

    /**
     * value space: stores the interletter values
     */
    protected ByteList vspace;

    /**
     * This map stores hyphenation exceptions
     */
    protected HashMap stoplist;

    /**
     * This map stores the character classes
     */
    protected TernaryTree classmap;

    /**
     * Temporary map to store interletter values on pattern loading.
     */
    private transient TernaryTree ivalues;

    /** Default constructor. */
    public HyphenationTree() {
        this.stoplist = new HashMap(23); // usually a small table
        this.classmap = new TernaryTree();
        this.vspace = new ByteList();
        this.vspace.alloc(1); // this reserves index 0, which we don't use
    }

    /**
     * Packs the values by storing them in 4 bits, two values into a byte Values
     * range is from 0 to 9. We use zero as terminator, so we'll add 1 to the
     * value.
     *
     * @param values
     *            a string of digits from '0' to '9' representing the
     *            interletter values.
     * @return the index into the vspace array where the packed values are
     *         stored.
     */
    protected int packValues(final String values) {
        int i;
        final int n = values.length();
        final int m = (n & 1) == 1 ? (n >> 1) + 2 : (n >> 1) + 1;
        final int offset = this.vspace.alloc(m);
        final byte[] va = this.vspace.getArray();
        for (i = 0; i < n; i++) {
            final int j = i >> 1;
            final byte v = (byte) (values.charAt(i) - '0' + 1 & 0x0f);
            if ((i & 1) == 1) {
                va[j + offset] = (byte) (va[j + offset] | v);
            } else {
                va[j + offset] = (byte) (v << 4); // big endian
            }
        }
        va[m - 1 + offset] = 0; // terminator
        return offset;
    }

    /**
     * Unpack values.
     *
     * @param k
     *            an integer
     * @return a string
     */
    protected String unpackValues(int k) {
        final StringBuilder buf = new StringBuilder();
        byte v = this.vspace.get(k++);
        while (v != 0) {
            char c = (char) ((v >>> 4) - 1 + '0');
            buf.append(c);
            c = (char) (v & 0x0f);
            if (c == 0) {
                break;
            }
            c = (char) (c - 1 + '0');
            buf.append(c);
            v = this.vspace.get(k++);
        }
        return buf.toString();
    }

    /**
     * Read hyphenation patterns from an XML file.
     *
     * @param filename
     *            the filename
     * @throws HyphenationException
     *             In case the parsing fails
     */
    public void loadPatterns(final String filename) throws HyphenationException {
        final File f = new File(filename);
        try {
            final InputSource src = new InputSource(f.toURI().toURL()
                    .toExternalForm());
            loadPatterns(src);
        } catch (final MalformedURLException e) {
            throw new HyphenationException("Error converting the File '" + f
                    + "' to a URL: " + e.getMessage());
        }
    }

    /**
     * Read hyphenation patterns from an XML file.
     *
     * @param source
     *            the InputSource for the file
     * @throws HyphenationException
     *             In case the parsing fails
     */
    public void loadPatterns(final InputSource source)
            throws HyphenationException {
        final PatternParser pp = new PatternParser(this);
        this.ivalues = new TernaryTree();

        pp.parse(source);

        // patterns/values should be now in the tree
        // let's optimize a bit
        trimToSize();
        this.vspace.trimToSize();
        this.classmap.trimToSize();

        // get rid of the auxiliary map
        this.ivalues = null;
    }

    /**
     * Find pattern.
     *
     * @param pat
     *            a pattern
     * @return a string
     */
    public String findPattern(final String pat) {
        final int k = super.find(pat);
        if (k >= 0) {
            return unpackValues(k);
        }
        return "";
    }

    /**
     * String compare, returns 0 if equal or t is a substring of s.
     *
     * @param s
     *            first character array
     * @param si
     *            starting index into first array
     * @param t
     *            second character array
     * @param ti
     *            starting index into second array
     * @return an integer
     */
    protected int hstrcmp(final char[] s, int si, final char[] t, int ti) {
        for (; s[si] == t[ti]; si++, ti++) {
            if (s[si] == 0) {
                return 0;
            }
        }
        if (t[ti] == 0) {
            return 0;
        }
        return s[si] - t[ti];
    }

    /**
     * Get values.
     *
     * @param k
     *            an integer
     * @return a byte array
     */
    protected byte[] getValues(int k) {
        final StringBuilder buf = new StringBuilder();
        byte v = this.vspace.get(k++);
        while (v != 0) {
            char c = (char) ((v >>> 4) - 1);
            buf.append(c);
            c = (char) (v & 0x0f);
            if (c == 0) {
                break;
            }
            c = (char) (c - 1);
            buf.append(c);
            v = this.vspace.get(k++);
        }
        final byte[] res = new byte[buf.length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = (byte) buf.charAt(i);
        }
        return res;
    }

    /**
     * <p>
     * Search for all possible partial matches of word starting at index an
     * update interletter values. In other words, it does something like:
     * </p>
     * <code>
     * for(i=0; i<patterns.length; i++) {
     * if ( word.substring(index).startsWidth(patterns[i]) )
     * update_interletter_values(patterns[i]);
     * }
     * </code>
     * <p>
     * But it is done in an efficient way since the patterns are stored in a
     * ternary tree. In fact, this is the whole purpose of having the tree:
     * doing this search without having to test every single pattern. The number
     * of patterns for languages such as English range from 4000 to 10000. Thus,
     * doing thousands of string comparisons for each word to hyphenate would be
     * really slow without the tree. The tradeoff is memory, but using a ternary
     * tree instead of a trie, almost halves the the memory used by Lout or TeX.
     * It's also faster than using a hash table
     * </p>
     *
     * @param word
     *            null terminated word to match
     * @param index
     *            start index from word
     * @param il
     *            interletter values array to update
     */
    protected void searchPatterns(final char[] word, final int index,
            final byte[] il) {
        byte[] values;
        int i = index;
        char p;
        char q;
        char sp = word[i];
        p = this.root;

        while (p > 0 && p < this.sc.length) {
            if (this.sc[p] == 0xFFFF) {
                if (hstrcmp(word, i, this.kv.getArray(), this.lo[p]) == 0) {
                    values = getValues(this.eq[p]); // data pointer is in eq[]
                    int j = index;
                    for (final byte value : values) {
                        if (j < il.length && value > il[j]) {
                            il[j] = value;
                        }
                        j++;
                    }
                }
                return;
            }
            final int d = sp - this.sc[p];
            if (d == 0) {
                if (sp == 0) {
                    break;
                }
                sp = word[++i];
                p = this.eq[p];
                q = p;

                // look for a pattern ending at this position by searching for
                // the null char ( splitchar == 0 )
                while (q > 0 && q < this.sc.length) {
                    if (this.sc[q] == 0xFFFF) { // stop at compressed branch
                        break;
                    }
                    if (this.sc[q] == 0) {
                        values = getValues(this.eq[q]);
                        int j = index;
                        for (final byte value : values) {
                            if (j < il.length && value > il[j]) {
                                il[j] = value;
                            }
                            j++;
                        }
                        break;
                    } else {
                        q = this.lo[q];

                        /**
                         * actually the code should be: q = sc[q] < 0 ? hi[q] :
                         * lo[q]; but java chars are unsigned
                         */
                    }
                }
            } else {
                p = d < 0 ? this.lo[p] : this.hi[p];
            }
        }
    }

    /**
     * Hyphenate word and return a Hyphenation object.
     *
     * @param word
     *            the word to be hyphenated
     * @param remainCharCount
     *            Minimum number of characters allowed before the hyphenation
     *            point.
     * @param pushCharCount
     *            Minimum number of characters allowed after the hyphenation
     *            point.
     * @return a {@link Hyphenation Hyphenation} object representing the
     *         hyphenated word or null if word is not hyphenated.
     */
    public Hyphenation hyphenate(final String word, final int remainCharCount,
            final int pushCharCount) {
        final char[] w = word.toCharArray();
        return hyphenate(w, 0, w.length, remainCharCount, pushCharCount);
    }

    /**
     * w = "****nnllllllnnn*****", where n is a non-letter, l is a letter, all n
     * may be absent, the first n is at offset, the first l is at offset +
     * iIgnoreAtBeginning; word = ".llllll.'\0'***", where all l in w are copied
     * into word. In the first part of the routine len = w.length, in the second
     * part of the routine len = word.length. Three indices are used: index(w),
     * the index in w, index(word), the index in word, letterindex(word), the
     * index in the letter part of word. The following relations exist: index(w)
     * = offset + i - 1 index(word) = i - iIgnoreAtBeginning letterindex(word) =
     * index(word) - 1 (see first loop). It follows that: index(w) - index(word)
     * = offset - 1 + iIgnoreAtBeginning index(w) = letterindex(word) + offset +
     * iIgnoreAtBeginning
     */

    /**
     * Hyphenate word and return an array of hyphenation points.
     *
     * @param w
     *            char array that contains the word
     * @param offset
     *            Offset to first character in word
     * @param len
     *            Length of word
     * @param remainCharCount
     *            Minimum number of characters allowed before the hyphenation
     *            point.
     * @param pushCharCount
     *            Minimum number of characters allowed after the hyphenation
     *            point.
     * @return a {@link Hyphenation Hyphenation} object representing the
     *         hyphenated word or null if word is not hyphenated.
     */
    public Hyphenation hyphenate(final char[] w, final int offset, int len,
            final int remainCharCount, final int pushCharCount) {
        int i;
        final char[] word = new char[len + 3];

        // normalize word
        final char[] c = new char[2];
        int iIgnoreAtBeginning = 0;
        int iLength = len;
        boolean bEndOfLetters = false;
        for (i = 1; i <= len; i++) {
            c[0] = w[offset + i - 1];
            final int nc = this.classmap.find(c, 0);
            if (nc < 0) { // found a non-letter character ...
                if (i == 1 + iIgnoreAtBeginning) {
                    // ... before any letter character
                    iIgnoreAtBeginning++;
                } else {
                    // ... after a letter character
                    bEndOfLetters = true;
                }
                iLength--;
            } else {
                if (!bEndOfLetters) {
                    word[i - iIgnoreAtBeginning] = (char) nc;
                } else {
                    return null;
                }
            }
        }
        len = iLength;
        if (len < remainCharCount + pushCharCount) {
            // word is too short to be hyphenated
            return null;
        }
        final int[] result = new int[len + 1];
        int k = 0;

        // check exception list first
        final String sw = new String(word, 1, len);
        if (this.stoplist.containsKey(sw)) {
            // assume only simple hyphens (Hyphen.pre="-", Hyphen.post =
            // Hyphen.no = null)
            final ArrayList hw = (ArrayList) this.stoplist.get(sw);
            int j = 0;
            for (i = 0; i < hw.size(); i++) {
                final Object o = hw.get(i);
                // j = index(sw) = letterindex(word)?
                // result[k] = corresponding index(w)
                if (o instanceof String) {
                    j += ((String) o).length();
                    if (j >= remainCharCount && j < len - pushCharCount) {
                        result[k++] = j + iIgnoreAtBeginning;
                    }
                }
            }
        } else {
            // use algorithm to get hyphenation points
            word[0] = '.'; // word start marker
            word[len + 1] = '.'; // word end marker
            word[len + 2] = 0; // null terminated
            final byte[] il = new byte[len + 3]; // initialized to zero
            for (i = 0; i < len + 1; i++) {
                searchPatterns(word, i, il);
            }

            // hyphenation points are located where interletter value is odd
            // i is letterindex(word),
            // i + 1 is index(word),
            // result[k] = corresponding index(w)
            for (i = 0; i < len; i++) {
                if ((il[i + 1] & 1) == 1 && i >= remainCharCount
                        && i <= len - pushCharCount) {
                    result[k++] = i + iIgnoreAtBeginning;
                }
            }
        }

        if (k > 0) {
            // trim result array
            final int[] res = new int[k];
            System.arraycopy(result, 0, res, 0, k);
            return new Hyphenation(new String(w, offset, len), res);
        } else {
            return null;
        }
    }

    /**
     * Add a character class to the tree. It is used by {@link PatternParser
     * PatternParser} as callback to add character classes. Character classes
     * define the valid word characters for hyphenation. If a word contains a
     * character not defined in any of the classes, it is not hyphenated. It
     * also defines a way to normalize the characters in order to compare them
     * with the stored patterns. Usually pattern files use only lower case
     * characters, in this case a class for letter 'a', for example, should be
     * defined as "aA", the first character being the normalization char.
     *
     * @param chargroup
     *            a character class (group)
     */
    @Override
    public void addClass(final String chargroup) {
        if (chargroup.length() > 0) {
            final char equivChar = chargroup.charAt(0);
            final char[] key = new char[2];
            key[1] = 0;
            for (int i = 0; i < chargroup.length(); i++) {
                key[0] = chargroup.charAt(i);
                this.classmap.insert(key, 0, equivChar);
            }
        }
    }

    /**
     * Add an exception to the tree. It is used by {@link PatternParser
     * PatternParser} class as callback to store the hyphenation exceptions.
     *
     * @param word
     *            normalized word
     * @param hyphenatedword
     *            a vector of alternating strings and {@link Hyphen hyphen}
     *            objects.
     */
    @Override
    public void addException(final String word, final ArrayList hyphenatedword) {
        this.stoplist.put(word, hyphenatedword);
    }

    /**
     * Add a pattern to the tree. Mainly, to be used by {@link PatternParser
     * PatternParser} class as callback to add a pattern to the tree.
     *
     * @param pattern
     *            the hyphenation pattern
     * @param ivalue
     *            interletter weight values indicating the desirability and
     *            priority of hyphenating at a given point within the pattern.
     *            It should contain only digit characters. (i.e. '0' to '9').
     */
    @Override
    public void addPattern(final String pattern, final String ivalue) {
        int k = this.ivalues.find(ivalue);
        if (k <= 0) {
            k = packValues(ivalue);
            this.ivalues.insert(ivalue, (char) k);
        }
        insert(pattern, (char) k);
    }

    /**
     * Print statistics.
     */
    @Override
    public void printStats() {
        log.info("Value space size = " + Integer.toString(this.vspace.length()));
        super.printStats();

    }

    /**
     * Main entry point for this hyphenation utility application.
     *
     * @param argv
     *            array of command linee arguments
     * @throws Exception
     *             in case an exception is raised but not caught
     */
    public static void main(final String[] argv) throws Exception {
        HyphenationTree ht = null;
        int minCharCount = 2;
        final BufferedReader in = new BufferedReader(
                new java.io.InputStreamReader(System.in));
        while (true) {
            System.out.print("l:\tload patterns from XML\n"
                    + "L:\tload patterns from serialized object\n"
                    + "s:\tset minimum character count\n"
                    + "w:\twrite hyphenation tree to object file\n"
                    + "h:\thyphenate\n" + "f:\tfind pattern\n"
                    + "b:\tbenchmark\n" + "q:\tquit\n\n" + "Command:");
            String token = in.readLine().trim();
            if (token.equals("f")) {
                System.out.print("Pattern: ");
                token = in.readLine().trim();
                log.info("Values: " + ht.findPattern(token));
            } else if (token.equals("s")) {
                System.out.print("Minimun value: ");
                token = in.readLine().trim();
                minCharCount = Integer.parseInt(token);
            } else if (token.equals("l")) {
                ht = new HyphenationTree();
                System.out.print("XML file name: ");
                token = in.readLine().trim();
                ht.loadPatterns(token);
            } else if (token.equals("L")) {
                ObjectInputStream ois = null;
                System.out.print("Object file name: ");
                token = in.readLine().trim();
                try {
                    ois = new ObjectInputStream(new FileInputStream(token));
                    ht = (HyphenationTree) ois.readObject();
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
                    oos = new ObjectOutputStream(new FileOutputStream(token));
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
        }

    }

}
