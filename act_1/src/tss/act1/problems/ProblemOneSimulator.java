package tss.act1.problems;

import tss.act1.core.RandomSource;
import tss.act1.distributions.ContinuousDistribution;

import java.util.PriorityQueue;

/**
 * Simple discrete-event simulator for Problem 1 (machines served by one mechanic).
 * For given m machines per mechanic, simulates failures and repairs over a horizon.
 */
public class ProblemOneSimulator {
    private final ContinuousDistribution timeToFailureDist;
    private final ContinuousDistribution repairTimeDist;
    private final double horizonHours;
    private final double idleCostPerHour;
    private final double mechanicSalaryPerHour;

    public ProblemOneSimulator(ContinuousDistribution timeToFailureDist,
                               ContinuousDistribution repairTimeDist,
                               double horizonHours,
                               double idleCostPerHour,
                               double mechanicSalaryPerHour) {
        this.timeToFailureDist = timeToFailureDist;
        this.repairTimeDist = repairTimeDist;
        this.horizonHours = horizonHours;
        this.idleCostPerHour = idleCostPerHour;
        this.mechanicSalaryPerHour = mechanicSalaryPerHour;
    }

    public Result evaluate(int mMachines, int replications, RandomSource rng) {
        double totalCost = 0.0;
        double totalIdleCost = 0.0;
        double totalSalary = 0.0;
        double totalDowntime = 0.0;
        ReplicationDetail[] details = new ReplicationDetail[replications];
        for (int rep = 0; rep < replications; rep++) {
            RunMetrics run = runOnce(mMachines, rng);
            totalCost += run.totalCost;
            totalIdleCost += run.idleCost;
            totalSalary += run.salaryCost;
            totalDowntime += run.totalDowntimeHours;
            details[rep] = new ReplicationDetail(rep + 1, run.totalDowntimeHours, run.idleCost, run.salaryCost, run.totalCost);
        }
        return new Result(
                mMachines,
                totalCost / replications,
                totalIdleCost / replications,
                totalSalary / replications,
                totalDowntime / replications,
                details);
    }
    private RunMetrics runOnce(int mMachines, RandomSource rng) {
        // Event-based: schedule initial failure for each machine
        PriorityQueue<Event> pq = new PriorityQueue<>((a,b)->Double.compare(a.time,b.time));
        for (int i = 0; i < mMachines; i++) {
            double t = timeToFailureDist.sample(rng);
            pq.add(new Event(t, Event.Type.FAILURE, i));
        }

        boolean mechanicBusy = false;
        double repairEndTime = 0.0;
        double totalDowntime = 0.0; // sum of downtime (waiting + repair)
        // for queued failures store occurrence time
        java.util.Queue<Double> queue = new java.util.ArrayDeque<>();

        while (!pq.isEmpty()) {
            Event e = pq.poll();
            if (e.time > horizonHours) break;
            if (e.type == Event.Type.FAILURE) {
                // failure occurs at e.time
                if (!mechanicBusy) {
                    // start repair immediately
                    double repair = repairTimeDist.sample(rng);
                    mechanicBusy = true;
                    repairEndTime = e.time + repair;
                    // downtime for this machine is repair (no waiting)
                    totalDowntime += repair;
                    pq.add(new Event(repairEndTime, Event.Type.REPAIR_COMPLETE, e.machineId));
                } else {
                    // enqueue failure time
                    queue.add(e.time);
                }
            } else { // repair complete
                // schedule next failure for this machine
                double nextFailure = e.time + timeToFailureDist.sample(rng);
                if (nextFailure <= horizonHours) pq.add(new Event(nextFailure, Event.Type.FAILURE, e.machineId));

                if (!queue.isEmpty()) {
                    double failTime = queue.remove();
                    double waiting = e.time - failTime;
                    double repair = repairTimeDist.sample(rng);
                    totalDowntime += waiting + repair;
                    repairEndTime = e.time + repair;
                    pq.add(new Event(repairEndTime, Event.Type.REPAIR_COMPLETE, e.machineId));
                    mechanicBusy = true;
                } else {
                    mechanicBusy = false;
                }
            }
        }

        double idleCost = totalDowntime * idleCostPerHour;
        double salary = mechanicSalaryPerHour * horizonHours; // one mechanic per group
        return new RunMetrics(idleCost + salary, idleCost, salary, totalDowntime);
    }

    private static class Event {
        enum Type { FAILURE, REPAIR_COMPLETE }
        final double time;
        final Type type;
        final int machineId;
        Event(double time, Type type, int machineId) { this.time = time; this.type = type; this.machineId = machineId; }
    }

    private static class RunMetrics {
        final double totalCost;
        final double idleCost;
        final double salaryCost;
        final double totalDowntimeHours;

        RunMetrics(double totalCost, double idleCost, double salaryCost, double totalDowntimeHours) {
            this.totalCost = totalCost;
            this.idleCost = idleCost;
            this.salaryCost = salaryCost;
            this.totalDowntimeHours = totalDowntimeHours;
        }
    }

    public static class Result {
        public final int machinesPerMechanic;
        public final double avgCost;
        public final double avgIdleCost;
        public final double avgSalaryCost;
        public final double avgDowntimeHours;
        public final ReplicationDetail[] details;

        public Result(int machinesPerMechanic,
                      double avgCost,
                      double avgIdleCost,
                      double avgSalaryCost,
                      double avgDowntimeHours,
                      ReplicationDetail[] details) {
            this.machinesPerMechanic = machinesPerMechanic;
            this.avgCost = avgCost;
            this.avgIdleCost = avgIdleCost;
            this.avgSalaryCost = avgSalaryCost;
            this.avgDowntimeHours = avgDowntimeHours;
            this.details = details;
        }
        @Override public String toString() { return String.format("m=%d avgCost=%.2f", machinesPerMechanic, avgCost); }
    }

    public static class ReplicationDetail {
        public final int replication;
        public final double downtimeHours;
        public final double idleCost;
        public final double salaryCost;
        public final double totalCost;

        public ReplicationDetail(int replication,
                                 double downtimeHours,
                                 double idleCost,
                                 double salaryCost,
                                 double totalCost) {
            this.replication = replication;
            this.downtimeHours = downtimeHours;
            this.idleCost = idleCost;
            this.salaryCost = salaryCost;
            this.totalCost = totalCost;
        }
    }
}
