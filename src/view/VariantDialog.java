package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import config.DatabaseConfig;
import utils.SupabaseStorage;
import java.awt.geom.RoundRectangle2D;


public class VariantDialog {
    private Point mousePoint;
    private JDialog dialog;
    private int productId;
    private DefaultTableModel variantModel;
    private JTable variantTable;
    private Frame ownerFrame;
    private ProductManagementPanel mainPanel;

    public VariantDialog(Frame ownerFrame, ProductManagementPanel mainPanel, int productId) {
        this.ownerFrame = ownerFrame;
        this.mainPanel = mainPanel;
        this.productId = productId;
        initDialog();
    }

    public void initDialog() {
        dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(ownerFrame), "Kelola Varian Produk", true);
        dialog.setUndecorated(true);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(ownerFrame);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = createDialogTitleBar("Kelola Varian Produk", dialog);
        dialog.add(titleBar, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Info Panel
        JPanel infoPanel = createInfoPanel();

        // Variant Table
        String[] columns = {"ID", "Warna", "Size", "Stok", "Status", "Foto"};
        variantModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        variantTable = new JTable(variantModel);
        variantTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        variantTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        variantTable.setRowHeight(30);
        JScrollPane scrollPane = new JScrollPane(variantTable);
        loadVariants();

        // Button Panel
        JPanel buttonPanel = createButtonPanel();

        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel, BorderLayout.CENTER);

        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);
    }

    private JPanel createInfoPanel() {
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
        return infoPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton btnAddVariant = createStyledButton("Tambah Varian", new Color(46, 204, 113), e ->
            new AddVariantDialog(ownerFrame, productId, this).show()
        );

        JButton btnEditVariant = createStyledButton("Edit Varian", new Color(52, 152, 219), e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int variantId = (int) variantModel.getValueAt(row, 0);
                new EditVariantDialog(ownerFrame, variantId, productId, this).show();
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian yang akan diedit!");
            }
        });

        JButton btnDeleteVariant = createStyledButton("Hapus Varian", new Color(231, 76, 60), e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int variantId = (int) variantModel.getValueAt(row, 0);
                deleteVariant(variantId);
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian yang akan dihapus!");
            }
        });

        JButton btnManagePhotos = createStyledButton("Kelola Foto", new Color(155, 89, 182), e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int variantId = (int) variantModel.getValueAt(row, 0);
                new PhotoDialog(ownerFrame, variantId).show();
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

        return buttonPanel;
    }

    public void loadVariants() {
        variantModel.setRowCount(0);
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
                    variantModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteVariant(int variantId) {
        int confirm = JOptionPane.showConfirmDialog(dialog,
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
                JOptionPane.showMessageDialog(dialog, "Varian berhasil dihapus!");
                loadVariants();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error: " + e.getMessage());
            }
        }
    }

    public void show() {
        dialog.setVisible(true);
    }

    // ==================== Shared UI Utilities ====================

    protected static JButton createStyledButton(String text, Color bgColor, ActionListener listener) {
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

    protected static JPanel createDialogTitleBar(String title, JDialog dialog) {
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

    protected static JTextField createStyledTextField(int columns) {
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

    protected static void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
    }

    protected static JComboBox<ComboItem> loadComboData(String tableName) {
        JComboBox<ComboItem> combo = new JComboBox<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM " + tableName + " ORDER BY name")) {
            while (rs.next()) {
                combo.addItem(new ComboItem(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return combo;
    }

    protected static void addWindowDrag(Component comp, JDialog dialog) {
        final Point[] mousePoint = new Point[1];

        comp.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                comp.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                mousePoint[0] = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                comp.setCursor(Cursor.getDefaultCursor());
            }
        });

        comp.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point curr = e.getLocationOnScreen();
                dialog.setLocation(
                    curr.x - mousePoint[0].x,
                    curr.y - mousePoint[0].y
                );
            }
        });
    }

    protected static void updateDialogShape(JDialog dialog) {
        int arc = 20;
        Shape shape = new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), arc, arc);
        dialog.setShape(shape);
    }

    protected static JButton createMacOSButton(Color color) {
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

    // ==================== INNER CLASSES ====================

    static class ComboItem {
        private final int id;
        private final String name;
        public ComboItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        public int getId() { return id; }
        @Override
        public String toString() { return name; }
    }
}

// ==================== ADD VARIANT DIALOG ====================
class AddVariantDialog {
    private final Component parent;
    private final int productId;
    private final VariantDialog variantDialog;
    private JDialog dialog;

    public AddVariantDialog(Component parent, int productId, VariantDialog variantDialog) {
        this.parent = parent;
        this.productId = productId;
        this.variantDialog = variantDialog;
        initDialog();
    }

    private void initDialog() {
        dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Tambah Varian Produk", true);
        dialog.setUndecorated(true);
        dialog.setSize(600, 420);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = VariantDialog.createDialogTitleBar("Tambah Varian Produk", dialog);
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

        JComboBox<VariantDialog.ComboItem> cmbColor = VariantDialog.loadComboData("colors");
        JComboBox<VariantDialog.ComboItem> cmbSize = VariantDialog.loadComboData("sizes");
        JTextField txtStock = VariantDialog.createStyledTextField(20);
        txtStock.setText("0");
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});
        cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbStatus.setBackground(Color.WHITE);
        cmbStatus.setForeground(Color.decode("#222222"));

        int row = 0;
        VariantDialog.addFormRow(panel, gbc, row++, "Warna:", cmbColor);
        VariantDialog.addFormRow(panel, gbc, row++, "Size:", cmbSize);
        VariantDialog.addFormRow(panel, gbc, row++, "Stok:", txtStock);
        VariantDialog.addFormRow(panel, gbc, row++, "Status:", cmbStatus);

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

        JButton btnSave = VariantDialog.createStyledButton("Simpan", new Color(46, 204, 113), e -> {
            try {
                int stock = Integer.parseInt(txtStock.getText());
                if (stock < 0) {
                    JOptionPane.showMessageDialog(dialog, "Stok tidak boleh negatif!");
                    return;
                }
                if (saveVariant(
                    ((VariantDialog.ComboItem) cmbColor.getSelectedItem()).getId(),
                    ((VariantDialog.ComboItem) cmbSize.getSelectedItem()).getId(),
                    stock,
                    (String) cmbStatus.getSelectedItem())) {
                    JOptionPane.showMessageDialog(dialog, "Varian berhasil ditambahkan!");
                    variantDialog.loadVariants();
                    dialog.dispose();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Stok harus berupa angka!");
            }
        });

        JButton btnCancel = VariantDialog.createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        VariantDialog.addWindowDrag(titleBar, dialog);
        VariantDialog.updateDialogShape(dialog);
    }

    private boolean saveVariant(int colorId, int sizeId, int stock, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement checkPs = conn.prepareStatement(
                    "SELECT id FROM product_details WHERE product_id = ? AND color_id = ? AND size_id = ?")) {
                checkPs.setInt(1, productId);
                checkPs.setInt(2, colorId);
                checkPs.setInt(3, sizeId);
                try (ResultSet checkRs = checkPs.executeQuery()) {
                    if (checkRs.next()) {
                        JOptionPane.showMessageDialog(dialog, "Varian dengan warna dan size ini sudah ada!");
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
            JOptionPane.showMessageDialog(dialog, "Error: " + e.getMessage());
            return false;
        }
    }

    public void show() {
        dialog.setVisible(true);
    }
}

// ==================== EDIT VARIANT DIALOG ====================
class EditVariantDialog {
    private final Component parent;
    private final int variantId;
    private final int productId;
    private final VariantDialog variantDialog;
    private JDialog dialog;

    public EditVariantDialog(Component parent, int variantId, int productId, VariantDialog variantDialog) {
        this.parent = parent;
        this.variantId = variantId;
        this.productId = productId;
        this.variantDialog = variantDialog;
        initDialog();
    }

    private void initDialog() {
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
                    JOptionPane.showMessageDialog(parent, "Varian tidak ditemukan!");
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Edit Varian", true);
        dialog.setUndecorated(true);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = VariantDialog.createDialogTitleBar("Edit Varian", dialog);
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

        JComboBox<VariantDialog.ComboItem> cmbColor = VariantDialog.loadComboData("colors");
        JComboBox<VariantDialog.ComboItem> cmbSize = VariantDialog.loadComboData("sizes");
        JTextField txtStock = VariantDialog.createStyledTextField(20);
        txtStock.setText(String.valueOf(stock));

        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});
        cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbStatus.setSelectedItem(status);

        // Pre-select color & size
        for (int i = 0; i < cmbColor.getItemCount(); i++) {
            if (cmbColor.getItemAt(i).getId() == colorId) {
                cmbColor.setSelectedIndex(i);
                break;
            }
        }
        for (int i = 0; i < cmbSize.getItemCount(); i++) {
            if (cmbSize.getItemAt(i).getId() == sizeId) {
                cmbSize.setSelectedIndex(i);
                break;
            }
        }

        int row = 0;
        VariantDialog.addFormRow(panel, gbc, row++, "Warna:", cmbColor);
        VariantDialog.addFormRow(panel, gbc, row++, "Size:", cmbSize);
        VariantDialog.addFormRow(panel, gbc, row++, "Stok:", txtStock);
        VariantDialog.addFormRow(panel, gbc, row++, "Status:", cmbStatus);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnUpdate = VariantDialog.createStyledButton("Update", new Color(52, 152, 219), e -> {
            try {
                int newStock = Integer.parseInt(txtStock.getText());
                if (newStock < 0) {
                    JOptionPane.showMessageDialog(dialog, "Stok tidak boleh negatif!");
                    return;
                }
                if (updateVariant(
                    ((VariantDialog.ComboItem) cmbColor.getSelectedItem()).getId(),
                    ((VariantDialog.ComboItem) cmbSize.getSelectedItem()).getId(),
                    newStock,
                    (String) cmbStatus.getSelectedItem())) {
                    JOptionPane.showMessageDialog(dialog, "Varian berhasil diupdate!");
                    variantDialog.loadVariants();
                    dialog.dispose();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Stok harus berupa angka!");
            }
        });

        JButton btnCancel = VariantDialog.createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());
        btnPanel.add(btnUpdate);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        VariantDialog.addWindowDrag(titleBar, dialog);
        VariantDialog.updateDialogShape(dialog);
    }

    private boolean updateVariant(int colorId, int sizeId, int stock, String status) {
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
            JOptionPane.showMessageDialog(dialog, "Error: " + e.getMessage());
            return false;
        }
    }

    public void show() {
        dialog.setVisible(true);
    }
}