import java.util.*;

/**
 * Next-Fit Decreasing Height (NFDH)
 *
 * Sortiert Rechtecke nach Höhe absteigend. Legt sie auf Regale
 * (horizontal levels). Passt ein Rechteck nicht mehr auf das aktuelle
 * Regal, wird ein neues direkt darüber geöffnet.
 */
public class NFDH {

    public static PackResult pack(List<Rect> input, int W) {
        long t0 = System.currentTimeMillis();

        List<Rect> sorted = new ArrayList<>(input);
        sorted.sort((a, b) -> b.h - a.h);

        List<Rect> result = new ArrayList<>();
        int shelfY = 0, shelfH = 0, shelfX = 0;

        for (Rect r : sorted) {
            if (r.w > W) continue;
            if (shelfX + r.w <= W) {
                result.add(r.placed(shelfX, shelfY));
                shelfX += r.w;
                shelfH = Math.max(shelfH, r.h);
            } else {
                // Neues Regal aufmachen
                shelfY += shelfH;
                shelfH  = r.h;
                result.add(r.placed(0, shelfY));
                shelfX  = r.w;
            }
        }

        return new PackResult("NFDH", result, W, System.currentTimeMillis() - t0);
    }
}
