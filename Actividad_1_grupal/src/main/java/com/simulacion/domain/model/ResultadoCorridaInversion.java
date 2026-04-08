package com.simulacion.domain.model;

/**
 * Resultado de una corrida individual de inversion.
 */
public class ResultadoCorridaInversion {
    private final int numeroCorrida;
    private final double activoFijo;
    private final double activoCirculante;
    private final double flujoAntesImpuestos;
    private final double vpn;
    private final double tirPorcentaje;
    private final boolean superaTrema;

    public ResultadoCorridaInversion(
        int numeroCorrida,
        double activoFijo,
        double activoCirculante,
        double flujoAntesImpuestos,
        double vpn,
        double tirPorcentaje,
        boolean superaTrema
    ) {
        this.numeroCorrida = numeroCorrida;
        this.activoFijo = activoFijo;
        this.activoCirculante = activoCirculante;
        this.flujoAntesImpuestos = flujoAntesImpuestos;
        this.vpn = vpn;
        this.tirPorcentaje = tirPorcentaje;
        this.superaTrema = superaTrema;
    }

    public int getNumeroCorrida() {
        return numeroCorrida;
    }

    public double getActivoFijo() {
        return activoFijo;
    }

    public double getActivoCirculante() {
        return activoCirculante;
    }

    public double getFlujoAntesImpuestos() {
        return flujoAntesImpuestos;
    }

    public double getVpn() {
        return vpn;
    }

    public double getTirPorcentaje() {
        return tirPorcentaje;
    }

    public boolean isSuperaTrema() {
        return superaTrema;
    }
}
