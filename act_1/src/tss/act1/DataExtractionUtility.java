package tss.act1;

import tss.act1.core.JavaRandomSource;
import tss.act1.problems.PartOneSimulator;
import tss.act1.problems.ProblemOneSimulator;
import tss.act1.problems.ProblemTwoSimulator;
import tss.act1.services.SimulationService;

import java.util.List;
import java.util.Locale;

/**
 * Utility to extract detailed simulation data for desktop testing
 */
public class DataExtractionUtility {
    private static final int[] SIZES = {10, 25, 50, 100, 250, 500, 1000};
    private static final long BASE_SEED_PART1 = 42000L;
    private static final long BASE_SEED_PROB1 = 52000L;
    private static final long BASE_SEED_PROB2 = 62000L;

    public static void main(String[] args) {
        SimulationService service = new SimulationService();

        System.out.println("=== PARTE 1: Transformada Inversa ===\n");
        extractPartOne(service);

        System.out.println("\n\n=== PROBLEMA 1: Máquinas y Mecánicos (m=1) ===\n");
        extractProblemOne(service);

        System.out.println("\n\n=== PROBLEMA 2: Trabajadores y Camiones (w=3) ===\n");
        extractProblemTwo(service);
    }

    private static void extractPartOne(SimulationService service) {
        for (int n : SIZES) {
            JavaRandomSource rng = new JavaRandomSource(BASE_SEED_PART1 + n);
            PartOneSimulator.Result result = service.runPartOne(n, rng);
            System.out.printf("[PARTE1 n=%d]%n", n);
            System.out.println("it | x | mean | valid");
            System.out.println("---+---+------+------");

            if (n <= 25) {
                for (PartOneSimulator.IterationDetail d : result.details) {
                    System.out.printf(Locale.US, "%d | %.4f | %.4f | %d/%d%n",
                            d.iteration, d.x, d.runningMean, d.validCountSoFar, n);
                }
            } else {
                for (int i = 0; i < Math.min(10, result.details.length); i++) {
                    PartOneSimulator.IterationDetail d = result.details[i];
                    System.out.printf(Locale.US, "%d | %.4f | %.4f | %d/%d%n",
                            d.iteration, d.x, d.runningMean, d.validCountSoFar, n);
                }
                PartOneSimulator.IterationDetail last = result.details[n - 1];
                System.out.println("...");
                System.out.printf(Locale.US, "%d | %.4f | %.4f | %d/%d%n",
                        last.iteration, last.x, last.runningMean, last.validCountSoFar, n);
            }
            System.out.printf(Locale.US, "final: mean=%.4f std=%.4f min=%.4f max=%.4f%n%n",
                    result.mean, result.std, result.min, result.max);
        }
    }

    private static void extractProblemOne(SimulationService service) {
        for (int n : SIZES) {
            JavaRandomSource rng = new JavaRandomSource(BASE_SEED_PROB1 + n);
            ProblemOneSimulator.Result result = service.runProblemOne(n, 1, 1, rng).get(0);
            System.out.printf("[PROB1 m=1 n=%d]%n", n);
            System.out.println("rep | downtime_h | idle_cost | salary | total");
            System.out.println("----+------------+-----------+--------+------");

            if (n <= 25) {
                for (ProblemOneSimulator.ReplicationDetail d : result.details) {
                    System.out.printf(Locale.US, "%d | %.4f | %.2f | %.2f | %.2f%n",
                            d.replication, d.downtimeHours, d.idleCost, d.salaryCost, d.totalCost);
                }
            } else {
                for (int i = 0; i < Math.min(10, result.details.length); i++) {
                    ProblemOneSimulator.ReplicationDetail d = result.details[i];
                    System.out.printf(Locale.US, "%d | %.4f | %.2f | %.2f | %.2f%n",
                            d.replication, d.downtimeHours, d.idleCost, d.salaryCost, d.totalCost);
                }
                ProblemOneSimulator.ReplicationDetail last = result.details[n - 1];
                System.out.println("...");
                System.out.printf(Locale.US, "%d | %.4f | %.2f | %.2f | %.2f%n",
                        last.replication, last.downtimeHours, last.idleCost, last.salaryCost, last.totalCost);
            }
            System.out.printf(Locale.US, "avg: downtime=%.4f idle=%.2f salary=%.2f total=%.2f%n%n",
                    result.avgDowntimeHours, result.avgIdleCost, result.avgSalaryCost, result.avgCost);
        }
    }

    private static void extractProblemTwo(SimulationService service) {
        for (int n : SIZES) {
            JavaRandomSource rng = new JavaRandomSource(BASE_SEED_PROB2 + n);
            ProblemTwoSimulator.Result result = service.runProblemTwo(n, 3, 3, rng).get(0);
            System.out.printf("[PROB2 w=3 n=%d]%n", n);
            System.out.println("rep | wait_h | wait_cost | salary | total");
            System.out.println("----+--------+-----------+--------+------");

            if (n <= 25) {
                for (ProblemTwoSimulator.ReplicationDetail d : result.details) {
                    System.out.printf(Locale.US, "%d | %.4f | %.2f | %.2f | %.2f%n",
                            d.replication, d.waitHours, d.waitCost, d.salaryCost, d.totalCost);
                }
            } else {
                for (int i = 0; i < Math.min(10, result.details.length); i++) {
                    ProblemTwoSimulator.ReplicationDetail d = result.details[i];
                    System.out.printf(Locale.US, "%d | %.4f | %.2f | %.2f | %.2f%n",
                            d.replication, d.waitHours, d.waitCost, d.salaryCost, d.totalCost);
                }
                ProblemTwoSimulator.ReplicationDetail last = result.details[n - 1];
                System.out.println("...");
                System.out.printf(Locale.US, "%d | %.4f | %.2f | %.2f | %.2f%n",
                        last.replication, last.waitHours, last.waitCost, last.salaryCost, last.totalCost);
            }
            System.out.printf(Locale.US, "avg: wait=%.4f waitCost=%.2f salary=%.2f total=%.2f%n%n",
                    result.avgWaitHours, result.avgWaitCost, result.avgSalaryCost, result.avgCost);
        }
    }
}
