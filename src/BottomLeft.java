import java.util.*;

/**
 * Bottom-Left (BL)
 *
 * Sortiert Rechtecke nach Fläche absteigend. Für jedes Rechteck werden
 * Kandidatenpositionen aus den Ecken bereits platzierten Rechtecke gebildet
 * (x=0 + rechte Kanten; y=0 + obere Kanten). Die unterste (dann linkeste)
 * überschneidungsfreie Position wird gewählt.
 *
 * Liefert oft kompaktere Packungen als Regal-Verfahren, ist aber O(n³).
 */
public class BottomLeft {

    public static PackResult pack(List<Rect> input, int W) {
        long t0 = System.currentTimeMillis();

        List<Rect> sorted = new ArrayList<>(input);
        sorted.sort((a, b) -> Integer.compare(b.w * b.h, a.w * a.h));

        List<Rect> placed = new ArrayList<>();

        for (Rect r : sorted) {
            if (r.w > W) continue;

            // Kandidaten-Koordinaten aus Eckpunkten der platzierten Rechtecke
            TreeSet<Integer> xs = new TreeSet<>();
            TreeSet<Integer> ys = new TreeSet<>();
            xs.add(0);
            ys.add(0);
            for (Rect p : placed) {
                xs.add(p.x + p.w);
                ys.add(p.y + p.h);
            }

            int bestX = 0, bestY = Integer.MAX_VALUE;

            outer:
            for (int y : ys) {
                if (y > bestY) break; // TreeSet ist sortiert → kein besseres y mehr möglich
                for (int x : xs) {
                    if (x + r.w > W) continue;

                    Rect cand = r.placed(x, y);
                    boolean ok = true;
                    for (Rect p : placed) {
                        if (cand.overlaps(p)) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok && (y < bestY || (y == bestY && x < bestX))) {
                        bestY = y;
                        bestX = x;
                    }
                }
            }

            if (bestY == Integer.MAX_VALUE) {
                // Fallback: ganz oben links ablegen
                bestY = placed.stream().mapToInt(p -> p.y + p.h).max().orElse(0);
                bestX = 0;
            }
            placed.add(r.placed(bestX, bestY));
        }

        return new PackResult("Bottom-Left", placed, W, System.currentTimeMillis() - t0);
    }
}
