package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;

public final class App {
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
