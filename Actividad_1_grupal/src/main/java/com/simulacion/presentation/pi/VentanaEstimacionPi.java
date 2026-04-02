package com.simulacion.presentation.pi;

import com.simulacion.application.usecases.EstimarPi;
import com.simulacion.domain.model.PuntoSimulado;
import com.simulacion.domain.model.ResultadoEstimacionPi;
import com.simulacion.infrastructure.random.GeneradorAleatorioJava;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Ventana principal para la estimación de π usando Monte Carlo
 * Incluye visualización gráfica de puntos y resultados
 */
public class VentanaEstimacionPi extends JFrame {
    private JTextField txtNumeroPuntos;
    private JTextField txtErrorMaximo;
    private JTextField txtNivelConfianza;
    private JButton btnSimular;
    private JButton btnCalcularCorridas;
    private JTextArea txtResultados;
    private JTable tablaResultados;
    private DefaultTableModel modeloTabla;
    private ChartPanel panelGraficoCirculo;
    private ChartPanel panelGraficoConvergencia;
    
    private final EstimarPi casoDeUso;

    public VentanaEstimacionPi() {
        this.casoDeUso = new EstimarPi(new GeneradorAleatorioJava());
        inicializarComponentes();
        configurarVentana();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));

        // Panel de entrada
        JPanel panelEntrada = crearPanelEntrada();
        add(panelEntrada, BorderLayout.NORTH);

        // Panel de resultados con pestañas
        JTabbedPane pestanas = new JTabbedPane();
        
        // Pestaña de resumen
        JPanel panelResumen = crearPanelResumen();
        pestanas.addTab("Resumen", panelResumen);
        
        // Pestaña de tabla
        JPanel panelTabla = crearPanelTabla();
        pestanas.addTab("Puntos Simulados", panelTabla);
        
        // Pestaña de visualización de círculo
        panelGraficoCirculo = new ChartPanel(null);
        panelGraficoCirculo.setPreferredSize(new Dimension(600, 600));
        pestanas.addTab("Visualización del Círculo", panelGraficoCirculo);
        
        // Pestaña de convergencia
        panelGraficoConvergencia = new ChartPanel(null);
        panelGraficoConvergencia.setPreferredSize(new Dimension(600, 400));
        pestanas.addTab("Convergencia de π", panelGraficoConvergencia);

        add(pestanas, BorderLayout.CENTER);
    }

    private JPanel crearPanelEntrada() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuración de la Estimación de π"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Número de puntos
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Número de Puntos:"), gbc);
        gbc.gridx = 1;
        txtNumeroPuntos = new JTextField("1000", 10);
        panel.add(txtNumeroPuntos, gbc);

        // Botón simular
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        btnSimular = new JButton("Ejecutar Simulación");
        btnSimular.setFont(new Font("Arial", Font.BOLD, 14));
        btnSimular.addActionListener(e -> ejecutarSimulacion());
        panel.add(btnSimular, gbc);

        // Separador
        gbc.gridy = 2;
        panel.add(new JSeparator(), gbc);

        // Sección para calcular corridas necesarias
        gbc.gridy = 3; gbc.gridwidth = 2;
        JLabel lblCalculo = new JLabel("Calcular Corridas Necesarias:");
        lblCalculo.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(lblCalculo, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Error Máximo:"), gbc);
        gbc.gridx = 1;
        txtErrorMaximo = new JTextField("0.1", 10);
        panel.add(txtErrorMaximo, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Nivel de Confianza:"), gbc);
        gbc.gridx = 1;
        txtNivelConfianza = new JTextField("0.95", 10);
        panel.add(txtNivelConfianza, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        btnCalcularCorridas = new JButton("Calcular Corridas Requeridas");
        btnCalcularCorridas.addActionListener(e -> calcularCorridasNecesarias());
        panel.add(btnCalcularCorridas, gbc);

        return panel;
    }

    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel(new BorderLayout());
        txtResultados = new JTextArea(12, 40);
        txtResultados.setEditable(false);
        txtResultados.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(txtResultados);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columnas = {"#", "R₁", "R₂", "Distancia", "¿Dentro?"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tablaResultados = new JTable(modeloTabla);
        tablaResultados.setFont(new Font("Arial", Font.PLAIN, 11));
        tablaResultados.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));
        
        JScrollPane scroll = new JScrollPane(tablaResultados);
        panel.add(scroll, BorderLayout.CENTER);
        
        return panel;
    }

    private void ejecutarSimulacion() {
        try {
            int numeroPuntos = Integer.parseInt(txtNumeroPuntos.getText());
            
            if (numeroPuntos > 50000) {
                int respuesta = JOptionPane.showConfirmDialog(
                    this,
                    "Simular " + numeroPuntos + " puntos puede tomar tiempo.\n¿Desea continuar?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION
                );
                if (respuesta != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            btnSimular.setEnabled(false);
            btnSimular.setText("Simulando...");
            
            SwingWorker<ResultadoEstimacionPi, Void> worker = new SwingWorker<>() {
                @Override
                protected ResultadoEstimacionPi doInBackground() {
                    return casoDeUso.ejecutar(numeroPuntos);
                }

                @Override
                protected void done() {
                    try {
                        ResultadoEstimacionPi resultado = get();
                        mostrarResultados(resultado);
                    } catch (Exception e) {
                        mostrarError("Error ejecutando simulación: " + e.getMessage());
                    } finally {
                        btnSimular.setEnabled(true);
                        btnSimular.setText("Ejecutar Simulación");
                    }
                }
            };
            
            worker.execute();

        } catch (NumberFormatException e) {
            mostrarError("Por favor ingrese un número válido");
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void calcularCorridasNecesarias() {
        try {
            double errorMaximo = Double.parseDouble(txtErrorMaximo.getText());
            double nivelConfianza = Double.parseDouble(txtNivelConfianza.getText());
            
            int corridas = casoDeUso.calcularCorridasNecesarias(errorMaximo, nivelConfianza);
            
            String mensaje = String.format(
                "Para que π̂ difiera de π en menos de %.2f\n" +
                "con un nivel de confianza de %.0f%%,\n" +
                "se necesitan aproximadamente:\n\n" +
                "%,d corridas",
                errorMaximo, nivelConfianza * 100, corridas
            );
            
            JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Corridas Necesarias",
                JOptionPane.INFORMATION_MESSAGE
            );
            
        } catch (NumberFormatException e) {
            mostrarError("Por favor ingrese valores numéricos válidos");
        }
    }

    private void mostrarResultados(ResultadoEstimacionPi resultado) {
        // Mostrar resumen
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("      ESTIMACIÓN DE π - MÉTODO MONTE CARLO\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        sb.append(String.format("Número de puntos simulados: %,d\n\n", resultado.getNumeroPuntos()));
        sb.append("RESULTADOS:\n");
        sb.append(String.format("  Puntos dentro del círculo: %,d\n", resultado.getPuntosDentro()));
        sb.append(String.format("  Puntos fuera del círculo:  %,d\n\n", resultado.getPuntosFuera()));
        sb.append(String.format("  π estimado (π̂):           %.10f\n", resultado.getPiEstimado()));
        sb.append(String.format("  π real:                   %.10f\n", Math.PI));
        sb.append(String.format("  Error absoluto:           %.10f\n", resultado.getError()));
        sb.append(String.format("  Error porcentual:         %.4f%%\n", resultado.getErrorPorcentual()));
        sb.append("\nFórmula: π̂ = 4 × (puntos_dentro / total_puntos)\n");
        sb.append(String.format("         π̂ = 4 × (%d / %d) = %.6f\n", 
            resultado.getPuntosDentro(), resultado.getNumeroPuntos(), resultado.getPiEstimado()));
        sb.append("═══════════════════════════════════════════════════\n");
        
        txtResultados.setText(sb.toString());

        // Llenar tabla (solo primeros 1000 para no saturar)
        modeloTabla.setRowCount(0);
        List<PuntoSimulado> puntos = resultado.getPuntos();
        int maxFilas = Math.min(1000, puntos.size());
        
        for (int i = 0; i < maxFilas; i++) {
            PuntoSimulado punto = puntos.get(i);
            Object[] fila = {
                punto.getNumero(),
                String.format("%.5f", punto.getR1()),
                String.format("%.5f", punto.getR2()),
                String.format("%.5f", punto.getDistancia()),
                punto.isDentroDeLCirculo() ? "Sí" : "No"
            };
            modeloTabla.addRow(fila);
        }
        
        if (puntos.size() > 1000) {
            txtResultados.append(String.format("\nNota: La tabla muestra solo los primeros 1,000 de %,d puntos\n", 
                puntos.size()));
        }

        // Crear gráficos
        actualizarGraficoCirculo(resultado);
        actualizarGraficoConvergencia(resultado);
    }

    private void actualizarGraficoCirculo(ResultadoEstimacionPi resultado) {
        XYSeries seriesDentro = new XYSeries("Dentro del círculo");
        XYSeries seriesFuera = new XYSeries("Fuera del círculo");
        
        // Limitar puntos mostrados para mejorar rendimiento
        List<PuntoSimulado> puntos = resultado.getPuntos();
        int step = Math.max(1, puntos.size() / 2000);
        
        for (int i = 0; i < puntos.size(); i += step) {
            PuntoSimulado punto = puntos.get(i);
            if (punto.isDentroDeLCirculo()) {
                seriesDentro.add(punto.getR1(), punto.getR2());
            } else {
                seriesFuera.add(punto.getR1(), punto.getR2());
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(seriesDentro);
        dataset.addSeries(seriesFuera);

        JFreeChart chart = ChartFactory.createScatterPlot(
            "Puntos Simulados en el Cuadrado Unitario",
            "R₁",
            "R₂",
            dataset
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        renderer.setSeriesPaint(0, new Color(34, 139, 34)); // Verde para dentro
        renderer.setSeriesPaint(1, new Color(220, 20, 60)); // Rojo para fuera
        plot.setRenderer(renderer);

        panelGraficoCirculo.setChart(chart);
    }

    private void actualizarGraficoConvergencia(ResultadoEstimacionPi resultado) {
        XYSeries serieConvergencia = new XYSeries("π estimado");
        XYSeries seriePiReal = new XYSeries("π real");
        
        List<PuntoSimulado> puntos = resultado.getPuntos();
        int puntosDentro = 0;
        
        // Calcular π estimado acumulado cada cierto número de puntos
        int step = Math.max(1, puntos.size() / 100);
        
        for (int i = 0; i < puntos.size(); i++) {
            if (puntos.get(i).isDentroDeLCirculo()) {
                puntosDentro++;
            }
            
            if (i > 0 && (i % step == 0 || i == puntos.size() - 1)) {
                double piEstimado = 4.0 * puntosDentro / (i + 1);
                serieConvergencia.add(i + 1, piEstimado);
                seriePiReal.add(i + 1, Math.PI);
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serieConvergencia);
        dataset.addSeries(seriePiReal);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Convergencia de π̂ hacia π",
            "Número de puntos",
            "Valor de π",
            dataset
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, new Color(30, 144, 255)); // Azul para estimación
        renderer.setSeriesPaint(1, new Color(255, 0, 0));    // Rojo para valor real
        plot.setRenderer(renderer);

        panelGraficoConvergencia.setChart(chart);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void configurarVentana() {
        setTitle("Estimación de π - Método Monte Carlo - Raúl Coss Bu");
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
