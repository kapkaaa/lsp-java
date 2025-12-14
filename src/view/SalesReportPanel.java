package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import com.toedter.calendar.JDateChooser;
import config.DatabaseConfig;
import utils.FormatterUtils;

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
        
        // Load data today by default
        dateTo.setDate(new java.util.Date());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
        dateFrom.setDate(cal.getTime());
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
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT t.transaction_code, t.created_at, u.name as cashier, " +
                "t.total, t.payment_method, " +
                "SUM(td.quantity * p.cost_price) as hpp, " +
                "(t.total - SUM(td.quantity * p.cost_price)) as profit " +
                "FROM transactions t " +
                "JOIN users u ON t.user_id = u.id " +
                "JOIN transaction_details td ON t.transaction_id = td.transaction_id " +
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
        JOptionPane.showMessageDialog(this, "Fitur export akan menggunakan Apache POI library");
    }
}

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
        
        // Load data for this month
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
}

// Currency Renderer
class CurrencyRenderer extends DefaultTableCellRenderer {
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