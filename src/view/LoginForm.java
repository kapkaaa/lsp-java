package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import config.DatabaseConfig;
import model.User;
import model.Role;
import utils.SecurityUtils;
import utils.OperationalHoursValidator;
import utils.SessionManager;

public class LoginForm extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblStatus;
    private Point mousePoint;
    private boolean isMaximized = false;
    private Rectangle normalBounds;

    public LoginForm() {
        setUndecorated(true);
        initComponents();
        checkOperationalHours();
        setLocationRelativeTo(null);
        updateWindowShape();
        getRootPane().setDefaultButton(btnLogin);
    }

    private void initComponents() {
        // 🎨 Warna tema (sama seperti Kavi Laundry)
        Color bgColor = Color.decode("#b3ebf2");
        Color textMain = Color.decode("#222222");
        Color textSub = Color.decode("#555555");
        Color accent = Color.decode("#3fc1d3");

        setSize(450, 450);
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
        titleBar.setPreferredSize(new Dimension(450, 40));
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

        JLabel titleLabel = new JLabel("Login - DistroZone", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(textMain);
        titleLabel.setOpaque(false);
        titleBar.add(Box.createHorizontalGlue());
        titleBar.add(titleLabel);
        titleBar.add(Box.createHorizontalGlue());

        mainPanel.add(titleBar, BorderLayout.NORTH);

        // =================== CONTENT LOGIN ===================
        JPanel contentPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 🖼️ Logo
        JLabel lblLogo = new JLabel();
        try {
            ImageIcon originalIcon = new ImageIcon(getClass().getResource("/images/Logo.jpg"));
            if (originalIcon.getIconWidth() > 0 && originalIcon.getIconHeight() > 0) {
                Image scaledImage = originalIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                lblLogo.setIcon(new ImageIcon(scaledImage));
            } else {
                lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 16));
                lblLogo.setForeground(textMain);
            }
        } catch (Exception e) {
            lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblLogo.setForeground(textMain);
        }
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 5, 0);
        contentPanel.add(lblLogo, gbc);

        // 👤 Judul
        JLabel lblTitle = new JLabel("DISTROZONE", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(textMain);
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 0, 5, 0);
        contentPanel.add(lblTitle, gbc);

        JLabel lblSubtitle = new JLabel("Sistem Kasir Desktop", SwingConstants.CENTER);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(textSub);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(lblSubtitle, gbc);

        // 📅 Operational Info
        JLabel lblOperationalInfo = new JLabel("", SwingConstants.CENTER);
        lblOperationalInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblOperationalInfo.setForeground(new Color(52, 152, 219));
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 15, 0);
        contentPanel.add(lblOperationalInfo, gbc);
        
        Dimension fieldSize = new Dimension(150, 32);

        // Username
        JLabel iconUser = new JLabel("👤");
        iconUser.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        txtUsername = new JTextField(15);
        txtUsername.setPreferredSize(fieldSize);
        txtUsername.setMaximumSize(fieldSize);
        styleTextField(txtUsername, "Username", textSub, textMain);

        JPanel userPanel = createInputPanel(iconUser, txtUsername, bgColor);
        gbc.gridy = 4;
        gbc.insets = new Insets(5, 0, 12, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0; 
        contentPanel.add(userPanel, gbc);
        
        // password
        JLabel iconLock = new JLabel("🔒");
        iconLock.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        txtPassword = new JPasswordField(15);
        txtPassword.setPreferredSize(fieldSize);
        txtPassword.setMaximumSize(fieldSize);
        stylePasswordField(txtPassword, "Password", textSub, textMain);

        JPanel passPanel = createInputPanel(iconLock, txtPassword, bgColor);
        gbc.gridy = 5;
        contentPanel.add(passPanel, gbc);

        // 🔘 Tombol login
        btnLogin = new JButton("LOG IN");
        btnLogin.setBackground(accent);
        btnLogin.setForeground(Color.black);
        btnLogin.setFocusPainted(false);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnLogin.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnLogin.addActionListener(e -> login());
        gbc.gridy = 6; 
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(10, 80, 10, 80);
        contentPanel.add(btnLogin, gbc);

        // Status Label
        lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(231, 76, 60));
        gbc.gridy = 7; gbc.insets = new Insets(5, 0, 0, 0);
        contentPanel.add(lblStatus, gbc);

        // Footer
        JLabel lblFooter = new JLabel("© 2024 DistroZone - Jln. Raya Pegangsaan Timur No.29H Kelapa Gading Jakarta");
        lblFooter.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblFooter.setForeground(new Color(127, 140, 141));
        lblFooter.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 8;
        gbc.insets = new Insets(15, 0, 0, 0);
        contentPanel.add(lblFooter, gbc);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        // Enter key listener
        txtUsername.addActionListener(e -> login());
        txtPassword.addActionListener(e -> login());

        // Enable drag window from title bar only
        addWindowDrag(titleBar);
        normalBounds = getBounds();
        
        // Simpan reference untuk update operational info
        checkOperationalHours();
        // Pastikan operational info bisa diakses
        for (Component comp : contentPanel.getComponents()) {
            if (comp instanceof JLabel && ((JLabel) comp).getForeground().equals(new Color(52, 152, 219))) {
                ((JLabel) comp).setText(OperationalHoursValidator.getOperationalMessage("store"));
                break;
            }
        }
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

    private JPanel createInputPanel(JLabel icon, JComponent field, Color bg) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBackground(Color.WHITE); 
        panel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        panel.setMaximumSize(new Dimension(220, 36));
        panel.setPreferredSize(new Dimension(220, 36));

        icon.setPreferredSize(new Dimension(30, 30));
        icon.setHorizontalAlignment(SwingConstants.CENTER);

        field.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        field.setOpaque(false); 

        panel.add(icon);
        panel.add(field);

        return panel;
    }



    private ImageIcon resizeIcon(ImageIcon icon, int width, int height) {
        Image img = icon.getImage();
        if (img == null) return new ImageIcon();
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private void styleTextField(JTextField field, String placeholder, Color textSub, Color textMain) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(textSub);
        field.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(textMain);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(textSub);
                }
            }
        });
    }

    private void stylePasswordField(JPasswordField field, String placeholder, Color textSub, Color textMain) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(textSub);
        field.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        field.setEchoChar((char) 0);
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setEchoChar('•');
                    field.setForeground(textMain);
                }
            }
            public void focusLost(FocusEvent e) {
                if (String.valueOf(field.getPassword()).isEmpty()) {
                    field.setText(placeholder);
                    field.setEchoChar((char) 0);
                    field.setForeground(textSub);
                }
            }
        });
    }

    // =================== LOGIN ===================
    private void checkOperationalHours() {
        // Operational info sudah di-set di initComponents
    }

    private void login() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        // Reset status
        lblStatus.setText("");

        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Username dan password harus diisi!");
            return;
        }

        // VALIDASI JAM OPERASIONAL
        if (!OperationalHoursValidator.isOperationalHour("store")) {
            String schedule = OperationalHoursValidator.getWeeklySchedule("store");
            JTextArea textArea = new JTextArea(
                "APLIKASI HANYA DAPAT DIAKSES PADA JAM OPERASIONAL TOKO\n\n" +
                "Jadwal Operasional Toko:\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                schedule +
                "\nMohon akses aplikasi pada jam operasional toko."
            );
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setBackground(new Color(236, 240, 241));
            textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JOptionPane.showMessageDialog(this,
                textArea,
                "Di Luar Jam Operasional",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Memproses...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private User user;
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DatabaseConfig.getConnection()) {
                    String sql = "SELECT u.*, r.name as role_name, r.information as role_info " +
                                "FROM users u " +
                                "JOIN roles r ON u.role_id = r.id " +
                                "WHERE u.username = ? AND u.status = 'active'";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, username);

                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        String hashedPassword = rs.getString("password");

                        if (SecurityUtils.verifyPassword(password, hashedPassword)) {
                            String roleName = rs.getString("role_name");
                            if (!"Admin".equals(roleName) && !"Cashier".equals(roleName)) {
                                errorMessage = "Hanya Admin dan Kasir yang dapat mengakses aplikasi ini!";
                                return false;
                            }

                            user = new User();
                            user.setId(rs.getInt("id"));
                            user.setRoleId(rs.getInt("role_id"));
                            user.setName(rs.getString("name"));
                            user.setUsername(rs.getString("username"));
                            user.setNik(rs.getString("nik"));
                            user.setAddress(rs.getString("address"));
                            user.setCity(rs.getString("city"));
                            user.setPhone(rs.getString("phone"));
                            user.setProfilePhoto(rs.getString("profile_photo"));
                            user.setStatus(rs.getString("status"));

                            Role role = new Role();
                            role.setId(rs.getInt("role_id"));
                            role.setName(roleName);
                            role.setInformation(rs.getString("role_info"));
                            user.setRole(role);

                            return true;
                        } else {
                            errorMessage = "Password salah!";
                            return false;
                        }
                    } else {
                        errorMessage = "Username tidak ditemukan atau akun tidak aktif!";
                        return false;
                    }

                } catch (SQLException e) {
                    errorMessage = "Error koneksi database: " + e.getMessage();
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("LOG IN");

                try {
                    if (get()) {
                        SessionManager.setCurrentUser(user);
                        openDashboard(user.getRole().getName());
                        dispose();
                    } else {
                        lblStatus.setText(errorMessage);
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                    }
                } catch (Exception e) {
                    lblStatus.setText("Error: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void openDashboard(String role) {
        SwingUtilities.invokeLater(() -> {
            if ("Admin".equals(role)) {
                new DashboardAdmin().setVisible(true);
            } else if ("Cashier".equals(role)) {
                new DashboardKasir().setVisible(true);
            }
        });
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
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}