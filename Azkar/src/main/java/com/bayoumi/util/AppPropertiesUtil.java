package com.bayoumi.util;

import com.bayoumi.Launcher;
import com.bayoumi.storage.DatabaseManager;
import com.bayoumi.storage.preferences.Preferences;
import com.bayoumi.storage.statistics.StatisticsStore;
import kong.unirest.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AppPropertiesUtil {

    public static Map<String, String> getProps() {
        final Map<String, String> props = new HashMap<>();

        // User Information
        props.put("assets_path", Constants.assetsPath);

        // OS Information
        props.put("os.name", System.getProperty("os.name"));
        props.put("os.version", System.getProperty("os.version"));
        props.put("os.architecture", System.getProperty("os.arch"));

        // Java Information
        props.put("java.version", System.getProperty("java.version"));

        // Timezone Information
        props.put("timezone.id", TimeZone.getDefault().getID());
        props.put("timezone.name", TimeZone.getDefault().getDisplayName());
        props.put("timezone.offset_hours", String.valueOf(TimeZone.getDefault().getRawOffset() / 3600000));
        props.put("timezone.dst_savings", TimeZone.getDefault().getDSTSavings() == 0 ? "No" : "Yes");

        // Locale Information
        props.put("locale.default", Locale.getDefault().toString());

        return props;
    }

    public static String getAllAppPropsAsJsonString() {
        final JSONObject jsonObject = new JSONObject(getProps());
        jsonObject.put("id", DatabaseManager.getInstance().getID());
        jsonObject.put("uptime", getUptime());
        Preferences.getInstance().getAllWithPrefix().forEach(jsonObject::put);
        StatisticsStore.getInstance().getAllWithPrefix().forEach(jsonObject::put);
        return jsonObject.toString();
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
