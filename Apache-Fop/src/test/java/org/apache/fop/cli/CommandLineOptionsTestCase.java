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

/* $Id: CommandLineOptions.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.cli;

import java.io.IOException;

import org.apache.fop.apps.FOPException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandLineOptionsTestCase {

    private final CommandLineOptions clo = new CommandLineOptions();
    private final String commandLine = "-fo examples/fo/basic/simple.fo -print";
    private String[] cmd;
    private boolean parsed;

    @Before
    public void setUp() throws Exception {
        this.cmd = this.commandLine.split(" ");
        this.parsed = this.clo.parse(this.cmd);
    }

    @Test
    public void testParse() {
        assertTrue(this.parsed);
    }

    @Test
    public void testGetOutputFormat() throws FOPException {
        assertEquals(this.clo.getOutputFormat(), "application/X-fop-print");
    }

    @Test
    public void testVandVersionSwitchs() throws FOPException, IOException {
        // test -v
        final String cl1 = "-v";
        final String[] cmd1 = cl1.split(" ");
        final CommandLineOptions clo1 = new CommandLineOptions();
        assertTrue(!clo1.parse(cmd1));
        // test -version
        final String cl2 = "-version";
        final String[] cmd2 = cl2.split(" ");
        final CommandLineOptions clo2 = new CommandLineOptions();
        assertTrue(!clo2.parse(cmd2));
        // test -v + more switches
        final String cl3 = "-v " + this.commandLine;
        final String[] cmd3 = cl3.split(" ");
        final CommandLineOptions clo3 = new CommandLineOptions();
        assertTrue(clo3.parse(cmd3));
    }
}
