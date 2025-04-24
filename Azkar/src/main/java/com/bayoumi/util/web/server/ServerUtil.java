package com.bayoumi.util.web.server;

import com.bayoumi.services.statistics.WeeklyStats;
import com.bayoumi.storage.DatabaseManager;
import kong.unirest.json.JSONObject;

import java.util.Properties;

public class ServerUtil {
    public static JSONObject preparePayload(WeeklyStats weeklyStats, Properties config) throws Exception {
        final JSONObject finalJSON = new JSONObject();
        finalJSON.put("id", DatabaseManager.getInstance().getID());
        finalJSON.put("preferences", weeklyStats.preferences);
        finalJSON.put("statistics", weeklyStats.statistics);
        finalJSON.put("week_start", weeklyStats.weekStart.toString());
        return finalJSON;
    }
}
