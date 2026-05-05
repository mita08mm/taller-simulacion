package tss.act1.distributions;

import tss.act1.core.RandomSource;

/**
 * Uniform distribution where inputs are minutes; returns hours.
 */
public class UniformMinutesDistribution implements ContinuousDistribution {
    private final double aMin; // minutes
    private final double bMin; // minutes

    public UniformMinutesDistribution(double aMin, double bMin) {
        this.aMin = aMin;
        this.bMin = bMin;
    }

    @Override
    public double sample(RandomSource rng) {
        double u = rng.nextDouble();
        double minutes = aMin + u * (bMin - aMin);
        return minutes / 60.0; // convert to hours
    }
}
