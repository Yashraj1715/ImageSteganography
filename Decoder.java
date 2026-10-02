import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Decoder {

    /**
     * NEW: Extracts the hidden Base64 string via LSB (using decode() below),
     * then AES-decrypts it using the password to get the original plain message.
     * This is the method the GUI should call going forward.
     */
    public static String decodeWithDecryption(BufferedImage stegoImage, String password) throws Exception {
        String encryptedBase64 = decode(stegoImage);
        return AESUtil.decrypt(encryptedBase64, password);
    }

    /**
     * Extracts a hidden text message from a stego image.
     * Reads first 32 bits to get message length, then reads that many bytes.
     *
     * NOTE: Returns whatever String was hidden - if it was encrypted before
     * hiding, this returns the encrypted Base64 string, NOT the original
     * plain message. Use decodeWithDecryption() to get the real message back.
     */
    public static String decode(BufferedImage stegoImage) {
        int width = stegoImage.getWidth();
        int height = stegoImage.getHeight();

        // Step 1: extract first 32 bits to get message length
        int[] lengthBits = new int[32];
        int extractedCount = 0;

        int totalPixels = width * height;
        int[] rgbBitsBuffer = new int[totalPixels * 3]; // enough room for all possible bits
        int bufferIndex = 0;

        outerLoop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = stegoImage.getRGB(x, y);
                int red   = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue  = pixel & 0xFF;

                rgbBitsBuffer[bufferIndex++] = red & 1;
                rgbBitsBuffer[bufferIndex++] = green & 1;
                rgbBitsBuffer[bufferIndex++] = blue & 1;

                // Stop early once we've collected enough bits for header + message
                // (we don't know message length yet, so first just get 32 bits)
                if (bufferIndex >= 32 && extractedCount == 0) {
                    extractedCount = 1; // mark header as available, keep going anyway
                }
            }
        }

        // Rebuild message length from first 32 bits
        int messageLength = 0;
        for (int i = 0; i < 32; i++) {
            messageLength = (messageLength << 1) | rgbBitsBuffer[i];
        }

        // Sanity check to avoid garbage reads on a non-stego image
        long maxPossibleBytes = (rgbBitsBuffer.length - 32) / 8;
        if (messageLength < 0 || messageLength > maxPossibleBytes) {
            throw new IllegalStateException(
                    "No valid hidden message found (decoded length = " + messageLength + ").");
        }

        // Step 2: extract messageLength bytes, starting right after the 32-bit header
        byte[] messageBytes = new byte[messageLength];
        int bitPos = 32;

        for (int i = 0; i < messageLength; i++) {
            int value = 0;
            for (int b = 0; b < 8; b++) {
                value = (value << 1) | rgbBitsBuffer[bitPos++];
            }
            messageBytes[i] = (byte) value;
        }

        return new String(messageBytes);
    }

    public static void main(String[] args) throws Exception {
        BufferedImage stego = ImageIO.read(new File("stego.png"));
        String password = "pirate123"; // must match the password used during encoding

        // NEW: use decodeWithDecryption instead of plain decode()
        String hiddenMessage = decodeWithDecryption(stego, password);
        System.out.println("Decoded + decrypted message: " + hiddenMessage);
    }
}