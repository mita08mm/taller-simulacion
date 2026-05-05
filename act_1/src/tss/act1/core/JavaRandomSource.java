package tss.act1.core;

import java.util.Random;

public class JavaRandomSource implements RandomSource {
    private final Random rnd;

    public JavaRandomSource() {
        this.rnd = new Random();
    }

    public JavaRandomSource(long seed) {
        this.rnd = new Random(seed);
    }

    @Override
    public double nextDouble() {
        return rnd.nextDouble();
    }
}
