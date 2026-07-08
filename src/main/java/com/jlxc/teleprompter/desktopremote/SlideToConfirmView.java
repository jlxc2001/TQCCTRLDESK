package com.jlxc.teleprompter.desktopremote;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class SlideToConfirmView extends JPanel {
    enum Direction { RIGHT, LEFT }

    private final String text;
    private final Direction direction;
    private final Runnable action;
    private int knobSize = 46;
    private int knobX = 4;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private boolean triggered = false;

    SlideToConfirmView(String text, Direction direction, Runnable action) {
        this.text = text;
        this.direction = direction;
        this.action = action;
        setOpaque(false);
        setPreferredSize(new Dimension(220, 58));
        setMinimumSize(new Dimension(160, 54));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int kx = getKnobX();
                if (e.getX() >= kx && e.getX() <= kx + knobSize) {
                    dragging = true;
                    triggered = false;
                    dragOffsetX = e.getX() - kx;
                }
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (!dragging) return;
                int min = 4;
                int max = getWidth() - knobSize - 4;
                knobX = Math.max(min, Math.min(max, e.getX() - dragOffsetX));
                repaint();
                double progress = direction == Direction.RIGHT
                        ? (knobX - min) / (double) Math.max(1, max - min)
                        : (max - knobX) / (double) Math.max(1, max - min);
                if (progress >= 0.88 && !triggered) {
                    triggered = true;
                    if (action != null) SwingUtilities.invokeLater(action);
                    resetLater();
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                dragging = false;
                if (!triggered) resetLater();
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private int getKnobX() {
        int min = 4;
        int max = Math.max(min, getWidth() - knobSize - 4);
        if (!dragging && !triggered) {
            knobX = direction == Direction.RIGHT ? min : max;
        }
        return Math.max(min, Math.min(max, knobX));
    }

    private void resetLater() {
        Timer t = new Timer(160, e -> {
            dragging = false;
            triggered = false;
            knobX = direction == Direction.RIGHT ? 4 : Math.max(4, getWidth() - knobSize - 4);
            repaint();
        });
        t.setRepeats(false);
        t.start();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int arc = Math.min(28, h - 10);
        g2.setColor(Theme.CARD_2);
        g2.fillRoundRect(0, 4, w, h - 8, arc, arc);

        int min = 4;
        int max = Math.max(min, w - knobSize - 4);
        int kx = getKnobX();
        double progress = direction == Direction.RIGHT
                ? (kx - min) / (double) Math.max(1, max - min)
                : (max - kx) / (double) Math.max(1, max - min);
        int fillW = (int) ((w - 8) * progress);
        g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 70));
        if (direction == Direction.RIGHT) {
            g2.fillRoundRect(4, 8, fillW, h - 16, arc, arc);
        } else {
            g2.fillRoundRect(w - 4 - fillW, 8, fillW, h - 16, arc, arc);
        }

        g2.setFont(Theme.boldFont(13));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Theme.SUB_TEXT);
        int tx = (w - fm.stringWidth(text)) / 2;
        int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, Math.max(8, tx), ty);

        g2.setColor(Theme.ACCENT);
        g2.fillRoundRect(kx, (h - knobSize) / 2, knobSize, knobSize, knobSize, knobSize);
        g2.setColor(Color.BLACK);
        g2.setFont(Theme.boldFont(22));
        String arrow = direction == Direction.RIGHT ? "›" : "‹";
        FontMetrics afm = g2.getFontMetrics();
        g2.drawString(arrow, kx + (knobSize - afm.stringWidth(arrow)) / 2, (h + afm.getAscent() - afm.getDescent()) / 2);
        g2.dispose();
    }
}
