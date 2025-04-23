package com.bayoumi.util.web.server;

import com.bayoumi.models.settings.Settings;
import com.bayoumi.services.statistics.WeeklyStats;
import com.bayoumi.services.statistics.WeeklyStatsManager;
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
                final String baseUrl = getBaseUrl(config);
                final boolean sendUsageData = Settings.getInstance().getSendUsageData();

                final WeeklyStats oldWeekStats = WeeklyStatsManager.oldWeekIfRolling(sendUsageData);
                if (oldWeekStats != null) {
                    // send last week’s data before it vanishes
                    sendRequest(baseUrl, ServerUtil.preparePayload(oldWeekStats, config));
                }

                // now send *this* week’s accumulating stats as usual
                final WeeklyStats currentWeekStats = WeeklyStatsManager.getCurrentWeekStats(sendUsageData);
                sendRequest(baseUrl, ServerUtil.preparePayload(currentWeekStats, config));
            } catch (Exception e) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }).start();
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

    private static void sendRequest(String baseUrl, JSONObject bodyJSON) {
        // TODO implement retry mechanism if the request fails & open request in thread
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