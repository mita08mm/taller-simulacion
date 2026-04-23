package tss.act1.problems;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import tss.act1.core.RandomSource;
import tss.act1.distributions.ContinuousDistribution;
import tss.act1.distributions.PiecewiseUniformDistribution;
import tss.act1.model.IntervalProbability;

public final class ProblemOneSimulator {
    private final ContinuousDistribution uptimeDistribution;
    private final ContinuousDistribution repairDistribution;

    public ProblemOneSimulator() {
        this.uptimeDistribution = new PiecewiseUniformDistribution(List.of(
                new IntervalProbability(6.0, 8.0, 0.10),
                new IntervalProbability(8.0, 10.0, 0.15),
                new IntervalProbability(10.0, 12.0, 0.24),
                new IntervalProbability(12.0, 14.0, 0.26),
                new IntervalProbability(16.0, 18.0, 0.18),
                new IntervalProbability(18.0, 20.0, 0.07)));

        this.repairDistribution = new PiecewiseUniformDistribution(List.of(
                new IntervalProbability(2.0, 4.0, 0.15),
                new IntervalProbability(4.0, 6.0, 0.25),
                new IntervalProbability(6.0, 8.0, 0.30),
                new IntervalProbability(8.0, 10.0, 0.20),
                new IntervalProbability(10.0, 12.0, 0.10)));
    }

    public ProblemOneResult findBestAssignment(
            int minMachines,
            int maxMachines,
            int replications,
            double horizonHours,
            double idleCostPerHour,
            double mechanicSalaryPerHour,
            RandomSource random) {

        List<ProblemOneResult> results = evaluateAssignments(
                minMachines,
                maxMachines,
                replications,
                horizonHours,
                idleCostPerHour,
                mechanicSalaryPerHour,
                random);

        ProblemOneResult best = null;
        for (ProblemOneResult candidate : results) {
            if (best == null || candidate.totalCostPerMachineHour() < best.totalCostPerMachineHour()) {
                best = candidate;
            }
        }
        return best;
    }

    public List<ProblemOneResult> evaluateAssignments(
            int minMachines,
            int maxMachines,
            int replications,
            double horizonHours,
            double idleCostPerHour,
            double mechanicSalaryPerHour,
            RandomSource random) {

        if (minMachines <= 0 || maxMachines < minMachines) {
            throw new IllegalArgumentException("Invalid machine range");
        }

        List<ProblemOneResult> results = new ArrayList<>();
        for (int machines = minMachines; machines <= maxMachines; machines++) {
            double downtime = 0.0;
            for (int r = 0; r < replications; r++) {
                downtime += simulateDowntimePerMachine(machines, horizonHours, random);
            }

            double averageDowntimePerMachine = downtime / replications;
            double downtimeFraction = averageDowntimePerMachine / horizonHours;
            double idleCostPerMachineHour = idleCostPerHour * downtimeFraction;
            double salaryPerMachineHour = mechanicSalaryPerHour / machines;
            double totalCost = idleCostPerMachineHour + salaryPerMachineHour;

            ProblemOneResult candidate = new ProblemOneResult(
                    machines,
                    averageDowntimePerMachine,
                    idleCostPerMachineHour,
                    salaryPerMachineHour,
                    totalCost);

            results.add(candidate);
        }
        return results;
    }

    private double simulateDowntimePerMachine(int machineCount, double horizonHours, RandomSource random) {
        PriorityQueue<Event> events = new PriorityQueue<>(Comparator
                .comparingDouble(Event::time)
                .thenComparingInt(Event::sequence));
        Queue<Integer> waitingQueue = new ArrayDeque<>();

        double[] downStart = new double[machineCount];
        double[] totalDown = new double[machineCount];
        for (int i = 0; i < machineCount; i++) {
            downStart[i] = -1.0;
            double failureTime = uptimeDistribution.sample(random);
            events.add(Event.failure(failureTime, i));
        }

        int sequence = machineCount;
        int currentMachineInRepair = -1;

        while (!events.isEmpty()) {
            Event event = events.poll();
            if (event.time() > horizonHours) {
                break;
            }

            if (event.type() == EventType.FAILURE) {
                int machineId = event.machineId();
                downStart[machineId] = event.time();

                if (currentMachineInRepair == -1) {
                    currentMachineInRepair = machineId;
                    double repairDuration = repairDistribution.sample(random);
                    events.add(Event.repairCompletion(event.time() + repairDuration, machineId, sequence++));
                } else {
                    waitingQueue.add(machineId);
                }
            } else {
                int machineId = event.machineId();
                if (downStart[machineId] >= 0.0) {
                    totalDown[machineId] += event.time() - downStart[machineId];
                    downStart[machineId] = -1.0;
                }

                double nextFailure = event.time() + uptimeDistribution.sample(random);
                events.add(Event.failure(nextFailure, machineId, sequence++));

                if (!waitingQueue.isEmpty()) {
                    int nextMachine = waitingQueue.remove();
                    currentMachineInRepair = nextMachine;
                    double repairDuration = repairDistribution.sample(random);
                    events.add(Event.repairCompletion(event.time() + repairDuration, nextMachine, sequence++));
                } else {
                    currentMachineInRepair = -1;
                }
            }
        }

        // Machines still down at horizon contribute downtime until horizon.
        for (int i = 0; i < machineCount; i++) {
            if (downStart[i] >= 0.0) {
                totalDown[i] += horizonHours - downStart[i];
            }
        }

        double sumDown = 0.0;
        for (double down : totalDown) {
            sumDown += down;
        }
        return sumDown / machineCount;
    }

    private enum EventType {
        FAILURE,
        REPAIR_COMPLETION
    }

    private record Event(double time, int machineId, EventType type, int sequence) {
        static Event failure(double time, int machineId) {
            return new Event(time, machineId, EventType.FAILURE, machineId);
        }

        static Event failure(double time, int machineId, int sequence) {
            return new Event(time, machineId, EventType.FAILURE, sequence);
        }

        static Event repairCompletion(double time, int machineId, int sequence) {
            return new Event(time, machineId, EventType.REPAIR_COMPLETION, sequence);
        }
    }
}
