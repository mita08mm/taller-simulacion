package tss.act1.app;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

public class Activity1Gui {
    private final JFrame frame;

    public Activity1Gui() {
        installLookAndFeel();
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(940, 680);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(245, 246, 248));
        frame.add(buildHeader(), BorderLayout.NORTH);
        frame.add(buildTabs(), BorderLayout.CENTER);

    }

    public void show() {
        frame.setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));
        panel.setBackground(new Color(245, 246, 248));

        JLabel title = new JLabel("Actividad 1 - Simulación por transformada inversa y colas");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));

        panel.add(title, BorderLayout.NORTH);

        return panel;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Transformada inversa", new tss.act1.ui.PartOnePanel());
        tabs.addTab("Máquinas y mecánicos", new tss.act1.ui.ProblemOnePanel());
        tabs.addTab("Camiones y descarga", new tss.act1.ui.ProblemTwoPanel());
        tabs.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        tabs.setBackground(new Color(245, 246, 248));
        tabs.setOpaque(true);
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        return tabs;
    }

    private void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep the default look and feel if the platform one is unavailable.
        }
    }
}