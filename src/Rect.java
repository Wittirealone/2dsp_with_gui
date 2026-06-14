import java.awt.Color;

public class Rect {
    public int x, y, w, h;
    public final Color color;
    public final int id;

    public Rect(int w, int h, int id) {
        this.w = w;
        this.h = h;
        this.id = id;
        // Goldener-Schnitt-Verteilung: visuell distinkte Farben ohne Wiederholung
        float hue = (id * 0.618033988749895f) % 1.0f;
        this.color = Color.getHSBColor(hue, 0.65f, 0.88f);
    }

    /** Kopie mit gesetzter Position */
    public Rect placed(int x, int y) {
        Rect r = new Rect(w, h, id);
        r.x = x;
        r.y = y;
        return r;
    }

    public boolean overlaps(Rect o) {
        return x < o.x + o.w && x + w > o.x
            && y < o.y + o.h && y + h > o.y;
    }

    @Override
    public String toString() {
        return String.format("Rect#%d(%dx%d @%d,%d)", id, w, h, x, y);
    }
}
