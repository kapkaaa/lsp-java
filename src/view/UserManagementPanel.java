package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import config.DatabaseConfig;
import utils.*;
import java.awt.event.*;
import java.awt.geom.*;

public class UserManagementPanel extends JPanel {
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;
    private JComboBox<String> cmbRoleFilter;
    
    public UserManagementPanel() {
        initComponents();
        loadData();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        
        // Header
        JLabel lblTitle = new JLabel("Kelola Karyawan");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);
        
        // Filter Panel — SATU BARIS
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);
        
        filterPanel.add(new JLabel("Cari:"));
        txtSearch = createStyledTextField(20);
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                searchData();
            }
        });
        filterPanel.add(txtSearch);
        
        filterPanel.add(new JLabel("Role:"));
        cmbRoleFilter = new JComboBox<>(new String[]{"Semua", "admin", "cashier"});
        cmbRoleFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbRoleFilter.setBackground(Color.WHITE);
        cmbRoleFilter.setForeground(Color.decode("#222222"));
        cmbRoleFilter.addActionListener(e -> loadData());
        filterPanel.add(cmbRoleFilter);
        
        // Table
        String[] columns = {"ID", "Nama", "Username", "NIK", "Alamat", "Kota", "Telepon", "Role", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setSelectionBackground(new Color(236, 240, 241));
        table.setSelectionForeground(Color.BLACK);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        
        // Style table header
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(236, 240, 241));
        header.setForeground(Color.BLACK);
        header.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton btnAdd = createStyledButton("Tambah", new Color(46, 204, 113), e -> showAddDialog());
        JButton btnEdit = createStyledButton("Edit", new Color(52, 152, 219), e -> showEditDialog());
        JButton btnDelete = createStyledButton("Hapus", new Color(231, 76, 60), e -> deleteUser());
        JButton btnChangePassword = createStyledButton("Ubah Password", new Color(241, 196, 15), e -> changePassword());
        
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnChangePassword);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(filterPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
    }
    
    // Helper: input field stylish
    private JTextField createStyledTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        field.setBackground(Color.WHITE);
        field.setForeground(Color.decode("#222222"));
        return field;
    }
    
    // Helper: tombol berwarna
    private JButton createStyledButton(String text, Color bgColor, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bgColor.brighter());
                } else {
                    g2.setColor(bgColor);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(120, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }
    
    private void loadData() {
        tableModel.setRowCount(0);
        String roleFilter = (String) cmbRoleFilter.getSelectedItem();
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT u.*, r.name as role_name FROM users u " +
                        "JOIN roles r ON u.role_id = r.id " +
                        "WHERE r.name IN ('admin', 'cashier')";
            
            if (!"Semua".equals(roleFilter)) {
                sql += " AND r.name = ?";
            }
            sql += " ORDER BY u.name";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!"Semua".equals(roleFilter)) {
                ps.setString(1, roleFilter);
            }
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("nik"),
                    rs.getString("address"),
                    rs.getString("city"),
                    rs.getString("phone"),
                    rs.getString("role_name"),
                    rs.getString("status")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void searchData() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        
        if (keyword.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + keyword));
        }
    }
    
    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tambah Karyawan", true);
        dialog.setUndecorated(true);
        dialog.setSize(400, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Custom title bar
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.decode("#b3ebf2"));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        titleBar.setPreferredSize(new Dimension(400, 40));
        titleBar.setOpaque(false);

        JButton btnClose = createMacOSButton(new Color(0xFF5F57));
        btnClose.addActionListener(e -> dialog.dispose());

        JLabel titleLabel = new JLabel("Tambah Karyawan", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.decode("#222222"));
        titleLabel.setOpaque(false);

        titleBar.add(btnClose);
        titleBar.add(Box.createHorizontalGlue());
        titleBar.add(titleLabel);
        titleBar.add(Box.createHorizontalGlue());

        dialog.add(titleBar, BorderLayout.NORTH);

        // Content
        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.decode("#b3ebf2"));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        contentPanel.setOpaque(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        JTextField txtName = createStyledTextField(20);
        JTextField txtUsername = createStyledTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        stylePasswordField(txtPassword);
        JTextField txtNIK = createStyledTextField(20);
        JTextArea txtAddress = new JTextArea(3, 20);
        txtAddress.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        txtAddress.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        txtAddress.setBackground(Color.WHITE);
        JTextField txtCity = createStyledTextField(20);
        JTextField txtPhone = createStyledTextField(20);
        JComboBox<String> cmbRole = new JComboBox<>(new String[]{"admin", "cashier"});
        cmbRole.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbRole.setBackground(Color.WHITE);
        cmbRole.setForeground(Color.decode("#222222"));

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Nama:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtName, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtUsername, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtPassword, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("NIK:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtNIK, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Alamat:"), gbc);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(txtAddress), gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Kota:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtCity, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Telepon:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtPhone, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        formPanel.add(cmbRole, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnSave = createStyledButton("Simpan", new Color(46, 204, 113), e -> {
            if (validateInput(txtName, txtUsername, txtPassword, txtNIK, txtPhone)) {
                saveUser(null, txtName.getText(), txtUsername.getText(), 
                        new String(txtPassword.getPassword()), txtNIK.getText(),
                        txtAddress.getText(), txtCity.getText(), txtPhone.getText(),
                        (String) cmbRole.getSelectedItem());
                dialog.dispose();
                loadData();
            }
        });

        JButton btnCancel = createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        contentPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(contentPanel, BorderLayout.CENTER);

        // Drag window
        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);

        dialog.setVisible(true);
    }
    
    // Helper untuk dialog edit & add
    private void stylePasswordField(JPasswordField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        field.setBackground(Color.WHITE);
        field.setForeground(Color.decode("#222222"));
        field.setEchoChar('•');
    }
    
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

                    if (color.equals(new Color(0xFF5F57))) {
                        g2.drawLine(cx - 3, cy - 3, cx + 3, cy + 3);
                        g2.drawLine(cx + 3, cy - 3, cx - 3, cy + 3);
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
        return button;
    }

    private void addWindowDrag(Component comp, JDialog dialog) {
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mousePoint = e.getPoint();
            }
        });
        comp.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point curr = e.getLocationOnScreen();
                dialog.setLocation(curr.x - mousePoint.x, curr.y - mousePoint.y);
            }
        });
    }

    private void updateDialogShape(JDialog dialog) {
        int arc = 20;
        Shape shape = new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), arc, arc);
        dialog.setShape(shape);
    }
    
    // ... (method lain seperti showEditDialog, validateInput, saveUser, dll TETAP SAMA)
    
    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih data yang akan diedit!");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0);
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM users WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Karyawan", true);
                dialog.setUndecorated(true);
                dialog.setSize(400, 520);
                dialog.setLocationRelativeTo(this);
                dialog.setLayout(new BorderLayout());

                JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setColor(Color.decode("#b3ebf2"));
                        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                        g2d.dispose();
                        super.paintComponent(g);
                    }
                };
                titleBar.setPreferredSize(new Dimension(400, 40));
                titleBar.setOpaque(false);

                JButton btnClose = createMacOSButton(new Color(0xFF5F57));
                btnClose.addActionListener(e -> dialog.dispose());

                JLabel titleLabel = new JLabel("Edit Karyawan", SwingConstants.CENTER);
                titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                titleLabel.setForeground(Color.decode("#222222"));
                titleLabel.setOpaque(false);

                titleBar.add(btnClose);
                titleBar.add(Box.createHorizontalGlue());
                titleBar.add(titleLabel);
                titleBar.add(Box.createHorizontalGlue());

                dialog.add(titleBar, BorderLayout.NORTH);

                JPanel contentPanel = new JPanel(new BorderLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setColor(Color.decode("#b3ebf2"));
                        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                        g2d.dispose();
                        super.paintComponent(g);
                    }
                };
                contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
                contentPanel.setOpaque(false);

                JPanel formPanel = new JPanel(new GridBagLayout());
                formPanel.setOpaque(false);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(8, 8, 8, 8);

                JTextField txtName = createStyledTextField(20);
                txtName.setText(rs.getString("name"));
                JTextField txtUsername = createStyledTextField(20);
                txtUsername.setText(rs.getString("username"));
                JTextField txtNIK = createStyledTextField(20);
                txtNIK.setText(rs.getString("nik"));
                JTextArea txtAddress = new JTextArea(rs.getString("address"), 3, 20);
                txtAddress.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                txtAddress.setLineWrap(true);
                txtAddress.setWrapStyleWord(true);
                txtAddress.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
                txtAddress.setBackground(Color.WHITE);
                JTextField txtCity = createStyledTextField(20);
                txtCity.setText(rs.getString("city"));
                JTextField txtPhone = createStyledTextField(20);
                txtPhone.setText(rs.getString("phone"));

                String sql2 = "SELECT r.name FROM roles r JOIN users u ON r.id = u.role_id WHERE u.id = ?";
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setInt(1, id);
                ResultSet rs2 = ps2.executeQuery();
                String currentRole = rs2.next() ? rs2.getString("name") : "cashier";

                JComboBox<String> cmbRole = new JComboBox<>(new String[]{"admin", "cashier"});
                cmbRole.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                cmbRole.setBackground(Color.WHITE);
                cmbRole.setForeground(Color.decode("#222222"));
                cmbRole.setSelectedItem(currentRole);

                JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"active", "inactive"});
                cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                cmbStatus.setBackground(Color.WHITE);
                cmbStatus.setForeground(Color.decode("#222222"));
                cmbStatus.setSelectedItem(rs.getString("status"));

                int r = 0;
                gbc.gridx = 0; gbc.gridy = r;
                formPanel.add(new JLabel("Nama:"), gbc);
                gbc.gridx = 1;
                formPanel.add(txtName, gbc);

                r++;
                gbc.gridx = 0; gbc.gridy = r;
                formPanel.add(new JLabel("Username:"), gbc);
                gbc.gridx = 1;
                formPanel.add(txtUsername, gbc);

                r++;
                gbc.gridx = 0; gbc.gridy = r;
                formPanel.add(new JLabel("NIK:"), gbc);
                gbc.gridx = 1;
                formPanel.add(txtNIK, gbc);

                r++;
                gbc.gridx = 0; gbc.gridy = r;
                formPanel.add(new JLabel("Alamat:"), gbc);
                gbc.gridx = 1;
                formPanel.add(new JScrollPane(txtAddress), gbc);

                r++;
                gbc.gridx = 0; gbc.gridy = r;
                formPanel.add(new JLabel("Kota:"), gbc);
                gbc.gridx = 1;
                formPanel.add(txtCity, gbc);

                r++;
                gbc.gridx = 0; gbc.gridy = r;
                formPanel.add(new JLabel("Telepon:"), gbc);
                gbc.gridx = 1;
                formPanel.add(txtPhone, gbc);

                r++;
                gbc.gridx = 0; gbc.gridy = r;
                formPanel.add(new JLabel("Role:"), gbc);
                gbc.gridx = 1;
                formPanel.add(cmbRole, gbc);

                r++;
                gbc.gridx = 0; gbc.gridy = r;
                formPanel.add(new JLabel("Status:"), gbc);
                gbc.gridx = 1;
                formPanel.add(cmbStatus, gbc);

                contentPanel.add(formPanel, BorderLayout.CENTER);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
                btnPanel.setOpaque(false);

                JButton btnUpdate = createStyledButton("Update", new Color(52, 152, 219), e -> {
                    if (validateInputEdit(txtName, txtUsername, txtNIK, txtPhone)) {
                        updateUser(id, txtName.getText(), txtUsername.getText(),
                                txtNIK.getText(), txtAddress.getText(), txtCity.getText(),
                                txtPhone.getText(), (String) cmbRole.getSelectedItem(),
                                (String) cmbStatus.getSelectedItem());
                        dialog.dispose();
                        loadData();
                    }
                });

                JButton btnCancel = createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());

                btnPanel.add(btnUpdate);
                btnPanel.add(btnCancel);
                contentPanel.add(btnPanel, BorderLayout.SOUTH);

                dialog.add(contentPanel, BorderLayout.CENTER);

                addWindowDrag(titleBar, dialog);
                updateDialogShape(dialog);

                dialog.setVisible(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private boolean validateInput(JTextField name, JTextField username, 
                                  JPasswordField password, JTextField nik, JTextField phone) {
        if (!InputValidator.isNotEmpty(name.getText())) {
            JOptionPane.showMessageDialog(this, "Nama harus diisi!");
            return false;
        }
        if (!InputValidator.isValidUsername(username.getText())) {
            JOptionPane.showMessageDialog(this, "Username tidak valid (3-20 karakter, huruf/angka/_)!");
            return false;
        }
        if (!InputValidator.isValidPassword(new String(password.getPassword()))) {
            JOptionPane.showMessageDialog(this, "Password minimal 6 karakter!");
            return false;
        }
        if (!InputValidator.isValidNIK(nik.getText())) {
            JOptionPane.showMessageDialog(this, "NIK harus 16 digit dan berupa angka!");
            return false;
        }
        if (!InputValidator.isValidPhone(phone.getText())) {
            JOptionPane.showMessageDialog(this, "Nomor telepon tidak valid!");
            return false;
        }
        return true;
    }
    
    private boolean validateInputEdit(JTextField name, JTextField username, 
                                      JTextField nik, JTextField phone) {
        if (!InputValidator.isNotEmpty(name.getText())) {
            JOptionPane.showMessageDialog(this, "Nama harus diisi!");
            return false;
        }
        if (!InputValidator.isValidUsername(username.getText())) {
            JOptionPane.showMessageDialog(this, "Username tidak valid!");
            return false;
        }
        if (nik.getText() != null && !nik.getText().isEmpty() && 
            !InputValidator.isValidNIK(nik.getText())) {
            JOptionPane.showMessageDialog(this, "NIK harus 16 digit dan berupa angka!");
            return false;
        }
        if (!InputValidator.isValidPhone(phone.getText())) {
            JOptionPane.showMessageDialog(this, "Nomor telepon tidak valid!");
            return false;
        }
        return true;
    }
    
    private void saveUser(Integer id, String name, String username, String password,
                         String nik, String address, String city, String phone, String role) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sqlRole = "SELECT id FROM roles WHERE name = ?";
            PreparedStatement psRole = conn.prepareStatement(sqlRole);
            psRole.setString(1, role);
            ResultSet rsRole = psRole.executeQuery();
            int roleId = rsRole.next() ? rsRole.getInt("id") : 0;
            
            String sql = "INSERT INTO users (role_id, name, username, password, nik, " +
                        "address, city, phone, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, roleId);
            ps.setString(2, name);
            ps.setString(3, username);
            ps.setString(4, SecurityUtils.hashPassword(password));
            ps.setString(5, nik);
            ps.setString(6, address);
            ps.setString(7, city);
            ps.setString(8, phone);
            
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Data berhasil disimpan!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void updateUser(int id, String name, String username, String nik,
                           String address, String city, String phone, String role, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sqlRole = "SELECT id FROM roles WHERE name = ?";
            PreparedStatement psRole = conn.prepareStatement(sqlRole);
            psRole.setString(1, role);
            ResultSet rsRole = psRole.executeQuery();
            int roleId = rsRole.next() ? rsRole.getInt("id") : 0;
            
            String sql = "UPDATE users SET role_id = ?, name = ?, username = ?, nik = ?, " +
                        "address = ?, city = ?, phone = ?, status = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, roleId);
            ps.setString(2, name);
            ps.setString(3, username);
            ps.setString(4, nik);
            ps.setString(5, address);
            ps.setString(6, city);
            ps.setString(7, phone);
            ps.setString(8, status);
            ps.setInt(9, id);
            
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Data berhasil diupdate!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void deleteUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih data yang akan dihapus!");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0);
        String name = (String) table.getValueAt(row, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Hapus karyawan: " + name + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                String sql = "DELETE FROM users WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Data berhasil dihapus!");
                loadData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
    
    private void changePassword() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih karyawan terlebih dahulu!");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0);
        String name = (String) table.getValueAt(row, 1);
        
        JPasswordField pwd = new JPasswordField(20);
        pwd.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pwd.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        pwd.setBackground(Color.WHITE);
        
        int option = JOptionPane.showConfirmDialog(this,
            new Object[]{"Password baru untuk " + name + ":", pwd},
            "Ubah Password", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            String newPassword = new String(pwd.getPassword());
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password minimal 6 karakter!");
                return;
            }
            
            try (Connection conn = DatabaseConfig.getConnection()) {
                String sql = "UPDATE users SET password = ? WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, SecurityUtils.hashPassword(newPassword));
                ps.setInt(2, id);
                ps.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Password berhasil diubah!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
    
    private Point mousePoint;
}