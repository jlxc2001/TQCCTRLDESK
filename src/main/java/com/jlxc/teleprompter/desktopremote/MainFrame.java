package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class MainFrame extends JFrame {
    private final AppSettings settings = AppSettings.load();

    private JTextField ipField;
    private JTextField portField;
    private JLabel statusLabel;
    private JLabel uploadSupportLabel;
    private JButton connectButton;
    private JButton keyboardModeButton;
    private JButton touchpadModeButton;
    private JButton scriptButton;
    private JButton pauseButton;
    private JButton topButton;
    private JSlider sensitivitySlider;
    private JCheckBox reverseBox;
    private JCheckBox udpBox;
    private JCheckBox httpFallbackBox;

    private RemoteClient client;
    private boolean connected = false;
    private boolean scriptUploadSupported = false;
    private boolean paused = false;
    private Timer pingTimer;
    private final AtomicBoolean pinging = new AtomicBoolean(false);

    MainFrame() {
        super("JLXC 提词器遥控器 - Windows / macOS");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(520, 720));
        setSize(560, 820);
        setLocationRelativeTo(null);
        buildUi();
        refreshButtonState();
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = Theme.label("提词器遥控器", 28, Font.BOLD, Theme.TEXT);
        JLabel sub = Theme.label("Windows / macOS 桌面控制端", 13, Font.PLAIN, Theme.SUB_TEXT);
        root.add(title, Theme.gbc(0, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(sub, Theme.gbc(0, 1, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        root.add(connectionCard(), Theme.gbc(0, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(remoteModeCard(), Theme.gbc(0, 3, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(scriptCard(), Theme.gbc(0, 4, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(settingsCard(), Theme.gbc(0, 5, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(protocolHint(), Theme.gbc(0, 6, 1, 1, 1, 1, GridBagConstraints.BOTH));

        JScrollPane sp = Theme.scroll(root);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        setContentPane(sp);
    }

    private JPanel connectionCard() {
        JPanel card = Theme.card();
        ipField = Theme.textField(settings.ip);
        portField = Theme.textField(Integer.toString(settings.port));
        statusLabel = Theme.label("未连接", 14, Font.BOLD, Theme.MUTED);
        uploadSupportLabel = Theme.label("连接后检测是否支持远程发送文稿", 13, Font.PLAIN, Theme.SUB_TEXT);
        connectButton = Theme.primaryButton("连接");
        connectButton.addActionListener(e -> connect());

        card.add(Theme.label("提词器手机 / 平板 IP", 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(ipField, Theme.gbc(0, 1, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(Theme.label("端口号", 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 2, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(portField, Theme.gbc(0, 3, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(statusLabel, Theme.gbc(0, 4, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(uploadSupportLabel, Theme.gbc(0, 5, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(connectButton, Theme.gbc(0, 6, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        return card;
    }

    private JPanel remoteModeCard() {
        JPanel card = Theme.card();
        card.add(Theme.label("遥控功能", 18, Font.BOLD, Theme.TEXT), Theme.gbc(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        JLabel desc = Theme.label("选择桌面遥控模式。键盘模式支持方向键、PageUp/PageDown、W/S，以及系统传递给窗口的音量键。", 13, Font.PLAIN, Theme.SUB_TEXT);
        card.add(desc, Theme.gbc(0, 1, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        keyboardModeButton = Theme.primaryButton("键盘 / 音量键遥控");
        touchpadModeButton = Theme.secondaryButton("虚拟触控板");
        pauseButton = Theme.secondaryButton("暂停 / 继续");
        topButton = Theme.secondaryButton("回到顶部");

        keyboardModeButton.addActionListener(e -> openKeyboardMode());
        touchpadModeButton.addActionListener(e -> openTouchpadMode());
        pauseButton.addActionListener(e -> sendPauseToggle());
        topButton.addActionListener(e -> sendTop());

        card.add(keyboardModeButton, Theme.gbc(0, 2, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(touchpadModeButton, Theme.gbc(0, 3, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(pauseButton, Theme.gbc(0, 4, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(topButton, Theme.gbc(1, 4, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        return card;
    }

    private JPanel scriptCard() {
        JPanel card = Theme.card();
        card.add(Theme.label("文稿发送", 18, Font.BOLD, Theme.TEXT), Theme.gbc(0, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(Theme.label("在电脑上新建或粘贴文稿，通过局域网发送到提词端新增文稿。", 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 1, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        scriptButton = Theme.primaryButton("发送文稿到提词器");
        scriptButton.addActionListener(e -> openScriptSender());
        card.add(scriptButton, Theme.gbc(0, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        return card;
    }

    private JPanel settingsCard() {
        JPanel card = Theme.card();
        card.add(Theme.label("控制设置", 18, Font.BOLD, Theme.TEXT), Theme.gbc(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        JLabel sensLabel = Theme.label("滚动灵敏度：" + settings.sensitivity, 14, Font.PLAIN, Theme.TEXT);
        sensitivitySlider = new JSlider(1, 10, settings.sensitivity);
        sensitivitySlider.setMajorTickSpacing(1);
        sensitivitySlider.setPaintTicks(true);
        sensitivitySlider.setBackground(Theme.CARD);
        ChangeListener saveSlider = e -> {
            settings.sensitivity = sensitivitySlider.getValue();
            sensLabel.setText("滚动灵敏度：" + settings.sensitivity);
            settings.save();
        };
        sensitivitySlider.addChangeListener(saveSlider);

        reverseBox = new JCheckBox("方向反转", settings.reverse);
        udpBox = new JCheckBox("UDP 高频控制，默认开启", settings.udpEnabled);
        httpFallbackBox = new JCheckBox("HTTP 兼容模式，默认开启", settings.httpFallback);
        reverseBox.setBackground(Theme.CARD);
        udpBox.setBackground(Theme.CARD);
        httpFallbackBox.setBackground(Theme.CARD);
        reverseBox.setForeground(Theme.TEXT);
        udpBox.setForeground(Theme.TEXT);
        httpFallbackBox.setForeground(Theme.TEXT);

        reverseBox.addActionListener(e -> { settings.reverse = reverseBox.isSelected(); settings.save(); });
        udpBox.addActionListener(e -> { settings.udpEnabled = udpBox.isSelected(); settings.save(); });
        httpFallbackBox.addActionListener(e -> { settings.httpFallback = httpFallbackBox.isSelected(); settings.save(); });

        card.add(sensLabel, Theme.gbc(0, 1, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(sensitivitySlider, Theme.gbc(0, 2, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(reverseBox, Theme.gbc(0, 3, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(udpBox, Theme.gbc(0, 4, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(httpFallbackBox, Theme.gbc(0, 5, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        return card;
    }

    private JComponent protocolHint() {
        JTextArea hint = Theme.textArea("协议：HTTP /api/ping 连接检测；UDP 高频发送 SCROLL dy；HTTP 备用 /api/remote/scroll?dy=；文稿使用 JSON POST /api/remote/scripts/add，失败后尝试 text/plain 备用接口。全部走局域网，不使用公网服务。");
        hint.setEditable(false);
        hint.setFocusable(false);
        hint.setBackground(Theme.BG);
        hint.setForeground(Theme.SUB_TEXT);
        return hint;
    }

    private void connect() {
        String ip = ipField.getText() == null ? "" : ipField.getText().trim();
        String portText = portField.getText() == null ? "" : portField.getText().trim();
        if (ip.isEmpty() || portText.isEmpty()) {
            setStatus("请输入 IP 和端口", Theme.DANGER);
            return;
        }
        if (!RemoteClient.isValidIpOrHost(ip)) {
            setStatus("IP / 主机名格式不正确", Theme.DANGER);
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (Exception e) {
            setStatus("端口号不正确", Theme.DANGER);
            return;
        }

        settings.ip = ip;
        settings.port = port;
        settings.save();
        client = new RemoteClient(ip, port);
        connectButton.setEnabled(false);
        setStatus("连接中……", Theme.SUB_TEXT);

        new SwingWorker<RemoteClient.PingResult, Void>() {
            @Override protected RemoteClient.PingResult doInBackground() {
                return client.ping();
            }

            @Override protected void done() {
                connectButton.setEnabled(true);
                try {
                    handlePingResult(get(), true);
                } catch (Exception e) {
                    connected = false;
                    scriptUploadSupported = false;
                    setStatus("连接失败：" + RemoteClient.humanError(e), Theme.DANGER);
                    refreshButtonState();
                }
            }
        }.execute();
    }

    private void handlePingResult(RemoteClient.PingResult result, boolean fromConnect) {
        connected = result.ok;
        if (result.ok) {
            scriptUploadSupported = result.scriptUpload;
            setStatus("已连接：" + client.endpoint(), Theme.SUCCESS);
            uploadSupportLabel.setForeground(scriptUploadSupported ? Theme.ACCENT : Theme.SUB_TEXT);
            uploadSupportLabel.setText(scriptUploadSupported ? "支持远程发送文稿" : "当前提词端版本不支持远程文稿");
            if (fromConnect) startPingTimer();
        } else {
            setStatus("连接失败：" + result.message, Theme.DANGER);
            uploadSupportLabel.setForeground(Theme.SUB_TEXT);
            uploadSupportLabel.setText("当前未连接");
        }
        refreshButtonState();
    }

    private void startPingTimer() {
        if (pingTimer != null) pingTimer.stop();
        pingTimer = new Timer(4000, e -> {
            if (client == null || !pinging.compareAndSet(false, true)) return;
            new SwingWorker<RemoteClient.PingResult, Void>() {
                @Override protected RemoteClient.PingResult doInBackground() {
                    return client.ping();
                }
                @Override protected void done() {
                    pinging.set(false);
                    try {
                        RemoteClient.PingResult r = get();
                        if (r.ok) {
                            connected = true;
                            scriptUploadSupported = r.scriptUpload;
                            setStatus("已连接：" + client.endpoint(), Theme.SUCCESS);
                            uploadSupportLabel.setForeground(scriptUploadSupported ? Theme.ACCENT : Theme.SUB_TEXT);
                            uploadSupportLabel.setText(scriptUploadSupported ? "支持远程发送文稿" : "当前提词端版本不支持远程文稿");
                        } else {
                            connected = false;
                            setStatus("已断开：" + r.message, Theme.DANGER);
                        }
                        refreshButtonState();
                    } catch (Exception ignored) {
                        connected = false;
                        setStatus("已断开", Theme.DANGER);
                        refreshButtonState();
                    }
                }
            }.execute();
        });
        pingTimer.start();
    }

    private void refreshButtonState() {
        boolean c = connected && client != null;
        keyboardModeButton.setEnabled(c);
        touchpadModeButton.setEnabled(c);
        pauseButton.setEnabled(c);
        topButton.setEnabled(c);
        scriptButton.setEnabled(c && scriptUploadSupported);
        if (!c) {
            scriptButton.setText("发送文稿到提词器");
        } else if (scriptUploadSupported) {
            scriptButton.setText("发送文稿到提词器");
        } else {
            scriptButton.setText("当前提词端不支持远程文稿");
        }
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private void ensureConnectedThen(Runnable r) {
        if (!connected || client == null) {
            setStatus("未连接，请先连接提词器", Theme.DANGER);
            return;
        }
        r.run();
    }

    private void openKeyboardMode() {
        ensureConnectedThen(() -> new KeyboardRemoteWindow(this, client, settings, this::showSendError).setVisible(true));
    }

    private void openTouchpadMode() {
        ensureConnectedThen(() -> new TouchpadRemoteWindow(this, client, settings, this::showSendError).setVisible(true));
    }

    private void openScriptSender() {
        ensureConnectedThen(() -> {
            if (!scriptUploadSupported) {
                JOptionPane.showMessageDialog(this, "当前提词端版本不支持远程文稿", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            new ScriptSendFrame(this, client).setVisible(true);
        });
    }

    private void sendPauseToggle() {
        ensureConnectedThen(() -> {
            boolean newPaused = !paused;
            pauseButton.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    client.pause(newPaused, settings.udpEnabled, settings.httpFallback);
                    return null;
                }
                @Override protected void done() {
                    pauseButton.setEnabled(true);
                    try {
                        get();
                        paused = newPaused;
                        setStatus(paused ? "已发送：暂停" : "已发送：继续", Theme.SUCCESS);
                    } catch (Exception e) {
                        showSendError(RemoteClient.humanError(e));
                    }
                }
            }.execute();
        });
    }

    private void sendTop() {
        ensureConnectedThen(() -> {
            topButton.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    client.top(settings.udpEnabled, settings.httpFallback);
                    return null;
                }
                @Override protected void done() {
                    topButton.setEnabled(true);
                    try {
                        get();
                        setStatus("已发送：回到顶部", Theme.SUCCESS);
                    } catch (Exception e) {
                        showSendError(RemoteClient.humanError(e));
                    }
                }
            }.execute();
        });
    }

    private void showSendError(String msg) {
        setStatus("发送失败，请检查提词器端是否开启遥控服务：" + msg, Theme.DANGER);
    }
}
