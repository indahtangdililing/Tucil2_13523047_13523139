import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

class QuadtreeNode {
    int x, y, size;
    int[] avgColor;
    QuadtreeNode[] children;

    public QuadtreeNode(int x, int y, int size, int[] avgColor) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.avgColor = avgColor;
        this.children = new QuadtreeNode[4];
    }
}

public class Quadtree {

    public static int[] calculateAverageColor(int[][][] block) {
        int N = block.length * block[0].length;
        int[] sum = {0, 0, 0};

        for (int[][] row : block) {
            for (int[] pixel : row) {
                sum[0] += pixel[0];
                sum[1] += pixel[1];
                sum[2] += pixel[2];
            }
        }
        return new int[]{sum[0] / N, sum[1] / N, sum[2] / N};
    }

    public static double calculateCompressionPercentage(int originalSize, int compressedSize) {
        return (1.0 - ((double) compressedSize / originalSize)) * 100.0;
    }

    public static double[] calculateVariance(int[][][] block, int[] avgColor) {
        int N = block.length * block[0].length;
        double[] variance = {0.0, 0.0, 0.0};

        for (int[][] row : block) {
            for (int[] pixel : row) {
                for (int c = 0; c < 3; c++) {
                    variance[c] += Math.pow(pixel[c] - avgColor[c], 2);
                }
            }
        }
        variance[0] /= N;
        variance[1] /= N;
        variance[2] /= N;

        double varianceRGB = (variance[0] + variance[1] + variance[2]) / 3.0;
        return new double[]{variance[0], variance[1], variance[2], varianceRGB};
    }

    public static double[] calculateMAD(int[][][] block, int[] avgColor) {
        int N = block.length * block[0].length;
        double[] mad = {0.0, 0.0, 0.0};

        for (int[][] row : block) {
            for (int[] pixel : row) {
                for (int c = 0; c < 3; c++) {
                    mad[c] += Math.abs(pixel[c] - avgColor[c]);
                }
            }
        }
        return new double[]{mad[0] / N, mad[1] / N, mad[2] / N, (mad[0] + mad[1] + mad[2]) / 3.0};
    }

    public static double[] calculateMaxPixelDifference(int[][][] block) {
        int minRGB[] = {255, 255, 255};
        int maxRGB[] = {0, 0, 0};

        for (int[][] row : block) {
            for (int[] pixel : row) {
                for (int c = 0; c < 3; c++) {
                    minRGB[c] = Math.min(minRGB[c], pixel[c]);
                    maxRGB[c] = Math.max(maxRGB[c], pixel[c]);
                }
            }
        }
        return new double[]{
            maxRGB[0] - minRGB[0],
            maxRGB[1] - minRGB[1],
            maxRGB[2] - minRGB[2],
            (maxRGB[0] - minRGB[0] + maxRGB[1] - minRGB[1] + maxRGB[2] - minRGB[2]) / 3.0
        };
    }

    public static double[] calculateEntropy(int[][][] block) {
        double[] entropy = {0.0, 0.0, 0.0};
        int totalPixels = block.length * block[0].length;

        for (int c = 0; c < 3; c++) {
            int[] histogram = new int[256];
            for (int[][] row : block) {
                for (int[] pixel : row) {
                    histogram[pixel[c]]++;
                }
            }

            for (int i = 0; i < 256; i++) {
                if (histogram[i] > 0) {
                    double p = (double) histogram[i] / totalPixels;
                    entropy[c] -= p * (Math.log(p) / Math.log(2));
                }
            }
        }
        return new double[]{entropy[0], entropy[1], entropy[2], (entropy[0] + entropy[1] + entropy[2]) / 3.0};
    }

    public static double[] calculateSSIM(int[][][] block1, int[][][] block2) {
        double C1 = Math.pow(0.01 * 255, 2);
        double C2 = Math.pow(0.03 * 255, 2);
        double[] ssim = new double[3];
        int[] avg1 = calculateAverageColor(block1);
        int[] avg2 = calculateAverageColor(block2);
        double[] var1 = calculateVariance(block1, avg1);
        double[] var2 = calculateVariance(block2, avg2);
        double[] covar = new double[3];
        int N = block1.length * block1[0].length;

        for (int[][] row1 : block1) {
            for (int[] pixel1 : row1) {
                for (int c = 0; c < 3; c++) {
                    for (int[][] row2 : block2) {
                        for (int[] pixel2 : row2) {
                            covar[c] += (pixel1[c] - avg1[c]) * (pixel2[c] - avg2[c]);
                        }
                    }
                }
            }
        }
        for (int c = 0; c < 3; c++) {
            covar[c] /= N;
            ssim[c] = (2 * avg1[c] * avg2[c] + C1) * (2 * covar[c] + C2)
                    / ((Math.pow(avg1[c], 2) + Math.pow(avg2[c], 2) + C1) * (var1[c] + var2[c] + C2));
        }
        return new double[]{ssim[0], ssim[1], ssim[2], (ssim[0] + ssim[1] + ssim[2]) / 3.0};
    }

    public static boolean shouldSplit(int[][][] block, double threshold, int minSize, int method, int[][][] originalBlock) {
        if (block.length <= minSize || block[0].length <= minSize) {
            return false;
        }
        int[] avgColor = calculateAverageColor(block);
        switch (method) {
            case 1:
                return calculateVariance(block, avgColor)[3] > threshold;
            case 2:
                return calculateMAD(block, avgColor)[3] > threshold;
            case 3:
                return calculateMaxPixelDifference(block)[3] > threshold;
            case 4:
                return calculateEntropy(block)[3] > threshold;
            case 5: //SSIM
                return calculateSSIM(block, originalBlock)[3] < threshold; //SSIM: lower is worse
            default:
                return false;
        }
    }

    public static QuadtreeNode buildQuadtree(int[][][] image, int x, int y, int size, double threshold, int minSize, int method, int[][][] originalImage) {
        int[][][] block = extractBlock(image, x, y, size);
        int[][][] originalBlock = extractBlock(originalImage, x, y, size); //For SSIM
        int[] avgColor = calculateAverageColor(block);

        if (!shouldSplit(block, threshold, minSize, method, originalBlock)) {
            return new QuadtreeNode(x, y, size, avgColor);
        }

        QuadtreeNode node = new QuadtreeNode(x, y, size, avgColor);
        int newSize = size / 2;
        node.children[0] = buildQuadtree(image, x, y, newSize, threshold, minSize, method, originalImage);
        node.children[1] = buildQuadtree(image, x + newSize, y, newSize, threshold, minSize, method, originalImage);
        node.children[2] = buildQuadtree(image, x, y + newSize, newSize, threshold, minSize, method, originalImage);
        node.children[3] = buildQuadtree(image, x + newSize, y + newSize, newSize, threshold, minSize, method, originalImage);

        return node;
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

    public static int[][][] reconstructImage(QuadtreeNode node, int[][][] image) {
        int height = image.length;
        int width = image[0].length;

        for (int i = 0; i < node.size; i++) {
            for (int j = 0; j < node.size; j++) {
                // Pastikan koordinat piksel berada dalam batas gambar
                if (node.y + i < height && node.x + j < width) {
                    image[node.y + i][node.x + j] = node.avgColor;
                }
            }
        }
        if (node.children[0] != null) {
            reconstructImage(node.children[0], image);
            reconstructImage(node.children[1], image);
            reconstructImage(node.children[2], image);
            reconstructImage(node.children[3], image);
        }
        return image;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Masukkan alamat absolut gambar yang akan dikompresi: ");
        String inputImagePath = scanner.nextLine();

        System.out.println("Pilih metode perhitungan error (1-5):");
        System.out.println("1. Variance");
        System.out.println("2. Mean Absolute Deviation (MAD)");
        System.out.println("3. Max Pixel Difference");
        System.out.println("4. Entropy");
        System.out.println("5. Structural Similarity Index (SSIM) [Bonus]");
        System.out.print("Masukkan nomor metode: ");
        int method = scanner.nextInt();

        System.out.print("Masukkan ambang batas: ");
        double threshold = scanner.nextDouble();

        System.out.print("Masukkan ukuran blok minimum: ");
        int minSize = scanner.nextInt();

        System.out.print("Masukkan target persentase kompresi (0 untuk menonaktifkan): ");
        double targetCompression = scanner.nextDouble();

        scanner.nextLine(); // Consume newline

        System.out.print("Masukkan alamat absolut gambar hasil kompresi (termasuk nama file dan ekstensi): ");
        String outputImagePath = scanner.nextLine();

        System.out.print("Masukkan alamat absolut GIF (bonus, kosongkan jika tidak ada): ");
        String gifOutputPath = scanner.nextLine();

        try {
            File inputFile = new File(inputImagePath);
            if (!inputFile.exists()) {
                System.err.println("File gambar tidak ditemukan: " + inputImagePath);
                return;
            }

            BufferedImage originalImage = null;
            try {
                originalImage = ImageIO.read(inputFile);
            } catch (IOException e) {
                System.err.println("Gagal membaca file gambar (IOException): " + inputImagePath + " - " + e.getMessage());
                return;
            }

            if (originalImage == null) {
                System.err.println("Gagal membaca file gambar (Format tidak didukung atau rusak): " + inputImagePath);
                return;
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
                    pixelData[y][x][2] = rgb & 0xFF;       // B
                }
            }

            long startTime = System.nanoTime();

            QuadtreeNode root = buildQuadtree(pixelData, 0, 0, Math.max(width, height), threshold, minSize, method, pixelData);
            int[][][] compressedPixelData = new int[height][width][3];
            compressedPixelData = reconstructImage(root, compressedPixelData);

            long endTime = System.nanoTime();
            long executionTime = (endTime - startTime) / 1000000; //milliseconds

            // Convert 3D int array back to BufferedImage
            BufferedImage compressedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = (compressedPixelData[y][x][0] << 16) | (compressedPixelData[y][x][1] << 8) | compressedPixelData[y][x][2];
                    compressedImage.setRGB(x, y, rgb);
                }
            }

            ImageIO.write(compressedImage, "jpg", new File(outputImagePath)); // You can change "jpg" to other formats

            File originalFile = new File(inputImagePath);
            File compressedFile = new File(outputImagePath);
            int originalSize = (int) originalFile.length();
            int compressedSize = (int) compressedFile.length();
            double compressionPercentage = calculateCompressionPercentage(originalSize, compressedSize);

            System.out.println("Waktu eksekusi: " + executionTime + " ms");
            System.out.println("Ukuran gambar sebelum: " + originalSize + " bytes");
            System.out.println("Ukuran gambar setelah: " + compressedSize + " bytes");
            System.out.println("Persentase kompresi: " + String.format("%.2f", compressionPercentage) + " %");

            // Calculate tree depth and node count (Simplified - can be further optimized)
            int treeDepth = calculateTreeDepth(root);
            int nodeCount = calculateNodeCount(root);
            System.out.println("Kedalaman pohon: " + treeDepth);
            System.out.println("Banyak simpul pada pohon: " + nodeCount);

            // GIF generation (Basic - requires external library like AnimatedGifEncoder)
            if (!gifOutputPath.isEmpty()) {
                System.out.println("Pembuatan GIF belum diimplementasikan. Membutuhkan library eksternal.");
                // Implement GIF generation here (BONUS)
            }

        } catch (IOException e) {
            System.err.println("Terjadi kesalahan: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Terjadi kesalahan tak terduga: " + e.getMessage());
        }
    }

    // --- Tambahan method untuk menghitung kedalaman pohon dan jumlah node ---

    public static int calculateTreeDepth(QuadtreeNode node) {
        if (node == null) {
            return 0;
        }

        if (node.children[0] == null) {
            return 1;
        }

        int maxDepth = 0;
        for (QuadtreeNode child : node.children) {
            int depth = calculateTreeDepth(child);
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        return maxDepth + 1;
    }

    public static int calculateNodeCount(QuadtreeNode node) {
        if (node == null) {
            return 0;
        }

        int count = 1;
        if (node.children[0] != null) {
            for (QuadtreeNode child : node.children) {
                count += calculateNodeCount(child);
            }
        }
        return count;
    }
}