package tss.act1.distributions;

import tss.act1.core.RandomSource;

public final class UniformDistribution implements ContinuousDistribution, InverseCdfDistribution {
    private final double min;
    private final double max;

    public UniformDistribution(double min, double max) {
        if (max <= min) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        this.min = min;
        this.max = max;
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
        return min + (max - min) * u;
    }
}
