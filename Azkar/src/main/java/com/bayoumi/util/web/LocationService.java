package com.bayoumi.util.web;

import com.bayoumi.models.location.City;
import com.bayoumi.util.Logger;
import kong.unirest.json.JSONObject;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Properties;
import java.util.TimeZone;

public class LocationService {

    private static String getIp2locationAPIKEY() throws Exception {
        final Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get(Objects.requireNonNull(LocationService.class.getResource("/config.properties")).toURI()))) {
            properties.load(input);
            return properties.getProperty("ip2location.apiKey");
        }
    }

    public static City getCityWithIP_API(final String IP) throws Exception {
        JSONObject jsonRoot = WebUtilities.getJsonResponse("http://ip-api.com/json/" + IP);
        if (jsonRoot.has("status") && jsonRoot.getString("status").equals("success")) {
            TimeZone tz = TimeZone.getTimeZone(jsonRoot.getString("timezone"));
            return new City(jsonRoot.getString("countryCode"),
                    jsonRoot.getString("country"),
                    jsonRoot.getString("city"),
                    null,
                    jsonRoot.getDouble("lat"),
                    jsonRoot.getDouble("lon"),
                    (tz.toZoneId().getRules().getStandardOffset(Instant.now()).getTotalSeconds() / 3600.0)
            );
        } else {
            throw new Exception("Error in fetching city data, ENDPOINT: " + "http://ip-api.com/json/" + IP);
        }
    }

    public static City getCityWithIp2location(final String IP) throws Exception {
        JSONObject jsonRoot = WebUtilities.getJsonResponse("https://api.ip2location.io/?key=" + getIp2locationAPIKEY() + "&ip=" + IP);
        Logger.debug("jsonRoot: " + jsonRoot);
        if (!jsonRoot.isEmpty()) {
            TimeZone tz = TimeZone.getTimeZone(ZoneOffset.of(jsonRoot.getString("time_zone")));
            return new City(jsonRoot.getString("country_code"),
                    jsonRoot.getString("country_name"),
                    jsonRoot.getString("region_name"),
                    jsonRoot.getString("region_name"),
                    jsonRoot.getDouble("latitude"),
                    jsonRoot.getDouble("longitude"),
                    (tz.toZoneId().getRules().getStandardOffset(Instant.now()).getTotalSeconds() / 3600.0)
            );
        } else {
            throw new Exception("Error in fetching city data, " + "https://api.ip2location.io/?key=" + getIp2locationAPIKEY() + "&ip=" + IP);
        }
    }

    public static City getCity(final String IP) throws Exception {
        try {
            return getCityWithIP_API(IP);
        } catch (Exception e) {
            Logger.error("Error in fetching city data with IP-API, trying with IP2Location", e, LocationService.class.getName() + ".getCity()");
            try {
                return getCityWithIp2location(IP);
            } catch (Exception ex) {
                Logger.error("Error in fetching city data with IP2Location", ex, LocationService.class.getName() + ".getCity()");
                throw new Exception("Error in fetching city data with IP-API and IP2Location");
            }
        }
    }
}
