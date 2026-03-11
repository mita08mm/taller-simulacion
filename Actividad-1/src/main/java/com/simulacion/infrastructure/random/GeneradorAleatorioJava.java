package com.simulacion.infrastructure.random;

import java.util.Random;

/**
 * Implementación del generador aleatorio usando java.util.Random
 */
public class GeneradorAleatorioJava implements GeneradorAleatorio {
    private Random random;

    public GeneradorAleatorioJava() {
        this.random = new Random();
    }

    public GeneradorAleatorioJava(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }

    @Override
    public void setSeed(long seed) {
        this.random = new Random(seed);
    }
}
