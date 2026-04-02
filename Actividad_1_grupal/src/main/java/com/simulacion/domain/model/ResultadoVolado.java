package com.simulacion.domain.model;

/**
 * Representa el resultado de un lanzamiento de volado individual
 * Aplica principio de Single Responsibility: solo maneja datos de un volado
 */
public class ResultadoVolado {
    private final double numeroAleatorio;
    private final int cantidadAntesDelVolado;
    private final int apuesta;
    private final boolean gano;
    private final int cantidadDespuesDelVolado;

    public ResultadoVolado(double numeroAleatorio, int cantidadAntesDelVolado, 
                          int apuesta, boolean gano, int cantidadDespuesDelVolado) {
        this.numeroAleatorio = numeroAleatorio;
        this.cantidadAntesDelVolado = cantidadAntesDelVolado;
        this.apuesta = apuesta;
        this.gano = gano;
        this.cantidadDespuesDelVolado = cantidadDespuesDelVolado;
    }

    public double getNumeroAleatorio() {
        return numeroAleatorio;
    }

    public int getCantidadAntesDelVolado() {
        return cantidadAntesDelVolado;
    }

    public int getApuesta() {
        return apuesta;
    }

    public boolean isGano() {
        return gano;
    }

    public int getCantidadDespuesDelVolado() {
        return cantidadDespuesDelVolado;
    }
}
