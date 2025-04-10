package compressor;

public class Calculate{

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

}