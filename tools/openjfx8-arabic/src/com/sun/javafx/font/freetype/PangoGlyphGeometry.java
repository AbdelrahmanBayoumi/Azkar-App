/*
 * Copyright (c) 2026, Abdelrahman Bayoumi. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.sun.javafx.font.freetype;

import com.sun.glass.utils.NativeLibLoader;

import java.security.AccessController;
import java.security.PrivilegedAction;

public final class PangoGlyphGeometry {
    private static final boolean AVAILABLE = loadBridge();

    private static boolean loadBridge() {
        try {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                NativeLibLoader.loadLibrary("javafx_font_pango_geometry");
                return null;
            });
            // The old bridge exposed shape(), which did not return absolute byte clusters.
            shapeUtf8Clusters(0, 0);
            return true;
        } catch (UnsatisfiedLinkError unavailable) {
            return false;
        }
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    private PangoGlyphGeometry() {
    }

    static native PangoGlyphString shapeUtf8Clusters(long text, long pangoItem);
}
