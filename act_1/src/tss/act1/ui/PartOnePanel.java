package tss.act1.ui;

import tss.act1.core.JavaRandomSource;
import tss.act1.services.SimulationService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.GridBagConstraints;
import java.util.Locale;

public class PartOnePanel extends SimulationTabPanel {
    private final SimulationService service;
    private final JSpinner sampleSizeSpinner;

    public PartOnePanel() {
        this.service = new SimulationService();
        this.sampleSizeSpinner = styleSpinner(new JSpinner(new SpinnerNumberModel(100, 1, 1_000_000, 1)), true);
        installContent(buildForm());
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = createFormPanel();
        GridBagConstraints c = baseConstraints();

        addRow(form, c, 0, new JLabel("Tamaño de muestra (n):"), sampleSizeSpinner);

        JButton runButton = styleButton(new JButton("Ejecutar"));
        runButton.addActionListener(e -> refresh());
        addButtonRow(form, c, 1, runButton);
        return form;
    }

    private void refresh() {
        int sampleSize = ((Number) sampleSizeSpinner.getValue()).intValue();
        var result = service.runPartOne(sampleSize, new JavaRandomSource());
        setChart("Resumen", new String[]{"Min", "Media", "Max"}, new double[]{result.min, result.mean, result.max});

        setOutput(String.format(Locale.US,
                "Transformada inversa%n" +
                "n=%d | media=%.4f | std=%.4f | min=%.4f | max=%.4f | válidos=%d/%d",
                result.n,
                result.mean,
                result.std,
                result.min,
                result.max,
                result.validCount,
                result.n));

        String[] columns = {"Iteración", "x", "Media acumulada", "Válidos acumulados"};
        Object[][] rows = new Object[result.details.length][4];
        for (int i = 0; i < result.details.length; i++) {
            var d = result.details[i];
            rows[i][0] = d.iteration;
            rows[i][1] = String.format(Locale.US, "%.6f", d.x);
            rows[i][2] = String.format(Locale.US, "%.6f", d.runningMean);
            rows[i][3] = d.validCountSoFar;
        }
        setDetailTable(columns, rows);
    }
}