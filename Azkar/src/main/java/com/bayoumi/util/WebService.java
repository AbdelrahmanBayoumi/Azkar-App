package com.bayoumi.util;

import com.bayoumi.models.PrayerTimes;
import com.bayoumi.util.time.HijriDate;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

public class WebService {
    // ?city=Ad%20Daqahliyah&country=Egypt&method=5&month=04&year=1442
    // http://api.aladhan.com/v1/timings/19-09-2020?latitude=31.2000924&longitude=29.9187387&method=5
    private static final String PRAYER_TIMES_END_POINT = "http://api.aladhan.com/v1/hijriCalendarByCity";

    public static PrayerTimes getPrayerTimes(String city, String country, String method, HijriDate hijriDate) {
        String language = "ar";
        try {
            HttpResponse<JsonNode> jsonResponse = Unirest.get(PRAYER_TIMES_END_POINT)
                    .queryString("city", city)
                    .queryString("country", country)
                    .queryString("method", method)
                    .queryString("month", hijriDate.getMonth())
                    .queryString("year", hijriDate.getYear())
                    .asJson();

            JSONObject jsonRoot = new JSONObject(jsonResponse.getBody().toString());

            if (jsonRoot.has("code")) {
                if (jsonRoot.getString("code").equals("200")) {
                    if (jsonRoot.has("data")) { // Is Data founded?
                        JSONObject timingsResult = jsonRoot.getJSONArray("data")
                                .getJSONObject(Integer.parseInt(hijriDate.getDay()) - 1)
                                .getJSONObject("timings");

                        return PrayerTimes.builder()
                                .fajr(Utilities.formatTimeOnly(language, timingsResult.getString("Fajr")))
                                .sunrise(Utilities.formatTimeOnly(language, timingsResult.getString("Sunrise")))
                                .dhuhr(Utilities.formatTimeOnly(language, timingsResult.getString("Dhuhr")))
                                .asr(Utilities.formatTimeOnly(language, timingsResult.getString("Asr")))
                                .maghrib(Utilities.formatTimeOnly(language, timingsResult.getString("Maghrib")))
                                .isha(Utilities.formatTimeOnly(language, timingsResult.getString("Isha")))
                                .build();
                    }
                }
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return PrayerTimes.builder()
                .fajr("--:--")
                .sunrise("--:--")
                .dhuhr("--:--")
                .asr("--:--")
                .maghrib("--:--")
                .isha("--:--")
                .build();
    }


    public static void main(String[] args) {
        String language = "ar";
        PrayerTimes prayerTimes = getPrayerTimes("Alexandria", "Egypt", "5", HijriDate.getHijriDate(language));
        System.out.println(prayerTimes);
    }
}
