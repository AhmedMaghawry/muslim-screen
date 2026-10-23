package com.gathin.tvhome;

import android.util.Log;

import org.json.JSONObject;

/** One-shot IP geolocation for Muslim Screen: most TVs have no GPS, so the TV's public IP stands in. */
final class GeoLocation {
    private static final String TAG = "TvHome.Geo";

    final double latitude;
    final double longitude;
    final String city;
    final String region;
    final String country;
    final String timezone;

    private GeoLocation(double latitude, double longitude, String city, String region, String country, String timezone) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.region = region;
        this.country = country;
        this.timezone = timezone;
    }

    /** Blocks on the network; call off the main thread. Returns null on any failure. */
    static GeoLocation fetch() {
        try {
            JSONObject o = new JSONObject(Net.get("https://ipapi.co/json/", null));
            if (o.has("error")) return null;
            return new GeoLocation(o.getDouble("latitude"), o.getDouble("longitude"),
                    o.optString("city", ""), o.optString("region", ""),
                    o.optString("country_name", ""), o.optString("timezone", "UTC"));
        } catch (Exception e) {
            Log.w(TAG, "geolocation failed: " + e.getMessage());
            return null;
        }
    }
}
