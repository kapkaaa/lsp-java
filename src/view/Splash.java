package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

public class Splash extends JWindow {

    private JProgressBar progressBar;
    private JLabel statusLabel;
    private Timer timer;
    private int progress = 0;

    public Splash() {
        initComponents();
        setLocationRelativeTo(null);
        startSplashScreen();
    }

    private void initComponents() {
        // Warna tema sesuai LoginForm
        Color bgColor = Color.decode("#b3ebf2");  // biru pastel
        Color cardColor = Color.decode("#ffffff"); // putih
        Color textMain = Color.decode("#000000");  // hitam
        Color textSub = new Color(80, 80, 80);     // abu lembut
        Color accent = Color.decode("#3fc1d3");    // biru toska lembut

        setSize(400, 300);

        // Panel utama dengan gradient lembut (biru pastel dominan di atas)
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient dari biru pastel ke putih (biru di atas lebih dominan)
                GradientPaint gradient = new GradientPaint(
                    0, 0, bgColor,       // atas: biru pastel
                    0, getHeight() * 1.5f, cardColor  // bawah: putih
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Border lembut putih transparan
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 20, 20));
            }
        };

        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel tengah untuk logo dan teks
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel logoLabel = createLogoLabel();
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel appNameLabel = new JLabel("DISTROZONE");
        appNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        appNameLabel.setForeground(textMain);
        appNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel taglineLabel = new JLabel("Layanan Laundry Gacor");
        taglineLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        taglineLabel.setForeground(textSub);
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Loading...");
        progressBar.setForeground(accent);
        progressBar.setBackground(cardColor);
        progressBar.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));

        // Status
        statusLabel = new JLabel("Memuat aplikasi...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(textSub);

        // Versi
        JLabel versionLabel = new JLabel("v1.0.0", SwingConstants.CENTER);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        versionLabel.setForeground(new Color(120, 120, 120));

        // Tambahkan komponen
        centerPanel.add(logoLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        centerPanel.add(appNameLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        centerPanel.add(taglineLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 70)));

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setOpaque(false);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));
        progressPanel.add(progressBar, BorderLayout.CENTER);

        bottomPanel.add(progressPanel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        bottomPanel.add(statusLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        bottomPanel.add(versionLabel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);

        setShape(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
    }

    private JLabel createLogoLabel() {
        JLabel logoLabel = new JLabel();
        ImageIcon originalIcon = new ImageIcon("src/images/Logo.jpg");

        Image img = originalIcon.getImage();
        Image scaledImg = img.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImg);

        logoLabel.setIcon(scaledIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setPreferredSize(new Dimension(120, 120));
        return logoLabel;
    }

    private void startSplashScreen() {
        String[] loadingSteps = {
            "Memuat aplikasi...",
            "Menginisialisasi komponen...",
            "Memuat data...",
            "Menyiapkan antarmuka...",
            "Hampir selesai...",
            "Selesai!"
        };

        timer = new Timer(500, new ActionListener() {
            private int stepIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                progress += 20;
                progressBar.setValue(progress);

                if (stepIndex < loadingSteps.length) {
                    statusLabel.setText(loadingSteps[stepIndex]);
                    stepIndex++;
                }

                if (progress >= 100) {
                    timer.stop();
                    Timer closeTimer = new Timer(800, evt -> closeSplashScreen());
                    closeTimer.setRepeats(false);
                    closeTimer.start();
                }
            }
        });

        timer.start();
    }

    private void closeSplashScreen() {
        setVisible(false);
        dispose();

        SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}
