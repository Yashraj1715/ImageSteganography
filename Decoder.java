import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Decoder {


    public static String decodeWithDecryption(BufferedImage stegoImage, String password) throws Exception {
        String encryptedBase64 = decode(stegoImage);
        return AESUtil.decrypt(encryptedBase64, password);
    }

 
    public static String decode(BufferedImage stegoImage) {
        int width = stegoImage.getWidth();
        int height = stegoImage.getHeight();

      
        int[] lengthBits = new int[32];
        int extractedCount = 0;

        int totalPixels = width * height;
        int[] rgbBitsBuffer = new int[totalPixels * 3]; 
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

                if (bufferIndex >= 32 && extractedCount == 0) {
                    extractedCount = 1; 
                }
            }
        }


        int messageLength = 0;
        for (int i = 0; i < 32; i++) {
            messageLength = (messageLength << 1) | rgbBitsBuffer[i];
        }

        long maxPossibleBytes = (rgbBitsBuffer.length - 32) / 8;
        if (messageLength < 0 || messageLength > maxPossibleBytes) {
            throw new IllegalStateException(
                    "No valid hidden message found (decoded length = " + messageLength + ").");
        }


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
        String password = "pirate123";


        String hiddenMessage = decodeWithDecryption(stego, password);
        System.out.println("Decoded + decrypted message: " + hiddenMessage);
    }
}
