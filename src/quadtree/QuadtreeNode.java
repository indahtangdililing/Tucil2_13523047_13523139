package quadtree;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;


public class QuadtreeNode {
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