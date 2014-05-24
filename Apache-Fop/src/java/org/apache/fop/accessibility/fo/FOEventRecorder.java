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

package org.apache.fop.accessibility.fo;

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.flow.BasicLink;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.flow.BlockContainer;
import org.apache.fop.fo.flow.Character;
import org.apache.fop.fo.flow.ExternalGraphic;
import org.apache.fop.fo.flow.Footnote;
import org.apache.fop.fo.flow.FootnoteBody;
import org.apache.fop.fo.flow.Inline;
import org.apache.fop.fo.flow.InstreamForeignObject;
import org.apache.fop.fo.flow.Leader;
import org.apache.fop.fo.flow.ListBlock;
import org.apache.fop.fo.flow.ListItem;
import org.apache.fop.fo.flow.ListItemBody;
import org.apache.fop.fo.flow.ListItemLabel;
import org.apache.fop.fo.flow.PageNumber;
import org.apache.fop.fo.flow.PageNumberCitation;
import org.apache.fop.fo.flow.PageNumberCitationLast;
import org.apache.fop.fo.flow.Wrapper;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableBody;
import org.apache.fop.fo.flow.table.TableCell;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.flow.table.TableFooter;
import org.apache.fop.fo.flow.table.TableHeader;
import org.apache.fop.fo.flow.table.TableRow;

final class FOEventRecorder extends FOEventHandler {

    private interface Event {
        void replay(final FOEventHandler target);
    }

    private final List<Event> events = new ArrayList<Event>();

    public void replay(final FOEventHandler target) {
        for (final Event event : this.events) {
            event.replay(target);
        }
    }

    @Override
    public void startPageNumber(final PageNumber pagenum) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startPageNumber(pagenum);
            }
        });
    }

    @Override
    public void endPageNumber(final PageNumber pagenum) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endPageNumber(pagenum);
            }
        });
    }

    @Override
    public void startPageNumberCitation(final PageNumberCitation pageCite) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startPageNumberCitation(pageCite);
            }
        });
    }

    @Override
    public void endPageNumberCitation(final PageNumberCitation pageCite) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endPageNumberCitation(pageCite);
            }
        });
    }

    @Override
    public void startPageNumberCitationLast(
            final PageNumberCitationLast pageLast) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startPageNumberCitationLast(pageLast);
            }
        });
    }

    @Override
    public void endPageNumberCitationLast(final PageNumberCitationLast pageLast) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endPageNumberCitationLast(pageLast);
            }
        });
    }

    @Override
    public void startBlock(final Block bl) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startBlock(bl);
            }
        });
    }

    @Override
    public void endBlock(final Block bl) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endBlock(bl);
            }
        });
    }

    @Override
    public void startBlockContainer(final BlockContainer blc) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startBlockContainer(blc);
            }
        });
    }

    @Override
    public void endBlockContainer(final BlockContainer blc) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endBlockContainer(blc);
            }
        });
    }

    @Override
    public void startInline(final Inline inl) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startInline(inl);
            }
        });
    }

    @Override
    public void endInline(final Inline inl) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endInline(inl);
            }
        });
    }

    @Override
    public void startTable(final Table tbl) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startTable(tbl);
            }
        });
    }

    @Override
    public void endTable(final Table tbl) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endTable(tbl);
            }
        });
    }

    @Override
    public void startColumn(final TableColumn tc) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startColumn(tc);
            }
        });
    }

    @Override
    public void endColumn(final TableColumn tc) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endColumn(tc);
            }
        });
    }

    @Override
    public void startHeader(final TableHeader header) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startHeader(header);
            }
        });
    }

    @Override
    public void endHeader(final TableHeader header) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endHeader(header);
            }
        });
    }

    @Override
    public void startFooter(final TableFooter footer) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startFooter(footer);
            }
        });
    }

    @Override
    public void endFooter(final TableFooter footer) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endFooter(footer);
            }
        });
    }

    @Override
    public void startBody(final TableBody body) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startBody(body);
            }
        });
    }

    @Override
    public void endBody(final TableBody body) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endBody(body);
            }
        });
    }

    @Override
    public void startRow(final TableRow tr) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startRow(tr);
            }
        });
    }

    @Override
    public void endRow(final TableRow tr) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endRow(tr);
            }
        });
    }

    @Override
    public void startCell(final TableCell tc) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startCell(tc);
            }
        });
    }

    @Override
    public void endCell(final TableCell tc) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endCell(tc);
            }
        });
    }

    @Override
    public void startList(final ListBlock lb) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startList(lb);
            }
        });
    }

    @Override
    public void endList(final ListBlock lb) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endList(lb);
            }
        });
    }

    @Override
    public void startListItem(final ListItem li) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startListItem(li);
            }
        });
    }

    @Override
    public void endListItem(final ListItem li) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endListItem(li);
            }
        });
    }

    @Override
    public void startListLabel(final ListItemLabel listItemLabel) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startListLabel(listItemLabel);
            }
        });
    }

    @Override
    public void endListLabel(final ListItemLabel listItemLabel) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endListLabel(listItemLabel);
            }
        });
    }

    @Override
    public void startListBody(final ListItemBody listItemBody) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startListBody(listItemBody);
            }
        });
    }

    @Override
    public void endListBody(final ListItemBody listItemBody) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endListBody(listItemBody);
            }
        });
    }

    @Override
    public void startLink(final BasicLink basicLink) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startLink(basicLink);
            }
        });
    }

    @Override
    public void endLink(final BasicLink basicLink) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endLink(basicLink);
            }
        });
    }

    @Override
    public void image(final ExternalGraphic eg) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.image(eg);
            }
        });
    }

    @Override
    public void startInstreamForeignObject(final InstreamForeignObject ifo) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startInstreamForeignObject(ifo);
            }
        });
    }

    @Override
    public void endInstreamForeignObject(final InstreamForeignObject ifo) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endInstreamForeignObject(ifo);
            }
        });
    }

    @Override
    public void startFootnote(final Footnote footnote) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startFootnote(footnote);
            }
        });
    }

    @Override
    public void endFootnote(final Footnote footnote) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endFootnote(footnote);
            }
        });
    }

    @Override
    public void startFootnoteBody(final FootnoteBody body) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startFootnoteBody(body);
            }
        });
    }

    @Override
    public void endFootnoteBody(final FootnoteBody body) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endFootnoteBody(body);
            }
        });
    }

    @Override
    public void startLeader(final Leader l) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startLeader(l);
            }
        });
    }

    @Override
    public void endLeader(final Leader l) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endLeader(l);
            }
        });
    }

    @Override
    public void startWrapper(final Wrapper wrapper) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.startWrapper(wrapper);
            }
        });
    }

    @Override
    public void endWrapper(final Wrapper wrapper) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.endWrapper(wrapper);
            }
        });
    }

    @Override
    public void character(final Character c) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.character(c);
            }
        });
    }

    @Override
    public void characters(final FOText foText) {
        this.events.add(new Event() {
            @Override
            public void replay(final FOEventHandler target) {
                target.characters(foText);
            }
        });
    }

}
