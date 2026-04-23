package tss.act1.model;

public record IntervalProbability(double minInclusive, double maxExclusive, double probability) {
    public IntervalProbability {
        if (maxExclusive <= minInclusive) {
            throw new IllegalArgumentException("Invalid interval bounds");
        }
        if (probability <= 0.0) {
            throw new IllegalArgumentException("Probability must be positive");
        }
    }

    public double width() {
        return maxExclusive - minInclusive;
    }
}