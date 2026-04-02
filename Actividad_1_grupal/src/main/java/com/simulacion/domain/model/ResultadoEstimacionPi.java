package com.simulacion.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa el resultado de una simulación para estimar π
 */
public class ResultadoEstimacionPi {
    private final int numeroPuntos;
    private final List<PuntoSimulado> puntos;
    private final int puntosDentro;
    private final double piEstimado;

    public ResultadoEstimacionPi(List<PuntoSimulado> puntos) {
        this.puntos = new ArrayList<>(puntos);
        this.numeroPuntos = puntos.size();
        this.puntosDentro = (int) puntos.stream()
            .filter(PuntoSimulado::isDentroDeLCirculo)
            .count();
        this.piEstimado = 4.0 * puntosDentro / numeroPuntos;
    }

    public int getNumeroPuntos() {
        return numeroPuntos;
    }

    public List<PuntoSimulado> getPuntos() {
        return Collections.unmodifiableList(puntos);
    }

    public int getPuntosDentro() {
        return puntosDentro;
    }

    public int getPuntosFuera() {
        return numeroPuntos - puntosDentro;
    }

    public double getPiEstimado() {
        return piEstimado;
    }

    public double getError() {
        return Math.abs(piEstimado - Math.PI);
    }

    public double getErrorPorcentual() {
        return (getError() / Math.PI) * 100;
    }
}
