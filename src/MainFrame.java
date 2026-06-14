import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MainFrame extends JFrame {

    // ── Steuerelemente ─────────────────────────────────────────────────────────
    private JSpinner spWidth, spCount, spMinW, spMaxW, spMinH, spMaxH, spSeed;
    private JCheckBox cbSeed;
    private JSlider   sldSpeed;

    // ── Visualisierung ─────────────────────────────────────────────────────────
    private StripPanel panNFDH, panFFDH, panBL;

    // ── Statistik-Labels ───────────────────────────────────────────────────────
    private JLabel lblStatNFDH, lblStatFFDH, lblStatBL;

    // ── Animation ─────────────────────────────────────────────────────────────
    private javax.swing.Timer fastTimer;
    private PackResult[] animResults;  // [NFDH, FFDH, BL]
    private int   animStep;            // Index des aktuell fliegenden Rects
    private float animProgress;        // 0.0 → 1.0 Einflugfortschritt

    /** Schneller Rendering-Takt für flüssige Animation (~50 fps). */
    private static final int TICK_MS = 20;

    // ── Konstruktor ────────────────────────────────────────────────────────────

    public MainFrame() {
        super("Strip Packing Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(6, 0));

        add(buildLeft(),   BorderLayout.WEST);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatus(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1250, 820));
        pack();
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
    }

    // ── Linkes Steuerfeld ──────────────────────────────────────────────────────

    private JPanel buildLeft() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(245, 246, 250));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(195, 198, 215)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        p.setPreferredSize(new Dimension(220, 0));

        p.add(section("STRIP-PARAMETER"));
        p.add(lbl("Strip-Breite"));
        spWidth = spin(100, 10, 3000, 10);
        p.add(spWidth); gap(p, 6);

        p.add(lbl("Anzahl Rechtecke"));
        spCount = spin(25, 1, 300, 1);
        p.add(spCount); gap(p, 6);

        p.add(lbl("Breite  min / max"));
        p.add(row2(spMinW = spin(5, 1, 999, 1), spMaxW = spin(25, 1, 999, 1)));
        gap(p, 4);

        p.add(lbl("Höhe  min / max"));
        p.add(row2(spMinH = spin(5, 1, 999, 1), spMaxH = spin(25, 1, 999, 1)));
        gap(p, 10);

        p.add(sep()); gap(p, 8);
        p.add(section("ZUFALLSGENERATOR"));

        cbSeed = new JCheckBox("Fixer Seed (reproduzierbar)");
        cbSeed.setOpaque(false);
        cbSeed.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cbSeed.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(cbSeed);
        spSeed = spin(42, 0, Integer.MAX_VALUE, 1);
        spSeed.setEnabled(false);
        cbSeed.addActionListener(e -> spSeed.setEnabled(cbSeed.isSelected()));
        p.add(spSeed); gap(p, 10);

        p.add(sep()); gap(p, 8);
        p.add(section("ANIMATION"));
        p.add(lbl("Schrittdauer (ms / Rechteck)"));

        sldSpeed = new JSlider(80, 1200, 400);
        sldSpeed.setOpaque(false);
        sldSpeed.setAlignmentX(Component.LEFT_ALIGNMENT);
        sldSpeed.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        Hashtable<Integer, JLabel> lt = new Hashtable<>();
        lt.put(80,   new JLabel("Schnell"));
        lt.put(1200, new JLabel("Langsam"));
        sldSpeed.setLabelTable(lt);
        sldSpeed.setPaintLabels(true);
        sldSpeed.setPaintTicks(true);
        sldSpeed.setMajorTickSpacing(560);
        p.add(sldSpeed); gap(p, 10);

        p.add(sep()); gap(p, 10);

        JButton btnPack = btn("▶  Sofort packen",    new Color(46, 125, 50));
        JButton btnAnim = btn("⏩  Animieren",        new Color(25, 90, 160));
        JButton btnStop = btn("⏹  Stopp",            new Color(150, 45, 35));

        btnPack.addActionListener(e -> runPacking(false));
        btnAnim.addActionListener(e -> runPacking(true));
        btnStop.addActionListener(e -> stopAnimation());

        p.add(btnPack); gap(p, 5);
        p.add(btnAnim); gap(p, 5);
        p.add(btnStop); gap(p, 14);

        p.add(sep()); gap(p, 8);
        p.add(section("ERGEBNISSE"));

        lblStatNFDH = statLbl();
        lblStatFFDH = statLbl();
        lblStatBL   = statLbl();
        p.add(lblStatNFDH); gap(p, 6);
        p.add(lblStatFFDH); gap(p, 6);
        p.add(lblStatBL);

        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Visualisierungsbereich ─────────────────────────────────────────────────

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridLayout(1, 3, 8, 0));
        p.setBackground(new Color(228, 231, 242));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        panNFDH = new StripPanel("NFDH");
        panFFDH = new StripPanel("FFDH");
        panBL   = new StripPanel("Bottom-Left");

        p.add(card(panNFDH));
        p.add(card(panFFDH));
        p.add(card(panBL));
        return p;
    }

    private JLabel buildStatus() {
        JLabel bar = new JLabel(
            "  Strip Packing Simulator  |  Rechtecke fallen per Animation von oben ein  |  Vorschau zeigt das nächste Stück");
        bar.setFont(new Font("SansSerif", Font.PLAIN, 11));
        bar.setForeground(new Color(80, 85, 110));
        bar.setOpaque(true);
        bar.setBackground(new Color(238, 240, 248));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(190, 193, 210)),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return bar;
    }

    // ── Packing-Logik ─────────────────────────────────────────────────────────

    private List<Rect> generateRects() {
        int n    = (int) spCount.getValue();
        int minW = (int) spMinW.getValue();
        int maxW = (int) spMaxW.getValue();
        int minH = (int) spMinH.getValue();
        int maxH = (int) spMaxH.getValue();
        if (minW > maxW) { int t = minW; minW = maxW; maxW = t; }
        if (minH > maxH) { int t = minH; minH = maxH; maxH = t; }

        Random rng = cbSeed.isSelected()
            ? new Random((int) spSeed.getValue())
            : new Random();

        List<Rect> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            out.add(new Rect(
                minW + rng.nextInt(maxW - minW + 1),
                minH + rng.nextInt(maxH - minH + 1),
                i + 1));
        return out;
    }

    private void runPacking(boolean animate) {
        stopAnimation();

        List<Rect> rects = generateRects();
        int W = (int) spWidth.getValue();

        PackResult rNFDH = NFDH.pack(rects, W);
        PackResult rFFDH = FFDH.pack(rects, W);
        PackResult rBL   = BottomLeft.pack(rects, W);

        updateStats(rNFDH, rFFDH, rBL);

        if (!animate) {
            panNFDH.setResult(rNFDH);
            panFFDH.setResult(rFFDH);
            panBL  .setResult(rBL);
            return;
        }

        // ── Animationsmodus ────────────────────────────────────────────────────
        animResults  = new PackResult[]{ rNFDH, rFFDH, rBL };
        StripPanel[] panels = { panNFDH, panFFDH, panBL };
        animStep     = 0;
        animProgress = 0f;

        int maxN = maxRects(rNFDH, rFFDH, rBL);

        // Alle Panels auf Schritt 0 (noch leer, erstes Rect fliegt gleich ein)
        for (int i = 0; i < 3; i++)
            panels[i].setAnimState(animResults[i], 0, 0f);

        fastTimer = new javax.swing.Timer(TICK_MS, e -> {
            float stepMs = sldSpeed.getValue();
            animProgress += TICK_MS / stepMs;

            if (animProgress >= 1f) {
                // Dieser Schritt ist fertig → nächstes Rect
                animProgress = 0f;
                animStep++;

                if (animStep >= maxN) {
                    // Alle Rects platziert
                    stopAnimation();
                    for (int i = 0; i < 3; i++)
                        panels[i].setResult(animResults[i]);
                    return;
                }
            }

            // Alle drei Panels synchron aktualisieren
            for (int i = 0; i < 3; i++)
                panels[i].setAnimState(animResults[i], animStep, animProgress);
        });
        fastTimer.start();
    }

    private void stopAnimation() {
        if (fastTimer != null && fastTimer.isRunning()) {
            fastTimer.stop();
            fastTimer = null;
        }
    }

    private static int maxRects(PackResult... rs) {
        int m = 0;
        for (PackResult r : rs) m = Math.max(m, r.rects.size());
        return m;
    }

    private void updateStats(PackResult a, PackResult b, PackResult c) {
        lblStatNFDH.setText(statText(a));
        lblStatFFDH.setText(statText(b));
        lblStatBL  .setText(statText(c));
    }

    private String statText(PackResult r) {
        return String.format(
            "<html><b style='color:#1a4080'>%s</b><br>"
            + "Höhe: <b>%d</b> &nbsp;|&nbsp; Eff: <b>%.1f%%</b><br>"
            + "Rects: %d &nbsp; Zeit: %dms</html>",
            r.algorithmName, r.totalHeight, r.efficiency,
            r.rects.size(), r.timeMs);
    }

    // ── UI-Hilfsmethoden ──────────────────────────────────────────────────────

    private JLabel section(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(new Color(80, 95, 140));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(new Color(50, 55, 75));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel statLbl() {
        JLabel l = new JLabel("<html><i style='color:gray'>– noch keine Daten –</i></html>");
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JSpinner spin(int val, int min, int max, int step) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(val, min, max, step));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return s;
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return b;
    }

    private JPanel row2(JComponent a, JComponent b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 5, 0));
        p.setOpaque(false);
        p.add(a); p.add(b);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return p;
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator();
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setForeground(new Color(195, 198, 215));
        return s;
    }

    private void gap(JPanel p, int h) {
        p.add(Box.createRigidArea(new Dimension(0, h)));
    }

    private JPanel card(StripPanel sp) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createLineBorder(new Color(175, 180, 205), 1));
        c.add(sp, BorderLayout.CENTER);
        return c;
    }
}
