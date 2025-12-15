package utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Utility class untuk formatting data (currency, date, dll)
 */
public class FormatterUtils {
    
    /**
     * Format angka menjadi format Rupiah
     * @param amount Jumlah uang
     * @return String format Rupiah
     */
    public static String formatCurrency(double amount) {
        return String.format("Rp %,.0f", amount);
    }
    
    /**
     * Format timestamp menjadi string tanggal + waktu
     * @param timestamp Timestamp
     * @return String format: dd/MM/yyyy HH:mm
     */
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
    
    /**
     * Format timestamp menjadi string tanggal saja (tanpa waktu)
     * @param timestamp Timestamp
     * @return String format: dd/MM/yyyy
     */
    public static String formatDateOnly(Timestamp timestamp) {
        if (timestamp == null) return "-";
        LocalDateTime ldt = timestamp.toLocalDateTime();
        return String.format("%02d/%02d/%d",
            ldt.getDayOfMonth(),
            ldt.getMonthValue(),
            ldt.getYear());
    }
    
    /**
     * Generate kode transaksi unik berdasarkan waktu
     * Format: TRXYYYYMMDDHHmmss
     * @return Kode transaksi
     */
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
    
    /**
     * Generate kode order unik berdasarkan waktu
     * Format: ORDYYYYMMDDHHmmss
     * @return Kode order
     */
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