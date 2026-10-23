package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Populates and live-updates a {@code dream_salaam.xml} view tree: prayer times, weather and a
 * Quran verse over a live mosque photo. Shared by {@link SalaamDream} (the system screensaver)
 * and {@link SalaamPreviewActivity} (Back at Home, to check it on demand without waiting for the
 * TV to go idle). Every network fetch (geolocation, Aladhan, Open-Meteo, Unsplash) fails soft,
 * leaving its own section blank rather than breaking the rest.
 */
final class SalaamScreenController {

    private static final long TICK_MS = 30_000L;
    private static final long VERSE_TICK_MS = 60_000L;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private ExecutorService network;

    private final ImageView dreamBg, weatherIcon;
    private final TextView locationLine, hijriLine, verseArabic, verseTranslation, verseReference;
    private final TextView nextPrayerName, nextPrayerCountdown, nextPrayerAt, prayerFooter;
    private final TextView timeFajr, timeDhuhr, timeAsr, timeMaghrib, timeIsha;
    private final LinearLayout rowFajr, rowDhuhr, rowAsr, rowMaghrib, rowIsha, hourlyRow;
    private final TextView weatherTemp, weatherDesc, weatherFeels, weatherHighLow, weatherUpdated, footerCredit;

    /** Fajr/Dhuhr/Asr/Maghrib/Isha for today, once the fetch lands; null until then or if it failed. */
    private PrayerTimes prayerTimes;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateNextPrayer();
            handler.postDelayed(this, TICK_MS);
        }
    };

    private final Runnable verseTick = new Runnable() {
        @Override
        public void run() {
            showVerse();
            handler.postDelayed(this, VERSE_TICK_MS);
        }
    };

    SalaamScreenController(Context context, View root) {
        this.context = context;
        dreamBg = root.findViewById(R.id.dreamBg);
        weatherIcon = root.findViewById(R.id.weatherIcon);
        locationLine = root.findViewById(R.id.locationLine);
        hijriLine = root.findViewById(R.id.hijriLine);
        verseArabic = root.findViewById(R.id.verseArabic);
        verseTranslation = root.findViewById(R.id.verseTranslation);
        verseReference = root.findViewById(R.id.verseReference);
        nextPrayerName = root.findViewById(R.id.nextPrayerName);
        nextPrayerCountdown = root.findViewById(R.id.nextPrayerCountdown);
        nextPrayerAt = root.findViewById(R.id.nextPrayerAt);
        prayerFooter = root.findViewById(R.id.prayerFooter);
        timeFajr = root.findViewById(R.id.timeFajr);
        timeDhuhr = root.findViewById(R.id.timeDhuhr);
        timeAsr = root.findViewById(R.id.timeAsr);
        timeMaghrib = root.findViewById(R.id.timeMaghrib);
        timeIsha = root.findViewById(R.id.timeIsha);
        rowFajr = root.findViewById(R.id.rowFajr);
        rowDhuhr = root.findViewById(R.id.rowDhuhr);
        rowAsr = root.findViewById(R.id.rowAsr);
        rowMaghrib = root.findViewById(R.id.rowMaghrib);
        rowIsha = root.findViewById(R.id.rowIsha);
        hourlyRow = root.findViewById(R.id.hourlyRow);
        weatherTemp = root.findViewById(R.id.weatherTemp);
        weatherDesc = root.findViewById(R.id.weatherDesc);
        weatherFeels = root.findViewById(R.id.weatherFeels);
        weatherHighLow = root.findViewById(R.id.weatherHighLow);
        weatherUpdated = root.findViewById(R.id.weatherUpdated);
        footerCredit = root.findViewById(R.id.footerCredit);
    }

    /** Shows a verse immediately (the bundled fallback, until the full Quran is cached), then
     *  fetches location/prayer times/weather/photo, and starts the verse and prayer ticks. */
    void start() {
        QuranCache.loadOnce();   // once per app process; later starts just reuse the cache
        showVerse();
        handler.postDelayed(tick, TICK_MS);
        handler.postDelayed(verseTick, VERSE_TICK_MS);
        startFetch();
    }

    /** Stops both ticks and any in-flight fetch, and releases the background photo. The Quran
     *  cache fetch is app-wide and is left to finish for next time, even if this one stops first. */
    void stop() {
        handler.removeCallbacksAndMessages(null);
        if (network != null) network.shutdownNow();
        if (dreamBg != null) dreamBg.setImageDrawable(null);
    }

    /** Picks from the full cached Quran once it has loaded; the small bundled list until then
     *  (or forever, if the fetch never lands). Called once on start and then every verse tick. */
    private void showVerse() {
        List<QuranVerses.Verse> pool = QuranCache.get();
        QuranVerses.Verse verse = pool != null ? pool.get(random.nextInt(pool.size())) : QuranVerses.random(random);
        verseArabic.setText(verse.arabic);
        verseTranslation.setText("“" + verse.translation + "”");
        verseReference.setText(verse.reference.toUpperCase(Locale.US) + "  ·  SAHIH INTERNATIONAL");
    }

    /** One background thread for the one-shot fetch; every result lands on the UI thread independently. */
    private void startFetch() {
        Config config = Config.load(context);
        network = Executors.newSingleThreadExecutor();
        network.execute(() -> {
            GeoLocation geo = GeoLocation.fetch();
            UnsplashPhoto photo = UnsplashPhoto.fetch(config.unsplashAccessKey);
            PrayerTimes prayer = geo != null
                    ? PrayerTimes.fetch(geo.latitude, geo.longitude, config.prayerCalcMethod, config.prayerAsrSchool)
                    : null;
            Weather weather = geo != null ? Weather.fetch(geo.latitude, geo.longitude) : null;
            handler.post(() -> {
                if (geo != null) applyLocation(geo);
                if (photo != null) applyPhoto(photo);
                if (prayer != null) applyPrayerTimes(prayer, config, geo);
                if (weather != null) applyWeather(weather);
            });
        });
    }

    private void applyLocation(GeoLocation geo) {
        StringBuilder sb = new StringBuilder();
        if (!geo.city.isEmpty()) sb.append(geo.city);
        if (!geo.country.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(geo.country);
        }
        locationLine.setText(sb.toString());
    }

    private void applyPhoto(UnsplashPhoto photo) {
        dreamBg.setImageBitmap(photo.bitmap);
        footerCredit.setText(photo.credit);
    }

    private void applyPrayerTimes(PrayerTimes prayer, Config config, GeoLocation geo) {
        this.prayerTimes = prayer;
        hijriLine.setText(prayer.hijriDate);
        timeFajr.setText(PrayerTimes.display(prayer.times.get("Fajr")));
        timeDhuhr.setText(PrayerTimes.display(prayer.times.get("Dhuhr")));
        timeAsr.setText(PrayerTimes.display(prayer.times.get("Asr")));
        timeMaghrib.setText(PrayerTimes.display(prayer.times.get("Maghrib")));
        timeIsha.setText(PrayerTimes.display(prayer.times.get("Isha")));

        String region = geo != null && !geo.country.isEmpty() ? geo.country : "World";
        String school = config.prayerAsrSchool == 1 ? "Hanafi Asr" : "Standard Asr";
        prayerFooter.setText(region + "  ·  AlAdhan  ·  " + school);

        updateNextPrayer();
    }

    private void applyWeather(Weather weather) {
        weatherTemp.setText(weather.currentTempC + "°");
        weatherDesc.setText(WeatherCode.label(weather.code));
        weatherFeels.setText("Feels like " + weather.feelsLikeC + "°  ·  Wind "
                + Math.round(weather.windKmh) + " km/h");
        weatherHighLow.setText("Today · H" + weather.highC + "° / L" + weather.lowC + "°");
        weatherIcon.setImageResource(WeatherCode.icon(weather.code, weather.isDay));
        weatherUpdated.setText("Open-Meteo · Updated " + nowDisplay());

        hourlyRow.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (Weather.Hour hour : weather.hours) {
            View column = inflater.inflate(R.layout.salaam_hour_column, hourlyRow, false);
            ((TextView) column.findViewById(R.id.hourLabel)).setText(hour.label);
            ((TextView) column.findViewById(R.id.hourTemp)).setText(hour.tempC + "°");
            ((TextView) column.findViewById(R.id.hourPop)).setText(hour.precipitationPercent + "% rain");
            ((ImageView) column.findViewById(R.id.hourIcon)).setImageResource(WeatherCode.icon(hour.code, true));
            hourlyRow.addView(column);
        }
    }

    /** Recomputes which prayer is next and its countdown; called once on load, then every tick. */
    private void updateNextPrayer() {
        if (prayerTimes == null) return;
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        PrayerTimes.Next next = prayerTimes.next(nowMinutes);

        highlightRow(next == null ? null : next.name);
        if (next == null) return;

        nextPrayerName.setText(next.name);
        nextPrayerCountdown.setText(PrayerTimes.formatCountdown(next.remainingMinutes));
        nextPrayerAt.setText((next.tomorrow ? "Tomorrow at " : "At ") + PrayerTimes.display(next.at));
    }

    private void highlightRow(String name) {
        setHighlighted(rowFajr, "Fajr".equals(name));
        setHighlighted(rowDhuhr, "Dhuhr".equals(name));
        setHighlighted(rowAsr, "Asr".equals(name));
        setHighlighted(rowMaghrib, "Maghrib".equals(name));
        setHighlighted(rowIsha, "Isha".equals(name));
    }

    private void setHighlighted(LinearLayout row, boolean highlighted) {
        if (row == null) return;
        row.setBackground(highlighted ? context.getDrawable(R.drawable.salaam_next_prayer_bg) : null);
    }

    private static String nowDisplay() {
        Calendar now = Calendar.getInstance();
        int h24 = now.get(Calendar.HOUR_OF_DAY);
        int h12 = h24 % 12 == 0 ? 12 : h24 % 12;
        return String.format(Locale.US, "%d:%02d %s", h12, now.get(Calendar.MINUTE), h24 < 12 ? "AM" : "PM");
    }
}
