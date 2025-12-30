package view;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import config.DatabaseConfig;
import utils.SupabaseStorage;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;

public class VariantDialog {
    private Point mousePoint;
    private JDialog dialog;
    private int productId;
    private DefaultTableModel variantModel;
    private JTable variantTable;
    private Frame ownerFrame;
    private ProductManagementPanel mainPanel;
    private JTextField searchField;

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

        // Search Panel
        JPanel searchPanel = createSearchPanel();

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

        // Top Panel (Info + Search)
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(infoPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel, BorderLayout.CENTER);

        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(Color.WHITE);

        JLabel lblSearch = new JLabel("Cari:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        searchField = createStyledTextField(20);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }
        });

        searchPanel.add(lblSearch);
        searchPanel.add(searchField);

        return searchPanel;
    }

    private void filterTable() {
        String searchText = searchField.getText().toLowerCase().trim();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(variantModel);
        variantTable.setRowSorter(sorter);

        if (searchText.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
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
                int modelRow = variantTable.convertRowIndexToModel(row);
                int variantId = (int) variantModel.getValueAt(modelRow, 0);
                new EditVariantDialog(ownerFrame, productId, this, variantId).show();
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian yang akan diedit!");
            }
        });

        JButton btnDeleteVariant = createStyledButton("Hapus Varian", new Color(231, 76, 60), e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int modelRow = variantTable.convertRowIndexToModel(row);
                int variantId = (int) variantModel.getValueAt(modelRow, 0);
                deleteVariant(variantId);
            } else {
                JOptionPane.showMessageDialog(dialog, "Pilih varian yang akan dihapus!");
            }
        });

        JButton btnManagePhotos = createStyledButton("Kelola Foto", new Color(155, 89, 182), e -> {
            int row = variantTable.getSelectedRow();
            if (row >= 0) {
                int modelRow = variantTable.convertRowIndexToModel(row);
                int variantId = (int) variantModel.getValueAt(modelRow, 0);
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

    private JTextField txtColorName;
    private JTextField[] stockFields;
    private List<File> selectedPhotos;
    private JLabel photoCountLabel;

    private static final String[] SIZES = {"XS", "S", "M", "L", "XL", "2XL", "3XL", "4XL", "5XL"};

    public AddVariantDialog(Component parent, int productId, VariantDialog variantDialog) {
        this.parent = parent;
        this.productId = productId;
        this.variantDialog = variantDialog;
        this.selectedPhotos = new ArrayList<>();
        initDialog();
    }

    private void initDialog() {
        dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Tambah Varian Produk", true);
        dialog.setUndecorated(true);
        dialog.setSize(550, 550);
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

        // Nama Warna
        txtColorName = VariantDialog.createStyledTextField(25);
        gbc.gridy = 0;
        panel.add(new JLabel("Nama Warna *"), gbc);
        gbc.gridy = 1;
        panel.add(txtColorName, gbc);

        // Stok per Ukuran *
        JLabel stockLabel = new JLabel("Stok per Ukuran *");
        stockLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = 2;
        panel.add(stockLabel, gbc);

        // Panel grid 3 kolom per baris
        JPanel sizeStockPanel = new JPanel(new GridLayout(0, 3, 15, 15));
        sizeStockPanel.setOpaque(false);
        stockFields = new JTextField[SIZES.length];

        for (int i = 0; i < SIZES.length; i++) {
            String size = SIZES[i];
            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            itemPanel.setOpaque(false);

            JLabel label = new JLabel(size + ":");
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            JTextField field = VariantDialog.createStyledTextField(5);
            field.setText("0");
            field.setHorizontalAlignment(JTextField.RIGHT);
            field.setPreferredSize(new Dimension(60, 28));
            stockFields[i] = field;

            itemPanel.add(label);
            itemPanel.add(field);
            sizeStockPanel.add(itemPanel);
        }

        int remainder = SIZES.length % 3;
        if (remainder != 0) {
            for (int i = 0; i < 3 - remainder; i++) {
                sizeStockPanel.add(new JPanel() {{ setOpaque(false); }});
            }
        }

        gbc.gridy = 3;
        panel.add(sizeStockPanel, gbc);

        // Upload Foto Section
        JLabel photoLabel = new JLabel("Foto Produk (Opsional)");
        photoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = 4;
        panel.add(photoLabel, gbc);

        JPanel photoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        photoPanel.setOpaque(false);

        JButton btnSelectPhoto = VariantDialog.createStyledButton("Pilih Foto", new Color(52, 152, 219), e -> selectPhotos());
        photoCountLabel = new JLabel("Belum ada foto");
        photoCountLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        photoPanel.add(btnSelectPhoto);
        photoPanel.add(photoCountLabel);

        gbc.gridy = 5;
        panel.add(photoPanel, gbc);

        // Tombol
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(VariantDialog.createStyledButton("Batal", Color.GRAY, e -> dialog.dispose()));
        btnPanel.add(VariantDialog.createStyledButton("Tambah", new Color(46, 204, 113), e -> handleSave()));
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        VariantDialog.addWindowDrag(titleBar, dialog);
        VariantDialog.updateDialogShape(dialog);
    }

    private void selectPhotos() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png"));

        int result = fileChooser.showOpenDialog(dialog);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            selectedPhotos.clear();
            for (File file : files) {
                selectedPhotos.add(file);
            }
            updatePhotoCount();
        }
    }

    private void updatePhotoCount() {
        if (selectedPhotos.isEmpty()) {
            photoCountLabel.setText("Belum ada foto");
        } else {
            photoCountLabel.setText(selectedPhotos.size() + " foto dipilih");
        }
    }

    private void handleSave() {
        if (txtColorName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Nama warna wajib diisi!");
            return;
        }

        boolean atLeastOneValid = false;
        for (JTextField field : stockFields) {
            String text = field.getText().trim();
            try {
                int stock = Integer.parseInt(text);
                if (stock > 0) {
                    atLeastOneValid = true;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(dialog, "Stok harus berupa angka!");
                return;
            }
        }

        if (!atLeastOneValid) {
            JOptionPane.showMessageDialog(dialog, "Minimal satu ukuran harus memiliki stok > 0!");
            return;
        }

        saveVariants();
    }

    private void saveVariants() {
        String colorName = txtColorName.getText().trim();

        List<String> failedSizes = new ArrayList<>();
        boolean anySuccess = false;
        List<Integer> savedVariantIds = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection()) {
            int colorId = getColorIdOrCreate(conn, colorName);
            if (colorId == -1) {
                JOptionPane.showMessageDialog(dialog, "Gagal menyimpan warna.");
                return;
            }

            for (int i = 0; i < SIZES.length; i++) {
                String sizeName = SIZES[i];
                int stock;
                try {
                    stock = Integer.parseInt(stockFields[i].getText().trim());
                } catch (NumberFormatException e) {
                    failedSizes.add(sizeName + " (stok tidak valid)");
                    continue;
                }

                if (stock <= 0) {
                    continue;
                }

                int sizeId = getSizeIdOrCreate(conn, sizeName);
                if (sizeId == -1) {
                    failedSizes.add(sizeName + " (gagal ukuran)");
                    continue;
                }

                // Cek duplikat
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT 1 FROM product_details WHERE product_id = ? AND color_id = ? AND size_id = ?")) {
                    check.setInt(1, productId);
                    check.setInt(2, colorId);
                    check.setInt(3, sizeId);
                    if (check.executeQuery().next()) {
                        failedSizes.add(sizeName + " (duplikat)");
                        continue;
                    }
                }

                // Insert
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO product_details (product_id, color_id, size_id, stock, status) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, productId);
                    ps.setInt(2, colorId);
                    ps.setInt(3, sizeId);
                    ps.setInt(4, stock);
                    ps.setString(5, "available");
                    ps.executeUpdate();
                    
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        savedVariantIds.add(rs.getInt(1));
                    }
                    anySuccess = true;
                }
            }

            // Upload foto untuk semua varian yang berhasil disimpan
            if (anySuccess && !selectedPhotos.isEmpty()) {
                uploadPhotosForVariants(conn, savedVariantIds);
            }

            if (anySuccess && failedSizes.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Varian berhasil ditambahkan!");
            } else if (!anySuccess) {
                JOptionPane.showMessageDialog(dialog, "Tidak ada varian yang berhasil disimpan.");
            } else {
                JOptionPane.showMessageDialog(dialog, "Beberapa ukuran gagal disimpan:\n" + String.join("\n", failedSizes));
            }

            variantDialog.loadVariants();
            dialog.dispose();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "Error database: " + e.getMessage());
        }
    }

    private void uploadPhotosForVariants(Connection conn, List<Integer> variantIds) {
        for (Integer variantId : variantIds) {
            for (File photo : selectedPhotos) {
                try {
                    String photoUrl = SupabaseStorage.uploadProductPhoto(productId, photo);
                    if (photoUrl != null) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO product_photos (product_detail_id, photo_url) VALUES (?, ?)")) {
                            ps.setInt(1, variantId);
                            ps.setString(2, photoUrl);
                            ps.executeUpdate();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int getColorIdOrCreate(Connection conn, String name) {
        try {
            try (PreparedStatement check = conn.prepareStatement("SELECT id FROM colors WHERE name = ?")) {
                check.setString(1, name);
                ResultSet rs = check.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO colors (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, name);
                ins.executeUpdate();
                ResultSet rs = ins.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int getSizeIdOrCreate(Connection conn, String name) {
        try {
            try (PreparedStatement check = conn.prepareStatement("SELECT id FROM sizes WHERE name = ?")) {
                check.setString(1, name);
                ResultSet rs = check.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO sizes (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, name);
                ins.executeUpdate();
                ResultSet rs = ins.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void show() {
        dialog.setVisible(true);
    }
}

// ==================== EDIT VARIANT DIALOG ====================
class EditVariantDialog {
    private final Component parent;
    private final int productId;
    private final VariantDialog variantDialog;
    private final int variantId;
    private JDialog dialog;

    private JTextField txtColorName;
    private JTextField txtStock;
    private JComboBox<String> cmbStatus;
    private JTextField txtSizeName;

    public EditVariantDialog(Component parent, int productId, VariantDialog variantDialog, int variantId) {
        this.parent = parent;
        this.productId = productId;
        this.variantDialog = variantDialog;
        this.variantId = variantId;
        initDialog();
    }

    private void initDialog() {
        String colorName = "", sizeName = "";
        int stock = 0;
        String status = "available";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT c.name as color_name, s.name as size_name, pd.stock, pd.status " +
                 "FROM product_details pd " +
                 "JOIN colors c ON pd.color_id = c.id " +
                 "JOIN sizes s ON pd.size_id = s.id " +
                 "WHERE pd.id = ?")) {
            ps.setInt(1, variantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                colorName = rs.getString("color_name");
                sizeName = rs.getString("size_name");
                stock = rs.getInt("stock");
                status = rs.getString("status");
            } else {
                JOptionPane.showMessageDialog(parent, "Varian tidak ditemukan!");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Edit Varian", true);
        dialog.setUndecorated(true);
        dialog.setSize(600, 450);
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

        txtColorName = VariantDialog.createStyledTextField(25);
        txtColorName.setText(colorName);

        txtSizeName = VariantDialog.createStyledTextField(25);
        txtSizeName.setText(sizeName);
        txtSizeName.setEditable(false);

        txtStock = VariantDialog.createStyledTextField(25);
        txtStock.setText(String.valueOf(stock));

        cmbStatus = new JComboBox<>(new String[]{"available", "out_of_stock", "discontinued"});
        cmbStatus.setSelectedItem(status);
        cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        int row = 0;
        panel.add(createLabel("Nama Warna *"), createGbc(gbc, 0, row));
        panel.add(txtColorName, createGbc(gbc, 1, row++));
        panel.add(createLabel("Ukuran"), createGbc(gbc, 0, row));
        panel.add(txtSizeName, createGbc(gbc, 1, row++));
        panel.add(createLabel("Stok *"), createGbc(gbc, 0, row));
        panel.add(txtStock, createGbc(gbc, 1, row++));
        panel.add(createLabel("Status"), createGbc(gbc, 0, row));
        panel.add(cmbStatus, createGbc(gbc, 1, row++));

        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(VariantDialog.createStyledButton("Batal", Color.GRAY, e -> dialog.dispose()));
        btnPanel.add(VariantDialog.createStyledButton("Simpan", new Color(52, 152, 219), e -> handleUpdate()));
        panel.add(btnPanel, gbc);

        dialog.add(panel, BorderLayout.CENTER);
        VariantDialog.addWindowDrag(titleBar, dialog);
        VariantDialog.updateDialogShape(dialog);
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private GridBagConstraints createGbc(GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        return gbc;
    }

    private void handleUpdate() {
        if (txtColorName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Nama warna wajib diisi!");
            return;
        }
        try {
            Integer.parseInt(txtStock.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(dialog, "Stok harus angka!");
            return;
        }

        updateVariant();
    }

    private void updateVariant() {
        String colorName = txtColorName.getText().trim();
        int stock = Integer.parseInt(txtStock.getText());
        String status = (String) cmbStatus.getSelectedItem();

        try (Connection conn = DatabaseConfig.getConnection()) {
            int colorId = getColorIdOrCreate(conn, colorName);
            if (colorId == -1) {
                JOptionPane.showMessageDialog(dialog, "Gagal memperbarui warna.");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                 "UPDATE product_details SET color_id = ?, stock = ?, status = ? WHERE id = ?")) {
                ps.setInt(1, colorId);
                ps.setInt(2, stock);
                ps.setString(3, status);
                ps.setInt(4, variantId);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(dialog, "Varian berhasil diperbarui!");
                variantDialog.loadVariants();
                dialog.dispose();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "Error: " + e.getMessage());
        }
    }

    private int getColorIdOrCreate(Connection conn, String name) {
        try {
            try (PreparedStatement check = conn.prepareStatement("SELECT id FROM colors WHERE name = ?")) {
                check.setString(1, name);
                ResultSet rs = check.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO colors (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, name);
                ins.executeUpdate();
                ResultSet rs = ins.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void show() {
        dialog.setVisible(true);
    }
}