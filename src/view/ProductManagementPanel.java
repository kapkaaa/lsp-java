package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.regex.Pattern;
import java.sql.*;
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

    // UI Helper Methods
    public static JTextField createStyledTextField(int columns) {
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

    public static JButton createStyledButton(String text, Color bgColor, ActionListener listener) {
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

    public static JPanel createDialogTitleBar(String title, JDialog dialog, Point mousePoint) {
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

        addWindowDrag(titleBar, dialog, mousePoint);
        return titleBar;
    }

    private static JButton createMacOSButton(Color color) {
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

    private static void addWindowDrag(Component comp, JDialog dialog, Point mousePoint) {
        final Point[] point = {mousePoint};
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                point[0] = e.getPoint();
            }
        });
        comp.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point curr = e.getLocationOnScreen();
                dialog.setLocation(curr.x - point[0].x, curr.y - point[0].y);
            }
        });
    }

    public static void updateDialogShape(JDialog dialog) {
        int arc = 20;
        Shape shape = new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), arc, arc);
        dialog.setShape(shape);
    }

    public void loadData() {
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

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            if (!"Semua".equals(statusFilter)) {
                stmt.setString(1, statusFilter);
            }

            try (ResultSet rs = stmt.executeQuery()) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
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
        ProductFormDialog dialog = new ProductFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), this);
        dialog.showAddDialog();
    }

    private void showEditProductDialog() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk yang akan diedit!");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        
        ProductFormDialog dialog = new ProductFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), this);
        dialog.showEditDialog(id);
    }

    private void manageProductVariants() {
    int viewRow = table.getSelectedRow();
    if (viewRow == -1) {
        JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!");
        return;
    }
    int modelRow = table.convertRowIndexToModel(viewRow);
    Object idValue = tableModel.getValueAt(modelRow, 0);
    
    if (idValue == null || !(idValue instanceof Number)) {
        JOptionPane.showMessageDialog(this, "ID produk tidak valid!");
        return;
    }
    
    int productId = ((Number) idValue).intValue();
    
    Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);

    new VariantDialog(frame, this, productId).show();
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
                            SupabaseStorage.deleteProductPhoto(rs.getString("photo_url"));
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

                try (PreparedStatement delVariantsPs = conn.prepareStatement(
                        "DELETE FROM product_details WHERE product_id = ?")) {
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