import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;

import image.ImageUtils;
import quadtree.QuadtreeBuilder;
import quadtree.QuadtreeNode;
import image.GifSequenceWriter;



public class Main {
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
            try {
                BufferedImage testImage = ImageIO.read(new File(inputImagePath));
                System.out.println("Gambar berhasil dibaca menggunakan program Java sederhana.");
            } catch (IOException e) {
                System.err.println("Gagal membaca gambar menggunakan program Java sederhana: " + e.getMessage());
            }
            
            int[][][] pixelData = ImageUtils.loadImage(inputImagePath);
            int height = pixelData.length;
            int width = pixelData[0].length;

            long startTime = System.nanoTime();
            long executionTime;

            if (pixelData.length > 0 && pixelData[0].length > 0) {
                QuadtreeNode root = QuadtreeBuilder.buildQuadtree(pixelData, 0, 0, width, height, threshold, minSize, method, pixelData);
                int[][][] compressedPixelData = new int[height][width][3];
                QuadtreeBuilder.reconstructImageByDepth(root, compressedPixelData, width, height, QuadtreeBuilder.calculateTreeDepth(root));

                ImageUtils.saveImage(compressedPixelData, outputImagePath, "jpg"); 
                File originalFile = new File(inputImagePath);
                File compressedFile = new File(outputImagePath);
                int originalSize = (int) originalFile.length();
                int compressedSize = (int) compressedFile.length();
                double compressionPercentage = ImageUtils.calculateCompressionPercentage(originalSize, compressedSize);

                executionTime = (System.nanoTime() - startTime) / 1000000; //milliseconds

                System.out.println("Waktu eksekusi: " + executionTime + " ms");
                System.out.println("Ukuran gambar sebelum: " + originalSize + " bytes");
                System.out.println("Ukuran gambar setelah: " + compressedSize + " bytes");
                System.out.println("Persentase kompresi: " + String.format("%.2f", compressionPercentage) + " %");

                int treeDepth = QuadtreeBuilder.calculateTreeDepth(root);
                int nodeCount = QuadtreeBuilder.calculateNodeCount(root);
                System.out.println("Kedalaman pohon: " + treeDepth);
                System.out.println("Banyak simpul pada pohon: " + nodeCount);

                if (!gifOutputPath.isEmpty()) {
                    List<BufferedImage> gifImages = new ArrayList<>();

                    for (int depth = 1; depth <= treeDepth; depth++) {
                        // Rekonstruksi gambar berdasarkan kedalaman tertentu
                        int[][][] depthCompressedPixelData = new int[height][width][3];
                        QuadtreeBuilder.reconstructImageByDepth(root, depthCompressedPixelData, width, height, depth);
            
                        // Konversi data piksel ke BufferedImage
                        BufferedImage frame = QuadtreeBuilder.convertToBufferedImage(depthCompressedPixelData);
                        gifImages.add(frame);
                    }

                    GifSequenceWriter gifWriter = new GifSequenceWriter(
                        new FileImageOutputStream(new File(gifOutputPath)),
                        BufferedImage.TYPE_INT_RGB,
                        500, // Delay antara frame dalam milidetik
                        true // Loop terus-menerus
                    );

                    for (BufferedImage frame : gifImages) {
                        gifWriter.writeToSequence(frame);
                    }
                    gifWriter.close();
                    System.out.println("GIF berhasil dibuat: " + gifOutputPath);
                } else {
                    System.out.println("GIF tidak dibuat.");
                }
            } else {
                System.err.println("Gambar kosong atau tidak valid.");
            }


        } catch (IOException e) {
            System.err.println("Terjadi kesalahan: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Terjadi kesalahan tak terduga: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}