import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {

    // AES-GCM standard settings
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128; // authentication tag size
    private static final int GCM_IV_LENGTH_BYTES = 12;  // recommended IV size for GCM

    /**
     * Converts a user's password (any length) into a fixed 256-bit AES key
     * using SHA-256 hashing. Same password always produces the same key.
     */
    private static SecretKey deriveKeyFromPassword(String password) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(password.getBytes("UTF-8")); // always 32 bytes
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts plain text using AES-GCM with a password-derived key.
     * Returns a Base64 string containing [IV + ciphertext] combined,
     * so the IV travels along with the encrypted data (needed for decryption).
     */
    public static String encrypt(String plainText, String password) throws Exception {
        SecretKey key = deriveKeyFromPassword(password);

        // Generate a random IV (Initialization Vector) - must be unique per encryption
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] cipherTextBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Combine IV + ciphertext into one array so we can decrypt later
        byte[] combined = new byte[iv.length + cipherTextBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherTextBytes, 0, combined, iv.length, cipherTextBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypts a Base64 string (produced by encrypt()) back to plain text.
     * Extracts the IV from the start of the combined data, then decrypts the rest.
     */
    public static String decrypt(String encryptedBase64, String password) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        // Extract IV (first 12 bytes) and actual ciphertext (rest)
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        byte[] cipherTextBytes = new byte[combined.length - GCM_IV_LENGTH_BYTES];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, cipherTextBytes, 0, cipherTextBytes.length);

        SecretKey key = deriveKeyFromPassword(password);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        byte[] plainTextBytes = cipher.doFinal(cipherTextBytes);
        return new String(plainTextBytes, "UTF-8");
    }

    public static void main(String[] args) throws Exception {
        String secret = "One Piece is the best anime, luffy pirate king!";
        String password = "pirate123";

        String encrypted = encrypt(secret, password);
        System.out.println("Encrypted (Base64): " + encrypted);

        String decrypted = decrypt(encrypted, password);
        System.out.println("Decrypted back: " + decrypted);

        System.out.println("Match: " + secret.equals(decrypted));

        // Test wrong password should fail
        try {
            decrypt(encrypted, "wrongPassword");
            System.out.println("FAIL: should have thrown exception for wrong password");
        } catch (Exception e) {
            System.out.println("PASS: wrong password correctly rejected -> " + e.getClass().getSimpleName());
        }
    }
}