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
#include <pango/pango.h>
#include <stdint.h>
#include <stdlib.h>

#define SHAPE_NATIVE Java_com_sun_javafx_font_freetype_PangoGlyphGeometry_shape

typedef struct {
    int cached;
    jclass glyphStringClass;
    jmethodID constructor;
    jfieldID offset;
    jfieldID length;
    jfieldID numChars;
    jfieldID font;
    jfieldID numGlyphs;
    jfieldID glyphs;
    jfieldID widths;
    jfieldID xOffsets;
    jfieldID yOffsets;
    jfieldID logClusters;
} GlyphStringFields;

static GlyphStringFields glyphStringFields;

static jboolean cacheGlyphStringFields(JNIEnv *environment) {
    if (glyphStringFields.cached) {
        return JNI_TRUE;
    }

    jclass localClass = (*environment)->FindClass(
            environment, "com/sun/javafx/font/freetype/PangoGlyphString");
    if (localClass == NULL) {
        return JNI_FALSE;
    }
    glyphStringFields.glyphStringClass =
            (jclass)(*environment)->NewGlobalRef(environment, localClass);
    (*environment)->DeleteLocalRef(environment, localClass);
    if (glyphStringFields.glyphStringClass == NULL) {
        return JNI_FALSE;
    }

    jclass glyphStringClass = glyphStringFields.glyphStringClass;
    glyphStringFields.constructor = (*environment)->GetMethodID(
            environment, glyphStringClass, "<init>", "()V");
    glyphStringFields.offset = (*environment)->GetFieldID(
            environment, glyphStringClass, "offset", "I");
    glyphStringFields.length = (*environment)->GetFieldID(
            environment, glyphStringClass, "length", "I");
    glyphStringFields.numChars = (*environment)->GetFieldID(
            environment, glyphStringClass, "num_chars", "I");
    glyphStringFields.font = (*environment)->GetFieldID(
            environment, glyphStringClass, "font", "J");
    glyphStringFields.numGlyphs = (*environment)->GetFieldID(
            environment, glyphStringClass, "num_glyphs", "I");
    glyphStringFields.glyphs = (*environment)->GetFieldID(
            environment, glyphStringClass, "glyphs", "[I");
    glyphStringFields.widths = (*environment)->GetFieldID(
            environment, glyphStringClass, "widths", "[I");
    glyphStringFields.xOffsets = (*environment)->GetFieldID(
            environment, glyphStringClass, "xOffsets", "[I");
    glyphStringFields.yOffsets = (*environment)->GetFieldID(
            environment, glyphStringClass, "yOffsets", "[I");
    glyphStringFields.logClusters = (*environment)->GetFieldID(
            environment, glyphStringClass, "log_clusters", "[I");
    if ((*environment)->ExceptionCheck(environment)) {
        return JNI_FALSE;
    }

    glyphStringFields.cached = 1;
    return JNI_TRUE;
}

static jintArray newIntArray(JNIEnv *environment, const jint *numbers, int count) {
    jintArray javaNumbers = (*environment)->NewIntArray(environment, count);
    if (javaNumbers == NULL) {
        return NULL;
    }
    (*environment)->SetIntArrayRegion(environment, javaNumbers, 0, count, numbers);
    if ((*environment)->ExceptionCheck(environment)) {
        return NULL;
    }
    return javaNumbers;
}

static jobject createJavaGlyphString(
        JNIEnv *environment, PangoItem *pangoItem, int glyphCount, jint *geometry) {
    if (!cacheGlyphStringFields(environment)) {
        return NULL;
    }

    jfieldID arrayFields[] = {
            glyphStringFields.glyphs,
            glyphStringFields.widths,
            glyphStringFields.xOffsets,
            glyphStringFields.yOffsets,
            glyphStringFields.logClusters
    };
    jintArray javaArrays[5];
    for (int arrayIndex = 0; arrayIndex < 5; arrayIndex++) {
        javaArrays[arrayIndex] = newIntArray(
                environment, geometry + (arrayIndex * glyphCount), glyphCount);
        if (javaArrays[arrayIndex] == NULL) {
            return NULL;
        }
    }

    jobject javaGlyphString = (*environment)->NewObject(
            environment, glyphStringFields.glyphStringClass,
            glyphStringFields.constructor);
    if (javaGlyphString == NULL) {
        return NULL;
    }
    (*environment)->SetIntField(
            environment, javaGlyphString, glyphStringFields.offset, pangoItem->offset);
    (*environment)->SetIntField(
            environment, javaGlyphString, glyphStringFields.length, pangoItem->length);
    (*environment)->SetIntField(
            environment, javaGlyphString, glyphStringFields.numChars, pangoItem->num_chars);
    (*environment)->SetLongField(
            environment, javaGlyphString, glyphStringFields.font,
            (jlong)(intptr_t)pangoItem->analysis.font);
    (*environment)->SetIntField(
            environment, javaGlyphString, glyphStringFields.numGlyphs, glyphCount);
    for (int arrayIndex = 0; arrayIndex < 5; arrayIndex++) {
        (*environment)->SetObjectField(
                environment, javaGlyphString, arrayFields[arrayIndex], javaArrays[arrayIndex]);
    }
    return javaGlyphString;
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
    if (glyphCount <= 0 || (size_t)glyphCount > SIZE_MAX / (5 * sizeof(jint))) {
        pango_glyph_string_free(shapedGlyphs);
        return NULL;
    }

    jint *geometry = (jint *)malloc((size_t)glyphCount * 5 * sizeof(jint));
    if (geometry == NULL) {
        pango_glyph_string_free(shapedGlyphs);
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
        clusters[glyphIndex] = (jint)g_utf8_pointer_to_offset(
                text, text + shapedGlyphs->log_clusters[glyphIndex]);
    }

    jobject javaGlyphString = createJavaGlyphString(
            environment, pangoItem, glyphCount, geometry);

    free(geometry);
    pango_glyph_string_free(shapedGlyphs);
    return javaGlyphString;
}
