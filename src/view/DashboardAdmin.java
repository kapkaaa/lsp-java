package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import utils.*;

public class DashboardAdmin extends JFrame {
    private JLabel lblWelcome, lblDateTime;
    private JPanel contentPanel;
    private Timer clockTimer;
    
    public DashboardAdmin() {
        initComponents();
        startClock();
    }
    
    private void initComponents() {
        setTitle("Dashboard Admin - DistroZone");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Sidebar
        add(createSidebarPanel(), BorderLayout.WEST);
        
        // Content
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        showWelcomePanel();
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(52, 152, 219));
        panel.setPreferredSize(new Dimension(0, 70));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
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
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createSidebarPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(44, 62, 80));
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        // Menu items
        panel.add(createMenuButton("🏠 Dashboard", e -> showWelcomePanel()));
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        panel.add(createMenuLabel("MASTER DATA"));
        panel.add(createMenuButton("👥 Kelola Karyawan", e -> showUserManagement()));
        panel.add(createMenuButton("🏷️ Kelola Brand", e -> showBrandManagement()));
        panel.add(createMenuButton("📦 Kelola Tipe Produk", e -> showTypeManagement()));
        panel.add(createMenuButton("📏 Kelola Size", e -> showSizeManagement()));
        panel.add(createMenuButton("🎨 Kelola Warna", e -> showColorManagement()));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        panel.add(createMenuLabel("PRODUK"));
        panel.add(createMenuButton("👕 Kelola Produk", e -> showProductManagement()));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        panel.add(createMenuLabel("LAPORAN"));
        panel.add(createMenuButton("📊 Laporan Penjualan", e -> showSalesReport()));
        panel.add(createMenuButton("💰 Laporan Laba/Rugi", e -> showProfitReport()));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        panel.add(createMenuLabel("PENGATURAN"));
        panel.add(createMenuButton("⏰ Jam Operasional", e -> showOperationalHours()));
        
        return panel;
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
            new LoginForm().setVisible(true);
        }
    }
}