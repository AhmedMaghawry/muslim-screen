package com.gathin.tvhome;

import android.util.Log;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Today's prayer times and the Islamic (Hijri) date, from Aladhan (api.aladhan.com), by coordinates. */
final class PrayerTimes {
    private static final String TAG = "TvHome.Prayer";
    private static final String[] HIJRI_MONTHS = {
            "Muharram", "Safar", "Rabi I", "Rabi II", "Jumada I", "Jumada II",
            "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    };
    private static final String[] ORDER = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};

    /** The next upcoming prayer from some point in time: which one, how soon, and at what time. */
    static final class Next {
        final String name;
        /** "HH:mm" (24h), straight from {@link #times}. */
        final String at;
        final int remainingMinutes;
        /** True once today's Isha has passed and this is tomorrow's Fajr. */
        final boolean tomorrow;

        Next(String name, String at, int remainingMinutes, boolean tomorrow) {
            this.name = name;
            this.at = at;
            this.remainingMinutes = remainingMinutes;
            this.tomorrow = tomorrow;
        }
    }

    /** Fajr, Dhuhr, Asr, Maghrib, Isha, in that order, each "HH:mm" (24h). */
    final Map<String, String> times;
    /** e.g. "10 Jumada I 1448 AH". */
    final String hijriDate;

    private PrayerTimes(Map<String, String> times, String hijriDate) {
        this.times = times;
        this.hijriDate = hijriDate;
    }

    /** Blocks on the network; call off the main thread. Returns null on any failure. */
    static PrayerTimes fetch(double lat, double lon, int method, int school) {
        try {
            long nowSeconds = System.currentTimeMillis() / 1000L;
            String url = "https://api.aladhan.com/v1/timings/" + nowSeconds
                    + "?latitude=" + lat + "&longitude=" + lon
                    + "&method=" + method + "&school=" + school;
            JSONObject root = new JSONObject(Net.get(url, null));
            if (root.optInt("code", 0) != 200) return null;
            JSONObject data = root.getJSONObject("data");
            JSONObject timings = data.getJSONObject("timings");

            Map<String, String> times = new LinkedHashMap<>();
            times.put("Fajr", clean(timings.getString("Fajr")));
            times.put("Dhuhr", clean(timings.getString("Dhuhr")));
            times.put("Asr", clean(timings.getString("Asr")));
            times.put("Maghrib", clean(timings.getString("Maghrib")));
            times.put("Isha", clean(timings.getString("Isha")));

            JSONObject hijri = data.getJSONObject("date").getJSONObject("hijri");
            int monthNum = hijri.getJSONObject("month").getInt("number");
            String month = monthNum >= 1 && monthNum <= 12
                    ? HIJRI_MONTHS[monthNum - 1] : hijri.getJSONObject("month").getString("en");
            String hijriDate = hijri.getString("day") + " " + month + " " + hijri.getString("year") + " AH";

            return new PrayerTimes(times, hijriDate);
        } catch (Exception e) {
            Log.w(TAG, "prayer times failed: " + e.getMessage());
            return null;
        }
    }

    /** Aladhan times sometimes carry a " (TZ)" suffix; keep just "HH:mm". */
    private static String clean(String raw) {
        int space = raw.indexOf(' ');
        return space > 0 ? raw.substring(0, space) : raw;
    }

    /** The next upcoming prayer, given the current minutes-since-midnight (local time). */
    Next next(int nowMinutes) {
        for (String name : ORDER) {
            String raw = times.get(name);
            if (raw == null) continue;
            int minutes = minutesOf(raw);
            if (minutes > nowMinutes) return new Next(name, raw, minutes - nowMinutes, false);
        }
        // Past Isha: tomorrow's Fajr is next.
        String raw = times.get(ORDER[0]);
        if (raw == null) return null;
        return new Next(ORDER[0], raw, minutesOf(raw) + 24 * 60 - nowMinutes, true);
    }

    static int minutesOf(String hhmm) {
        int colon = hhmm.indexOf(':');
        return Integer.parseInt(hhmm.substring(0, colon)) * 60 + Integer.parseInt(hhmm.substring(colon + 1));
    }

    static String formatCountdown(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    /** "HH:mm" (24h) to "h:mm a", e.g. "04:19" -> "4:19 AM". */
    static String display(String hhmm) {
        if (hhmm == null) return "—";
        int minutes = minutesOf(hhmm);
        int h24 = minutes / 60;
        int h12 = h24 % 12 == 0 ? 12 : h24 % 12;
        return String.format(Locale.US, "%d:%02d %s", h12, minutes % 60, h24 < 12 ? "AM" : "PM");
    }
}
