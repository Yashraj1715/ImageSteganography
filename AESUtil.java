import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {

    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128; // authentication tag size
    private static final int GCM_IV_LENGTH_BYTES = 12;  // recommended IV size for GCM

    private static SecretKey deriveKeyFromPassword(String password) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(password.getBytes("UTF-8")); // always 32 bytes
        return new SecretKeySpec(keyBytes, "AES");
    }

  
    public static String encrypt(String plainText, String password) throws Exception {
        SecretKey key = deriveKeyFromPassword(password);

       
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] cipherTextBytes = cipher.doFinal(plainText.getBytes("UTF-8"));


        byte[] combined = new byte[iv.length + cipherTextBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherTextBytes, 0, combined, iv.length, cipherTextBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

  
    public static String decrypt(String encryptedBase64, String password) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

      
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


        try {
            decrypt(encrypted, "wrongPassword");
            System.out.println("FAIL: should have thrown exception for wrong password");
        } catch (Exception e) {
            System.out.println("PASS: wrong password correctly rejected -> " + e.getClass().getSimpleName());
        }
    }
}
