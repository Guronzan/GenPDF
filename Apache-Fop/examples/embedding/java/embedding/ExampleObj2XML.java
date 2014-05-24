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

/* $Id: ExampleObj2XML.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package embedding;

//Hava
import java.io.File;
import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
//JAXP
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import lombok.extern.slf4j.Slf4j;
import embedding.model.ProjectMember;
import embedding.model.ProjectTeam;

/**
 * This class demonstrates the conversion of an arbitrary object file to an XML
 * file.
 */
@Slf4j
public class ExampleObj2XML {

    /**
     * Converts a ProjectTeam object to XML.
     *
     * @param team
     *            the ProjectTeam object
     * @param xml
     *            the target XML file
     * @throws IOException
     *             In case of an I/O problem
     * @throws TransformerException
     *             In case of a XSL transformation problem
     */
    public void convertProjectTeam2XML(final ProjectTeam team, final File xml)
            throws IOException, TransformerException {

        // Setup XSLT
        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer transformer = factory.newTransformer();
        /*
         * Note: We use the identity transformer, no XSL transformation is done.
         * The transformer is basically just used to serialize the generated
         * document to XML.
         */

        // Setup input
        final Source src = team.getSourceForProjectTeam();

        // Setup output
        final Result res = new StreamResult(xml);

        // Start XSLT transformation
        transformer.transform(src, res);
    }

    /**
     * Creates a sample ProjectTeam instance for this demo.
     *
     * @return ProjectTeam the newly created ProjectTeam instance
     */
    public static ProjectTeam createSampleProjectTeam() {
        final ProjectTeam team = new ProjectTeam();
        team.setProjectName("Rule the Galaxy");
        team.addMember(new ProjectMember("Emperor Palpatine", "lead",
                "palpatine@empire.gxy"));
        team.addMember(new ProjectMember("Lord Darth Vader", "Jedi-Killer",
                "vader@empire.gxy"));
        team.addMember(new ProjectMember("Grand Moff Tarkin", "Planet-Killer",
                "tarkin@empire.gxy"));
        team.addMember(new ProjectMember("Admiral Motti",
                "Death Star operations", "motti@empire.gxy"));
        return team;
    }

    /**
     * Main method.
     *
     * @param args
     *            command-line arguments
     */
    public static void main(final String[] args) {
        try {
            log.info("FOP ExampleObj2XML\n");
            log.info("Preparing...");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output
            final File xmlfile = new File(outDir, "ResultObj2XML.xml");

            log.info("Input: a ProjectTeam object");
            log.info("Output: XML (" + xmlfile + ")");
            log.info("");
            log.info("Serializing...");

            final ExampleObj2XML app = new ExampleObj2XML();
            app.convertProjectTeam2XML(createSampleProjectTeam(), xmlfile);

            log.info("Success!");
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
