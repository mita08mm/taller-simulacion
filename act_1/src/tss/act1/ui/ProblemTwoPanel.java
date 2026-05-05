package tss.act1.ui;

import tss.act1.core.JavaRandomSource;
import tss.act1.problems.ProblemTwoSimulator;
import tss.act1.services.SimulationService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.GridBagConstraints;
import java.util.List;
import java.util.Locale;

public class ProblemTwoPanel extends SimulationTabPanel {
    private final SimulationService service;
    private final JSpinner replicationsSpinner;
    private final JSpinner fromWorkersSpinner;
    private final JSpinner toWorkersSpinner;

    public ProblemTwoPanel() {
        this.service = new SimulationService();
        this.replicationsSpinner = styleSpinner(new JSpinner(new SpinnerNumberModel(100, 1, 100_000, 1)), true);
        this.fromWorkersSpinner = styleSpinner(new JSpinner(new SpinnerNumberModel(3, 1, 100, 1)), true);
        this.toWorkersSpinner = styleSpinner(new JSpinner(new SpinnerNumberModel(6, 1, 100, 1)), true);
        installContent(buildForm());
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = createFormPanel();
        GridBagConstraints c = baseConstraints();

        addRow(form, c, 0, new JLabel("Réplicas:"), replicationsSpinner);
        addRow(form, c, 1, new JLabel("Trabajador inicial:"), fromWorkersSpinner);
        addRow(form, c, 2, new JLabel("Trabajador final:"), toWorkersSpinner);

        JButton runButton = styleButton(new JButton("Ejecutar"));
        runButton.addActionListener(e -> refresh());
        addButtonRow(form, c, 3, runButton);
        return form;
    }

    private void refresh() {
        int replications = ((Number) replicationsSpinner.getValue()).intValue();
        int fromWorkers = ((Number) fromWorkersSpinner.getValue()).intValue();
        int toWorkers = ((Number) toWorkersSpinner.getValue()).intValue();
        List<ProblemTwoSimulator.Result> results = service.runProblemTwo(replications, fromWorkers, toWorkers, new JavaRandomSource());

        String[] labels = new String[results.size()];
        double[] values = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            ProblemTwoSimulator.Result result = results.get(i);
            labels[i] = "w=" + result.workers;
            values[i] = result.avgCost;
        }
        setChart("Costo promedio", labels, values);

        ProblemTwoSimulator.Result best = results.stream()
                .min((a, b) -> Double.compare(a.avgCost, b.avgCost))
                .orElse(null);

        if (best != null) {
            setOutput(String.format(Locale.US,
                    "Camiones y descarga%nRéplicas=%d | rango w=%d..%d | mejor w=%d | costo=%.2f",
                    replications,
                    fromWorkers,
                    toWorkers,
                    best.workers,
                    best.avgCost));
        } else {
            setOutput(String.format(Locale.US,
                    "Camiones y descarga%nRéplicas=%d | rango w=%d..%d",
                    replications,
                    fromWorkers,
                    toWorkers));
        }

        int rowCount = 0;
        for (ProblemTwoSimulator.Result result : results) {
            rowCount += result.details.length;
        }

        String[] columns = {
                "w",
                "Corrida",
                "Horas espera",
                "Costo espera",
                "Salario",
                "Costo total"
        };
        Object[][] rows = new Object[rowCount][6];
        int row = 0;
        for (ProblemTwoSimulator.Result result : results) {
            for (ProblemTwoSimulator.ReplicationDetail detail : result.details) {
                rows[row][0] = result.workers;
                rows[row][1] = detail.replication;
                rows[row][2] = String.format(Locale.US, "%.4f", detail.waitHours);
                rows[row][3] = String.format(Locale.US, "%.2f", detail.waitCost);
                rows[row][4] = String.format(Locale.US, "%.2f", detail.salaryCost);
                rows[row][5] = String.format(Locale.US, "%.2f", detail.totalCost);
                row++;
            }
        }
        setDetailTable(columns, rows);
    }
}