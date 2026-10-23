package com.gathin.tvhome;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

/**
 * Rotates a short Arabic reminder on Home every few minutes, each shown on its own: no network,
 * so unlike the rest of Muslim Screen this one can't fail.
 */
final class DhikrRotator {
    private static final long ROTATE_MS = 5 * 60_000L;
    private static final String[] PHRASES = {"اذكر الله", "صلِّ على النبي"};

    private final TextView label;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int index;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            index = (index + 1) % PHRASES.length;
            label.setText(PHRASES[index]);
            handler.postDelayed(this, ROTATE_MS);
        }
    };

    DhikrRotator(TextView label) {
        this.label = label;
    }

    void start() {
        label.setText(PHRASES[index]);
        handler.postDelayed(tick, ROTATE_MS);
    }

    void stop() {
        handler.removeCallbacks(tick);
    }
}
