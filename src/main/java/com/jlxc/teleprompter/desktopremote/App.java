package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;
import java.awt.*;

public final class App {
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            Image icon = Theme.loadAppIcon();
            try {
                if (Taskbar.isTaskbarSupported()) {
                    Taskbar taskbar = Taskbar.getTaskbar();
                    if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) taskbar.setIconImage(icon);
                }
            } catch (Exception ignored) {
            }
            MainFrame frame = new MainFrame();
            frame.setIconImage(icon);
            frame.setVisible(true);
        });
    }
}
