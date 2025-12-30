package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.List;
import java.io.File;
import javax.imageio.ImageIO;
import config.DatabaseConfig;
import utils.SupabaseStorage;

public class PhotoDialog {
    private Point mousePoint;
    private JDialog dialog;
    private int variantId;
    private Component parent;
    private JPanel photoGridPanel;

    public PhotoDialog(Component parent, int variantId) {
        this.parent = parent;
        this.variantId = variantId;
        initDialog();
    }

    private void initDialog() {
        dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Kelola Foto Varian", true);
        dialog.setUndecorated(true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = createDialogTitleBar("Kelola Foto Varian");
        dialog.add(titleBar, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Info Panel
        JPanel infoPanel = createInfoPanel();

        // Photo Grid Panel
        photoGridPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        JScrollPane scrollPane = new JScrollPane(photoGridPanel);
        loadVariantPhotos();

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddPhoto = createStyledButton("Tambah Foto", new Color(46, 204, 113), e -> {
            addVariantPhotos();
        });
        JButton btnClose = createStyledButton("Tutup", Color.GRAY, e -> dialog.dispose());
        buttonPanel.add(btnAddPhoto);
        buttonPanel.add(btnClose);

        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel, BorderLayout.CENTER);

        addWindowDrag(titleBar);
        updateDialogShape();
    }

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(new Color(236, 240, 241));
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT p.name, c.name as color, s.name as size " +
                "FROM product_details pd " +
                "JOIN products p ON pd.product_id = p.id " +
                "JOIN colors c ON pd.color_id = c.id " +
                "JOIN sizes s ON pd.size_id = s.id " +
                "WHERE pd.id = ?")) {
            ps.setInt(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JLabel lblInfo = new JLabel(String.format("Produk: %s | Warna: %s | Size: %s",
                        rs.getString("name"), rs.getString("color"), rs.getString("size")));
                    lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    infoPanel.add(lblInfo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return infoPanel;
    }

    private void loadVariantPhotos() {
        photoGridPanel.removeAll();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, photo_url FROM product_photos WHERE product_detail_id = ? ORDER BY created_at")) {
            ps.setInt(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int photoId = rs.getInt("id");
                    String photoUrl = rs.getString("photo_url");
                    JPanel photoPanel = createPhotoPanel(photoId, photoUrl);
                    photoGridPanel.add(photoPanel);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        photoGridPanel.revalidate();
        photoGridPanel.repaint();
    }

    private JPanel createPhotoPanel(int photoId, String photoUrl) {
        JPanel photoPanel = new JPanel(new BorderLayout());
        photoPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JLabel lblPhoto = new JLabel("Loading...", SwingConstants.CENTER);
        lblPhoto.setPreferredSize(new Dimension(250, 250));
        lblPhoto.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        // Load image asynchronously
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                try {
                    BufferedImage img = ImageIO.read(new java.net.URL(photoUrl));
                    if (img != null) {
                        Image scaled = img.getScaledInstance(250, 250, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        lblPhoto.setIcon(icon);
                        lblPhoto.setText("");
                    } else {
                        lblPhoto.setText("Gagal muat");
                    }
                } catch (Exception ex) {
                    lblPhoto.setText("Error");
                }
            }
        }.execute();

        JButton btnDelete = createStyledButton("Hapus", new Color(231, 76, 60), ev -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                "Hapus foto ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                deleteVariantPhoto(photoId, photoUrl);
            }
        });
        
        photoPanel.add(lblPhoto, BorderLayout.CENTER);
        photoPanel.add(btnDelete, BorderLayout.SOUTH);
        return photoPanel;
    }

    private void addVariantPhotos() {
        JFileChooser fileChooser = new JFileChooser();
        
        String userHome = System.getProperty("user.home");
        File picturesFolder = new File(userHome, "Pictures");
        fileChooser.setCurrentDirectory(picturesFolder);
        
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif");
            }
            public String getDescription() {
                return "Image Files (*.jpg, *.jpeg, *.png, *.gif)";
            }
        });
        
        int result = fileChooser.showOpenDialog(dialog);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            
            JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Mengupload Foto...", true);
            JProgressBar progressBar = new JProgressBar(0, files.length);
            progressBar.setStringPainted(true);
            progressDialog.add(progressBar);
            progressDialog.setSize(400, 100);
            progressDialog.setLocationRelativeTo(dialog);

            new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try (Connection conn = DatabaseConfig.getConnection()) {
                        int uploaded = 0;
                        for (File file : files) {
                            String photoUrl = SupabaseStorage.uploadProductPhoto(variantId, file);
                            if (photoUrl != null) {
                                try (PreparedStatement ps = conn.prepareStatement(
                                        "INSERT INTO product_photos (product_detail_id, photo_url) VALUES (?, ?)")) {
                                    ps.setInt(1, variantId);
                                    ps.setString(2, photoUrl);
                                    ps.executeUpdate();
                                    uploaded++;
                                    publish(uploaded);
                                }
                            }
                        }
                    }
                    return null;
                }
                
                @Override
                protected void process(List<Integer> chunks) {
                    progressBar.setValue(chunks.get(chunks.size() - 1));
                }
                
                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        get();
                        JOptionPane.showMessageDialog(dialog, files.length + " foto berhasil diupload!");
                        loadVariantPhotos();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(dialog, "Error: " + e.getMessage());
                    }
                }
            }.execute();
            
            progressDialog.setVisible(true);
        }
    }

    private void deleteVariantPhoto(int photoId, String photoUrl) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM product_photos WHERE id = ?")) {
            SupabaseStorage.deleteProductPhoto(photoUrl);
            ps.setInt(1, photoId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(dialog, "Foto berhasil dihapus!");
            loadVariantPhotos();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "Error: " + e.getMessage());
        }
    }

    public void show() {
        dialog.setVisible(true);
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
        btn.setPreferredSize(new Dimension(130, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }

    private JPanel createDialogTitleBar(String title) {
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
        titleBar.setPreferredSize(new Dimension(600, 40));
        titleBar.setOpaque(false);

        JButton btnClose = createMacOSButton(new Color(0xFF5F57));
        btnClose.addActionListener(e -> dialog.dispose());

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.decode("#222222"));
        titleLabel.setOpaque(false);

        titleBar.add(btnClose);
        titleBar.add(Box.createHorizontalGlue());
        titleBar.add(titleLabel);
        titleBar.add(Box.createHorizontalGlue());
        return titleBar;
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
                dialog.setLocation(curr.x - mousePoint.x, curr.y - mousePoint.y);
            }
        });
    }

    private void updateDialogShape() {
        int arc = 20;
        Shape shape = new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), arc, arc);
        dialog.setShape(shape);
    }
}