package com.bayoumi.util;

import io.sentry.Sentry;
import io.sentry.SentryLevel;

public class SentryUtil {


    public static void init() throws Exception {
        if (!Constants.RUNNING_MODE.equals(Constants.Mode.DEVELOPMENT)) {
            Sentry.init(options -> {
                options.setEnableExternalConfiguration(true);
                options.setTracesSampleRate(0.2);
                options.setDiagnosticLevel(SentryLevel.ERROR);
                options.setDebug(false);
                options.setRelease("azkar.app@" + Constants.VERSION + "+1");
            });
            AppPropertiesUtil.getProps().forEach(Sentry::setTag);
        }
    }
}
