import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Encoder {

    /**
     * NEW: Encrypts the message with AES (using the password) before hiding it.
     * This is the method you should call from the GUI going forward.
     * Internally: message -> AES encrypt -> Base64 string -> passed to the
     * original encode() method, which does the LSB hiding exactly as before.
     */
    public static BufferedImage encodeWithEncryption(BufferedImage coverImage, String message, String password) throws Exception {
        String encryptedBase64 = AESUtil.encrypt(message, password);
        return encode(coverImage, encryptedBase64);
    }

    /**
     * Hides a text message inside a cover image using LSB steganography.
     * First 32 bits store the message length (in bytes).
     * After that, each byte of the message is hidden bit by bit.
     *
     * NOTE: This method itself doesn't know or care whether "message" is
     * plain text or an AES-encrypted Base64 string - it just hides whatever
     * String it's given. That's what makes the encryption layer easy to add
     * on top without touching this logic at all.
     */
    public static BufferedImage encode(BufferedImage coverImage, String message) {
        BufferedImage stegoImage = copyImage(coverImage);

        byte[] messageBytes = message.getBytes();
        int messageLength = messageBytes.length;

        // Convert everything (length header + message) into one bit stream
        // Header = 32 bits for length, then 8 bits per character
        int totalBitsNeeded = 32 + (messageLength * 8);
        checkCapacity(stegoImage, totalBitsNeeded);

        // Build the full bit array: [32-bit length][message bits...]
        int[] bits = new int[totalBitsNeeded];
        int bitIndex = 0;

        // 1) Write length as 32 bits (MSB first)
        for (int i = 31; i >= 0; i--) {
            bits[bitIndex++] = (messageLength >> i) & 1;
        }

        // 2) Write message bytes as bits (MSB first, per byte)
        for (byte b : messageBytes) {
            for (int i = 7; i >= 0; i--) {
                bits[bitIndex++] = (b >> i) & 1;
            }
        }

        // 3) Embed bits into RGB channels, one bit per channel, skipping alpha
        int width = stegoImage.getWidth();
        int height = stegoImage.getHeight();
        int currentBit = 0;

        outerLoop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (currentBit >= totalBitsNeeded) break outerLoop;

                int pixel = stegoImage.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                int red   = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue  = pixel & 0xFF;

                // Modify Red channel LSB
                if (currentBit < totalBitsNeeded) {
                    red = (red & 0xFE) | bits[currentBit++];
                }
                // Modify Green channel LSB
                if (currentBit < totalBitsNeeded) {
                    green = (green & 0xFE) | bits[currentBit++];
                }
                // Modify Blue channel LSB
                if (currentBit < totalBitsNeeded) {
                    blue = (blue & 0xFE) | bits[currentBit++];
                }

                int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                stegoImage.setRGB(x, y, newPixel);
            }
        }

        return stegoImage;
    }

    /** Deep copy so we never mutate the original cover image in memory. */
    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(source, 0, 0, null);
        return copy;
    }

    /** Each pixel gives us 3 usable bits (R, G, B). Make sure image is big enough. */
    private static void checkCapacity(BufferedImage image, int bitsNeeded) {
        long capacityBits = (long) image.getWidth() * image.getHeight() * 3;
        if (bitsNeeded > capacityBits) {
            throw new IllegalArgumentException(
                    "Message too large for this image. Need " + bitsNeeded +
                            " bits but image only holds " + capacityBits + " bits.");
        }
    }

    public static void main(String[] args) throws Exception {
        // Example usage — replace with your own paths
        BufferedImage cover = ImageIO.read(new File("cover.png"));
        String secret = "One Piece is the best anime, luffy pirate king!";
        String password = "pirate123";

        // NEW: use encodeWithEncryption instead of plain encode()
        BufferedImage stego = encodeWithEncryption(cover, secret, password);
        ImageIO.write(stego, "png", new File("stego.png"));

        System.out.println("Message encrypted + encoded successfully into stego.png");
        System.out.println("Original message length: " + secret.getBytes().length + " bytes");
    }
}