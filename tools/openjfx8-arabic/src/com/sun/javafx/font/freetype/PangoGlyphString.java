/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

class PangoGlyphString {
    // Keep the stock JNI fields for runtimes where the geometry library cannot load.
    int offset;
    int length;
    int num_chars;
    long font;
    int num_glyphs;
    int[] glyphs;
    int[] widths;
    int[] log_clusters;
    int slot;
    private int[] geometry;

    PangoGlyphString() {
    }

    PangoGlyphString(long font, int[] geometry) {
        this.font = font;
        this.num_glyphs = geometry.length / 5;
        this.geometry = geometry;
    }

    int glyphAt(int index) {
        return geometry == null ? glyphs[index] : geometry[index];
    }

    int widthAt(int index) {
        return geometry == null ? widths[index] : geometry[num_glyphs + index];
    }

    int xOffsetAt(int index) {
        return geometry == null ? 0 : geometry[2 * num_glyphs + index];
    }

    int yOffsetAt(int index) {
        return geometry == null ? 0 : geometry[3 * num_glyphs + index];
    }

    int clusterAt(int index) {
        return geometry == null ? log_clusters[index] : geometry[4 * num_glyphs + index];
    }
}
