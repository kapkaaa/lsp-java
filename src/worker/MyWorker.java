package worker;

import javax.swing.*;
import java.sql.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import config.DatabaseConfig;
import utils.SupabaseStorage;
import view.VariantDialog;

public class MyWorker extends SwingWorker<Boolean, Integer> {
    
    private final int productId;
    private final int colorId;
    private final List<String> sizeList;
    private final JTextField[] stockFields;
    private final List<File> selectedPhotos;
    private final VariantDialog variantDialog;
    private final JDialog parentDialog;
    private String errorMessage;
    
    public MyWorker(int productId, int colorId, List<String> sizeList, 
                            JTextField[] stockFields, List<File> selectedPhotos,
                            VariantDialog variantDialog, JDialog parentDialog) {
        this.productId = productId;
        this.colorId = colorId;
        this.sizeList = sizeList;
        this.stockFields = stockFields;
        this.selectedPhotos = selectedPhotos;
        this.variantDialog = variantDialog;
        this.parentDialog = parentDialog;
    }
    
    @Override
    protected Boolean doInBackground() throws Exception {
        boolean anySuccess = false;
        List<Integer> savedVariantIds = new ArrayList<>();
        
        // Hitung total langkah
        int totalSteps = sizeList.size() + (selectedPhotos.isEmpty() ? 0 : selectedPhotos.size());
        int currentStep = 0;
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Simpan varian per ukuran
            for (int i = 0; i < sizeList.size(); i++) {
                String sizeName = sizeList.get(i);
                int stock;
                
                try {
                    stock = Integer.parseInt(stockFields[i].getText().trim());
                } catch (NumberFormatException e) {
                    currentStep++;
                    publish((int) ((currentStep * 100.0) / totalSteps));
                    continue;
                }
                
                if (stock <= 0) {
                    currentStep++;
                    publish((int) ((currentStep * 100.0) / totalSteps));
                    continue;
                }
                
                int sizeId = getSizeIdOrCreate(conn, sizeName);
                if (sizeId == -1) {
                    currentStep++;
                    publish((int) ((currentStep * 100.0) / totalSteps));
                    continue;
                }
                
                // Cek duplikat
                if (isDuplicate(conn, productId, colorId, sizeId)) {
                    currentStep++;
                    publish((int) ((currentStep * 100.0) / totalSteps));
                    continue;
                }
                
                // Insert varian
                int variantId = insertVariant(conn, productId, colorId, sizeId, stock);
                if (variantId > 0) {
                    savedVariantIds.add(variantId);
                    anySuccess = true;
                }
                
                currentStep++;
                publish((int) ((currentStep * 100.0) / totalSteps));
            }
            
            // Upload foto untuk semua varian yang berhasil disimpan
            if (anySuccess && !selectedPhotos.isEmpty()) {
                currentStep = uploadPhotos(conn, savedVariantIds, currentStep, totalSteps);
            }
            
            if (!anySuccess) {
                errorMessage = "Tidak ada varian yang berhasil disimpan.";
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = "Gagal menyimpan: " + e.getMessage();
            return false;
        }
    }
    
    @Override
    protected void process(List<Integer> chunks) {
        // Method ini akan dipanggil di EDT oleh SwingWorker
        // LoadingDialog akan di-update di sini
    }
    
    @Override
    protected void done() {
        // Akan di-handle di AddVariantDialog
    }
    
    private boolean isDuplicate(Connection conn, int productId, int colorId, int sizeId) 
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM product_details WHERE product_id = ? AND color_id = ? AND size_id = ?")) {
            ps.setInt(1, productId);
            ps.setInt(2, colorId);
            ps.setInt(3, sizeId);
            return ps.executeQuery().next();
        }
    }
    
    private int insertVariant(Connection conn, int productId, int colorId, int sizeId, int stock) 
            throws SQLException {
        String barcode = VariantDialog.generateEAN13Barcode(conn);
        
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO product_details (product_id, color_id, size_id, stock, status, barcode, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, productId);
            ps.setInt(2, colorId);
            ps.setInt(3, sizeId);
            ps.setInt(4, stock);
            ps.setString(5, "available");
            ps.setString(6, barcode);
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }
    
    private int uploadPhotos(Connection conn, List<Integer> variantIds, int currentStep, int totalSteps) {
        int step = currentStep;
        
        for (Integer variantId : variantIds) {
            for (File photo : selectedPhotos) {
                try {
                    String photoUrl = SupabaseStorage.uploadProductPhoto(productId, photo);
                    if (photoUrl != null) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO product_photos (product_detail_id, photo_url) VALUES (?, ?)")) {
                            ps.setInt(1, variantId);
                            ps.setString(2, photoUrl);
                            ps.executeUpdate();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                step++;
                publish((int) ((step * 100.0) / totalSteps));
            }
        }
        
        return step;
    }
    
    private int getSizeIdOrCreate(Connection conn, String name) {
        try {
            try (PreparedStatement check = conn.prepareStatement("SELECT id FROM sizes WHERE name = ?")) {
                check.setString(1, name);
                ResultSet rs = check.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO sizes (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, name);
                ins.executeUpdate();
                ResultSet rs = ins.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}