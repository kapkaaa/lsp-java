/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoadingDialog extends JDialog {
    private int angle = 0;
    private Timer timer;
    private int progress = 0;
    private boolean isDone = false;
    private final Color BLUE = Color.decode("#2196f3"); // Material Blue 500
    private final Color WHITE = Color.WHITE;

    public LoadingDialog(Frame parent) {
        super(parent, true);
        setUndecorated(true);
        setBackground(BLUE); // Set background dialog ke biru

        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Isi latar belakang panel dengan warna biru
                g2d.setColor(BLUE);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Pusat spinner
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2 - 20;

                // Gambar spinner (lingkaran kecil berputar) — warna PUTIH
                for (int i = 0; i < 8; i++) {
                    double rad = Math.toRadians(angle - i * 45);
                    int x = centerX + (int)(Math.cos(rad) * 30);
                    int y = centerY + (int)(Math.sin(rad) * 30);
                    float alpha = (float) (1.0 - i * 0.12);
                    g2d.setColor(new Color(WHITE.getRed(), WHITE.getGreen(), WHITE.getBlue(), (int)(alpha * 255)));
                    g2d.fillOval(x - 6, y - 6, 12, 12);
                }

                // Teks "Loading......" — tetap putih
                g2d.setColor(WHITE);
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                String text = "Loading......";
                int textWidth = g2d.getFontMetrics().stringWidth(text);
                g2d.drawString(text, (getWidth() - textWidth) / 2, centerY + 50);

                // Progress bar
                int barWidth = getWidth() - 80;
                int barHeight = 8;
                int barX = (getWidth() - barWidth) / 2;
                int barY = centerY + 70;

                // Background progress bar: abu-abu transparan di atas biru
                g2d.setColor(new Color(255, 255, 255, 60)); // Abu-abu terang transparan
                g2d.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);

                // Isi progress bar: PUTIH
                int filledWidth = (int)(barWidth * (progress / 100.0));
                g2d.setColor(WHITE);
                g2d.fillRoundRect(barX, barY, filledWidth, barHeight, 4, 4);

                g2d.dispose();
            }
        };
        contentPanel.setOpaque(false);

        add(contentPanel);
        setSize(320, 180);
        setLocationRelativeTo(parent);

        // Animasi spinner
        timer = new Timer(120, e -> {
            angle += 45;
            if (angle >= 360) angle = 0;
            contentPanel.repaint();
        });
        timer.start();
    }

    public void setProgress(int p) {
        this.progress = Math.min(100, Math.max(0, p));
        if (p >= 100) isDone = true;
        repaint();
    }

    public void close() {
        if (timer != null) timer.stop();
        dispose();
    }
}