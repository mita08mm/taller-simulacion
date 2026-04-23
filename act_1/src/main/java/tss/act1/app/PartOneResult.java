package tss.act1.app;

import java.util.List;

public record PartOneResult(
        int samples,
        double mean,
        double min,
        double max,
        List<InverseSample> samplePairs) {
}
