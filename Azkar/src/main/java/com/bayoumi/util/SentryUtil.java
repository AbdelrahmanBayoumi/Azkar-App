package com.bayoumi.util;

import com.bayoumi.models.preferences.Preferences;
import io.sentry.Sentry;
import io.sentry.SentryLevel;

public class SentryUtil {


    public static void init() throws Exception {
        if (!Constants.RUNNING_MODE.equals(Constants.Mode.DEVELOPMENT)) {
            Sentry.init(options -> {
                options.setEnableExternalConfiguration(true); // To read sentry.properties
                options.setDiagnosticLevel(SentryLevel.ERROR);
                options.setDebug(false);
                options.setRelease("azkar.app@" + Constants.VERSION + "+1");
                AppPropertiesUtil.getProps().forEach(options::setTag);

                options.setBeforeSend((event, hint) -> {
                    event.setExtra("upTime", AppPropertiesUtil.getUptime());
                    Preferences.getInstance().getAll().forEach(event::setTag);
                    return event;
                });
            });
        }
    }
}
