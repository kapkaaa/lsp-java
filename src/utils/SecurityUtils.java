package utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * SecurityUtils
 * ----------------------------
 * Bcrypt utility (Java ↔ Laravel compatible)
 *
 * - Output bcrypt format: $2y$
 * - Compatible with Laravel Hash::check()
 * - Cost 12 (recommended)
 */
public class SecurityUtils {

    // bcrypt cost / log rounds
    private static final int BCRYPT_COST = 12;

    /**
     * Hash password menggunakan bcrypt
     * (Laravel compatible: $2y$)
     *
     * @param password plain text password
     * @return bcrypt hash ($2y$)
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password tidak boleh kosong");
        }

        // Generate bcrypt hash ($2a$ by default)
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));

        // Convert $2a$ -> $2y$ for Laravel compatibility
        if (hash.startsWith("$2a$")) {
            hash = "$2y$" + hash.substring(4);
        }

        return hash;
    }

    /**
     * Verify password against bcrypt hash
     * (supports $2a$ and $2y$)
     *
     * @param password plain text password
     * @param hashedPassword bcrypt hash
     * @return true if password matches
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }

        // jBCrypt expects $2a$, so convert back if needed
        String normalizedHash = hashedPassword;
        if (hashedPassword.startsWith("$2y$")) {
            normalizedHash = "$2a$" + hashedPassword.substring(4);
        }

        return BCrypt.checkpw(password, normalizedHash);
    }

    /**
     * Utility: check apakah hash bcrypt valid
     */
    public static boolean isBcryptHash(String hash) {
        if (hash == null) return false;
        return hash.startsWith("$2a$") || hash.startsWith("$2y$");
    }
}
