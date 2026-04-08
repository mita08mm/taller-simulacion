package com.simulacion.presentation;

import com.simulacion.presentation.apuestas.VentanaSimulacionApuestas;
import com.simulacion.presentation.inversion.VentanaSimulacionInversion;
import com.simulacion.presentation.pi.VentanaEstimacionPi;

import javax.swing.*;
import java.awt.*;

public class MainApplication extends JFrame {
    
    public MainApplication() {
        configurarVentana();
        inicializarComponentes();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(20, 20));
        
        JPanel panelTitulo = createPanelTitulo();
        add(panelTitulo, BorderLayout.NORTH);
        
        JPanel panelBotones = createPanelBotones();
        add(panelBotones, BorderLayout.CENTER);
    }

    private JPanel createPanelTitulo() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        
        JLabel lblTitulo = new JLabel("Simulación Monte Carlo", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 28));
        
        panel.add(lblTitulo);
        return panel;
    }

    private JPanel createPanelBotones() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 20, 15, 20);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        JButton btnApuestas = crearBoton("Juego de Volados");
        btnApuestas.addActionListener(e -> abrirVentanaApuestas());
        panel.add(btnApuestas, gbc);
        
        gbc.gridy = 1;
        JButton btnPi = crearBoton("Estimación de π");
        btnPi.addActionListener(e -> abrirVentanaEstimacionPi());
        panel.add(btnPi, gbc);

        gbc.gridy = 2;
        JButton btnInversion = crearBoton("Proyecto de Inversión");
        btnInversion.addActionListener(e -> abrirVentanaInversion());
        panel.add(btnInversion, gbc);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(30, 20, 15, 20);
        JButton btnSalir = crearBoton("Salir");
        btnSalir.addActionListener(e -> System.exit(0));
        panel.add(btnSalir, gbc);
        
        return panel;
    }

    private JButton crearBoton(String titulo) {
        JButton boton = new JButton(titulo);
        boton.setFont(new Font("Arial", Font.BOLD, 16));
        boton.setPreferredSize(new Dimension(350, 60));
        boton.setFocusPainted(false);
        return boton;
    }

    private void abrirVentanaApuestas() {
        SwingUtilities.invokeLater(() -> {
            VentanaSimulacionApuestas ventana = new VentanaSimulacionApuestas();
            ventana.setVisible(true);
        });
    }

    private void abrirVentanaEstimacionPi() {
        SwingUtilities.invokeLater(() -> {
            VentanaEstimacionPi ventana = new VentanaEstimacionPi();
            ventana.setVisible(true);
        });
    }

    private void abrirVentanaInversion() {
        SwingUtilities.invokeLater(() -> {
            VentanaSimulacionInversion ventana = new VentanaSimulacionInversion();
            ventana.setVisible(true);
        });
    }

    private void configurarVentana() {
        setTitle("Simulación Monte Carlo");
        setSize(560, 440);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainApplication app = new MainApplication();
            app.setVisible(true);
        });
    }
}
