package com.jlxc.teleprompter.desktopremote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JsonUtil {
    private JsonUtil() {}

    static boolean bool(String json, String key, boolean fallback) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE).matcher(json == null ? "" : json);
        if (m.find()) return Boolean.parseBoolean(m.group(1));
        return fallback;
    }

    static String string(String json, String key, String fallback) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json == null ? "" : json);
        if (m.find()) return unescape(m.group(1));
        return fallback;
    }

    static int integer(String json, String key, int fallback) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)").matcher(json == null ? "" : json);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return fallback;
    }

    static long longValue(String json, String key, long fallback) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)").matcher(json == null ? "" : json);
        if (m.find()) {
            try { return Long.parseLong(m.group(1)); } catch (Exception ignored) {}
        }
        return fallback;
    }

    static String escape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.toString();
    }

    private static String unescape(String s) {
        if (s == null || s.indexOf('\\') < 0) return s;
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case '\\': out.append('\\'); break;
                    case '"': out.append('"'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try { out.append((char) Integer.parseInt(hex, 16)); i += 4; }
                            catch (Exception e) { out.append("\\u").append(hex); i += 4; }
                        } else out.append("\\u");
                        break;
                    default: out.append(n);
                }
            } else out.append(c);
        }
        return out.toString();
    }
}
