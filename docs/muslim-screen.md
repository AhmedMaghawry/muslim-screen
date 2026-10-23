# Muslim Screen

A second screensaver, alongside the built-in `Clock`: prayer times, a Quran verse and the weather
over a live mosque photo. Unlike `Clock`, it needs the network — location, prayer times, weather
and the photo are all fetched when it starts, and each piece fails on its own rather than taking
the rest of the screen down with it.

Pick it in the TV's own Settings → Screen saver (it's registered as a second option next to
`Clock`), or set it directly:

```bash
TV_HOST=192.168.1.50 tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.SalaamDream'
```

## Data sources

| What | Source | Key needed |
|---|---|---|
| Location | [ipapi.co](https://ipapi.co) (IP geolocation; most TVs have no GPS) | none |
| Prayer times, Hijri date | [Aladhan](https://aladhan.com/prayer-times-api) | none |
| Weather, hourly forecast | [Open-Meteo](https://open-meteo.com) | none |
| Background photo | [Unsplash](https://unsplash.com/developers) | `unsplash_access_key` |
| Quran verse | bundled, six short verses, picked at random | none (no network) |

Only the photo needs a key, because Unsplash is the one source here that requires one. Get a free
Access Key from [unsplash.com/developers](https://unsplash.com/developers) and set it locally, or
push it with a theme (never commit a real key — this repo is public):

```json
{ "unsplash_access_key": "your-access-key-here" }
```

With no key, the background is a plain dark gradient instead of a photo; everything else still
works.

## Config keys

| Key | Type | Meaning |
|---|---|---|
| `unsplash_access_key` | string | Unsplash Access Key for the background photo; empty shows a plain gradient instead |
| `prayer_calc_method` | 0–23 | Aladhan calculation method ([api.aladhan.com/v1/methods](https://api.aladhan.com/v1/methods)); 3 (Muslim World League) by default |
| `prayer_asr_school` | 0 or 1 | Asr juristic school: 0 Standard (Shafi/Maliki/Hanbali, the default), 1 Hanafi |

## Verifying it on a real TV

Daydreams only start once the TV is genuinely idle, and checking over ADB resets that same idle
timer — polling `dumpsys dreams` to see when it activates mostly proves that polling stops it from
ever activating. The reliable way to preview it is on the TV itself: Settings → Device → Screen
saver → **Start now**.

## What's not verified

The Arabic text and the Sahih International translation in `QuranVerses.java` were typed from
memory, not pulled from a verified source. Check them against a trusted Quran text before relying
on this for more than a screensaver.
