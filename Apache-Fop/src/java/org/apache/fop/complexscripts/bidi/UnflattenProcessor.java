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

package org.apache.fop.complexscripts.bidi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.fop.area.Area;
import org.apache.fop.area.LinkResolver;
import org.apache.fop.area.inline.BasicLinkArea;
import org.apache.fop.area.inline.FilledArea;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.area.inline.SpaceArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.UnresolvedPageNumber;

// CSOFF: EmptyForIteratorPadCheck
// CSOFF: LineLengthCheck
// CSOFF: NoWhitespaceAfterCheck
// CSOFF: SimplifyBooleanReturnCheck

/**
 * <p>
 * The <code>UnflattenProcessor</code> class is used to reconstruct (by
 * unflattening) a line area's internal area hierarachy after leaf inline area
 * reordering is completed.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
class UnflattenProcessor {
    private final List<InlineArea> il; // list of flattened inline areas being
                                       // unflattened
    private final List<InlineArea> ilNew; // list of unflattened inline areas
                                          // being constructed
    private int iaLevelLast; // last (previous) level of current inline area (if
                             // applicable) or -1
    private TextArea tcOrig; // original text area container
    private TextArea tcNew; // new text area container being constructed
    private final Stack<InlineParent> icOrig; // stack of original inline parent
                                              // containers
    private final Stack<InlineParent> icNew; // stack of new inline parent
                                             // containers being constructed

    UnflattenProcessor(final List<InlineArea> inlines) {
        this.il = inlines;
        this.ilNew = new ArrayList<InlineArea>();
        this.iaLevelLast = -1;
        this.icOrig = new Stack<InlineParent>();
        this.icNew = new Stack<InlineParent>();
    }

    List unflatten() {
        if (this.il != null) {
            for (final Iterator<InlineArea> it = this.il.iterator(); it
                    .hasNext();) {
                process(it.next());
            }
        }
        finishAll();
        return this.ilNew;
    }

    private void process(final InlineArea ia) {
        process(findInlineContainers(ia), findTextContainer(ia), ia);
    }

    private void process(final List<InlineParent> ich, final TextArea tc,
            final InlineArea ia) {
        if (this.tcNew == null || tc != this.tcNew) {
            maybeFinishTextContainer(tc, ia);
            maybeFinishInlineContainers(ich, tc, ia);
            update(ich, tc, ia);
        } else {
            // skip inline area whose text container is the current new text
            // container,
            // which occurs in the context of the inline runs produced by a
            // filled area
        }
    }

    private boolean shouldFinishTextContainer(final TextArea tc,
            final InlineArea ia) {
        if (this.tcOrig != null && tc != this.tcOrig) {
            return true;
        } else if (this.iaLevelLast != -1
                && ia.getBidiLevel() != this.iaLevelLast) {
            return true;
        } else {
            return false;
        }
    }

    private void finishTextContainer() {
        finishTextContainer(null, null);
    }

    private void finishTextContainer(final TextArea tc, final InlineArea ia) {
        if (this.tcNew != null) {
            updateIPD(this.tcNew);
            if (!this.icNew.empty()) {
                this.icNew.peek().addChildArea(this.tcNew);
            } else {
                this.ilNew.add(this.tcNew);
            }
        }
        this.tcNew = null;
    }

    private void maybeFinishTextContainer(final TextArea tc, final InlineArea ia) {
        if (shouldFinishTextContainer(tc, ia)) {
            finishTextContainer(tc, ia);
        }
    }

    private boolean shouldFinishInlineContainer(final List<InlineParent> ich,
            final TextArea tc, final InlineArea ia) {
        if (ich == null || ich.isEmpty()) {
            return !this.icOrig.empty();
        } else {
            if (!this.icOrig.empty()) {
                final InlineParent ic = ich.get(0);
                final InlineParent ic0 = this.icOrig.peek();
                return ic != ic0 && !isInlineParentOf(ic, ic0);
            } else {
                return false;
            }
        }
    }

    private void finishInlineContainer() {
        finishInlineContainer(null, null, null);
    }

    private void finishInlineContainer(final List<InlineParent> ich,
            final TextArea tc, final InlineArea ia) {
        if (ich != null && !ich.isEmpty()) { // finish non-matching inner inline
                                             // container(s)
            for (final Iterator<InlineParent> it = ich.iterator(); it.hasNext();) {
                final InlineParent ic = it.next();
                final InlineParent ic0 = this.icOrig.empty() ? null
                        : this.icOrig.peek();
                if (ic0 == null) {
                    assert this.icNew.empty();
                } else if (ic != ic0) {
                    assert !this.icNew.empty();
                    final InlineParent icO0 = this.icOrig.pop();
                    final InlineParent icN0 = this.icNew.pop();
                    assert icO0 != null;
                    assert icN0 != null;
                    if (this.icNew.empty()) {
                        this.ilNew.add(icN0);
                    } else {
                        this.icNew.peek().addChildArea(icN0);
                    }
                    if (!this.icOrig.empty() && this.icOrig.peek() == ic) {
                        break;
                    }
                } else {
                    break;
                }
            }
        } else { // finish all inline containers
            while (!this.icNew.empty()) {
                final InlineParent icO0 = this.icOrig.pop();
                final InlineParent icN0 = this.icNew.pop();
                assert icO0 != null;
                assert icN0 != null;
                if (this.icNew.empty()) {
                    this.ilNew.add(icN0);
                } else {
                    this.icNew.peek().addChildArea(icN0);
                }
            }
        }
    }

    private void maybeFinishInlineContainers(final List<InlineParent> ich,
            final TextArea tc, final InlineArea ia) {
        if (shouldFinishInlineContainer(ich, tc, ia)) {
            finishInlineContainer(ich, tc, ia);
        }
    }

    private void finishAll() {
        finishTextContainer();
        finishInlineContainer();
    }

    private void update(final List<InlineParent> ich, final TextArea tc,
            final InlineArea ia) {
        if (!alreadyUnflattened(ia)) {
            if (ich != null && !ich.isEmpty()) {
                pushInlineContainers(ich);
            }
            if (tc != null) {
                pushTextContainer(tc, ia);
            } else {
                pushNonTextInline(ia);
            }
            this.iaLevelLast = ia.getBidiLevel();
            this.tcOrig = tc;
        } else if (this.tcNew != null) {
            finishTextContainer();
            this.tcOrig = null;
        } else {
            this.tcOrig = null;
        }
    }

    private boolean alreadyUnflattened(final InlineArea ia) {
        for (final Iterator<InlineArea> it = this.ilNew.iterator(); it
                .hasNext();) {
            if (ia.isAncestorOrSelf(it.next())) {
                return true;
            }
        }
        return false;
    }

    private void pushInlineContainers(final List<InlineParent> ich) {
        final LinkedList<InlineParent> icl = new LinkedList<InlineParent>();
        for (final Iterator<InlineParent> it = ich.iterator(); it.hasNext();) {
            final InlineParent ic = it.next();
            if (this.icOrig.search(ic) >= 0) {
                break;
            } else {
                icl.addFirst(ic);
            }
        }
        for (final Iterator<InlineParent> it = icl.iterator(); it.hasNext();) {
            final InlineParent ic = it.next();
            this.icOrig.push(ic);
            this.icNew.push(generateInlineContainer(ic));
        }
    }

    private void pushTextContainer(final TextArea tc, final InlineArea ia) {
        if (tc instanceof UnresolvedPageNumber) {
            this.tcNew = tc;
        } else {
            if (this.tcNew == null) {
                this.tcNew = generateTextContainer(tc);
            }
            this.tcNew.addChildArea(ia);
        }
    }

    private void pushNonTextInline(final InlineArea ia) {
        if (this.icNew.empty()) {
            this.ilNew.add(ia);
        } else {
            this.icNew.peek().addChildArea(ia);
        }
    }

    private InlineParent generateInlineContainer(final InlineParent i) {
        if (i instanceof BasicLinkArea) {
            return generateBasicLinkArea((BasicLinkArea) i);
        } else if (i instanceof FilledArea) {
            return generateFilledArea((FilledArea) i);
        } else {
            return generateInlineContainer0(i);
        }
    }

    private InlineParent generateBasicLinkArea(final BasicLinkArea l) {
        final BasicLinkArea lc = new BasicLinkArea();
        if (l != null) {
            initializeInlineContainer(lc, l);
            initializeLinkArea(lc, l);
        }
        return lc;
    }

    private void initializeLinkArea(final BasicLinkArea lc,
            final BasicLinkArea l) {
        assert lc != null;
        assert l != null;
        final LinkResolver r = l.getResolver();
        if (r != null) {
            final String[] idrefs = r.getIDRefs();
            if (idrefs.length > 0) {
                final String idref = idrefs[0];
                final LinkResolver lr = new LinkResolver(idref, lc);
                lc.setResolver(lr);
                r.addDependent(lr);
            }
        }
    }

    private InlineParent generateFilledArea(final FilledArea f) {
        final FilledArea fc = new FilledArea();
        if (f != null) {
            initializeInlineContainer(fc, f);
            initializeFilledArea(fc, f);
        }
        return fc;
    }

    private void initializeFilledArea(final FilledArea fc, final FilledArea f) {
        assert fc != null;
        assert f != null;
        fc.setIPD(f.getIPD());
        fc.setUnitWidth(f.getUnitWidth());
    }

    private InlineParent generateInlineContainer0(final InlineParent i) {
        final InlineParent ic = new InlineParent();
        if (i != null) {
            initializeInlineContainer(ic, i);
        }
        return ic;
    }

    private void initializeInlineContainer(final InlineParent ic,
            final InlineParent i) {
        assert ic != null;
        assert i != null;
        ic.setTraits(i.getTraits());
        ic.setBPD(i.getBPD());
        ic.setBlockProgressionOffset(i.getBlockProgressionOffset());
    }

    private TextArea generateTextContainer(final TextArea t) {
        final TextArea tc = new TextArea();
        if (t != null) {
            tc.setTraits(t.getTraits());
            tc.setBPD(t.getBPD());
            tc.setBlockProgressionOffset(t.getBlockProgressionOffset());
            tc.setBaselineOffset(t.getBaselineOffset());
            tc.setTextWordSpaceAdjust(t.getTextWordSpaceAdjust());
            tc.setTextLetterSpaceAdjust(t.getTextLetterSpaceAdjust());
        }
        return tc;
    }

    private void updateIPD(final TextArea tc) {
        int numAdjustable = 0;
        for (final Iterator it = tc.getChildAreas().iterator(); it.hasNext();) {
            final InlineArea ia = (InlineArea) it.next();
            if (ia instanceof SpaceArea) {
                final SpaceArea sa = (SpaceArea) ia;
                if (sa.isAdjustable()) {
                    numAdjustable++;
                }
            }
        }
        if (numAdjustable > 0) {
            tc.setIPD(tc.getIPD() + numAdjustable * tc.getTextWordSpaceAdjust());
        }
    }

    private TextArea findTextContainer(InlineArea ia) {
        assert ia != null;
        TextArea t = null;
        while (t == null) {
            if (ia instanceof TextArea) {
                t = (TextArea) ia;
            } else {
                final Area p = ia.getParentArea();
                if (p instanceof InlineArea) {
                    ia = (InlineArea) p;
                } else {
                    break;
                }
            }
        }
        return t;
    }

    private List<InlineParent> findInlineContainers(final InlineArea ia) {
        assert ia != null;
        final List<InlineParent> ich = new ArrayList<InlineParent>();
        Area a = ia.getParentArea();
        while (a != null) {
            if (a instanceof InlineArea) {
                if (a instanceof InlineParent && !(a instanceof TextArea)) {
                    ich.add((InlineParent) a);
                }
                a = ((InlineArea) a).getParentArea();
            } else {
                a = null;
            }
        }
        return ich;
    }

    private boolean isInlineParentOf(final InlineParent ic0,
            final InlineParent ic1) {
        assert ic0 != null;
        return ic0.getParentArea() == ic1;
    }
}
