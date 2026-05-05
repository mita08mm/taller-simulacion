package tss.act1.problems;

import tss.act1.core.RandomSource;
import tss.act1.distributions.ContinuousDistribution;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Simulator for Problem 2: Poisson arrivals, single server, service time depends on worker count.
 */
public class ProblemTwoSimulator {
    private final double lambda; // arrivals per hour
    private final double horizonHours;
    private final double waitCostPerHour;
    private final double salaryPerWorkerPerHour;

    public ProblemTwoSimulator(double lambda, double horizonHours, double waitCostPerHour, double salaryPerWorkerPerHour) {
        this.lambda = lambda;
        this.horizonHours = horizonHours;
        this.waitCostPerHour = waitCostPerHour;
        this.salaryPerWorkerPerHour = salaryPerWorkerPerHour;
    }

    public Result evaluate(int workers, int replications, RandomSource rng, ContinuousDistribution serviceTimeDist) {
        double totalCost = 0.0;
        double totalWaitHours = 0.0;
        double totalWaitCost = 0.0;
        double totalSalaryCost = 0.0;
        ReplicationDetail[] details = new ReplicationDetail[replications];
        for (int rep = 0; rep < replications; rep++) {
            RunMetrics run = runOnce(workers, rng, serviceTimeDist);
            totalCost += run.totalCost;
            totalWaitHours += run.totalWaitHours;
            totalWaitCost += run.waitCost;
            totalSalaryCost += run.salaryCost;
            details[rep] = new ReplicationDetail(rep + 1, run.totalWaitHours, run.waitCost, run.salaryCost, run.totalCost);
        }
        return new Result(
                workers,
                totalCost / replications,
                totalWaitHours / replications,
                totalWaitCost / replications,
                totalSalaryCost / replications,
                details);
    }

    private RunMetrics runOnce(int workers, RandomSource rng, ContinuousDistribution serviceTimeDist) {
        double t = 0.0;
        double totalWait = 0.0;
        double serverFreeAt = 0.0;
        Queue<Double> q = new LinkedList<>();

        while (true) {
            double ia = -Math.log(1.0 - rng.nextDouble()) / lambda;
            t += ia;
            if (t > horizonHours) break;
            // arrival at time t
            if (t >= serverFreeAt) {
                // serve immediately
                double service = serviceTimeDist.sample(rng);
                serverFreeAt = t + service;
            } else {
                // joins queue
                q.add(t);
            }

            // process queue as server becomes free before next arrival
            while (!q.isEmpty() && serverFreeAt <= horizonHours) {
                double arrivalTime = q.remove();
                double wait = serverFreeAt - arrivalTime;
                totalWait += wait;
                double service = serviceTimeDist.sample(rng);
                serverFreeAt = serverFreeAt + service;
            }
        }

        double salary = workers * salaryPerWorkerPerHour * horizonHours;
        double waitCost = totalWait * waitCostPerHour;
        return new RunMetrics(salary + waitCost, totalWait, waitCost, salary);
    }

    private static class RunMetrics {
        final double totalCost;
        final double totalWaitHours;
        final double waitCost;
        final double salaryCost;

        RunMetrics(double totalCost, double totalWaitHours, double waitCost, double salaryCost) {
            this.totalCost = totalCost;
            this.totalWaitHours = totalWaitHours;
            this.waitCost = waitCost;
            this.salaryCost = salaryCost;
        }
    }

    public static class Result {
        public final int workers;
        public final double avgCost;
        public final double avgWaitHours;
        public final double avgWaitCost;
        public final double avgSalaryCost;
        public final ReplicationDetail[] details;

        public Result(int workers,
                      double avgCost,
                      double avgWaitHours,
                      double avgWaitCost,
                      double avgSalaryCost,
                      ReplicationDetail[] details) {
            this.workers = workers;
            this.avgCost = avgCost;
            this.avgWaitHours = avgWaitHours;
            this.avgWaitCost = avgWaitCost;
            this.avgSalaryCost = avgSalaryCost;
            this.details = details;
        }
        @Override public String toString() { return String.format("w=%d avgCost=%.2f", workers, avgCost); }
    }

    public static class ReplicationDetail {
        public final int replication;
        public final double waitHours;
        public final double waitCost;
        public final double salaryCost;
        public final double totalCost;

        public ReplicationDetail(int replication,
                                 double waitHours,
                                 double waitCost,
                                 double salaryCost,
                                 double totalCost) {
            this.replication = replication;
            this.waitHours = waitHours;
            this.waitCost = waitCost;
            this.salaryCost = salaryCost;
            this.totalCost = totalCost;
        }
    }
}
