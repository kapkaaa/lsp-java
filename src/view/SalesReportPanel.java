package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.List;
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

// Sales Report Panel
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
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);
        
        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);
        
        filterPanel.add(new JLabel("Dari:"));
        dateFrom = new JDateChooser();
        dateFrom.setPreferredSize(new Dimension(120, 25));
        filterPanel.add(dateFrom);
        
        filterPanel.add(new JLabel("Sampai:"));
        dateTo = new JDateChooser();
        dateTo.setPreferredSize(new Dimension(120, 25));
        dateTo.setDate(new java.util.Date());
        filterPanel.add(dateTo);
        
        filterPanel.add(new JLabel("Kasir:"));
        cmbCashier = new JComboBox<>();
        cmbCashier.addItem("Semua");
        loadCashiers();
        filterPanel.add(cmbCashier);
        
        JButton btnFilter = new JButton("Tampilkan");
        btnFilter.setBackground(new Color(52, 152, 219));
        btnFilter.setForeground(Color.WHITE);
        btnFilter.addActionListener(e -> loadData());
        filterPanel.add(btnFilter);
        
        JButton btnExport = new JButton("Export Excel");
        btnExport.setBackground(new Color(46, 204, 113));
        btnExport.setForeground(Color.WHITE);
        btnExport.addActionListener(e -> exportToExcel());
        filterPanel.add(btnExport);
        
        // Summary Panel
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
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
        table.setRowHeight(25);
        table.getColumnModel().getColumn(3).setCellRenderer(new CurrencyRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new CurrencyRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new CurrencyRenderer());
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBackground(Color.WHITE);
        topPanel.add(filterPanel, BorderLayout.NORTH);
        topPanel.add(summaryPanel, BorderLayout.CENTER);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Load data for last 7 days by default
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
        dateFrom.setDate(cal.getTime());
        dateTo.setDate(new java.util.Date());
        loadData();
    }
    
    private JPanel createSummaryCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(color);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Arial", Font.PLAIN, 12));
        
        JLabel lblValue;
        if (title.equals("Total Transaksi")) {
            lblTotalTransactions = new JLabel(value);
            lblValue = lblTotalTransactions;
        } else if (title.equals("Total Pendapatan")) {
            lblTotalRevenue = new JLabel(value);
            lblValue = lblTotalRevenue;
        } else {
            lblTotalProfit = new JLabel(value);
            lblValue = lblTotalProfit;
        }
        
        lblValue.setForeground(Color.WHITE);
        lblValue.setFont(new Font("Arial", Font.BOLD, 20));
        
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        
        return card;
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
            JOptionPane.showMessageDialog(this, "Pilih periode tanggal!");
            return;
        }

        // Validasi: dateFrom tidak boleh setelah dateTo
        if (dateFrom.getDate().after(dateTo.getDate())) {
            JOptionPane.showMessageDialog(this, "Tanggal 'Dari' tidak boleh lebih besar dari 'Sampai'!");
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
            
            // Update summary
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

            // Header style
            CellStyle headerStyle = createHeaderCellStyle(workbook);

            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Kode Transaksi", "Tanggal", "Kasir", "Total", 
                               "Metode Bayar", "HPP", "Laba"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
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

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write file
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
}