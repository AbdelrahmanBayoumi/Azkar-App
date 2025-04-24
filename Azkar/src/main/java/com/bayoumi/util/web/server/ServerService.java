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
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * Attempts to fetch remote config.json with retries; falls back on local if all attempts fail.
     */
    private static String getBaseUrl(Properties fallbackConfig) {
        final String REMOTE_CONFIG_URL = "https://azkar-site.web.app/desktop/config.json";
        final AtomicReference<HttpResponse<JsonNode>> respRef = new AtomicReference<>();

        final boolean fetched = RetryTask.builder(() -> {
                    HttpResponse<JsonNode> resp = Unirest.get(REMOTE_CONFIG_URL)
                            .header("Accept", "application/json")
                            .asJson();
                    respRef.set(resp);
                    return resp != null && resp.isSuccess();
                })
                .enableJitter(true)
                .threadName("ConfigFetch-Thread")
                .execute(); // synchronous retry

        final HttpResponse<JsonNode> remoteResp = respRef.get();
        if (fetched && remoteResp != null) {
            try {
                final String server = remoteResp.getBody().getObject().getString("server");
                if (server != null && !server.isEmpty()) {
                    return server;
                }
            } catch (Exception e) {
                Logger.error("Malformed remote config, using local.", e, ServerService.class.getName() + ".getBaseUrl()");
            }
        } else {
            Logger.debug("[ServerService] Remote config fetch failed, falling back to local.");
        }

        return fallbackConfig.getProperty("collectorServer.baseUrl");
    }

    private static void sendRequestWithRetryAsync(String baseUrl, JSONObject bodyJSON) {
        RetryTask.builder(() -> sendRequest(baseUrl, bodyJSON)).enableJitter(true).threadName("UsagePost-Thread").executeAsync();
    }

    private static boolean sendRequest(String baseUrl, JSONObject bodyJSON) {
        final HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/client/usage")
                .header("Content-Type", "application/json")
                .body(bodyJSON)
                .asJson();

        if (response != null && response.isSuccess()) {
            Logger.debug("[ServerService] Success: " + response.getBody());
            return true;  // exit after success
        } else {
            final String err = response != null && response.getBody() != null ? response.getBody().toString() : "null";
            Logger.debug("[ServerService] API Error: " + err);
            Sentry.captureMessage("API Error: " + err, SentryLevel.WARNING);
            return false;
        }
    }
}
