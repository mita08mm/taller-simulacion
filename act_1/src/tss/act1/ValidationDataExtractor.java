package tss.act1;

import tss.act1.core.JavaRandomSource;
import tss.act1.problems.PartOneSimulator;
import tss.act1.services.SimulationService;

/**
 * Generate validation data for Part 1 with different sample sizes
 */
public class ValidationDataExtractor {
    private static final int[] SIZES = {10, 25, 50, 100, 250, 500, 1000};
    private static final long BASE_SEED = 20260504L;
    
    public static void main(String[] args) {
        SimulationService service = new SimulationService();
        
        System.out.println("Validation: Part 1 — Convergence of sample mean to μ = 3");
        System.out.println("\nn    | Mean    | Error   | Min     | Max     | Valid");
        System.out.println("-----+---------+---------+---------+---------+-------");
        
        for (int n : SIZES) {
            JavaRandomSource rng = new JavaRandomSource(BASE_SEED + n);
            PartOneSimulator.Result result = service.runPartOne(n, rng);
            double error = Math.abs(result.mean - 3.0);
            System.out.printf("%-4d | %.4f | %.4f | %.4f | %.4f | %d/%d%n",
                n, result.mean, error, result.min, result.max, result.validCount, n);
        }

        System.out.println("\nDesktop test summary (report-friendly)");
        System.out.println("n    | Rows shown in report | Mean at row 10 | Final mean");
        System.out.println("-----+----------------------+----------------+-----------");
        for (int n : SIZES) {
            JavaRandomSource rng = new JavaRandomSource(BASE_SEED + n);
            PartOneSimulator.Result result = service.runPartOne(n, rng);
            double meanAt10 = result.details[Math.min(9, n - 1)].runningMean;
            System.out.printf("%-4d | 1..10, ..., %4d      | %.4f         | %.4f%n",
                    n, n, meanAt10, result.mean);
        }
    }
}
