package view;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import config.DatabaseConfig;
import utils.InputValidator;
import static view.ProductManagementPanel.*;

public class ProductFormDialog {
    private Frame parent;
    private ProductManagementPanel mainPanel;
    private Point mousePoint = new Point();

    public ProductFormDialog(Frame parent, ProductManagementPanel mainPanel) {
        this.parent = parent;
        this.mainPanel = mainPanel;
    }

    public void showAddDialog() {
        JDialog dialog = createDialog("Tambah Produk Baru");
        
        JPanel panel = createFormPanel();
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
            if (validateInput(txtName, txtCostPrice, txtSellingPrice, dialog)) {
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
                    mainPanel.loadData();
                    
                    new VariantDialog((Frame) parent, mainPanel, productId).show();
                }
            }
        });
        JButton btnCancel = createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        updateDialogShape(dialog);
        dialog.setVisible(true);
    }

    public void showEditDialog(int productId) {
        Product product = loadProduct(productId);
        if (product == null) {
            JOptionPane.showMessageDialog(parent, "Produk tidak ditemukan!");
            return;
        }

        JDialog dialog = createDialog("Edit Produk");
        
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 12, 8);

        JTextField txtName = createStyledTextField(25);
        txtName.setText(product.name);
        JTextField txtCostPrice = createStyledTextField(25);
        txtCostPrice.setText(String.valueOf(product.costPrice));
        JTextField txtSellingPrice = createStyledTextField(25);
        txtSellingPrice.setText(String.valueOf(product.sellingPrice));
        JComboBox<ComboItem> cmbBrand = loadComboData("brands");
        JComboBox<ComboItem> cmbType = loadComboData("types");
        selectComboItem(cmbBrand, product.brandId);
        selectComboItem(cmbType, product.typeId);

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
            if (validateInput(txtName, txtCostPrice, txtSellingPrice, dialog)) {
                if (updateProduct(productId,
                    txtName.getText(),
                    ((ComboItem) cmbBrand.getSelectedItem()).getId(),
                    ((ComboItem) cmbType.getSelectedItem()).getId(),
                    Double.parseDouble(txtCostPrice.getText()),
                    Double.parseDouble(txtSellingPrice.getText()))) {
                    JOptionPane.showMessageDialog(dialog, "Produk berhasil diupdate!");
                    dialog.dispose();
                    mainPanel.loadData();
                }
            }
        });
        JButton btnCancel = createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());
        btnPanel.add(btnUpdate);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        updateDialogShape(dialog);
        dialog.setVisible(true);
    }

    private JDialog createDialog(String title) {
        JDialog dialog = new JDialog(parent, title, true);
        dialog.setUndecorated(true);
        dialog.setSize(600, 420);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = createDialogTitleBar(title, dialog, mousePoint);
        dialog.add(titleBar, BorderLayout.NORTH);

        return dialog;
    }

    private JPanel createFormPanel() {
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
        return panel;
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
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM " + tableName + " ORDER BY name")) {
            while (rs.next()) {
                combo.addItem(new ComboItem(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Error loading " + tableName + ": " + e.getMessage());
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

    private boolean validateInput(JTextField name, JTextField costPrice, JTextField sellingPrice, JDialog dialog) {
        if (!InputValidator.isNotEmpty(name.getText())) {
            JOptionPane.showMessageDialog(dialog, "Nama produk harus diisi!");
            return false;
        }
        if (!InputValidator.isValidPrice(costPrice.getText())) {
            JOptionPane.showMessageDialog(dialog, "Harga beli tidak valid!");
            return false;
        }
        if (!InputValidator.isValidPrice(sellingPrice.getText())) {
            JOptionPane.showMessageDialog(dialog, "Harga jual tidak valid!");
            return false;
        }
        return true;
    }

    private Product loadProduct(int productId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE id = ?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                        rs.getString("name"),
                        rs.getDouble("cost_price"),
                        rs.getDouble("selling_price"),
                        rs.getInt("brand_id"),
                        rs.getInt("type_id")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
        }
        return null;
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
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
        }
        return 0;
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
            JOptionPane.showMessageDialog(parent, "Error: " + e.getMessage());
            return false;
        }
    }

    public static class ComboItem {
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

    private static class Product {
        String name;
        double costPrice;
        double sellingPrice;
        int brandId;
        int typeId;

        Product(String name, double costPrice, double sellingPrice, int brandId, int typeId) {
            this.name = name;
            this.costPrice = costPrice;
            this.sellingPrice = sellingPrice;
            this.brandId = brandId;
            this.typeId = typeId;
        }
    }
}