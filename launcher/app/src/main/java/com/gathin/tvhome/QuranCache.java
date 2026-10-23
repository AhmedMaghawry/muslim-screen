package com.gathin.tvhome;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The full Quran (Sahih International, Arabic and English) from alquran.cloud, fetched once per
 * app process and kept in memory afterwards so every later screensaver or preview reuses it
 * without a second ~1-2 MB download. {@link SalaamScreenController} falls back to
 * {@link QuranVerses}' small bundled list until this loads, or if it never does.
 */
final class QuranCache {
    private static final String TAG = "TvHome.Quran";
    private static final String ARABIC_URL = "https://api.alquran.cloud/v1/quran/ar.sahih";
    private static final String ENGLISH_URL = "https://api.alquran.cloud/v1/quran/en.sahih";
    private static final int TIMEOUT_MS = 25_000;

    private static volatile List<QuranVerses.Verse> cache;
    private static volatile boolean fetching;

    private QuranCache() {}

    /** The cached verses, or null if the fetch hasn't finished (or hasn't been started) yet. */
    static List<QuranVerses.Verse> get() {
        return cache;
    }

    /** Starts the fetch on its own background thread if it isn't cached yet. A fetch already in
     *  flight is not duplicated; callers just poll {@link #get()} on their own schedule. */
    static synchronized void loadOnce() {
        if (cache != null || fetching) return;
        fetching = true;
        new Thread(() -> {
            List<QuranVerses.Verse> verses = fetch();
            if (verses != null) cache = verses;
            fetching = false;
        }, "quran-fetch").start();
    }

    private static List<QuranVerses.Verse> fetch() {
        try {
            JSONArray arabicSurahs = new JSONObject(Net.get(ARABIC_URL, null, TIMEOUT_MS))
                    .getJSONObject("data").getJSONArray("surahs");
            JSONArray englishSurahs = new JSONObject(Net.get(ENGLISH_URL, null, TIMEOUT_MS))
                    .getJSONObject("data").getJSONArray("surahs");

            List<QuranVerses.Verse> verses = new ArrayList<>(6300);
            int surahCount = Math.min(arabicSurahs.length(), englishSurahs.length());
            for (int s = 0; s < surahCount; s++) {
                JSONObject arSurah = arabicSurahs.getJSONObject(s);
                JSONObject enSurah = englishSurahs.getJSONObject(s);
                JSONArray arAyahs = arSurah.getJSONArray("ayahs");
                JSONArray enAyahs = enSurah.getJSONArray("ayahs");
                String surahName = enSurah.getString("englishName");
                int surahNumber = enSurah.getInt("number");

                int ayahCount = Math.min(arAyahs.length(), enAyahs.length());
                for (int a = 0; a < ayahCount; a++) {
                    String arabicText = arAyahs.getJSONObject(a).getString("text");
                    JSONObject enAyah = enAyahs.getJSONObject(a);
                    String reference = surahName + " " + surahNumber + ":" + enAyah.getInt("numberInSurah");
                    verses.add(new QuranVerses.Verse(arabicText, enAyah.getString("text"), reference));
                }
            }
            return verses.isEmpty() ? null : verses;
        } catch (Exception e) {
            Log.w(TAG, "Quran fetch failed: " + e.getMessage());
            return null;
        }
    }
}
