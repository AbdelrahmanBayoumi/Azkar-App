package com.bayoumi.util.web.server;

import com.bayoumi.util.db.DatabaseManager;
import kong.unirest.json.JSONObject;

import java.util.Properties;

public class ServerUtil {
    public static JSONObject preparePayload(JSONObject preferencesJSON, Properties config) throws Exception {
        final JSONObject finalJSON = new JSONObject();
        finalJSON.put("id", DatabaseManager.getInstance().getID());
        finalJSON.put("preferences", preferencesJSON);
        return finalJSON;
    }
}
