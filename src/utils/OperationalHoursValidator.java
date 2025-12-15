package utils;

import java.sql.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.Locale;
import config.DatabaseConfig;

/**
 * Utility class untuk validasi jam operasional
 */
public class OperationalHoursValidator {
    
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
     * @param serviceType "store" or "customer_service"
     * @return Message string
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
     * @param serviceType "store" or "customer_service"
     * @return Weekly schedule string
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