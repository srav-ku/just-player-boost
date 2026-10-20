package com.brouken.player;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnlineSubtitleSearch {

    public interface Callback {
        void onResult(List<Entry> entries);
    }

    public static class Entry {
        public final String url;
        public final String lang;
        public final String name;

        Entry(String url, String lang, String name) {
            this.url = url;
            this.lang = lang;
            this.name = name;
        }

        public String label() {
            final String language = langName(lang);
            if (name != null && !name.isEmpty() && !name.equalsIgnoreCase(language)) {
                return language + "  ·  " + name;
            }
            return language;
        }
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void search(final String text, final Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Entry> entries = new ArrayList<>();
                try {
                    entries = doSearch(text);
                } catch (Exception ignored) {
                }
                final List<Entry> result = entries;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(result);
                    }
                });
            }
        }).start();
    }

    private static List<Entry> doSearch(String text) {
        final Parsed parsed = parse(text);
        final List<Entry> entries = new ArrayList<>();
        final boolean series = parsed.season > 0 && parsed.episode > 0;
        String imdb = null;

        final String catalog = series ? "series" : "movie";
        String metaUrl = "https://v3-cinemeta.strem.io/catalog/" + catalog + "/top/search="
                + encode(parsed.title) + ".json";
        final String metaJson = http(metaUrl);
        if (metaJson != null) {
            try {
                JSONObject root = new JSONObject(metaJson);
                JSONArray metas = root.optJSONArray("metas");
                if (metas != null && metas.length() > 0) {
                    JSONObject best = metas.getJSONObject(0);
                    if (parsed.year > 0) {
                        for (int i = 0; i < metas.length(); i++) {
                            JSONObject candidate = metas.getJSONObject(i);
                            String release = candidate.optString("releaseInfo", "");
                            if (release.contains(String.valueOf(parsed.year))) {
                                best = candidate;
                                break;
                            }
                        }
                    }
                    imdb = best.optString("imdb_id", null);
                }
            } catch (Exception ignored) {
            }
        }

        if (imdb != null && !imdb.isEmpty()) {
            final String path = series
                    ? ("series/" + imdb + ":" + parsed.season + ":" + parsed.episode)
                    : ("movie/" + imdb);
            final String subsJson = http("https://opensubtitles-v3.strem.io/subtitles/" + path + ".json");
            if (subsJson != null) {
                try {
                    JSONObject root = new JSONObject(subsJson);
                    JSONArray arr = root.optJSONArray("subtitles");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject sub = arr.getJSONObject(i);
                            String url = sub.optString("url", null);
                            if (url == null || url.isEmpty()) {
                                continue;
                            }
                            String lang = sub.optString("lang", "");
                            String name = sub.optString("SubFileName", null);
                            if (name == null || name.isEmpty()) {
                                name = sub.optString("id", null);
                            }
                            entries.add(new Entry(url, lang, name));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return entries;
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private static String http(String urlStr) {
        HttpURLConnection connection = null;
        try {
            final URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "JustPlayer");
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

    private static class Parsed {
        String title = "";
        int year;
        int season;
        int episode;
    }

    private static Parsed parse(String raw) {
        final Parsed parsed = new Parsed();
        String s = raw == null ? "" : raw;
        final int slash = s.lastIndexOf('/');
        if (slash >= 0) {
            s = s.substring(slash + 1);
        }
        try {
            s = java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception ignored) {
        }
        s = s.replaceAll("(?i)\\.(mkv|mp4|avi|mov|webm|m4v|ts)$", "");
        s = s.replace('.', ' ').replace('_', ' ');

        Matcher seasonEpisode = Pattern.compile("(?i)S(\\d{1,2})[ .]?E(\\d{1,3})").matcher(s);
        if (seasonEpisode.find()) {
            parsed.season = Integer.parseInt(seasonEpisode.group(1));
            parsed.episode = Integer.parseInt(seasonEpisode.group(2));
        }

        Matcher year = Pattern.compile("(19\\d{2}|20\\d{2})").matcher(s);
        if (year.find()) {
            parsed.year = Integer.parseInt(year.group(1));
        }

        final String[] tokens = s.split("\\s+");
        final StringBuilder title = new StringBuilder();
        for (String token : tokens) {
            if (token.matches("(?i).*(1080p|720p|2160p|480p|4k|bluray|blu-ray|webrip|web-dl|webdl|hdrip|brrip|dvdrip|hdtv|x264|x265|h264|h265|hevc|avc|aac|ac3|eac3|dts|ddp|10bit|8bit|hdr|remux|proper|repack|extended|esub|multi).*")) {
                continue;
            }
            if (token.matches("(19|20)\\d{2}")) {
                continue;
            }
            if (token.matches("(?i)[sS]\\d{1,2}")) {
                continue;
            }
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(token);
        }
        parsed.title = title.toString().trim();
        if (parsed.title.isEmpty()) {
            parsed.title = s.trim();
        }
        return parsed;
    }

    private static String langName(String code) {
        if (code == null) {
            return "Unknown";
        }
        switch (code.toLowerCase()) {
            case "en": return "English";
            case "te": return "Telugu";
            case "hi": return "Hindi";
            case "ta": return "Tamil";
            case "ml": return "Malayalam";
            case "kn": return "Kannada";
            case "es": return "Spanish";
            case "fr": return "French";
            case "de": return "German";
            case "it": return "Italian";
            case "pt": return "Portuguese";
            case "ru": return "Russian";
            case "ar": return "Arabic";
            case "zh": return "Chinese";
            case "ja": return "Japanese";
            case "ko": return "Korean";
            default: return code.toUpperCase();
        }
    }
}