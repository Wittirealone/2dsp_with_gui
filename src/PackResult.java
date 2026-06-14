import java.util.List;

public class PackResult {
    public final String algorithmName;
    public final List<Rect> rects;
    public final int stripWidth;
    public final int totalHeight;
    public final double efficiency;   // gepackte Fläche / Streifenfläche * 100
    public final long timeMs;

    public PackResult(String name, List<Rect> rects, int W, long timeMs) {
        this.algorithmName = name;
        this.rects         = rects;
        this.stripWidth    = W;
        this.totalHeight   = rects.stream().mapToInt(r -> r.y + r.h).max().orElse(0);
        long usedArea      = rects.stream().mapToLong(r -> (long) r.w * r.h).sum();
        this.efficiency    = (totalHeight > 0 && W > 0)
            ? (100.0 * usedArea / ((long) W * totalHeight))
            : 0.0;
        this.timeMs        = timeMs;
    }
}
