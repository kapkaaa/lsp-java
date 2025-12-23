package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.FileOutputStream;
import java.sql.*;
import com.toedter.calendar.JDateChooser;
import config.DatabaseConfig;
import utils.FormatterUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Sales Report Panel — Satu Baris + Auto-Refresh
class SalesReportPanel extends JPanel {
    private DefaultTableModel tableModel;
    private JTable table;
    private JDateChooser dateFrom, dateTo;
    private JComboBox<String> cmbCashier;
    private JLabel lblTotalTransactions, lblTotalRevenue, lblTotalProfit;
    
    public SalesReportPanel() {
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        
        // Header
        JLabel lblTitle = new JLabel("Laporan Penjualan");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);
        
        // Filter Panel — SATU BARIS
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);
        
        filterPanel.add(new JLabel("Dari:"));
        dateFrom = new JDateChooser();
        dateFrom.setPreferredSize(new Dimension(130, 30));
        styleDateChooser(dateFrom);
        filterPanel.add(dateFrom);
        
        filterPanel.add(new JLabel("Sampai:"));
        dateTo = new JDateChooser();
        dateTo.setPreferredSize(new Dimension(130, 30));
        styleDateChooser(dateTo);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
        dateFrom.setDate(cal.getTime());
        dateTo.setDate(new java.util.Date());
        filterPanel.add(dateTo);
        
        filterPanel.add(new JLabel("Kasir:"));
        cmbCashier = new JComboBox<>();
        cmbCashier.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbCashier.setBackground(Color.WHITE);
        cmbCashier.setForeground(Color.decode("#222222"));
        cmbCashier.addItem("Semua");
        loadCashiers();
        filterPanel.add(cmbCashier);
        
        // HANYA TOMBOL EXPORT
        JButton btnExport = createStyledButton("Export Excel", new Color(46, 204, 113), e -> exportToExcel());
        filterPanel.add(btnExport);
        
        // Summary Panel
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));
        
        summaryPanel.add(createSummaryCard("Total Transaksi", "0", new Color(52, 152, 219)));
        summaryPanel.add(createSummaryCard("Total Pendapatan", "Rp 0", new Color(46, 204, 113)));
        summaryPanel.add(createSummaryCard("Total Laba", "Rp 0", new Color(241, 196, 15)));
        
        // Table
        String[] columns = {"Kode Transaksi", "Tanggal", "Kasir", "Total", 
                           "Metode Bayar", "HPP", "Laba"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setRowHeight(28);
        table.setSelectionBackground(new Color(236, 240, 241));
        table.setSelectionForeground(Color.BLACK);
        
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(236, 240, 241));
        header.setForeground(Color.BLACK);
        header.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        table.getColumnModel().getColumn(3).setCellRenderer(new CurrencyRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new CurrencyRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new CurrencyRenderer());
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBackground(Color.WHITE);
        topPanel.add(filterPanel, BorderLayout.NORTH);
        topPanel.add(summaryPanel, BorderLayout.CENTER);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Auto-refresh listeners
        dateFrom.getDateEditor().addPropertyChangeListener("date", e -> loadData());
        dateTo.getDateEditor().addPropertyChangeListener("date", e -> loadData());
        cmbCashier.addActionListener(e -> loadData());
        
        loadData();
    }
    
    // Helper: summary card stylish
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
            case "Total Transaksi": lblTotalTransactions = new JLabel(value); lblValue = lblTotalTransactions; break;
            case "Total Pendapatan": lblTotalRevenue = new JLabel(value); lblValue = lblTotalRevenue; break;
            default: lblTotalProfit = new JLabel(value); lblValue = lblTotalProfit; break;
        }
        
        lblValue.setForeground(Color.WHITE);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        
        return card;
    }
    
    // Helper: tombol berwarna
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
    
    private void loadCashiers() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT DISTINCT u.id, u.name FROM users u " +
                        "JOIN roles r ON u.role_id = r.id " +
                        "WHERE r.name = 'cashier' ORDER BY u.name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                cmbCashier.addItem(rs.getInt("id") + " - " + rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading cashiers: " + e.getMessage());
        }
    }
    
    private void loadData() {
        tableModel.setRowCount(0);
        
        if (dateFrom.getDate() == null || dateTo.getDate() == null) {
            return;
        }

        if (dateFrom.getDate().after(dateTo.getDate())) {
            return;
        }
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT t.transaction_code, t.created_at, u.name as cashier, " +
                "t.total, t.payment_method, " +
                "SUM(td.quantity * p.cost_price) as hpp, " +
                "(t.total - SUM(td.quantity * p.cost_price)) as profit " +
                "FROM transactions t " +
                "JOIN users u ON t.user_id = u.id " +
                "JOIN transaction_details td ON t.id = td.transaction_id " +
                "JOIN products p ON td.product_id = p.id " +
                "WHERE t.transaction_status = 'completed' " +
                "AND DATE(t.created_at) BETWEEN ? AND ?"
            );
            
            String cashierFilter = (String) cmbCashier.getSelectedItem();
            if (cashierFilter != null && !cashierFilter.equals("Semua")) {
                int cashierId = Integer.parseInt(cashierFilter.split(" - ")[0]);
                sql.append(" AND t.user_id = ").append(cashierId);
            }
            
            sql.append(" GROUP BY t.id ORDER BY t.created_at DESC");
            
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            ps.setDate(1, new java.sql.Date(dateFrom.getDate().getTime()));
            ps.setDate(2, new java.sql.Date(dateTo.getDate().getTime()));
            
            ResultSet rs = ps.executeQuery();
            
            double totalRevenue = 0;
            double totalProfit = 0;
            int count = 0;
            
            while (rs.next()) {
                double revenue = rs.getDouble("total");
                double profit = rs.getDouble("profit");
                
                totalRevenue += revenue;
                totalProfit += profit;
                count++;
                
                Object[] row = {
                    rs.getString("transaction_code"),
                    FormatterUtils.formatDate(rs.getTimestamp("created_at")),
                    rs.getString("cashier"),
                    revenue,
                    rs.getString("payment_method"),
                    rs.getDouble("hpp"),
                    profit
                };
                tableModel.addRow(row);
            }
            
            lblTotalTransactions.setText(String.valueOf(count));
            lblTotalRevenue.setText(FormatterUtils.formatCurrency(totalRevenue));
            lblTotalProfit.setText(FormatterUtils.formatCurrency(totalProfit));
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void exportToExcel() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Tidak ada data untuk diekspor.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Laporan Penjualan");
        fileChooser.setSelectedFile(new java.io.File("Laporan_Penjualan.xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Laporan Penjualan");

            CellStyle headerStyle = createHeaderCellStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Kode Transaksi", "Tanggal", "Kasir", "Total", 
                               "Metode Bayar", "HPP", "Laba"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < tableModel.getRowCount(); r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    Object value = tableModel.getValueAt(r, c);
                    Cell cell = row.createCell(c);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                        if (c == 3 || c == 5 || c == 6) {
                            cell.setCellStyle(createCurrencyCellStyle(workbook));
                        }
                    } else {
                        cell.setCellValue(value == null ? "" : value.toString());
                    }
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(fileChooser.getSelectedFile())) {
                workbook.write(out);
            }

            JOptionPane.showMessageDialog(this, "Laporan berhasil diekspor!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mengekspor: " + e.getMessage());
        }
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
        return style;
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