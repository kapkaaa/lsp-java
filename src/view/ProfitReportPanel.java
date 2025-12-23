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

// Profit Report Panel — Satu Baris + Auto-Refresh (Versi Kavi Laundry)
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
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        dateFrom.setDate(cal.getTime());
        dateTo.setDate(new java.util.Date());
        filterPanel.add(dateTo);
        
        // HANYA TOMBOL EXPORT
        JButton btnExport = createStyledButton("Export Excel", new Color(46, 204, 113), e -> exportToExcel());
        filterPanel.add(btnExport);
        
        // Summary Panel
        JPanel summaryPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));
        
        summaryPanel.add(createSummaryCard("Total Pendapatan", "Rp 0", new Color(52, 152, 219)));
        summaryPanel.add(createSummaryCard("Total HPP", "Rp 0", new Color(231, 76, 60)));
        summaryPanel.add(createSummaryCard("Laba Bersih", "Rp 0", new Color(46, 204, 113)));
        summaryPanel.add(createSummaryCard("Margin Laba", "0%", new Color(241, 196, 15)));
        
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
        table.getColumnModel().getColumn(4).setCellRenderer(new CurrencyRenderer());
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
        
        // Auto-refresh saat tanggal berubah
        dateFrom.getDateEditor().addPropertyChangeListener("date", e -> loadData());
        dateTo.getDateEditor().addPropertyChangeListener("date", e -> loadData());
        
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
            case "Total Pendapatan": lblTotalRevenue = new JLabel(value); lblValue = lblTotalRevenue; break;
            case "Total HPP": lblTotalCost = new JLabel(value); lblValue = lblTotalCost; break;
            case "Laba Bersih": lblTotalProfit = new JLabel(value); lblValue = lblTotalProfit; break;
            default: lblProfitMargin = new JLabel(value); lblValue = lblProfitMargin; break;
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
    
    private void loadData() {
        tableModel.setRowCount(0);
        
        if (dateFrom.getDate() == null || dateTo.getDate() == null) {
            return; // Jangan tampilkan error, cukup kosongkan
        }

        if (dateFrom.getDate().after(dateTo.getDate())) {
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

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Produk", "Merek", "Terjual", "Harga Jual", "HPP", 
                               "Pendapatan", "Laba"};
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
                        if (c >= 3) {
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