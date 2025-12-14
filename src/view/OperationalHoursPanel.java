package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import config.DatabaseConfig;

public class OperationalHoursPanel extends JPanel {
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cmbServiceType;
    
    public OperationalHoursPanel() {
        initComponents();
        loadData();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        
        JLabel lblTitle = new JLabel("Pengaturan Jam Operasional");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(lblTitle, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);
        
        filterPanel.add(new JLabel("Tipe Layanan:"));
        cmbServiceType = new JComboBox<>(new String[]{"store", "customer_service"});
        cmbServiceType.addActionListener(e -> loadData());
        filterPanel.add(cmbServiceType);
        
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadData());
        filterPanel.add(btnRefresh);
        
        // Info Label
        JLabel lblInfo = new JLabel("* Kosongkan jam untuk hari libur");
        lblInfo.setFont(new Font("Arial", Font.ITALIC, 11));
        lblInfo.setForeground(Color.GRAY);
        filterPanel.add(lblInfo);
        
        // Table
        String[] columns = {"ID", "Hari", "Jam Buka", "Jam Tutup", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        
        // Custom renderer for status
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, 
                    isSelected, hasFocus, row, column);
                String status = (String) value;
                if ("open".equals(status)) {
                    setForeground(new Color(46, 204, 113));
                    setText("BUKA");
                } else {
                    setForeground(new Color(231, 76, 60));
                    setText("TUTUP");
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Arial", Font.BOLD, 12));
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton btnEdit = new JButton("Edit Jam Operasional");
        btnEdit.setBackground(new Color(52, 152, 219));
        btnEdit.setForeground(Color.WHITE);
        btnEdit.setFocusPainted(false);
        btnEdit.setBorderPainted(false);
        btnEdit.setPreferredSize(new Dimension(180, 35));
        btnEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEdit.addActionListener(e -> showEditDialog());
        
        buttonPanel.add(btnEdit);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(filterPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
    }
    
    private void loadData() {
        tableModel.setRowCount(0);
        String serviceType = (String) cmbServiceType.getSelectedItem();
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id, day, open_time, close_time, status FROM operational_hours " +
                        "WHERE service_type = ? ORDER BY FIELD(day, 'monday', 'tuesday', " +
                        "'wednesday', 'thursday', 'friday', 'saturday', 'sunday')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, serviceType);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String day = rs.getString("day");
                Time openTime = rs.getTime("open_time");
                Time closeTime = rs.getTime("close_time");
                String status = rs.getString("status");
                
                Object[] row = {
                    rs.getInt("id"),
                    getDayInIndonesian(day),
                    openTime != null ? openTime.toString().substring(0, 5) : "-",
                    closeTime != null ? closeTime.toString().substring(0, 5) : "-",
                    status
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih hari yang akan diatur!");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0);
        String day = (String) table.getValueAt(row, 1);
        String openTime = (String) table.getValueAt(row, 2);
        String closeTime = (String) table.getValueAt(row, 3);
        String status = (String) table.getValueAt(row, 4);
        
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Edit Jam Operasional - " + day, true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"open", "closed"});
        cmbStatus.setSelectedItem("open".equals(status) || "BUKA".equals(status) ? "open" : "closed");
        
        // Time spinners
        SpinnerDateModel openModel = new SpinnerDateModel();
        JSpinner spinnerOpen = new JSpinner(openModel);
        JSpinner.DateEditor openEditor = new JSpinner.DateEditor(spinnerOpen, "HH:mm");
        spinnerOpen.setEditor(openEditor);
        
        SpinnerDateModel closeModel = new SpinnerDateModel();
        JSpinner spinnerClose = new JSpinner(closeModel);
        JSpinner.DateEditor closeEditor = new JSpinner.DateEditor(spinnerClose, "HH:mm");
        spinnerClose.setEditor(closeEditor);
        
        // Set initial values
        if (!"-".equals(openTime)) {
            try {
                java.util.Date date = new java.text.SimpleDateFormat("HH:mm").parse(openTime);
                spinnerOpen.setValue(date);
            } catch (Exception e) {}
        }
        
        if (!"-".equals(closeTime)) {
            try {
                java.util.Date date = new java.text.SimpleDateFormat("HH:mm").parse(closeTime);
                spinnerClose.setValue(date);
            } catch (Exception e) {}
        }
        
        // Enable/disable spinners based on status
        cmbStatus.addActionListener(e -> {
            boolean isOpen = "open".equals(cmbStatus.getSelectedItem());
            spinnerOpen.setEnabled(isOpen);
            spinnerClose.setEnabled(isOpen);
        });
        
        spinnerOpen.setEnabled("open".equals(cmbStatus.getSelectedItem()));
        spinnerClose.setEnabled("open".equals(cmbStatus.getSelectedItem()));
        
        int r = 0;
        gbc.gridx = 0; gbc.gridy = r;
        panel.add(new JLabel("Hari:"), gbc);
        gbc.gridx = 1;
        JLabel lblDay = new JLabel(day);
        lblDay.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(lblDay, gbc);
        
        r++;
        gbc.gridx = 0; gbc.gridy = r;
        panel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbStatus, gbc);
        
        r++;
        gbc.gridx = 0; gbc.gridy = r;
        panel.add(new JLabel("Jam Buka:"), gbc);
        gbc.gridx = 1;
        panel.add(spinnerOpen, gbc);
        
        r++;
        gbc.gridx = 0; gbc.gridy = r;
        panel.add(new JLabel("Jam Tutup:"), gbc);
        gbc.gridx = 1;
        panel.add(spinnerClose, gbc);
        
        r++;
        gbc.gridx = 0; gbc.gridy = r;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> {
            String newStatus = (String) cmbStatus.getSelectedItem();
            String newOpenTime = null;
            String newCloseTime = null;
            
            if ("open".equals(newStatus)) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
                newOpenTime = sdf.format(spinnerOpen.getValue());
                newCloseTime = sdf.format(spinnerClose.getValue());
            }
            
            updateOperationalHours(id, newOpenTime, newCloseTime, newStatus);
            dialog.dispose();
            loadData();
        });
        
        JButton btnCancel = new JButton("Batal");
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        panel.add(btnPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void updateOperationalHours(int id, String openTime, String closeTime, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE operational_hours SET open_time = ?, close_time = ?, status = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            
            if (openTime != null && closeTime != null) {
                ps.setTime(1, Time.valueOf(openTime));
                ps.setTime(2, Time.valueOf(closeTime));
            } else {
                ps.setNull(1, Types.TIME);
                ps.setNull(2, Types.TIME);
            }
            
            ps.setString(3, status);
            ps.setInt(4, id);
            
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Jam operasional berhasil diupdate!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private String getDayInIndonesian(String day) {
        switch (day.toLowerCase()) {
            case "monday": return "Senin";
            case "tuesday": return "Selasa";
            case "wednesday": return "Rabu";
            case "thursday": return "Kamis";
            case "friday": return "Jumat";
            case "saturday": return "Sabtu";
            case "sunday": return "Minggu";
            default: return day;
        }
    }
}