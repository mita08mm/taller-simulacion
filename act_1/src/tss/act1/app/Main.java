package tss.act1.app;

import tss.act1.core.JavaRandomSource;
import tss.act1.core.RandomSource;
import tss.act1.problems.PartOneSimulator;
import tss.act1.problems.ProblemOneSimulator;
import tss.act1.problems.ProblemTwoSimulator;
import tss.act1.services.SimulationService;

import java.util.List;

public class Main {
    private static final int FIXED_EXECUTIONS = 100;

    public static void main(String[] args) {
        if (args.length == 0 || containsArg(args, "--gui")) {
            javax.swing.SwingUtilities.invokeLater(() -> new Activity1Gui().show());
            return;
        }

        runConsoleDemo();
    }

    private static boolean containsArg(String[] args, String target) {
        for (String arg : args) {
            if (target.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static void runConsoleDemo() {
        System.out.println("Simulación — ejecución de prueba rápida");
        RandomSource rng = new JavaRandomSource();
        SimulationService service = new SimulationService();

        PartOneSimulator.Result r1 = service.runPartOne(FIXED_EXECUTIONS, rng);
        System.out.println("[Parte1] " + r1);

        List<ProblemOneSimulator.Result> p1Results = service.runProblemOne(FIXED_EXECUTIONS, 1, 4, rng);
        for (ProblemOneSimulator.Result res : p1Results) {
            System.out.println("[Problema1] " + res);
        }

        List<ProblemTwoSimulator.Result> p2Results = service.runProblemTwo(FIXED_EXECUTIONS, 3, 6, rng);
        for (ProblemTwoSimulator.Result r2 : p2Results) {
            System.out.println("[Problema2] " + r2);
        }
    }
}
