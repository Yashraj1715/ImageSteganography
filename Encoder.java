import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Encoder {


    public static BufferedImage encodeWithEncryption(BufferedImage coverImage, String message, String password) throws Exception {
        String encryptedBase64 = AESUtil.encrypt(message, password);
        return encode(coverImage, encryptedBase64);
    }

    public static BufferedImage encode(BufferedImage coverImage, String message) {
        BufferedImage stegoImage = copyImage(coverImage);

        byte[] messageBytes = message.getBytes();
        int messageLength = messageBytes.length;

        int totalBitsNeeded = 32 + (messageLength * 8);
        checkCapacity(stegoImage, totalBitsNeeded);


        int[] bits = new int[totalBitsNeeded];
        int bitIndex = 0;

        for (int i = 31; i >= 0; i--) {
            bits[bitIndex++] = (messageLength >> i) & 1;
        }


        for (byte b : messageBytes) {
            for (int i = 7; i >= 0; i--) {
                bits[bitIndex++] = (b >> i) & 1;
            }
        }

        int width = stegoImage.getWidth();
        int height = stegoImage.getHeight();
        int currentBit = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (currentBit >= totalBitsNeeded) break outerLoop;

                int pixel = stegoImage.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                int red   = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue  = pixel & 0xFF;

                if (currentBit < totalBitsNeeded) {
                    red = (red & 0xFE) | bits[currentBit++];
                }

                if (currentBit < totalBitsNeeded) {
                    green = (green & 0xFE) | bits[currentBit++];
                }

                if (currentBit < totalBitsNeeded) {
                    blue = (blue & 0xFE) | bits[currentBit++];
                }

                int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                stegoImage.setRGB(x, y, newPixel);
            }
        }

        return stegoImage;
    }


    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(source, 0, 0, null);
        return copy;
    }

    private static void checkCapacity(BufferedImage image, int bitsNeeded) {
        long capacityBits = (long) image.getWidth() * image.getHeight() * 3;
        if (bitsNeeded > capacityBits) {
            throw new IllegalArgumentException(
                    "Message too large for this image. Need " + bitsNeeded +
                            " bits but image only holds " + capacityBits + " bits.");
        }
    }

    public static void main(String[] args) throws Exception {
 
        BufferedImage cover = ImageIO.read(new File("cover.png"));
        String secret = "One Piece is the best anime, luffy pirate king!";
        String password = "pirate123";

    
        BufferedImage stego = encodeWithEncryption(cover, secret, password);
        ImageIO.write(stego, "png", new File("stego.png"));

        System.out.println("Message encrypted + encoded successfully into stego.png");
        System.out.println("Original message length: " + secret.getBytes().length + " bytes");
    }
}
