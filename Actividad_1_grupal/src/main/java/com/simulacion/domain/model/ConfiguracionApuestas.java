package com.simulacion.domain.model;

/**
 * Value Object que contiene la configuración inicial para la simulación de apuestas
 * Inmutable y con validaciones
 */
public class ConfiguracionApuestas {
    private final int cantidadInicial;
    private final int apuestaInicial;
    private final int meta;
    private final double probabilidadGanar;
    private final int numeroCorridas;

    public ConfiguracionApuestas(int cantidadInicial, int apuestaInicial, 
                                int meta, double probabilidadGanar, int numeroCorridas) {
        validar(cantidadInicial, apuestaInicial, meta, probabilidadGanar, numeroCorridas);
        
        this.cantidadInicial = cantidadInicial;
        this.apuestaInicial = apuestaInicial;
        this.meta = meta;
        this.probabilidadGanar = probabilidadGanar;
        this.numeroCorridas = numeroCorridas;
    }

    private void validar(int cantidadInicial, int apuestaInicial, int meta, 
                        double probabilidadGanar, int numeroCorridas) {
        if (cantidadInicial <= 0) {
            throw new IllegalArgumentException("La cantidad inicial debe ser mayor que 0");
        }
        if (apuestaInicial <= 0) {
            throw new IllegalArgumentException("La apuesta inicial debe ser mayor que 0");
        }
        if (meta <= cantidadInicial) {
            throw new IllegalArgumentException("La meta debe ser mayor que la cantidad inicial");
        }
        if (probabilidadGanar < 0 || probabilidadGanar > 1) {
            throw new IllegalArgumentException("La probabilidad debe estar entre 0 y 1");
        }
        if (numeroCorridas <= 0) {
            throw new IllegalArgumentException("El número de corridas debe ser mayor que 0");
        }
    }

    public int getCantidadInicial() {
        return cantidadInicial;
    }

    public int getApuestaInicial() {
        return apuestaInicial;
    }

    public int getMeta() {
        return meta;
    }

    public double getProbabilidadGanar() {
        return probabilidadGanar;
    }

    public int getNumeroCorridas() {
        return numeroCorridas;
    }
}
