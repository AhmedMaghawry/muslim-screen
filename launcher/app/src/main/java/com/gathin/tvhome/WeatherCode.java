package com.gathin.tvhome;

/** Open-Meteo WMO weather codes, collapsed to the handful of icons Muslim Screen draws. */
final class WeatherCode {
    private WeatherCode() {}

    static int icon(int code, boolean isDay) {
        if (code == 0 || code == 1) return isDay ? R.drawable.ic_weather_sun : R.drawable.ic_weather_moon;
        if (code == 2) return isDay ? R.drawable.ic_weather_cloud_sun : R.drawable.ic_weather_cloud_moon;
        if (code == 3) return R.drawable.ic_weather_cloud;
        if (code == 45 || code == 48) return R.drawable.ic_weather_fog;
        if (code >= 51 && code <= 67) return R.drawable.ic_weather_rain;
        if (code >= 71 && code <= 86) return R.drawable.ic_weather_snow;
        if (code >= 95) return R.drawable.ic_weather_thunder;
        return R.drawable.ic_weather_cloud;
    }

    static String label(int code) {
        switch (code) {
            case 0: return "Clear sky";
            case 1: return "Mostly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Fog";
            case 51: case 53: case 55: return "Drizzle";
            case 56: case 57: return "Freezing drizzle";
            case 61: case 63: case 65: return "Rain";
            case 66: case 67: return "Freezing rain";
            case 71: case 73: case 75: case 77: return "Snow";
            case 80: case 81: case 82: return "Rain showers";
            case 85: case 86: return "Snow showers";
            case 95: case 96: case 99: return "Thunderstorm";
            default: return "—";
        }
    }
}
