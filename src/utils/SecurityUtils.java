package utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class untuk keamanan dan enkripsi password menggunakan bcrypt
 */
public class SecurityUtils {

    // cost / log rounds (10–12 disarankan)
    private static final int BCRYPT_COST = 12;

    /**
     * Hash password menggunakan bcrypt
     * @param password Password plain text
     * @return Password yang sudah di-hash
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Verifikasi password dengan hash bcrypt
     * @param password Password plain text
     * @param hashedPassword Password bcrypt dari database
     * @return true jika password cocok
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        return BCrypt.checkpw(password, hashedPassword);
    }
}
