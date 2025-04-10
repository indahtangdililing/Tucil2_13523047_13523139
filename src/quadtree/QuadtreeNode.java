package quadtree;

public class QuadtreeNode {
    public int x, y;
    public int width, height;
    public int[] avgColor;
    public QuadtreeNode[] children;

    public QuadtreeNode(int x, int y, int width, int height, int[] avgColor) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.avgColor = avgColor;
        this.children = new QuadtreeNode[4];
    }
}