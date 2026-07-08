package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

final class ScriptSendFrame extends JFrame {
    private static final int MAX_SCRIPT_BYTES = 2 * 1024 * 1024;

    private final Window owner;
    private final RemoteClient client;
    private JTextField titleField;
    private JTextArea contentArea;
    private JLabel countLabel;
    private JLabel statusLabel;
    private JButton sendButton;
    private JButton clearButton;
    private JButton pasteButton;
    private JButton importButton;
    private JButton listButton;

    ScriptSendFrame(Window owner, RemoteClient client) {
        super("发送文稿到提词器");
        this.owner = owner;
        this.client = client;
        setMinimumSize(new Dimension(560, 720));
        setSize(720, 820);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildUi();
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        root.add(Theme.label("发送文稿到提词器", 26, Font.BOLD, Theme.TEXT), Theme.gbc(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(Theme.label("目标：" + client.endpoint(), 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 1, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        JPanel card = Theme.card();
        titleField = Theme.textField("");
        titleField.setToolTipText("文稿标题，可留空");
        contentArea = Theme.textArea("");
        contentArea.setToolTipText("粘贴提词内容");
        countLabel = Theme.label("当前 0 字", 13, Font.BOLD, Theme.SUB_TEXT);
        statusLabel = Theme.label("未发送", 14, Font.BOLD, Theme.MUTED);

        contentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateCount(); }
            @Override public void removeUpdate(DocumentEvent e) { updateCount(); }
            @Override public void changedUpdate(DocumentEvent e) { updateCount(); }
        });

        card.add(Theme.label("标题", 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(titleField, Theme.gbc(0, 1, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(Theme.label("正文", 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 2, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        JScrollPane contentScroll = Theme.scroll(contentArea);
        contentScroll.setPreferredSize(new Dimension(480, 360));
        card.add(contentScroll, Theme.gbc(0, 3, 2, 1, 1, 1, GridBagConstraints.BOTH));
        card.add(countLabel, Theme.gbc(0, 4, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        pasteButton = Theme.secondaryButton("从剪贴板粘贴");
        importButton = Theme.secondaryButton("从 txt 文件导入");
        sendButton = Theme.primaryButton("发送到提词器");
        clearButton = Theme.secondaryButton("清空");
        listButton = Theme.secondaryButton("查看提词端文稿");

        pasteButton.addActionListener(e -> pasteClipboard());
        importButton.addActionListener(e -> importTxtFile());
        sendButton.addActionListener(e -> sendScript());
        clearButton.addActionListener(e -> clearContent());
        listButton.addActionListener(e -> listScripts());

        JPanel row1 = new JPanel(new GridLayout(1, 2, 12, 12));
        row1.setOpaque(false);
        row1.add(pasteButton);
        row1.add(importButton);
        JPanel row2 = new JPanel(new GridLayout(1, 1, 12, 12));
        row2.setOpaque(false);
        row2.add(sendButton);
        JPanel row3 = new JPanel(new GridLayout(1, 2, 12, 12));
        row3.setOpaque(false);
        row3.add(clearButton);
        row3.add(listButton);

        card.add(row1, Theme.gbc(0, 5, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(row2, Theme.gbc(0, 6, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(row3, Theme.gbc(0, 7, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(statusLabel, Theme.gbc(0, 8, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        root.add(card, Theme.gbc(0, 2, 2, 1, 1, 1, GridBagConstraints.BOTH));

        JTextArea hint = Theme.textArea("发送协议：优先 JSON POST /api/remote/scripts/add。如果 JSON 请求失败，会自动尝试 text/plain 备用接口。正文超过 2MB 会被拦截，避免提词端内存压力过大。");
        hint.setEditable(false);
        hint.setFocusable(false);
        hint.setBackground(Theme.BG);
        hint.setForeground(Theme.SUB_TEXT);
        root.add(hint, Theme.gbc(0, 3, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        setContentPane(root);
    }

    private void updateCount() {
        String text = contentArea.getText();
        int chars = text == null ? 0 : text.length();
        int bytes = text == null ? 0 : text.getBytes(StandardCharsets.UTF_8).length;
        countLabel.setText("当前 " + chars + " 字 / " + bytes + " 字节");
        countLabel.setForeground(bytes > MAX_SCRIPT_BYTES ? Theme.DANGER : Theme.SUB_TEXT);
    }

    private void pasteClipboard() {
        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data instanceof String text && !text.isEmpty()) {
                contentArea.replaceSelection(text);
                status("已从剪贴板粘贴", Theme.SUCCESS);
            } else {
                status("剪贴板没有文本", Theme.DANGER);
            }
        } catch (Exception e) {
            status("读取剪贴板失败：" + RemoteClient.humanError(e), Theme.DANGER);
        }
    }

    private void importTxtFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择 txt 文稿");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        try {
            long size = Files.size(path);
            if (size > MAX_SCRIPT_BYTES) {
                status("文稿太大，单篇不要超过 2MB", Theme.DANGER);
                return;
            }
            String text = readTextFile(path);
            contentArea.setText(text);
            if (titleField.getText().trim().isEmpty()) {
                String name = chooser.getSelectedFile().getName();
                int dot = name.lastIndexOf('.');
                titleField.setText(dot > 0 ? name.substring(0, dot) : name);
            }
            status("已导入：" + chooser.getSelectedFile().getName(), Theme.SUCCESS);
        } catch (Exception e) {
            status("导入失败：" + RemoteClient.humanError(e), Theme.DANGER);
        }
    }

    private String readTextFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    private void sendScript() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String content = contentArea.getText() == null ? "" : contentArea.getText();
        if (content.trim().isEmpty()) {
            status("正文为空，不能发送", Theme.DANGER);
            return;
        }
        int bytes = content.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_SCRIPT_BYTES) {
            status("文稿太大，单篇不要超过 2MB", Theme.DANGER);
            return;
        }
        setLoading(true);
        status("发送中……", Theme.SUB_TEXT);
        new SwingWorker<RemoteClient.UploadResult, Void>() {
            @Override protected RemoteClient.UploadResult doInBackground() {
                return client.uploadScript(title, content);
            }
            @Override protected void done() {
                setLoading(false);
                try {
                    RemoteClient.UploadResult r = get();
                    if (r.ok) {
                        status(r.message, Theme.SUCCESS);
                    } else {
                        status(r.message == null ? "发送失败" : r.message, Theme.DANGER);
                        if (r.detail != null) showDetail(r.detail);
                    }
                } catch (Exception e) {
                    status("发送失败：" + RemoteClient.humanError(e), Theme.DANGER);
                }
            }
        }.execute();
    }

    private void clearContent() {
        titleField.setText("");
        contentArea.setText("");
        status("已清空", Theme.SUB_TEXT);
    }

    private void listScripts() {
        listButton.setEnabled(false);
        status("正在读取提词端文稿……", Theme.SUB_TEXT);
        new SwingWorker<RemoteClient.ListResult, Void>() {
            @Override protected RemoteClient.ListResult doInBackground() {
                return client.listScripts();
            }
            @Override protected void done() {
                listButton.setEnabled(true);
                try {
                    RemoteClient.ListResult r = get();
                    if (!r.ok) {
                        status("读取失败：" + r.message, Theme.DANGER);
                        return;
                    }
                    status("共 " + r.count + " 篇文稿", Theme.SUCCESS);
                    showScriptList(r.scripts, r.count);
                } catch (Exception e) {
                    status("读取失败：" + RemoteClient.humanError(e), Theme.DANGER);
                }
            }
        }.execute();
    }

    private void showScriptList(List<RemoteClient.ScriptSummary> scripts, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("提词端文稿：").append(count).append(" 篇\n\n");
        if (scripts.isEmpty()) {
            sb.append("没有返回文稿摘要，或提词端列表为空。");
        } else {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < scripts.size(); i++) {
                RemoteClient.ScriptSummary s = scripts.get(i);
                sb.append(i + 1).append(". ").append(s.title).append(" | ").append(s.length).append(" 字");
                if (s.updatedAt > 0) sb.append(" | 更新：").append(fmt.format(new Date(s.updatedAt)));
                sb.append('\n');
            }
        }
        JTextArea area = Theme.textArea(sb.toString());
        area.setEditable(false);
        area.setCaretPosition(0);
        JScrollPane sp = Theme.scroll(area);
        sp.setPreferredSize(new Dimension(520, 420));
        JOptionPane.showMessageDialog(this, sp, "提词端文稿", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showDetail(String detail) {
        JTextArea area = Theme.textArea(detail);
        area.setEditable(false);
        JScrollPane sp = Theme.scroll(area);
        sp.setPreferredSize(new Dimension(520, 220));
        JOptionPane.showMessageDialog(this, sp, "失败详情", JOptionPane.ERROR_MESSAGE);
    }

    private void setLoading(boolean loading) {
        sendButton.setEnabled(!loading);
        sendButton.setText(loading ? "发送中……" : "发送到提词器");
        clearButton.setEnabled(!loading);
        pasteButton.setEnabled(!loading);
        importButton.setEnabled(!loading);
    }

    private void status(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }
}
