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

import java.util.Properties;
import java.util.function.Consumer;

public class ServerService {

    public static void init() {
        new Thread(() -> {
            try {
                Logger.debug("[ServerService] Starting...");
                final Properties config = FileUtils.getConfig();
                final ClientUsageService usageService = new ClientUsageService(config);
                final boolean sendUsageData = Settings.getInstance().getSendUsageData();


                final WeeklyStats oldWeekStats = WeeklyStatsManager.oldWeekIfRolling(sendUsageData);
                if (oldWeekStats != null) {
                    // send last week’s data before it vanishes
                    sendRequest(usageService, oldWeekStats, config);
                }

                // now send *this* week’s accumulating stats as usual
                final WeeklyStats currentWeekStats = WeeklyStatsManager.getCurrentWeekStats(sendUsageData);
                sendRequest(usageService, currentWeekStats, config);
            } catch (Exception e) {
                Logger.error("Exception during server init", e, ServerService.class.getName() + ".init()");
            }
        }, "ServerService-Init-Thread").start();
    }

    private static void sendRequest(ClientUsageService usage, WeeklyStats weeklyStats, Properties config) throws Exception {
        final String id = DatabaseManager.getInstance().getID();

        final Consumer<HttpResponse<JsonNode>> successCallback = (res) -> Logger.debug("[ServerService] Success: " + res.getBody());
        final Consumer<HttpResponse<JsonNode>> failCallback = (res) -> {
            final String err = res != null && res.getBody() != null ? res.getBody().toString() : "null";
            Logger.debug("[ServerService] API Error: " + err);
            Sentry.captureMessage("API Error: " + err, SentryLevel.WARNING);
        };

        final boolean isFirstTimeOpened = (id == null || id.isEmpty());
        if (isFirstTimeOpened) {
            RetryTask.builder(() -> usage.createUsage(weeklyStats, config, successCallback, failCallback)).enableJitter(true).execute();
        } else {
            RetryTask.builder(() -> usage.updateUsage(id, weeklyStats, config, successCallback, failCallback)).enableJitter(true).execute();
        }
    }
}
