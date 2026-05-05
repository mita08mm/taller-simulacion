package tss.act1.distributions;

import tss.act1.core.RandomSource;

public class ExponentialDistribution implements ContinuousDistribution {
    private final double lambda;

    public ExponentialDistribution(double lambda) {
        if (lambda <= 0) throw new IllegalArgumentException("lambda>0");
        this.lambda = lambda;
    }

    @Override
    public double sample(RandomSource rng) {
        double u = rng.nextDouble();
        return -Math.log(1.0 - u) / lambda; // mean = 1/lambda
    }
}
