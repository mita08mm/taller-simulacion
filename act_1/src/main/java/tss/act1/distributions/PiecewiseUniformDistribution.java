package tss.act1.distributions;

import java.util.ArrayList;
import java.util.List;

import tss.act1.core.RandomSource;
import tss.act1.model.IntervalProbability;

public final class PiecewiseUniformDistribution implements ContinuousDistribution, InverseCdfDistribution {
    private final List<IntervalProbability> intervals;
    private final double totalProbability;

    public PiecewiseUniformDistribution(List<IntervalProbability> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            throw new IllegalArgumentException("Intervals cannot be empty");
        }
        this.intervals = List.copyOf(new ArrayList<>(intervals));
        double sum = 0.0;
        for (IntervalProbability interval : this.intervals) {
            sum += interval.probability();
        }
        if (Math.abs(sum - 1.0) > 1e-9) {
            throw new IllegalArgumentException("Interval probabilities must sum to 1.0");
        }
        this.totalProbability = sum;
    }

    @Override
    public double sample(RandomSource random) {
        return inverseCdf(random.nextDouble());
    }

    @Override
    public double inverseCdf(double u) {
        if (u < 0.0 || u > 1.0) {
            throw new IllegalArgumentException("u must be in [0,1]");
        }

        double scaled = u * totalProbability;
        double cumulative = 0.0;
        for (IntervalProbability interval : intervals) {
            double previous = cumulative;
            cumulative += interval.probability();
            if (scaled <= cumulative) {
                double localU = (scaled - previous) / interval.probability();
                return interval.minInclusive() + localU * interval.width();
            }
        }

        IntervalProbability last = intervals.get(intervals.size() - 1);
        return last.maxExclusive();
    }
}
