package com.simulacion.application.usecases;

import com.simulacion.domain.model.ConfiguracionInversion;
import com.simulacion.domain.model.ResultadoCorridaInversion;
import com.simulacion.domain.model.ResultadoSimulacionInversion;
import com.simulacion.infrastructure.random.GeneradorAleatorio;
import com.simulacion.infrastructure.random.GeneradorAleatorioJava;

import java.util.ArrayList;
import java.util.List;

public class SimularProyectoInversion {
    private static final double[] INFLACION_A = { 18, 18, 22, 25, 28 };
    private static final double[] INFLACION_B = { 15, 15, 18, 20, 22 };
    private static final double[] INFLACION_C = { 12, 12, 15, 18, 19 };

    private final GeneradorAleatorio generador;

    public SimularProyectoInversion() {
        this(new GeneradorAleatorioJava());
    }

    public SimularProyectoInversion(GeneradorAleatorio generador) {
        this.generador = generador;
    }

    public ResultadoSimulacionInversion ejecutar(ConfiguracionInversion config) {
        List<ResultadoCorridaInversion> corridas = new ArrayList<>();

        for (int i = 1; i <= config.getNumeroCorridas(); i++) {
            corridas.add(simularCorrida(i, config));
        }

        return new ResultadoSimulacionInversion(config, corridas);
    }

    private ResultadoCorridaInversion simularCorrida(int numeroCorrida, ConfiguracionInversion config) {
        double activoFijoInicial = triangular(
                config.getAfPesimista(),
                config.getAfProbable(),
                config.getAfOptimista());
        double activoCirculanteInicial = triangular(
                config.getAcPesimista(),
                config.getAcProbable(),
                config.getAcOptimista());
        double flujoBaseAntesImpuestos = triangular(
                config.getFlujoPesimista(),
                config.getFlujoProbable(),
                config.getFlujoOptimista());

        double depreciacionAnual = depreciacionAnual(activoFijoInicial, config.getVidaFiscal());
        double factorInflacion = 1.0;
        double[] flujosConstantes = new double[config.getVidaFiscal()];

        for (int year = 0; year < config.getVidaFiscal(); year++) {
            int ix = Math.min(year, INFLACION_A.length - 1);
            double inflacionAnual = triangular(INFLACION_A[ix], INFLACION_B[ix], INFLACION_C[ix]) / 100.0;
            factorInflacion = factorInflacion(factorInflacion, inflacionAnual);

            flujosConstantes[year] = flujoConstanteDespuesDeImpuestos(
                    flujoBaseAntesImpuestos,
                    factorInflacion,
                    depreciacionAnual,
                    config.getTasaImpuestos());
        }

        double valorRescateConstante = valorRescateConstante(
                activoFijoInicial,
                activoCirculanteInicial,
                config.getTasaImpuestos(),
                factorInflacion);
        double inversionInicial = inversionInicial(activoFijoInicial, activoCirculanteInicial);

        double tir = tir(inversionInicial, flujosConstantes, valorRescateConstante);
        double tirPorcentaje = tir * 100;
        double vpn = vpn(config.getTrema() / 100.0, inversionInicial, flujosConstantes, valorRescateConstante);
        boolean superaTrema = tirPorcentaje > config.getTrema();

        return new ResultadoCorridaInversion(
                numeroCorrida,
                activoFijoInicial,
                activoCirculanteInicial,
                flujoBaseAntesImpuestos,
                vpn,
                tirPorcentaje,
                superaTrema);
    }

    private double triangular(double pesimista, double probable, double optimista) {
        double a = pesimista;
        double b = probable;
        double c = optimista;

        boolean negativos = (a < 0 && c < 0);
        if (negativos) {
            double tmp = Math.abs(c);
            c = Math.abs(a);
            a = tmp;
            b = Math.abs(b);
        }

        double numeroAleatorio = generador.nextDouble();
        double umbralRamaIzquierda = (b - a) / (c - a);
        double x = numeroAleatorio <= umbralRamaIzquierda
                ? a + Math.sqrt((c - a) * (b - a) * numeroAleatorio)
                : c - Math.sqrt((c - a) * (c - b) * (1 - numeroAleatorio));

        return negativos ? -x : x;
    }

    private double tir(double inversion, double[] flujos, double vr) {
        double lo = -0.99;
        double hi = 9.0;
        double flo = vpn(lo, inversion, flujos, vr);
        double fhi = vpn(hi, inversion, flujos, vr);

        if (flo * fhi > 0) {
            return Math.abs(flo) < Math.abs(fhi) ? lo : hi;
        }

        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2;
            double fm = vpn(mid, inversion, flujos, vr);
            if (Math.abs(fm) < 1e-6) {
                return mid;
            }
            if (flo * fm < 0) {
                hi = mid;
            } else {
                lo = mid;
                flo = fm;
            }
        }

        return (lo + hi) / 2;
    }

    private double vpn(double tasa, double inversion, double[] flujos, double vr) {
        double valor = inversion;
        for (int t = 0; t < flujos.length; t++) {
            valor += flujos[t] / Math.pow(1 + tasa, t + 1);
        }
        valor += vr / Math.pow(1 + tasa, flujos.length);
        return valor;
    }

    private double depreciacionAnual(double activoFijoInicial, int vidaFiscal) {
        return Math.abs(activoFijoInicial) / vidaFiscal;
    }

    private double factorInflacion(double factorActual, double inflacionAnual) {
        return factorActual * (1 + inflacionAnual);
    }

    private double flujoConstanteDespuesDeImpuestos(
            double flujoBaseAntesImpuestos,
            double factorInflacionAcumulado,
            double depreciacionAnual,
            double tasaImpuestos) {
        double flujoCorriente = flujoBaseAntesImpuestos * factorInflacionAcumulado;
        double impuesto = Math.max(0, (flujoCorriente - depreciacionAnual) * tasaImpuestos);
        return (flujoCorriente - impuesto) / factorInflacionAcumulado;
    }

    private double valorRescateConstante(
            double activoFijoInicial,
            double activoCirculanteInicial,
            double tasaImpuestos,
            double factorInflacionAcumulado) {
        return (Math.abs(activoCirculanteInicial)
                + Math.abs(activoFijoInicial) * 0.20 * (1 - tasaImpuestos)) / factorInflacionAcumulado;
    }

    private double inversionInicial(double activoFijoInicial, double activoCirculanteInicial) {
        return activoFijoInicial + activoCirculanteInicial;
    }
}
