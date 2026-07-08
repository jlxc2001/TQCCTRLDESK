package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

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
    static final Color DISABLED_BG = new Color(0x2A3038);
    static final Color DISABLED_TEXT = new Color(0xD0D6DE);

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
        UIManager.put("Button.disabledText", new ColorUIResource(DISABLED_TEXT));
    }

    static Image loadAppIcon() {
        try {
            URL url = Theme.class.getResource("/icons/app.png");
            if (url != null) return new ImageIcon(url).getImage();
        } catch (Exception ignored) {}
        BufferedImage fallback = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fallback.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ACCENT);
        g.fillRoundRect(4, 4, 56, 56, 18, 18);
        g.setColor(Color.WHITE);
        g.setFont(boldFont(18));
        g.drawString("JL", 19, 38);
        g.dispose();
        return fallback;
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
        return new UnifiedButton(text, true);
    }

    static JButton secondaryButton(String text) {
        return new UnifiedButton(text, false);
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

    static JTextArea helpText(String text, float size) {
        JTextArea a = new JTextArea(text);
        a.setFont(normalFont(size));
        a.setForeground(SUB_TEXT);
        a.setBackground(CARD);
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(new EmptyBorder(0, 0, 0, 0));
        return a;
    }

    static JPanel buttonRow(int columns, JButton... buttons) {
        JPanel row = new JPanel(new GridLayout(1, Math.max(1, columns), 12, 12));
        row.setOpaque(false);
        for (JButton b : buttons) row.add(b);
        return row;
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
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return sp;
    }

    private static final class UnifiedButton extends JButton {
        private final boolean primary;

        UnifiedButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(boldFont(primary ? 15 : 14));
            setForeground(primary ? Color.BLACK : TEXT);
            setBackground(primary ? ACCENT : CARD_2);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(new EmptyBorder(12, 16, 12, 16));
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setMinimumSize(new Dimension(120, 46));
            setPreferredSize(new Dimension(160, 48));
        }

        @Override public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            Color bg;
            Color fg;
            if (!isEnabled()) {
                bg = DISABLED_BG;
                fg = DISABLED_TEXT;
            } else if (primary) {
                bg = getModel().isPressed() ? ACCENT_DARK : ACCENT;
                fg = Color.BLACK;
            } else {
                bg = getModel().isPressed() ? new Color(0x303741) : CARD_2;
                fg = TEXT;
            }
            if (isEnabled() && getModel().isRollover()) bg = lighten(bg, primary ? 10 : 8);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w, h, 14, 14);
            if (primary) {
                g2.setColor(new Color(0x4DDAD0));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            } else {
                g2.setColor(new Color(0x303844));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            }
            g2.dispose();
            setForeground(fg);
            super.paintComponent(g);
        }

        private static Color lighten(Color c, int amount) {
            return new Color(
                    Math.min(255, c.getRed() + amount),
                    Math.min(255, c.getGreen() + amount),
                    Math.min(255, c.getBlue() + amount),
                    c.getAlpha()
            );
        }
    }
}
