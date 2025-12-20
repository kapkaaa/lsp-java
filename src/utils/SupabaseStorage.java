package utils;

import config.SupabaseConfig;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;

public class SupabaseStorage {
    
    /**
     * Upload foto produk ke Supabase Storage
     * @param productId ID produk
     * @param file File foto yang akan diupload
     * @return URL foto di Supabase Storage, atau null jika gagal
     */
    public static String uploadProductPhoto(int productId, File file) {
        HttpsURLConnection conn = null;
        try {
            // Validate file first
            if (!SupabaseConfig.isValidFile(file)) {
                System.err.println("File validation failed");
                return null;
            }
            
            // Generate unique filename
            String extension = getFileExtension(file.getName());
            String fileName = "product_" + productId + "_" + UUID.randomUUID().toString() + extension;
            String filePath = "products/" + productId + "/" + fileName;
            
            // Read file bytes
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            // Upload to Supabase
            String uploadUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/" + 
                              SupabaseConfig.BUCKET_NAME + "/" + filePath;
            
            URL url = new URL(uploadUrl);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            
            // PENTING: Set headers yang benar
            conn.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_SERVICE_KEY);
            conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_SERVICE_KEY);
            conn.setRequestProperty("Content-Type", getMimeType(extension));
            conn.setRequestProperty("x-upsert", "false"); // false = tidak overwrite jika file sudah ada
            conn.setDoOutput(true);
            conn.setDoInput(true);
            
            // Write file content
            try (OutputStream os = conn.getOutputStream()) {
                os.write(fileContent);
                os.flush();
            }
            
            int responseCode = conn.getResponseCode();
            System.out.println("Upload response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                // Read response
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Upload response: " + response.toString());
                
                // Return public URL
                return SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/" + 
                       SupabaseConfig.BUCKET_NAME + "/" + filePath;
            } else {
                // Print error details
                System.err.println("Upload failed. Response code: " + responseCode);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.err.println("Error response: " + errorResponse.toString());
                } catch (Exception e) {
                    System.err.println("Could not read error stream");
                }
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Exception during upload: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * Delete foto produk dari Supabase Storage
     * @param photoUrl URL foto yang akan dihapus
     * @return true jika berhasil, false jika gagal
     */
    public static boolean deleteProductPhoto(String photoUrl) {
        HttpsURLConnection conn = null;
        try {
            // Extract file path from URL
            String prefix = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/" + 
                           SupabaseConfig.BUCKET_NAME + "/";
            if (!photoUrl.startsWith(prefix)) {
                System.err.println("Invalid photo URL format: " + photoUrl);
                return false;
            }
            
            String filePath = photoUrl.substring(prefix.length());
            
            // Delete from Supabase
            String deleteUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/" + 
                              SupabaseConfig.BUCKET_NAME + "/" + filePath;
            
            URL url = new URL(deleteUrl);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_SERVICE_KEY);
            conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_SERVICE_KEY);
            
            int responseCode = conn.getResponseCode();
            System.out.println("Delete response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                return true;
            } else {
                System.err.println("Delete failed. Response code: " + responseCode);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.err.println("Error response: " + errorResponse.toString());
                } catch (Exception e) {
                    System.err.println("Could not read error stream");
                }
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Exception during delete: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * Get file extension from filename
     */
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot).toLowerCase();
        }
        return ".jpg"; // default
    }
    
    /**
     * Get MIME type from extension
     */
    private static String getMimeType(String extension) {
        switch (extension.toLowerCase()) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            case ".bmp":
                return "image/bmp";
            default:
                return "image/jpeg"; // default to JPEG
        }
    }
    
    /**
     * Test connection to Supabase
     */
    public static boolean testConnection() {
        HttpsURLConnection conn = null;
        try {
            String testUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/bucket/" + SupabaseConfig.BUCKET_NAME;
            
            URL url = new URL(testUrl);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_SERVICE_KEY);
            conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_SERVICE_KEY);
            
            int responseCode = conn.getResponseCode();
            System.out.println("Test connection response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Bucket info: " + response.toString());
                return true;
            } else {
                System.err.println("Test failed. Response code: " + responseCode);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.err.println("Error response: " + errorResponse.toString());
                } catch (Exception e) {
                    System.err.println("Could not read error stream");
                }
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Exception during test: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}