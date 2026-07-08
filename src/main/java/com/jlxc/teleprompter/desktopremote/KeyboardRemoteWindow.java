package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

final class KeyboardRemoteWindow extends JFrame {
    private static final int KEY_VOLUME_DOWN = 174;
    private static final int KEY_VOLUME_UP = 175;
    private final RemoteClient client;
    private final AppSettings settings;
    private final Consumer<String> errorCallback;
    private final JLabel stateLabel;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private Timer repeatTimer;
    private int activeDirection = 0;

    KeyboardRemoteWindow(Window owner, RemoteClient client, AppSettings settings, Consumer<String> errorCallback) {
        super("键盘控制");
        this.client = client;
        this.settings = settings;
        this.errorCallback = errorCallback;
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setBackground(Theme.BG);

        JPanel root = new JPanel(new BorderLayout(20, 20));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(36, 36, 24, 36));

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Theme.BG);
        JLabel title = Theme.label("键盘控制", 40, Font.BOLD, Theme.TEXT);
        JTextArea desc = Theme.helpText("防误触全屏模式：按 ↓ / → / PageDown 继续往后滚动；按 ↑ / ← / PageUp 向前回退；长按会连续发送。空格：快速回到文档开头。", 16);
        stateLabel = Theme.label("已连接：" + client.endpoint(), 16, Font.BOLD, Theme.ACCENT);
        center.add(title, Theme.gbc(0, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        center.add(desc, Theme.gbc(0, 1, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        center.add(stateLabel, Theme.gbc(0, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(center, BorderLayout.CENTER);

        SlideToConfirmView slider = new SlideToConfirmView("向右滑动解锁退出", SlideToConfirmView.Direction.RIGHT, this::exitFullScreen);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(slider, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

        installKeyBindings();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) {
                requestFocusInWindow();
                getRootPane().requestFocusInWindow();
            }
        });

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private void installKeyBindings() {
        bindDirection(KeyEvent.VK_DOWN, 1);
        bindDirection(KeyEvent.VK_RIGHT, 1);
        bindDirection(KeyEvent.VK_PAGE_DOWN, 1);
        bindDirection(KeyEvent.VK_S, 1);
        bindDirection(KEY_VOLUME_DOWN, 1);

        bindDirection(KeyEvent.VK_UP, -1);
        bindDirection(KeyEvent.VK_LEFT, -1);
        bindDirection(KeyEvent.VK_PAGE_UP, -1);
        bindDirection(KeyEvent.VK_W, -1);
        bindDirection(KEY_VOLUME_UP, -1);

        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "topBySpace");
        am.put("topBySpace", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { sendTop(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0, false), "top");
        am.put("top", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { sendTop(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "escHint");
        am.put("escHint", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                stateLabel.setForeground(Theme.SUB_TEXT);
                stateLabel.setText("防误触模式下不会直接退出，请滑动底部滑块解锁退出");
            }
        });
    }

    private void bindDirection(int keyCode, int direction) {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        String pressed = "pressed_" + keyCode;
        String released = "released_" + keyCode;
        im.put(KeyStroke.getKeyStroke(keyCode, 0, false), pressed);
        im.put(KeyStroke.getKeyStroke(keyCode, 0, true), released);
        am.put(pressed, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                keyPressedOnce(keyCode, direction);
            }
        });
        am.put(released, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                keyReleasedOnce(keyCode, direction);
            }
        });
    }

    private synchronized void keyPressedOnce(int keyCode, int direction) {
        if (pressedKeys.add(keyCode)) {
            activeDirection = direction;
            sendStep(direction);
            startRepeat(direction);
        }
    }

    private synchronized void keyReleasedOnce(int keyCode, int direction) {
        pressedKeys.remove(keyCode);
        if (pressedKeys.isEmpty() || activeDirection == direction) {
            stopRepeat();
            if (!pressedKeys.isEmpty()) {
                Integer first = pressedKeys.iterator().next();
                int d = directionForKey(first);
                if (d != 0) startRepeat(d);
            }
        }
    }

    private int directionForKey(int keyCode) {
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_PAGE_DOWN || keyCode == KeyEvent.VK_S || keyCode == KEY_VOLUME_DOWN) return 1;
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_W || keyCode == KEY_VOLUME_UP) return -1;
        return 0;
    }

    private void startRepeat(int direction) {
        stopRepeat();
        activeDirection = direction;
        repeatTimer = new Timer(70, e -> sendStep(direction));
        repeatTimer.start();
    }

    private void stopRepeat() {
        if (repeatTimer != null) {
            repeatTimer.stop();
            repeatTimer = null;
        }
    }

    private void sendStep(int direction) {
        int dy = direction * settings.scrollStep();
        if (settings.reverse) dy = -dy;
        final int finalDy = dy;
        new Thread(() -> {
            try {
                client.scroll(finalDy, settings.udpEnabled, settings.httpFallback);
                SwingUtilities.invokeLater(() -> {
                    stateLabel.setForeground(Theme.ACCENT);
                    stateLabel.setText(finalDy > 0 ? "继续往后滚动：SCROLL " + finalDy : "向前回退：SCROLL " + finalDy);
                });
            } catch (Exception e) {
                String msg = RemoteClient.humanError(e);
                SwingUtilities.invokeLater(() -> {
                    stateLabel.setForeground(Theme.DANGER);
                    stateLabel.setText("发送失败：" + msg);
                });
                if (errorCallback != null) errorCallback.accept(msg);
            }
        }, "scroll-key-send").start();
    }

    private void sendTop() {
        new Thread(() -> {
            try {
                client.top(settings.udpEnabled, settings.httpFallback);
                SwingUtilities.invokeLater(() -> {
                    stateLabel.setForeground(Theme.ACCENT);
                    stateLabel.setText("已发送：回到顶部");
                });
            } catch (Exception e) {
                String msg = RemoteClient.humanError(e);
                SwingUtilities.invokeLater(() -> {
                    stateLabel.setForeground(Theme.DANGER);
                    stateLabel.setText("发送失败：" + msg);
                });
                if (errorCallback != null) errorCallback.accept(msg);
            }
        }, "top-send").start();
    }

    private void exitFullScreen() {
        stopRepeat();
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.getFullScreenWindow() == this) device.setFullScreenWindow(null);
        dispose();
    }
}
