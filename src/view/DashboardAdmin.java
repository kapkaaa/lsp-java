package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import utils.*;

public class DashboardAdmin extends JFrame {
    private JLabel lblWelcome, lblDateTime;
    private JPanel contentPanel;
    private Timer clockTimer;
    private Point mousePoint;
    private boolean isMaximized = false;
    private Rectangle normalBounds;

    public DashboardAdmin() {
        setUndecorated(true);
        initComponents();
        startClock();
        setLocationRelativeTo(null);
        updateWindowShape();
    }

    private void initComponents() {
        Color bgColor = Color.decode("#b3ebf2");
        Color textMain = Color.decode("#222222");
        Color sidebarBg = Color.decode("#2c3e50");
        Color accent = Color.decode("#3fc1d3");

        setSize(1024, 768);
        setBackground(new Color(0, 0, 0, 0));
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // =================== PANEL UTAMA DENGAN ROUNDED CORNERS ===================
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false);

        // =================== macOS TITLE BAR ===================
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        titleBar.setPreferredSize(new Dimension(1024, 40));
        titleBar.setOpaque(false);

        JButton btnClose = createMacOSButton(new Color(0xFF5F57));
        JButton btnMinimize = createMacOSButton(new Color(0xFFBD2E));
        JButton btnMaximize = createMacOSButton(new Color(0x28CA42));

        btnClose.addActionListener(e -> System.exit(0));
        btnMinimize.addActionListener(e -> setState(JFrame.ICONIFIED));
        btnMaximize.addActionListener(e -> toggleMaximize());

        titleBar.add(btnClose);
        titleBar.add(btnMinimize);
        titleBar.add(btnMaximize);

        JLabel titleLabel = new JLabel("Dashboard Admin - DistroZone", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI  Emoji", Font.BOLD, 14));
        titleLabel.setForeground(textMain);
        titleLabel.setOpaque(false);
        titleBar.add(Box.createHorizontalGlue());
        titleBar.add(titleLabel);
        titleBar.add(Box.createHorizontalGlue());

        mainPanel.add(titleBar, BorderLayout.NORTH);

        // =================== HEADER PANEL ===================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(52, 152, 219));
        headerPanel.setPreferredSize(new Dimension(0, 70));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel leftPanel = new JPanel(new GridLayout(2, 1));
        leftPanel.setOpaque(false);

        lblWelcome = new JLabel("Dashboard Administrator");
        lblWelcome.setFont(new Font("Segoe UI  Emoji", Font.BOLD, 20));
        lblWelcome.setForeground(Color.WHITE);

        lblDateTime = new JLabel();
        lblDateTime.setFont(new Font("Segoe UI  Emoji", Font.PLAIN, 12));
        lblDateTime.setForeground(new Color(236, 240, 241));

        leftPanel.add(lblWelcome);
        leftPanel.add(lblDateTime);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);

        JLabel lblUser = new JLabel("👤 " + SessionManager.getCurrentUserName());
        lblUser.setFont(new Font("Segoe UI  Emoji", Font.BOLD, 14));
        lblUser.setForeground(Color.WHITE);
        rightPanel.add(lblUser);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBackground(new Color(231, 76, 60));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setPreferredSize(new Dimension(100, 35));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> logout());
        rightPanel.add(btnLogout);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        // =================== SIDEBAR PANEL ===================
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(sidebarBg);
        sidebarPanel.setPreferredSize(new Dimension(250, 0));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        sidebarPanel.add(createMenuButton("🏠 Dashboard", e -> showWelcomePanel()));
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        sidebarPanel.add(createMenuLabel("MASTER DATA"));
        sidebarPanel.add(createMenuButton("👥 Kelola Karyawan", e -> showUserManagement()));
        sidebarPanel.add(createMenuButton("🏷️ Kelola Merek", e -> showBrandManagement()));
        sidebarPanel.add(createMenuButton("📦 Kelola Tipe Produk", e -> showTypeManagement()));
        sidebarPanel.add(createMenuButton("📏 Kelola Ukuran", e -> showSizeManagement()));
        sidebarPanel.add(createMenuButton("🎨 Kelola Warna", e -> showColorManagement()));
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        sidebarPanel.add(createMenuLabel("PRODUK"));
        sidebarPanel.add(createMenuButton("👕 Kelola Produk", e -> showProductManagement()));
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        sidebarPanel.add(createMenuLabel("LAPORAN"));
        sidebarPanel.add(createMenuButton("📊 Laporan Penjualan", e -> showSalesReport()));
        sidebarPanel.add(createMenuButton("💰 Laporan Laba/Rugi", e -> showProfitReport()));
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        sidebarPanel.add(createMenuLabel("PENGATURAN"));
        sidebarPanel.add(createMenuButton("⏰ Jam Operasional", e -> showOperationalHours()));

        // =================== CONTENT PANEL ===================
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        showWelcomePanel();

        // =================== ASSEMBLE LAYOUT ===================
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(headerPanel, BorderLayout.NORTH);
        centerPanel.add(contentPanel, BorderLayout.CENTER);

        mainPanel.add(sidebarPanel, BorderLayout.WEST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        // Drag window
        addWindowDrag(titleBar);
        normalBounds = getBounds();
    }

    // =================== UTILITAS ===================

    private JButton createMacOSButton(Color color) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, getWidth(), getHeight());

                if (getModel().isRollover()) {
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(1.2f));
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;

                    if (color.equals(new Color(0xFF5F57))) { // Close
                        g2.drawLine(cx - 3, cy - 3, cx + 3, cy + 3);
                        g2.drawLine(cx + 3, cy - 3, cx - 3, cy + 3);
                    } else if (color.equals(new Color(0xFFBD2E))) { // Minimize
                        g2.drawLine(cx - 3, cy, cx + 3, cy);
                    } else if (color.equals(new Color(0x28CA42))) { // Maximize/Restore
                        if (isMaximized) {
                            g2.drawRect(cx - 2, cy - 1, 3, 3);
                            g2.drawRect(cx - 1, cy - 2, 3, 3);
                        } else {
                            g2.drawRect(cx - 2, cy - 2, 4, 4);
                        }
                    }
                }
                g2.dispose();
            }
        };
        button.setPreferredSize(new Dimension(14, 14));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setPreferredSize(new Dimension(15, 15));
                button.revalidate();
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setPreferredSize(new Dimension(14, 14));
                button.revalidate();
                button.repaint();
            }
        });

        return button;
    }

    private void toggleMaximize() {
        if (isMaximized) {
            setBounds(normalBounds);
            isMaximized = false;
        } else {
            normalBounds = getBounds();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle screenBounds = ge.getMaximumWindowBounds();
            setBounds(screenBounds);
            isMaximized = true;
        }
        updateWindowShape();
    }

    private void addWindowDrag(Component comp) {
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mousePoint = e.getPoint();
            }
        });
        comp.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (!isMaximized) {
                    Point curr = e.getLocationOnScreen();
                    setLocation(curr.x - mousePoint.x, curr.y - mousePoint.y);
                }
            }
        });
    }

    private JLabel createMenuLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI  Emoji", Font.BOLD, 11));
        label.setForeground(new Color(149, 165, 166));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));
        label.setMaximumSize(new Dimension(250, 30));
        return label;
    }

    private JButton createMenuButton(String text, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI  Emoji", Font.PLAIN, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(44, 62, 80));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(250, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(52, 73, 94));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(44, 62, 80));
            }
        });

        return btn;
    }

    // =================== CONTENT PANELS ===================
    private void showWelcomePanel() {
        contentPanel.removeAll();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel lblTitle = new JLabel("Selamat Datang di Dashboard Admin");
        lblTitle.setFont(new Font("Segoe UI  Emoji", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblTitle, gbc);

        JLabel lblSubtitle = new JLabel("Pilih menu di sebelah kiri untuk mengelola sistem");
        lblSubtitle.setFont(new Font("Segoe UI  Emoji", Font.PLAIN, 14));
        lblSubtitle.setForeground(Color.GRAY);
        gbc.gridy = 1;
        panel.add(lblSubtitle, gbc);

        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showUserManagement() {
        contentPanel.removeAll();
        contentPanel.add(new UserManagementPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showBrandManagement() {
        contentPanel.removeAll();
        contentPanel.add(new BrandManagementPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showTypeManagement() {
        contentPanel.removeAll();
        contentPanel.add(new TypeManagementPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showSizeManagement() {
        contentPanel.removeAll();
        contentPanel.add(new SizeManagementPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showColorManagement() {
        contentPanel.removeAll();
        contentPanel.add(new ColorManagementPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showProductManagement() {
        contentPanel.removeAll();
        contentPanel.add(new ProductManagementPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showSalesReport() {
        contentPanel.removeAll();
        contentPanel.add(new SalesReportPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showProfitReport() {
        contentPanel.removeAll();
        contentPanel.add(new ProfitReportPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showOperationalHours() {
        contentPanel.removeAll();
        contentPanel.add(new OperationalHoursPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // =================== UTILITAS WAKTU & LOGOUT ===================
    private void startClock() {
        clockTimer = new Timer(1000, e -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            lblDateTime.setText(FormatterUtils.formatDate(java.sql.Timestamp.valueOf(now)));
        });
        clockTimer.start();
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Logout dari sistem?", "Konfirmasi", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.clearSession();
            dispose();
            SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
        }
    }

    // =================== ROUNDED CORNERS ===================
    private void updateWindowShape() {
        if (!isMaximized) {
            int arc = 20;
            Shape shape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc);
            setShape(shape);
        } else {
            setShape(null);
        }
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        updateWindowShape();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        updateWindowShape();
    }

    // =================== MAIN ===================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DashboardAdmin().setVisible(true));
    }
}