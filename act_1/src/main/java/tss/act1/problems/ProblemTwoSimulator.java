package tss.act1.problems;

import java.util.ArrayList;
import java.util.List;

import tss.act1.core.RandomSource;
import tss.act1.distributions.ContinuousDistribution;
import tss.act1.distributions.ExponentialDistribution;
import tss.act1.distributions.UniformDistribution;

public final class ProblemTwoSimulator {
    private final ContinuousDistribution arrivalInterTimeDistribution;

    public ProblemTwoSimulator() {
        this.arrivalInterTimeDistribution = new ExponentialDistribution(2.0);
    }

    public ProblemTwoResult findBestTeamSize(
            int minTeam,
            int maxTeam,
            int replications,
            double shiftHours,
            double workerSalaryPerHour,
            double waitingCostPerHour,
            RandomSource random) {

        List<ProblemTwoResult> results = evaluateTeamSizes(
                minTeam,
                maxTeam,
                replications,
                shiftHours,
                workerSalaryPerHour,
                waitingCostPerHour,
                random);

        ProblemTwoResult best = null;
        for (ProblemTwoResult candidate : results) {
            if (best == null || candidate.totalCostPerShift() < best.totalCostPerShift()) {
                best = candidate;
            }
        }
        return best;
    }

    public List<ProblemTwoResult> evaluateTeamSizes(
            int minTeam,
            int maxTeam,
            int replications,
            double shiftHours,
            double workerSalaryPerHour,
            double waitingCostPerHour,
            RandomSource random) {

        List<ProblemTwoResult> results = new ArrayList<>();

        for (int teamSize = minTeam; teamSize <= maxTeam; teamSize++) {
            double totalWaiting = 0.0;
            for (int r = 0; r < replications; r++) {
                totalWaiting += simulateTotalWaitingHours(teamSize, shiftHours, random);
            }

            double avgTotalWaitingHours = totalWaiting / replications;
            double waitingCost = avgTotalWaitingHours * waitingCostPerHour;
            double laborCost = teamSize * workerSalaryPerHour * shiftHours;
            double totalCost = waitingCost + laborCost;

            ProblemTwoResult candidate = new ProblemTwoResult(
                    teamSize,
                    avgTotalWaitingHours,
                    waitingCost,
                    laborCost,
                    totalCost);

            results.add(candidate);
        }

        return results;
    }

    private double simulateTotalWaitingHours(int teamSize, double shiftHours, RandomSource random) {
        ContinuousDistribution serviceTime = serviceTimeDistributionByTeamSize(teamSize);

        double clock = 0.0;
        double serverAvailableAt = 0.0;
        double totalWaiting = 0.0;

        while (true) {
            clock += arrivalInterTimeDistribution.sample(random);
            if (clock > shiftHours) {
                break;
            }

            double serviceStart = Math.max(clock, serverAvailableAt);
            totalWaiting += serviceStart - clock;
            serverAvailableAt = serviceStart + serviceTime.sample(random);
        }

        return totalWaiting;
    }

    private ContinuousDistribution serviceTimeDistributionByTeamSize(int teamSize) {
        return switch (teamSize) {
            case 3 -> new UniformDistribution(20.0 / 60.0, 30.0 / 60.0);
            case 4 -> new UniformDistribution(15.0 / 60.0, 25.0 / 60.0);
            case 5 -> new UniformDistribution(10.0 / 60.0, 20.0 / 60.0);
            case 6 -> new UniformDistribution(5.0 / 60.0, 15.0 / 60.0);
            default -> throw new IllegalArgumentException("Team size must be between 3 and 6");
        };
    }
}
