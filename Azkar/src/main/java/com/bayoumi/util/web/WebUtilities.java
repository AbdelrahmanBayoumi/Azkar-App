package com.bayoumi.util.web;

import com.bayoumi.models.Query;
import kong.unirest.GetRequest;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

public class WebUtilities {

    public static JSONObject getJsonResponse(final String END_POINT, Query... query) {
        GetRequest getRequest = Unirest.get(END_POINT);
        for (Query q : query) {
            getRequest = getRequest.queryString(q.getKey(), q.getValue());
        }
        System.out.println("URL: " + getRequest.getUrl());
        return new JSONObject(getRequest.asJson().getBody().toString());
    }

}