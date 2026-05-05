package tss.act1.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

abstract class SimulationTabPanel extends JPanel {
    protected static final Insets FORM_INSETS = new Insets(7, 7, 7, 7);
    protected static final Dimension FIELD_SIZE = new Dimension(160, 26);
    protected static final Dimension BUTTON_SIZE = new Dimension(145, 28);

    protected final JTextArea outputArea;
    protected final JTable detailTable;
    protected final DefaultTableModel detailTableModel;

    protected SimulationTabPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(new Color(245, 246, 248));
        outputArea = createOutputArea();
        detailTableModel = new DefaultTableModel();
        detailTable = createDetailTable();
    }

    protected JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);
        return panel;
    }

    protected GridBagConstraints baseConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = FORM_INSETS;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        return constraints;
    }

    protected void addRow(JPanel panel, GridBagConstraints base, int row, JComponent label, JComponent field) {
        GridBagConstraints labelConstraints = (GridBagConstraints) base.clone();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.weightx = 0.0;
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = (GridBagConstraints) base.clone();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        panel.add(field, fieldConstraints);
    }

    protected void addButtonRow(JPanel panel, GridBagConstraints base, int row, JComponent button) {
        GridBagConstraints buttonConstraints = (GridBagConstraints) base.clone();
        buttonConstraints.gridx = 0;
        buttonConstraints.gridy = row;
        buttonConstraints.gridwidth = 2;
        buttonConstraints.fill = GridBagConstraints.NONE;
        panel.add(button, buttonConstraints);
    }

    protected JSpinner styleSpinner(JSpinner spinner, boolean editable) {
        spinner.setPreferredSize(FIELD_SIZE);
        spinner.setMinimumSize(FIELD_SIZE);
        spinner.setMaximumSize(FIELD_SIZE);
        spinner.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        spinner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(202, 207, 213), 1, true),
                new EmptyBorder(2, 8, 2, 8)));

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setHorizontalAlignment(JFormattedTextField.RIGHT);
            textField.setBorder(BorderFactory.createEmptyBorder());
            textField.setEditable(editable);
            textField.setBackground(editable ? Color.WHITE : new Color(248, 249, 251));
        }

        hideSpinnerArrowButtons(spinner);
        return spinner;
    }

    private void hideSpinnerArrowButtons(JSpinner spinner) {
        for (Component component : spinner.getComponents()) {
            if (!(component instanceof JSpinner.DefaultEditor)) {
                component.setVisible(false);
                component.setPreferredSize(new Dimension(0, 0));
                component.setMinimumSize(new Dimension(0, 0));
                component.setMaximumSize(new Dimension(0, 0));
            }
        }
    }

    protected JButton styleButton(JButton button) {
        button.setPreferredSize(BUTTON_SIZE);
        button.setMinimumSize(BUTTON_SIZE);
        button.setMaximumSize(BUTTON_SIZE);
        button.setFocusPainted(false);
        button.setMargin(new Insets(5, 12, 5, 12));
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        button.setBackground(new Color(237, 239, 242));
        button.setForeground(new Color(35, 35, 35));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(196, 201, 207), 1, true),
                new EmptyBorder(6, 12, 6, 12)));
        button.setContentAreaFilled(true);
        return button;
    }

    protected void installContent(JComponent form) {
        JPanel workspace = new JPanel(new BorderLayout(0, 12));
        workspace.setBackground(new Color(245, 246, 248));

        JPanel resultsCard = new JPanel(new BorderLayout(0, 10));
        resultsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(216, 219, 223), 1, true),
                new EmptyBorder(12, 12, 12, 12)));
        resultsCard.setBackground(Color.WHITE);

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createEmptyBorder());
        outputScroll.getViewport().setBackground(Color.WHITE);
        outputScroll.setPreferredSize(new Dimension(100, 160));

        JScrollPane tableScroll = new JScrollPane(detailTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);

        JPanel detailsPanel = new JPanel(new BorderLayout(0, 8));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.add(outputScroll, BorderLayout.NORTH);
        detailsPanel.add(tableScroll, BorderLayout.CENTER);

        resultsCard.add(detailsPanel, BorderLayout.CENTER);

        workspace.add(form, BorderLayout.NORTH);
        workspace.add(resultsCard, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
    }

    protected void setOutput(String text) {
        outputArea.setText(text);
        outputArea.setCaretPosition(0);
    }

    protected void setChart(String title, String[] labels, double[] values) {
        // Charts removed from the UI by request.
    }

    protected void setDetailTable(String[] columns, Object[][] rows) {
        detailTableModel.setDataVector(rows, columns);
    }

    protected void clearPresentation() {
        setOutput("");
        setChart("Sin datos", new String[0], new double[0]);
        setDetailTable(new String[0], new Object[0][0]);
    }

    private JTextArea createOutputArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(Color.WHITE);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(216, 219, 223), 1, true),
                new EmptyBorder(10, 10, 10, 10)));
        area.setLineWrap(false);
        area.setCaretPosition(0);
        area.setPreferredSize(new Dimension(520, 150));
        return area;
    }

    private JTable createDetailTable() {
        JTable table = new JTable(detailTableModel);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setRowHeight(22);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        return table;
    }
}