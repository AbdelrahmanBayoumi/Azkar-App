/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.font.freetype;

import com.sun.javafx.font.CompositeFontResource;
import com.sun.javafx.font.CompositeGlyphMapper;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.text.GlyphLayout;
import com.sun.javafx.text.TextRun;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

class PangoGlyphLayout extends GlyphLayout {
    private static final long fontmap;

    static {
        fontmap = OSPango.pango_ft2_font_map_new();
    }

    private int getSlot(PGFont font, PangoGlyphString glyphString) {
        CompositeFontResource fr = (CompositeFontResource)font.getFontResource();
        long fallbackFont = glyphString.font;
        long fallbackFd = OSPango.pango_font_describe(fallbackFont);
        String fallbackFamily = OSPango.pango_font_description_get_family(fallbackFd);
        int fallbackStyle = OSPango.pango_font_description_get_style(fallbackFd);
        int fallbackWeight = OSPango.pango_font_description_get_weight(fallbackFd);
        OSPango.pango_font_description_free(fallbackFd);
        boolean bold = fallbackWeight == OSPango.PANGO_WEIGHT_BOLD;
        boolean italic = fallbackStyle != OSPango.PANGO_STYLE_NORMAL;

        PrismFontFactory prismFactory = PrismFontFactory.getFontFactory();
        PGFont fallbackPGFont = prismFactory.createFont(fallbackFamily, bold,
                                                        italic, font.getSize());
        String fallbackFullname =  fallbackPGFont.getFullName();
        String primaryFullname = fr.getSlotResource(0).getFullName();

        int slot = 0;
        if (!fallbackFullname.equalsIgnoreCase(primaryFullname)) {
            slot = fr.getSlotForFont(fallbackFullname);
            if (PrismFontFactory.debugFonts) {
                System.err.println("\tFallback font= "+ fallbackFullname + " slot=" + (slot>>24));
            }
        }
        return slot;
    }

    private boolean check(long checkValue, String message, long context, long desc, long attrList) {
        if (checkValue != 0) return false;
        if (message != null && PrismFontFactory.debugFonts) {
            System.err.println(message);
        }
        /* pango_attr_list_unref() also frees the attributes it contains */
        if (attrList != 0) OSPango.pango_attr_list_unref(attrList);
        if (desc != 0) OSPango.pango_font_description_free(desc);
        if (context != 0) OSPango.g_object_unref(context);
        return true;
    }

    private static final class Utf8Buffer {
        private final long address;
        private final int start;
        private final int end;
        private final String text;
        private final int[] byteToUtf16;

        private Utf8Buffer(int start, int end, char[] slice) {
            this.start = start;
            this.end = end;
            this.text = new String(slice);
            this.byteToUtf16 = new int[text.length() * 3 + 1];
            int byteOffset = 0;
            for (int offset = 0; offset < text.length();) {
                int codePoint = text.codePointAt(offset);
                byteToUtf16[byteOffset] = offset;
                byteOffset += utf8Length(codePoint);
                offset += Character.charCount(codePoint);
            }
            byteToUtf16[byteOffset] = text.length();
            this.address = OSPango.g_utf16_to_utf8(slice);
        }

        private int clusterIndex(PangoGlyphString shaped, int glyph) {
            if (PangoGlyphGeometry.isAvailable()) {
                return byteToUtf16[shaped.clusterAt(glyph)];
            }
            return text.offsetByCodePoints(byteToUtf16[shaped.offset], shaped.clusterAt(glyph));
        }

        private boolean matches(TextRun run) {
            return start == run.getStart() && end == run.getEnd();
        }
    }

    private static int utf8Length(int codePoint) {
        if (codePoint < 0x80) return 1;
        if (codePoint < 0x800) return 2;
        return Character.isSupplementaryCodePoint(codePoint) ? 4 : 3;
    }

    private final Map<TextRun, Utf8Buffer> runUtf8 = new LinkedHashMap<>();

    public void layout(TextRun run, PGFont font, FontStrike strike, char[] text) {
        /* Create the pango font and attribute list */
        FontResource fr = font.getFontResource();
        boolean composite = fr instanceof CompositeFontResource;
        if (composite) {
            fr = ((CompositeFontResource)fr).getSlotResource(0);
        }
        if (check(fontmap, "Failed allocating PangoFontMap.", 0, 0, 0)) {
            return;
        }
        long context = OSPango.pango_font_map_create_context(fontmap);
        if (check(context, "Failed allocating PangoContext.", 0, 0, 0)) {
            return;
        }
        boolean rtl = (run.getLevel() & 1) != 0;
        if (rtl) {
            OSPango.pango_context_set_base_dir(context, OSPango.PANGO_DIRECTION_RTL);
        }
        float size = font.getSize();
        int style = fr.isItalic() ? OSPango.PANGO_STYLE_ITALIC : OSPango.PANGO_STYLE_NORMAL;
        int weight = fr.isBold() ? OSPango.PANGO_WEIGHT_BOLD : OSPango.PANGO_WEIGHT_NORMAL;
        long desc = OSPango.pango_font_description_new();
        if (check(desc, "Failed allocating FontDescription.", context, 0, 0)) {
            return;
        }
        OSPango.pango_font_description_set_family(desc, fr.getFamilyName());
        OSPango.pango_font_description_set_absolute_size(desc, size * OSPango.PANGO_SCALE);
        OSPango.pango_font_description_set_stretch(desc, OSPango.PANGO_STRETCH_NORMAL);
        OSPango.pango_font_description_set_style(desc, style);
        OSPango.pango_font_description_set_weight(desc, weight);
        long attrList = OSPango.pango_attr_list_new();
        if (check(attrList, "Failed allocating PangoAttributeList.", context, desc, 0)) {
            return;
        }
        long attr = OSPango.pango_attr_font_desc_new(desc);
        if (check(attr, "Failed allocating PangoAttribute.", context, desc, attrList)) {
            return;
        }
        OSPango.pango_attr_list_insert(attrList, attr);
        if (!composite) {
            attr = OSPango.pango_attr_fallback_new(false);
            OSPango.pango_attr_list_insert(attrList, attr);
        }

        try {
            Utf8Buffer buffer = utf8Buffer(run, text);
            if (buffer != null) {
                long length = OSPango.g_utf8_strlen(buffer.address, -1);
                long end = OSPango.g_utf8_offset_to_pointer(buffer.address, length);
                long items = OSPango.pango_itemize(
                        context, buffer.address, 0, (int)(end - buffer.address), attrList, 0);
                if (items != 0) {
                    shapeRun(run, font.getSize(), buffer, shapeItems(items, buffer.address, font));
                }
            }
        } finally {
            check(0, null, context, desc, attrList);
        }
    }

    private Utf8Buffer utf8Buffer(TextRun run, char[] text) {
        Utf8Buffer cached = runUtf8.get(run);
        if (cached != null) {
            if (cached.matches(run)) return cached;
            OSPango.g_free(cached.address);
            runUtf8.remove(run);
        }
        char[] slice = Arrays.copyOfRange(text, run.getStart(), run.getEnd());
        replaceUnpairedSurrogates(slice);
        Utf8Buffer buffer = new Utf8Buffer(run.getStart(), run.getEnd(), slice);
        if (buffer.address == 0) return null;
        runUtf8.put(run, buffer);
        return buffer;
    }

    private static void replaceUnpairedSurrogates(char[] slice) {
        for (int index = 0; index < slice.length; index++) {
            char ch = slice[index];
            if (Character.isHighSurrogate(ch) && index + 1 < slice.length
                    && Character.isLowSurrogate(slice[index + 1])) {
                index++;
            } else if (Character.isSurrogate(ch)) {
                slice[index] = '\uFFFD';
            }
        }
    }

    private PangoGlyphString[] shapeItems(long items, long text, PGFont font) {
        int count = OSPango.g_list_length(items);
        boolean composite = font.getFontResource() instanceof CompositeFontResource;
        try {
            PangoGlyphString[] shaped = new PangoGlyphString[count];
            for (int index = 0; index < count; index++) {
                long item = OSPango.g_list_nth_data(items, index);
                if (item == 0) continue;
                PangoGlyphString glyphs = PangoGlyphGeometry.isAvailable()
                        ? PangoGlyphGeometry.shapeUtf8Clusters(text, item) : OSPango.pango_shape(text, item);
                if (glyphs != null) {
                    // The item holds a reference to the font used to identify the slot.
                    glyphs.slot = composite ? getSlot(font, glyphs) : 0;
                    shaped[index] = glyphs;
                }
            }
            return shaped;
        } finally {
            for (int index = 0; index < count; index++) {
                long item = OSPango.g_list_nth_data(items, index);
                if (item != 0) OSPango.pango_item_free(item);
            }
            OSPango.g_list_free(items);
        }
    }

    private static void shapeRun(TextRun run, float size, Utf8Buffer buffer, PangoGlyphString[] items) {
        int count = 0;
        for (PangoGlyphString item : items) {
            if (item != null) count += item.num_glyphs;
        }
        int[] glyphs = new int[count];
        int[] clusters = new int[count];
        float[] positions = new float[2 * (count + 1)];
        float[] xOffsets = null;
        int index = 0;
        long advance = 0;
        for (int visualIndex = 0; visualIndex < items.length; visualIndex++) {
            // Itemization is logical; each item's glyphs are already in visual order.
            int itemIndex = run.isLeftToRight() ? visualIndex : items.length - 1 - visualIndex;
            PangoGlyphString item = items[itemIndex];
            if (item == null) continue;
            for (int glyph = 0; glyph < item.num_glyphs; glyph++, index++) {
                int id = item.glyphAt(glyph);
                if (item.slot != -1 && 0 <= id && id <= CompositeGlyphMapper.GLYPHMASK) {
                    glyphs[index] = (item.slot << 24) | id;
                }
                clusters[index] = buffer.clusterIndex(item, glyph);
                if (size != 0) {
                    positions[2 * index + 1] = (float)item.yOffsetAt(glyph) / OSPango.PANGO_SCALE;
                    int xOffset = item.xOffsetAt(glyph);
                    if (xOffset != 0) {
                        if (xOffsets == null) xOffsets = new float[count];
                        xOffsets[index] = (float)xOffset / OSPango.PANGO_SCALE;
                    }
                    advance += item.widthAt(glyph);
                    positions[2 * (index + 1)] = (float)advance / OSPango.PANGO_SCALE;
                }
            }
        }
        run.shape(count, glyphs, positions, clusters, xOffsets);
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Utf8Buffer utf8Buffer : runUtf8.values()) {
            OSPango.g_free(utf8Buffer.address);
        }
        runUtf8.clear();
    }
}
