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

/* $Id: ConvertUtils.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.visual;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;

/**
 * Utilities for converting files with external converters.
 */
public class ConvertUtils {

    /**
     * Calls an external converter application (GhostScript, for example).
     *
     * @param cmd
     *            the full command
     * @param envp
     *            array of strings, each element of which has environment
     *            variable settings in format name=value.
     * @param workDir
     *            the working directory of the subprocess, or null if the
     *            subprocess should inherit the working directory of the current
     *            process.
     * @param log
     *            the logger to log output by the external application to
     * @throws IOException
     *             in case the external call fails
     */
    public static void convert(final String cmd, final String[] envp,
            final File workDir, final Logger log) throws IOException {
        log.debug(cmd);

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmd, envp, null);

            // Redirect stderr output
            final RedirectorLineHandler errorHandler = new AbstractRedirectorLineHandler() {
                @Override
                public void handleLine(final String line) {
                    log.error("ERR> " + line);
                }
            };
            final StreamRedirector errorRedirector = new StreamRedirector(
                    process.getErrorStream(), errorHandler);

            // Redirect stdout output
            final RedirectorLineHandler outputHandler = new AbstractRedirectorLineHandler() {
                @Override
                public void handleLine(final String line) {
                    log.debug("OUT> " + line);
                }
            };
            final StreamRedirector outputRedirector = new StreamRedirector(
                    process.getInputStream(), outputHandler);
            new Thread(errorRedirector).start();
            new Thread(outputRedirector).start();

            process.waitFor();
        } catch (final java.lang.InterruptedException ie) {
            throw new IOException("The call to the external converter failed: "
                    + ie.getMessage());
        } catch (final java.io.IOException ioe) {
            throw new IOException("The call to the external converter failed: "
                    + ioe.getMessage());
        }

        final int exitValue = process.exitValue();
        if (exitValue != 0) {
            throw new IOException(
                    "The call to the external converter failed. Result: "
                            + exitValue);
        }

    }

}
