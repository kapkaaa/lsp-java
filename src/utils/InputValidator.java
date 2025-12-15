package utils;

/**
 * Utility class untuk validasi input
 */
public class InputValidator {
    
    /**
     * Validasi format email
     * @param email Email address
     * @return true jika valid
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    /**
     * Validasi nomor telepon
     * @param phone Nomor telepon
     * @return true jika valid (10-15 digit)
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String cleaned = phone.replaceAll("[^0-9]", "");
        return cleaned.length() >= 10 && cleaned.length() <= 15;
    }
    
    /**
     * Validasi NIK (16 digit)
     * @param nik Nomor Induk Kependudukan
     * @return true jika valid
     */
    public static boolean isValidNIK(String nik) {
        return nik != null && nik.matches("^[0-9]{16}$");
    }
    
    /**
     * Validasi harga (harus positif)
     * @param price Harga dalam string
     * @return true jika valid
     */
    public static boolean isValidPrice(String price) {
        try {
            double p = Double.parseDouble(price);
            return p > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validasi stok (harus >= 0)
     * @param stock Stok dalam string
     * @return true jika valid
     */
    public static boolean isValidStock(String stock) {
        try {
            int s = Integer.parseInt(stock);
            return s >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validasi text tidak kosong
     * @param text Text to validate
     * @return true jika tidak kosong
     */
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }
    
    /**
     * Validasi username (3-20 karakter, alphanumeric + underscore)
     * @param username Username
     * @return true jika valid
     */
    public static boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{3,20}$");
    }
    
    /**
     * Validasi password (minimal 6 karakter)
     * @param password Password
     * @return true jika valid
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}