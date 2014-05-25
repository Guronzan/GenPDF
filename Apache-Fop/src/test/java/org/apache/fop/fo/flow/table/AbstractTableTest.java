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

/* $Id: AbstractTableTest.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.fo.flow.table;

import java.io.FileInputStream;
import java.util.Iterator;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.FODocumentParser;
import org.apache.fop.fo.FODocumentParser.FOEventHandlerFactory;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.util.ConsoleEventListenerForTests;

/**
 * Superclass for testcases related to tables, factoring the common stuff.
 */
abstract class AbstractTableTest {

    private FODocumentParser documentParser;

    private TableHandler tableHandler;

    protected void setUp(final String filename) throws Exception {
        createDocumentParser();
        this.documentParser.setEventListener(new ConsoleEventListenerForTests(
                filename));
        this.documentParser.parse(new FileInputStream("test/fotree/unittests/"
                + filename));
    }

    private void createDocumentParser() {
        this.documentParser = FODocumentParser
                .newInstance(new FOEventHandlerFactory() {
                    @Override
                    public FOEventHandler newFOEventHandler(
                            final FOUserAgent foUserAgent) {
                        AbstractTableTest.this.tableHandler = new TableHandler(
                                foUserAgent);
                        return AbstractTableTest.this.tableHandler;
                    }
                });
    }

    protected TableHandler getTableHandler() {
        return this.tableHandler;
    }

    protected Iterator getTableIterator() {
        return this.tableHandler.getTables().iterator();
    }
}