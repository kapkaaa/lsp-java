package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.io.File;
import javax.imageio.ImageIO;
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
        
        JLabel lblTitle = new JLabel("Kelola Produk & Varian");
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
        String[] columns = {"ID", "Nama Produk", "Merek", "Tipe", "Harga Beli", "Harga Jual", "Total Varian"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(4).setCellRenderer(new CurrencyRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new CurrencyRenderer());
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton btnAdd = createButton("Tambah Produk", new Color(46, 204, 113), e -> showAddProductDialog());
        JButton btnEdit = createButton("Edit Produk", new Color(52, 152, 219), e -> showEditProductDialog());
        JButton btnDelete = createButton("Hapus", new Color(231, 76, 60), e -> deleteProduct());
        JButton btnViewVariants = createButton("Kelola Varian", new Color(155, 89, 182), e -> manageProductVariants());
        
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnViewVariants);
        
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
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT p.id, p.name, b.name as brand, t.name as type, " +
                        "p.cost_price, p.selling_price, " +
                        "COUNT(DISTINCT pd.id) as variant_count " +
                        "FROM products p " +
                        "JOIN brands b ON p.brand_id = b.id " +
                        "JOIN types t ON p.type_id = t.id " +
                        "LEFT JOIN product_details pd ON p.id = pd.product_id " +
                        "GROUP BY p.id, p.name, b.name, t.name, p.cost_price, p.selling_price " +
                        "ORDER BY p.name";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("brand"),
                    rs.getString("type"),
                    rs.getDouble("cost_price"),
                    rs.getDouble("selling_price"),
                    rs.getInt("variant_count") + " varian"
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
    
    private void showAddProductDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tambah Produk Baru", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField txtName = new JTextField(25);
        JComboBox<ComboItem> cmbBrand = loadComboData("brands");
        JComboBox<ComboItem> cmbType = loadComboData("types");
        JTextField txtCostPrice = new JTextField(25);
        JTextField txtSellingPrice = new JTextField(25);
        
        int row = 0;
        addFormRow(panel, gbc, row++, "Nama Produk:", txtName);
        addFormRow(panel, gbc, row++, "Merek:", cmbBrand);
        addFormRow(panel, gbc, row++, "Tipe:", cmbType);
        addFormRow(panel, gbc, row++, "Harga Beli:", txtCostPrice);
        addFormRow(panel, gbc, row++, "Harga Jual:", txtSellingPrice);
        
        // Info label
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel lblInfo = new JLabel("<html><i>* Setelah produk dibuat, Anda dapat menambahkan varian (warna, size, stok, foto)</i></html>");
        lblInfo.setForeground(Color.GRAY);
        panel.add(lblInfo, gbc);
        
        gbc.gridy = row;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnSave = new JButton("Simpan & Tambah Varian");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> {
            if (validateProductInput(txtName, txtCostPrice, txtSellingPrice)) {
                int productId = saveProduct(
                    txtName.getText(),
                    ((ComboItem)cmbBrand.getSelectedItem()).getId(),
                    ((ComboItem)cmbType.getSelectedItem()).getId(),
                    Double.parseDouble(txtCostPrice.getText()),
                    Double.parseDouble(txtSellingPrice.getText())
                );
                
                if (productId > 0) {
                    JOptionPane.showMessageDialog(dialog, "Produk berhasil dibuat!\nSilakan tambahkan varian produk.");
                    dialog.dispose();
                    loadData();
                    // Open variant management
                    manageProductVariants(productId);
                }
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
    
    private int saveProduct(String name, int brandId, int typeId, double costPrice, double sellingPrice) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO products (brand_id, type_id, name, cost_price, selling_price) " +
                        "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, brandId);
            ps.setInt(2, typeId);
            ps.setString(3, name);
            ps.setDouble(4, costPrice);
            ps.setDouble(5, sellingPrice);
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
        return 0;
    }
    
    private void showEditProductDialog() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang akan diedit!");
            return;
        }
        
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM products WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Produk", true);
                dialog.setSize(600, 400);
                dialog.setLocationRelativeTo(this);
                
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);
                
                JTextField txtName = new JTextField(rs.getString("name"), 25);
                JTextField txtCostPrice = new JTextField(String.valueOf(rs.getDouble("cost_price")), 25);
                JTextField txtSellingPrice = new JTextField(String.valueOf(rs.getDouble("selling_price")), 25);
                
                JComboBox<ComboItem> cmbBrand = loadComboData("brands");
                JComboBox<ComboItem> cmbType = loadComboData("types");
                
                selectComboItem(cmbBrand, rs.getInt("brand_id"));
                selectComboItem(cmbType, rs.getInt("type_id"));
                
                int row = 0;
                addFormRow(panel, gbc, row++, "Nama Produk:", txtName);
                addFormRow(panel, gbc, row++, "Merek:", cmbBrand);
                addFormRow(panel, gbc, row++, "Tipe:", cmbType);
                addFormRow(panel, gbc, row++, "Harga Beli:", txtCostPrice);
                addFormRow(panel, gbc, row++, "Harga Jual:", txtSellingPrice);
                
                gbc.gridx = 0; gbc.gridy = row;
                gbc.gridwidth = 2;
                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                
                JButton btnUpdate = new JButton("Update");
                btnUpdate.setBackground(new Color(52, 152, 219));
                btnUpdate.setForeground(Color.WHITE);
                btnUpdate.setFocusPainted(false);
                btnUpdate.addActionListener(e -> {
                    if (validateProductInput(txtName, txtCostPrice, txtSellingPrice)) {
                        if (updateProduct(id, txtName.getText(),
                            ((ComboItem)cmbBrand.getSelectedItem()).getId(),
                            ((ComboItem)cmbType.getSelectedItem()).getId(),
                            Double.parseDouble(txtCostPrice.getText()),
                            Double.parseDouble(txtSellingPrice.getText()))) {
                            
                            JOptionPane.showMessageDialog(dialog, "Produk berhasil diupdate!");
                            dialog.dispose();
                            loadData();
                        }
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
    
    private boolean updateProduct(int id, String name, int brandId, int typeId, 
                                 double costPrice, double sellingPrice) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE products SET brand_id = ?, type_id = ?, name = ?, " +
                        "cost_price = ?, selling_price = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, brandId);
            ps.setInt(2, typeId);
            ps.setString(3, name);
            ps.setDouble(4, costPrice);
            ps.setDouble(5, sellingPrice);
            ps.setInt(6, id);
            
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return false;
        }
    }
    
    private void manageProductVariants() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!");
            return;
        }
        
        int modelRow = table.convertRowIndexToModel(viewRow);
        int productId = (int) tableModel.getValueAt(modelRow, 0);
        
        manageProductVariants(productId);
    }
    
    private void manageProductVariants(int productId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Kelola Varian Produk", true);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Product info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(new Color(236, 240, 241));
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT p.name, b.name as brand, t.name as type " +
                        "FROM products p " +
                        "JOIN brands b ON p.brand_id = b.id " +
                        "JOIN types t ON p.type_id = t.id " +
                        "WHERE p.id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                JLabel lblInfo = new JLabel(String.format("Produk: %s | Merek: %s | Tipe: %s", 
                    rs.getString("name"), rs.getString("brand"), rs.getString("type")));
                lblInfo.setFont(new Font("Arial", Font.BOLD, 14));
                infoPanel.add(lblInfo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Variants table
        String[] columns = {"ID", "Warna", "Size", "Stok", "Status", "Foto"};
        DefaultTableModel variantModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable variantTable = new JTable(variantModel);
        variantTable.setRowHeight(30);
        JScrollPane scrollPane = new JScrollPane(variantTable);
        
        // Load variants
        loadVariants(variantModel, productId);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        
        JButton btnAddVariant = new JButton("Tambah Varian");
        btnAddVariant.setBackground(new Color(46, 204, 113));
        btnAddVariant.setForeground(Color.WHITE);
        btnAddVariant.setFocusPainted(false);
        btnAddVariant.addActionListener(e -> {
            showAddVariantDialog(productId, variantModel);
        });
        
        JButton btnEditVariant = new JButton("Edit Varian");
        btnEditVariant.setBackground(new Color(52, 152, 219));
        btnEditVariant.setForeground(Color.WHITE);
        btnEditVariant.setFocusPainted(false);
        btnEditVariant.addActionListener(e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int variantId = (int) variantModel.getValueAt(row, 0);
                showEditVariantDialog(variantId, productId, variantModel);
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian yang akan diedit!");
            }
        });
        
        JButton btnDeleteVariant = new JButton("Hapus Varian");
        btnDeleteVariant.setBackground(new Color(231, 76, 60));
        btnDeleteVariant.setForeground(Color.WHITE);
        btnDeleteVariant.setFocusPainted(false);
        btnDeleteVariant.addActionListener(e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int variantId = (int) variantModel.getValueAt(row, 0);
                deleteVariant(variantId);
                loadVariants(variantModel, productId);
                loadData(); // Refresh main table
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian yang akan dihapus!");
            }
        });
        
        JButton btnManagePhotos = new JButton("Kelola Foto");
        btnManagePhotos.setBackground(new Color(155, 89, 182));
        btnManagePhotos.setForeground(Color.WHITE);
        btnManagePhotos.setFocusPainted(false);
        btnManagePhotos.addActionListener(e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int variantId = (int) variantModel.getValueAt(row, 0);
                manageVariantPhotos(variantId);
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian terlebih dahulu!");
            }
        });
        
        JButton btnClose = new JButton("Tutup");
        btnClose.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(btnAddVariant);
        buttonPanel.add(btnEditVariant);
        buttonPanel.add(btnDeleteVariant);
        buttonPanel.add(btnManagePhotos);
        buttonPanel.add(btnClose);
        
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private void loadVariants(DefaultTableModel model, int productId) {
        model.setRowCount(0);
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT pd.id, c.name as color, s.name as size, pd.stock, pd.status, " +
                        "COUNT(pp.id) as photo_count " +
                        "FROM product_details pd " +
                        "JOIN colors c ON pd.color_id = c.id " +
                        "JOIN sizes s ON pd.size_id = s.id " +
                        "LEFT JOIN product_photos pp ON pd.id = pp.product_detail_id " +
                        "WHERE pd.product_id = ? " +
                        "GROUP BY pd.id, c.name, s.name, pd.stock, pd.status " +
                        "ORDER BY c.name, s.name";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("color"),
                    rs.getString("size"),
                    rs.getInt("stock"),
                    rs.getString("status"),
                    rs.getInt("photo_count") + " foto"
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void showAddVariantDialog(int productId, DefaultTableModel variantModel) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Tambah Varian Produk", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JComboBox<ComboItem> cmbColor = loadComboData("colors");
        JComboBox<ComboItem> cmbSize = loadComboData("sizes");
        JTextField txtStock = new JTextField("0", 20);
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});
        
        int row = 0;
        addFormRow(panel, gbc, row++, "Warna:", cmbColor);
        addFormRow(panel, gbc, row++, "Size:", cmbSize);
        addFormRow(panel, gbc, row++, "Stok:", txtStock);
        addFormRow(panel, gbc, row++, "Status:", cmbStatus);
        
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel lblInfo = new JLabel("<html><i>* Foto dapat ditambahkan setelah varian dibuat</i></html>");
        lblInfo.setForeground(Color.GRAY);
        panel.add(lblInfo, gbc);
        
        gbc.gridy = row;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> {
            try {
                int stock = Integer.parseInt(txtStock.getText());
                if (stock < 0) {
                    JOptionPane.showMessageDialog(dialog, "Stok tidak boleh negatif!");
                    return;
                }
                
                if (saveVariant(productId,
                    ((ComboItem)cmbColor.getSelectedItem()).getId(),
                    ((ComboItem)cmbSize.getSelectedItem()).getId(),
                    stock,
                    (String)cmbStatus.getSelectedItem())) {
                    
                    JOptionPane.showMessageDialog(dialog, "Varian berhasil ditambahkan!");
                    loadVariants(variantModel, productId);
                    loadData();
                    dialog.dispose();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Stok harus berupa angka!");
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
    
    private boolean saveVariant(int productId, int colorId, int sizeId, int stock, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Check if variant already exists
            String checkSql = "SELECT id FROM product_details WHERE product_id = ? AND color_id = ? AND size_id = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, productId);
            checkPs.setInt(2, colorId);
            checkPs.setInt(3, sizeId);
            ResultSet checkRs = checkPs.executeQuery();
            
            if (checkRs.next()) {
                JOptionPane.showMessageDialog(this, "Varian dengan warna dan size ini sudah ada!");
                return false;
            }
            
            String sql = "INSERT INTO product_details (product_id, color_id, size_id, stock, status) " +
                        "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, productId);
            ps.setInt(2, colorId);
            ps.setInt(3, sizeId);
            ps.setInt(4, stock);
            ps.setString(5, status);
            
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return false;
        }
    }
    
    private void showEditVariantDialog(int variantId, int productId, DefaultTableModel variantModel) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM product_details WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, variantId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                    "Edit Varian", true);
                dialog.setSize(500, 350);
                dialog.setLocationRelativeTo(this);
                
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);
                
                JComboBox<ComboItem> cmbColor = loadComboData("colors");
                JComboBox<ComboItem> cmbSize = loadComboData("sizes");
                JTextField txtStock = new JTextField(String.valueOf(rs.getInt("stock")), 20);
                JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});
                
                selectComboItem(cmbColor, rs.getInt("color_id"));
                selectComboItem(cmbSize, rs.getInt("size_id"));
                cmbStatus.setSelectedItem(rs.getString("status"));
                
                int row = 0;
                addFormRow(panel, gbc, row++, "Warna:", cmbColor);
                addFormRow(panel, gbc, row++, "Size:", cmbSize);
                addFormRow(panel, gbc, row++, "Stok:", txtStock);
                addFormRow(panel, gbc, row++, "Status:", cmbStatus);
                
                gbc.gridx = 0; gbc.gridy = row;
                gbc.gridwidth = 2;
                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                
                JButton btnUpdate = new JButton("Update");
                btnUpdate.setBackground(new Color(52, 152, 219));
                btnUpdate.setForeground(Color.WHITE);
                btnUpdate.setFocusPainted(false);
                btnUpdate.addActionListener(e -> {
                    try {
                        int stock = Integer.parseInt(txtStock.getText());
                        if (stock < 0) {
                            JOptionPane.showMessageDialog(dialog, "Stok tidak boleh negatif!");
                            return;
                        }
                        
                        if (updateVariant(variantId,
                            ((ComboItem)cmbColor.getSelectedItem()).getId(),
                            ((ComboItem)cmbSize.getSelectedItem()).getId(),
                            stock,
                            (String)cmbStatus.getSelectedItem())) {
                            
                            JOptionPane.showMessageDialog(dialog, "Varian berhasil diupdate!");
                            loadVariants(variantModel, productId);
                            loadData();
                            dialog.dispose();
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(dialog, "Stok harus berupa angka!");
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
            e.printStackTrace();
        }
    }
    
    private boolean updateVariant(int variantId, int colorId, int sizeId, int stock, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE product_details SET color_id = ?, size_id = ?, stock = ?, status = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, colorId);
            ps.setInt(2, sizeId);
            ps.setInt(3, stock);
            ps.setString(4, status);
            ps.setInt(5, variantId);
            
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return false;
        }
    }
    
    private void deleteVariant(int variantId) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Hapus varian ini?\nSemua foto varian juga akan dihapus.", 
            "Konfirmasi", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.setAutoCommit(false);
                
                // Delete photos from Supabase and database
                String photoSql = "SELECT photo_url FROM product_photos WHERE product_detail_id = ?";
                PreparedStatement photoPs = conn.prepareStatement(photoSql);
                photoPs.setInt(1, variantId);
                ResultSet rs = photoPs.executeQuery();
                
                while (rs.next()) {
                    String photoUrl = rs.getString("photo_url");
                    SupabaseStorage.deleteProductPhoto(photoUrl);
                }
                
                String deletePhotosSql = "DELETE FROM product_photos WHERE product_detail_id = ?";
                PreparedStatement delPhotosPs = conn.prepareStatement(deletePhotosSql);
                delPhotosPs.setInt(1, variantId);
                delPhotosPs.executeUpdate();
                
                // Delete variant
                String sql = "DELETE FROM product_details WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, variantId);
                ps.executeUpdate();
                
                conn.commit();
                
                JOptionPane.showMessageDialog(this, "Varian berhasil dihapus!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
    
    private void manageVariantPhotos(int variantId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Kelola Foto Varian", true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Variant info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(new Color(236, 240, 241));
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT p.name, c.name as color, s.name as size " +
                        "FROM product_details pd " +
                        "JOIN products p ON pd.product_id = p.id " +
                        "JOIN colors c ON pd.color_id = c.id " +
                        "JOIN sizes s ON pd.size_id = s.id " +
                        "WHERE pd.id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, variantId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                JLabel lblInfo = new JLabel(String.format("Produk: %s | Warna: %s | Size: %s", 
                    rs.getString("name"), rs.getString("color"), rs.getString("size")));
                lblInfo.setFont(new Font("Arial", Font.BOLD, 13));
                infoPanel.add(lblInfo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        JPanel photoGridPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        JScrollPane scrollPane = new JScrollPane(photoGridPanel);
        
        // Load photos
        loadVariantPhotos(photoGridPanel, variantId, dialog);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton btnAddPhoto = new JButton("Tambah Foto");
        btnAddPhoto.setBackground(new Color(46, 204, 113));
        btnAddPhoto.setForeground(Color.WHITE);
        btnAddPhoto.setFocusPainted(false);
        btnAddPhoto.addActionListener(e -> {
            addVariantPhotos(variantId);
            dialog.dispose();
            manageVariantPhotos(variantId);
        });
        
        JButton btnClose = new JButton("Tutup");
        btnClose.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(btnAddPhoto);
        buttonPanel.add(btnClose);
        
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private void loadVariantPhotos(JPanel panel, int variantId, JDialog parentDialog) {
        panel.removeAll();
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id, photo_url FROM product_photos WHERE product_detail_id = ? ORDER BY created_at";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, variantId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                int photoId = rs.getInt("id");
                String photoUrl = rs.getString("photo_url");
                
                JPanel photoPanel = new JPanel(new BorderLayout());
                photoPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                
                JLabel lblPhoto = new JLabel("Loading...", SwingConstants.CENTER);
                lblPhoto.setPreferredSize(new Dimension(250, 250));
                
                // Load image asynchronously
                SwingWorker<ImageIcon, Void> imageLoader = new SwingWorker<ImageIcon, Void>() {
                    @Override
                    protected ImageIcon doInBackground() {
                        try {
                            BufferedImage img = ImageIO.read(new java.net.URL(photoUrl));
                            if (img != null) {
                                Image scaledImg = img.getScaledInstance(250, 250, Image.SCALE_SMOOTH);
                                return new ImageIcon(scaledImg);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            ImageIcon icon = get();
                            if (icon != null) {
                                lblPhoto.setIcon(icon);
                                lblPhoto.setText("");
                            } else {
                                lblPhoto.setText("Gagal memuat");
                            }
                        } catch (Exception e) {
                            lblPhoto.setText("Error");
                        }
                    }
                };
                imageLoader.execute();
                
                JButton btnDelete = new JButton("Hapus");
                btnDelete.setBackground(new Color(231, 76, 60));
                btnDelete.setForeground(Color.WHITE);
                btnDelete.setFocusPainted(false);
                btnDelete.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(parentDialog,
                        "Hapus foto ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        deleteVariantPhoto(photoId, photoUrl);
                        parentDialog.dispose();
                        manageVariantPhotos(variantId);
                    }
                });
                
                photoPanel.add(lblPhoto, BorderLayout.CENTER);
                photoPanel.add(btnDelete, BorderLayout.SOUTH);
                
                panel.add(photoPanel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        panel.revalidate();
        panel.repaint();
    }
    
    private void addVariantPhotos(int variantId) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                       name.endsWith(".png") || name.endsWith(".gif");
            }
            public String getDescription() {
                return "Image Files (*.jpg, *.jpeg, *.png, *.gif)";
            }
        });
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            
            // Show progress
            JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                "Mengupload Foto...", true);
            JProgressBar progressBar = new JProgressBar(0, files.length);
            progressBar.setStringPainted(true);
            progressDialog.add(progressBar);
            progressDialog.setSize(400, 100);
            progressDialog.setLocationRelativeTo(this);
            
            SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Connection conn = DatabaseConfig.getConnection();
                    int uploaded = 0;
                    
                    for (File file : files) {
                        String photoUrl = SupabaseStorage.uploadProductPhoto(variantId, file);
                        
                        if (photoUrl != null) {
                            String sql = "INSERT INTO product_photos (product_detail_id, photo_url) VALUES (?, ?)";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setInt(1, variantId);
                            ps.setString(2, photoUrl);
                            ps.executeUpdate();
                            uploaded++;
                        }
                        
                        publish(uploaded);
                    }
                    
                    conn.close();
                    return null;
                }
                
                @Override
                protected void process(List<Integer> chunks) {
                    int latest = chunks.get(chunks.size() - 1);
                    progressBar.setValue(latest);
                }
                
                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        get();
                        JOptionPane.showMessageDialog(ProductManagementPanel.this, 
                            files.length + " foto berhasil diupload!");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ProductManagementPanel.this, 
                            "Error: " + e.getMessage());
                    }
                }
            };
            
            worker.execute();
            progressDialog.setVisible(true);
        }
    }
    
    private void deleteVariantPhoto(int photoId, String photoUrl) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Delete from Supabase
            SupabaseStorage.deleteProductPhoto(photoUrl);
            
            // Delete from database
            String sql = "DELETE FROM product_photos WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, photoId);
            ps.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Foto berhasil dihapus!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void deleteProduct() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang akan dihapus!");
            return;
        }
        
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Hapus produk: " + name + "?\nSemua varian dan foto produk juga akan dihapus.", 
            "Konfirmasi", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.setAutoCommit(false);
                
                // Delete all photos from all variants
                String photoSql = "SELECT pp.photo_url FROM product_photos pp " +
                                 "JOIN product_details pd ON pp.product_detail_id = pd.id " +
                                 "WHERE pd.product_id = ?";
                PreparedStatement photoPs = conn.prepareStatement(photoSql);
                photoPs.setInt(1, id);
                ResultSet rs = photoPs.executeQuery();
                
                while (rs.next()) {
                    String photoUrl = rs.getString("photo_url");
                    SupabaseStorage.deleteProductPhoto(photoUrl);
                }
                
                // Delete photos from database (cascade will handle this, but explicit is better)
                String deletePhotosSql = "DELETE pp FROM product_photos pp " +
                                        "JOIN product_details pd ON pp.product_detail_id = pd.id " +
                                        "WHERE pd.product_id = ?";
                PreparedStatement delPhotosPs = conn.prepareStatement(deletePhotosSql);
                delPhotosPs.setInt(1, id);
                delPhotosPs.executeUpdate();
                
                // Delete variants
                String deleteVariantsSql = "DELETE FROM product_details WHERE product_id = ?";
                PreparedStatement delVariantsPs = conn.prepareStatement(deleteVariantsSql);
                delVariantsPs.setInt(1, id);
                delVariantsPs.executeUpdate();
                
                // Delete product
                String sql = "DELETE FROM products WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
                
                conn.commit();
                
                JOptionPane.showMessageDialog(this, "Produk berhasil dihapus!");
                loadData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
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
    
    private boolean validateProductInput(JTextField name, JTextField costPrice, JTextField sellingPrice) {
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
        return true;
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