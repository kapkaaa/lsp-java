package utils;

import model.User;

/**
 * Class untuk manajemen session user yang sedang login
 */
public class SessionManager {
    private static User currentUser;
    
    /**
     * Set user yang sedang login
     * @param user User object
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    /**
     * Get user yang sedang login
     * @return User object atau null jika belum login
     */
    public static User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Get ID user yang sedang login
     * @return User ID atau 0 jika belum login
     */
    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : 0;
    }
    
    /**
     * Get nama user yang sedang login
     * @return Nama user atau empty string jika belum login
     */
    public static String getCurrentUserName() {
        return currentUser != null ? currentUser.getName() : "";
    }
    
    /**
     * Get role user yang sedang login
     * @return Role name atau empty string jika belum login
     */
    public static String getCurrentUserRole() {
        return currentUser != null && currentUser.getRole() != null 
            ? currentUser.getRole().getName() : "";
    }
    
    /**
     * Check apakah user yang login adalah admin
     * @return true jika admin
     */
    public static boolean isAdmin() {
        return "admin".equals(getCurrentUserRole());
    }
    
    /**
     * Check apakah user yang login adalah kasir
     * @return true jika kasir
     */
    public static boolean isCashier() {
        return "cashier".equals(getCurrentUserRole());
    }
    
    /**
     * Check apakah user yang login adalah customer
     * @return true jika customer
     */
    public static boolean isCustomer() {
        return "customer".equals(getCurrentUserRole());
    }
    
    /**
     * Hapus session (logout)
     */
    public static void clearSession() {
        currentUser = null;
    }
    
    /**
     * Check apakah ada user yang sedang login
     * @return true jika ada user login
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}