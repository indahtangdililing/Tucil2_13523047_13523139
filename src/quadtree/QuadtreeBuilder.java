package quadtree;
import image.ImageUtils;
import compressor.Calculate;

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
            case 5: //SSIM
                return Calculate.calculateSSIM(block, originalBlock)[3] < threshold; //SSIM: lower is worse
            default:
                return false;
        }
    }

    public static QuadtreeNode buildQuadtree(int[][][] image, int x, int y, int size, double threshold, int minSize, int method, int[][][] originalImage) {
        int[][][] block = ImageUtils.extractBlock(image, x, y, size);
        int[][][] originalBlock = ImageUtils.extractBlock(originalImage, x, y, size); //For SSIM
        int[] avgColor = Calculate.calculateAverageColor(block);

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

    public static int[][][] reconstructImage(QuadtreeNode node, int[][][] image) {
        int height = image.length;
        int width = image[0].length;

        for (int i = 0; i < node.size; i++) {
            for (int j = 0; j < node.size; j++) {
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