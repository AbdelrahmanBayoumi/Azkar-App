package com.bayoumi.util.gui;

import com.bayoumi.util.OSUtil;

public class ArabicTextSupport {
    public static void fix() {
        if (OSUtil.isLinux()) {
            // Pango applies Unicode bidi mirroring; T2K keeps the legacy bracket shapes.
            System.setProperty("prism.text", "native");
            System.setProperty("prism.lcdtext", "true");
            return;
        }
        System.setProperty("prism.text", "t2k");
        System.setProperty("prism.lcdtext", "false");
    }
}
