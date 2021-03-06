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

/* $Id: PDFDests.java 611114 2008-01-11 09:04:28Z jeremias $ */

package org.apache.fop.pdf;

import java.util.List;

/**
 * class representing an /Dests dictionary object
 */
public class PDFDests extends PDFNameTreeNode {

    /**
     * Create a named destination
     */
    public PDFDests() {
        /* generic creation of PDF object */
        super();
    }

    /**
     * Create a named destination
     * 
     * @param destinationList
     *            a list of destinations
     */
    public PDFDests(final List destinationList) {
        this();
        setNames(new PDFArray(this, destinationList));
    }

}
