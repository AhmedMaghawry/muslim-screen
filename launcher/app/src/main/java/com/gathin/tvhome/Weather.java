package com.gathin.tvhome;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Current conditions and a same-day hourly strip, from Open-Meteo (no key, no account). */
final class Weather {
    private static final String TAG = "TvHome.Weather";

    static final class Hour {
        final String label;   // "Now", "11 AM", ...
        final int tempC;
        final int code;
        final int precipitationPercent;

        Hour(String label, int tempC, int code, int precipitationPercent) {
            this.label = label;
            this.tempC = tempC;
            this.code = code;
            this.precipitationPercent = precipitationPercent;
        }
    }

    final int currentTempC;
    final int feelsLikeC;
    final int code;
    final boolean isDay;
    final double windKmh;
    final int highC;
    final int lowC;
    final List<Hour> hours;

    private Weather(int currentTempC, int feelsLikeC, int code, boolean isDay, double windKmh,
                     int highC, int lowC, List<Hour> hours) {
        this.currentTempC = currentTempC;
        this.feelsLikeC = feelsLikeC;
        this.code = code;
        this.isDay = isDay;
        this.windKmh = windKmh;
        this.highC = highC;
        this.lowC = lowC;
        this.hours = hours;
    }

    /** Blocks on the network; call off the main thread. Returns null on any failure. */
    static Weather fetch(double lat, double lon) {
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,is_day"
                    + "&hourly=temperature_2m,weather_code,precipitation_probability"
                    + "&daily=temperature_2m_max,temperature_2m_min"
                    + "&timezone=auto&forecast_days=2";
            JSONObject root = new JSONObject(Net.get(url, null));
            JSONObject current = root.getJSONObject("current");
            int code = current.getInt("weather_code");
            boolean isDay = current.getInt("is_day") == 1;
            int temp = Math.round((float) current.getDouble("temperature_2m"));
            int feels = Math.round((float) current.getDouble("apparent_temperature"));
            double wind = current.getDouble("wind_speed_10m");

            JSONObject daily = root.getJSONObject("daily");
            int high = Math.round((float) daily.getJSONArray("temperature_2m_max").getDouble(0));
            int low = Math.round((float) daily.getJSONArray("temperature_2m_min").getDouble(0));

            JSONObject hourly = root.getJSONObject("hourly");
            JSONArray timeArr = hourly.getJSONArray("time");
            JSONArray tempArr = hourly.getJSONArray("temperature_2m");
            JSONArray codeArr = hourly.getJSONArray("weather_code");
            JSONArray popArr = hourly.getJSONArray("precipitation_probability");

            String nowIso = current.getString("time");
            int startIndex = 0;
            for (int i = 0; i < timeArr.length(); i++) {
                if (timeArr.getString(i).compareTo(nowIso) >= 0) { startIndex = i; break; }
            }

            List<Hour> hours = new ArrayList<>();
            for (int step = 0; step < 8; step++) {
                int i = startIndex + step * 3;
                if (i >= timeArr.length()) break;
                String label = step == 0 ? "Now" : hourLabel(timeArr.getString(i));
                hours.add(new Hour(label, Math.round((float) tempArr.getDouble(i)), codeArr.getInt(i), popArr.optInt(i, 0)));
            }

            return new Weather(temp, feels, code, isDay, wind, high, low, hours);
        } catch (Exception e) {
            Log.w(TAG, "weather failed: " + e.getMessage());
            return null;
        }
    }

    /** "2026-10-21T11:00" -> "11 AM". */
    private static String hourLabel(String iso) {
        int hour = Integer.parseInt(iso.substring(11, 13));
        int h12 = hour % 12 == 0 ? 12 : hour % 12;
        return h12 + (hour < 12 ? " AM" : " PM");
    }
}
