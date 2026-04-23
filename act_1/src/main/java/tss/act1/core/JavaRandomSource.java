package tss.act1.core;

import java.util.Random;

public final class JavaRandomSource implements RandomSource {
    private final Random random;

    public JavaRandomSource() {
        this.random = new Random();
    }

    public JavaRandomSource(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }
}
