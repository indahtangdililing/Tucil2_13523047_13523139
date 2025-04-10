package quadtree;
import image.ImageUtils;
import compressor.Calculate;
import java.awt.image.BufferedImage;

public class QuadtreeBuilder {
    public static boolean shouldSplit(int[][][] block, double threshold, int minSize, int method, int[][][] originalBlock) {
        if (block.length <= minSize || block[0].length <= minSize) {
            return false;
        }
        int[] avgColor = Calculate.calculateAverageColor(block);
        switch (method) {
            case 1:
                return Calculate.calculateVariance(block, avgColor)[3] > threshold;
            case 2:
                return Calculate.calculateMAD(block, avgColor)[3] > threshold;
            case 3:
                return Calculate.calculateMaxPixelDifference(block)[3] > threshold;
            case 4:
                return Calculate.calculateEntropy(block)[3] > threshold;
            case 5: 
                return Calculate.calculateSSIM(block, originalBlock)[3] < threshold;
            default:
                return false;
        }
    }

    public static QuadtreeNode buildQuadtree(int[][][] image, int x, int y, int width, int height, double threshold, int minSize, int method, int[][][] originalImage) {
        int[][][] block = ImageUtils.extractBlock(image, x, y, width, height);
        int[][][] originalBlock = ImageUtils.extractBlock(originalImage, x, y, width, height); 
        int[] avgColor = Calculate.calculateAverageColor(block);

        if (!shouldSplit(block, threshold, minSize, method, originalBlock)) {
            return new QuadtreeNode(x, y, width, height, avgColor);
        }

        QuadtreeNode node = new QuadtreeNode(x, y, width, height, avgColor);
        int halfWidth = width / 2;
        int halfHeight = height / 2;

        node.children[0] = buildQuadtree(image, x, y, halfWidth, halfHeight, threshold, minSize, method, originalImage);
        node.children[1] = buildQuadtree(image, x + halfWidth, y, width - halfWidth, halfHeight, threshold, minSize, method, originalImage);
        node.children[2] = buildQuadtree(image, x, y + halfHeight, halfWidth, height - halfHeight, threshold, minSize, method, originalImage);
        node.children[3] = buildQuadtree(image, x + halfWidth, y + halfHeight, width - halfWidth, height - halfHeight, threshold, minSize, method, originalImage);

        return node;
    }


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
    
    public static void reconstructImageByDepth(QuadtreeNode node, int[][][] image, int originalWidth, int originalHeight, int currentDepth) {
        if (node == null) {
            return;
        }
    
        if (currentDepth == 1 || node.children[0] == null) {
            for (int i = 0; i < node.height; i++) {
                for (int j = 0; j < node.width; j++) {
                    if (node.y + i < originalHeight && node.x + j < originalWidth) {
                        image[node.y + i][node.x + j] = node.avgColor;
                    }
                }
            }
            return; 
        }
    
        boolean hasValidChildren = false;
        for (QuadtreeNode child : node.children) {
            if (child != null) {
                hasValidChildren = true;
                reconstructImageByDepth(child, image, originalWidth, originalHeight, currentDepth - 1);
            }
        }
    
        if (!hasValidChildren) {
            for (int i = 0; i < node.height; i++) {
                for (int j = 0; j < node.width; j++) {
                    if (node.y + i < originalHeight && node.x + j < originalWidth) {
                        image[node.y + i][node.x + j] = node.avgColor;
                    }
                }
            }
        }
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

    public static BufferedImage convertToBufferedImage(int[][][] pixelData) {
        int height = pixelData.length;
        int width = pixelData[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = pixelData[y][x][0];
                int g = pixelData[y][x][1];
                int b = pixelData[y][x][2];
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }
    
        return image;
    }
}