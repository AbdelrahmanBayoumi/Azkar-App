package com.bayoumi.util.web;

import com.bayoumi.models.City;
import kong.unirest.json.JSONObject;

import java.time.Instant;
import java.util.TimeZone;

public class LocationService {

    public static City getCity(final String IP) throws Exception {
        JSONObject jsonRoot = WebUtilities.getJsonResponse("http://ip-api.com/json/" + IP);
        if (jsonRoot.has("status") && jsonRoot.getString("status").equals("success")) {
            TimeZone tz = TimeZone.getTimeZone(jsonRoot.getString("timezone"));
            return new City(jsonRoot.getString("countryCode"),
                    jsonRoot.getString("city"),
                    null,
                    jsonRoot.getDouble("lat"),
                    jsonRoot.getDouble("lon"),
                    (tz.toZoneId().getRules().getStandardOffset(Instant.now()).getTotalSeconds() / 3600.0)
            );
        } else {
            throw new Exception("Error in fetching city data,ENDPOINT: " + "http://ip-api.com/json/" + IP);
        }
    }
}
