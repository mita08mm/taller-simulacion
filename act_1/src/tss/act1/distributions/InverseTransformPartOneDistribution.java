package tss.act1.distributions;

import tss.act1.core.RandomSource;

/**
 * Distribution for Part 1: f(x) = ((x-3)^2)/18 on [0,6]
 * Inverse CDF: x = 3 + cbrt(54*R - 27)
 */
public class InverseTransformPartOneDistribution implements ContinuousDistribution {

    @Override
    public double sample(RandomSource rng) {
        double r = rng.nextDouble();
        double rad = 54.0 * r - 27.0;
        // use Math.cbrt to handle negative values correctly
        return 3.0 + Math.cbrt(rad);
    }
}
