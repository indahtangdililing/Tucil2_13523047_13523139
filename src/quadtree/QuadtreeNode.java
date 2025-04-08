package quadtree;


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