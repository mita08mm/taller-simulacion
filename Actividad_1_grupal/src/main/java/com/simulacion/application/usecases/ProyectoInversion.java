package com.simulacion.application.usecases;

import com.simulacion.domain.model.ConfiguracionInversion;
import com.simulacion.domain.model.ConfiguracionInversion.ModoSimulacion;
import com.simulacion.domain.model.ResultadoCorridaInversion;
import com.simulacion.domain.model.ResultadoSimulacionInversion;
import com.simulacion.infrastructure.random.GeneradorAleatorio;
import com.simulacion.infrastructure.random.GeneradorAleatorioJava;

import java.util.ArrayList;
import java.util.List;

public class ProyectoInversion {
    private static final double PORC_RESC_AF = 0.20;
    private static final double TASA_MINIMA_BISECCION = -0.99;
    private static final double TASA_MAXIMA_BISECCION = 9.0;
    private static final int MAX_ITERACIONES_BISECCION = 200;
    private static final double TOLERANCIA_BISECCION = 1e-6;

    private static final double[] INF_A = { 18, 18, 22, 25, 28 };
    private static final double[] INF_B = { 15, 15, 18, 20, 22 };
    private static final double[] INF_C = { 12, 12, 15, 18, 19 };

    private final GeneradorAleatorio generador;

    public ProyectoInversion() {
        this(new GeneradorAleatorioJava());
    }

    public ProyectoInversion(GeneradorAleatorio generador) {
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
        boolean pesFijo = config.getModoSimulacion() == ModoSimulacion.PESIMISTA_FIJO;

        double AF = selVal(
                pesFijo,
                config.getAfPesimista(),
                config.getAfProbable(),
                config.getAfOptimista());
        double AC = selVal(
                pesFijo,
                config.getAcPesimista(),
                config.getAcProbable(),
                config.getAcOptimista());
        double x_t = selVal(
                pesFijo,
                config.getFlujoPesimista(),
                config.getFlujoProbable(),
                config.getFlujoOptimista());

        double prodAcum_t = 1;
        double invAdicAC_t = 0;
        double[] flujosCte = new double[config.getVidaFiscal()];
        double T = config.getTasaImpuestos();

        for (int t = 0; t < config.getVidaFiscal(); t++) {
            int idxInf = Math.min(t, INF_A.length - 1);
            double i_t = selVal(
                    pesFijo,
                    INF_A[idxInf],
                    INF_B[idxInf],
                    INF_C[idxInf]) / 100.0;
            if (t == 0) {
                prodAcum_t = 1 + i_t;
                invAdicAC_t = Math.abs(AC) * i_t;
            } else {
                prodAcum_t = acumInf(prodAcum_t, i_t);
                invAdicAC_t = invAdicAC_t * (1 + i_t);
            }

            double s_t = flujoConstante(
                    x_t,
                    prodAcum_t,
                    invAdicAC_t,
                    AF,
                    T);
            flujosCte[t] = s_t;
        }

        double VR = valorRescateConstante(AF, AC, T);

        double I0 = inversionInicial(AF, AC);
        double TIR = tir(I0, flujosCte, VR);
        double tirPorcentaje = TIR * 100;
        double vpn = vpn(config.getTrema() / 100.0, I0, flujosCte, VR);
        boolean superaTrema = tirPorcentaje > config.getTrema();
        return new ResultadoCorridaInversion(
                numeroCorrida,
                AF,
                AC,
                x_t,
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

    private double selVal(
            boolean pesFijo,
            double pesimista,
            double probable,
            double optimista) {
        if (pesFijo) {
            return pesimista;
        }
        return triangular(pesimista, probable, optimista);
    }

    private double tir(double inversion, double[] flujos, double vr) {
        double tasaMinimaBiseccion = TASA_MINIMA_BISECCION;
        double tasaMaximaBiseccion = TASA_MAXIMA_BISECCION;
        double vpnEnTasaMinima = vpn(tasaMinimaBiseccion, inversion, flujos, vr);
        double vpnEnTasaMaxima = vpn(tasaMaximaBiseccion, inversion, flujos, vr);

        if (vpnEnTasaMinima * vpnEnTasaMaxima > 0) {
            return Math.abs(vpnEnTasaMinima) < Math.abs(vpnEnTasaMaxima)
                    ? tasaMinimaBiseccion
                    : tasaMaximaBiseccion;
        }

        for (int iteracion = 0; iteracion < MAX_ITERACIONES_BISECCION; iteracion++) {
            double tasaMedia = (tasaMinimaBiseccion + tasaMaximaBiseccion) / 2;
            double vpnEnTasaMedia = vpn(tasaMedia, inversion, flujos, vr);
            if (Math.abs(vpnEnTasaMedia) < TOLERANCIA_BISECCION) {
                return tasaMedia;
            }
            if (vpnEnTasaMinima * vpnEnTasaMedia < 0) {
                tasaMaximaBiseccion = tasaMedia;
            } else {
                tasaMinimaBiseccion = tasaMedia;
                vpnEnTasaMinima = vpnEnTasaMedia;
            }
        }

        return (tasaMinimaBiseccion + tasaMaximaBiseccion) / 2;
    }

    private double vpn(double tasa, double inversion, double[] flujos, double vr) {
        double valor = inversion;
        for (int t = 0; t < flujos.length; t++) {
            valor += flujos[t] / Math.pow(1 + tasa, t + 1);
        }
        valor += vr / Math.pow(1 + tasa, flujos.length);
        return valor;
    }

    private double acumInf(double factorActual, double infAnual) {
        return factorActual * (1 + infAnual);
    }

    private double flujoConstante(
            double x_t, double prodAcum_t, double invAdicAC_t,
            double AF, double T) {
        double flujoNetoImp = x_t * prodAcum_t * (1 - T);
        double escFiscalDep = (PORC_RESC_AF * Math.abs(AF)) * T;
        double s_t = (flujoNetoImp + escFiscalDep - invAdicAC_t) / prodAcum_t;

        return s_t;
    }

    private double valorRescateConstante(
            double AF,
            double AC,
            double T) {
        return Math.abs(AC)
                + Math.abs(AF) * PORC_RESC_AF * (1 - T);
    }

    private double inversionInicial(double AF, double AC) {
        return AF + AC;
    }
}
