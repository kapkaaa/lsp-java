package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import config.DatabaseConfig;
import utils.FormatterUtils;
import utils.OperationalHoursValidator;
import utils.SessionManager;

public class DashboardKasir extends JFrame {
    private JLabel lblWelcome, lblDateTime;
    private JPanel contentPanel;
    private Timer clockTimer;
    
    // Transaction components
    private DefaultTableModel productTableModel;
    private DefaultTableModel cartTableModel;
    private JTable productTable, cartTable;
    private JTextField txtSearch, txtTunai;
    private JComboBox<String> cmbPaymentMethod;
    private JLabel lblTotal, lblKembalian;
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
        setSize(1200, 750);
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
        
//        JButton btnRefresh = new JButton("Refresh");
//        btnRefresh.addActionListener(e -> {
//            txtSearch.setText("");
//            loadProducts();
//        });
//        searchPanel.add(btnRefresh);
        
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
        
        productTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addToCart();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(productTable);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(Color.WHITE);
        
        JButton btnAdd = new JButton("Tambah ke Keranjang");
        btnAdd.setBackground(new Color(46, 204, 113));
        btnAdd.setForeground(Color.black);
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
        
        String[] columns = {"Produk", "Harga", "Qty", "Subtotal"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        cartTable = new JTable(cartTableModel);
        cartTable.setRowHeight(25);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.getColumnModel().getColumn(1).setCellRenderer(new CurrencyRenderer());
        cartTable.getColumnModel().getColumn(3).setCellRenderer(new CurrencyRenderer());
        
        JScrollPane scrollPane = new JScrollPane(cartTable);
        
        JPanel cartBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cartBtnPanel.setBackground(Color.WHITE);
        
        JButton btnUpdate = new JButton("Update");
        btnUpdate.addActionListener(e -> updateCartQuantityViaDialog());
        cartBtnPanel.add(btnUpdate);
        
        JButton btnRemove = new JButton("Hapus");
        btnRemove.setBackground(new Color(231, 76, 60));
        btnRemove.setForeground(Color.black);
        btnRemove.addActionListener(e -> removeFromCart());
        cartBtnPanel.add(btnRemove);
        
        JButton btnClear = new JButton("Kosongkan");
        btnClear.addActionListener(e -> clearCart());
        cartBtnPanel.add(btnClear);
        
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
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("TOTAL:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        lblTotal = new JLabel("Rp 0");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 20));
        lblTotal.setForeground(new Color(46, 204, 113));
        panel.add(lblTotal, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Metode Bayar:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        cmbPaymentMethod = new JComboBox<>(new String[]{"cash", "qris", "transfer"});
        cmbPaymentMethod.addActionListener(e -> updatePaymentFields());
        panel.add(cmbPaymentMethod, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Tunai:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 2;
        txtTunai = new JTextField("0");
        txtTunai.setColumns(15);
        txtTunai.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                updateKembalian();
            }
        });
        panel.add(txtTunai, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Kembalian:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 3;
        lblKembalian = new JLabel("Rp 0");
        lblKembalian.setFont(new Font("Arial", Font.BOLD, 16));
        lblKembalian.setForeground(new Color(231, 76, 60));
        panel.add(lblKembalian, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        JButton btnProcess = new JButton("PROSES TRANSAKSI");
        btnProcess.setFont(new Font("Arial", Font.BOLD, 14));
        btnProcess.setBackground(new Color(52, 152, 219));
        btnProcess.setForeground(Color.black);
        btnProcess.setPreferredSize(new Dimension(0, 40));
        btnProcess.addActionListener(e -> processTransaction());
        panel.add(btnProcess, gbc);
        
        updatePaymentFields();
        return panel;
    }
    
    private void updatePaymentFields() {
        String method = (String) cmbPaymentMethod.getSelectedItem();
        boolean isCash = "cash".equals(method);
        
        txtTunai.setEnabled(isCash);
        txtTunai.setEditable(isCash);
        
        double total = cartItems.stream()
            .mapToDouble(i -> i.price * i.quantity)
            .sum();
        
        if (!isCash) {
            txtTunai.setText(String.valueOf(total));
            lblKembalian.setText("Rp 0");
        } else {
            if (txtTunai.getText().trim().isEmpty()) {
                txtTunai.setText("0");
            }
            updateKembalian();
        }
    }
    
    private void updateKembalian() {
        if ("cash".equals(cmbPaymentMethod.getSelectedItem())) {
            try {
                double total = cartItems.stream()
                    .mapToDouble(i -> i.price * i.quantity)
                    .sum();
                double tunai = Double.parseDouble(txtTunai.getText().trim());
                double kembalian = tunai - total;
                lblKembalian.setText(FormatterUtils.formatCurrency(kembalian));
                lblKembalian.setForeground(kembalian < 0 ? Color.RED : new Color(231, 76, 60));
            } catch (NumberFormatException e) {
                lblKembalian.setText("Rp 0");
                lblKembalian.setForeground(new Color(231, 76, 60));
            }
        } else {
            lblKembalian.setText("Rp 0");
            lblKembalian.setForeground(new Color(231, 76, 60));
        }
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
    
    private void updateCartQuantityViaDialog() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih item yang akan diupdate!");
            return;
        }

        CartItem item = cartItems.get(row);
        String input = JOptionPane.showInputDialog(this, 
            "Ubah jumlah untuk '" + item.productName + "':", 
            item.quantity);
        
        if (input == null) return;

        try {
            int newQty = Integer.parseInt(input.trim());
            if (newQty <= 0) {
                JOptionPane.showMessageDialog(this, "Jumlah harus lebih dari 0!");
                return;
            }

            int availableStock = getAvailableStock(item.productId);
            if (newQty > availableStock) {
                JOptionPane.showMessageDialog(this, 
                    "Stok tidak mencukupi! Tersedia: " + availableStock);
                return;
            }

            item.quantity = newQty;
            updateCartDisplay();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Input tidak valid!");
        }
    }

    private int getAvailableStock(int productId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT stock FROM products WHERE id = ? AND status = 'available'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
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
        
        cartItems.clear();
        updateCartDisplay();
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
        updatePaymentFields();
    }
    
    private void processTransaction() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keranjang kosong!");
            return;
        }
        
        double total = cartItems.stream().mapToDouble(i -> i.price * i.quantity).sum();
        String paymentMethod = (String) cmbPaymentMethod.getSelectedItem();
        
        if ("cash".equals(paymentMethod)) {
            try {
                double tunai = Double.parseDouble(txtTunai.getText().trim());
                if (tunai < total) {
                    JOptionPane.showMessageDialog(this, "Tunai tidak mencukupi!");
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Input tunai tidak valid!");
                return;
            }
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Proses transaksi dengan total " + FormatterUtils.formatCurrency(total) + "?",
            "Konfirmasi", JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        Connection conn = null;
        int transId = 0;
        String transCode = "";
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            // Hitung tunai & kembalian
            double tunaiValue = 0;
            double kembalianValue = 0;
            if ("cash".equals(paymentMethod)) {
                tunaiValue = Double.parseDouble(txtTunai.getText().trim());
                kembalianValue = tunaiValue - total;
            } else {
                tunaiValue = total;
                kembalianValue = 0;
            }
            
            // Insert transaction (dengan cash_received & change_given)
            transCode = FormatterUtils.generateTransactionCode();
            String sqlTrans = "INSERT INTO transactions (user_id, transaction_code, total, " +
                             "payment_method, transaction_status, cash_received, change_given, created_at) " +
                             "VALUES (?, ?, ?, ?, 'completed', ?, ?, NOW())";
            PreparedStatement psTrans = conn.prepareStatement(sqlTrans, Statement.RETURN_GENERATED_KEYS);
            psTrans.setInt(1, SessionManager.getCurrentUserId());
            psTrans.setString(2, transCode);
            psTrans.setDouble(3, total);
            psTrans.setString(4, paymentMethod);
            psTrans.setDouble(5, tunaiValue);      // cash_received
            psTrans.setDouble(6, kembalianValue);  // change_given
            psTrans.executeUpdate();
            
            ResultSet rs = psTrans.getGeneratedKeys();
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
            
            // ✅ Tampilkan struk (ambil data dari DB)
            showReceipt(transId, transCode);
            
            clearCart();
            loadProducts();
            
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            JOptionPane.showMessageDialog(this, "Error saat proses transaksi: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    private void showReceipt(int transId, String transCode) {
        StringBuilder receipt = new StringBuilder();
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            
            String sql1 = "SELECT t.total, t.payment_method, t.created_at, " +
                         "t.cash_received, t.change_given, u.name AS cashier " +
                         "FROM transactions t " +
                         "JOIN users u ON t.user_id = u.id " +
                         "WHERE t.id = ?";
            
            PreparedStatement ps1 = conn.prepareStatement(sql1);
            ps1.setInt(1, transId);
            ResultSet rs1 = ps1.executeQuery();
            
            if (!rs1.next()) {
                JOptionPane.showMessageDialog(this, "Data transaksi tidak ditemukan!");
                return;
            }
            
            double total = rs1.getDouble("total");
            String paymentMethod = rs1.getString("payment_method");
            double cashReceived = rs1.getDouble("cash_received");
            double changeGiven = rs1.getDouble("change_given");
            
            receipt.append("═".repeat(50)).append("\n");
            receipt.append("              DISTROZONE              \n");
            receipt.append("   Jln. Raya Pegangsaan Timur No.29H  \n");
            receipt.append("         Kelapa Gading Jakarta        \n");
            receipt.append("          Telp: 081234567890          \n");
            receipt.append("═".repeat(50)).append("\n\n");
            
            receipt.append("Kasir  : ").append(rs1.getString("cashier")).append("\n");
            receipt.append("Tanggal: ").append(FormatterUtils.formatDate(rs1.getTimestamp("created_at"))).append("\n");
            receipt.append("No.    : ").append(transCode).append("\n");
            receipt.append("─".repeat(50)).append("\n\n");

            String sql2 = "SELECT p.name, td.quantity, td.unit_price, td.subtotal " +
                         "FROM transaction_details td " +
                         "JOIN products p ON td.product_id = p.id " +
                         "WHERE td.transaction_id = ?";
            
            PreparedStatement ps2 = conn.prepareStatement(sql2);
            ps2.setInt(1, transId);
            ResultSet rs2 = ps2.executeQuery();
            
            while (rs2.next()) {
                String name = rs2.getString("name");
                int qty = rs2.getInt("quantity");
                double price = rs2.getDouble("unit_price");
                double subtotal = rs2.getDouble("subtotal");
                
                receipt.append(name).append("\n");
                receipt.append(String.format("  %d x %s = %s\n", 
                    qty,
                    FormatterUtils.formatCurrency(price),
                    FormatterUtils.formatCurrency(subtotal)));
            }
            
            receipt.append("\n").append("─".repeat(50)).append("\n");
            receipt.append(String.format("%-30s %s\n", "TOTAL:", FormatterUtils.formatCurrency(total)));
            
            if ("cash".equals(paymentMethod)) {
                receipt.append(String.format("%-30s %s\n", "TUNAI:", FormatterUtils.formatCurrency(cashReceived)));
                receipt.append(String.format("%-30s %s\n", "KEMBALIAN:", FormatterUtils.formatCurrency(changeGiven)));
            } else {
                receipt.append(String.format("%-30s %s\n", "METODE BAYAR:", paymentMethod.toUpperCase()));
            }
            
            receipt.append("─".repeat(50)).append("\n");
            receipt.append("\n    Terima kasih atas kunjungan Anda!\n");
            receipt.append("        www.distrozone.vercel.app      \n");
            receipt.append("═".repeat(50)).append("\n");
            
            JTextArea textArea = new JTextArea(receipt.toString());
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            textArea.setEditable(false);
            JScrollPane scroll = new JScrollPane(textArea);
            scroll.setPreferredSize(new Dimension(320, 490));
            
            JOptionPane.showMessageDialog(this, scroll, "STRUK TRANSAKSI", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menampilkan struk: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
    
    private void showTransactionHistory() {
        new TransactionHistoryDialog(this).setVisible(true);
    }
    
    private void startClock() {
        clockTimer = new Timer(1000, e -> {
            LocalDateTime now = LocalDateTime.now();
            String formattedDate = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"));
            String dayName = OperationalHoursValidator.getOperationalMessage("store");
            lblDateTime.setText(formattedDate + " - " + dayName);
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