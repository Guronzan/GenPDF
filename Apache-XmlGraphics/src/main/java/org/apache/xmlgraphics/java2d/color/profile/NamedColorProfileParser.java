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

/* $Id: NamedColorProfileParser.java 1051421 2010-12-21 08:54:25Z jeremias $ */

package org.apache.xmlgraphics.java2d.color.profile;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

import org.apache.xmlgraphics.java2d.color.CIELabColorSpace;
import org.apache.xmlgraphics.java2d.color.ColorSpaces;
import org.apache.xmlgraphics.java2d.color.NamedColorSpace;
import org.apache.xmlgraphics.java2d.color.RenderingIntent;

/**
 * This class is a parser for ICC named color profiles. It uses Java's
 * {@link ICC_Profile} class for parsing the basic structure but adds
 * functionality to parse certain profile tags.
 */
public class NamedColorProfileParser {

    private static final int MLUC = 0x6D6C7563; // 'mluc'
    private static final int NCL2 = 0x6E636C32; // 'ncl2'

    /**
     * Indicates whether the profile is a named color profile.
     *
     * @param profile
     *            the color profile
     * @return true if the profile is a named color profile, false otherwise
     */
    public static boolean isNamedColorProfile(final ICC_Profile profile) {
        return profile.getProfileClass() == ICC_Profile.CLASS_NAMEDCOLOR;
    }

    /**
     * Parses a named color profile (NCP).
     *
     * @param profile
     *            the profile to analyze
     * @param profileName
     *            Optional profile name associated with this color profile
     * @param profileURI
     *            Optional profile URI associated with this color profile
     * @return an object representing the parsed NCP
     * @throws IOException
     *             if an I/O error occurs
     */
    public NamedColorProfile parseProfile(final ICC_Profile profile,
            final String profileName, final String profileURI)
                    throws IOException {
        if (!isNamedColorProfile(profile)) {
            throw new IllegalArgumentException(
                    "Given profile is not a named color profile (NCP)");
        }
        final String profileDescription = getProfileDescription(profile);
        final String copyright = getCopyright(profile);
        final RenderingIntent intent = getRenderingIntent(profile);
        final NamedColorSpace[] ncs = readNamedColors(profile, profileName,
                profileURI);
        return new NamedColorProfile(profileDescription, copyright, ncs, intent);
    }

    /**
     * Parses a named color profile (NCP).
     *
     * @param profile
     *            the profile to analyze
     * @return an object representing the parsed NCP
     * @throws IOException
     *             if an I/O error occurs
     */
    public NamedColorProfile parseProfile(final ICC_Profile profile)
            throws IOException {
        return parseProfile(profile, null, null);
    }

    private String getProfileDescription(final ICC_Profile profile)
            throws IOException {
        final byte[] tag = profile
                .getData(ICC_Profile.icSigProfileDescriptionTag);
        return readSimpleString(tag);
    }

    private String getCopyright(final ICC_Profile profile) throws IOException {
        final byte[] tag = profile.getData(ICC_Profile.icSigCopyrightTag);
        return readSimpleString(tag);
    }

    private RenderingIntent getRenderingIntent(final ICC_Profile profile) {
        final byte[] hdr = profile.getData(ICC_Profile.icSigHead);
        final int value = hdr[ICC_Profile.icHdrRenderingIntent];
        return RenderingIntent.fromICCValue(value);
    }

    private NamedColorSpace[] readNamedColors(final ICC_Profile profile,
            final String profileName, final String profileURI)
                    throws IOException {
        final byte[] tag = profile.getData(ICC_Profile.icSigNamedColor2Tag);
        final DataInput din = new DataInputStream(new ByteArrayInputStream(tag));
        final int sig = din.readInt();
        if (sig != NCL2) {
            throw new UnsupportedOperationException(
                    "Unsupported structure type: " + toSignatureString(sig)
                    + ". Expected " + toSignatureString(NCL2));
        }
        din.skipBytes(8);
        final int numColors = din.readInt();
        final NamedColorSpace[] result = new NamedColorSpace[numColors];
        final int numDeviceCoord = din.readInt();
        final String prefix = readAscii(din, 32);
        final String suffix = readAscii(din, 32);
        for (int i = 0; i < numColors; i++) {
            final String name = prefix + readAscii(din, 32) + suffix;
            final int[] pcs = readUInt16Array(din, 3);
            final float[] colorvalue = new float[3];
            for (int j = 0; j < pcs.length; j++) {
                colorvalue[j] = (float) pcs[j] / 0x8000;
            }

            // device coordinates are ignored for now
            /* int[] deviceCoord = */readUInt16Array(din, numDeviceCoord);

            switch (profile.getPCSType()) {
            case ColorSpace.TYPE_XYZ:
                result[i] = new NamedColorSpace(name, colorvalue, profileName,
                        profileURI);
                break;
            case ColorSpace.TYPE_Lab:
                // Not sure if this always D50 here,
                // but the illuminant in the header is fixed to D50.
                final CIELabColorSpace labCS = ColorSpaces
                .getCIELabColorSpaceD50();
                result[i] = new NamedColorSpace(name, labCS.toColor(colorvalue,
                        1.0f), profileName, profileURI);
                break;
            default:
                throw new UnsupportedOperationException(
                        "PCS type is not supported: " + profile.getPCSType());
            }
        }
        return result;
    }

    private int[] readUInt16Array(final DataInput din, final int count)
            throws IOException {
        if (count == 0) {
            return null;
        }
        final int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            final int v = din.readUnsignedShort();
            result[i] = v;
        }
        return result;
    }

    private String readAscii(final DataInput in, final int maxLength)
            throws IOException {
        final byte[] data = new byte[maxLength];
        in.readFully(data);
        String result = new String(data, "US-ASCII");
        final int idx = result.indexOf('\0');
        if (idx >= 0) {
            result = result.substring(0, idx);
        }
        return result;
    }

    private String readSimpleString(final byte[] tag) throws IOException {
        final DataInput din = new DataInputStream(new ByteArrayInputStream(tag));
        final int sig = din.readInt();
        if (sig == MLUC) {
            return readMLUC(din);
        } else {
            return null; // Unsupported tag structure type
        }
    }

    private String readMLUC(final DataInput din) throws IOException {
        din.skipBytes(16);
        final int firstLength = din.readInt();
        final int firstOffset = din.readInt();
        final int offset = 28;
        din.skipBytes(firstOffset - offset);
        final byte[] utf16 = new byte[firstLength];
        din.readFully(utf16);
        return new String(utf16, "UTF-16BE");
    }

    private String toSignatureString(final int sig) {
        final StringBuilder sb = new StringBuilder();
        sb.append((char) (sig >> 24 & 0xFF));
        sb.append((char) (sig >> 16 & 0xFF));
        sb.append((char) (sig >> 8 & 0xFF));
        sb.append((char) (sig & 0xFF));
        return sb.toString();
    }

}
