package com.gathin.tvhome;

import android.service.dreams.DreamService;

/**
 * Screensaver: prayer times, weather and a Quran verse over a live mosque photo. The actual
 * rendering lives in {@link SalaamScreenController}, shared with {@link SalaamPreviewActivity}
 * (Back at Home previews the same screen on demand, without waiting for the TV to go idle).
 */
public class SalaamDream extends DreamService {

    private SalaamScreenController controller;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(true);   // this is read, not ambient decoration
        setContentView(R.layout.dream_salaam);
        controller = new SalaamScreenController(this, getWindow().getDecorView());
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        controller.start();
    }

    @Override
    public void onDreamingStopped() {
        controller.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        controller.stop();
        super.onDetachedFromWindow();
    }
}
