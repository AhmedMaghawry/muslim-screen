package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;

/**
 * Back at Home previews Muslim Screen on demand, without waiting for the TV to go idle (the real
 * screensaver, {@link SalaamDream}, only starts on the system's own idle timer). Any key finishes
 * back to Home, same as waking the real screensaver would.
 */
public class SalaamPreviewActivity extends Activity {

    /** The Back press that launched this activity can still be in flight (its own key-up) when
     *  this window gets focus; dismissing on that same event would close it before it is ever seen. */
    private static final long ARM_DELAY_MS = 500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SalaamScreenController controller;
    private boolean armed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setContentView(R.layout.dream_salaam);
        controller = new SalaamScreenController(this, getWindow().getDecorView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        armed = false;
        handler.postDelayed(() -> armed = true, ARM_DELAY_MS);
        controller.start();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        controller.stop();
        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (armed && event.getAction() == KeyEvent.ACTION_UP) finish();
        return true;
    }
}
