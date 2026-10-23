package com.gathin.tvhome;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Minimal blocking HTTP GET shared by Muslim Screen's data classes: one TLS request at a time,
 *  a handful of times a day, so no HTTP library is worth the dependency. */
final class Net {
    private static final int TIMEOUT_MS = 8000;

    private Net() {}

    /** Reads the whole body as UTF-8 text. authHeader, e.g. "Client-ID xxxx", or null for none. Throws on any failure. */
    static String get(String url, String authHeader) throws IOException {
        return get(url, authHeader, TIMEOUT_MS);
    }

    /** As {@link #get(String, String)}, with a custom timeout for a larger-than-usual body. */
    static String get(String url, String authHeader, int timeoutMs) throws IOException {
        HttpURLConnection c = open(url, authHeader, timeoutMs);
        try {
            int code = c.getResponseCode();
            try (InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream()) {
                String body = readAll(in);
                if (code >= 400) throw new IOException("HTTP " + code + ": " + body);
                return body;
            }
        } finally {
            c.disconnect();
        }
    }

    /** Fetches raw bytes (an image), with the same headers and timeout. */
    static byte[] getBytes(String url, String authHeader) throws IOException {
        HttpURLConnection c = open(url, authHeader);
        try {
            int code = c.getResponseCode();
            if (code >= 400) throw new IOException("HTTP " + code);
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }

    private static HttpURLConnection open(String url, String authHeader) throws IOException {
        return open(url, authHeader, TIMEOUT_MS);
    }

    private static HttpURLConnection open(String url, String authHeader, int timeoutMs) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(timeoutMs);
        c.setReadTimeout(timeoutMs);
        c.setRequestProperty("User-Agent", "cleanyourtv-launcher");
        c.setRequestProperty("Accept", "application/json");
        if (authHeader != null) c.setRequestProperty("Authorization", authHeader);
        return c;
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
