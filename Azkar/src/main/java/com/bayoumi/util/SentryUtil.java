package com.bayoumi.util;

import io.sentry.Sentry;
import io.sentry.SentryLevel;

public class SentryUtil {
    public static void init() throws Exception {
        if (!Constants.RUNNING_MODE.equals(Constants.Mode.DEVELOPMENT)) {
            Sentry.init(options -> {
                options.setEnableExternalConfiguration(true);
                options.setTracesSampleRate(0.2);
                options.setDiagnosticLevel(SentryLevel.DEBUG);
                options.setDebug(true);
                options.setRelease("azkar.app@" + Constants.VERSION + "+1");
            });
            Sentry.setTag("os.name", System.getProperty("os.name"));
            Sentry.setTag("os.version", System.getProperty("os.version"));
            Sentry.setTag("os.arch", System.getProperty("os.arch"));
            Sentry.setTag("java.version", System.getProperty("java.version"));
        }
    }
}
