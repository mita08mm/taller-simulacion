package com.simulacion.presentation.inversion;

import com.simulacion.application.usecases.SimularProyectoInversion;
import com.simulacion.domain.model.ConfiguracionInversion;
import com.simulacion.domain.model.ResultadoCorridaInversion;
import com.simulacion.domain.model.ResultadoSimulacionInversion;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de presentacion para la simulacion de inversion.
 * Solo gestiona entrada/salida de UI; la logica vive en application/domain.
 */
public class VentanaSimulacionInversion extends JFrame {
    private JTextField txtNumCorridas;
    private JTextField txtAFPes;
    private JTextField txtAFProb;
    private JTextField txtAFOpt;
    private JTextField txtACPes;
    private JTextField txtACProb;
    private JTextField txtACOpt;
    private JTextField txtFlujoPes;
    private JTextField txtFlujoProb;
    private JTextField txtFlujoOpt;
    private JTextField txtImpuestos;
    private JTextField txtTREMA;
    private JTextField txtVida;

    private JButton btnSimular;
    private JTextArea txtResumen;
    private DefaultTableModel modeloTabla;
    private DefaultTableModel modeloTablaDistribucion;
    private JLabel lblDecision;
    private ChartPanel panelHistograma;
    private ChartPanel panelAcumulada;

    private final SimularProyectoInversion casoDeUso;

    public VentanaSimulacionInversion() {
        this.casoDeUso = new SimularProyectoInversion();
        inicializarComponentes();
        configurarVentana();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));

        JPanel panelEntrada = crearPanelEntrada();
        add(panelEntrada, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Resumen", crearPanelResumen());
        tabs.addTab("Detalle de Corridas", crearPanelTabla());
        tabs.addTab("Tabla 5.6", crearPanelTablaDistribucion());
        panelHistograma = new ChartPanel(null);
        tabs.addTab("Histograma TIR", panelHistograma);
        panelAcumulada = new ChartPanel(null);
        tabs.addTab("Figura 5.1 Acumulada", panelAcumulada);
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel crearPanelEntrada() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Proyecto de Inversion (Monte Carlo)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtNumCorridas = new JTextField("1000", 10);
        txtAFPes = new JTextField("-100000", 10);
        txtAFProb = new JTextField("-70000", 10);
        txtAFOpt = new JTextField("-60000", 10);
        txtACPes = new JTextField("-40000", 10);
        txtACProb = new JTextField("-30000", 10);
        txtACOpt = new JTextField("-25000", 10);
        txtFlujoPes = new JTextField("30000", 10);
        txtFlujoProb = new JTextField("40000", 10);
        txtFlujoOpt = new JTextField("45000", 10);
        txtImpuestos = new JTextField("50", 10);
        txtTREMA = new JTextField("15", 10);
        txtVida = new JTextField("5", 10);

        int y = 0;
        agregarCampo(panel, gbc, y++, "Num. corridas:", txtNumCorridas);
        agregarCampo(panel, gbc, y++, "AF pes/prob/opt:", crearFila(txtAFPes, txtAFProb, txtAFOpt));
        agregarCampo(panel, gbc, y++, "AC pes/prob/opt:", crearFila(txtACPes, txtACProb, txtACOpt));
        agregarCampo(panel, gbc, y++, "Flujo pes/prob/opt:", crearFila(txtFlujoPes, txtFlujoProb, txtFlujoOpt));
        agregarCampo(panel, gbc, y++, "Impuestos (%):", txtImpuestos);
        agregarCampo(panel, gbc, y++, "TREMA (%):", txtTREMA);
        agregarCampo(panel, gbc, y++, "Vida fiscal (anios):", txtVida);

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 2;
        btnSimular = new JButton("Ejecutar Simulacion");
        btnSimular.addActionListener(e -> ejecutarSimulacion());
        panel.add(btnSimular, gbc);

        gbc.gridy = y + 1;
        lblDecision = new JLabel("Decision: --", SwingConstants.CENTER);
        lblDecision.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(lblDecision, gbc);

        return panel;
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, int y, String etiqueta, JComponent campo) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = y;
        panel.add(new JLabel(etiqueta), gbc);

        gbc.gridx = 1;
        panel.add(campo, gbc);
    }

    private JPanel crearFila(JTextField a, JTextField b, JTextField c) {
        JPanel fila = new JPanel(new GridLayout(1, 3, 5, 0));
        fila.add(a);
        fila.add(b);
        fila.add(c);
        return fila;
    }

    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel(new BorderLayout());
        txtResumen = new JTextArea(14, 40);
        txtResumen.setEditable(false);
        txtResumen.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtResumen.setText(
            "Configure parametros y ejecute la simulacion.\n\n" +
            "La logica de negocio esta en:\n" +
            "- application/usecases/SimularProyectoInversion\n" +
            "- domain/model/ConfiguracionInversion\n" +
            "- domain/model/ResultadoSimulacionInversion\n\n" +
            "Criterio de decision: ACEPTAR si Prob(TIR > TREMA) >= 0.90"
        );
        panel.add(new JScrollPane(txtResumen), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columnas = {"#", "AF", "AC", "Flujo", "VPN", "TIR (%)", "TIR>TREMA", "Decision"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modeloTabla);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelTablaDistribucion() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columnas = {
            "Limite inferior TIR (%)",
            "Limite superior TIR (%)",
            "Fraccion",
            "Fraccion acumulada"
        };
        modeloTablaDistribucion = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modeloTablaDistribucion);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    private void ejecutarSimulacion() {
        ConfiguracionInversion config = leerConfiguracion();

        btnSimular.setEnabled(false);
        btnSimular.setText("Simulando...");

        SwingWorker<ResultadoSimulacionInversion, Void> worker = new SwingWorker<>() {
            @Override
            protected ResultadoSimulacionInversion doInBackground() {
                return casoDeUso.ejecutar(config);
            }

            @Override
            protected void done() {
                try {
                    ResultadoSimulacionInversion resultado = get();
                    mostrarResultados(resultado);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        VentanaSimulacionInversion.this,
                        "Error ejecutando simulacion: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    btnSimular.setEnabled(true);
                    btnSimular.setText("Ejecutar Simulacion");
                }
            }
        };

        worker.execute();
    }

    private ConfiguracionInversion leerConfiguracion() {
        return new ConfiguracionInversion(
            entInt(txtNumCorridas, 1000),
            entInt(txtVida, 5),
            entDbl(txtAFPes, -100000),
            entDbl(txtAFProb, -70000),
            entDbl(txtAFOpt, -60000),
            entDbl(txtACPes, -40000),
            entDbl(txtACProb, -30000),
            entDbl(txtACOpt, -25000),
            entDbl(txtFlujoPes, 30000),
            entDbl(txtFlujoProb, 40000),
            entDbl(txtFlujoOpt, 45000),
            entDbl(txtImpuestos, 50) / 100.0,
            entDbl(txtTREMA, 15)
        );
    }

    private void mostrarResultados(ResultadoSimulacionInversion resultado) {
        modeloTabla.setRowCount(0);
        for (ResultadoCorridaInversion corrida : resultado.getCorridas()) {
            modeloTabla.addRow(new Object[]{
                corrida.getNumeroCorrida(),
                String.format("%,.0f", corrida.getActivoFijo()),
                String.format("%,.0f", corrida.getActivoCirculante()),
                String.format("%,.0f", corrida.getFlujoAntesImpuestos()),
                String.format("%,.2f", corrida.getVpn()),
                String.format("%.2f", corrida.getTirPorcentaje()),
                corrida.isSuperaTrema() ? "Si" : "No",
                corrida.isSuperaTrema() ? "ACEPTAR" : "RECHAZAR"
            });
        }

        String resumen = String.format(
            "Corridas: %,d\n" +
            "TIR min: %.2f %%\n" +
            "TIR max: %.2f %%\n" +
            "TIR media: %.2f %%\n" +
            "Desv. estandar: %.2f %%\n" +
            "TREMA: %.2f %%\n" +
            "Corridas con TIR > TREMA: %,d\n" +
            "Prob(TIR > TREMA): %.4f (%.2f %%)\n\n" +
            "Percentiles:\n" +
            "P10: %.2f %%\n" +
            "P50: %.2f %%\n" +
            "P90: %.2f %%\n\n" +
            "Regla: ACEPTAR si Prob(TIR > TREMA) >= 0.90\n" +
            "(Se genero Tabla 5.6 e histogramas para sustento en presentacion)",
            resultado.getTotalCorridas(),
            resultado.getTirMinima(),
            resultado.getTirMaxima(),
            resultado.getTirMedia(),
            resultado.getDesviacionEstandar(),
            resultado.getConfiguracion().getTrema(),
            resultado.getCorridasExitosas(),
            resultado.getProbabilidadExito(),
            resultado.getProbabilidadExito() * 100,
            resultado.getPercentil(0.10),
            resultado.getPercentil(0.50),
            resultado.getPercentil(0.90)
        );

        txtResumen.setText(resumen);

        if (resultado.aceptarProyecto()) {
            lblDecision.setText("Decision: ACEPTAR (Prob >= 0.90)");
            lblDecision.setForeground(new Color(0, 128, 0));
        } else {
            lblDecision.setText("Decision: RECHAZAR (Prob < 0.90)");
            lblDecision.setForeground(new Color(180, 30, 30));
        }

        actualizarTablaYFiguras(resultado);
    }

    private void actualizarTablaYFiguras(ResultadoSimulacionInversion resultado) {
        List<IntervaloDistribucion> intervalos = construirDistribucion(resultado, 20);

        modeloTablaDistribucion.setRowCount(0);
        for (IntervaloDistribucion intervalo : intervalos) {
            modeloTablaDistribucion.addRow(new Object[]{
                String.format("%.2f", intervalo.limiteInferior),
                String.format("%.2f", intervalo.limiteSuperior),
                String.format("%.3f", intervalo.fraccion),
                String.format("%.3f", intervalo.fraccionAcumulada)
            });
        }

        actualizarHistograma(intervalos, resultado.getConfiguracion().getTrema());
        actualizarFiguraAcumulada(intervalos, resultado.getConfiguracion().getTrema());
    }

    private List<IntervaloDistribucion> construirDistribucion(ResultadoSimulacionInversion resultado, int numeroIntervalos) {
        List<Double> tires = resultado.getCorridas().stream()
            .map(ResultadoCorridaInversion::getTirPorcentaje)
            .toList();

        double minimo = resultado.getTirMinima();
        double maximo = resultado.getTirMaxima();
        double rango = maximo - minimo;
        if (rango == 0) {
            rango = 1;
        }

        int[] frecuencias = new int[numeroIntervalos];
        for (double tir : tires) {
            int idx = (int) ((tir - minimo) / rango * numeroIntervalos);
            if (idx >= numeroIntervalos) {
                idx = numeroIntervalos - 1;
            }
            if (idx < 0) {
                idx = 0;
            }
            frecuencias[idx]++;
        }

        List<IntervaloDistribucion> intervalos = new ArrayList<>();
        int acumulada = 0;
        for (int i = 0; i < numeroIntervalos; i++) {
            double li = minimo + i * rango / numeroIntervalos;
            double ls = minimo + (i + 1) * rango / numeroIntervalos;
            double fraccion = (double) frecuencias[i] / tires.size();
            acumulada += frecuencias[i];
            double fraccionAcumulada = (double) acumulada / tires.size();
            intervalos.add(new IntervaloDistribucion(li, ls, fraccion, fraccionAcumulada));
        }

        return intervalos;
    }

    private void actualizarHistograma(List<IntervaloDistribucion> intervalos, double trema) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (IntervaloDistribucion intervalo : intervalos) {
            String etiqueta = String.format("%.2f-%.2f", intervalo.limiteInferior, intervalo.limiteSuperior);
            dataset.addValue(intervalo.fraccion, "Fraccion", etiqueta);
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Histograma de TIR (Tabla 5.6)",
            "Intervalos de TIR (%)",
            "Fraccion",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        );

        chart.addSubtitle(new org.jfree.chart.title.TextTitle(
            String.format("TREMA = %.2f%%", trema)
        ));

        panelHistograma.setChart(chart);
    }

    private void actualizarFiguraAcumulada(List<IntervaloDistribucion> intervalos, double trema) {
        XYSeries serieAcumulada = new XYSeries("Fraccion acumulada");
        for (IntervaloDistribucion intervalo : intervalos) {
            serieAcumulada.add(intervalo.limiteSuperior, intervalo.fraccionAcumulada);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serieAcumulada);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Distribucion acumulada de la TIR (Figura 5.1)",
            "TIR (%)",
            "Fraccion acumulada",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        plot.setRenderer(renderer);

        ValueMarker marker = new ValueMarker(trema);
        marker.setPaint(Color.RED);
        marker.setStroke(new BasicStroke(1.8f));
        plot.addDomainMarker(marker);

        panelAcumulada.setChart(chart);
    }

    private static class IntervaloDistribucion {
        private final double limiteInferior;
        private final double limiteSuperior;
        private final double fraccion;
        private final double fraccionAcumulada;

        private IntervaloDistribucion(double limiteInferior, double limiteSuperior, double fraccion, double fraccionAcumulada) {
            this.limiteInferior = limiteInferior;
            this.limiteSuperior = limiteSuperior;
            this.fraccion = fraccion;
            this.fraccionAcumulada = fraccionAcumulada;
        }
    }

    private double entDbl(JTextField tf, double def) {
        try {
            return Double.parseDouble(tf.getText().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private int entInt(JTextField tf, int def) {
        try {
            return Integer.parseInt(tf.getText().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private void configurarVentana() {
        setTitle("Simulacion de Proyecto de Inversion");
        setSize(1100, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
