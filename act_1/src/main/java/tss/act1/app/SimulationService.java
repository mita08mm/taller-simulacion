package tss.act1.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tss.act1.core.JavaRandomSource;
import tss.act1.core.RandomSource;
import tss.act1.distributions.InverseTransformPartOneDistribution;
import tss.act1.problems.ProblemOneResult;
import tss.act1.problems.ProblemOneSimulator;
import tss.act1.problems.ProblemTwoResult;
import tss.act1.problems.ProblemTwoSimulator;

public final class SimulationService {
    public String run(SimulationInput input) {
        SimulationDetailedResult result = runDetailed(input);
        return format(result);
    }

    public SimulationDetailedResult runDetailed(SimulationInput input) {
        RandomSource random = new JavaRandomSource();

        PartOneResult partOne = simulatePartOne(random, input.partOneSamples());
        ProblemOneComputation problemOne = simulateProblemOne(random, input);
        ProblemTwoComputation problemTwo = simulateProblemTwo(random, input);

        return new SimulationDetailedResult(
                partOne,
                problemOne.results(),
                problemOne.best(),
                problemTwo.results(),
                problemTwo.best());
    }

    private String format(SimulationDetailedResult result) {
        StringBuilder out = new StringBuilder();
        appendPartOne(out, result.partOne());
        appendProblemOne(out, result.problemOneResults(), result.bestProblemOne());
        appendProblemTwo(out, result.problemTwoResults(), result.bestProblemTwo());

        return out.toString();
    }

    private PartOneResult simulatePartOne(RandomSource random, int samples) {
        InverseTransformPartOneDistribution distribution = new InverseTransformPartOneDistribution();
        double sum = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        List<InverseSample> pairs = new ArrayList<>();

        for (int i = 0; i < samples; i++) {
            double u = random.nextDouble();
            double x = distribution.inverseCdf(u);
            pairs.add(new InverseSample(u, x));
            sum += x;
            min = Math.min(min, x);
            max = Math.max(max, x);
        }

        double mean = sum / samples;

        return new PartOneResult(samples, mean, min, max, List.copyOf(pairs));
    }

    private void appendPartOne(StringBuilder out, PartOneResult partOne) {
        out.append("Metodo: CDF inversa (Raul Coss Bu)\n");
        out.append("Ejemplo U -> X usando F^-1(u):\n");
        int index = 1;
        int maxExamples = Math.min(5, partOne.samplePairs().size());
        for (int i = 0; i < maxExamples; i++) {
            InverseSample pair = partOne.samplePairs().get(i);
            out.append(String.format(Locale.US, "  %d) u=%.5f -> x=%.5f%n", index++, pair.u(), pair.x()));
        }
        if (partOne.samplePairs().size() > maxExamples) {
            out.append(String.format(Locale.US, "  ... (%d muestras generadas en total)%n", partOne.samples()));
        }
        out.append(String.format(Locale.US, "Muestras: %d%n", partOne.samples()));
        out.append(String.format(Locale.US, "Media estimada: %.5f%n", partOne.mean()));
        out.append(String.format(Locale.US, "Min/Max observados: %.5f / %.5f%n%n", partOne.min(), partOne.max()));
    }

    private ProblemOneComputation simulateProblemOne(RandomSource random, SimulationInput input) {
        ProblemOneSimulator simulator = new ProblemOneSimulator();
        List<ProblemOneResult> results = simulator.evaluateAssignments(
                input.problemOneMinMachines(),
                input.problemOneMaxMachines(),
                input.problemOneReplications(),
                input.problemOneHorizonHours(),
                input.problemOneIdleCostPerHour(),
                input.problemOneMechanicSalaryPerHour(),
                random);

        ProblemOneResult best = null;
        for (ProblemOneResult candidate : results) {
            if (best == null || candidate.totalCostPerMachineHour() < best.totalCostPerMachineHour()) {
                best = candidate;
            }
        }

        return new ProblemOneComputation(results, best);
    }

    private void appendProblemOne(StringBuilder out, List<ProblemOneResult> results, ProblemOneResult best) {
        out.append("=== PARTE 2 - Problema 1 ===\n");
        out.append("Metodo de muestreo: CDF inversa por intervalos\n");
        out.append("Simulacion por alternativa (maquinas por mecanico):\n");
        out.append("  m | costo_total_mh | costo_ocio_mh | salario_mh\n");
        for (ProblemOneResult r : results) {
            out.append(String.format(Locale.US,
                    "  %2d | %14.5f | %13.5f | %10.5f%n",
                    r.machinesPerMechanic(),
                    r.totalCostPerMachineHour(),
                    r.idleCostPerMachineHour(),
                    r.mechanicSalaryPerMachineHour()));
        }
        out.append(String.format(Locale.US, "Mejor maquinas por mecanico: %d%n", best.machinesPerMechanic()));
        out.append(String.format(Locale.US, "Tiempo ocioso promedio por maquina (h): %.5f%n", best.averageDowntimeHoursPerMachine()));
        out.append(String.format(Locale.US, "Costo ociosidad por maquina-hora: %.5f%n", best.idleCostPerMachineHour()));
        out.append(String.format(Locale.US, "Salario por maquina-hora: %.5f%n", best.mechanicSalaryPerMachineHour()));
        out.append(String.format(Locale.US, "Costo total por maquina-hora: %.5f%n%n", best.totalCostPerMachineHour()));
    }

    private ProblemTwoComputation simulateProblemTwo(RandomSource random, SimulationInput input) {
        ProblemTwoSimulator simulator = new ProblemTwoSimulator();
        List<ProblemTwoResult> results = simulator.evaluateTeamSizes(
                input.problemTwoMinTeam(),
                input.problemTwoMaxTeam(),
                input.problemTwoReplications(),
                input.problemTwoShiftHours(),
                input.problemTwoWorkerSalaryPerHour(),
                input.problemTwoWaitingCostPerHour(),
                random);

        ProblemTwoResult best = null;
        for (ProblemTwoResult candidate : results) {
            if (best == null || candidate.totalCostPerShift() < best.totalCostPerShift()) {
                best = candidate;
            }
        }

        return new ProblemTwoComputation(results, best);
    }

    private void appendProblemTwo(StringBuilder out, List<ProblemTwoResult> results, ProblemTwoResult best) {
        out.append("=== PARTE 2 - Problema 2 ===\n");
        out.append("Metodo de muestreo: Exponencial y Uniforme por CDF inversa\n");
        out.append("Simulacion por alternativa (tamano de equipo):\n");
        out.append("  n | costo_total_turno | costo_espera | costo_mano_obra\n");
        for (ProblemTwoResult r : results) {
            out.append(String.format(Locale.US,
                    "  %2d | %17.5f | %11.5f | %15.5f%n",
                    r.teamSize(),
                    r.totalCostPerShift(),
                    r.waitingCostPerShift(),
                    r.laborCostPerShift()));
        }
        out.append(String.format(Locale.US, "Mejor tamano de equipo: %d%n", best.teamSize()));
        out.append(String.format(Locale.US, "Espera promedio total de camiones por turno (h): %.5f%n", best.averageTruckWaitingHours()));
        out.append(String.format(Locale.US, "Costo espera por turno: %.5f%n", best.waitingCostPerShift()));
        out.append(String.format(Locale.US, "Costo mano de obra por turno: %.5f%n", best.laborCostPerShift()));
        out.append(String.format(Locale.US, "Costo total por turno: %.5f%n", best.totalCostPerShift()));
    }

    private record ProblemOneComputation(List<ProblemOneResult> results, ProblemOneResult best) {
    }

    private record ProblemTwoComputation(List<ProblemTwoResult> results, ProblemTwoResult best) {
    }
}
