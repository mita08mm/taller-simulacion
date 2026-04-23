package tss.act1.distributions;

import tss.act1.core.RandomSource;

public final class ExponentialDistribution implements ContinuousDistribution, InverseCdfDistribution {
    private final double ratePerHour;

    public ExponentialDistribution(double ratePerHour) {
        if (ratePerHour <= 0.0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
        this.ratePerHour = ratePerHour;
    }

    @Override
    public double sample(RandomSource random) {
        double u = random.nextDouble();
        return inverseCdf(u);
    }

    @Override
    public double inverseCdf(double u) {
        if (u < 0.0 || u > 1.0) {
            throw new IllegalArgumentException("u must be in [0,1]");
        }
        double adjusted = (u <= 0.0) ? Double.MIN_VALUE : u;
        return -Math.log(1.0 - adjusted) / ratePerHour;
    }
}
