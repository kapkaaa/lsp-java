package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.regex.Pattern;
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
    private Point mousePoint;

    public ProductManagementPanel() {
        initComponents();
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Kelola Produk & Varian");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);

        filterPanel.add(new JLabel("Cari:"));
        txtSearch = createStyledTextField(25);
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                searchData();
            }
        });
        filterPanel.add(txtSearch);

        filterPanel.add(new JLabel("Status:"));
        cmbStatusFilter = new JComboBox<>(new String[]{"Semua", "available", "out_of_stock", "discontinued"});
        cmbStatusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbStatusFilter.setBackground(Color.WHITE);
        cmbStatusFilter.setForeground(Color.decode("#222222"));
        cmbStatusFilter.addActionListener(e -> loadData());
        filterPanel.add(cmbStatusFilter);

        String[] columns = {"ID", "Nama Produk", "Merek", "Tipe", "Harga Beli", "Harga Jual", "Total Varian"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(30);
        table.setSelectionBackground(new Color(236, 240, 241));
        table.setSelectionForeground(Color.BLACK);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(4).setCellRenderer(new CurrencyRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new CurrencyRenderer());

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(236, 240, 241));
        header.setForeground(Color.BLACK);
        header.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);

        JButton btnAdd = createStyledButton("Tambah Produk", new Color(46, 204, 113), e -> showAddProductDialog());
        JButton btnEdit = createStyledButton("Edit Produk", new Color(52, 152, 219), e -> showEditProductDialog());
        JButton btnDelete = createStyledButton("Hapus", new Color(231, 76, 60), e -> deleteProduct());
        JButton btnViewVariants = createStyledButton("Kelola Varian", new Color(155, 89, 182), e -> manageProductVariants());

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

    private JButton createStyledButton(String text, Color bgColor, ActionListener listener) {
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
        btn.setPreferredSize(new Dimension(130, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
    }

    private void loadData() {
    tableModel.setRowCount(0);
    String statusFilter = (String) cmbStatusFilter.getSelectedItem();
    StringBuilder sql = new StringBuilder(
        "SELECT p.id, p.name, b.name as brand, t.name as type, " +
        "p.cost_price, p.selling_price, " +
        "COUNT(DISTINCT pd.id) as variant_count " +
        "FROM products p " +
        "JOIN brands b ON p.brand_id = b.id " +
        "JOIN types t ON p.type_id = t.id " +
        "LEFT JOIN product_details pd ON p.id = pd.product_id "
    );

    if (!"Semua".equals(statusFilter)) {
        sql.append("WHERE p.status = ? ");
    }

    sql.append("GROUP BY p.id, p.name, b.name, t.name, p.cost_price, p.selling_price " +
               "ORDER BY p.name");

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    
    try {
        conn = DatabaseConfig.getConnection();
        stmt = conn.prepareStatement(sql.toString());
        
        if (!"Semua".equals(statusFilter)) {
            stmt.setString(1, statusFilter);
        }

        rs = stmt.executeQuery();
        
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
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
    } finally {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

    private void searchData() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        if (keyword.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(keyword)));
        }
    }

    private void showAddProductDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tambah Produk Baru", true);
        dialog.setUndecorated(true);
        dialog.setSize(600, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = createDialogTitleBar("Tambah Produk Baru", dialog);
        dialog.add(titleBar, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout()) {
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
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 12, 8);

        JTextField txtName = createStyledTextField(25);
        JComboBox<ComboItem> cmbBrand = loadComboData("brands");
        JComboBox<ComboItem> cmbType = loadComboData("types");
        JTextField txtCostPrice = createStyledTextField(25);
        JTextField txtSellingPrice = createStyledTextField(25);

        int row = 0;
        addFormRow(panel, gbc, row++, "Nama Produk:", txtName);
        addFormRow(panel, gbc, row++, "Merek:", cmbBrand);
        addFormRow(panel, gbc, row++, "Tipe:", cmbType);
        addFormRow(panel, gbc, row++, "Harga Beli:", txtCostPrice);
        addFormRow(panel, gbc, row++, "Harga Jual:", txtSellingPrice);

        gbc.gridx = 0; gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel lblInfo = new JLabel("<html><i>* Setelah produk dibuat, Anda dapat menambahkan varian (warna, size, stok, foto)</i></html>");
        lblInfo.setForeground(Color.GRAY);
        panel.add(lblInfo, gbc);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        
        JButton btnSave = createStyledButton("Simpan & Tambah Varian", new Color(46, 204, 113), e -> {
            if (validateProductInput(txtName, txtCostPrice, txtSellingPrice)) {
                int productId = saveProduct(
                    txtName.getText(),
                    ((ComboItem) cmbBrand.getSelectedItem()).getId(),
                    ((ComboItem) cmbType.getSelectedItem()).getId(),
                    Double.parseDouble(txtCostPrice.getText()),
                    Double.parseDouble(txtSellingPrice.getText())
                );
                if (productId > 0) {
                    JOptionPane.showMessageDialog(dialog, "Produk berhasil dibuat!\nSilakan tambahkan varian produk.");
                    dialog.dispose();
                    loadData();
                    manageProductVariants(productId);
                }
            }
        });
        JButton btnCancel = createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);
        dialog.setVisible(true);
    }

    private int saveProduct(String name, int brandId, int typeId, double costPrice, double sellingPrice) {
        String sql = "INSERT INTO products (brand_id, type_id, name, cost_price, selling_price) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, brandId);
            ps.setInt(2, typeId);
            ps.setString(3, name);
            ps.setDouble(4, costPrice);
            ps.setDouble(5, sellingPrice);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        
        String productName = null;
        double costPrice = 0;
        double sellingPrice = 0;
        int brandId = 0;
        int typeId = 0;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    productName = rs.getString("name");
                    costPrice = rs.getDouble("cost_price");
                    sellingPrice = rs.getDouble("selling_price");
                    brandId = rs.getInt("brand_id");
                    typeId = rs.getInt("type_id");
                } else {
                    JOptionPane.showMessageDialog(this, "Produk tidak ditemukan!");
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return;
        }
        
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Produk", true);
        dialog.setUndecorated(true);
        dialog.setSize(600, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = createDialogTitleBar("Edit Produk", dialog);
        dialog.add(titleBar, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout()) {
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
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 12, 8);

        JTextField txtName = createStyledTextField(25);
        txtName.setText(productName);
        JTextField txtCostPrice = createStyledTextField(25);
        txtCostPrice.setText(String.valueOf(costPrice));
        JTextField txtSellingPrice = createStyledTextField(25);
        txtSellingPrice.setText(String.valueOf(sellingPrice));
        JComboBox<ComboItem> cmbBrand = loadComboData("brands");
        JComboBox<ComboItem> cmbType = loadComboData("types");
        selectComboItem(cmbBrand, brandId);
        selectComboItem(cmbType, typeId);

        int row = 0;
        addFormRow(panel, gbc, row++, "Nama Produk:", txtName);
        addFormRow(panel, gbc, row++, "Merek:", cmbBrand);
        addFormRow(panel, gbc, row++, "Tipe:", cmbType);
        addFormRow(panel, gbc, row++, "Harga Beli:", txtCostPrice);
        addFormRow(panel, gbc, row++, "Harga Jual:", txtSellingPrice);
 
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton btnUpdate = createStyledButton("Update", new Color(52, 152, 219), e -> {
            if (validateProductInput(txtName, txtCostPrice, txtSellingPrice)) {
                if (updateProduct(id,
                    txtName.getText(),
                    ((ComboItem) cmbBrand.getSelectedItem()).getId(),
                    ((ComboItem) cmbType.getSelectedItem()).getId(),
                    Double.parseDouble(txtCostPrice.getText()),
                    Double.parseDouble(txtSellingPrice.getText()))) {
                    JOptionPane.showMessageDialog(dialog, "Produk berhasil diupdate!");
                    dialog.dispose();
                    loadData();
                }
            }
        });
        JButton btnCancel = createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());
        btnPanel.add(btnUpdate);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);
        dialog.setVisible(true);
    }

    private boolean updateProduct(int id, String name, int brandId, int typeId, double costPrice, double sellingPrice) {
        String sql = "UPDATE products SET brand_id = ?, type_id = ?, name = ?, cost_price = ?, selling_price = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, brandId);
            ps.setInt(2, typeId);
            ps.setString(3, name);
            ps.setDouble(4, costPrice);
            ps.setDouble(5, sellingPrice);
            ps.setInt(6, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
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
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Kelola Varian Produk", true);
        dialog.setUndecorated(true);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = createDialogTitleBar("Kelola Varian Produk", dialog);
        dialog.add(titleBar, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(new Color(236, 240, 241));
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT p.name, b.name as brand, t.name as type " +
                "FROM products p " +
                "JOIN brands b ON p.brand_id = b.id " +
                "JOIN types t ON p.type_id = t.id " +
                "WHERE p.id = ?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JLabel lblInfo = new JLabel(String.format("Produk: %s | Merek: %s | Tipe: %s",
                        rs.getString("name"), rs.getString("brand"), rs.getString("type")));
                    lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    infoPanel.add(lblInfo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String[] columns = {"ID", "Warna", "Size", "Stok", "Status", "Foto"};
        DefaultTableModel variantModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable variantTable = new JTable(variantModel);
        variantTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        variantTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        variantTable.setRowHeight(30);
        JScrollPane scrollPane = new JScrollPane(variantTable);
        loadVariants(variantModel, productId);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnAddVariant = createStyledButton("Tambah Varian", new Color(46, 204, 113), e -> {
            showAddVariantDialog(productId, variantModel);
        });
        JButton btnEditVariant = createStyledButton("Edit Varian", new Color(52, 152, 219), e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int variantId = (int) variantModel.getValueAt(row, 0);
                showEditVariantDialog(variantId, productId, variantModel);
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian yang akan diedit!");
            }
        });
        JButton btnDeleteVariant = createStyledButton("Hapus Varian", new Color(231, 76, 60), e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int variantId = (int) variantModel.getValueAt(row, 0);
                deleteVariant(variantId);
                loadVariants(variantModel, productId);
                loadData();
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian yang akan dihapus!");
            }
        });
        JButton btnManagePhotos = createStyledButton("Kelola Foto", new Color(155, 89, 182), e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int variantId = (int) variantModel.getValueAt(row, 0);
                manageVariantPhotos(variantId);
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian terlebih dahulu!");
            }
        });
        JButton btnClose = createStyledButton("Tutup", Color.GRAY, e -> dialog.dispose());
        buttonPanel.add(btnAddVariant);
        buttonPanel.add(btnEditVariant);
        buttonPanel.add(btnDeleteVariant);
        buttonPanel.add(btnManagePhotos);
        buttonPanel.add(btnClose);

        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel, BorderLayout.CENTER);

        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);
        dialog.setVisible(true);
    }

    private void loadVariants(DefaultTableModel model, int productId) {
        model.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT pd.id, c.name as color, s.name as size, pd.stock, pd.status, " +
                "COUNT(pp.id) as photo_count " +
                "FROM product_details pd " +
                "JOIN colors c ON pd.color_id = c.id " +
                "JOIN sizes s ON pd.size_id = s.id " +
                "LEFT JOIN product_photos pp ON pd.id = pp.product_detail_id " +
                "WHERE pd.product_id = ? " +
                "GROUP BY pd.id, c.name, s.name, pd.stock, pd.status " +
                "ORDER BY c.name, s.name")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAddVariantDialog(int productId, DefaultTableModel variantModel) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tambah Varian Produk", true);
        dialog.setUndecorated(true);
        dialog.setSize(600, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = createDialogTitleBar("Tambah Varian Produk", dialog);
        dialog.add(titleBar, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout()) {
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
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 12, 8);

        JComboBox<ComboItem> cmbColor = loadComboData("colors");
        JComboBox<ComboItem> cmbSize = loadComboData("sizes");
        JTextField txtStock = createStyledTextField(20);
        txtStock.setText("0");
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});
        cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbStatus.setBackground(Color.WHITE);
        cmbStatus.setForeground(Color.decode("#222222"));

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

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton btnSave = createStyledButton("Simpan", new Color(46, 204, 113), e -> {
            try {
                int stock = Integer.parseInt(txtStock.getText());
                if (stock < 0) {
                    JOptionPane.showMessageDialog(dialog, "Stok tidak boleh negatif!");
                    return;
                }
                if (saveVariant(productId,
                    ((ComboItem) cmbColor.getSelectedItem()).getId(),
                    ((ComboItem) cmbSize.getSelectedItem()).getId(),
                    stock,
                    (String) cmbStatus.getSelectedItem())) {
                    JOptionPane.showMessageDialog(dialog, "Varian berhasil ditambahkan!");
                    loadVariants(variantModel, productId);
                    loadData();
                    dialog.dispose();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Stok harus berupa angka!");
            }
        });
        JButton btnCancel = createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);
        dialog.setVisible(true);
    }

    private boolean saveVariant(int productId, int colorId, int sizeId, int stock, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement checkPs = conn.prepareStatement(
                    "SELECT id FROM product_details WHERE product_id = ? AND color_id = ? AND size_id = ?")) {
                checkPs.setInt(1, productId);
                checkPs.setInt(2, colorId);
                checkPs.setInt(3, sizeId);
                try (ResultSet checkRs = checkPs.executeQuery()) {
                    if (checkRs.next()) {
                        JOptionPane.showMessageDialog(this, "Varian dengan warna dan size ini sudah ada!");
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_details (product_id, color_id, size_id, stock, status) VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, productId);
                ps.setInt(2, colorId);
                ps.setInt(3, sizeId);
                ps.setInt(4, stock);
                ps.setString(5, status);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            return false;
        }
    }

    private void showEditVariantDialog(int variantId, int productId, DefaultTableModel variantModel) {
        int stock = 0;
        int colorId = 0;
        int sizeId = 0;
        String status = null;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM product_details WHERE id = ?")) {
            ps.setInt(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stock = rs.getInt("stock");
                    colorId = rs.getInt("color_id");
                    sizeId = rs.getInt("size_id");
                    status = rs.getString("status");
                } else {
                  JOptionPane.showMessageDialog(this, "Varian tidak ditemukan!");
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Varian", true);
        dialog.setUndecorated(true);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = createDialogTitleBar("Edit Varian", dialog);
        dialog.add(titleBar, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout()) {
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
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 12, 8);

        JComboBox<ComboItem> cmbColor = loadComboData("colors");
        JComboBox<ComboItem> cmbSize = loadComboData("sizes");
        JTextField txtStock = createStyledTextField(20);
        txtStock.setText(String.valueOf(stock));
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});
        cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbStatus.setBackground(Color.WHITE);
        cmbStatus.setForeground(Color.decode("#222222"));
        selectComboItem(cmbColor, colorId);
        selectComboItem(cmbSize, sizeId );
        cmbStatus.setSelectedItem(status);

        int row = 0;
        addFormRow(panel, gbc, row++, "Warna:", cmbColor);
        addFormRow(panel, gbc, row++, "Size:", cmbSize);
        addFormRow(panel, gbc, row++, "Stok:", txtStock);
        addFormRow(panel, gbc, row++, "Status:", cmbStatus);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; 
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton btnUpdate = createStyledButton("Update", new Color(52, 152, 219), e -> {
            try {
                int newStock = Integer.parseInt(txtStock.getText());
                if (newStock < 0) {
                    JOptionPane.showMessageDialog(dialog, "Stok tidak boleh negatif!");
                    return;
                }
                if (updateVariant(variantId,
                    ((ComboItem) cmbColor.getSelectedItem()).getId(),
                    ((ComboItem) cmbSize.getSelectedItem()).getId(),
                    newStock,
                    (String) cmbStatus.getSelectedItem())) {
                    JOptionPane.showMessageDialog(dialog, "Varian berhasil diupdate!");
                    loadVariants(variantModel, productId);
                    loadData();
                    dialog.dispose();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Stok harus berupa angka!");
            }
        });
        JButton btnCancel = createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());
        btnPanel.add(btnUpdate);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);
        dialog.setVisible(true);
    }

    private boolean updateVariant(int variantId, int colorId, int sizeId, int stock, String status) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE product_details SET color_id = ?, size_id = ?, stock = ?, status = ? WHERE id = ?")) {
            ps.setInt(1, colorId);
            ps.setInt(2, sizeId);
            ps.setInt(3, stock);
            ps.setString(4, status);
            ps.setInt(5, variantId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
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

                try (PreparedStatement photoPs = conn.prepareStatement("SELECT photo_url FROM product_photos WHERE product_detail_id = ?")) {
                    photoPs.setInt(1, variantId);
                    try (ResultSet rs = photoPs.executeQuery()) {
                        while (rs.next()) {
                            String photoUrl = rs.getString("photo_url");
                            SupabaseStorage.deleteProductPhoto(photoUrl);
                        }
                    }
                }

                try (PreparedStatement delPhotosPs = conn.prepareStatement("DELETE FROM product_photos WHERE product_detail_id = ?")) {
                    delPhotosPs.setInt(1, variantId);
                    delPhotosPs.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_details WHERE id = ?")) {
                    ps.setInt(1, variantId);
                    ps.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Varian berhasil dihapus!");
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void manageVariantPhotos(int variantId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Kelola Foto Varian", true);
        dialog.setUndecorated(true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = createDialogTitleBar("Kelola Foto Varian", dialog);
        dialog.add(titleBar, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(new Color(236, 240, 241));
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT p.name, c.name as color, s.name as size " +
                "FROM product_details pd " +
                "JOIN products p ON pd.product_id = p.id " +
                "JOIN colors c ON pd.color_id = c.id " +
                "JOIN sizes s ON pd.size_id = s.id " +
                "WHERE pd.id = ?")) {
            ps.setInt(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JLabel lblInfo = new JLabel(String.format("Produk: %s | Warna: %s | Size: %s",
                        rs.getString("name"), rs.getString("color"), rs.getString("size")));
                    lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    infoPanel.add(lblInfo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JPanel photoGridPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        JScrollPane scrollPane = new JScrollPane(photoGridPanel);
        loadVariantPhotos(photoGridPanel, variantId, dialog);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddPhoto = createStyledButton("Tambah Foto", new Color(46, 204, 113), e -> {
            addVariantPhotos(variantId);
            dialog.dispose();
            manageVariantPhotos(variantId);
        });
        JButton btnClose = createStyledButton("Tutup", Color.GRAY, e -> dialog.dispose());
        buttonPanel.add(btnAddPhoto);
        buttonPanel.add(btnClose);

        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel, BorderLayout.CENTER);

        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);
        dialog.setVisible(true);
    }

    private void loadVariantPhotos(JPanel panel, int variantId, JDialog parentDialog) {
        panel.removeAll();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, photo_url FROM product_photos WHERE product_detail_id = ? ORDER BY created_at")) {
            ps.setInt(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int photoId = rs.getInt("id");
                    String photoUrl = rs.getString("photo_url");
                    JPanel photoPanel = new JPanel(new BorderLayout());
                    photoPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                    JLabel lblPhoto = new JLabel("Loading...", SwingConstants.CENTER);
                    lblPhoto.setPreferredSize(new Dimension(250, 250));
                    lblPhoto.setFont(new Font("Segoe UI", Font.PLAIN, 10));

                    new SwingWorker<ImageIcon, Void>() {
                        @Override
                        protected ImageIcon doInBackground() throws Exception {
                            try {
                                BufferedImage img = ImageIO.read(new java.net.URL(photoUrl));
                                if (img != null) {
                                    Image scaled = img.getScaledInstance(250, 250, Image.SCALE_SMOOTH);
                                    return new ImageIcon(scaled);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
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
                                    lblPhoto.setText("Gagal muat");
                                }
                            } catch (Exception ex) {
                                lblPhoto.setText("Error");
                            }
                        }
                    }.execute();

                    JButton btnDelete = createStyledButton("Hapus", new Color(231, 76, 60), ev -> {
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
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif");
            }
            public String getDescription() {
                return "Image Files (*.jpg, *.jpeg, *.png, *.gif)";
            }
        });
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Mengupload Foto...", true);
            JProgressBar progressBar = new JProgressBar(0, files.length);
            progressBar.setStringPainted(true);
            progressDialog.add(progressBar);
            progressDialog.setSize(400, 100);
            progressDialog.setLocationRelativeTo(this);

            new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try (Connection conn = DatabaseConfig.getConnection()) {
                        int uploaded = 0;
                        for (File file : files) {
                            String photoUrl = SupabaseStorage.uploadProductPhoto(variantId, file);
                            if (photoUrl != null) {
                                try (PreparedStatement ps = conn.prepareStatement(
                                        "INSERT INTO product_photos (product_detail_id, photo_url) VALUES (?, ?)")) {
                                    ps.setInt(1, variantId);
                                    ps.setString(2, photoUrl);
                                    ps.executeUpdate();
                                    uploaded++;
                                    publish(uploaded);
                                }
                            }
                        }
                    }
                    return null;
                }
                @Override
                protected void process(List<Integer> chunks) {
                    progressBar.setValue(chunks.get(chunks.size() - 1));
                }
                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        get();
                        JOptionPane.showMessageDialog(ProductManagementPanel.this, files.length + " foto berhasil diupload!");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ProductManagementPanel.this, "Error: " + e.getMessage());
                    }
                }
            }.execute();
            progressDialog.setVisible(true);
        }
    }

    private void deleteVariantPhoto(int photoId, String photoUrl) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM product_photos WHERE id = ?")) {
            SupabaseStorage.deleteProductPhoto(photoUrl);
            ps.setInt(1, photoId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Foto berhasil dihapus!");
        } catch (SQLException e) {
            e.printStackTrace();
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

                try (PreparedStatement photoPs = conn.prepareStatement(
                        "SELECT pp.photo_url FROM product_photos pp " +
                        "JOIN product_details pd ON pp.product_detail_id = pd.id " +
                        "WHERE pd.product_id = ?")) {
                    photoPs.setInt(1, id);
                    try (ResultSet rs = photoPs.executeQuery()) {
                        while (rs.next()) {
                            String photoUrl = rs.getString("photo_url");
                            SupabaseStorage.deleteProductPhoto(photoUrl);
                        }
                    }
                }

                try (PreparedStatement delPhotosPs = conn.prepareStatement(
                        "DELETE pp FROM product_photos pp " +
                        "JOIN product_details pd ON pp.product_detail_id = pd.id " +
                        "WHERE pd.product_id = ?")) {
                    delPhotosPs.setInt(1, id);
                    delPhotosPs.executeUpdate();
                }

                try (PreparedStatement delVariantsPs = conn.prepareStatement("DELETE FROM product_details WHERE product_id = ?")) {
                    delVariantsPs.setInt(1, id);
                    delVariantsPs.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Produk berhasil dihapus!");
                loadData();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private JPanel createDialogTitleBar(String title, JDialog dialog) {
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
        titleBar.setPreferredSize(new Dimension(600, 40));
        titleBar.setOpaque(false);

        JButton btnClose = createMacOSButton(new Color(0xFF5F57));
        btnClose.addActionListener(e -> dialog.dispose());

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.decode("#222222"));
        titleLabel.setOpaque(false);

        titleBar.add(btnClose);
        titleBar.add(Box.createHorizontalGlue());
        titleBar.add(titleLabel);
        titleBar.add(Box.createHorizontalGlue());
        return titleBar;
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

    private JComboBox<ComboItem> loadComboData(String tableName) {
        JComboBox<ComboItem> combo = new JComboBox<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM " + tableName + " ORDER BY name")) {
            while (rs.next()) {
                combo.addItem(new ComboItem(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
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