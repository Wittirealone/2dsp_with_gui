import java.util.*;

/**
 * Bottom-Left (BL)
 *
 * Sortiert Rechtecke nach einem wählbaren Kriterium (Fläche, Breite oder Höhe,
 * jeweils absteigend). Für jedes Rechteck werden Kandidatenpositionen aus den
 * Ecken bereits platzierten Rechtecke gebildet. Die unterste (dann linkeste)
 * überschneidungsfreie Position wird gewählt.
 *
 * Liefert oft kompaktere Packungen als Regal-Verfahren, ist aber O(n³).
 */
public class BottomLeft {

    public enum Sort { AREA, WIDTH, HEIGHT }

    public static PackResult pack(List<Rect> input, int W, Sort sort) {
        long t0 = System.currentTimeMillis();

        List<Rect> sorted = new ArrayList<>(input);
        switch (sort) {
            case WIDTH:  sorted.sort((a, b) -> b.w - a.w); break;
            case HEIGHT: sorted.sort((a, b) -> b.h - a.h); break;
            default:     sorted.sort((a, b) -> Integer.compare(b.w * b.h, a.w * a.h)); break;
        }

        String name;
        switch (sort) {
            case WIDTH:  name = "BL (Breite↓)";  break;
            case HEIGHT: name = "BL (Höhe↓)"; break;
            default:     name = "BL (Fläche↓)"; break;
        }

        List<Rect> placed = new ArrayList<>();

        for (Rect r : sorted) {
            if (r.w > W) continue;

            TreeSet<Integer> xs = new TreeSet<>();
            TreeSet<Integer> ys = new TreeSet<>();
            xs.add(0);
            ys.add(0);
            for (Rect p : placed) {
                xs.add(p.x + p.w);
                ys.add(p.y + p.h);
            }

            int bestX = 0, bestY = Integer.MAX_VALUE;

            for (int y : ys) {
                if (y > bestY) break;
                for (int x : xs) {
                    if (x + r.w > W) continue;
                    Rect cand = r.placed(x, y);
                    boolean ok = true;
                    for (Rect p : placed) {
                        if (cand.overlaps(p)) { ok = false; break; }
                    }
                    if (ok && (y < bestY || (y == bestY && x < bestX))) {
                        bestY = y;
                        bestX = x;
                    }
                }
            }

            if (bestY == Integer.MAX_VALUE) {
                bestY = placed.stream().mapToInt(p -> p.y + p.h).max().orElse(0);
                bestX = 0;
            }
            placed.add(r.placed(bestX, bestY));
        }

        return new PackResult(name, placed, W, System.currentTimeMillis() - t0);
    }
}
