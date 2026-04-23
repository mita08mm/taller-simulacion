package tss.act1.app;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        boolean cliMode = args.length > 0 && "--cli".equalsIgnoreCase(args[0]);

        if (cliMode || GraphicsEnvironment.isHeadless()) {
            SimulationService service = new SimulationService();
            String result = service.run(SimulationInput.defaults());
            System.out.println(result);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            SimulationFrame frame = new SimulationFrame();
            frame.setVisible(true);
        });
    }
}
