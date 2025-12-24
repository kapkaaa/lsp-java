package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import com.toedter.calendar.JDateChooser;
import config.DatabaseConfig;
import utils.*;

public class TransactionHistoryDialog extends JDialog {
    private DefaultTableModel tableModel;
    private JTable table;
    private JDateChooser dateFrom, dateTo;
    private JLabel lblTotalCash, lblTotalQRIS, lblTotalTransfer, lblTotalPenjualan;
    private Point mousePoint;

    public TransactionHistoryDialog(Frame parent) {
        super(parent, false); // non-modal, tanpa title bar sistem
        this.setUndecorated(true);
        initComponents();
        loadData();
    }

    private void initComponents() {
        setSize(950, 620);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        // =================== CUSTOM TITLE BAR ===================
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
        titleBar.setPreferredSize(new Dimension(950, 40));
        titleBar.setOpaque(false);

        JButton btnClose = createMacOSButton(new Color(0xFF5F57));
        btnClose.addActionListener(e -> dispose());

        JLabel titleLabel = new JLabel("Riwayat Transaksi - " + SessionManager.getCurrentUserName(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.decode("#222222"));
        titleLabel.setOpaque(false);

        titleBar.add(btnClose);
        titleBar.add(Box.createHorizontalGlue());
        titleBar.add(titleLabel);
        titleBar.add(Box.createHorizontalGlue());

        add(titleBar, BorderLayout.NORTH);

        // =================== MAIN CONTENT PANEL ===================
        JPanel mainPanel = new JPanel(new BorderLayout()) {
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        mainPanel.setOpaque(false);

        // Header (simpan info kasir di dalam mainPanel)
        JLabel lblHeader = new JLabel("Riwayat Transaksi", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblHeader.setForeground(Color.decode("#222222"));
        lblHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        mainPanel.add(lblHeader, BorderLayout.NORTH);

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setOpaque(false);

        filterPanel.add(new JLabel("Dari:"));
        dateFrom = new JDateChooser();
        dateFrom.setPreferredSize(new Dimension(130, 30));
        styleDateChooser(dateFrom);
        filterPanel.add(dateFrom);

        filterPanel.add(new JLabel("Sampai:"));
        dateTo = new JDateChooser();
        dateTo.setPreferredSize(new Dimension(130, 30));
        styleDateChooser(dateTo);
        java.util.Date today = new java.util.Date();
        dateFrom.setDate(today);
        dateTo.setDate(today);
        filterPanel.add(dateTo);

        JButton btnFilter = createStyledButton("Tampilkan", new Color(52, 152, 219), e -> validateAndLoadData());
        JButton btnPrint = createStyledButton("Cetak Struk", new Color(46, 204, 113), e -> printReceipt());

        filterPanel.add(btnFilter);
        filterPanel.add(btnPrint);

        // Summary Panel
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setOpaque(false);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));

        summaryPanel.add(createSummaryCard("Cash", "Rp 0", new Color(39, 174, 96)));
        summaryPanel.add(createSummaryCard("QRIS", "Rp 0", new Color(41, 128, 185)));
        summaryPanel.add(createSummaryCard("Transfer", "Rp 0", new Color(155, 89, 182)));
        summaryPanel.add(createSummaryCard("Total Penjualan", "Rp 0", new Color(230, 126, 34)));

        // Table
        String[] columns = {"Kode", "Tanggal", "Total", "Metode", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setSelectionBackground(new Color(236, 240, 241));
        table.setSelectionForeground(Color.BLACK);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setCellRenderer(new CurrencyRenderer());

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(236, 240, 241));
        header.setForeground(Color.BLACK);
        header.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showTransactionDetails();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.setOpaque(false);

        JButton btnClosee = createStyledButton("Tutup", Color.GRAY, e -> dispose());
        bottomPanel.add(btnClosee);

        // Assemble
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(summaryPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(filterPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Drag & shape
        addWindowDrag(titleBar);
        updateWindowShape();
    }

    // === HELPER METHODS ===
    private JPanel createSummaryCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(color);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblValue;
        switch (title) {
            case "Cash": lblTotalCash = new JLabel(value); lblValue = lblTotalCash; break;
            case "QRIS": lblTotalQRIS = new JLabel(value); lblValue = lblTotalQRIS; break;
            case "Transfer": lblTotalTransfer = new JLabel(value); lblValue = lblTotalTransfer; break;
            case "Total Penjualan": lblTotalPenjualan = new JLabel(value); lblValue = lblTotalPenjualan; break;
            default: lblValue = new JLabel(value);
        }

        lblValue.setForeground(Color.WHITE);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 16));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);

        return card;
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
        btn.setPreferredSize(new Dimension(120, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }

    // Helper: date chooser stylish
    private void styleDateChooser(JDateChooser chooser) {
        JFormattedTextField textField =
        (JFormattedTextField) chooser.getDateEditor().getUiComponent();
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        textField.setBackground(Color.WHITE);
        textField.setForeground(Color.decode("#222222"));
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

    private void addWindowDrag(Component comp) {
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mousePoint = e.getPoint();
            }
        });
        comp.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point curr = e.getLocationOnScreen();
                setLocation(curr.x - mousePoint.x, curr.y - mousePoint.y);
            }
        });
    }

    private void updateWindowShape() {
        int arc = 20;
        Shape shape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc);
        setShape(shape);
    }

    // === LOGIKA UTAMA (TETAP SAMA) ===
    private void loadData() {
        tableModel.setRowCount(0);

        if (dateFrom.getDate() == null || dateTo.getDate() == null) {
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
                        case "cash": totalCash += amount; break;
                        case "qris": totalQRIS += amount; break;
                        case "transfer": totalTransfer += amount; break;
                    }
                }
            }

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
            JOptionPane.showMessageDialog(this, "Harap pilih periode tanggal terlebih dahulu!");
            return;
        }

        java.util.Date fromDate = dateFrom.getDate();
        java.util.Date toDate = dateTo.getDate();

        if (fromDate.after(toDate)) {
            JOptionPane.showMessageDialog(this, "Tanggal 'Dari' tidak boleh lebih dari tanggal 'Sampai'!");
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