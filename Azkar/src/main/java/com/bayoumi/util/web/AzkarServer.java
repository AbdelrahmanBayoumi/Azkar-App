package com.bayoumi.util.web;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.AppPropertiesUtil;
import com.bayoumi.util.Constants;
import com.bayoumi.util.db.DatabaseManager;
import io.sentry.Sentry;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public class AzkarServer {

    private static Properties getConfig() throws Exception {
        final Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get(Objects.requireNonNull(LocationService.class.getResource("/config.properties")).toURI()))) {
            properties.load(input);
            return properties;
        }
    }

    public static void init() {
        new Thread(() -> {
            try {
                Properties config = getConfig();
                final JSONObject bodyJSON = new JSONObject();
                bodyJSON.put("id", DatabaseManager.getInstance().getID());
                bodyJSON.put("API_KEY", config.getProperty("collectorServer.apiKey"));
                bodyJSON.put("preferences", getPreferencesJSON());

                // TODO: get baseURL from central config file
                sendRequest(bodyJSON, config.getProperty("collectorServer.baseUrl"));
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

    private static void sendRequest(JSONObject bodyJSON, String baseUrl) throws Exception {
        final HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/client")
                .header("Content-Type", "application/json")
                .body(bodyJSON)
                .asJson();

        // check response status
        if (response != null && response.isSuccess()) {
            System.out.println("OK");
        } else {
            System.out.println("S-ERROR");
            Sentry.captureMessage("AzkarServer API Error");
        }
    }
}
