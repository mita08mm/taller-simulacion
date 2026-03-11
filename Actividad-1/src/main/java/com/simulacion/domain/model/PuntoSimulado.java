package com.simulacion.domain.model;

/**
 * Representa un punto simulado en el cuadrado unitario
 */
public class PuntoSimulado {
    private final int numero;
    private final double r1;
    private final double r2;
    private final double distancia;
    private final boolean dentroDeLCirculo;

    public PuntoSimulado(int numero, double r1, double r2) {
        this.numero = numero;
        this.r1 = r1;
        this.r2 = r2;
        this.distancia = Math.sqrt(r1 * r1 + r2 * r2);
        this.dentroDeLCirculo = distancia <= 1.0;
    }

    public int getNumero() {
        return numero;
    }

    public double getR1() {
        return r1;
    }

    public double getR2() {
        return r2;
    }

    public double getDistancia() {
        return distancia;
    }

    public boolean isDentroDeLCirculo() {
        return dentroDeLCirculo;
    }
}
