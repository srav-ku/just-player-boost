package com.brouken.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Talks to the Stremio account API (api.strem.io) so progress made in this
 * external player lands in the user's Stremio library and shows up on any
 * other device signed into the same account.
 */
public class StremioSync {

    private static final String TAG = "JP_SYNC";
    private static final String API = "https://api.strem.io/api/";
    private static final String PREFS = "stremio_sync";

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static long lastPushAt = 0L;
    private static long lastPushPosition = -1L;

    public interface Callback {
        void done(boolean ok, String message);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isLoggedIn(Context context) {
        return prefs(context).getString("authKey", null) != null;
    }

    public static void loginAsync(final Context context, final String email, final String password, final Callback callback) {
        EXEC.execute(new Runnable() {
            @Override
            public void run() {
                String message;
                boolean ok = false;
                try {
                    final JSONObject body = new JSONObject();
                    body.put("email", email);
                    body.put("password", password);
                    final String response = http(API + "login", body.toString());
                    Log.d(TAG, "login response: " + trim(response));
                    final JSONObject json = new JSONObject(response != null ? response : "{}");
                    if (json.has("error")) {
                        message = "Login failed: " + json.getJSONObject("error").optString("message");
                    } else {
                        final JSONObject result = json.getJSONObject("result");
                        final String authKey = result.getString("authKey");
                        final String userId = result.optJSONObject("user") != null ? result.getJSONObject("user").optString("_id") : "";
                        prefs(context).edit()
                                .putString("authKey", authKey)
                                .putString("userId", userId)
                                .putString("email", email)
                                .apply();
                        ok = true;
                        message = "Signed in";
                    }
                } catch (Exception e) {
                    message = "Login error: " + e.getMessage();
                    Log.d(TAG, "login exception " + e);
                }
                final boolean okFinal = ok;
                final String msgFinal = message;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.done(okFinal, msgFinal);
                        }
                    }
                });
            }
        });
    }

    /** Push progress to the Stremio account. Safe to call often; throttled. */
    public static void pushAsync(final Context context, final String titleText, final long positionMs, final long durationMs, final boolean force) {
        final String authKey = prefs(context).getString("authKey", null);
        if (authKey == null || positionMs <= 0 || durationMs <= 0) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (!force && now - lastPushAt < 20000L && Math.abs(positionMs - lastPushPosition) < 15000L) {
            return;
        }
        lastPushAt = now;
        lastPushPosition = positionMs;
        EXEC.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Resolved resolved = resolve(titleText);
                    if (resolved == null || resolved.imdb == null) {
                        Log.d(TAG, "push: could not resolve in " + titleText);
                        return;
                    }
                    final String videoId = resolved.series
                            ? resolved.imdb + ":" + resolved.season + ":" + resolved.episode
                            : resolved.imdb;
                    final JSONObject getBody = new JSONObject();
                    getBody.put("authKey", authKey);
                    getBody.put("collection", "libraryItem");
                    getBody.put("ids", new JSONArray().put(resolved.imdb));
                    getBody.put("all", false);
                    final String getResponse = http(API + "datastoreGet", getBody.toString());
                    final JSONObject getJson = new JSONObject(getResponse != null ? getResponse : "{}");
                    if (getJson.has("error")) {
                        Log.d(TAG, "datastoreGet error: " + getJson);
                        return;
                    }
                    final JSONArray items = getJson.optJSONArray("result");
                    if (items == null || items.length() == 0) {
                        Log.d(TAG, "push: no library item for " + resolved.imdb);
                        return;
                    }
                    final JSONObject item = items.getJSONObject(0);
                    JSONObject state = item.optJSONObject("state");
                    if (state == null) {
                        state = new JSONObject();
                    }
                    final long incoming = positionMs / 1000;
                    final long remote = state.optLong("timeOffset", 0L);
                    final long durationSec = durationMs / 1000;
                    final boolean nearEnd = durationSec > 0 && incoming >= durationSec - 30;
                    if (!nearEnd && remote > 0 && incoming + 60 < remote) {
                        Log.d(TAG, "push skipped: remote " + remote + "s > local " + incoming + "s");
                        return;
                    }
                    state.put("timeOffset", incoming);
                    state.put("duration", durationMs / 1000);
                    state.put("lastWatched", isoNow());
                    state.put("video_id", videoId);
                    if (resolved.series) {
                        state.put("season", resolved.season);
                        state.put("episode", resolved.episode);
                    }
                    item.put("state", state);
                    item.put("_mtime", isoNow());
                    final JSONObject putBody = new JSONObject();
                    putBody.put("authKey", authKey);
                    putBody.put("collection", "libraryItem");
                    putBody.put("changes", new JSONArray().put(item));
                    final String putResponse = http(API + "datastorePut", putBody.toString());
                    Log.d(TAG, "datastorePut <- " + trim(putResponse) + " at " + (positionMs / 1000) + "s timeOffset=" + incoming + "ms duration=" + durationLocal + "ms");
                    verifyLibrary(videoId);
                } catch (Exception e) {
                    Log.d(TAG, "push exception: " + e);
                }
            }
        });
    }

    private static void verifyLibrary(final String videoId) {
        try {
            final JSONObject body = new JSONObject();
            body.put("authKey", authKey);
            body.put("collection", "libraryItem");
            body.put("all", true);
            final JSONObject json = new JSONObject(http(API + "datastoreGet", body.toString()));
            final JSONArray items = json.optJSONArray("result");
            if (items == null) {
                Log.d(TAG, "verify: no items");
                return;
            }
            int samples = 0;
            for (int i = 0; i < items.length(); i++) {
                final JSONObject it = items.optJSONObject(i);
                if (it == null) {
                    continue;
                }
                final JSONObject st = it.optJSONObject("state");
                if (st == null) {
                    continue;
                }
                final String vid = st.optString("video_id", "");
                if (vid.equals(videoId)) {
                    Log.d(TAG, "verify MATCH " + it.optString("name") + " timeOffset=" + st.optLong("timeOffset", 0) + " duration=" + st.optLong("duration", 0) + " timesWatched=" + st.optLong("timesWatched", 0) + " flagged=" + st.optBoolean("flaggedWatched", false));
                } else if (samples < 3) {
                    samples++;
                    Log.d(TAG, "sample " + it.optString("name") + " type=" + it.optString("type") + " timeOffset=" + st.optLong("timeOffset", 0) + " duration=" + st.optLong("duration", 0));
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "verify exception: " + e);
        }
    }

    private static String isoNow() {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private static String trim(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    private static String http(String urlStr, String body) {
        HttpURLConnection connection = null;
        try {
            final URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            final byte[] bytes = body.getBytes("UTF-8");
            connection.setFixedLengthStreamingMode(bytes.length);
            final OutputStream out = connection.getOutputStream();
            try {
                out.write(bytes);
            } finally {
                out.close();
            }
            final int code = connection.getResponseCode();
            final StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 400 ? connection.getErrorStream() : connection.getInputStream()));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } finally {
                reader.close();
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static class Resolved {
        String imdb;
        boolean series;
        int season;
        int episode;
    }

    private static Resolved resolve(String raw) {
        try {
            String s = raw == null ? "" : raw;
            final int slash = s.lastIndexOf('/');
            if (slash >= 0) {
                s = s.substring(slash + 1);
            }
            try {
                s = java.net.URLDecoder.decode(s, "UTF-8");
            } catch (Exception ignored) {
            }
            s = s.replaceAll("(?i)\\.(mkv|mp4|avi|mov|webm|m4v|ts)$", "").replace('.', ' ').replace('_', ' ');

            final Resolved out = new Resolved();
            final Matcher seasonEpisode = Pattern.compile("(?i)S(\\d{1,2})[ .]?E(\\d{1,3})").matcher(s);
            if (seasonEpisode.find()) {
                out.series = true;
                out.season = Integer.parseInt(seasonEpisode.group(1));
                out.episode = Integer.parseInt(seasonEpisode.group(2));
            }
            int year = 0;
            final Matcher yearMatcher = Pattern.compile("(19\\d{2}|20\\d{2})").matcher(s);
            if (yearMatcher.find()) {
                year = Integer.parseInt(yearMatcher.group(1));
            }
            final StringBuilder title = new StringBuilder();
            for (String token : s.split("\\s+")) {
                if (token.matches("(?i).*(1080p|720p|2160p|480p|4k|bluray|blu-ray|webrip|web-dl|webdl|hdrip|brrip|dvdrip|hdtv|x264|x265|h264|h265|hevc|avc|aac|ac3|eac3|dts|ddp|10bit|8bit|hdr|remux|proper|repack|extended|esub|multi).*")) {
                    continue;
                }
                if (token.matches("(19|20)\\d{2}")) {
                    continue;
                }
                if (token.matches("(?i)[sS]\\d{1,2}[eE]\\d{1,3}")) {
                    continue;
                }
                if (title.length() > 0) {
                    title.append(' ');
                }
                title.append(token);
            }
            String query = title.toString().trim();
            if (query.isEmpty()) {
                return null;
            }
            final String catalog = out.series ? "series" : "movie";
            final String metaJson = httpGet("https://v3-cinemeta.strem.io/catalog/" + catalog + "/top/search="
                    + URLEncoder.encode(query, "UTF-8") + ".json");
            if (metaJson == null) {
                return null;
            }
            final JSONObject root = new JSONObject(metaJson);
            final JSONArray metas = root.optJSONArray("metas");
            if (metas == null || metas.length() == 0) {
                return null;
            }
            JSONObject best = metas.getJSONObject(0);
            if (year > 0) {
                for (int i = 0; i < metas.length(); i++) {
                    final JSONObject candidate = metas.getJSONObject(i);
                    if (candidate.optString("releaseInfo", "").contains(String.valueOf(year))) {
                        best = candidate;
                        break;
                    }
                }
            }
            out.imdb = best.optString("imdb_id", null);
            return out.imdb == null || out.imdb.isEmpty() ? null : out;
        } catch (Exception e) {
            return null;
        }
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection connection = null;
        try {
            final URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            if (connection.getResponseCode() != 200) {
                return null;
            }
            final StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } finally {
                reader.close();
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}