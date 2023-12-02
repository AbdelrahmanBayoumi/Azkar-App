package com.bayoumi.util.web;

import com.bayoumi.models.Query;
import com.bayoumi.util.Logger;
import kong.unirest.GetRequest;
import kong.unirest.ProgressMonitor;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

import java.io.File;
import java.nio.file.StandardCopyOption;

public class WebUtilities {

    public static JSONObject getJsonResponse(final String END_POINT, Query... query) {
        GetRequest getRequest = Unirest.get(END_POINT);
        for (Query q : query) {
            getRequest = getRequest.queryString(q.getKey(), q.getValue());
        }
        Logger.debug("URL: " + getRequest.getUrl());
        return new JSONObject(getRequest.asJson().getBody().toString());
    }

    public static File downloadFile(String url, String downloadedFilePath, ProgressMonitor progressMonitor) {
        Unirest.config().cookieSpec("STANDARD");
        return Unirest.get(url)
                .downloadMonitor(progressMonitor)
                .asFile(downloadedFilePath, StandardCopyOption.REPLACE_EXISTING).getBody();
    }

}