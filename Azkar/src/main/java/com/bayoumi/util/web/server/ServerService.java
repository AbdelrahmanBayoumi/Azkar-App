package com.bayoumi.util.web.server;

import com.bayoumi.models.settings.Settings;
import com.bayoumi.services.statistics.WeeklyStats;
import com.bayoumi.services.statistics.WeeklyStatsManager;
import com.bayoumi.util.Logger;
import com.bayoumi.util.file.FileUtils;
import com.bayoumi.util.web.RetryTask;
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
                final String baseUrl = getBaseUrl(config);
                final boolean sendUsageData = Settings.getInstance().getSendUsageData();

                final WeeklyStats oldWeekStats = WeeklyStatsManager.oldWeekIfRolling(sendUsageData);
                if (oldWeekStats != null) {
                    // send last week’s data before it vanishes
                    sendRequestWithRetryAsync(baseUrl, ServerUtil.preparePayload(oldWeekStats, config));
                }

                // now send *this* week’s accumulating stats as usual
                final WeeklyStats currentWeekStats = WeeklyStatsManager.getCurrentWeekStats(sendUsageData);
                sendRequestWithRetryAsync(baseUrl, ServerUtil.preparePayload(currentWeekStats, config));
            } catch (Exception e) {
                Logger.error("Exception during server init", e, ServerService.class.getName() + ".init()");
            }
        }, "ServerService-Init-Thread").start();
    }

    private static String getBaseUrl(Properties fallbackConfig) {
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


    private static void sendRequestWithRetryAsync(String baseUrl, JSONObject bodyJSON) {
        RetryTask.builder(() -> sendRequest(baseUrl, bodyJSON)).enableJitter(true).executeAsync();
    }

    private static boolean sendRequest(String baseUrl, JSONObject bodyJSON) {
        final HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/client/usage")
                .header("Content-Type", "application/json")
                .body(bodyJSON)
                .asJson();

        if (response != null && response.isSuccess()) {
            Logger.debug("[ServerService] Success: " + response.getBody().toString());
            return true; // exit after success
        } else {
            String errMsg = "[ServerService] API Error: " +
                    (response != null && response.getBody() != null ? response.getBody().toString() : "null");
            Logger.debug("[ServerService] " + errMsg);
            Sentry.captureMessage(errMsg, SentryLevel.WARNING);
        }
        return false;
    }
}