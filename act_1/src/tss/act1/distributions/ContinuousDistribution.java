package tss.act1.distributions;

import tss.act1.core.RandomSource;

public interface ContinuousDistribution {
    double sample(RandomSource rng);
}
