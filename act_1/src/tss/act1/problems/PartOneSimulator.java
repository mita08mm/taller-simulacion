package tss.act1.problems;

import tss.act1.core.RandomSource;
import tss.act1.distributions.ContinuousDistribution;

public class PartOneSimulator {
    private final ContinuousDistribution dist;

    public PartOneSimulator(ContinuousDistribution dist) {
        this.dist = dist;
    }

    public Result run(int n, RandomSource rng) {
        double sum = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int valid = 0;
        double[] values = new double[n];
        IterationDetail[] details = new IterationDetail[n];
        for (int i = 0; i < n; i++) {
            double x = dist.sample(rng);
            values[i] = x;
            sum += x;
            min = Math.min(min, x);
            max = Math.max(max, x);
            if (x >= 0.0 && x <= 6.0) {
                valid++;
            }
            double runningMean = sum / (i + 1);
            details[i] = new IterationDetail(i + 1, x, runningMean, valid);
        }
        double mean = sum / n;
        double var = 0.0;
        for (double v : values) var += (v - mean) * (v - mean);
        double std = Math.sqrt(var / (n - 1));
        double validRatio = (double) valid / n;
        double range = max - min;
        return new Result(n, mean, std, min, max, valid, validRatio, range, details);
    }

    public static class Result {
        public final int n;
        public final double mean;
        public final double std;
        public final double min;
        public final double max;
        public final int validCount;
        public final double validRatio;
        public final double range;
        public final IterationDetail[] details;

        public Result(int n,
                      double mean,
                      double std,
                      double min,
                      double max,
                      int validCount,
                      double validRatio,
                      double range,
                      IterationDetail[] details) {
            this.n = n;
            this.mean = mean;
            this.std = std;
            this.min = min;
            this.max = max;
            this.validCount = validCount;
            this.validRatio = validRatio;
            this.range = range;
            this.details = details;
        }

        @Override
        public String toString() {
            return String.format("n=%d mean=%.4f std=%.4f min=%.4f max=%.4f valid=%d/%d",
                    n, mean, std, min, max, validCount, n);
        }
    }

    public static class IterationDetail {
        public final int iteration;
        public final double x;
        public final double runningMean;
        public final int validCountSoFar;

        public IterationDetail(int iteration, double x, double runningMean, int validCountSoFar) {
            this.iteration = iteration;
            this.x = x;
            this.runningMean = runningMean;
            this.validCountSoFar = validCountSoFar;
        }
    }
}
