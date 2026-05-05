package tss.act1.services;

import tss.act1.core.RandomSource;
import tss.act1.distributions.ContinuousDistribution;
import tss.act1.distributions.InverseTransformPartOneDistribution;
import tss.act1.distributions.PiecewiseUniformDistribution;
import tss.act1.distributions.UniformMinutesDistribution;
import tss.act1.problems.PartOneSimulator;
import tss.act1.problems.ProblemOneSimulator;
import tss.act1.problems.ProblemTwoSimulator;

import java.util.ArrayList;
import java.util.List;

public class SimulationService {
    private final PartOneSimulator partOneSimulator;
    private final ProblemOneSimulator problemOneSimulator;
    private final ProblemTwoSimulator problemTwoSimulator;

    public SimulationService() {
        this.partOneSimulator = new PartOneSimulator(new InverseTransformPartOneDistribution());
        this.problemOneSimulator = new ProblemOneSimulator(
                new PiecewiseUniformDistribution(problemOneFailureTable()),
                new PiecewiseUniformDistribution(problemOneRepairTable()),
                8.0,
                500.0,
                50.0);
        this.problemTwoSimulator = new ProblemTwoSimulator(2.0, 8.0, 50.0, 25.0);
    }

    public PartOneSimulator.Result runPartOne(int sampleSize, RandomSource rng) {
        return partOneSimulator.run(sampleSize, rng);
    }

    public List<ProblemOneSimulator.Result> runProblemOne(int replications, int fromMachines, int toMachines, RandomSource rng) {
        List<ProblemOneSimulator.Result> results = new ArrayList<>();
        for (int machines = fromMachines; machines <= toMachines; machines++) {
            results.add(problemOneSimulator.evaluate(machines, replications, rng));
        }
        return results;
    }

    public List<ProblemTwoSimulator.Result> runProblemTwo(int replications, int fromWorkers, int toWorkers, RandomSource rng) {
        List<ProblemTwoSimulator.Result> results = new ArrayList<>();
        for (int workers = fromWorkers; workers <= toWorkers; workers++) {
            results.add(problemTwoSimulator.evaluate(workers, replications, rng, serviceDistributionForWorkers(workers)));
        }
        return results;
    }

    private double[][] problemOneFailureTable() {
        return new double[][]{
                {6, 8, 0.10}, {8, 10, 0.15}, {10, 12, 0.24}, {12, 14, 0.26}, {16, 18, 0.18}, {18, 20, 0.07}
        };
    }

    private double[][] problemOneRepairTable() {
        return new double[][]{
                {2, 4, 0.15}, {4, 6, 0.25}, {6, 8, 0.30}, {8, 10, 0.20}, {10, 12, 0.10}
        };
    }

    private ContinuousDistribution serviceDistributionForWorkers(int workers) {
        switch (workers) {
            case 3:
                return new UniformMinutesDistribution(20, 30);
            case 4:
                return new UniformMinutesDistribution(15, 25);
            case 5:
                return new UniformMinutesDistribution(10, 20);
            default:
                return new UniformMinutesDistribution(5, 15);
        }
    }
}