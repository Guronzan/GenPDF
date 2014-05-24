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

/* $Id: DestinationComparator.java 1296437 2012-03-02 20:24:40Z gadams $ */

package org.apache.fop.pdf;

/**
 * Comparator class to enable comparing (and hence sorting) of PDFDestination
 * objects.
 */
public class DestinationComparator implements
java.util.Comparator<PDFDestination> {
    @Override
    public int compare(final PDFDestination dest1, final PDFDestination dest2) {
        return dest1.getIDRef().compareTo(dest2.getIDRef());
    }

}
