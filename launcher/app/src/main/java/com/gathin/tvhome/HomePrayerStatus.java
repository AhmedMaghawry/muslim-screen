package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A compact "next prayer" status line on Home, e.g. "Asr in 2h 40m  ·  3:24 PM" — kept current
 * only while Home is on screen, same as {@code show_stats}. Location comes from the TV's public
 * IP; if that fails, or Aladhan does, the line just stays hidden rather than showing stale data.
 */
final class HomePrayerStatus {
    private static final long TICK_MS = 30_000L;

    private final Context context;
    private final TextView label;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService network;
    private PrayerTimes prayerTimes;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
            handler.postDelayed(this, TICK_MS);
        }
    };

    HomePrayerStatus(Context context, TextView label) {
        this.context = context;
        this.label = label;
    }

    /** Fetches fresh each time (Home can stay resumed for days), then ticks the countdown. */
    void start() {
        handler.postDelayed(tick, TICK_MS);
        Config config = Config.load(context);
        network = Executors.newSingleThreadExecutor();
        network.execute(() -> {
            GeoLocation geo = GeoLocation.fetch();
            PrayerTimes prayer = geo != null
                    ? PrayerTimes.fetch(geo.latitude, geo.longitude, config.prayerCalcMethod, config.prayerAsrSchool)
                    : null;
            if (prayer != null) handler.post(() -> {
                prayerTimes = prayer;
                update();
            });
        });
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);
        if (network != null) network.shutdownNow();
        prayerTimes = null;
        label.setVisibility(View.GONE);
    }

    private void update() {
        PrayerTimes.Next next = prayerTimes == null ? null
                : prayerTimes.next(nowMinutes());
        if (next == null) {
            label.setVisibility(View.GONE);
            return;
        }
        label.setVisibility(View.VISIBLE);
        label.setText(next.name + " in " + PrayerTimes.formatCountdown(next.remainingMinutes)
                + "  ·  " + PrayerTimes.display(next.at));
    }

    private static int nowMinutes() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    }
}
