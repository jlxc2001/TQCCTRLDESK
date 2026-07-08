package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

final class ScriptManageFrame extends JFrame {
    private final Window owner;
    private final RemoteClient client;
    private final boolean scriptManageSupported;
    private final boolean remotePromptSupported;

    private JLabel promptStatusLabel;
    private JLabel statusLabel;
    private JPanel listPanel;
    private JButton refreshButton;
    private JButton newButton;
    private JButton stopPromptButton;

    ScriptManageFrame(Window owner, RemoteClient client, boolean scriptManageSupported, boolean remotePromptSupported) {
        super("文稿管理");
        this.owner = owner;
        this.client = client;
        this.scriptManageSupported = scriptManageSupported;
        this.remotePromptSupported = remotePromptSupported;
        setIconImage(Theme.loadAppIcon());
        setMinimumSize(new Dimension(760, 680));
        setSize(920, 820);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildUi();
        SwingUtilities.invokeLater(this::refreshAll);
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        root.add(Theme.label("文稿管理", 28, Font.BOLD, Theme.TEXT), Theme.gbc(0, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        root.add(Theme.label("目标：" + client.endpoint(), 13, Font.PLAIN, Theme.SUB_TEXT), Theme.gbc(0, 1, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        JPanel toolbar = Theme.card();
        promptStatusLabel = Theme.label("当前提词状态：读取中……", 14, Font.BOLD, Theme.SUB_TEXT);
        statusLabel = Theme.label("未刷新", 13, Font.BOLD, Theme.MUTED);
        refreshButton = Theme.secondaryButton("刷新列表");
        newButton = Theme.primaryButton("新增文稿");
        stopPromptButton = Theme.secondaryButton("关闭提词");

        refreshButton.addActionListener(e -> refreshAll());
        newButton.addActionListener(e -> openNewScript());
        stopPromptButton.addActionListener(e -> stopPrompt());

        toolbar.add(promptStatusLabel, Theme.gbc(0, 0, 3, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        toolbar.add(statusLabel, Theme.gbc(0, 1, 3, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        toolbar.add(refreshButton, Theme.gbc(0, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        toolbar.add(newButton, Theme.gbc(1, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        toolbar.add(stopPromptButton, Theme.gbc(2, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        root.add(toolbar, Theme.gbc(0, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        listPanel = new JPanel(new GridBagLayout());
        listPanel.setBackground(Theme.BG);
        JScrollPane listScroll = Theme.scroll(listPanel);
        listScroll.setPreferredSize(new Dimension(780, 520));
        root.add(listScroll, Theme.gbc(0, 3, 1, 1, 1, 1, GridBagConstraints.BOTH));

        JTextArea hint = Theme.helpText("支持：查看列表、获取全文、编辑、删除、开始提词、关闭提词。旧版提词端如果没有 scriptManage / remotePrompt 字段，会自动禁用对应功能。", 13);
        root.add(hint, Theme.gbc(0, 4, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        setContentPane(root);
        newButton.setEnabled(scriptManageSupported);
        refreshButton.setEnabled(scriptManageSupported);
        stopPromptButton.setEnabled(remotePromptSupported);
        if (!scriptManageSupported) status("当前提词端版本不支持远程文稿管理", Theme.DANGER);
        if (!remotePromptSupported) promptStatusLabel.setText("当前提词状态：提词端不支持远程开始 / 关闭提词");
    }

    private void refreshAll() {
        if (remotePromptSupported) refreshPromptStatus();
        if (scriptManageSupported) refreshList();
    }

    private void refreshPromptStatus() {
        promptStatusLabel.setText("当前提词状态：读取中……");
        new SwingWorker<RemoteClient.PromptStatusResult, Void>() {
            @Override protected RemoteClient.PromptStatusResult doInBackground() {
                return client.promptStatus();
            }
            @Override protected void done() {
                try {
                    RemoteClient.PromptStatusResult r = get();
                    if (r.ok) {
                        promptStatusLabel.setForeground(r.prompting ? Theme.ACCENT : Theme.SUB_TEXT);
                        promptStatusLabel.setText("当前提词状态：" + r.message);
                    } else {
                        promptStatusLabel.setForeground(Theme.DANGER);
                        promptStatusLabel.setText("当前提词状态：读取失败：" + r.message);
                    }
                } catch (Exception e) {
                    promptStatusLabel.setForeground(Theme.DANGER);
                    promptStatusLabel.setText("当前提词状态：读取失败：" + RemoteClient.humanError(e));
                }
            }
        }.execute();
    }

    private void refreshList() {
        refreshButton.setEnabled(false);
        status("正在读取提词端文稿……", Theme.SUB_TEXT);
        setListMessage("读取中……");
        new SwingWorker<RemoteClient.ListResult, Void>() {
            @Override protected RemoteClient.ListResult doInBackground() {
                return client.listScripts();
            }
            @Override protected void done() {
                refreshButton.setEnabled(true);
                try {
                    RemoteClient.ListResult r = get();
                    if (!r.ok) {
                        status("读取失败：" + r.message, Theme.DANGER);
                        setListMessage("读取失败：" + r.message);
                        return;
                    }
                    status("共 " + r.count + " 篇文稿", Theme.SUCCESS);
                    renderScriptList(r.scripts, r.count);
                } catch (Exception e) {
                    status("读取失败：" + RemoteClient.humanError(e), Theme.DANGER);
                    setListMessage("读取失败：" + RemoteClient.humanError(e));
                }
            }
        }.execute();
    }

    private void renderScriptList(List<RemoteClient.ScriptSummary> scripts, int count) {
        listPanel.removeAll();
        if (scripts == null || scripts.isEmpty()) {
            setListMessage(count > 0 ? "提词端返回了数量，但没有返回文稿摘要。" : "提词端暂无文稿。");
            return;
        }
        int row = 0;
        for (RemoteClient.ScriptSummary s : scripts) {
            listPanel.add(scriptCard(s), Theme.gbc(0, row++, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        }
        listPanel.add(Box.createVerticalGlue(), Theme.gbc(0, row, 1, 1, 1, 1, GridBagConstraints.BOTH));
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel scriptCard(RemoteClient.ScriptSummary s) {
        JPanel card = Theme.card();
        String title = s.title == null || s.title.isBlank() ? "未命名文稿" : s.title;
        JLabel titleLabel = Theme.label(title, 18, Font.BOLD, Theme.TEXT);
        JTextArea meta = Theme.helpText(s.length + " 字" + formatTime(s.updatedAt), 13);

        JButton startButton = Theme.primaryButton("开始提词");
        JButton editButton = Theme.secondaryButton("编辑");
        JButton deleteButton = Theme.secondaryButton("删除");
        startButton.setEnabled(remotePromptSupported && s.id != null && !s.id.isBlank());
        editButton.setEnabled(scriptManageSupported && s.id != null && !s.id.isBlank());
        deleteButton.setEnabled(scriptManageSupported && s.id != null && !s.id.isBlank());

        startButton.addActionListener(e -> startPrompt(s, startButton));
        editButton.addActionListener(e -> editScript(s, editButton));
        deleteButton.addActionListener(e -> deleteScript(s, deleteButton));

        card.add(titleLabel, Theme.gbc(0, 0, 3, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(meta, Theme.gbc(0, 1, 3, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(startButton, Theme.gbc(0, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(editButton, Theme.gbc(1, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        card.add(deleteButton, Theme.gbc(2, 2, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        return card;
    }

    private void openNewScript() {
        if (!scriptManageSupported) {
            status("当前提词端版本不支持新增文稿", Theme.DANGER);
            return;
        }
        new ScriptEditFrame(this, client, null, "", "", this::refreshAll).setVisible(true);
    }

    private void editScript(RemoteClient.ScriptSummary summary, JButton button) {
        button.setEnabled(false);
        status("正在读取全文……", Theme.SUB_TEXT);
        new SwingWorker<RemoteClient.ScriptDetailResult, Void>() {
            @Override protected RemoteClient.ScriptDetailResult doInBackground() {
                return client.getScript(summary.id);
            }
            @Override protected void done() {
                button.setEnabled(true);
                try {
                    RemoteClient.ScriptDetailResult r = get();
                    if (!r.ok) {
                        status("读取全文失败：" + r.message, Theme.DANGER);
                        return;
                    }
                    status("已读取全文：" + r.title, Theme.SUCCESS);
                    new ScriptEditFrame(ScriptManageFrame.this, client, r.id, r.title, r.content, ScriptManageFrame.this::refreshAll).setVisible(true);
                } catch (Exception e) {
                    status("读取全文失败：" + RemoteClient.humanError(e), Theme.DANGER);
                }
            }
        }.execute();
    }

    private void deleteScript(RemoteClient.ScriptSummary summary, JButton button) {
        String title = summary.title == null || summary.title.isBlank() ? "未命名文稿" : summary.title;
        int choice = JOptionPane.showConfirmDialog(this, "确定删除这篇文稿吗？\n\n" + title, "确认删除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        button.setEnabled(false);
        status("正在删除……", Theme.SUB_TEXT);
        new SwingWorker<RemoteClient.BasicResult, Void>() {
            @Override protected RemoteClient.BasicResult doInBackground() {
                return client.deleteScript(summary.id);
            }
            @Override protected void done() {
                button.setEnabled(true);
                try {
                    RemoteClient.BasicResult r = get();
                    if (r.ok) {
                        status(r.message, Theme.SUCCESS);
                        refreshAll();
                    } else {
                        status("删除失败：" + r.message, Theme.DANGER);
                    }
                } catch (Exception e) {
                    status("删除失败：" + RemoteClient.humanError(e), Theme.DANGER);
                }
            }
        }.execute();
    }

    private void startPrompt(RemoteClient.ScriptSummary summary, JButton button) {
        if (!remotePromptSupported) {
            status("当前提词端版本不支持远程开始提词", Theme.DANGER);
            return;
        }
        button.setEnabled(false);
        status("正在发送开始提词……", Theme.SUB_TEXT);
        new SwingWorker<RemoteClient.BasicResult, Void>() {
            @Override protected RemoteClient.BasicResult doInBackground() {
                return client.startPrompt(summary.id);
            }
            @Override protected void done() {
                button.setEnabled(true);
                try {
                    RemoteClient.BasicResult r = get();
                    if (r.ok) {
                        status(r.message + "：" + (summary.title == null ? "未命名文稿" : summary.title), Theme.SUCCESS);
                        refreshPromptStatus();
                    } else {
                        status("开始提词失败：" + r.message, Theme.DANGER);
                    }
                } catch (Exception e) {
                    status("开始提词失败：" + RemoteClient.humanError(e), Theme.DANGER);
                }
            }
        }.execute();
    }

    private void stopPrompt() {
        if (!remotePromptSupported) {
            status("当前提词端版本不支持远程关闭提词", Theme.DANGER);
            return;
        }
        stopPromptButton.setEnabled(false);
        status("正在关闭提词……", Theme.SUB_TEXT);
        new SwingWorker<RemoteClient.BasicResult, Void>() {
            @Override protected RemoteClient.BasicResult doInBackground() {
                return client.stopPrompt();
            }
            @Override protected void done() {
                stopPromptButton.setEnabled(true);
                try {
                    RemoteClient.BasicResult r = get();
                    if (r.ok) {
                        status(r.message, Theme.SUCCESS);
                        refreshPromptStatus();
                    } else {
                        status("关闭提词失败：" + r.message, Theme.DANGER);
                    }
                } catch (Exception e) {
                    status("关闭提词失败：" + RemoteClient.humanError(e), Theme.DANGER);
                }
            }
        }.execute();
    }

    private void setListMessage(String text) {
        listPanel.removeAll();
        JTextArea area = Theme.helpText(text, 15);
        listPanel.add(area, Theme.gbc(0, 0, 1, 1, 1, 1, GridBagConstraints.BOTH));
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void status(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private String formatTime(long time) {
        if (time <= 0) return "";
        return "  |  更新：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));
    }
}
