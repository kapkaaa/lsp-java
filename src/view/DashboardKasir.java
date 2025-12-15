package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import config.DatabaseConfig;
import utils.*;
import model.*;

public class DashboardKasir extends JFrame {
    private JLabel lblWelcome, lblDateTime;
    private JPanel contentPanel;
    private Timer clockTimer;
    
    // Transaction components
    private DefaultTableModel productTableModel;
    private DefaultTableModel cartTableModel;
    private JTable productTable, cartTable;
    private JTextField txtSearch, txtBarcode;
    private JComboBox<String> cmbPaymentMethod;
    private JLabel lblTotal, lblSubtotal;
    private List<CartItem> cartItems;
    
    public DashboardKasir() {
        cartItems = new ArrayList<>();
        initComponents();
        loadProducts();
        startClock();
    }
    
    private void initComponents() {
        setTitle("Dashboard Kasir - DistroZone");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Content Panel
        contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPanel.setBackground(Color.WHITE);
        
        // Create transaction interface
        JPanel transactionPanel = createTransactionPanel();
        contentPanel.add(transactionPanel, BorderLayout.CENTER);
        
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(52, 152, 219));
        panel.setPreferredSize(new Dimension(0, 80));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Left: Welcome message
        JPanel leftPanel = new JPanel(new GridLayout(2, 1));
        leftPanel.setOpaque(false);
        
        lblWelcome = new JLabel("Selamat Datang, " + SessionManager.getCurrentUserName());
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 18));
        lblWelcome.setForeground(Color.WHITE);
        
        lblDateTime = new JLabel();
        lblDateTime.setFont(new Font("Arial", Font.PLAIN, 13));
        lblDateTime.setForeground(new Color(236, 240, 241));
        
        leftPanel.add(lblWelcome);
        leftPanel.add(lblDateTime);
        
        // Right: Buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JButton btnHistory = createHeaderButton("Riwayat Transaksi", e -> showTransactionHistory());
        JButton btnLogout = createHeaderButton("Logout", e -> logout());
        
        rightPanel.add(btnHistory);
        rightPanel.add(btnLogout);
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JButton createHeaderButton(String text, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.setBackground(new Color(41, 128, 185));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(140, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(31, 97, 141));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(41, 128, 185));
            }
        });
        
        return btn;
    }
    
    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        
        // Left: Product list
        JPanel leftPanel = createProductPanel();
        
        // Right: Cart and payment
        JPanel rightPanel = createCartPanel();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(650);
        splitPane.setResizeWeight(0.6);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Daftar Produk"));
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.setBackground(Color.WHITE);
        
        searchPanel.add(new JLabel("Cari:"));
        txtSearch = new JTextField(20);
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                searchProducts();
            }
        });
        searchPanel.add(txtSearch);
        
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadProducts());
        searchPanel.add(btnRefresh);
        
        // Product table
        String[] columns = {"ID", "Produk", "Merek", "Warna", "Size", "Stok", "Harga"};
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        productTable = new JTable(productTableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setRowHeight(25);
        productTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        productTable.getColumnModel().getColumn(6).setCellRenderer(new CurrencyRenderer());
        
        // Double click to add to cart
        productTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addToCart();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(productTable);
        
        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(Color.WHITE);
        
        JButton btnAdd = new JButton("Tambah ke Keranjang");
        btnAdd.setBackground(new Color(46, 204, 113));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> addToCart());
        btnPanel.add(btnAdd);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Keranjang Belanja"));
        
        // Cart table
        String[] columns = {"Produk", "Harga", "Qty", "Subtotal"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only quantity editable
            }
        };
        
        cartTable = new JTable(cartTableModel);
        cartTable.setRowHeight(25);
        cartTable.getColumnModel().getColumn(1).setCellRenderer(new CurrencyRenderer());
        cartTable.getColumnModel().getColumn(3).setCellRenderer(new CurrencyRenderer());
        
        JScrollPane scrollPane = new JScrollPane(cartTable);
        
        // Button panel for cart
        JPanel cartBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cartBtnPanel.setBackground(Color.WHITE);
        
        JButton btnUpdate = new JButton("Update");
        btnUpdate.addActionListener(e -> updateCart());
        cartBtnPanel.add(btnUpdate);
        
        JButton btnRemove = new JButton("Hapus");
        btnRemove.setBackground(new Color(231, 76, 60));
        btnRemove.setForeground(Color.WHITE);
        btnRemove.addActionListener(e -> removeFromCart());
        cartBtnPanel.add(btnRemove);
        
        JButton btnClear = new JButton("Kosongkan");
        btnClear.addActionListener(e -> clearCart());
        cartBtnPanel.add(btnClear);
        
        // Payment panel
        JPanel paymentPanel = createPaymentPanel();
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(cartBtnPanel, BorderLayout.NORTH);
        bottomPanel.add(paymentPanel, BorderLayout.CENTER);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createPaymentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Pembayaran"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Total
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("TOTAL:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        lblTotal = new JLabel("Rp 0");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 20));
        lblTotal.setForeground(new Color(46, 204, 113));
        panel.add(lblTotal, gbc);
        
        // Payment method
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Metode Bayar:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        cmbPaymentMethod = new JComboBox<>(new String[]{"cash", "qris", "transfer"});
        panel.add(cmbPaymentMethod, gbc);
        
        // Process button
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JButton btnProcess = new JButton("PROSES TRANSAKSI");
        btnProcess.setFont(new Font("Arial", Font.BOLD, 14));
        btnProcess.setBackground(new Color(52, 152, 219));
        btnProcess.setForeground(Color.WHITE);
        btnProcess.setPreferredSize(new Dimension(0, 40));
        btnProcess.addActionListener(e -> processTransaction());
        panel.add(btnProcess, gbc);
        
        return panel;
    }
    
    private void loadProducts() {
        productTableModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT p.id, p.name, b.name as brand, c.name as color, " +
                        "s.name as size, p.stock, p.selling_price " +
                        "FROM products p " +
                        "JOIN brands b ON p.brand_id = b.id " +
                        "JOIN colors c ON p.color_id = c.id " +
                        "JOIN sizes s ON p.size_id = s.id " +
                        "WHERE p.status = 'available' AND p.stock > 0 " +
                        "ORDER BY p.name";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("brand"),
                    rs.getString("color"),
                    rs.getString("size"),
                    rs.getInt("stock"),
                    rs.getDouble("selling_price")
                };
                productTableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void searchProducts() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(productTableModel);
        productTable.setRowSorter(sorter);
        
        if (keyword.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + keyword));
        }
    }
    
    private void addToCart() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!");
            return;
        }
        
        int productId = (int) productTable.getValueAt(row, 0);
        String productName = (String) productTable.getValueAt(row, 1);
        double price = (double) productTable.getValueAt(row, 6);
        int availableStock = (int) productTable.getValueAt(row, 5);
        
        String input = JOptionPane.showInputDialog(this, "Jumlah:", "1");
        if (input == null) return;
        
        try {
            int qty = Integer.parseInt(input);
            if (qty <= 0 || qty > availableStock) {
                JOptionPane.showMessageDialog(this, "Jumlah tidak valid!");
                return;
            }
            
            // Check if already in cart
            CartItem existingItem = null;
            for (CartItem item : cartItems) {
                if (item.productId == productId) {
                    existingItem = item;
                    break;
                }
            }
            
            if (existingItem != null) {
                existingItem.quantity += qty;
            } else {
                cartItems.add(new CartItem(productId, productName, price, qty));
            }
            
            updateCartDisplay();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Input tidak valid!");
        }
    }
    
    private void updateCart() {
        int row = cartTable.getSelectedRow();
        if (row == -1) return;
        
        try {
            int newQty = Integer.parseInt(cartTable.getValueAt(row, 2).toString());
            if (newQty > 0) {
                cartItems.get(row).quantity = newQty;
                updateCartDisplay();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Jumlah tidak valid!");
        }
    }
    
    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih item yang akan dihapus!");
            return;
        }
        
        cartItems.remove(row);
        updateCartDisplay();
    }
    
    private void clearCart() {
        if (cartItems.isEmpty()) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Kosongkan keranjang?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            cartItems.clear();
            updateCartDisplay();
        }
    }
    
    private void updateCartDisplay() {
        cartTableModel.setRowCount(0);
        double total = 0;
        
        for (CartItem item : cartItems) {
            double subtotal = item.price * item.quantity;
            total += subtotal;
            
            Object[] row = {
                item.productName,
                item.price,
                item.quantity,
                subtotal
            };
            cartTableModel.addRow(row);
        }
        
        lblTotal.setText(FormatterUtils.formatCurrency(total));
    }
    
    private void processTransaction() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keranjang kosong!");
            return;
        }
        
        double total = cartItems.stream().mapToDouble(i -> i.price * i.quantity).sum();
        String paymentMethod = (String) cmbPaymentMethod.getSelectedItem();
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Proses transaksi dengan total " + FormatterUtils.formatCurrency(total) + "?",
            "Konfirmasi", JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Insert transaction
                String transCode = FormatterUtils.generateTransactionCode();
                String sqlTrans = "INSERT INTO transactions (user_id, transaction_code, total, " +
                                 "payment_method, transaction_status) VALUES (?, ?, ?, ?, 'completed')";
                PreparedStatement psTrans = conn.prepareStatement(sqlTrans, Statement.RETURN_GENERATED_KEYS);
                psTrans.setInt(1, SessionManager.getCurrentUserId());
                psTrans.setString(2, transCode);
                psTrans.setDouble(3, total);
                psTrans.setString(4, paymentMethod);
                psTrans.executeUpdate();
                
                ResultSet rs = psTrans.getGeneratedKeys();
                int transId = 0;
                if (rs.next()) {
                    transId = rs.getInt(1);
                }
                
                // Insert details and update stock
                String sqlDetail = "INSERT INTO transaction_details (transaction_id, product_id, " +
                                  "quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement psDetail = conn.prepareStatement(sqlDetail);
                
                String sqlUpdateStock = "UPDATE products SET stock = stock - ? WHERE id = ?";
                PreparedStatement psStock = conn.prepareStatement(sqlUpdateStock);
                
                for (CartItem item : cartItems) {
                    psDetail.setInt(1, transId);
                    psDetail.setInt(2, item.productId);
                    psDetail.setInt(3, item.quantity);
                    psDetail.setDouble(4, item.price);
                    psDetail.setDouble(5, item.price * item.quantity);
                    psDetail.executeUpdate();
                    
                    psStock.setInt(1, item.quantity);
                    psStock.setInt(2, item.productId);
                    psStock.executeUpdate();
                }
                
                conn.commit();
                
                JOptionPane.showMessageDialog(this, 
                    "Transaksi berhasil!\nKode: " + transCode,
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
                
                clearCart();
                loadProducts();
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void showTransactionHistory() {
        new TransactionHistoryDialog(this).setVisible(true);
    }
    
    private void startClock() {
        clockTimer = new Timer(1000, e -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String dayName = OperationalHoursValidator.getOperationalMessage("store");
            lblDateTime.setText(FormatterUtils.formatDate(Timestamp.valueOf(now)) + " - " + dayName);
        });
        clockTimer.start();
    }
    
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Logout?", "Konfirmasi", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.clearSession();
            dispose();
            new LoginForm().setVisible(true);
        }
    }
    
    // Inner class untuk cart item
    private static class CartItem {
        int productId;
        String productName;
        double price;
        int quantity;
        
        CartItem(int productId, String productName, double price, int quantity) {
            this.productId = productId;
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
        }
    }
    
    // Currency renderer
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