package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

final class Theme {
    static final Color BG = new Color(0x101214);
    static final Color CARD = new Color(0x1B1F24);
    static final Color CARD_2 = new Color(0x23282F);
    static final Color TEXT = new Color(0xF2F4F6);
    static final Color SUB_TEXT = new Color(0xB8C0CC);
    static final Color MUTED = new Color(0x77808C);
    static final Color ACCENT = new Color(0x39C5BB);
    static final Color ACCENT_DARK = new Color(0x249E98);
    static final Color DANGER = new Color(0xFF6B6B);
    static final Color SUCCESS = new Color(0x6EE7B7);

    private Theme() {}

    static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TextField.background", CARD_2);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextArea.background", CARD_2);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.caretForeground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("CheckBox.background", CARD);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("Slider.background", CARD);
        UIManager.put("Slider.foreground", ACCENT);
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("Viewport.background", BG);
        UIManager.put("Button.focus", new ColorUIResource(new Color(0, 0, 0, 0)));
    }

    static Font titleFont(float size) {
        return new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size));
    }

    static Font normalFont(float size) {
        return new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(size));
    }

    static Font boldFont(float size) {
        return new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size));
    }

    static JPanel card() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    static JLabel label(String text, float size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, style, Math.round(size)));
        label.setForeground(color);
        return label;
    }

    static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(boldFont(15));
        b.setForeground(Color.BLACK);
        b.setBackground(ACCENT);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(12, 18, 12, 18));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(boldFont(14));
        b.setForeground(TEXT);
        b.setBackground(CARD_2);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JTextField textField(String text) {
        JTextField f = new JTextField(text);
        f.setFont(normalFont(16));
        f.setForeground(TEXT);
        f.setBackground(CARD_2);
        f.setCaretColor(TEXT);
        f.setBorder(new EmptyBorder(10, 12, 10, 12));
        return f;
    }

    static JTextArea textArea(String text) {
        JTextArea a = new JTextArea(text);
        a.setFont(normalFont(16));
        a.setForeground(TEXT);
        a.setBackground(CARD_2);
        a.setCaretColor(TEXT);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(new EmptyBorder(12, 12, 12, 12));
        return a;
    }

    static GridBagConstraints gbc(int x, int y, int w, int h, double wx, double wy, int fill) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = w;
        c.gridheight = h;
        c.weightx = wx;
        c.weighty = wy;
        c.fill = fill;
        c.insets = new Insets(6, 6, 6, 6);
        return c;
    }

    static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(BG);
        sp.setBackground(BG);
        return sp;
    }
}
