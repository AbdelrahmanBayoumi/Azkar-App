package com.bayoumi.util.prayertimes;

import com.bayoumi.models.PrayerTimes;
import com.bayoumi.util.Query;
import com.bayoumi.util.Utilities;
import com.bayoumi.util.time.HijriDate;
import kong.unirest.GetRequest;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

import java.util.ArrayList;

public class WebService {

    // http://api.aladhan.com/v1/hijriCalendarByCity?city=Ad%20Daqahliyah&country=Egypt&method=5&month=04&year=1442
    // http://api.aladhan.com/v1/timings/19-09-2020?latitude=31.2000924&longitude=29.9187387&method=5
    private static final String PRAYER_TIMES_END_POINT = "http://api.aladhan.com/v1/hijriCalendarByCity";

    /**
     * @return PrayerTimes for Today in (Gregorian Calendar)
     */
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


    public static ArrayList<PrayerTimes> getPrayerTimesMonth(HijriDate hijriDate) {
        ArrayList<PrayerTimes> prayerTimes = new ArrayList<>();
        try {
            PrayerTimes.PrayerTimeSettings prayerTimeSettings = new PrayerTimes.PrayerTimeSettings();
            System.out.println("prayerTimeSettings: "+ prayerTimeSettings);
            if(!prayerTimeSettings.hasLocation()){
                return prayerTimes;
            }
            JSONObject jsonRoot = getJsonResponse(PRAYER_TIMES_END_POINT
                    , new Query("country", prayerTimeSettings.getCountry())
                    , new Query("city", prayerTimeSettings.getCity())
                    , new Query("method", String.valueOf(prayerTimeSettings.getMethod().getId()))
                    , new Query("school", String.valueOf(prayerTimeSettings.getAsrJuristic()))
                    , new Query("month", String.valueOf(hijriDate.getMonth() + 1))
                    , new Query("year", String.valueOf(hijriDate.getYear())));

            if (jsonRoot.has("code")) {
                if (jsonRoot.getString("code").equals("200")) {
                    if (jsonRoot.has("data")) { // Is Data founded?
                        JSONArray monthData = jsonRoot.getJSONArray("data");
                        for (int i = 0; i < monthData.length(); i++) {
                            JSONObject timings = ((JSONObject) monthData.get(i)).getJSONObject("timings");
                            prayerTimes.add(PrayerTimes.builder()
                                    .hijriDate(new HijriDate(hijriDate.getYear(), hijriDate.getMonth(), i + 1))
                                    .fajr(Utilities.formatTimeOnly(timings.getString("Fajr")))
                                    .sunrise(Utilities.formatTimeOnly(timings.getString("Sunrise")))
                                    .dhuhr(Utilities.formatTimeOnly(timings.getString("Dhuhr")))
                                    .asr(Utilities.formatTimeOnly(timings.getString("Asr")))
                                    .maghrib(Utilities.formatTimeOnly(timings.getString("Maghrib")))
                                    .isha(Utilities.formatTimeOnly(timings.getString("Isha")))
                                    .build());
                        }
                    }
                }
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return prayerTimes;
    }

    private static JSONObject getJsonResponse(final String END_POINT, Query... query) {
        GetRequest getRequest = Unirest.get(END_POINT);
        for (Query q : query) {
            getRequest = getRequest.queryString(q.getKey(), q.getValue());
        }
        System.out.println("URL: " + getRequest.getUrl());
        return new JSONObject(getRequest.asJson().getBody().toString());
    }

}