package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONObject;

/** A live mosque photo from Unsplash, with photographer credit. Needs config's unsplash_access_key. */
final class UnsplashPhoto {
    private static final String TAG = "TvHome.Unsplash";

    final Bitmap bitmap;
    /** "Photo by X on Unsplash", or "" when the photographer's name wasn't given. */
    final String credit;

    private UnsplashPhoto(Bitmap bitmap, String credit) {
        this.bitmap = bitmap;
        this.credit = credit;
    }

    /** Blocks on the network; call off the main thread. Returns null with no key, or on any failure. */
    static UnsplashPhoto fetch(String accessKey) {
        if (accessKey == null || accessKey.isEmpty()) return null;
        try {
            String url = "https://api.unsplash.com/photos/random?query=mosque&orientation=landscape&content_filter=high";
            JSONObject root = new JSONObject(Net.get(url, "Client-ID " + accessKey));
            String imageUrl = root.getJSONObject("urls").getString("regular");
            String photographer = root.getJSONObject("user").optString("name", "");

            byte[] bytes = Net.getBytes(imageUrl, null);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap == null) return null;

            String credit = photographer.isEmpty() ? "" : "Photo by " + photographer + " on Unsplash";
            return new UnsplashPhoto(bitmap, credit);
        } catch (Exception e) {
            Log.w(TAG, "unsplash photo failed: " + e.getMessage());
            return null;
        }
    }
}
