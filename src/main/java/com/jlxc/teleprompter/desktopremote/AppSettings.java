package com.jlxc.teleprompter.desktopremote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class AppSettings {
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".jlxc_teleprompter_desktop_remote.properties");

    String ip = "";
    int port = 47230;
    int sensitivity = 5;
    boolean reverse = false;
    boolean udpEnabled = true;
    boolean httpFallback = true;

    static AppSettings load() {
        AppSettings s = new AppSettings();
        if (!Files.exists(FILE)) return s;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            s.ip = p.getProperty("ip", s.ip);
            s.port = parseInt(p.getProperty("port"), s.port);
            s.sensitivity = clamp(parseInt(p.getProperty("sensitivity"), s.sensitivity), 1, 10);
            s.reverse = Boolean.parseBoolean(p.getProperty("reverse", Boolean.toString(s.reverse)));
            s.udpEnabled = Boolean.parseBoolean(p.getProperty("udpEnabled", Boolean.toString(s.udpEnabled)));
            s.httpFallback = Boolean.parseBoolean(p.getProperty("httpFallback", Boolean.toString(s.httpFallback)));
        } catch (IOException ignored) {
        }
        return s;
    }

    synchronized void save() {
        Properties p = new Properties();
        p.setProperty("ip", ip == null ? "" : ip.trim());
        p.setProperty("port", Integer.toString(port));
        p.setProperty("sensitivity", Integer.toString(clamp(sensitivity, 1, 10)));
        p.setProperty("reverse", Boolean.toString(reverse));
        p.setProperty("udpEnabled", Boolean.toString(udpEnabled));
        p.setProperty("httpFallback", Boolean.toString(httpFallback));
        try (OutputStream out = Files.newOutputStream(FILE)) {
            p.store(out, "JLXC Teleprompter Desktop Remote settings");
        } catch (IOException ignored) {
        }
    }

    int scrollStep() {
        return clamp(sensitivity, 1, 10) * 20;
    }

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
