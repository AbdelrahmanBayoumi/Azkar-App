package com.bayoumi.util.web;

import com.bayoumi.models.Query;
import com.bayoumi.util.Logger;
import kong.unirest.*;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

import java.io.File;
import java.nio.file.StandardCopyOption;

public class WebUtilities {

    public static JSONObject getJsonResponse(final String END_POINT, Query... query) throws Exception {
        try {
            GetRequest getRequest = Unirest.get(END_POINT);
            for (Query q : query) {
                getRequest = getRequest.queryString(q.getKey(), q.getValue());
            }
            Logger.debug("URL: " + getRequest.getUrl());
            return new JSONObject(getRequest.asJson().getBody().toString());
        } catch (UnirestException ue) {
            throw new Exception("Network error or host unreachable: " + END_POINT, ue);
        } catch (Exception e) {
            throw new Exception("Invalid JSON or unexpected server response from: " + END_POINT, e);
        }
    }

    public static File downloadFile(String url, String downloadedFilePath, ProgressMonitor progressMonitor) throws UnirestException {
        Unirest.config().cookieSpec("STANDARD");
        GetRequest getRequest = Unirest.get(url);
        if (progressMonitor != null) {
            getRequest = getRequest.downloadMonitor(progressMonitor);
        }
        return getRequest.asFile(downloadedFilePath, StandardCopyOption.REPLACE_EXISTING).getBody();
    }


    public static String getLatestVersion(String repoURL) throws UnirestException {
        HttpResponse<String> response = Unirest.get(repoURL)
                .header("Accept", "application/vnd.github.v3+json")
                .asString();

        try {
            final JSONArray releases = new JSONArray(response.getBody());
            if (!releases.isEmpty()) {
                Logger.debug("[WebUtilities] getLatestVersion: " + releases.getJSONObject(0).getString("tag_name"));
                return releases.getJSONObject(0).getString("tag_name");
            }
        } catch (Exception e) {
            Logger.error("Failed to parse releases res.body:" + response.getBody(), e, WebUtilities.class.getName() + ".getLatestVersion()");
        }
        throw new UnirestException("No releases found for the repository.");
    }
}
