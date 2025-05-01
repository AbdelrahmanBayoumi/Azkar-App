package com.bayoumi.util.web.server;

import com.bayoumi.models.settings.Settings;
import com.bayoumi.services.statistics.WeeklyStats;
import com.bayoumi.services.statistics.WeeklyStatsManager;
import com.bayoumi.storage.DatabaseManager;
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
import java.util.UUID;
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
                    sendRequest(baseUrl, oldWeekStats, config);
                }

                // now send *this* week’s accumulating stats as usual
                final WeeklyStats currentWeekStats = WeeklyStatsManager.getCurrentWeekStats(sendUsageData);
                sendRequest(baseUrl, currentWeekStats, config);
            } catch (Exception e) {
                Logger.error("Exception during server init", e, ServerService.class.getName() + ".init()");
            }
        }, "ServerService-Init-Thread").start();
    }

    private static void sendRequest(String baseUrl, WeeklyStats weeklyStats, Properties config) throws Exception {
        final String id = DatabaseManager.getInstance().getID();
        final boolean isFirstTimeOpened = (id == null || id.isEmpty());
        RetryTask.Builder taskBuilder;
        if (isFirstTimeOpened) {
            taskBuilder = RetryTask.builder(() -> {
                try {
                    return createRequest(baseUrl, weeklyStats, config);
                } catch (Exception ex) {
                    Logger.error(null, ex, ServerService.class.getName() + ".sendRequest()");
                    return false;
                }
            });
        } else {
            final JSONObject payload = ServerUtil.preparePayload(id, weeklyStats, config);
            taskBuilder = RetryTask.builder(() -> updateRequest(baseUrl, payload));
        }

        taskBuilder.enableJitter(true).threadName("UsagePost-Thread").execute();
    }

    private static boolean updateRequest(String baseUrl, JSONObject bodyJSON) {
        final HttpResponse<JsonNode> response = Unirest.put(baseUrl + "/client/usage")
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

    private static boolean createRequest(String baseUrl, WeeklyStats weeklyStats, Properties config) throws Exception {
        String id = UUID.randomUUID().toString();
        final HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/client/usage")
                .header("Content-Type", "application/json")
                .body(ServerUtil.preparePayload(id, weeklyStats, config))
                .asJson();

        if (response != null && response.isSuccess()) {
            Logger.debug("[ServerService] Success: " + response.getBody());
            // save generated ID to DB
            DatabaseManager.getInstance().setID(id);
            return true;  // exit after success
        } else {
            final String err = response != null && response.getBody() != null ? response.getBody().toString() : "null";
            Logger.debug("[ServerService] API Error: " + err);
            Sentry.captureMessage("API Error: " + err, SentryLevel.WARNING);
            return false;
        }
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
}
