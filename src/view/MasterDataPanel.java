package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import config.DatabaseConfig;

// Base class untuk master data management
abstract class MasterDataPanel extends JPanel {
    protected DefaultTableModel tableModel;
    protected JTable table;
    protected JTextField txtSearch;
    protected String tableName;
    protected String displayName;
    
    public MasterDataPanel(String tableName, String displayName) {
        this.tableName = tableName;
        this.displayName = displayName;
        initComponents();
        loadData();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        
        JLabel lblTitle = new JLabel("Kelola " + displayName);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);
        
        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.setBackground(Color.WHITE);
        
        searchPanel.add(new JLabel("Cari:"));
        txtSearch = new JTextField(25);
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                searchData();
            }
        });
        searchPanel.add(txtSearch);
        
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadData());
        searchPanel.add(btnRefresh);
        
        // Table
        String[] columns = {"ID", "Nama", "Keterangan"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton btnAdd = createButton("Tambah", new Color(46, 204, 113), e -> showAddDialog());
        JButton btnEdit = createButton("Edit", new Color(52, 152, 219), e -> showEditDialog());
        JButton btnDelete = createButton("Hapus", new Color(231, 76, 60), e -> deleteData());
        
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(searchPanel, BorderLayout.NORTH);
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
        btn.setPreferredSize(new Dimension(100, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }
    
    protected void loadData() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id, name, information FROM " + tableName + " ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("information")
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
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Tambah " + displayName, true);
        dialog.setSize(450, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField txtName = new JTextField(25);
        JTextArea txtInfo = new JTextArea(4, 25);
        txtInfo.setLineWrap(true);
        txtInfo.setWrapStyleWord(true);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nama:"), gbc);
        gbc.gridx = 1;
        panel.add(txtName, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Keterangan:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(txtInfo), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (txtName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Nama harus diisi!");
                return;
            }
            saveData(null, txtName.getText(), txtInfo.getText());
            dialog.dispose();
            loadData();
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
        String name = (String) table.getValueAt(row, 1);
        String info = (String) table.getValueAt(row, 2);
        
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Edit " + displayName, true);
        dialog.setSize(450, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField txtName = new JTextField(name, 25);
        JTextArea txtInfo = new JTextArea(info != null ? info : "", 4, 25);
        txtInfo.setLineWrap(true);
        txtInfo.setWrapStyleWord(true);
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nama:"), gbc);
        gbc.gridx = 1;
        panel.add(txtName, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Keterangan:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(txtInfo), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnUpdate = new JButton("Update");
        btnUpdate.setBackground(new Color(52, 152, 219));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.addActionListener(e -> {
            if (txtName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Nama harus diisi!");
                return;
            }
            updateData(id, txtName.getText(), txtInfo.getText());
            dialog.dispose();
            loadData();
        });
        
        JButton btnCancel = new JButton("Batal");
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnUpdate);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void saveData(Integer id, String name, String information) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO " + tableName + " (name, information) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, information);
            ps.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Data berhasil disimpan!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void updateData(int id, String name, String information) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE " + tableName + " SET name = ?, information = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, information);
            ps.setInt(3, id);
            ps.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Data berhasil diupdate!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void deleteData() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih data yang akan dihapus!");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0);
        String name = (String) table.getValueAt(row, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Hapus " + displayName + ": " + name + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                String sql = "DELETE FROM " + tableName + " WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Data berhasil dihapus!");
                loadData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage() + 
                    "\nData mungkin sedang digunakan di produk.");
            }
        }
    }
}

// Concrete classes for each master data
class BrandManagementPanel extends MasterDataPanel {
    public BrandManagementPanel() {
        super("brands", "Merek");
    }
}

class TypeManagementPanel extends MasterDataPanel {
    public TypeManagementPanel() {
        super("types", "Tipe Produk");
    }
}

class SizeManagementPanel extends MasterDataPanel {
    public SizeManagementPanel() {
        super("sizes", "Size");
    }
}

class ColorManagementPanel extends MasterDataPanel {
    public ColorManagementPanel() {
        super("colors", "Warna");
    }
}