package com.bayoumi.util.web;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.AppPropertiesUtil;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.file.FileUtils;
import io.sentry.Sentry;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

import java.util.Properties;

public class AzkarServer {

    private static final String REMOTE_CONFIG_URL = "https://azkar-site.web.app/desktop/config.json";

    private static String getBaseUrl(Properties fallbackConfig) {
        try {
            final HttpResponse<JsonNode> response = Unirest.get(REMOTE_CONFIG_URL)
                    .header("Accept", "application/json")
                    .asJson();

            if (response.isSuccess()) {
                final JSONObject remoteJson = response.getBody().getObject();
                final String server = remoteJson.getString("server");
                if (server != null && !server.isEmpty()) {
                    return server;
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to load remote config, falling back to local.", e, AzkarServer.class.getName() + ".getBaseUrl()");
        }

        // fallback to local property
        return fallbackConfig.getProperty("collectorServer.baseUrl");
    }

    public static void init() {
        new Thread(() -> {
            try {
                Properties config = FileUtils.getConfig();
                final JSONObject bodyJSON = new JSONObject();
                bodyJSON.put("id", DatabaseManager.getInstance().getID());
                bodyJSON.put("API_KEY", config.getProperty("collectorServer.apiKey"));
                bodyJSON.put("preferences", getPreferencesJSON());

                String baseUrl = getBaseUrl(config);
                sendRequest(bodyJSON, baseUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static JSONObject getPreferencesJSON() {
        final JSONObject json = new JSONObject();
        json.put("version", Constants.VERSION);
        AppPropertiesUtil.getProps().forEach(json::put);
        final boolean sendUsageData = Settings.getInstance().getSendUsageData();
        if (sendUsageData) {
            Preferences.getInstance().getAll().forEach(json::put);
        } else {
            json.put("preferences.send_usage_data", false);
        }
        return json;
    }

    private static void sendRequest(JSONObject bodyJSON, String baseUrl) {
        final HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/client")
                .header("Content-Type", "application/json")
                .body(bodyJSON)
                .asJson();

        if (response != null && response.isSuccess()) {
            System.out.println("OK");
        } else {
            System.out.println("S-ERROR");
            Sentry.captureMessage("AzkarServer API Error");
        }
    }
}
