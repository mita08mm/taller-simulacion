package com.simulacion.infrastructure.random;

/**
 * Interfaz para generación de números aleatorios
 * Aplica Dependency Inversion Principle
 */
public interface GeneradorAleatorio {
    /**
     * Genera un número aleatorio uniforme entre 0 y 1
     */
    double nextDouble();
    
    /**
     * Establece la semilla para reproducibilidad
     */
    void setSeed(long seed);
}
