package tss.act1.ui;

import tss.act1.core.JavaRandomSource;
import tss.act1.problems.ProblemOneSimulator;
import tss.act1.services.SimulationService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.GridBagConstraints;
import java.util.List;
import java.util.Locale;

public class ProblemOnePanel extends SimulationTabPanel {
    private final SimulationService service;
    private final JSpinner replicationsSpinner;
    private final JSpinner fromMachinesSpinner;
    private final JSpinner toMachinesSpinner;

    public ProblemOnePanel() {
        this.service = new SimulationService();
        this.replicationsSpinner = styleSpinner(new JSpinner(new SpinnerNumberModel(100, 1, 100_000, 1)), true);
        this.fromMachinesSpinner = styleSpinner(new JSpinner(new SpinnerNumberModel(1, 1, 100, 1)), true);
        this.toMachinesSpinner = styleSpinner(new JSpinner(new SpinnerNumberModel(4, 1, 100, 1)), true);
        installContent(buildForm());
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = createFormPanel();
        GridBagConstraints c = baseConstraints();

        addRow(form, c, 0, new JLabel("Réplicas:"), replicationsSpinner);
        addRow(form, c, 1, new JLabel("m inicial:"), fromMachinesSpinner);
        addRow(form, c, 2, new JLabel("m final:"), toMachinesSpinner);

        JButton runButton = styleButton(new JButton("Ejecutar"));
        runButton.addActionListener(e -> refresh());
        addButtonRow(form, c, 3, runButton);
        return form;
    }

    private void refresh() {
        int replications = ((Number) replicationsSpinner.getValue()).intValue();
        int fromMachines = ((Number) fromMachinesSpinner.getValue()).intValue();
        int toMachines = ((Number) toMachinesSpinner.getValue()).intValue();
        List<ProblemOneSimulator.Result> results = service.runProblemOne(replications, fromMachines, toMachines, new JavaRandomSource());

        String[] labels = new String[results.size()];
        double[] values = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            ProblemOneSimulator.Result result = results.get(i);
            labels[i] = "m=" + result.machinesPerMechanic;
            values[i] = result.avgCost;
        }
        setChart("Costo promedio", labels, values);

        ProblemOneSimulator.Result best = results.stream()
                .min((a, b) -> Double.compare(a.avgCost, b.avgCost))
                .orElse(null);

        if (best != null) {
            setOutput(String.format(Locale.US,
                    "Máquinas y mecánicos%nRéplicas=%d | rango m=%d..%d | mejor m=%d | costo=%.2f",
                    replications,
                    fromMachines,
                    toMachines,
                    best.machinesPerMechanic,
                    best.avgCost));
        } else {
            setOutput(String.format(Locale.US,
                    "Máquinas y mecánicos%nRéplicas=%d | rango m=%d..%d",
                    replications,
                    fromMachines,
                    toMachines));
        }

        int rowCount = 0;
        for (ProblemOneSimulator.Result result : results) {
            rowCount += result.details.length;
        }

        String[] columns = {
                "m",
                "Corrida",
                "Horas inactividad",
                "Costo ociosidad",
                "Salario",
                "Costo total"
        };
        Object[][] rows = new Object[rowCount][6];
        int row = 0;
        for (ProblemOneSimulator.Result result : results) {
            for (ProblemOneSimulator.ReplicationDetail detail : result.details) {
                rows[row][0] = result.machinesPerMechanic;
                rows[row][1] = detail.replication;
                rows[row][2] = String.format(Locale.US, "%.4f", detail.downtimeHours);
                rows[row][3] = String.format(Locale.US, "%.2f", detail.idleCost);
                rows[row][4] = String.format(Locale.US, "%.2f", detail.salaryCost);
                rows[row][5] = String.format(Locale.US, "%.2f", detail.totalCost);
                row++;
            }
        }
        setDetailTable(columns, rows);
    }
}