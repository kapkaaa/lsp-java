package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
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
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(lblTitle, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);
        
        filterPanel.add(new JLabel("Tipe Layanan:"));
        
        cmbServiceType = new JComboBox<>(new String[]{"store", "customer_service"});
        cmbServiceType.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbServiceType.setBackground(Color.WHITE);
        cmbServiceType.setForeground(Color.decode("#222222"));
        cmbServiceType.addActionListener(e -> loadData());
        filterPanel.add(cmbServiceType);
        
        // Info Label
        JLabel lblInfo = new JLabel("* Kosongkan jam untuk hari libur");
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
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
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(30);
        table.setSelectionBackground(new Color(236, 240, 241));
        table.setSelectionForeground(Color.BLACK);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        
        // Style table header
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(236, 240, 241));
        header.setForeground(Color.BLACK);
        header.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
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
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton btnEdit = createStyledButton("Edit Jam Operasional", new Color(52, 152, 219), e -> showEditDialog());
        
        buttonPanel.add(btnEdit);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(filterPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
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
        btn.setPreferredSize(new Dimension(180, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
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
        dialog.setUndecorated(true);
        dialog.setSize(500, 260); // Lebar diperbesar untuk 2 kolom
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

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
        titleBar.setPreferredSize(new Dimension(500, 40));
        titleBar.setOpaque(false);

        JButton btnClose = createMacOSButton(new Color(0xFF5F57));
        btnClose.addActionListener(e -> dialog.dispose());

        JLabel titleLabel = new JLabel("Edit Jam Operasional", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.decode("#222222"));
        titleLabel.setOpaque(false);

        titleBar.add(btnClose);
        titleBar.add(Box.createHorizontalGlue());
        titleBar.add(titleLabel);
        titleBar.add(Box.createHorizontalGlue());

        dialog.add(titleBar, BorderLayout.NORTH);

        // =================== CONTENT PANEL ===================
        JPanel contentPanel = new JPanel(new BorderLayout()) {
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
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        contentPanel.setOpaque(false);

        // =================== FORM PANEL (2 KOLUMN) ===================
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Kolom Kiri
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.fill = GridBagConstraints.HORIZONTAL;
        gbcLeft.insets = new Insets(8, 8, 8, 8);

        // Hari (read-only)
        gbcLeft.gridx = 0; gbcLeft.gridy = 0;
        leftPanel.add(new JLabel("Hari:"), gbcLeft);
        gbcLeft.gridx = 1;
        JLabel lblDay = new JLabel(day);
        lblDay.setFont(new Font("Segoe UI", Font.BOLD, 14));
        leftPanel.add(lblDay, gbcLeft);

        // Status
        gbcLeft.gridx = 0; gbcLeft.gridy = 1;
        leftPanel.add(new JLabel("Status:"), gbcLeft);
        gbcLeft.gridx = 1;
        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"open", "closed"});
        cmbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbStatus.setBackground(Color.WHITE);
        cmbStatus.setForeground(Color.decode("#222222"));
        cmbStatus.setSelectedItem("open".equals(status) || "BUKA".equals(status) ? "open" : "closed");
        leftPanel.add(cmbStatus, gbcLeft);

        // Kolom Kanan
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        gbcRight.insets = new Insets(8, 8, 8, 8);

        // Jam Buka
        gbcRight.gridx = 0; gbcRight.gridy = 0;
        rightPanel.add(new JLabel("Jam Buka:"), gbcRight);
        gbcRight.gridx = 1;
        
        SpinnerDateModel openModel = new SpinnerDateModel();
        JSpinner spinnerOpen = new JSpinner(openModel);
        JSpinner.DateEditor openEditor = new JSpinner.DateEditor(spinnerOpen, "HH:mm");
        spinnerOpen.setEditor(openEditor);
        spinnerOpen.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinnerOpen.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.decode("#CCCCCC"), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        spinnerOpen.setBackground(Color.WHITE);
        rightPanel.add(spinnerOpen, gbcRight);

        // Jam Tutup
        gbcRight.gridx = 0; gbcRight.gridy = 1;
        rightPanel.add(new JLabel("Jam Tutup:"), gbcRight);
        gbcRight.gridx = 1;
        
        SpinnerDateModel closeModel = new SpinnerDateModel();
        JSpinner spinnerClose = new JSpinner(closeModel);
        JSpinner.DateEditor closeEditor = new JSpinner.DateEditor(spinnerClose, "HH:mm");
        spinnerClose.setEditor(closeEditor);
        spinnerClose.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinnerClose.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.decode("#CCCCCC"), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        spinnerClose.setBackground(Color.WHITE);
        rightPanel.add(spinnerClose, gbcRight);

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

        // Enable/disable based on status
        cmbStatus.addActionListener(e -> {
            boolean isOpen = "open".equals(cmbStatus.getSelectedItem());
            spinnerOpen.setEnabled(isOpen);
            spinnerClose.setEnabled(isOpen);
            if (!isOpen) {
                spinnerOpen.setValue(new java.util.Date());
                spinnerClose.setValue(new java.util.Date());
            }
        });
        
        spinnerOpen.setEnabled("open".equals(cmbStatus.getSelectedItem()));
        spinnerClose.setEnabled("open".equals(cmbStatus.getSelectedItem()));

        // Gabungkan kedua panel
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.5;
        formPanel.add(leftPanel, gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 0.5;
        formPanel.add(rightPanel, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnSave = createStyledButton("Simpan", new Color(46, 204, 113), e -> {
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

        JButton btnCancel = createStyledButton("Batal", Color.GRAY, e -> dialog.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        contentPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(contentPanel, BorderLayout.CENTER);

        // Drag window
        addWindowDrag(titleBar, dialog);
        updateDialogShape(dialog);

        dialog.setVisible(true);
    }
    
    // Helper: macOS button (bisa dipindah ke kelas util jika sering dipakai)
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

    private void addWindowDrag(Component comp, JDialog dialog) {
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mousePoint = e.getPoint();
            }
        });
        comp.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point curr = e.getLocationOnScreen();
                dialog.setLocation(curr.x - mousePoint.x, curr.y - mousePoint.y);
            }
        });
    }

    private void updateDialogShape(JDialog dialog) {
        int arc = 20;
        Shape shape = new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), arc, arc);
        dialog.setShape(shape);
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
    
    private Point mousePoint;
}