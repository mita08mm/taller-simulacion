package com.simulacion.domain.model;

/**
 * Resultado de una corrida individual de inversion.
 * Incluye valores intermedios por año para poder mostrar tabla 5.4 detallada.
 */
public class ResultadoCorridaInversion {
    private final int numeroCorrida;
    private final double activoFijo;
    private final double activoCirculante;
    private final double flujoAntesImpuestos;
    private final double vpn;
    private final double tirPorcentaje;
    private final boolean superaTrema;

    // Valores por año para tabla detallada (Tabla 5.4)
    private final double[] tasasInflacion;
    private final double[] inversionAdicionalAC;
    private final double[] flujoAntesImpuestoCorriente;
    private final double[] depreciacion;
    private final double[] ingresoGravable;
    private final double[] impuestos;
    private final double[] flujoDespuesImpuestoCorriente;
    private final double[] flujoDespuesImpuestesConstante;
    private final double productoInflacionTotal;
    private final double valorRescate;

    public ResultadoCorridaInversion(
        int numeroCorrida,
        double activoFijo,
        double activoCirculante,
        double flujoAntesImpuestos,
        double vpn,
        double tirPorcentaje,
        boolean superaTrema,
        double[] tasasInflacion,
        double[] inversionAdicionalAC,
        double[] flujoAntesImpuestoCorriente,
        double[] depreciacion,
        double[] ingresoGravable,
        double[] impuestos,
        double[] flujoDespuesImpuestoCorriente,
        double[] flujoDespuesImpuestesConstante,
        double productoInflacionTotal,
        double valorRescate
    ) {
        this.numeroCorrida = numeroCorrida;
        this.activoFijo = activoFijo;
        this.activoCirculante = activoCirculante;
        this.flujoAntesImpuestos = flujoAntesImpuestos;
        this.vpn = vpn;
        this.tirPorcentaje = tirPorcentaje;
        this.superaTrema = superaTrema;
        this.tasasInflacion = tasasInflacion;
        this.inversionAdicionalAC = inversionAdicionalAC;
        this.flujoAntesImpuestoCorriente = flujoAntesImpuestoCorriente;
        this.depreciacion = depreciacion;
        this.ingresoGravable = ingresoGravable;
        this.impuestos = impuestos;
        this.flujoDespuesImpuestoCorriente = flujoDespuesImpuestoCorriente;
        this.flujoDespuesImpuestesConstante = flujoDespuesImpuestesConstante;
        this.productoInflacionTotal = productoInflacionTotal;
        this.valorRescate = valorRescate;
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

    public double[] getTasasInflacion() {
        return tasasInflacion;
    }

    public double[] getInversionAdicionalAC() {
        return inversionAdicionalAC;
    }

    public double[] getFlujoAntesImpuestoCorriente() {
        return flujoAntesImpuestoCorriente;
    }

    public double[] getDepreciacion() {
        return depreciacion;
    }

    public double[] getIngresoGravable() {
        return ingresoGravable;
    }

    public double[] getImpuestos() {
        return impuestos;
    }

    public double[] getFlujoDespuesImpuestoCorriente() {
        return flujoDespuesImpuestoCorriente;
    }

    public double[] getFlujoDespuesImpuestesConstante() {
        return flujoDespuesImpuestesConstante;
    }

    public double getProductoInflacionTotal() {
        return productoInflacionTotal;
    }

    public double getValorRescate() {
        return valorRescate;
    }
}
