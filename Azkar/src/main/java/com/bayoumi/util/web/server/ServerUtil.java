package com.bayoumi.util.web.server;

import com.bayoumi.services.statistics.WeeklyStats;
import kong.unirest.json.JSONObject;

import java.util.Properties;

public class ServerUtil {
    public static JSONObject preparePayload(String id, WeeklyStats weeklyStats, Properties config) throws Exception {
        final JSONObject finalJSON = new JSONObject();
        finalJSON.put("id", id);
        finalJSON.put("preferences", weeklyStats.preferences);
        finalJSON.put("statistics", weeklyStats.statistics);
        finalJSON.put("week_start", weeklyStats.weekStart.toString());
        return finalJSON;
    }
}
