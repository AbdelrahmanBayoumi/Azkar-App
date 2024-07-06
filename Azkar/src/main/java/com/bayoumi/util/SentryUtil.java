package com.bayoumi.util;

import com.bayoumi.Launcher;
import io.sentry.Sentry;
import io.sentry.SentryLevel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SentryUtil {

    public static void main(String[] args) {
        Launcher.startTime = System.currentTimeMillis();

        getProps().forEach((k, v) -> System.out.println(k + " : " + v));
    }


    public static void init() throws Exception {
        if (!Constants.RUNNING_MODE.equals(Constants.Mode.DEVELOPMENT)) {
            Sentry.init(options -> {
                options.setEnableExternalConfiguration(true);
                options.setTracesSampleRate(0.2);
                options.setDiagnosticLevel(SentryLevel.ERROR);
                options.setDebug(false);
                options.setRelease("azkar.app@" + Constants.VERSION + "+1");
            });
            getProps().forEach(Sentry::setTag);
        }
    }

    private static Map<String, String> getProps() {
        final Map<String, String> props = new HashMap<>();

        // Application Information
        props.put("app.running_mode", Constants.RUNNING_MODE.toString());
        props.put("app.timestamp", String.valueOf(System.currentTimeMillis()));
        props.put("app.startup_local_datetime", Launcher.startTime == null ? "N/A" : LocalDateTime.ofInstant(Instant.ofEpochMilli(Launcher.startTime), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        props.put("app.thread_count", String.valueOf(Thread.activeCount()));

        // User Information
        props.put("user.name", System.getProperty("user.name"));
        props.put("user.dir", System.getProperty("user.dir"));
        props.put("user.home", System.getProperty("user.home"));

        // OS Information
        props.put("os.name", System.getProperty("os.name"));
        props.put("os.version", System.getProperty("os.version"));
        props.put("os.architecture", System.getProperty("os.arch"));

        // Java Information
        props.put("java.version", System.getProperty("java.version"));
        props.put("java.tmpdir", System.getProperty("java.io.tmpdir"));

        // Timezone Information
        props.put("timezone.id", TimeZone.getDefault().getID());
        props.put("timezone.name", TimeZone.getDefault().getDisplayName());
        props.put("timezone.offset_hours", String.valueOf(TimeZone.getDefault().getRawOffset() / 3600000));
        props.put("timezone.dst_savings", TimeZone.getDefault().getDSTSavings() == 0 ? "No" : "Yes");

        // Locale Information
        props.put("locale.default", Locale.getDefault().toString());

        // Current Date and Time
        props.put("current.local_datetime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        props.put("current.utc_datetime", ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT));

        // Hardware Information
        props.put("hardware.available_processors", String.valueOf(Runtime.getRuntime().availableProcessors()));

        return props;
    }

    public static String getUptime() {
        final long uptime = System.currentTimeMillis() - Launcher.startTime;
        final long days = uptime / 86400000;
        final long hours = (uptime % 86400000) / 3600000;
        final long minutes = (uptime % 3600000) / 60000;
        final long seconds = (uptime % 60000) / 1000;
        final long milliseconds = uptime % 1000;
        return String.format("%d days, %d hours, %d minutes, %d seconds, %d milliseconds", days, hours, minutes, seconds, milliseconds);
    }

}
