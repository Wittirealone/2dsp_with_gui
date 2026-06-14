import java.util.*;

/**
 * First-Fit Decreasing Height (FFDH)
 *
 * Sortiert Rechtecke nach Höhe absteigend. Für jedes Rechteck wird das
 * erste vorhandene Regal gesucht, auf das es (von der Breite her) passt.
 * Passt es nirgends, wird ein neues Regal ganz oben eröffnet.
 * Besser als NFDH, da bereits geöffnete Regale wiederverwendet werden.
 */
public class FFDH {

    public static PackResult pack(List<Rect> input, int W) {
        long t0 = System.currentTimeMillis();

        List<Rect> sorted = new ArrayList<>(input);
        sorted.sort((a, b) -> b.h - a.h);

        // shelves[i] = { y-Position, Regalhöhe, bisher genutzte Breite }
        List<int[]> shelves = new ArrayList<>();
        List<Rect>  result  = new ArrayList<>();

        for (Rect r : sorted) {
            if (r.w > W) continue;
            boolean placed = false;

            for (int[] sh : shelves) {
                // sh[2] = usedWidth, sh[1] = shelfHeight
                if (sh[2] + r.w <= W && r.h <= sh[1]) {
                    result.add(r.placed(sh[2], sh[0]));
                    sh[2] += r.w;
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                int newY = shelves.stream().mapToInt(s -> s[0] + s[1]).max().orElse(0);
                shelves.add(new int[]{newY, r.h, r.w});
                result.add(r.placed(0, newY));
            }
        }

        return new PackResult("FFDH", result, W, System.currentTimeMillis() - t0);
    }
}
