package com.bayoumi.util;

import com.bayoumi.models.PrayerTimes;
import com.bayoumi.util.time.HijriDate;
import kong.unirest.GetRequest;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONObject;

public class WebService {

    // http://api.aladhan.com/v1/hijriCalendarByCity?city=Ad%20Daqahliyah&country=Egypt&method=5&month=04&year=1442
    // http://api.aladhan.com/v1/timings/19-09-2020?latitude=31.2000924&longitude=29.9187387&method=5
    private static final String PRAYER_TIMES_END_POINT = "http://api.aladhan.com/v1/hijriCalendarByCity";

    public static PrayerTimes getPrayerTimesToday() {
        PrayerTimes.PrayerTimeSettings prayerTimeSettings = new PrayerTimes.PrayerTimeSettings();
        try {
            JSONObject jsonRoot = getJsonResponse("http://api.aladhan.com/v1/timingsByCity"
                    , new Query("country", prayerTimeSettings.getCountry())
                    , new Query("city", prayerTimeSettings.getCity())
                    , new Query("method", String.valueOf(prayerTimeSettings.getMethod().getId()))
                    , new Query("school", String.valueOf(prayerTimeSettings.getAsrJuristic())));

            if (jsonRoot.has("code")) {
                if (jsonRoot.getString("code").equals("200")) {
                    if (jsonRoot.has("data")) { // Is Data founded?
                        JSONObject timingsResult = jsonRoot.getJSONObject("data").getJSONObject("timings");
                        return PrayerTimes.builder()
                                .fajr(timingsResult.getString("Fajr"))
                                .sunrise(timingsResult.getString("Sunrise"))
                                .dhuhr(timingsResult.getString("Dhuhr"))
                                .asr(timingsResult.getString("Asr"))
                                .maghrib(timingsResult.getString("Maghrib"))
                                .isha(timingsResult.getString("Isha"))
                                .build();
                    }
                }
            }
        } catch (Exception e) {
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

    private static JSONObject getJsonResponse(final String END_POINT, Query... query) {
        GetRequest getRequest = Unirest.get(END_POINT);
        for (Query q : query) {
            getRequest = getRequest.queryString(q.getKey(), q.getValue());
        }
        System.out.println("URL: " + getRequest.getUrl());
        return new JSONObject(getRequest.asJson().getBody().toString());
    }

    public static PrayerTimes getPrayerTimesMonth(String city, String country, String method, HijriDate hijriDate) {
        String language = "ar";
        try {

            JSONObject jsonRoot = getJsonResponse(PRAYER_TIMES_END_POINT
                    , new Query("country", country), new Query("method", method), new Query("city", city)
                    , new Query("month", hijriDate.getMonth()), new Query("year", hijriDate.getYear()));

            if (jsonRoot.has("code")) {
                if (jsonRoot.getString("code").equals("200")) {
                    if (jsonRoot.has("data")) { // Is Data founded?
                        JSONObject timingsResult = jsonRoot.getJSONArray("data")
                                .getJSONObject(Integer.parseInt(hijriDate.getDay()) - 1)
                                .getJSONObject("timings");

                        return PrayerTimes.builder()
                                .hijriDate(hijriDate)
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
                .hijriDate(hijriDate)
                .fajr("--:--")
                .sunrise("--:--")
                .dhuhr("--:--")
                .asr("--:--")
                .maghrib("--:--")
                .isha("--:--")
                .build();
    }

    public static PrayerTimes getPrayerTimes(String city, String country, String method, HijriDate hijriDate) {
        String language = "ar";
        try {

            JSONObject jsonRoot = getJsonResponse(PRAYER_TIMES_END_POINT
                    , new Query("country", country), new Query("method", method), new Query("city", city)
                    , new Query("month", hijriDate.getMonth()), new Query("year", hijriDate.getYear()));

            if (jsonRoot.has("code")) {
                if (jsonRoot.getString("code").equals("200")) {
                    if (jsonRoot.has("data")) { // Is Data founded?
                        JSONObject timingsResult = jsonRoot.getJSONArray("data")
                                .getJSONObject(Integer.parseInt(hijriDate.getDay()) - 1)
                                .getJSONObject("timings");

                        return PrayerTimes.builder()
                                .hijriDate(hijriDate)
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
                .hijriDate(hijriDate)
                .fajr("--:--")
                .sunrise("--:--")
                .dhuhr("--:--")
                .asr("--:--")
                .maghrib("--:--")
                .isha("--:--")
                .build();
    }

//    public static void main(String[] args) {
//        String language = "ar";
//        PrayerTimes prayerTimes = getPrayerTimes("Alexandria", "Egypt", "5", HijriDate.getHijriDate(language));
//        System.out.println(prayerTimes);
//        System.out.println(getJsonResponse("http://api.aladhan.com/v1/timingsByCity", new Query("country", "Egypt"), new Query("method", "5"), new Query("city", "Alexandria")));
//    }
}