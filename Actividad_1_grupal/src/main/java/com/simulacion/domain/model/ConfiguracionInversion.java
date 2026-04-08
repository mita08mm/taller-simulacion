package com.simulacion.domain.model;

/**
 * Value Object inmutable con la configuracion de la simulacion de inversion.
 */
public class ConfiguracionInversion {
    private final int numeroCorridas;
    private final int vidaFiscal;

    private final double afPesimista;
    private final double afProbable;
    private final double afOptimista;

    private final double acPesimista;
    private final double acProbable;
    private final double acOptimista;

    private final double flujoPesimista;
    private final double flujoProbable;
    private final double flujoOptimista;

    private final double tasaImpuestos;
    private final double trema;

    public ConfiguracionInversion(
        int numeroCorridas,
        int vidaFiscal,
        double afPesimista,
        double afProbable,
        double afOptimista,
        double acPesimista,
        double acProbable,
        double acOptimista,
        double flujoPesimista,
        double flujoProbable,
        double flujoOptimista,
        double tasaImpuestos,
        double trema
    ) {
        validar(
            numeroCorridas,
            vidaFiscal,
            afPesimista,
            afProbable,
            afOptimista,
            acPesimista,
            acProbable,
            acOptimista,
            flujoPesimista,
            flujoProbable,
            flujoOptimista,
            tasaImpuestos,
            trema
        );

        this.numeroCorridas = numeroCorridas;
        this.vidaFiscal = vidaFiscal;
        this.afPesimista = afPesimista;
        this.afProbable = afProbable;
        this.afOptimista = afOptimista;
        this.acPesimista = acPesimista;
        this.acProbable = acProbable;
        this.acOptimista = acOptimista;
        this.flujoPesimista = flujoPesimista;
        this.flujoProbable = flujoProbable;
        this.flujoOptimista = flujoOptimista;
        this.tasaImpuestos = tasaImpuestos;
        this.trema = trema;
    }

    private void validar(
        int numeroCorridas,
        int vidaFiscal,
        double afPesimista,
        double afProbable,
        double afOptimista,
        double acPesimista,
        double acProbable,
        double acOptimista,
        double flujoPesimista,
        double flujoProbable,
        double flujoOptimista,
        double tasaImpuestos,
        double trema
    ) {
        if (numeroCorridas <= 0) {
            throw new IllegalArgumentException("El numero de corridas debe ser mayor que 0");
        }
        if (vidaFiscal <= 0) {
            throw new IllegalArgumentException("La vida fiscal debe ser mayor que 0");
        }
        if (tasaImpuestos < 0 || tasaImpuestos > 1) {
            throw new IllegalArgumentException("La tasa de impuestos debe estar entre 0 y 1");
        }
        if (trema < 0) {
            throw new IllegalArgumentException("La TREMA debe ser mayor o igual que 0");
        }

        validarTriangular(afPesimista, afProbable, afOptimista, "activo fijo");
        validarTriangular(acPesimista, acProbable, acOptimista, "activo circulante");
        validarTriangular(flujoPesimista, flujoProbable, flujoOptimista, "flujo");
    }

    private void validarTriangular(double a, double b, double c, String nombre) {
        double min = Math.min(a, c);
        double max = Math.max(a, c);
        if (b < min || b > max) {
            throw new IllegalArgumentException(
                "El valor mas probable de " + nombre + " debe estar entre pesimista y optimista"
            );
        }
        if (a == c) {
            throw new IllegalArgumentException("Los extremos de la triangular de " + nombre + " no pueden ser iguales");
        }
    }

    public int getNumeroCorridas() {
        return numeroCorridas;
    }

    public int getVidaFiscal() {
        return vidaFiscal;
    }

    public double getAfPesimista() {
        return afPesimista;
    }

    public double getAfProbable() {
        return afProbable;
    }

    public double getAfOptimista() {
        return afOptimista;
    }

    public double getAcPesimista() {
        return acPesimista;
    }

    public double getAcProbable() {
        return acProbable;
    }

    public double getAcOptimista() {
        return acOptimista;
    }

    public double getFlujoPesimista() {
        return flujoPesimista;
    }

    public double getFlujoProbable() {
        return flujoProbable;
    }

    public double getFlujoOptimista() {
        return flujoOptimista;
    }

    public double getTasaImpuestos() {
        return tasaImpuestos;
    }

    public double getTrema() {
        return trema;
    }
}
