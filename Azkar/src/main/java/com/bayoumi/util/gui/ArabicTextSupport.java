package com.bayoumi.util.gui;

import com.bayoumi.util.OSUtil;

public class ArabicTextSupport {
    public static void fix() {
        if (OSUtil.isLinux() && hasPangoGeometry()) {
            // Pango applies Unicode bidi mirroring; T2K keeps the legacy bracket shapes.
            System.setProperty("prism.text", "native");
            System.setProperty("prism.lcdtext", "true");
            return;
        }
        System.setProperty("prism.text", "t2k");
        System.setProperty("prism.lcdtext", "false");
    }

    private static boolean hasPangoGeometry() {
        try {
            // Stock JavaFX has no Azkar bridge. Keep its original renderer settings.
            Class<?> bridge = Class.forName("com.sun.javafx.font.freetype.PangoGlyphGeometry");
            return Boolean.TRUE.equals(bridge.getMethod("isAvailable").invoke(null));
        } catch (ReflectiveOperationException | LinkageError unavailable) {
            return false;
        }
    }
}
