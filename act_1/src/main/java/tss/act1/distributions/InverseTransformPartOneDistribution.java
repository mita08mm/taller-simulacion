package tss.act1.distributions;

import tss.act1.core.RandomSource;

public final class InverseTransformPartOneDistribution implements ContinuousDistribution, InverseCdfDistribution {
    @Override
    public double sample(RandomSource random) {
        return inverseCdf(random.nextDouble());
    }

    @Override
    public double inverseCdf(double u) {
        if (u < 0.0 || u > 1.0) {
            throw new IllegalArgumentException("u must be in [0,1]");
        }
        // Inverse CDF for F(x)=((x-3)^3+27)/54, 0<=x<=6
        double x = 3.0 + Math.cbrt(54.0 * u - 27.0);
        if (x < 0.0) {
            return 0.0;
        }
        if (x > 6.0) {
            return 6.0;
        }
        return x;
    }
}
