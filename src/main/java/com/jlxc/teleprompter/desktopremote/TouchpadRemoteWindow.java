package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class TouchpadRemoteWindow extends JFrame {
    private final RemoteClient client;
    private final AppSettings settings;
    private final Consumer<String> errorCallback;
    private final JLabel stateLabel;
    private final AtomicInteger pendingDy = new AtomicInteger(0);
    private Timer sendTimer;

    TouchpadRemoteWindow(Window owner, RemoteClient client, AppSettings settings, Consumer<String> errorCallback) {
        super("滑动控制");
        this.client = client;
        this.settings = settings;
        this.errorCallback = errorCallback;
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setBackground(Theme.BG);

        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        SlideToConfirmView exitSlider = new SlideToConfirmView("滑动退出", SlideToConfirmView.Direction.RIGHT, this::exitFullScreen);
        SlideToConfirmView topSlider = new SlideToConfirmView("滑动回到顶部", SlideToConfirmView.Direction.LEFT, this::sendTop);
        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        top.add(exitSlider, Theme.gbc(0, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        stateLabel = Theme.label("已连接：" + client.endpoint(), 14, Font.BOLD, Theme.ACCENT);
        stateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(stateLabel, Theme.gbc(1, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        top.add(topSlider, Theme.gbc(2, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(top, BorderLayout.NORTH);

        TouchpadPanel pad = new TouchpadPanel();
        root.add(pad, BorderLayout.CENTER);
        setContentPane(root);

        sendTimer = new Timer(24, e -> flushPendingScroll());
        sendTimer.start();

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private void flushPendingScroll() {
        int dy = pendingDy.getAndSet(0);
        if (dy == 0) return;
        new Thread(() -> {
            try {
                client.scroll(dy, settings.udpEnabled, settings.httpFallback);
                SwingUtilities.invokeLater(() -> {
                    stateLabel.setForeground(Theme.ACCENT);
                    stateLabel.setText("SCROLL " + dy);
                });
            } catch (Exception e) {
                String msg = RemoteClient.humanError(e);
                SwingUtilities.invokeLater(() -> {
                    stateLabel.setForeground(Theme.DANGER);
                    stateLabel.setText("发送失败：" + msg);
                });
                if (errorCallback != null) errorCallback.accept(msg);
            }
        }, "touchpad-scroll-send").start();
    }

    private void addPendingDelta(float deltaY) {
        int dy = Math.round(-deltaY * (settings.sensitivity / 3.0f));
        if (settings.reverse) dy = -dy;
        if (dy != 0) pendingDy.addAndGet(dy);
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
        if (sendTimer != null) sendTimer.stop();
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.getFullScreenWindow() == this) device.setFullScreenWindow(null);
        dispose();
    }

    private final class TouchpadPanel extends JPanel {
        private int lastY = -1;
        private long lastMoveTime = 0;

        TouchpadPanel() {
            setOpaque(true);
            setBackground(Theme.CARD);
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            MouseAdapter adapter = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    lastY = e.getY();
                    lastMoveTime = System.currentTimeMillis();
                }

                @Override public void mouseDragged(MouseEvent e) {
                    if (lastY >= 0) {
                        int dy = e.getY() - lastY;
                        if (Math.abs(dy) > 0) addPendingDelta(dy);
                    }
                    lastY = e.getY();
                    lastMoveTime = System.currentTimeMillis();
                }

                @Override public void mouseReleased(MouseEvent e) {
                    lastY = -1;
                    pendingDy.set(0);
                    stateLabel.setForeground(Theme.SUB_TEXT);
                    stateLabel.setText("停止滑动");
                }

                @Override public void mouseExited(MouseEvent e) {
                    if (System.currentTimeMillis() - lastMoveTime > 200) {
                        lastY = -1;
                    }
                }
            };
            addMouseListener(adapter);
            addMouseMotionListener(adapter);
            addMouseWheelListener(e -> {
                double rotation = e.getPreciseWheelRotation();
                int dy = (int) Math.round(rotation * settings.scrollStep());
                if (settings.reverse) dy = -dy;
                if (dy != 0) {
                    pendingDy.addAndGet(dy);
                    stateLabel.setForeground(Theme.SUB_TEXT);
                    stateLabel.setText(dy > 0 ? "鼠标滚轮：继续往后" : "鼠标滚轮：向前回退");
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.CARD);
            g2.fillRoundRect(0, 0, w, h, 34, 34);
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 40));
            for (int y = 80; y < h; y += 80) {
                g2.drawLine(50, y, w - 50, y);
            }
            g2.setFont(Theme.titleFont(36));
            g2.setColor(Theme.TEXT);
            String main = "滑动控制提词器滚动";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(main, (w - fm.stringWidth(main)) / 2, h / 2 - 20);
            g2.setFont(Theme.normalFont(18));
            g2.setColor(Theme.SUB_TEXT);
            String sub = "按住鼠标上下滑动，或使用鼠标滚轮控制。向上：继续；向下：回退。";
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(sub, (w - fm2.stringWidth(sub)) / 2, h / 2 + 28);
            g2.dispose();
        }
    }
}
