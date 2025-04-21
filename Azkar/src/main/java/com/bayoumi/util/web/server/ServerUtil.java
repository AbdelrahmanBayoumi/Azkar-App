package com.bayoumi.util.web.server;

import com.bayoumi.storage.DatabaseManager;
import kong.unirest.json.JSONObject;

import java.util.Properties;

public class ServerUtil {
    public static JSONObject preparePayload(JSONObject preferencesJSON, JSONObject statistics, Properties config) throws Exception {
        System.out.println("[ServerService] preferencesJSON: " + preferencesJSON);
        System.out.println("[ServerService] statistics: " + statistics);
        final JSONObject finalJSON = new JSONObject();
        finalJSON.put("id", DatabaseManager.getInstance().getID());
        finalJSON.put("preferences", preferencesJSON);
        finalJSON.put("statistics", statistics);
        return finalJSON;
    }
}
