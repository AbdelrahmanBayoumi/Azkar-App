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

#include <jni.h>
#include <limits.h>
#include <pango/pango.h>
#include <stdint.h>
#include <stdlib.h>

#define SHAPE_NATIVE Java_com_sun_javafx_font_freetype_PangoGlyphGeometry_shapeUtf8Clusters

static jobject createJavaGlyphString(
        JNIEnv *environment, PangoItem *pangoItem, jsize length, const jint *geometry) {
    jclass glyphStringClass = (*environment)->FindClass(
            environment, "com/sun/javafx/font/freetype/PangoGlyphString");
    if (glyphStringClass == NULL) {
        return NULL;
    }
    jmethodID constructor = (*environment)->GetMethodID(
            environment, glyphStringClass, "<init>", "(J[I)V");
    if (constructor == NULL) {
        return NULL;
    }
    jintArray glyphData = (*environment)->NewIntArray(environment, length);
    if (glyphData == NULL) {
        return NULL;
    }
    (*environment)->SetIntArrayRegion(environment, glyphData, 0, length, geometry);
    if ((*environment)->ExceptionCheck(environment)) {
        return NULL;
    }
    return (*environment)->NewObject(
            environment, glyphStringClass, constructor,
            (jlong)(intptr_t)pangoItem->analysis.font, glyphData);
}

JNIEXPORT jobject JNICALL SHAPE_NATIVE(
        JNIEnv *environment, jclass ignoredClass, jlong textAddress, jlong itemAddress) {
    (void)ignoredClass;
    if (textAddress == 0 || itemAddress == 0) {
        return NULL;
    }

    PangoItem *pangoItem = (PangoItem *)(intptr_t)itemAddress;
    const gchar *text = (const gchar *)(intptr_t)textAddress + pangoItem->offset;
    PangoGlyphString *shapedGlyphs = pango_glyph_string_new();
    if (shapedGlyphs == NULL) {
        return NULL;
    }
    pango_shape(text, pangoItem->length, &pangoItem->analysis, shapedGlyphs);

    int glyphCount = shapedGlyphs->num_glyphs;
    if (glyphCount <= 0) {
        pango_glyph_string_free(shapedGlyphs);
        return NULL;
    }

    jint *geometry = NULL;
    if (glyphCount <= INT_MAX / 5 && (size_t)glyphCount <= SIZE_MAX / (5 * sizeof(jint))) {
        geometry = malloc((size_t)glyphCount * 5 * sizeof(jint));
    }
    if (geometry == NULL) {
        pango_glyph_string_free(shapedGlyphs);
        jclass errorClass = (*environment)->FindClass(environment, "java/lang/OutOfMemoryError");
        if (errorClass != NULL) {
            (*environment)->ThrowNew(environment, errorClass, "Pango glyph geometry");
        }
        return NULL;
    }
    jint *glyphs = geometry;
    jint *widths = glyphs + glyphCount;
    jint *xOffsets = widths + glyphCount;
    jint *yOffsets = xOffsets + glyphCount;
    jint *clusters = yOffsets + glyphCount;
    for (int glyphIndex = 0; glyphIndex < glyphCount; glyphIndex++) {
        PangoGlyphInfo *glyph = &shapedGlyphs->glyphs[glyphIndex];
        glyphs[glyphIndex] = (jint)glyph->glyph;
        widths[glyphIndex] = (jint)glyph->geometry.width;
        xOffsets[glyphIndex] = (jint)glyph->geometry.x_offset;
        yOffsets[glyphIndex] = (jint)glyph->geometry.y_offset;
        clusters[glyphIndex] = pangoItem->offset + shapedGlyphs->log_clusters[glyphIndex];
    }

    jobject javaGlyphString = createJavaGlyphString(
            environment, pangoItem, (jsize)(5 * glyphCount), geometry);

    free(geometry);
    pango_glyph_string_free(shapedGlyphs);
    return javaGlyphString;
}
