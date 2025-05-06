package com.bayoumi.util;

import com.bayoumi.storage.DatabaseManager;
import com.bayoumi.storage.preferences.Preferences;
import com.bayoumi.storage.statistics.StatisticsStore;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.protocol.User;

public class SentryUtil {


    public static void init() throws Exception {
        if (!Constants.RUNNING_MODE.equals(Constants.Mode.PRODUCTION)) return;

        Sentry.init(options -> {
            options.setEnableExternalConfiguration(true); // To read sentry.properties
            options.setDiagnosticLevel(SentryLevel.ERROR);
            options.setDebug(false);
            options.setRelease("azkar.app@" + Constants.VERSION + "+1");
            AppPropertiesUtil.getProps().forEach(options::setTag);

            options.setBeforeSend((event, hint) -> {
                event.setExtra("upTime", AppPropertiesUtil.getUptime());
                Preferences.getInstance().getAllWithPrefix().forEach(event::setTag);
                StatisticsStore.getInstance().getAllWithPrefix().forEach(event::setTag);
                if (event.getUser() == null || event.getUser().getId() == null || event.getUser().getId().isEmpty()) {
                    event.setUser(getSentryUser());
                }
                return event;
            });
        });

        setSentryUser();
    }

    private static User getSentryUser() {
        final User user = new User();
        user.setId(DatabaseManager.getInstance().getID());
        return user;
    }

    private static void setSentryUser() {
        Sentry.configureScope(scope -> scope.setUser(getSentryUser()));
    }
}
