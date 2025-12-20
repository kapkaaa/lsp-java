package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
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

// Profit Report Panel
class ProfitReportPanel extends JPanel {
    private DefaultTableModel tableModel;
    private JTable table;
    private JDateChooser dateFrom, dateTo;
    private JLabel lblTotalRevenue, lblTotalCost, lblTotalProfit, lblProfitMargin;
    
    public ProfitReportPanel() {
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        
        // Header
        JLabel lblTitle = new JLabel("Laporan Laba/Rugi");
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
        JPanel summaryPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        summaryPanel.add(createSummaryCard("Total Pendapatan", "Rp 0", new Color(52, 152, 219), true));
        summaryPanel.add(createSummaryCard("Total HPP", "Rp 0", new Color(231, 76, 60), false));
        summaryPanel.add(createSummaryCard("Laba Bersih", "Rp 0", new Color(46, 204, 113), true));
        summaryPanel.add(createSummaryCard("Margin Laba", "0%", new Color(241, 196, 15), true));
        
        // Table
        String[] columns = {"Produk", "Merek", "Terjual", "Harga Jual", "HPP", 
                           "Pendapatan", "Laba"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(3).setCellRenderer(new CurrencyRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new CurrencyRenderer());
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
        
        // Load data for current month by default
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        dateFrom.setDate(cal.getTime());
        dateTo.setDate(new java.util.Date());
        loadData();
    }
    
    private JPanel createSummaryCard(String title, String value, Color color, boolean isRevenue) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(color);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Arial", Font.PLAIN, 12));
        
        JLabel lblValue;
        if (title.equals("Total Pendapatan")) {
            lblTotalRevenue = new JLabel(value);
            lblValue = lblTotalRevenue;
        } else if (title.equals("Total HPP")) {
            lblTotalCost = new JLabel(value);
            lblValue = lblTotalCost;
        } else if (title.equals("Laba Bersih")) {
            lblTotalProfit = new JLabel(value);
            lblValue = lblTotalProfit;
        } else {
            lblProfitMargin = new JLabel(value);
            lblValue = lblProfitMargin;
        }
        
        lblValue.setForeground(Color.WHITE);
        lblValue.setFont(new Font("Arial", Font.BOLD, 18));
        
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

        // Validasi: dateFrom tidak boleh setelah dateTo
        if (dateFrom.getDate().after(dateTo.getDate())) {
            JOptionPane.showMessageDialog(this, "Tanggal 'Dari' tidak boleh lebih besar dari 'Sampai'!");
            return;
        }
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT p.name, b.name as brand, " +
                        "SUM(td.quantity) as qty_sold, " +
                        "AVG(td.unit_price) as avg_price, " +
                        "AVG(p.cost_price) as avg_cost, " +
                        "SUM(td.subtotal) as revenue, " +
                        "SUM(td.subtotal - (td.quantity * p.cost_price)) as profit " +
                        "FROM transaction_details td " +
                        "JOIN transactions t ON td.transaction_id = t.id " +
                        "JOIN products p ON td.product_id = p.id " +
                        "JOIN brands b ON p.brand_id = b.id " +
                        "WHERE t.transaction_status = 'completed' " +
                        "AND DATE(t.created_at) BETWEEN ? AND ? " +
                        "GROUP BY td.product_id " +
                        "ORDER BY profit DESC";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDate(1, new java.sql.Date(dateFrom.getDate().getTime()));
            ps.setDate(2, new java.sql.Date(dateTo.getDate().getTime()));
            
            ResultSet rs = ps.executeQuery();
            
            double totalRevenue = 0;
            double totalCost = 0;
            
            while (rs.next()) {
                int qtySold = rs.getInt("qty_sold");
                double avgPrice = rs.getDouble("avg_price");
                double avgCost = rs.getDouble("avg_cost");
                double revenue = rs.getDouble("revenue");
                double profit = rs.getDouble("profit");
                
                totalRevenue += revenue;
                totalCost += (qtySold * avgCost);
                
                Object[] row = {
                    rs.getString("name"),
                    rs.getString("brand"),
                    qtySold,
                    avgPrice,
                    avgCost,
                    revenue,
                    profit
                };
                tableModel.addRow(row);
            }
            
            double totalProfit = totalRevenue - totalCost;
            double margin = totalRevenue > 0 ? (totalProfit / totalRevenue * 100) : 0;
            
            lblTotalRevenue.setText(FormatterUtils.formatCurrency(totalRevenue));
            lblTotalCost.setText(FormatterUtils.formatCurrency(totalCost));
            lblTotalProfit.setText(FormatterUtils.formatCurrency(totalProfit));
            lblProfitMargin.setText(String.format("%.2f%%", margin));
            
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
        fileChooser.setDialogTitle("Simpan Laporan Laba/Rugi");
        fileChooser.setSelectedFile(new java.io.File("Laporan_Laba_Rugi.xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Laporan Laba Rugi");

            CellStyle headerStyle = createHeaderCellStyle(workbook);

            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Produk", "Merek", "Terjual", "Harga Jual", "HPP", 
                               "Pendapatan", "Laba"};
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
                        if (c >= 3) { // Kolom mata uang
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
}