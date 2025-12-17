package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import config.DatabaseConfig;
import utils.*;

public class ProductManagementPanel extends JPanel {
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;
    private JComboBox<String> cmbStatusFilter;
    
    public ProductManagementPanel() {
        initComponents();
        loadData();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        
        JLabel lblTitle = new JLabel("Kelola Produk");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);
        
        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);
        
        filterPanel.add(new JLabel("Cari:"));
        txtSearch = new JTextField(25);
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                searchData();
            }
        });
        filterPanel.add(txtSearch);
        
        filterPanel.add(new JLabel("Status:"));
        cmbStatusFilter = new JComboBox<>(new String[]{"Semua", "available", "out_of_stock", "discontinued"});
        cmbStatusFilter.addActionListener(e -> loadData());
        filterPanel.add(cmbStatusFilter);
        
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadData());
        filterPanel.add(btnRefresh);
        
        // Table
        String[] columns = {"ID", "Nama Produk", "Merek", "Tipe", "Size", "Warna", 
                           "Harga Beli", "Harga Jual", "Stok", "Status"};
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
        table.getColumnModel().getColumn(6).setCellRenderer(new CurrencyRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(new CurrencyRenderer());
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton btnAdd = createButton("Tambah Produk", new Color(46, 204, 113), e -> showAddDialog());
        JButton btnEdit = createButton("Edit Produk", new Color(52, 152, 219), e -> showEditDialog());
        JButton btnDelete = createButton("Hapus", new Color(231, 76, 60), e -> deleteProduct());
        JButton btnUpdateStock = createButton("Update Stok", new Color(241, 196, 15), e -> updateStock());
        
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnUpdateStock);
        
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
        btn.setPreferredSize(new Dimension(130, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }
    
    private void loadData() {
        tableModel.setRowCount(0);
        String statusFilter = (String) cmbStatusFilter.getSelectedItem();
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT p.id, p.name, b.name as brand, t.name as type, " +
                        "s.name as size, c.name as color, p.cost_price, p.selling_price, " +
                        "p.stock, p.status FROM products p " +
                        "JOIN brands b ON p.brand_id = b.id " +
                        "JOIN types t ON p.type_id = t.id " +
                        "JOIN sizes s ON p.size_id = s.id " +
                        "JOIN colors c ON p.color_id = c.id";
            
            if (!"Semua".equals(statusFilter)) {
                sql += " WHERE p.status = ?";
            }
            sql += " ORDER BY p.name";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!"Semua".equals(statusFilter)) {
                ps.setString(1, statusFilter);
            }
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("brand"),
                    rs.getString("type"),
                    rs.getString("size"),
                    rs.getString("color"),
                    rs.getDouble("cost_price"),
                    rs.getDouble("selling_price"),
                    rs.getInt("stock"),
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
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tambah Produk", true);
        dialog.setSize(550, 650);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField txtName = new JTextField(25);
        JComboBox<ComboItem> cmbBrand = loadComboData("brands");
        JComboBox<ComboItem> cmbType = loadComboData("types");
        JComboBox<ComboItem> cmbSize = loadComboData("sizes");
        JComboBox<ComboItem> cmbColor = loadComboData("colors");
        JTextField txtCostPrice = new JTextField(25);
        JTextField txtSellingPrice = new JTextField(25);
        JTextField txtStock = new JTextField(25);
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});
        
        int row = 0;
        addFormRow(panel, gbc, row++, "Nama Produk:", txtName);
        addFormRow(panel, gbc, row++, "Merek:", cmbBrand);
        addFormRow(panel, gbc, row++, "Tipe:", cmbType);
        addFormRow(panel, gbc, row++, "Size:", cmbSize);
        addFormRow(panel, gbc, row++, "Warna:", cmbColor);
        addFormRow(panel, gbc, row++, "Harga Beli:", txtCostPrice);
        addFormRow(panel, gbc, row++, "Harga Jual:", txtSellingPrice);
        addFormRow(panel, gbc, row++, "Stok Awal:", txtStock);
        addFormRow(panel, gbc, row++, "Status:", cmbStatus);
        
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (validateProductInput(txtName, txtCostPrice, txtSellingPrice, txtStock)) {
                saveProduct(null, txtName.getText(),
                    ((ComboItem)cmbBrand.getSelectedItem()).getId(),
                    ((ComboItem)cmbType.getSelectedItem()).getId(),
                    ((ComboItem)cmbSize.getSelectedItem()).getId(),
                    ((ComboItem)cmbColor.getSelectedItem()).getId(),
                    Double.parseDouble(txtCostPrice.getText()),
                    Double.parseDouble(txtSellingPrice.getText()),
                    Integer.parseInt(txtStock.getText()),
                    (String)cmbStatus.getSelectedItem());
                dialog.dispose();
                loadData();
            }
        });
        
        JButton btnCancel = new JButton("Batal");
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);
        
        dialog.add(new JScrollPane(panel));
        dialog.setVisible(true);
    }
    
    private void showEditDialog() {
        int viewRow = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang akan diedit!");
            return;
        }
        
        int id = (int) table.getValueAt(modelRow, 0);
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM products WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Produk", true);
                dialog.setSize(550, 650);
                dialog.setLocationRelativeTo(this);
                
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);
                
                // 🔐 SALIN DATA RESULTSET KE VARIABEL
                String name = rs.getString("name");
                int brandId = rs.getInt("brand_id");
                int typeId  = rs.getInt("type_id");
                int sizeId  = rs.getInt("size_id");
                int colorId = rs.getInt("color_id");
                double costPrice = rs.getDouble("cost_price");
                double sellingPrice = rs.getDouble("selling_price");
                int stock = rs.getInt("stock");
                String status = rs.getString("status");

                // 🔒 AMAN — rs sudah tidak dipakai lagi
                rs.close();

                JTextField txtName = new JTextField(name, 25);
                JTextField txtCostPrice = new JTextField(String.valueOf(costPrice), 25);
                JTextField txtSellingPrice = new JTextField(String.valueOf(sellingPrice), 25);
                JTextField txtStock = new JTextField(String.valueOf(stock), 25);

                JComboBox<ComboItem> cmbBrand = loadComboData("brands");
                JComboBox<ComboItem> cmbType  = loadComboData("types");
                JComboBox<ComboItem> cmbSize  = loadComboData("sizes");
                JComboBox<ComboItem> cmbColor = loadComboData("colors");
                JComboBox<String> cmbStatus =
                    new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});

                selectComboItem(cmbBrand, brandId);
                selectComboItem(cmbType, typeId);
                selectComboItem(cmbSize, sizeId);
                selectComboItem(cmbColor, colorId);
                cmbStatus.setSelectedItem(status);
                
                int r = 0;
                addFormRow(panel, gbc, r++, "Nama Produk:", txtName);
                addFormRow(panel, gbc, r++, "Merek:", cmbBrand);
                addFormRow(panel, gbc, r++, "Tipe:", cmbType);
                addFormRow(panel, gbc, r++, "Size:", cmbSize);
                addFormRow(panel, gbc, r++, "Warna:", cmbColor);
                addFormRow(panel, gbc, r++, "Harga Beli:", txtCostPrice);
                addFormRow(panel, gbc, r++, "Harga Jual:", txtSellingPrice);
                addFormRow(panel, gbc, r++, "Stok:", txtStock);
                addFormRow(panel, gbc, r++, "Status:", cmbStatus);
                
                gbc.gridx = 0; gbc.gridy = r;
                gbc.gridwidth = 2;
                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                
                JButton btnUpdate = new JButton("Update");
                btnUpdate.setBackground(new Color(52, 152, 219));
                btnUpdate.setForeground(Color.WHITE);
                btnUpdate.addActionListener(e -> {
                    if (validateProductInput(txtName, txtCostPrice, txtSellingPrice, txtStock)) {
                        updateProduct(id, txtName.getText(),
                            ((ComboItem)cmbBrand.getSelectedItem()).getId(),
                            ((ComboItem)cmbType.getSelectedItem()).getId(),
                            ((ComboItem)cmbSize.getSelectedItem()).getId(),
                            ((ComboItem)cmbColor.getSelectedItem()).getId(),
                            Double.parseDouble(txtCostPrice.getText()),
                            Double.parseDouble(txtSellingPrice.getText()),
                            Integer.parseInt(txtStock.getText()),
                            (String)cmbStatus.getSelectedItem());
                        dialog.dispose();
                        loadData();
                    }
                });
                
                JButton btnCancel = new JButton("Batal");
                btnCancel.addActionListener(e -> dialog.dispose());
                
                btnPanel.add(btnUpdate);
                btnPanel.add(btnCancel);
                panel.add(btnPanel, gbc);
                
                dialog.add(new JScrollPane(panel));
                dialog.setVisible(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
    }
    
    private JComboBox<ComboItem> loadComboData(String tableName) {
        JComboBox<ComboItem> combo = new JComboBox<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id, name FROM " + tableName + " ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                combo.addItem(new ComboItem(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading " + tableName + ": " + e.getMessage());
        }
        return combo;
    }
    
    private void selectComboItem(JComboBox<ComboItem> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId() == id) {
                combo.setSelectedIndex(i);
                break;
            }
        }
    }
    
    private boolean validateProductInput(JTextField name, JTextField costPrice, 
                                        JTextField sellingPrice, JTextField stock) {
        if (!InputValidator.isNotEmpty(name.getText())) {
            JOptionPane.showMessageDialog(this, "Nama produk harus diisi!");
            return false;
        }
        if (!InputValidator.isValidPrice(costPrice.getText())) {
            JOptionPane.showMessageDialog(this, "Harga beli tidak valid!");
            return false;
        }
        if (!InputValidator.isValidPrice(sellingPrice.getText())) {
            JOptionPane.showMessageDialog(this, "Harga jual tidak valid!");
            return false;
        }
        if (!InputValidator.isValidStock(stock.getText())) {
            JOptionPane.showMessageDialog(this, "Stok tidak valid!");
            return false;
        }
        return true;
    }
    
    private void saveProduct(Integer id, String name, int brandId, int typeId, 
                            int sizeId, int colorId, double costPrice, 
                            double sellingPrice, int stock, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO products (brand_id, type_id, size_id, color_id, " +
                        "name, cost_price, selling_price, stock, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, brandId);
            ps.setInt(2, typeId);
            ps.setInt(3, sizeId);
            ps.setInt(4, colorId);
            ps.setString(5, name);
            ps.setDouble(6, costPrice);
            ps.setDouble(7, sellingPrice);
            ps.setInt(8, stock);
            ps.setString(9, status);
            
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Produk berhasil ditambahkan!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void updateProduct(int id, String name, int brandId, int typeId, 
                              int sizeId, int colorId, double costPrice, 
                              double sellingPrice, int stock, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE products SET brand_id = ?, type_id = ?, size_id = ?, " +
                        "color_id = ?, name = ?, cost_price = ?, selling_price = ?, " +
                        "stock = ?, status = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, brandId);
            ps.setInt(2, typeId);
            ps.setInt(3, sizeId);
            ps.setInt(4, colorId);
            ps.setString(5, name);
            ps.setDouble(6, costPrice);
            ps.setDouble(7, sellingPrice);
            ps.setInt(8, stock);
            ps.setString(9, status);
            ps.setInt(10, id);
            
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Produk berhasil diupdate!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void deleteProduct() {
        int viewRow = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang akan dihapus!");
            return;
        }
        
        int id = (int) table.getValueAt(modelRow, 0);
        String name = (String) table.getValueAt(modelRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Hapus produk: " + name + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                String sql = "DELETE FROM products WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Produk berhasil dihapus!");
                loadData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
    
    private void updateStock() {
        int viewRow = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!");
            return;
        }
        
        int id = (int) table.getValueAt(modelRow, 0);
        String name = (String) table.getValueAt(modelRow, 1);
        int currentStock = (int) table.getValueAt(modelRow, 8);
        
        String input = JOptionPane.showInputDialog(this, 
            "Stok saat ini: " + currentStock + "\nStok baru:", currentStock);
        
        if (input != null) {
            try {
                int newStock = Integer.parseInt(input);
                if (newStock < 0) {
                    JOptionPane.showMessageDialog(this, "Stok tidak boleh negatif!");
                    return;
                }
                
                try (Connection conn = DatabaseConfig.getConnection()) {
                    String sql = "UPDATE products SET stock = ? WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, newStock);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                    
                    JOptionPane.showMessageDialog(this, "Stok berhasil diupdate!");
                    loadData();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Input tidak valid!");
            }
        }
    }
    
    private static class ComboItem {
        private int id;
        private String name;
        
        public ComboItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        
        @Override
        public String toString() { return name; }
    }
    
    private static class CurrencyRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, 
                isSelected, hasFocus, row, column);
            if (value instanceof Double) {
                setText(FormatterUtils.formatCurrency((Double) value));
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            return c;
        }
    }
}