package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import config.DatabaseConfig;
import utils.*;

public class ProductManagementPanel extends JPanel {
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;
    private JComboBox<String> cmbStatusFilter;
    private List<File> selectedPhotos = new ArrayList<>();
    
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
        
        // Table
        String[] columns = {"ID", "Nama Produk", "Merek", "Tipe", "Size", "Warna", 
                           "Harga Beli", "Harga Jual", "Stok", "Status", "Foto"};
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
        JButton btnViewPhotos = createButton("Lihat Foto", new Color(155, 89, 182), e -> viewProductPhotos());
        
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnUpdateStock);
        buttonPanel.add(btnViewPhotos);
        
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
                        "p.stock, p.status, COUNT(pp.id) as photo_count FROM products p " +
                        "JOIN brands b ON p.brand_id = b.id " +
                        "JOIN types t ON p.type_id = t.id " +
                        "JOIN sizes s ON p.size_id = s.id " +
                        "JOIN colors c ON p.color_id = c.id " +
                        "LEFT JOIN product_photos pp ON p.id = pp.product_id";
            
            if (!"Semua".equals(statusFilter)) {
                sql += " WHERE p.status = ?";
            }
            sql += " GROUP BY p.id, p.name, b.name, t.name, s.name, c.name, p.cost_price, p.selling_price, p.stock, p.status";
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
                    rs.getString("status"),
                    rs.getInt("photo_count") + " foto"
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
        selectedPhotos.clear();
        
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tambah Produk", true);
        dialog.setSize(600, 750);
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
        
        // Photo selection panel
        JPanel photoPanel = new JPanel(new BorderLayout(5, 5));
        JLabel lblPhotoCount = new JLabel("0 foto dipilih");
        JButton btnSelectPhotos = new JButton("Pilih Foto");
        btnSelectPhotos.addActionListener(e -> {
            selectPhotos();
            lblPhotoCount.setText(selectedPhotos.size() + " foto dipilih");
        });
        photoPanel.add(btnSelectPhotos, BorderLayout.WEST);
        photoPanel.add(lblPhotoCount, BorderLayout.CENTER);
        
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
        addFormRow(panel, gbc, row++, "Foto Produk:", photoPanel);
        
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            if (validateProductInput(txtName, txtCostPrice, txtSellingPrice, txtStock)) {
                // Show progress dialog
                JDialog progressDialog = new JDialog(dialog, "Menyimpan...", true);
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                progressDialog.add(progressBar);
                progressDialog.setSize(300, 100);
                progressDialog.setLocationRelativeTo(dialog);
                
                SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() {
                        return saveProductWithPhotos(
                            txtName.getText(),
                            ((ComboItem)cmbBrand.getSelectedItem()).getId(),
                            ((ComboItem)cmbType.getSelectedItem()).getId(),
                            ((ComboItem)cmbSize.getSelectedItem()).getId(),
                            ((ComboItem)cmbColor.getSelectedItem()).getId(),
                            Double.parseDouble(txtCostPrice.getText()),
                            Double.parseDouble(txtSellingPrice.getText()),
                            Integer.parseInt(txtStock.getText()),
                            (String)cmbStatus.getSelectedItem(),
                            selectedPhotos
                        );
                    }
                    
                    @Override
                    protected void done() {
                        progressDialog.dispose();
                        try {
                            if (get()) {
                                JOptionPane.showMessageDialog(dialog, "Produk berhasil ditambahkan!");
                                dialog.dispose();
                                loadData();
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
                        }
                    }
                };
                
                worker.execute();
                progressDialog.setVisible(true);
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
    
    private void selectPhotos() {
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
            selectedPhotos.clear();
            selectedPhotos.addAll(Arrays.asList(files));
        }
    }
    
    private boolean saveProductWithPhotos(String name, int brandId, int typeId, 
                                         int sizeId, int colorId, double costPrice, 
                                         double sellingPrice, int stock, String status,
                                         List<File> photos) {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Insert product
            String sql = "INSERT INTO products (brand_id, type_id, size_id, color_id, " +
                        "name, cost_price, selling_price, stock, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
            
            // 2. Get generated product ID
            ResultSet rs = ps.getGeneratedKeys();
            int productId = 0;
            if (rs.next()) {
                productId = rs.getInt(1);
            }
            
            // 3. Upload photos to Supabase and save URLs
            if (!photos.isEmpty() && productId > 0) {
                for (File photo : photos) {
                    String photoUrl = SupabaseStorage.uploadProductPhoto(productId, photo);
                    
                    if (photoUrl != null) {
                        String photoSql = "INSERT INTO product_photos (product_id, photo_url) VALUES (?, ?)";
                        PreparedStatement photoPs = conn.prepareStatement(photoSql);
                        photoPs.setInt(1, productId);
                        photoPs.setString(2, photoUrl);
                        photoPs.executeUpdate();
                    }
                }
            }
            
            conn.commit();
            return true;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void showEditDialog() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang akan diedit!");
            return;
        }
        
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        
        selectedPhotos.clear();
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT * FROM products WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String name = rs.getString("name");
                int brandId = rs.getInt("brand_id");
                int typeId = rs.getInt("type_id");
                int sizeId = rs.getInt("size_id");
                int colorId = rs.getInt("color_id");
                double costPrice = rs.getDouble("cost_price");
                double sellingPrice = rs.getDouble("selling_price");
                int stock = rs.getInt("stock");
                String status = rs.getString("status");
                rs.close();
                
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Produk", true);
                dialog.setSize(600, 750);
                dialog.setLocationRelativeTo(this);
                
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);
                
                JTextField txtName = new JTextField(name, 25);
                JTextField txtCostPrice = new JTextField(String.valueOf(costPrice), 25);
                JTextField txtSellingPrice = new JTextField(String.valueOf(sellingPrice), 25);
                JTextField txtStock = new JTextField(String.valueOf(stock), 25);
                
                JComboBox<ComboItem> cmbBrand = loadComboData("brands");
                JComboBox<ComboItem> cmbType = loadComboData("types");
                JComboBox<ComboItem> cmbSize = loadComboData("sizes");
                JComboBox<ComboItem> cmbColor = loadComboData("colors");
                JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});
                
                selectComboItem(cmbBrand, brandId);
                selectComboItem(cmbType, typeId);
                selectComboItem(cmbSize, sizeId);
                selectComboItem(cmbColor, colorId);
                cmbStatus.setSelectedItem(status);
                
                // Photo management panel
                JPanel photoPanel = new JPanel(new BorderLayout(5, 5));
                JLabel lblPhotoCount = new JLabel("0 foto baru dipilih");
                JButton btnSelectPhotos = new JButton("Tambah Foto");
                btnSelectPhotos.addActionListener(e -> {
                    selectPhotos();
                    lblPhotoCount.setText(selectedPhotos.size() + " foto baru dipilih");
                });
                JButton btnManagePhotos = new JButton("Kelola Foto");
                btnManagePhotos.addActionListener(e -> manageProductPhotos(id));
                
                JPanel photoButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                photoButtonPanel.add(btnSelectPhotos);
                photoButtonPanel.add(btnManagePhotos);
                photoPanel.add(photoButtonPanel, BorderLayout.WEST);
                photoPanel.add(lblPhotoCount, BorderLayout.CENTER);
                
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
                addFormRow(panel, gbc, r++, "Foto Produk:", photoPanel);
                
                gbc.gridx = 0; gbc.gridy = r;
                gbc.gridwidth = 2;
                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                
                JButton btnUpdate = new JButton("Update");
                btnUpdate.setBackground(new Color(52, 152, 219));
                btnUpdate.setForeground(Color.WHITE);
                btnUpdate.addActionListener(e -> {
                    if (validateProductInput(txtName, txtCostPrice, txtSellingPrice, txtStock)) {
                        JDialog progressDialog = new JDialog(dialog, "Menyimpan...", true);
                        JProgressBar progressBar = new JProgressBar();
                        progressBar.setIndeterminate(true);
                        progressDialog.add(progressBar);
                        progressDialog.setSize(300, 100);
                        progressDialog.setLocationRelativeTo(dialog);
                        
                        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                            @Override
                            protected Boolean doInBackground() {
                                return updateProductWithPhotos(id, txtName.getText(),
                                    ((ComboItem)cmbBrand.getSelectedItem()).getId(),
                                    ((ComboItem)cmbType.getSelectedItem()).getId(),
                                    ((ComboItem)cmbSize.getSelectedItem()).getId(),
                                    ((ComboItem)cmbColor.getSelectedItem()).getId(),
                                    Double.parseDouble(txtCostPrice.getText()),
                                    Double.parseDouble(txtSellingPrice.getText()),
                                    Integer.parseInt(txtStock.getText()),
                                    (String)cmbStatus.getSelectedItem(),
                                    selectedPhotos);
                            }
                            
                            @Override
                            protected void done() {
                                progressDialog.dispose();
                                try {
                                    if (get()) {
                                        JOptionPane.showMessageDialog(dialog, "Produk berhasil diupdate!");
                                        dialog.dispose();
                                        loadData();
                                    }
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
                                }
                            }
                        };
                        
                        worker.execute();
                        progressDialog.setVisible(true);
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
    
    private boolean updateProductWithPhotos(int id, String name, int brandId, int typeId, 
                                           int sizeId, int colorId, double costPrice, 
                                           double sellingPrice, int stock, String status,
                                           List<File> newPhotos) {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Update product
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
            
            // 2. Upload new photos if any
            if (!newPhotos.isEmpty()) {
                for (File photo : newPhotos) {
                    String photoUrl = SupabaseStorage.uploadProductPhoto(id, photo);
                    
                    if (photoUrl != null) {
                        String photoSql = "INSERT INTO product_photos (product_id, photo_url) VALUES (?, ?)";
                        PreparedStatement photoPs = conn.prepareStatement(photoSql);
                        photoPs.setInt(1, id);
                        photoPs.setString(2, photoUrl);
                        photoPs.executeUpdate();
                    }
                }
            }
            
            conn.commit();
            return true;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void manageProductPhotos(int productId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Kelola Foto Produk", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel photoGridPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        JScrollPane scrollPane = new JScrollPane(photoGridPanel);
        
        // Load photos
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id, photo_url FROM product_photos WHERE product_id = ? ORDER BY created_at";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                int photoId = rs.getInt("id");
                String photoUrl = rs.getString("photo_url");
                
                JPanel photoPanel = new JPanel(new BorderLayout());
                photoPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                
                JLabel lblPhoto = new JLabel("Loading...", SwingConstants.CENTER);
                lblPhoto.setPreferredSize(new Dimension(200, 200));
                
                // Load image asynchronously
                SwingWorker<ImageIcon, Void> imageLoader = new SwingWorker<ImageIcon, Void>() {
                    @Override
                    protected ImageIcon doInBackground() {
                        try {
                            BufferedImage img = ImageIO.read(new java.net.URL(photoUrl));
                            if (img != null) {
                                Image scaledImg = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
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
                btnDelete.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Hapus foto ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        deleteProductPhoto(photoId, photoUrl);
                        dialog.dispose();
                        manageProductPhotos(productId);
                    }
                });
                
                photoPanel.add(lblPhoto, BorderLayout.CENTER);
                photoPanel.add(btnDelete, BorderLayout.SOUTH);
                
                photoGridPanel.add(photoPanel);
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(dialog, "Error: " + e.getMessage());
        }
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        JButton btnClose = new JButton("Tutup");
        btnClose.addActionListener(e -> dialog.dispose());
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(btnClose);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private void deleteProductPhoto(int photoId, String photoUrl) {
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
    
    private void viewProductPhotos() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!");
            return;
        }
        
        int modelRow = table.convertRowIndexToModel(viewRow);
        int productId = (int) tableModel.getValueAt(modelRow, 0);
        String productName = (String) tableModel.getValueAt(modelRow, 1);
        
        manageProductPhotos(productId);
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
            "Hapus produk: " + name + "?\nSemua foto produk juga akan dihapus.", 
            "Konfirmasi", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                conn.setAutoCommit(false);
                
                // Delete photos from Supabase and database
                String photoSql = "SELECT photo_url FROM product_photos WHERE product_id = ?";
                PreparedStatement photoPs = conn.prepareStatement(photoSql);
                photoPs.setInt(1, id);
                ResultSet rs = photoPs.executeQuery();
                
                while (rs.next()) {
                    String photoUrl = rs.getString("photo_url");
                    SupabaseStorage.deleteProductPhoto(photoUrl);
                }
                
                String deletePhotosSql = "DELETE FROM product_photos WHERE product_id = ?";
                PreparedStatement delPhotosPs = conn.prepareStatement(deletePhotosSql);
                delPhotosPs.setInt(1, id);
                delPhotosPs.executeUpdate();
                
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
    
    private void updateStock() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!");
            return;
        }
        
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        int currentStock = (int) tableModel.getValueAt(modelRow, 8);
        
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