package tss.act1.distributions;

import tss.act1.core.RandomSource;

/**
 * Piecewise distribution defined by intervals with probabilities.
 * Intervals: double[][] where each row = {a, b, prob}
 */
public class PiecewiseUniformDistribution implements ContinuousDistribution {
    private final double[] a;
    private final double[] b;
    private final double[] cum;

    public PiecewiseUniformDistribution(double[][] intervals) {
        int n = intervals.length;
        a = new double[n];
        b = new double[n];
        cum = new double[n];
        double s = 0.0;
        for (int i = 0; i < n; i++) {
            a[i] = intervals[i][0];
            b[i] = intervals[i][1];
            double p = intervals[i][2];
            s += p;
            cum[i] = s;
        }
        // assume probabilities sum to 1.0 (caller responsibility)
    }

    @Override
    public double sample(RandomSource rng) {
        double r = rng.nextDouble();
        double prev = 0.0;
        for (int i = 0; i < cum.length; i++) {
            if (r <= cum[i]) {
                double local = (r - prev) / (cum[i] - prev);
                return a[i] + local * (b[i] - a[i]);
            }
            prev = cum[i];
        }
        // fallback to last interval
        int last = cum.length - 1;
        return a[last] + rng.nextDouble() * (b[last] - a[last]);
    }
}
