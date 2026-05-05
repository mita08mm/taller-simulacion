package tss.act1.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.Arrays;

final class SeriesLineChartPanel extends JPanel {
    private String title = "";
    private String[] labels = new String[0];
    private double[] values = new double[0];

    SeriesLineChartPanel() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(100, 92));
    }

    void setData(String title, String[] labels, double[] values) {
        this.title = title == null ? "" : title;
        this.labels = labels == null ? new String[0] : Arrays.copyOf(labels, labels.length);
        this.values = values == null ? new double[0] : Arrays.copyOf(values, values.length);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(new Color(35, 35, 35));
            g.setFont(getFont().deriveFont(Font.BOLD, 12f));
            g.drawString(title, 2, 13);

            if (values.length == 0) {
                g.setColor(new Color(120, 120, 120));
                g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
                g.drawString("Sin datos", 2, 33);
                return;
            }

            int left = 30;
            int right = 10;
            int top = 20;
            int bottom = 18;
            int width = Math.max(1, getWidth() - left - right);
            int height = Math.max(1, getHeight() - top - bottom);

            double min = Arrays.stream(values).min().orElse(0.0);
            double max = Arrays.stream(values).max().orElse(1.0);
            if (Math.abs(max - min) < 1e-9) {
                max = min + 1.0;
            }

            g.setColor(new Color(231, 233, 236));
            for (int i = 0; i < 3; i++) {
                int y = top + (height * i) / 2;
                g.drawLine(left, y, left + width, y);
            }

            int baselineY = top + height;
            g.setColor(new Color(170, 175, 180));
            g.drawLine(left, top, left, baselineY);
            g.drawLine(left, baselineY, left + width, baselineY);

            Path2D path = new Path2D.Double();
            for (int i = 0; i < values.length; i++) {
                double x = left + (values.length == 1 ? width / 2.0 : (double) i * width / (values.length - 1));
                double normalized = (values[i] - min) / (max - min);
                double y = baselineY - normalized * height;
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            g.setColor(new Color(86, 104, 128));
            g.draw(path);

            for (int i = 0; i < values.length; i++) {
                double x = left + (values.length == 1 ? width / 2.0 : (double) i * width / (values.length - 1));
                double normalized = (values[i] - min) / (max - min);
                double y = baselineY - normalized * height;
                g.fillOval((int) Math.round(x) - 3, (int) Math.round(y) - 3, 6, 6);
            }

            g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
            g.setColor(new Color(95, 95, 95));
            if (labels.length == values.length) {
                for (int i = 0; i < labels.length; i++) {
                    double x = left + (labels.length == 1 ? width / 2.0 : (double) i * width / (labels.length - 1));
                    String label = labels[i] == null ? "" : labels[i];
                    int textWidth = g.getFontMetrics().stringWidth(label);
                    g.drawString(label, (int) Math.round(x) - textWidth / 2, baselineY + 13);
                }
            }

            String minText = String.format(java.util.Locale.US, "%.2f", min);
            String maxText = String.format(java.util.Locale.US, "%.2f", max);
            g.drawString(minText, 2, baselineY);
            g.drawString(maxText, 2, top + 10);
        } finally {
            g.dispose();
        }
    }
}