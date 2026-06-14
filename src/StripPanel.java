import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

/**
 * Zeichnet einen Streifen mit platzierten Rechtecken, einem fliegenden Rect
 * (Einflug-Animation von oben) und einer Vorschau des nächsten Stücks.
 *
 * Koordinatenursprung ist unten-links; y-Achse wird gespiegelt.
 */
public class StripPanel extends JPanel {

    // ── Zustand ────────────────────────────────────────────────────────────────
    private PackResult result;
    private int   stepIndex   = -1;   // -1 = statisch; ≥0 = Animation
    private float flyProgress =  0f;  // 0=start, 1=gelandet

    // ── Layout ─────────────────────────────────────────────────────────────────
    private static final int HEADER    = 38;
    private static final int FOOTER    = 20;
    private static final int PREVIEW_H = 90;  // Vorschaubereich unten
    private static final int MARGIN    = 12;
    private static final int AXIS_L    = 28;  // Platz für y-Beschriftung

    private final String label;

    public StripPanel(String label) {
        this.label = label;
        setBackground(new Color(250, 251, 253));
    }

    // ── Öffentliche API ────────────────────────────────────────────────────────

    /** Statisch: alle Rechtecke sofort anzeigen, kein Animationsbereich. */
    public void setResult(PackResult result) {
        this.result    = result;
        this.stepIndex = -1;
        repaint();
    }

    /**
     * Animationsschritt: stepIndex = Index des aktuell fliegenden Rects
     * in result.rects (0-basiert). flyProgress 0→1 steuert den Einflug.
     */
    public void setAnimState(PackResult result, int stepIndex, float flyProgress) {
        this.result      = result;
        this.stepIndex   = stepIndex;
        this.flyProgress = flyProgress;
        repaint();
    }

    // ── Zeichnen ───────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        hint(g2);

        drawHeader(g2);

        if (result == null) {
            g2.setColor(new Color(140, 145, 165));
            g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g2.drawString("Noch keine Daten", MARGIN + AXIS_L, HEADER + 30);
            g2.dispose();
            return;
        }

        // ── Welche Rects sind platziert / fliegend / als Vorschau? ─────────────
        boolean animating = stepIndex >= 0;
        int   placedCount = animating
            ? Math.min(stepIndex, result.rects.size())
            : result.rects.size();

        Rect flyingRect = null;
        Rect nextRect   = null;
        if (animating && stepIndex < result.rects.size()) {
            flyingRect = result.rects.get(stepIndex);
            if (stepIndex + 1 < result.rects.size())
                nextRect = result.rects.get(stepIndex + 1);
        }

        // ── Layoutberechnung ───────────────────────────────────────────────────
        int W = result.stripWidth;
        int H = Math.max(result.totalHeight, 1);

        int previewSpace = animating ? PREVIEW_H : 0;
        int availW = getWidth()  - 2 * MARGIN - AXIS_L - 4;
        int availH = getHeight() - HEADER - FOOTER - 2 * MARGIN - previewSpace;
        if (availW <= 0 || availH <= 0) { g2.dispose(); return; }

        double scale = Math.min((double) availW / W, (double) availH / H);
        int    drawW = (int) (W * scale);
        int    drawH = (int) (H * scale);
        int    offX  = MARGIN + AXIS_L + (availW - drawW) / 2;
        int    offY  = HEADER + MARGIN;

        // ── Streifen zeichnen ──────────────────────────────────────────────────
        drawGrid(g2, offX, offY, drawW, drawH, H, scale);
        drawPlaced(g2, result.rects, placedCount, offX, offY, drawH, scale);

        // Fliegendes Rect: Clip auf Bereich unterhalb des Headers setzen,
        // damit es beim Einflug sichtbar (aber nicht über den Header gemalt) wird.
        if (flyingRect != null) {
            Shape oldClip = g2.getClip();
            g2.clipRect(0, HEADER, getWidth(), getHeight() - HEADER);
            drawFlying(g2, flyingRect, offX, offY, drawW, drawH, scale);
            g2.setClip(oldClip);
        }

        drawBorder(g2, offX, offY, drawW, drawH);
        drawAxes(g2, offX, offY, drawW, drawH, W, H, scale);

        // ── Statistik-Fußzeile ─────────────────────────────────────────────────
        int statsY = offY + drawH + FOOTER - 2;
        drawFooter(g2, placedCount, statsY, animating);

        // ── Vorschaubereich ────────────────────────────────────────────────────
        if (animating) {
            drawPreview(g2, nextRect, getHeight() - PREVIEW_H);
        }

        g2.dispose();
    }

    // ── Einzelne Zeichenmethoden ───────────────────────────────────────────────

    private void drawHeader(Graphics2D g2) {
        GradientPaint gp = new GradientPaint(
            0, 0, new Color(40, 80, 140),
            0, HEADER, new Color(60, 110, 180));
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), HEADER);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, (getWidth() - fm.stringWidth(label)) / 2, HEADER - 10);
    }

    private void drawGrid(Graphics2D g2, int offX, int offY,
                          int drawW, int drawH, int H, double scale) {
        g2.setColor(new Color(244, 246, 250));
        g2.fillRect(offX, offY, drawW, drawH);

        g2.setColor(new Color(208, 214, 228));
        g2.setStroke(new BasicStroke(0.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
            1f, new float[]{4, 4}, 0));
        int step = gridStep(H);
        for (int gy = step; gy < H; gy += step) {
            int sy = offY + drawH - (int) (gy * scale);
            g2.drawLine(offX, sy, offX + drawW, sy);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawPlaced(Graphics2D g2, List<Rect> rects, int count,
                            int offX, int offY, int drawH, double scale) {
        for (int i = 0; i < count; i++) {
            Rect r  = rects.get(i);
            int  sx = offX + (int) (r.x * scale);
            int  sy = offY + drawH - (int) ((r.y + r.h) * scale);
            int  sw = Math.max(1, (int) (r.w * scale));
            int  sh = Math.max(1, (int) (r.h * scale));
            paintRect(g2, r, sx, sy, sw, sh, false);
        }
    }

    private void drawFlying(Graphics2D g2, Rect r,
                            int offX, int offY, int drawW, int drawH, double scale) {
        int sx = offX + (int) (r.x * scale);
        int sw = Math.max(1, (int) (r.w * scale));
        int sh = Math.max(1, (int) (r.h * scale));

        // Zielposition im Streifen
        int finalSy = offY + drawH - (int) ((r.y + r.h) * scale);
        // Startposition: sh Pixel oberhalb des Streifens (gerade noch unsichtbar)
        int startSy = offY - sh;

        // Ease-out cubic: schnell am Anfang, verlangsamt beim Landen
        float t   = Math.min(1f, Math.max(0f, flyProgress));
        float eas = 1f - (float) Math.pow(1.0 - t, 3);
        int   sy  = startSy + (int) ((finalSy - startSy) * eas);

        // Bewegungsschatten (senkrechte Spur, verblasst nach oben)
        if (t < 0.88f) {
            int trailLen = sh + (int) (sh * (1f - t) * 0.6f);
            for (int i = 1; i <= 4; i++) {
                int alpha = (int) (55 * (1f - t) / i);
                g2.setColor(new Color(
                    r.color.getRed(), r.color.getGreen(), r.color.getBlue(), alpha));
                g2.fillRect(sx, sy - i * sh / 3, sw, sh);
            }
        }

        // Rect selbst
        paintRect(g2, r, sx, sy, sw, sh, true);

        // Lande-Glow (weißer Puls wenn t > 0.75)
        if (t > 0.75f) {
            float glowT = (t - 0.75f) / 0.25f;
            // Sinuswelle: blinkt kurz auf und verblasst wieder
            float pulse = (float) Math.sin(glowT * Math.PI);
            int   alpha = (int) (180 * pulse);
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRect(sx, sy, sw, sh);
            g2.setStroke(new BasicStroke(1f));
        }
    }

    private void paintRect(Graphics2D g2, Rect r, int sx, int sy, int sw, int sh,
                           boolean flying) {
        // Füllung
        g2.setColor(r.color);
        g2.fillRect(sx, sy, sw, sh);

        // Glanzeffekt
        if (sw > 4 && sh > 4) {
            GradientPaint shine = new GradientPaint(
                sx, sy, new Color(255, 255, 255, flying ? 90 : 55),
                sx, sy + sh / 2, new Color(255, 255, 255, 0));
            g2.setPaint(shine);
            g2.fillRect(sx, sy, sw, sh / 2);
        }

        // Rahmen
        g2.setColor(r.color.darker());
        g2.setStroke(new BasicStroke(flying ? 1.5f : 1f));
        g2.drawRect(sx, sy, sw, sh);
        g2.setStroke(new BasicStroke(1f));

        // Beschriftung: ID + Maße
        if (sw >= 16 && sh >= 12) {
            int fsize = Math.min(11, Math.max(7, (int) (Math.min(sw, sh) * 0.38)));
            g2.setFont(new Font("SansSerif", Font.BOLD, fsize));
            FontMetrics fm = g2.getFontMetrics();
            String top = String.valueOf(r.id);
            g2.setColor(new Color(0, 0, 0, 165));
            g2.drawString(top,
                sx + (sw - fm.stringWidth(top)) / 2,
                sy + (sh + fm.getAscent() - fm.getDescent()) / 2);

            if (sh >= 28 && sw >= 22) {
                String dim = r.w + "×" + r.h;
                int fs2 = Math.max(6, fsize - 2);
                g2.setFont(new Font("SansSerif", Font.PLAIN, fs2));
                FontMetrics fm2 = g2.getFontMetrics();
                if (fm2.stringWidth(dim) < sw - 2)
                    g2.drawString(dim,
                        sx + (sw - fm2.stringWidth(dim)) / 2,
                        sy + (sh + fm.getAscent() - fm.getDescent()) / 2 + fm2.getHeight() - 1);
            }
        }
    }

    private void drawPreview(Graphics2D g2, Rect next, int previewY) {
        int pw = getWidth();

        // Hintergrund
        g2.setColor(new Color(228, 233, 248));
        g2.fillRect(0, previewY, pw, PREVIEW_H);
        g2.setColor(new Color(170, 177, 210));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, previewY, pw, previewY);

        // Abschnitts-Label
        g2.setColor(new Color(65, 80, 130));
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("NÄCHSTES", MARGIN, previewY + 14);

        if (next == null) {
            g2.setColor(new Color(130, 138, 170));
            g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
            String msg = "— letztes Stück —";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (pw - fm.stringWidth(msg)) / 2, previewY + PREVIEW_H / 2 + 5);
            return;
        }

        // Maße rechts oben
        g2.setColor(new Color(90, 100, 140));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        String dim = next.w + " × " + next.h;
        FontMetrics fmd = g2.getFontMetrics();
        g2.drawString(dim, pw - MARGIN - fmd.stringWidth(dim), previewY + 14);

        // Rect skaliert in den Vorschaubereich passen
        int boxW = pw - 2 * MARGIN - 4;
        int boxH = PREVIEW_H - 22;
        double ps = Math.min(
            Math.min((double) boxW / next.w, (double) boxH / next.h),
            4.0); // max. 4px/Einheit damit es nicht riesig wird
        int prw = Math.max(6, (int) (next.w * ps));
        int prh = Math.max(6, (int) (next.h * ps));
        int prx = (pw - prw) / 2;
        int pry = previewY + 20 + (boxH - prh) / 2;

        // Schatten
        g2.setColor(new Color(0, 0, 0, 45));
        g2.fillRect(prx + 3, pry + 3, prw, prh);

        // Vorschau-Rect
        paintRect(g2, next, prx, pry, prw, prh, false);
    }

    private void drawBorder(Graphics2D g2, int offX, int offY, int drawW, int drawH) {
        g2.setColor(new Color(35, 55, 100));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(offX, offY, drawW, drawH);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawAxes(Graphics2D g2, int offX, int offY,
                          int drawW, int drawH, int W, int H, double scale) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.setColor(new Color(80, 88, 118));
        FontMetrics fm = g2.getFontMetrics();

        // y=0 und y=H
        String s0 = "0", sH = String.valueOf(H);
        g2.drawString(s0, offX - fm.stringWidth(s0) - 4, offY + drawH + 3);
        g2.drawString(sH, offX - fm.stringWidth(sH) - 4, offY + fm.getAscent());

        // Zwischenwerte
        int step = gridStep(H);
        for (int gy = step; gy < H; gy += step) {
            int   sy = offY + drawH - (int) (gy * scale);
            String s = String.valueOf(gy);
            g2.drawString(s, offX - fm.stringWidth(s) - 4, sy + fm.getAscent() / 2);
        }

        // Strip-Breite
        String sW = "W=" + W;
        g2.drawString(sW, offX + (drawW - fm.stringWidth(sW)) / 2, offY + drawH + 12);
    }

    private void drawFooter(Graphics2D g2, int placedCount, int y, boolean animating) {
        if (result == null) return;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(new Color(60, 68, 98));
        String prog = animating
            ? String.format("(%d/%d)  ", placedCount, result.rects.size()) : "";
        String txt  = String.format("%sH=%d  Effizienz=%.1f%%  %dms",
            prog, result.totalHeight, result.efficiency, result.timeMs);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, (getWidth() - fm.stringWidth(txt)) / 2, y);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private static void hint(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static int gridStep(int H) {
        int[] c = {1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000};
        for (int v : c) if (H / v <= 8) return v;
        return Math.max(1, H / 5);
    }
}
