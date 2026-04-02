package com.simulacion.presentation.apuestas;

import com.simulacion.application.usecases.SimularJuegoApuestas;
import com.simulacion.domain.model.*;
import com.simulacion.infrastructure.random.GeneradorAleatorioJava;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Ventana principal para la simulación del juego de apuestas (volados)
 * Aplica principio de separación de concerns: UI separada de lógica de negocio
 */
public class VentanaSimulacionApuestas extends JFrame {
    private JTextField txtCantidadInicial;
    private JTextField txtApuestaInicial;
    private JTextField txtMeta;
    private JTextField txtProbabilidadGanar;
    private JTextField txtNumeroCorridas;
    private JButton btnSimular;
    private JTextArea txtResultados;
    private JTable tablaResultados;
    private DefaultTableModel modeloTabla;
    private ChartPanel panelGrafico;
    
    private final SimularJuegoApuestas casoDeUso;

    public VentanaSimulacionApuestas() {
        this.casoDeUso = new SimularJuegoApuestas(new GeneradorAleatorioJava());
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
        pestanas.addTab("Detalle de Corridas", panelTabla);
        
        // Pestaña de gráfico
        panelGrafico = new ChartPanel(null);
        panelGrafico.setPreferredSize(new Dimension(600, 400));
        pestanas.addTab("Gráfico", panelGrafico);

        add(pestanas, BorderLayout.CENTER);
    }

    private JPanel crearPanelEntrada() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuración de la Simulación"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Cantidad inicial
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Cantidad Inicial ($):"), gbc);
        gbc.gridx = 1;
        txtCantidadInicial = new JTextField("30", 10);
        panel.add(txtCantidadInicial, gbc);

        // Apuesta inicial
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Apuesta Inicial ($):"), gbc);
        gbc.gridx = 1;
        txtApuestaInicial = new JTextField("10", 10);
        panel.add(txtApuestaInicial, gbc);

        // Meta
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Meta ($):"), gbc);
        gbc.gridx = 1;
        txtMeta = new JTextField("50", 10);
        panel.add(txtMeta, gbc);

        // Probabilidad de ganar
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Probabilidad de Ganar:"), gbc);
        gbc.gridx = 1;
        txtProbabilidadGanar = new JTextField("0.5", 10);
        panel.add(txtProbabilidadGanar, gbc);

        // Número de corridas
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Número de Corridas:"), gbc);
        gbc.gridx = 1;
        txtNumeroCorridas = new JTextField("10", 10);
        panel.add(txtNumeroCorridas, gbc);

        // Botón simular
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        btnSimular = new JButton("Ejecutar Simulación");
        btnSimular.setFont(new Font("Arial", Font.BOLD, 14));
        btnSimular.addActionListener(e -> ejecutarSimulacion());
        panel.add(btnSimular, gbc);

        return panel;
    }

    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel(new BorderLayout());
        txtResultados = new JTextArea(10, 40);
        txtResultados.setEditable(false);
        txtResultados.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(txtResultados);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columnas = {"Corrida", "# Volados", "Estado Final"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tablaResultados = new JTable(modeloTabla);
        tablaResultados.setFont(new Font("Arial", Font.PLAIN, 12));
        tablaResultados.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        JScrollPane scroll = new JScrollPane(tablaResultados);
        panel.add(scroll, BorderLayout.CENTER);
        
        return panel;
    }

    private void ejecutarSimulacion() {
        try {
            // Validar y obtener parámetros
            int cantidadInicial = Integer.parseInt(txtCantidadInicial.getText());
            int apuestaInicial = Integer.parseInt(txtApuestaInicial.getText());
            int meta = Integer.parseInt(txtMeta.getText());
            double probabilidadGanar = Double.parseDouble(txtProbabilidadGanar.getText());
            int numeroCorridas = Integer.parseInt(txtNumeroCorridas.getText());

            // Crear configuración
            ConfiguracionApuestas config = new ConfiguracionApuestas(
                cantidadInicial, apuestaInicial, meta, probabilidadGanar, numeroCorridas
            );

            // Ejecutar simulación
            btnSimular.setEnabled(false);
            btnSimular.setText("Simulando...");
            
            SwingWorker<ResultadoSimulacionApuestas, Void> worker = new SwingWorker<>() {
                @Override
                protected ResultadoSimulacionApuestas doInBackground() {
                    return casoDeUso.ejecutar(config);
                }

                @Override
                protected void done() {
                    try {
                        ResultadoSimulacionApuestas resultado = get();
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
            mostrarError("Por favor ingrese valores numéricos válidos");
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarResultados(ResultadoSimulacionApuestas resultado) {
        // Mostrar resumen
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("        RESULTADOS DE LA SIMULACIÓN\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        sb.append("CONFIGURACIÓN:\n");
        sb.append(String.format("  Cantidad Inicial: $%d\n", resultado.getConfiguracion().getCantidadInicial()));
        sb.append(String.format("  Apuesta Inicial:  $%d\n", resultado.getConfiguracion().getApuestaInicial()));
        sb.append(String.format("  Meta:             $%d\n", resultado.getConfiguracion().getMeta()));
        sb.append(String.format("  Prob. de Ganar:   %.2f\n", resultado.getConfiguracion().getProbabilidadGanar()));
        sb.append(String.format("  Num. Corridas:    %d\n\n", resultado.getTotalCorridas()));
        
        sb.append("RESULTADOS:\n");
        sb.append(String.format("  Metas Alcanzadas: %d (%.1f%%)\n", 
            resultado.getMetasAlcanzadas(),
            resultado.getProbabilidadExito() * 100));
        sb.append(String.format("  Quiebras:         %d (%.1f%%)\n", 
            resultado.getQuiebras(),
            (1 - resultado.getProbabilidadExito()) * 100));
        sb.append(String.format("\n  Probabilidad de Éxito: %.4f\n", resultado.getProbabilidadExito()));
        sb.append("═══════════════════════════════════════════════════\n");
        
        txtResultados.setText(sb.toString());

        // Llenar tabla
        modeloTabla.setRowCount(0);
        for (ResultadoCorrida corrida : resultado.getCorridas()) {
            Object[] fila = {
                corrida.getNumeroCorrida(),
                corrida.getCantidadVolados(),
                corrida.getEstadoFinal() == ResultadoCorrida.EstadoFinal.ALCANZA_META ? "Meta" : "Quiebra"
            };
            modeloTabla.addRow(fila);
        }

        // Crear gráfico
        actualizarGrafico(resultado);
    }

    private void actualizarGrafico(ResultadoSimulacionApuestas resultado) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(resultado.getMetasAlcanzadas(), "Cantidad", "Metas Alcanzadas");
        dataset.addValue(resultado.getQuiebras(), "Cantidad", "Quiebras");

        JFreeChart chart = ChartFactory.createBarChart(
            "Resultados de la Simulación",
            "Resultado",
            "Cantidad de Corridas",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        );

        panelGrafico.setChart(chart);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void configurarVentana() {
        setTitle("Simulación del Juego de Volados - Raúl Coss Bu");
        setSize(900, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
