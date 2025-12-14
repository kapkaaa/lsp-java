package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.Locale;
import config.DatabaseConfig;

// ===========================
// SECURITY UTILS
// ===========================

public class SecurityUtils {
    
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    public static boolean verifyPassword(String password, String hashedPassword) {
        return hashPassword(password).equals(hashedPassword);
    }
}

// ===========================
// OPERATIONAL HOURS VALIDATOR
// ===========================

class OperationalHoursValidator {
    
    /**
     * Check if current time is within operational hours
     * @param serviceType "store" or "customer_service"
     * @return true if operational, false otherwise
     */
    public static boolean isOperationalHour(String serviceType) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            LocalDateTime now = LocalDateTime.now();
            String day = getDayInEnglish(now.getDayOfWeek()).toLowerCase();
            LocalTime currentTime = now.toLocalTime();
            
            String sql = "SELECT open_time, close_time, status FROM operational_hours " +
                        "WHERE service_type = ? AND day = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, serviceType);
            ps.setString(2, day);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                
                if ("closed".equals(status)) {
                    return false;
                }
                
                Time openTime = rs.getTime("open_time");
                Time closeTime = rs.getTime("close_time");
                
                if (openTime != null && closeTime != null) {
                    LocalTime open = openTime.toLocalTime();
                    LocalTime close = closeTime.toLocalTime();
                    
                    return !currentTime.isBefore(open) && !currentTime.isAfter(close);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking operational hours: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get operational hours message for display
     */
    public static String getOperationalMessage(String serviceType) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            LocalDateTime now = LocalDateTime.now();
            String day = getDayInEnglish(now.getDayOfWeek()).toLowerCase();
            
            String sql = "SELECT open_time, close_time, status FROM operational_hours " +
                        "WHERE service_type = ? AND day = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, serviceType);
            ps.setString(2, day);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                String displayDay = getDayInIndonesian(now.getDayOfWeek());
                
                if ("closed".equals(status)) {
                    return "Toko tutup pada hari " + displayDay;
                }
                
                Time openTime = rs.getTime("open_time");
                Time closeTime = rs.getTime("close_time");
                
                return String.format("Jam operasional hari %s: %s - %s", 
                    displayDay,
                    openTime != null ? openTime.toString().substring(0, 5) : "N/A",
                    closeTime != null ? closeTime.toString().substring(0, 5) : "N/A");
            }
        } catch (SQLException e) {
            System.err.println("Error getting operational message: " + e.getMessage());
        }
        return "Jam operasional tidak tersedia";
    }
    
    /**
     * Get full week operational hours
     */
    public static String getWeeklySchedule(String serviceType) {
        StringBuilder schedule = new StringBuilder();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT day, open_time, close_time, status FROM operational_hours " +
                        "WHERE service_type = ? ORDER BY FIELD(day, 'monday', 'tuesday', " +
                        "'wednesday', 'thursday', 'friday', 'saturday', 'sunday')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, serviceType);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String day = rs.getString("day");
                String status = rs.getString("status");
                Time openTime = rs.getTime("open_time");
                Time closeTime = rs.getTime("close_time");
                
                String dayIndo = getDayInIndonesian(getDayOfWeekFromString(day));
                schedule.append(String.format("%-10s: ", dayIndo));
                
                if ("closed".equals(status)) {
                    schedule.append("TUTUP");
                } else {
                    schedule.append(String.format("%s - %s",
                        openTime != null ? openTime.toString().substring(0, 5) : "N/A",
                        closeTime != null ? closeTime.toString().substring(0, 5) : "N/A"));
                }
                schedule.append("\n");
            }
        } catch (SQLException e) {
            System.err.println("Error getting weekly schedule: " + e.getMessage());
        }
        return schedule.toString();
    }
    
    private static String getDayInEnglish(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
    
    private static String getDayInIndonesian(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "Senin";
            case TUESDAY: return "Selasa";
            case WEDNESDAY: return "Rabu";
            case THURSDAY: return "Kamis";
            case FRIDAY: return "Jumat";
            case SATURDAY: return "Sabtu";
            case SUNDAY: return "Minggu";
            default: return "Unknown";
        }
    }
    
    private static DayOfWeek getDayOfWeekFromString(String day) {
        switch (day.toLowerCase()) {
            case "monday": return DayOfWeek.MONDAY;
            case "tuesday": return DayOfWeek.TUESDAY;
            case "wednesday": return DayOfWeek.WEDNESDAY;
            case "thursday": return DayOfWeek.THURSDAY;
            case "friday": return DayOfWeek.FRIDAY;
            case "saturday": return DayOfWeek.SATURDAY;
            case "sunday": return DayOfWeek.SUNDAY;
            default: return DayOfWeek.MONDAY;
        }
    }
}

// ===========================
// INPUT VALIDATOR
// ===========================

class InputValidator {
    
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String cleaned = phone.replaceAll("[^0-9]", "");
        return cleaned.length() >= 10 && cleaned.length() <= 15;
    }
    
    public static boolean isValidNIK(String nik) {
        return nik != null && nik.matches("^[0-9]{16}$");
    }
    
    public static boolean isValidPrice(String price) {
        try {
            double p = Double.parseDouble(price);
            return p > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidStock(String stock) {
        try {
            int s = Integer.parseInt(stock);
            return s >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }
    
    public static boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{3,20}$");
    }
    
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}

// ===========================
// FORMATTER UTILS
// ===========================

class FormatterUtils {
    
    public static String formatCurrency(double amount) {
        return String.format("Rp %,.0f", amount);
    }
    
    public static String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "-";
        LocalDateTime ldt = timestamp.toLocalDateTime();
        return String.format("%02d/%02d/%d %02d:%02d",
            ldt.getDayOfMonth(),
            ldt.getMonthValue(),
            ldt.getYear(),
            ldt.getHour(),
            ldt.getMinute());
    }
    
    public static String formatDateOnly(Timestamp timestamp) {
        if (timestamp == null) return "-";
        LocalDateTime ldt = timestamp.toLocalDateTime();
        return String.format("%02d/%02d/%d",
            ldt.getDayOfMonth(),
            ldt.getMonthValue(),
            ldt.getYear());
    }
    
    public static String generateTransactionCode() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("TRX%04d%02d%02d%02d%02d%02d",
            now.getYear(),
            now.getMonthValue(),
            now.getDayOfMonth(),
            now.getHour(),
            now.getMinute(),
            now.getSecond());
    }
    
    public static String generateOrderCode() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("ORD%04d%02d%02d%02d%02d%02d",
            now.getYear(),
            now.getMonthValue(),
            now.getDayOfMonth(),
            now.getHour(),
            now.getMinute(),
            now.getSecond());
    }
}

// ===========================
// SESSION MANAGER
// ===========================

class SessionManager {
    private static model.User currentUser;
    
    public static void setCurrentUser(model.User user) {
        currentUser = user;
    }
    
    public static model.User getCurrentUser() {
        return currentUser;
    }
    
    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : 0;
    }
    
    public static String getCurrentUserName() {
        return currentUser != null ? currentUser.getName() : "";
    }
    
    public static String getCurrentUserRole() {
        return currentUser != null && currentUser.getRole() != null 
            ? currentUser.getRole().getName() : "";
    }
    
    public static boolean isAdmin() {
        return "admin".equals(getCurrentUserRole());
    }
    
    public static boolean isCashier() {
        return "cashier".equals(getCurrentUserRole());
    }
    
    public static boolean isCustomer() {
        return "customer".equals(getCurrentUserRole());
    }
    
    public static void clearSession() {
        currentUser = null;
    }
    
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}