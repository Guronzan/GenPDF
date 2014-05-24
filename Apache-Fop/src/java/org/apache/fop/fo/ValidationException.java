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

/* $Id: ValidationException.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.fo;

import org.apache.fop.apps.FOPException;
import org.xml.sax.Locator;

/**
 * Exception thrown during FO tree validation.
 */
public class ValidationException extends FOPException {

    /**
     *
     */
    private static final long serialVersionUID = 1837390066992007730L;

    /**
     * Construct a validation exception instance.
     *
     * @param message
     *            a message
     */
    public ValidationException(final String message) {
        super(message);
    }

    /**
     * Construct a validation exception instance.
     *
     * @param message
     *            a message
     * @param locator
     *            a locator
     */
    public ValidationException(final String message, final Locator locator) {
        super(message, locator);
    }
}
