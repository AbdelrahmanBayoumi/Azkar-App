package com.bayoumi.util.web.server;

import com.bayoumi.services.statistics.WeeklyStats;
import com.bayoumi.storage.DatabaseManager;
import com.bayoumi.util.Logger;
import com.bayoumi.util.web.RetryTask;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ClientUsageService {
    private final String baseUrl;

    /**
     * Fetches remote config (with retries) or falls back to local.
     */
    public ClientUsageService(Properties config) {
        this.baseUrl = resolveBaseUrl(config);
    }

    /**
     * Create (POST) a new client-usage record.
     */
    public boolean createUsage(WeeklyStats stats,
                               Consumer<HttpResponse<JsonNode>> successCallback,
                               Consumer<HttpResponse<JsonNode>> failCallback) {
        String id = UUID.randomUUID().toString();
        JSONObject payload;
        try {
            payload = ServerUtil.preparePayload(id, stats, null);
        } catch (Exception e) {
            Logger.error("Payload prep failed", e, getClass().getName() + ".createUsage()");
            return false;
        }

        final HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/client/usage")
                .header("Content-Type", "application/json")
                .body(payload)
                .asJson();

        if (response != null && response.isSuccess()) {
            DatabaseManager.getInstance().setID(id);
            successCallback.accept(response);
            return true;
        } else {
            failCallback.accept(response);
            return false;
        }
    }

    /**
     * Update (PUT) an existing client-usage record.
     */
    public boolean updateUsage(JSONObject payload,
                               Consumer<HttpResponse<JsonNode>> successCallback,
                               Consumer<HttpResponse<JsonNode>> failCallback) {
        final HttpResponse<JsonNode> response = Unirest.put(baseUrl + "/client/usage")
                .header("Content-Type", "application/json")
                .body(payload)
                .asJson();

        if (response != null && response.isSuccess()) {
            successCallback.accept(response);
            return true;
        } else {
            failCallback.accept(response);
            return false;
        }
    }

    private String resolveBaseUrl(Properties fallbackConfig) {
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
                Logger.error("Malformed remote config, using local.", e, ClientUsageService.class.getName() + ".getBaseUrl()");
            }
        } else {
            Logger.debug("[ClientUsageService] Remote config fetch failed, falling back to local.");
        }

        return fallbackConfig.getProperty("collectorServer.baseUrl");
    }
}
