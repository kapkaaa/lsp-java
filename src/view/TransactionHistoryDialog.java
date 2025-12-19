package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import com.toedter.calendar.JDateChooser;
import config.DatabaseConfig;
import utils.*;

public class TransactionHistoryDialog extends JDialog {
    private DefaultTableModel tableModel;
    private JTable table;
    private JDateChooser dateFrom, dateTo;
    private JLabel lblTotalCash, lblTotalQRIS, lblTotalTransfer, lblTotalPenjualan;

    public TransactionHistoryDialog(Frame parent) {
        super(parent, "Riwayat Transaksi", true);
        initComponents();
        loadData();
    }

    private void initComponents() {
        setSize(950, 620);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(52, 152, 219));
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel lblTitle = new JLabel("Riwayat Transaksi - " + SessionManager.getCurrentUserName());
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        headerPanel.add(lblTitle, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        filterPanel.add(new JLabel("Dari:"));
        dateFrom = new JDateChooser();
        dateFrom.setPreferredSize(new Dimension(120, 25));
        filterPanel.add(dateFrom);

        filterPanel.add(new JLabel("Sampai:"));
        dateTo = new JDateChooser();
        dateTo.setPreferredSize(new Dimension(120, 25));
        dateTo.setDate(new java.util.Date());
        filterPanel.add(dateTo);

        JButton btnFilter = new JButton("Tampilkan");
        btnFilter.setBackground(new Color(52, 152, 219));
        btnFilter.setForeground(Color.BLACK);
        btnFilter.setFocusPainted(false);
        btnFilter.addActionListener(e -> validateAndLoadData());
        filterPanel.add(btnFilter);

        JButton btnPrint = new JButton("Cetak Struk");
        btnPrint.setBackground(new Color(46, 204, 113));
        btnPrint.setForeground(Color.BLACK);
        btnPrint.setFocusPainted(false);
        btnPrint.addActionListener(e -> printReceipt());
        filterPanel.add(btnPrint);

        // Summary Panel (4 cards)
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel cardCash = createSummaryCard("Cash", "Rp 0", new Color(39, 174, 96));       // Hijau
        JPanel cardQRIS = createSummaryCard("QRIS", "Rp 0", new Color(41, 128, 185));      // Biru
        JPanel cardTransfer = createSummaryCard("Transfer", "Rp 0", new Color(155, 89, 182)); // Ungu
        JPanel cardTotal = createSummaryCard("Total Penjualan", "Rp 0", new Color(230, 126, 34)); // Oranye

        summaryPanel.add(cardCash);
        summaryPanel.add(cardQRIS);
        summaryPanel.add(cardTransfer);
        summaryPanel.add(cardTotal);

        // Table
        String[] columns = {"Kode", "Tanggal", "Total", "Metode", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setCellRenderer(new CurrencyRenderer());

        // Double click to view details
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showTransactionDetails();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnClose = new JButton("Tutup");
        btnClose.setPreferredSize(new Dimension(100, 35));
        btnClose.addActionListener(e -> dispose());
        bottomPanel.add(btnClose);

        // Main Content Panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.add(filterPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(summaryPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(centerPanel, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);

        // Set default date today
        java.util.Calendar cal = java.util.Calendar.getInstance();
        dateFrom.setDate(new java.util.Date());
        dateTo.setDate(new java.util.Date());
    }

    private JPanel createSummaryCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(color);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Arial", Font.PLAIN, 12));

        JLabel lblValue;
        switch (title) {
            case "Cash":
                lblTotalCash = new JLabel(value);
                lblValue = lblTotalCash;
                break;
            case "QRIS":
                lblTotalQRIS = new JLabel(value);
                lblValue = lblTotalQRIS;
                break;
            case "Transfer":
                lblTotalTransfer = new JLabel(value);
                lblValue = lblTotalTransfer;
                break;
            case "Total Penjualan":
                lblTotalPenjualan = new JLabel(value);
                lblValue = lblTotalPenjualan;
                break;
            default:
                lblValue = new JLabel(value);
        }

        lblValue.setForeground(Color.WHITE);
        lblValue.setFont(new Font("Arial", Font.BOLD, 16));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);

        return card;
    }

    private void loadData() {
        tableModel.setRowCount(0);

        if (dateFrom.getDate() == null || dateTo.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Pilih periode tanggal!");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id, transaction_code, created_at, total, " +
                        "payment_method, transaction_status " +
                        "FROM transactions " +
                        "WHERE user_id = ? AND DATE(created_at) BETWEEN ? AND ? " +
                        "ORDER BY created_at DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, SessionManager.getCurrentUserId());
            ps.setDate(2, new java.sql.Date(dateFrom.getDate().getTime()));
            ps.setDate(3, new java.sql.Date(dateTo.getDate().getTime()));

            ResultSet rs = ps.executeQuery();

            double totalCash = 0, totalQRIS = 0, totalTransfer = 0, totalAll = 0;

            while (rs.next()) {
                double amount = rs.getDouble("total");
                String status = rs.getString("transaction_status");
                String method = rs.getString("payment_method").toLowerCase();

                Object[] row = {
                    rs.getString("transaction_code"),
                    FormatterUtils.formatDate(rs.getTimestamp("created_at")),
                    amount,
                    rs.getString("payment_method").toUpperCase(),
                    status.toUpperCase()
                };
                tableModel.addRow(row);

                if ("completed".equals(status)) {
                    totalAll += amount;
                    switch (method) {
                        case "cash":
                            totalCash += amount;
                            break;
                        case "qris":
                            totalQRIS += amount;
                            break;
                        case "transfer":
                            totalTransfer += amount;
                            break;
                    }
                }
            }

            // Update summary cards
            lblTotalCash.setText(FormatterUtils.formatCurrency(totalCash));
            lblTotalQRIS.setText(FormatterUtils.formatCurrency(totalQRIS));
            lblTotalTransfer.setText(FormatterUtils.formatCurrency(totalTransfer));
            lblTotalPenjualan.setText(FormatterUtils.formatCurrency(totalAll));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void validateAndLoadData() {
        if (dateFrom.getDate() == null || dateTo.getDate() == null) {
            JOptionPane.showMessageDialog(this, 
                "Harap pilih periode tanggal terlebih dahulu!", 
                "Validasi Tanggal", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.Date fromDate = dateFrom.getDate();
        java.util.Date toDate = dateTo.getDate();

        if (fromDate.after(toDate)) {
            JOptionPane.showMessageDialog(this,
                "Tanggal 'Dari' tidak boleh lebih dari tanggal 'Sampai'!",
                "Validasi Tanggal",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        loadData();
    }

    private void showTransactionDetails() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih transaksi terlebih dahulu!");
            return;
        }

        String transCode = (String) table.getValueAt(row, 0);

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql1 = "SELECT id FROM transactions WHERE transaction_code = ?";
            PreparedStatement ps1 = conn.prepareStatement(sql1);
            ps1.setString(1, transCode);
            ResultSet rs1 = ps1.executeQuery();

            if (rs1.next()) {
                int transId = rs1.getInt("id");

                String sql2 = "SELECT p.name, b.name as brand, s.name as size, " +
                            "c.name as color, td.quantity, td.unit_price, td.subtotal " +
                            "FROM transaction_details td " +
                            "JOIN products p ON td.product_id = p.id " +
                            "JOIN brands b ON p.brand_id = b.id " +
                            "JOIN sizes s ON p.size_id = s.id " +
                            "JOIN colors c ON p.color_id = c.id " +
                            "WHERE td.transaction_id = ?";

                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setInt(1, transId);
                ResultSet rs2 = ps2.executeQuery();

                StringBuilder details = new StringBuilder();
                details.append("DETAIL TRANSAKSI\n");
                details.append("Kode: ").append(transCode).append("\n\n");
                details.append(String.format("%-30s %5s %12s %12s\n", 
                    "PRODUK", "QTY", "HARGA", "SUBTOTAL"));
                details.append("─".repeat(70)).append("\n");

                while (rs2.next()) {
                    String product = String.format("%s (%s, %s, %s)",
                        rs2.getString("name"),
                        rs2.getString("brand"),
                        rs2.getString("size"),
                        rs2.getString("color"));

                    details.append(String.format("%-30s %5d %12s %12s\n",
                        product.length() > 30 ? product.substring(0, 27) + "..." : product,
                        rs2.getInt("quantity"),
                        FormatterUtils.formatCurrency(rs2.getDouble("unit_price")),
                        FormatterUtils.formatCurrency(rs2.getDouble("subtotal"))));
                }

                details.append("─".repeat(70)).append("\n");
                details.append(String.format("%-48s %12s\n", "TOTAL:",
                    FormatterUtils.formatCurrency((Double) table.getValueAt(row, 2))));

                JTextArea textArea = new JTextArea(details.toString());
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
                textArea.setEditable(false);

                JOptionPane.showMessageDialog(this, new JScrollPane(textArea),
                    "Detail Transaksi", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void printReceipt() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih transaksi yang akan dicetak!");
            return;
        }

        String transCode = (String) table.getValueAt(row, 0);

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql1 = "SELECT t.*, u.name as cashier FROM transactions t " +
                        "JOIN users u ON t.user_id = u.id " +
                        "WHERE t.transaction_code = ?";
            PreparedStatement ps1 = conn.prepareStatement(sql1);
            ps1.setString(1, transCode);
            ResultSet rs1 = ps1.executeQuery();

            if (rs1.next()) {
                int transId = rs1.getInt("id");

                StringBuilder receipt = new StringBuilder();
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

                double total = rs1.getDouble("total");
                String paymentMethod = rs1.getString("payment_method");

                receipt.append("\n").append("─".repeat(50)).append("\n");
                receipt.append(String.format("%-30s %s\n", "TOTAL:", 
                    FormatterUtils.formatCurrency(total)));
                receipt.append(String.format("%-30s %s\n", "Metode Bayar:", 
                    paymentMethod.toUpperCase()));
                receipt.append("─".repeat(50)).append("\n");
                receipt.append("\n    Terima kasih atas kunjungan Anda!\n");
                receipt.append("        www.distrozone.vercel.app      \n");
                receipt.append("═".repeat(50)).append("\n");

                JTextArea textArea = new JTextArea(receipt.toString());
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
                textArea.setEditable(false);

                JDialog receiptDialog = new JDialog(this, "Struk Pembayaran", true);
                receiptDialog.setSize(320, 450);
                receiptDialog.setLocationRelativeTo(this);
                receiptDialog.setLayout(new BorderLayout());

                receiptDialog.add(new JScrollPane(textArea), BorderLayout.CENTER);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                JButton btnPrint = new JButton("Print");
                btnPrint.addActionListener(e -> {
                    try {
                        textArea.print();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(receiptDialog, 
                            "Error printing: " + ex.getMessage());
                    }
                });
                JButton btnClose = new JButton("Tutup");
                btnClose.addActionListener(e -> receiptDialog.dispose());

                btnPanel.add(btnPrint);
                btnPanel.add(btnClose);
                receiptDialog.add(btnPanel, BorderLayout.SOUTH);

                receiptDialog.setVisible(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
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