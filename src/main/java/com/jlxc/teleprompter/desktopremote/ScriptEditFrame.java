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

final class ScriptEditFrame extends JFrame {
    private static final int MAX_SCRIPT_BYTES = 2 * 1024 * 1024;

    private final Window owner;
    private final RemoteClient client;
    private final Runnable onSaved;
    private String scriptId;

    private JTextField titleField;
    private JTextArea contentArea;
    private JLabel countLabel;
    private JLabel statusLabel;
    private JButton saveButton;
    private JButton clearButton;
    private JButton pasteButton;
    private JButton importButton;

    ScriptEditFrame(Window owner, RemoteClient client, String scriptId, String initialTitle, String initialContent, Runnable onSaved) {
        super(scriptId == null || scriptId.isBlank() ? "新增文稿" : "编辑文稿");
        this.owner = owner;
        this.client = client;
        this.scriptId = scriptId;
        this.onSaved = onSaved == null ? () -> {} : onSaved;
        setIconImage(Theme.loadAppIcon());
        setMinimumSize(new Dimension(620, 720));
        setSize(780, 840);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildUi(initialTitle == null ? "" : initialTitle, initialContent == null ? "" : initialContent);
    }

    private void buildUi(String title, String content) {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        boolean edit = scriptId != null && !scriptId.isBlank();
        root.add(Theme.label(edit ? "编辑文稿" : "新增文稿", 26, Font.BOLD, Theme.TEXT), Theme.gbc(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(Theme.label("目标：" + client.endpoint(), 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 1, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        JPanel card = Theme.card();
        titleField = Theme.textField(title);
        titleField.setToolTipText("文稿标题，可留空");
        contentArea = Theme.textArea(content);
        contentArea.setToolTipText("粘贴提词内容");
        countLabel = Theme.label("当前 0 字", 13, Font.BOLD, Theme.SUB_TEXT);
        statusLabel = Theme.label(edit ? "未保存" : "未发送", 14, Font.BOLD, Theme.MUTED);

        contentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateCount(); }
            @Override public void removeUpdate(DocumentEvent e) { updateCount(); }
            @Override public void changedUpdate(DocumentEvent e) { updateCount(); }
        });

        card.add(Theme.label("标题", 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 0, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(titleField, Theme.gbc(0, 1, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(Theme.label("正文", 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 2, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        JScrollPane contentScroll = Theme.scroll(contentArea);
        contentScroll.setPreferredSize(new Dimension(560, 380));
        card.add(contentScroll, Theme.gbc(0, 3, 2, 1, 1, 1, GridBagConstraints.BOTH));
        card.add(countLabel, Theme.gbc(0, 4, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        pasteButton = Theme.secondaryButton("从剪贴板粘贴");
        importButton = Theme.secondaryButton("从 txt 文件导入");
        saveButton = Theme.primaryButton(edit ? "保存到提词器" : "发送到提词器");
        clearButton = Theme.secondaryButton("清空");

        pasteButton.addActionListener(e -> pasteClipboard());
        importButton.addActionListener(e -> importTxtFile());
        saveButton.addActionListener(e -> saveScript());
        clearButton.addActionListener(e -> clearContent());

        card.add(Theme.buttonRow(2, pasteButton, importButton), Theme.gbc(0, 5, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(saveButton, Theme.gbc(0, 6, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(clearButton, Theme.gbc(0, 7, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(statusLabel, Theme.gbc(0, 8, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        root.add(card, Theme.gbc(0, 2, 2, 1, 1, 1, GridBagConstraints.BOTH));

        JTextArea hint = Theme.helpText("新增使用 JSON POST /api/remote/scripts/add；编辑使用 JSON POST /api/remote/scripts/update?id=。正文超过 2MB 会被拦截。", 13);
        root.add(hint, Theme.gbc(0, 3, 2, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        setContentPane(root);
        updateCount();
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

    private void saveScript() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String content = contentArea.getText() == null ? "" : contentArea.getText();
        if (content.trim().isEmpty()) {
            status("正文为空，不能保存", Theme.DANGER);
            return;
        }
        int bytes = content.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_SCRIPT_BYTES) {
            status("文稿太大，单篇不要超过 2MB", Theme.DANGER);
            return;
        }
        setLoading(true);
        status(scriptId == null || scriptId.isBlank() ? "发送中……" : "保存中……", Theme.SUB_TEXT);
        new SwingWorker<RemoteClient.UploadResult, Void>() {
            @Override protected RemoteClient.UploadResult doInBackground() {
                if (scriptId == null || scriptId.isBlank()) return client.uploadScript(title, content);
                return client.updateScript(scriptId, title, content);
            }
            @Override protected void done() {
                setLoading(false);
                try {
                    RemoteClient.UploadResult r = get();
                    if (r.ok) {
                        if (scriptId == null || scriptId.isBlank()) scriptId = r.id;
                        status(r.message, Theme.SUCCESS);
                        onSaved.run();
                    } else {
                        status(r.message == null ? "保存失败" : r.message, Theme.DANGER);
                        if (r.detail != null) showDetail(r.detail);
                    }
                } catch (Exception e) {
                    status("保存失败：" + RemoteClient.humanError(e), Theme.DANGER);
                }
            }
        }.execute();
    }

    private void clearContent() {
        titleField.setText("");
        contentArea.setText("");
        status("已清空", Theme.SUB_TEXT);
    }

    private void showDetail(String detail) {
        JTextArea area = Theme.textArea(detail);
        area.setEditable(false);
        JScrollPane sp = Theme.scroll(area);
        sp.setPreferredSize(new Dimension(520, 220));
        JOptionPane.showMessageDialog(this, sp, "失败详情", JOptionPane.ERROR_MESSAGE);
    }

    private void setLoading(boolean loading) {
        saveButton.setEnabled(!loading);
        saveButton.setText(loading ? "处理中……" : (scriptId == null || scriptId.isBlank() ? "发送到提词器" : "保存到提词器"));
        clearButton.setEnabled(!loading);
        pasteButton.setEnabled(!loading);
        importButton.setEnabled(!loading);
    }

    private void status(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }
}
