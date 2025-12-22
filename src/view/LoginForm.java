package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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
    private JLabel lblOperationalInfo;
    
    public LoginForm() {
        initComponents();
        checkOperationalHours();
    }
    
    private void initComponents() {
        setTitle("Login - DistroZone Kasir");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);
        
        // Panel Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setPreferredSize(new Dimension(450, 80));
        headerPanel.setLayout(new BorderLayout());
        
        JLabel lblTitle = new JLabel("DISTROZONE", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 28));
        lblTitle.setForeground(Color.WHITE);
        
        JLabel lblSubtitle = new JLabel("Sistem Kasir Desktop", SwingConstants.CENTER);
        lblSubtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(236, 240, 241));
        
        JPanel titleContainer = new JPanel(new GridLayout(2, 1, 0, 5));
        titleContainer.setOpaque(false);
        titleContainer.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        titleContainer.add(lblTitle);
        titleContainer.add(lblSubtitle);
        
        headerPanel.add(titleContainer, BorderLayout.CENTER);
        
        // Panel Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 20, 50));
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);
        
        // Operational Info
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        lblOperationalInfo = new JLabel("", SwingConstants.CENTER);
        lblOperationalInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        lblOperationalInfo.setForeground(new Color(52, 152, 219));
        formPanel.add(lblOperationalInfo, gbc);
        
        gbc.gridwidth = 1;
        
        // Username
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.insets = new Insets(15, 5, 8, 5);
        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Arial", Font.BOLD, 13));
        formPanel.add(lblUsername, gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        txtUsername = new JTextField(18);
        txtUsername.setFont(new Font("Arial", Font.PLAIN, 13));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        formPanel.add(txtUsername, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.insets = new Insets(8, 5, 8, 5);
        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("Arial", Font.BOLD, 13));
        formPanel.add(lblPassword, gbc);
        
        gbc.gridx = 1; gbc.gridy = 2;
        txtPassword = new JPasswordField(18);
        txtPassword.setFont(new Font("Arial", Font.PLAIN, 13));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        formPanel.add(txtPassword, gbc);
        
        // Button Login
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 5, 8, 5);
        btnLogin = new JButton("LOGIN");
        btnLogin.setFont(new Font("Arial", Font.BOLD, 14));
        btnLogin.setBackground(new Color(52, 152, 219));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setPreferredSize(new Dimension(0, 40));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> login());
        
        // Hover effect
        btnLogin.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btnLogin.setBackground(new Color(41, 128, 185));
            }
            public void mouseExited(MouseEvent e) {
                btnLogin.setBackground(new Color(52, 152, 219));
            }
        });
        
        formPanel.add(btnLogin, gbc);
        
        // Status Label
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 5, 5, 5);
        lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(231, 76, 60));
        formPanel.add(lblStatus, gbc);
        
        // Enter key listener
        KeyAdapter enterListener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    login();
                }
            }
        };
        txtUsername.addKeyListener(enterListener);
        txtPassword.addKeyListener(enterListener);
        
        add(headerPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        
        // Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(new Color(236, 240, 241));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        JLabel lblFooter = new JLabel("© 2024 DistroZone - Jln. Raya Pegangsaan Timur No.29H Kelapa Gading Jakarta");
        lblFooter.setFont(new Font("Arial", Font.PLAIN, 11));
        lblFooter.setForeground(new Color(127, 140, 141));
        footerPanel.add(lblFooter);
        add(footerPanel, BorderLayout.SOUTH);
    }
    
    private void checkOperationalHours() {
        String message = OperationalHoursValidator.getOperationalMessage("store");
        lblOperationalInfo.setText(message);
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
        
        // VALIDASI JAM OPERASIONAL (WAJIB)
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
        
        // Proses login
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
                            // Hanya admin dan cashier yang bisa login ke aplikasi desktop
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
                btnLogin.setText("LOGIN");
                
                try {
                    if (get()) {
                        // Simpan session
                        SessionManager.setCurrentUser(user);
                        
                        // Buka dashboard sesuai role
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
    
    public static void main(String[] args) {
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}