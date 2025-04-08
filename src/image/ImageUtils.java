package image;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageUtils {

    public static int[][][] loadImage(String imagePath) throws IOException {
        File inputFile = new File(imagePath);
        if (!inputFile.exists()) {
            throw new IOException("File not found: " + imagePath);
        }

        BufferedImage originalImage = ImageIO.read(inputFile);
        if (originalImage == null) {
            throw new IOException("Failed to read image (Unsupported format or corrupted file): " + imagePath);
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int[][][] pixelData = new int[height][width][3];

        // Convert BufferedImage to 3D int array
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);
                pixelData[y][x][0] = (rgb >> 16) & 0xFF; // R
                pixelData[y][x][1] = (rgb >> 8) & 0xFF;  // G
                pixelData[y][x][2] = rgb & 0xFF;         // B
            }
        }
        
        return pixelData;
    }
    public static int[][][] extractBlock(int[][][] image, int x, int y, int size) {
        int height = image.length;
        int width = image[0].length;

        // Pastikan blok tidak melebihi batas gambar
        int actualSize = Math.min(size, Math.min(width - x, height - y));

        int[][][] block = new int[actualSize][actualSize][3];
        for (int i = 0; i < actualSize; i++) {
            for (int j = 0; j < actualSize; j++) {
                block[i][j] = image[y + i][x + j];
            }
        }
        return block;
    }
    public static void saveImage(int[][][] pixelData, String outputPath, String format) throws IOException {
        int height = pixelData.length;
        int width = pixelData[0].length;
        
        BufferedImage compressedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = (pixelData[y][x][0] << 16) | (pixelData[y][x][1] << 8) | pixelData[y][x][2];
                compressedImage.setRGB(x, y, rgb);
            }
        }

        ImageIO.write(compressedImage, format, new File(outputPath));
    }
    
    public static double calculateCompressionPercentage(int originalSize, int compressedSize) {
        return (1.0 - ((double) compressedSize / originalSize)) * 100.0;
    }
}
