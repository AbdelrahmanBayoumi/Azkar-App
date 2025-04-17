package com.bayoumi.util.web.server;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.AppPropertiesUtil;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.file.FileUtils;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

import java.util.Properties;

public class ServerService {
    public static void init() {
        new Thread(() -> {
            try {
                Logger.debug("[ServerService] Starting...");
                final Properties config = FileUtils.getConfig();
                Logger.debug("[ServerService] Config: " + config);
                final String baseUrl = getBaseUrl(config);
                Logger.debug("[ServerService] Base URL: " + baseUrl);
                sendRequest(baseUrl, ServerUtil.preparePayload(getPreferencesJSON(), config));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static String getBaseUrl(Properties fallbackConfig) {
        Logger.debug("[ServerUtil] Loading remote config...");
        try {
            final String REMOTE_CONFIG_URL = "https://azkar-site.web.app/desktop/config.json";
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
            Logger.error("Failed to load remote config, falling back to local.", e, ServerUtil.class.getName() + ".getBaseUrl()");
        }

        // fallback to local property
        return fallbackConfig.getProperty("collectorServer.baseUrl");
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

    private static void sendRequest(String baseUrl, JSONObject bodyJSON) {
        Logger.debug("[ServerService] Sending request...");
        final HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/client/usage")
                .header("Content-Type", "application/json")
                .body(bodyJSON)
                .asJson();

        if (response != null && response.isSuccess()) {
            Logger.debug("[ServerService] Response: " + response.getBody().toString());
        } else {
            String errMsg = "[ServerService] API Error: " + (response != null && response.getBody() != null ? response.getBody().toString() : "");
            Logger.debug("[ServerService] " + errMsg);
            Sentry.captureMessage(errMsg, SentryLevel.ERROR);
        }
    }
}