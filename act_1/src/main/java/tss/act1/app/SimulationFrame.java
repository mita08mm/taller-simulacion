package tss.act1.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import tss.act1.problems.ProblemOneResult;
import tss.act1.problems.ProblemTwoResult;

public final class SimulationFrame extends JFrame {
    private static final int PART_ONE_TABLE_LIMIT = 100;

    private final JTextField samplesField = new JTextField();
    private final JTextField p1ReplicationsField = new JTextField();
    private final JTextField p1MachinesMinField = new JTextField();
    private final JTextField p1MachinesMaxField = new JTextField();
    private final JTextField p1IdleCostField = new JTextField();
    private final JTextField p1SalaryField = new JTextField();
    private final JTextField p2ReplicationsField = new JTextField();
    private final JTextArea partOneSummaryArea = new JTextArea();
    private final JTable partOneTable = new JTable();
    private final JTable problemOneTable = new JTable();
    private final JTable problemTwoTable = new JTable();
    private final JTextArea problemOneSummaryArea = new JTextArea();
    private final JTextArea problemTwoSummaryArea = new JTextArea();
    private final SimulationService simulationService = new SimulationService();

    public SimulationFrame() {
        super("Actividad 1 - Simulacion de Sistemas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        SimulationInput defaults = SimulationInput.defaults();

        samplesField.setText(String.valueOf(defaults.partOneSamples()));
        p1ReplicationsField.setText(String.valueOf(defaults.problemOneReplications()));
        p1MachinesMinField.setText(String.valueOf(defaults.problemOneMinMachines()));
        p1MachinesMaxField.setText(String.valueOf(defaults.problemOneMaxMachines()));
        p1IdleCostField.setText(String.valueOf(defaults.problemOneIdleCostPerHour()));
        p1SalaryField.setText(String.valueOf(defaults.problemOneMechanicSalaryPerHour()));
        p2ReplicationsField.setText(String.valueOf(defaults.problemTwoReplications()));

        samplesField.setToolTipText("Cantidad de valores generados en la Parte 1");
        p1ReplicationsField.setToolTipText("Cantidad de corridas por alternativa en Maquinas y mecanicos");
        p1MachinesMinField.setToolTipText("Numero minimo de maquinas por mecanico (m)");
        p1MachinesMaxField.setToolTipText("Numero maximo de maquinas por mecanico (m)");
        p1IdleCostField.setToolTipText("Costo por hora de maquina ociosa");
        p1SalaryField.setToolTipText("Salario por hora del mecanico");
        p2ReplicationsField.setToolTipText("Cantidad de corridas por alternativa en el Problema 2");

        JPanel generalPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        generalPanel.setBorder(BorderFactory.createTitledBorder("General"));
        generalPanel.add(new JLabel("Muestras inversa"));
        generalPanel.add(samplesField);
        generalPanel.add(new JLabel("Corridas camiones"));
        generalPanel.add(p2ReplicationsField);

        JPanel machinesPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        machinesPanel.setBorder(BorderFactory.createTitledBorder("Maquinas y mecanicos"));
        machinesPanel.add(new JLabel("Corridas"));
        machinesPanel.add(p1ReplicationsField);
        machinesPanel.add(new JLabel("Maquinas min (m)"));
        machinesPanel.add(p1MachinesMinField);
        machinesPanel.add(new JLabel("Maquinas max (m)"));
        machinesPanel.add(p1MachinesMaxField);
        machinesPanel.add(new JLabel("Costo ociosa/h"));
        machinesPanel.add(p1IdleCostField);
        machinesPanel.add(new JLabel("Salario mecanico/h"));
        machinesPanel.add(p1SalaryField);

        JPanel formPanel = new JPanel(new BorderLayout(8, 8));
        formPanel.add(generalPanel, BorderLayout.NORTH);
        formPanel.add(machinesPanel, BorderLayout.CENTER);

        JLabel noteLabel = new JLabel("Parte 1: transformada inversa. Parte 2: maquinas y camiones.");

        JButton runButton = new JButton("Ejecutar simulacion");
        runButton.addActionListener(e -> runSimulation(defaults));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(noteLabel, BorderLayout.SOUTH);

        JTabbedPane tabbedPane = buildTabbedResults();

        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));
        headerPanel.add(topPanel, BorderLayout.CENTER);
        headerPanel.add(runButton, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        setPreferredSize(new Dimension(980, 780));
        pack();
        setLocationRelativeTo(null);

        SwingUtilities.invokeLater(() -> runSimulation(defaults));
    }

    private void runSimulation(SimulationInput defaults) {
        try {
            SimulationInput input = new SimulationInput(
                    Integer.parseInt(samplesField.getText().trim()),
                    Integer.parseInt(p1MachinesMinField.getText().trim()),
                    Integer.parseInt(p1MachinesMaxField.getText().trim()),
                    Integer.parseInt(p1ReplicationsField.getText().trim()),
                    defaults.problemOneHorizonHours(),
                    Double.parseDouble(p1IdleCostField.getText().trim()),
                    Double.parseDouble(p1SalaryField.getText().trim()),
                    defaults.problemTwoMinTeam(),
                    defaults.problemTwoMaxTeam(),
                    Integer.parseInt(p2ReplicationsField.getText().trim()),
                    defaults.problemTwoShiftHours(),
                    defaults.problemTwoWorkerSalaryPerHour(),
                    defaults.problemTwoWaitingCostPerHour());

                    SimulationDetailedResult result = simulationService.runDetailed(input);
                    fillPartOne(result.partOne());
                    fillProblemOne(result.problemOneResults(), result.bestProblemOne());
                    fillProblemTwo(result.problemTwoResults(), result.bestProblemTwo());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Verifica que todos los parametros sean numericos.",
                    "Entrada invalida",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error de validacion",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JTabbedPane buildTabbedResults() {
        JTabbedPane tabs = new JTabbedPane();

        partOneSummaryArea.setEditable(false);
        partOneSummaryArea.setLineWrap(true);
        partOneSummaryArea.setWrapStyleWord(true);

        problemOneSummaryArea.setEditable(false);
        problemOneSummaryArea.setLineWrap(true);
        problemOneSummaryArea.setWrapStyleWord(true);

        problemTwoSummaryArea.setEditable(false);
        problemTwoSummaryArea.setLineWrap(true);
        problemTwoSummaryArea.setWrapStyleWord(true);

        JPanel partOnePanel = new JPanel(new BorderLayout(8, 8));
        partOnePanel.add(new JScrollPane(partOneSummaryArea), BorderLayout.NORTH);
        partOnePanel.add(new JScrollPane(partOneTable), BorderLayout.CENTER);

        JPanel problemOnePanel = new JPanel(new BorderLayout(8, 8));
        problemOnePanel.add(new JScrollPane(problemOneSummaryArea), BorderLayout.NORTH);
        problemOnePanel.add(new JScrollPane(problemOneTable), BorderLayout.CENTER);

        JPanel problemTwoPanel = new JPanel(new BorderLayout(8, 8));
        problemTwoPanel.add(new JScrollPane(problemTwoSummaryArea), BorderLayout.NORTH);
        problemTwoPanel.add(new JScrollPane(problemTwoTable), BorderLayout.CENTER);

        tabs.addTab("Inversa", partOnePanel);
        tabs.addTab("Maquinas", problemOnePanel);
        tabs.addTab("Camiones", problemTwoPanel);

        return tabs;
    }

    private void fillPartOne(PartOneResult result) {
        partOneSummaryArea.setText(String.format(Locale.US,
            "Cada fila representa una muestra generada con F^-1(u)\nMuestras: %d\nMedia: %.5f\nMinimo: %.5f\nMaximo: %.5f",
                result.samples(),
                result.mean(),
                result.min(),
                result.max()));

        DefaultTableModel model = new DefaultTableModel(new Object[] { "#", "u", "x = F^-1(u)" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int rowsToShow = Math.min(result.samplePairs().size(), PART_ONE_TABLE_LIMIT);
        int index = 1;
        for (int i = 0; i < rowsToShow; i++) {
            InverseSample sample = result.samplePairs().get(i);
            model.addRow(new Object[] {
                    index++,
                    String.format(Locale.US, "%.5f", sample.u()),
                    String.format(Locale.US, "%.5f", sample.x())
            });
        }
        if (result.samplePairs().size() > rowsToShow) {
            partOneSummaryArea.append(String.format(Locale.US,
                    "\nMostrando solo las primeras %d filas por tamano del resultado.",
                    rowsToShow));
        }
        partOneTable.setModel(model);
    }

    private void fillProblemOne(java.util.List<ProblemOneResult> results, ProblemOneResult best) {
        problemOneSummaryArea.setText(String.format(Locale.US,
                "Mejor m: %d | Costo total: %.5f | Ociosidad: %.5f | Salario/m: %.5f",
                best.machinesPerMechanic(),
                best.totalCostPerMachineHour(),
                best.idleCostPerMachineHour(),
                best.mechanicSalaryPerMachineHour()));

        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "m", "Costo total", "Costo ocio", "Salario/m", "Downtime promedio" }, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

        for (ProblemOneResult r : results) {
            model.addRow(new Object[] {
                    r.machinesPerMechanic(),
                    String.format(Locale.US, "%.5f", r.totalCostPerMachineHour()),
                    String.format(Locale.US, "%.5f", r.idleCostPerMachineHour()),
                    String.format(Locale.US, "%.5f", r.mechanicSalaryPerMachineHour()),
                    String.format(Locale.US, "%.5f", r.averageDowntimeHoursPerMachine())
            });
        }
        problemOneTable.setModel(model);
    }

    private void fillProblemTwo(java.util.List<ProblemTwoResult> results, ProblemTwoResult best) {
        problemTwoSummaryArea.setText(String.format(Locale.US,
                "Mejor n: %d | Costo total turno: %.5f | Espera: %.5f | Mano de obra: %.5f",
                best.teamSize(),
                best.totalCostPerShift(),
                best.waitingCostPerShift(),
                best.laborCostPerShift()));

        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "n", "Costo total turno", "Costo espera", "Costo mano obra", "Espera promedio" }, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

        for (ProblemTwoResult r : results) {
            model.addRow(new Object[] {
                    r.teamSize(),
                    String.format(Locale.US, "%.5f", r.totalCostPerShift()),
                    String.format(Locale.US, "%.5f", r.waitingCostPerShift()),
                    String.format(Locale.US, "%.5f", r.laborCostPerShift()),
                    String.format(Locale.US, "%.5f", r.averageTruckWaitingHours())
            });
        }
        problemTwoTable.setModel(model);
    }
}
