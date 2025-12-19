package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import config.DatabaseConfig;
import utils.*;

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
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        
        JLabel lblTitle = new JLabel("Kelola Karyawan");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(lblTitle, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Search & Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);
        
        filterPanel.add(new JLabel("Cari:"));
        txtSearch = new JTextField(20);
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                searchData();
            }
        });
        filterPanel.add(txtSearch);
        
        filterPanel.add(new JLabel("Role:"));
        cmbRoleFilter = new JComboBox<>(new String[]{"Semua", "admin", "cashier"});
        cmbRoleFilter.addActionListener(e -> loadData());
        filterPanel.add(cmbRoleFilter);
        
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadData());
        filterPanel.add(btnRefresh);
        
        // Table
        String[] columns = {"ID", "Nama", "Username", "NIK", "Alamat", "Kota", "Telepon", "Role", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton btnAdd = createButton("Tambah", new Color(46, 204, 113), e -> showAddDialog());
        JButton btnEdit = createButton("Edit", new Color(52, 152, 219), e -> showEditDialog());
        JButton btnDelete = createButton("Hapus", new Color(231, 76, 60), e -> deleteUser());
        JButton btnChangePassword = createButton("Ubah Password", new Color(241, 196, 15), e -> changePassword());
        
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
    
    private JButton createButton(String text, Color color, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(120, 35));
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
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField txtName = new JTextField(20);
        JTextField txtUsername = new JTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        JTextField txtNIK = new JTextField(20);
        JTextArea txtAddress = new JTextArea(3, 20);
        JTextField txtCity = new JTextField(20);
        JTextField txtPhone = new JTextField(20);
        JComboBox<String> cmbRole = new JComboBox<>(new String[]{"admin", "cashier"});
        
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Nama:"), gbc);
        gbc.gridx = 1;
        panel.add(txtName, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(txtUsername, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("NIK:"), gbc);
        gbc.gridx = 1;
        panel.add(txtNIK, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Alamat:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(txtAddress), gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Kota:"), gbc);
        gbc.gridx = 1;
        panel.add(txtCity, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Telepon:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPhone, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbRole, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (validateInput(txtName, txtUsername, txtPassword, txtNIK, txtPhone)) {
                saveUser(null, txtName.getText(), txtUsername.getText(), 
                        new String(txtPassword.getPassword()), txtNIK.getText(),
                        txtAddress.getText(), txtCity.getText(), txtPhone.getText(),
                        (String) cmbRole.getSelectedItem());
                dialog.dispose();
                loadData();
            }
        });
        
        JButton btnCancel = new JButton("Batal");
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih data yang akan diedit!");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0);
        
        // Load data
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM users WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Karyawan", true);
                dialog.setSize(500, 600);
                dialog.setLocationRelativeTo(this);
                
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);
                
                JTextField txtName = new JTextField(rs.getString("name"), 20);
                JTextField txtUsername = new JTextField(rs.getString("username"), 20);
                JTextField txtNIK = new JTextField(rs.getString("nik"), 20);
                JTextArea txtAddress = new JTextArea(rs.getString("address"), 3, 20);
                JTextField txtCity = new JTextField(rs.getString("city"), 20);
                JTextField txtPhone = new JTextField(rs.getString("phone"), 20);
                
                // Get role
                String sql2 = "SELECT r.name FROM roles r JOIN users u ON r.id = u.role_id WHERE u.id = ?";
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setInt(1, id);
                ResultSet rs2 = ps2.executeQuery();
                String currentRole = rs2.next() ? rs2.getString("name") : "cashier";
                
                JComboBox<String> cmbRole = new JComboBox<>(new String[]{"admin", "cashier"});
                cmbRole.setSelectedItem(currentRole);
                
                JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"active", "inactive"});
                cmbStatus.setSelectedItem(rs.getString("status"));
                
                int r = 0;
                gbc.gridx = 0; gbc.gridy = r;
                panel.add(new JLabel("Nama:"), gbc);
                gbc.gridx = 1;
                panel.add(txtName, gbc);
                
                r++;
                gbc.gridx = 0; gbc.gridy = r;
                panel.add(new JLabel("Username:"), gbc);
                gbc.gridx = 1;
                panel.add(txtUsername, gbc);
                
                r++;
                gbc.gridx = 0; gbc.gridy = r;
                panel.add(new JLabel("NIK:"), gbc);
                gbc.gridx = 1;
                panel.add(txtNIK, gbc);
                
                r++;
                gbc.gridx = 0; gbc.gridy = r;
                panel.add(new JLabel("Alamat:"), gbc);
                gbc.gridx = 1;
                panel.add(new JScrollPane(txtAddress), gbc);
                
                r++;
                gbc.gridx = 0; gbc.gridy = r;
                panel.add(new JLabel("Kota:"), gbc);
                gbc.gridx = 1;
                panel.add(txtCity, gbc);
                
                r++;
                gbc.gridx = 0; gbc.gridy = r;
                panel.add(new JLabel("Telepon:"), gbc);
                gbc.gridx = 1;
                panel.add(txtPhone, gbc);
                
                r++;
                gbc.gridx = 0; gbc.gridy = r;
                panel.add(new JLabel("Role:"), gbc);
                gbc.gridx = 1;
                panel.add(cmbRole, gbc);
                
                r++;
                gbc.gridx = 0; gbc.gridy = r;
                panel.add(new JLabel("Status:"), gbc);
                gbc.gridx = 1;
                panel.add(cmbStatus, gbc);
                
                r++;
                gbc.gridx = 0; gbc.gridy = r;
                gbc.gridwidth = 2;
                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                
                JButton btnUpdate = new JButton("Update");
                btnUpdate.setBackground(new Color(52, 152, 219));
                btnUpdate.setForeground(Color.WHITE);
                btnUpdate.addActionListener(e -> {
                    if (validateInputEdit(txtName, txtUsername, txtNIK, txtPhone)) {
                        updateUser(id, txtName.getText(), txtUsername.getText(),
                                txtNIK.getText(), txtAddress.getText(), txtCity.getText(),
                                txtPhone.getText(), (String) cmbRole.getSelectedItem(),
                                (String) cmbStatus.getSelectedItem());
                        dialog.dispose();
                        loadData();
                    }
                });
                
                JButton btnCancel = new JButton("Batal");
                btnCancel.addActionListener(e -> dialog.dispose());
                
                btnPanel.add(btnUpdate);
                btnPanel.add(btnCancel);
                panel.add(btnPanel, gbc);
                
                dialog.add(panel);
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
            // Get role_id
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
            // Get role_id
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
}