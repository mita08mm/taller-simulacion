import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.Arrays;

/**
 * Simulacion de Proyecto de Inversion "
 */
public class SimulacionInversion extends JFrame {

    private static final Color AZUL_OSCURO = new Color(30, 60, 114);
    private static final Color AZUL_MEDIO  = new Color(42, 82, 152);
    private static final Color AZUL_CLARO  = new Color(100, 149, 237);
    private static final Color VERDE       = new Color(39, 174, 96);
    private static final Color VERDE_CLARO = new Color(220, 255, 230);
    private static final Color ROJO        = new Color(192, 57, 43);
    private static final Color ROJO_CLARO  = new Color(255, 225, 220);
    private static final Color FONDO       = new Color(240, 244, 250);
    private static final Color BLANCO      = Color.WHITE;
    private static final Color GRIS_TEXTO  = new Color(60, 60, 80);
    private static final Color GRIS_TABLA  = new Color(245, 247, 252);

    // Campos con valores por defecto del libro (Ejemplo 5.4)
    private JTextField txtNumCorridas = campo("1000");
    private JTextField txtAFPes       = campo("-100000");
    private JTextField txtAFProb      = campo("-70000");
    private JTextField txtAFOpt       = campo("-60000");
    private JTextField txtACPes       = campo("-40000");
    private JTextField txtACProb      = campo("-30000");
    private JTextField txtACOpt       = campo("-25000");
    private JTextField txtFlujoPes    = campo("30000");
    private JTextField txtFlujoProb   = campo("40000");
    private JTextField txtFlujoOpt    = campo("45000");
    private JTextField txtImpuestos   = campo("50");
    private JTextField txtTREMA       = campo("15");
    private JTextField txtVida        = campo("5");

    private JTextArea        areaResultados;
    private PanelLineas      panelLineas;
    private PanelHist        panelHist;
    private JLabel           lblDecision;
    private JButton          btnSimular;
    private JTable           tabla;
    private DefaultTableModel modeloTabla;
    private double[]         tires;

    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimulacionInversion().setVisible(true));
    }

    public SimulacionInversion() {
        super("Simulacion de Proyecto de Inversion");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 760);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(FONDO);
        construirUI();
    }

    
    private void construirUI() {
        setLayout(new BorderLayout(6, 6));
        add(encabezado(), BorderLayout.NORTH);

        // Izquierda: entradas
        JScrollPane izq = new JScrollPane(panelEntradas());
        izq.setBorder(BorderFactory.createEmptyBorder());
        izq.setPreferredSize(new Dimension(295, 0));
        izq.getVerticalScrollBar().setUnitIncrement(12);

        // Centro/Derecha: pestanas (resultados + tabla + graficas)
        JTabbedPane tabs = crearTabs();

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izq, tabs);
        sp.setDividerLocation(298);
        sp.setDividerSize(4);
        sp.setBorder(new EmptyBorder(0, 8, 8, 8));
        add(sp, BorderLayout.CENTER);
    }

    // ── Encabezado ─────────────────────────────────────────────────────────────
    private JPanel encabezado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(AZUL_OSCURO);
        p.setBorder(new EmptyBorder(10, 18, 10, 18));
        JLabel t1 = new JLabel("Simulacion de Proyecto de Inversion");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t1.setForeground(BLANCO);
        JLabel t2 = new JLabel("Distribuciones triangulares  ");
        t2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        t2.setForeground(new Color(180, 200, 230));
        p.add(t1, BorderLayout.NORTH);
        p.add(t2, BorderLayout.SOUTH);
        return p;
    }

    // ── Panel entradas ─────────────────────────────────────────────────────────
    private JPanel panelEntradas() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(FONDO);
        p.setBorder(new EmptyBorder(8, 8, 8, 4));

        p.add(grupo("Configuracion",
            new String[]{"Num. corridas"}, new JTextField[]{txtNumCorridas}));
        p.add(Box.createVerticalStrut(6));
        p.add(grupoTri("Activo Fijo Inicial ($)", txtAFPes, txtAFProb, txtAFOpt));
        p.add(Box.createVerticalStrut(6));
        p.add(grupoTri("Activo Circulante ($)", txtACPes, txtACProb, txtACOpt));
        p.add(Box.createVerticalStrut(6));
        p.add(grupoTri("Flujo Anual Antes Imp. ($)", txtFlujoPes, txtFlujoProb, txtFlujoOpt));
        p.add(Box.createVerticalStrut(6));
        p.add(grupo("Parametros Financieros",
            new String[]{"Impuestos (%)", "TREMA (%)", "Vida (anios)"},
            new JTextField[]{txtImpuestos, txtTREMA, txtVida}));
        p.add(Box.createVerticalStrut(10));

        btnSimular = new JButton("Ejecutar Simulacion");
        btnSimular.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSimular.setBackground(AZUL_MEDIO);
        btnSimular.setForeground(BLANCO);
        btnSimular.setFocusPainted(false);
        btnSimular.setBorder(new EmptyBorder(10, 16, 10, 16));
        btnSimular.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSimular.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnSimular.setOpaque(true);
        btnSimular.addActionListener(e -> ejecutar());
        btnSimular.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnSimular.setBackground(AZUL_CLARO); }
            public void mouseExited(MouseEvent e)  { btnSimular.setBackground(AZUL_MEDIO); }
        });
        p.add(btnSimular);
        p.add(Box.createVerticalStrut(8));

        lblDecision = new JLabel("  --  ");
        lblDecision.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDecision.setHorizontalAlignment(SwingConstants.CENTER);
        lblDecision.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        lblDecision.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 210, 230), 1, true),
            new EmptyBorder(6, 8, 6, 8)));
        lblDecision.setBackground(BLANCO);
        lblDecision.setOpaque(true);
        p.add(lblDecision);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel grupo(String titulo, String[] etiq, JTextField[] campos) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BLANCO);
        p.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 215, 235), 1, true),
            new EmptyBorder(8, 10, 10, 10)));
        JLabel lt = new JLabel(titulo);
        lt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lt.setForeground(AZUL_OSCURO);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 2, 4, 2);
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        p.add(lt, gc);
        gc.gridwidth = 1;
        for (int i = 0; i < etiq.length; i++) {
            gc.gridy = i + 1; gc.gridx = 0; gc.weightx = 0.55;
            JLabel l = new JLabel(etiq[i]);
            l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            l.setForeground(GRIS_TEXTO);
            p.add(l, gc);
            gc.gridx = 1; gc.weightx = 0.45;
            p.add(campos[i], gc);
        }
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 8));
        return p;
    }

    private JPanel grupoTri(String t, JTextField a, JTextField b, JTextField c) {
        return grupo(t, new String[]{"Pesimista", "Mas probable", "Optimista"},
                        new JTextField[]{a, b, c});
    }

    // ── Pestanas principales ───────────────────────────────────────────────────
    private JTabbedPane crearTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Tab 1: Resumen estadistico
        tabs.addTab("Resumen", iconoTab(AZUL_MEDIO), panelResumen());

        // Tab 2: Tabla de simulaciones
        tabs.addTab("Tabla de Simulaciones", iconoTab(new Color(80, 120, 200)), panelTabla());

        // Tab 3: Grafica TIR por corrida
        panelLineas = new PanelLineas();
        tabs.addTab("TIR por Corrida", iconoTab(VERDE), panelLineas);

        // Tab 4: Histograma
        panelHist = new PanelHist();
        tabs.addTab("Histograma TIR", iconoTab(new Color(180, 80, 20)), panelHist);

        return tabs;
    }

    private Icon iconoTab(Color c) {
        return new Icon() {
            public int getIconWidth()  { return 10; }
            public int getIconHeight() { return 10; }
            public void paintIcon(Component comp, Graphics g, int x, int y) {
                g.setColor(c);
                g.fillOval(x, y, 9, 9);
            }
        };
    }

    // ── Tab Resumen ────────────────────────────────────────────────────────────
    private JPanel panelResumen() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(FONDO);
        p.setBorder(new EmptyBorder(6, 6, 6, 6));

        areaResultados = new JTextArea(
            "Ingrese parametros y haga clic en  Ejecutar Simulacion\n\n" +
            "Valores por defecto: Ejemplo 5.4 del libro\n" +
            "(Coss Bu - Simulacion: Un Enfoque Practico)\n\n" +
            "Variables con distribucion triangular:\n" +
            "  - Activo fijo inicial\n" +
            "  - Activo circulante inicial\n" +
            "  - Flujo de efectivo antes de impuestos\n\n" +
            "Se calcula la TIR por Monte Carlo y se evalua\n" +
            "la probabilidad de superar la TREMA.");
        areaResultados.setFont(new Font("Consolas", Font.PLAIN, 13));
        areaResultados.setEditable(false);
        areaResultados.setBackground(new Color(18, 22, 36));
        areaResultados.setForeground(new Color(180, 220, 180));
        areaResultados.setBorder(new EmptyBorder(12, 14, 12, 14));
        areaResultados.setLineWrap(false);

        JScrollPane s = new JScrollPane(areaResultados);
        s.setBorder(new TitledBorder(
            new LineBorder(AZUL_MEDIO, 1, true), " Estadisticas de la Simulacion ",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 11), AZUL_OSCURO));
        p.add(s, BorderLayout.CENTER);
        return p;
    }

    // ── Tab Tabla ──────────────────────────────────────────────────────────────
    private JPanel panelTabla() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBackground(FONDO);
        p.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Columnas
        String[] cols = {
            "#", "AF Simulado ($)", "AC Simulado ($)",
            "Flujo Simulado ($)", "VPN ($)", "TIR (%)",
            "TIR > TREMA", "Decision"
        };
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(modeloTabla);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tabla.setRowHeight(22);
        tabla.setGridColor(new Color(220, 228, 240));
        tabla.setShowVerticalLines(true);
        tabla.setShowHorizontalLines(true);
        tabla.setSelectionBackground(new Color(180, 210, 250));
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Header
        JTableHeader header = tabla.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setBackground(AZUL_OSCURO);
        header.setForeground(BLANCO);
        header.setReorderingAllowed(false);

        // Renderer para colorear filas segun decision
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    Object decision = t.getValueAt(row, 7);
                    if ("ACEPTAR".equals(decision)) {
                        c.setBackground(row % 2 == 0 ? VERDE_CLARO : new Color(205, 245, 215));
                    } else {
                        c.setBackground(row % 2 == 0 ? ROJO_CLARO  : new Color(250, 215, 210));
                    }
                }
                // Alinear numeros a la derecha
                if (col >= 1 && col <= 5) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                }
                // Colorear columna decision
                if (col == 7) {
                    if ("ACEPTAR".equals(val)) {
                        c.setForeground(new Color(0, 120, 50));
                        ((JLabel)c).setFont(new Font("Segoe UI", Font.BOLD, 11));
                    } else {
                        c.setForeground(new Color(160, 30, 20));
                        ((JLabel)c).setFont(new Font("Segoe UI", Font.BOLD, 11));
                    }
                } else {
                    c.setForeground(GRIS_TEXTO);
                    ((JLabel)c).setFont(new Font("Segoe UI", Font.PLAIN, 11));
                }
                return c;
            }
        });

        // Anchos de columna
        int[] anchos = {45, 115, 115, 120, 100, 75, 80, 80};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new TitledBorder(
            new LineBorder(AZUL_MEDIO, 1, true),
            " Datos de cada simulacion  (verde = TIR supera TREMA  |  rojo = TIR no supera TREMA) ",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 11), AZUL_OSCURO));

        // Barra de busqueda / info
        JPanel barraInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barraInfo.setBackground(FONDO);
        JLabel lblInfo = new JLabel("Total filas: 0");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblInfo.setForeground(GRIS_TEXTO);
        barraInfo.add(lblInfo);

        // Guardar referencia para actualizar
        tabla.putClientProperty("lblInfo", lblInfo);

        p.add(barraInfo, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // LOGICA DE SIMULACION
    // =========================================================================
    private void ejecutar() {
        btnSimular.setEnabled(false);
        btnSimular.setText("Simulando...");
        SwingWorker<Object[][], Void> w = new SwingWorker<Object[][], Void>() {
            protected Object[][] doInBackground() { return simular(); }
            protected void done() {
                try {
                    Object[][] filas = get();
                    poblarTabla(filas);
                    mostrarResultados(filas);
                } catch (Exception ex) {
                    areaResultados.setText("Error: " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    btnSimular.setEnabled(true);
                    btnSimular.setText("Ejecutar Simulacion");
                }
            }
        };
        w.execute();
    }

    /**
     * Devuelve matriz: cada fila = { #, AF, AC, Flujo, VPN, TIR%, superaTREMA, decision }
     */
    private Object[][] simular() {
        int    n    = entInt(txtNumCorridas, 1000);
        double afA  = entDbl(txtAFPes,   -100000);
        double afB  = entDbl(txtAFProb,   -70000);
        double afC  = entDbl(txtAFOpt,    -60000);
        double acA  = entDbl(txtACPes,    -40000);
        double acB  = entDbl(txtACProb,   -30000);
        double acC  = entDbl(txtACOpt,    -25000);
        double flA  = entDbl(txtFlujoPes,  30000);
        double flB  = entDbl(txtFlujoProb, 40000);
        double flC  = entDbl(txtFlujoOpt,  45000);
        double T    = entDbl(txtImpuestos, 50) / 100.0;
        double trema = entDbl(txtTREMA, 15);
        int    vida = entInt(txtVida, 5);

        // Inflacion del libro 
        double[] iA = {18,18,22,25,28};
        double[] iB = {15,15,18,20,22};
        double[] iC = {12,12,15,18,19};

        Random rnd     = new Random();
        Object[][] res = new Object[n][8];
        tires          = new double[n];

        for (int c = 0; c < n; c++) {
            double AF  = tri(rnd, afA, afB, afC);
            double AC  = tri(rnd, acA, acB, acC);
            double xF  = tri(rnd, flA, flB, flC);
            double dep = Math.abs(AF) / vida;
            double dfl = 1.0;
            double[] fl = new double[vida];

            for (int t = 0; t < vida; t++) {
                int ix = Math.min(t, iA.length - 1);
                dfl *= (1 + tri(rnd, iA[ix], iB[ix], iC[ix]) / 100.0);
                double fc = xF * dfl; 
                double imp = Math.max(0, (fc - dep) * T);
                fl[t] = (fc - imp) / dfl;
            }

            double VR  = (Math.abs(AC) + Math.abs(AF) * 0.20 * (1 - T)) / dfl;
            double inv = AF + AC;
            double tirVal = tir(inv, fl, VR);
            double vpnVal = vpn(trema / 100.0, inv, fl, VR);

            tires[c] = tirVal * 100;
            boolean supera = tires[c] > trema;

            res[c][0] = c + 1;
            res[c][1] = String.format("%,.0f", AF);
            res[c][2] = String.format("%,.0f", AC);
            res[c][3] = String.format("%,.0f", xF);
            res[c][4] = String.format("%,.2f", vpnVal);
            res[c][5] = String.format("%.2f", tires[c]);
            res[c][6] = supera ? "Si" : "No";
            res[c][7] = supera ? "ACEPTAR" : "RECHAZAR";
        }
        return res;
    }

    private void poblarTabla(Object[][] filas) {
        modeloTabla.setRowCount(0);
        for (Object[] fila : filas) modeloTabla.addRow(fila);

        // Actualizar label info
        JLabel lbl = (JLabel) tabla.getClientProperty("lblInfo");
        if (lbl != null) {
            long aceptados = Arrays.stream(filas)
                .filter(f -> "ACEPTAR".equals(f[7])).count();
            lbl.setText(String.format(
                "Total corridas: %,d   |   Aceptar: %,d   |   Rechazar: %,d",
                filas.length, aceptados, filas.length - aceptados));
        }
    }

    private void mostrarResultados(Object[][] filas) {
        double trema = entDbl(txtTREMA, 15);
        int n = tires.length;
        double suma = 0, mn = tires[0], mx = tires[0];
        for (double v : tires) { suma += v; if (v < mn) mn = v; if (v > mx) mx = v; }
        double med = suma / n, var = 0;
        for (double v : tires) var += (v - med) * (v - med);
        double desv = Math.sqrt(var / n);

        int sup = 0;
        for (double v : tires) if (v > trema) sup++;
        double prob = (double) sup / n;

        double[] ord = tires.clone();
        Arrays.sort(ord);

        // Histograma para texto
        int bins = 10;
        double rng = mx - mn; if (rng == 0) rng = 1;
        int[] frec = new int[bins];
        for (double v : tires) {
            int idx = (int)((v - mn) / rng * bins);
            if (idx >= bins) idx = bins - 1;
            frec[idx]++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=================================================\n");
        sb.append("   RESULTADOS DE LA SIMULACION\n");
        sb.append("=================================================\n\n");
        sb.append(String.format("  Corridas realizadas : %,d%n", n));
        sb.append(String.format("  TIR minima          : %8.2f %%%n", mn));
        sb.append(String.format("  TIR maxima          : %8.2f %%%n", mx));
        sb.append(String.format("  TIR media (E[TIR])  : %8.2f %%%n", med));
        sb.append(String.format("  Desv. estandar      : %8.2f %%%n", desv));
        sb.append(String.format("%n  TREMA fijada        : %8.2f %%%n", trema));
        sb.append(String.format("  Corridas > TREMA    : %,d  de  %,d%n", sup, n));
        sb.append(String.format("  Prob(TIR > TREMA)   : %8.4f   (%.2f %%)%n",
                                prob, prob * 100));
        sb.append("\n-------------------------------------------------\n");
        sb.append("  PERCENTILES\n");
        sb.append("-------------------------------------------------\n");
        sb.append(String.format("  P10  : %8.2f %%%n", ord[(int)(0.10 * n)]));
        sb.append(String.format("  P25  : %8.2f %%%n", ord[(int)(0.25 * n)]));
        sb.append(String.format("  P50  : %8.2f %%%n", ord[(int)(0.50 * n)]));
        sb.append(String.format("  P75  : %8.2f %%%n", ord[(int)(0.75 * n)]));
        sb.append(String.format("  P90  : %8.2f %%%n", ord[(int)(0.90 * n)]));
        sb.append("\n-------------------------------------------------\n");
        sb.append("  DISTRIBUCION DE FRECUENCIAS TIR\n");
        sb.append("-------------------------------------------------\n");
        sb.append(String.format("  %-14s %-14s %-8s %-8s%n",
                                "Lim Inf(%)", "Lim Sup(%)", "Frec.", "Acum."));
        int acum = 0;
        for (int i = 0; i < bins; i++) {
            double li = mn + i * rng / bins;
            double ls = li + rng / bins;
            acum += frec[i];
            sb.append(String.format("  %-14.2f %-14.2f %-8d %-8d%n",
                                    li, ls, frec[i], acum));
        }
        sb.append("\n=================================================\n");
        sb.append("  DECISION\n");
        sb.append("=================================================\n");
        if (prob >= 0.90) {
            sb.append("  ACEPTAR el proyecto\n");
            sb.append("  Prob(TIR>TREMA) >= 90%  ->  rentabilidad suficiente.\n");
            lblDecision.setText("ACEPTAR el proyecto");
            lblDecision.setForeground(VERDE);
            lblDecision.setBackground(VERDE_CLARO);
        } else {
            sb.append("  RECHAZAR el proyecto\n");
            sb.append("  Prob(TIR>TREMA) < 90%  ->  riesgo elevado.\n");
            lblDecision.setText("RECHAZAR el proyecto");
            lblDecision.setForeground(ROJO);
            lblDecision.setBackground(ROJO_CLARO);
        }

        areaResultados.setText(sb.toString());
        areaResultados.setCaretPosition(0);
        panelLineas.setDatos(tires, trema);
        panelHist.setDatos(tires, trema);
    }

    // =========================================================================
    // METODOS MATEMATICOS
    // =========================================================================
    private double tri(Random rnd, double a, double b, double c) {
        boolean neg = (a < 0 && c < 0);
        if (neg) { double t = Math.abs(c); c = Math.abs(a); a = t; b = Math.abs(b); }
        double R = rnd.nextDouble();
        double u = (b - a) / (c - a);
        double x = R <= u
            ? a + Math.sqrt((c - a) * (b - a) * R)
            : c - Math.sqrt((c - a) * (c - b) * (1 - R));
        return neg ? -x : x;
    }

    private double tir(double inv, double[] fl, double VR) {
        double lo = -0.99, hi = 9.0;
        double flo = vpn(lo, inv, fl, VR), fhi = vpn(hi, inv, fl, VR);
        if (flo * fhi > 0) return Math.abs(flo) < Math.abs(fhi) ? lo : hi;
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2, fm = vpn(mid, inv, fl, VR);
            if (Math.abs(fm) < 1e-6) return mid;
            if (flo * fm < 0) hi = mid; else { lo = mid; flo = fm; }
        }
        return (lo + hi) / 2;
    }

    private double vpn(double r, double inv, double[] fl, double VR) {
        double v = inv;
        for (int t = 0; t < fl.length; t++) v += fl[t] / Math.pow(1 + r, t + 1);
        v += VR / Math.pow(1 + r, fl.length);
        return v;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static JTextField campo(String def) {
        JTextField tf = new JTextField(def, 9);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tf.setHorizontalAlignment(SwingConstants.RIGHT);
        tf.setBorder(new CompoundBorder(
            new LineBorder(new Color(180, 200, 230), 1, true),
            new EmptyBorder(2, 4, 2, 4)));
        return tf;
    }

    private double entDbl(JTextField tf, double def) {
        try { return Double.parseDouble(tf.getText().trim()); } catch (Exception e) { return def; }
    }

    private int entInt(JTextField tf, int def) {
        try { return Integer.parseInt(tf.getText().trim()); } catch (Exception e) { return def; }
    }

    // =========================================================================
    // GRAFICAS
    // =========================================================================
    static class PanelLineas extends JPanel {
        private double[] datos; private double trema;
        PanelLineas() { setBackground(new Color(18, 22, 36)); }
        void setDatos(double[] d, double tr) { datos = d; trema = tr; repaint(); }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (datos == null) { sinDatos(g); return; }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int mg=50, w=getWidth()-mg*2, h=getHeight()-mg*2;
            double mn=datos[0], mx=datos[0];
            for (double v:datos){if(v<mn)mn=v;if(v>mx)mx=v;}
            double rng=mx-mn; if(rng==0)rng=1;

            // Grid
            g2.setColor(new Color(40,50,70));
            for(int i=0;i<=6;i++) g2.drawLine(mg,mg+h*i/6,mg+w,mg+h*i/6);

            // TREMA
            int yTr=mg+h-(int)((trema-mn)/rng*h);
            g2.setColor(new Color(255,210,0,200));
            g2.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{7,4},0));
            g2.drawLine(mg,yTr,mg+w,yTr);
            g2.setFont(new Font("Segoe UI",Font.BOLD,10));
            g2.drawString("TREMA="+(int)trema+"%",mg+6,yTr-4);

            // Barras
            g2.setStroke(new BasicStroke(1f));
            int paso=Math.max(1,datos.length/w);
            int yBase=mg+h-(int)((0-mn)/rng*h);
            for(int i=0;i<datos.length;i+=paso){
                int x=mg+(int)((double)i/datos.length*w);
                int y=mg+h-(int)((datos[i]-mn)/rng*h);
                g2.setColor(datos[i]>trema?new Color(0,200,120,210):new Color(220,80,60,210));
                g2.drawLine(x,yBase,x,y);
            }

            // Ejes
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(160,180,210));
            g2.drawLine(mg,mg,mg,mg+h);
            g2.drawLine(mg,mg+h,mg+w,mg+h);

            // Etiquetas Y
            g2.setFont(new Font("Segoe UI",Font.PLAIN,10));
            for(int i=0;i<=6;i++){
                double val=mx-(mx-mn)*i/6.0;
                g2.drawString(String.format("%.1f%%",val),2,mg+h*i/6+4);
            }
            // Etiquetas X (indices de corrida)
            for(int i=0;i<=4;i++){
                int x=mg+w*i/4;
                int idx=(int)((double)i/4*(datos.length-1));
                g2.drawString(String.valueOf(idx+1),x-8,mg+h+14);
            }

            // Titulo
            g2.setColor(new Color(180,200,230));
            g2.setFont(new Font("Segoe UI",Font.BOLD,12));
            g2.drawString("TIR por corrida  ("+datos.length+" simulaciones) -- verde: TIR>TREMA",mg,mg-8);
        }

        private void sinDatos(Graphics g) {
            g.setColor(new Color(100,120,150));
            g.setFont(new Font("Segoe UI",Font.PLAIN,14));
            g.drawString("Sin datos - ejecute la simulacion",getWidth()/2-130,getHeight()/2);
        }
    }

    static class PanelHist extends JPanel {
        private double[] datos; private double trema;
        PanelHist() { setBackground(new Color(18, 22, 36)); }
        void setDatos(double[] d, double tr) { datos=d; trema=tr; repaint(); }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(datos==null){
                g.setColor(new Color(100,120,150));
                g.setFont(new Font("Segoe UI",Font.PLAIN,14));
                g.drawString("Sin datos - ejecute la simulacion",getWidth()/2-130,getHeight()/2);
                return;
            }
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int bins=25, mg=54, w=getWidth()-mg*2, h=getHeight()-mg*2;
            double mn=datos[0], mx=datos[0];
            for(double v:datos){if(v<mn)mn=v;if(v>mx)mx=v;}
            double rng=mx-mn; if(rng==0)rng=1;
            int[]frec=new int[bins];
            for(double v:datos){int idx=(int)((v-mn)/rng*bins);if(idx>=bins)idx=bins-1;frec[idx]++;}
            int mxF=0; for(int f:frec)if(f>mxF)mxF=f;
            double bw=(double)w/bins;

            // Grid
            g2.setColor(new Color(40,50,70));
            for(int i=0;i<=5;i++) g2.drawLine(mg,mg+h*i/5,mg+w,mg+h*i/5);

            // Barras
            for(int i=0;i<bins;i++){
                double centro=mn+(i+0.5)*rng/bins;
                int bh=mxF==0?0:(int)((double)frec[i]/mxF*h);
                int bx=mg+(int)(i*bw), by=mg+h-bh;
                Color c=centro>trema?new Color(0,190,100):new Color(210,65,45);
                g2.setColor(c); g2.fillRect(bx+1,by,(int)bw-2,bh);
                g2.setColor(c.darker()); g2.drawRect(bx+1,by,(int)bw-2,bh);
            }

            // TREMA
            int xTr=mg+(int)((trema-mn)/rng*w);
            g2.setColor(new Color(255,220,0));
            g2.setStroke(new BasicStroke(2.2f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{7,4},0));
            g2.drawLine(xTr,mg,xTr,mg+h);
            g2.setFont(new Font("Segoe UI",Font.BOLD,10));
            g2.setColor(new Color(255,220,0));
            g2.drawString("TREMA="+(int)trema+"%",xTr+4,mg+14);

            // Ejes
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(160,180,210));
            g2.drawLine(mg,mg,mg,mg+h); g2.drawLine(mg,mg+h,mg+w,mg+h);

            // Etiquetas X
            g2.setFont(new Font("Segoe UI",Font.PLAIN,9));
            for(int i=0;i<=5;i++){
                double val=mn+(mx-mn)*i/5.0;
                int x=mg+w*i/5;
                g2.drawString(String.format("%.1f%%",val),x-12,mg+h+14);
            }
            // Etiquetas Y (frecuencias)
            for(int i=0;i<=5;i++){
                int fv=(int)(mxF*(5-i)/5.0);
                g2.drawString(String.valueOf(fv),2,mg+h*i/5+4);
            }

            // Titulo
            g2.setColor(new Color(180,200,230));
            g2.setFont(new Font("Segoe UI",Font.BOLD,12));
            g2.drawString("Histograma de TIR  (verde = supera TREMA  |  rojo = no supera TREMA)",mg,mg-8);
        }
    }
}