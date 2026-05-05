package com.simulacion.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Resultado agregado de la simulacion de inversion.
 */
public class ResultadoSimulacionInversion {
    private final ConfiguracionInversion configuracion;
    private final List<ResultadoCorridaInversion> corridas;
    private final double probabilidadExito;
    private final int corridasExitosas;

    public ResultadoSimulacionInversion(
            ConfiguracionInversion configuracion,
            List<ResultadoCorridaInversion> corridas) {
        this.configuracion = configuracion;
        this.corridas = new ArrayList<>(corridas);
        this.corridasExitosas = (int) corridas.stream().filter(ResultadoCorridaInversion::isSuperaTrema).count();
        this.probabilidadExito = (double) corridasExitosas / corridas.size();
    }

    public ConfiguracionInversion getConfiguracion() {
        return configuracion;
    }

    public List<ResultadoCorridaInversion> getCorridas() {
        return Collections.unmodifiableList(corridas);
    }

    public int getTotalCorridas() {
        return corridas.size();
    }

    public int getCorridasExitosas() {
        return corridasExitosas;
    }

    public double getProbabilidadExito() {
        return probabilidadExito;
    }

    public boolean aceptarProyecto() {
        return probabilidadExito >= 0.90;
    }

    public double getTirMinima() {
        return corridas.stream().mapToDouble(ResultadoCorridaInversion::getTirPorcentaje).min().orElse(0);
    }

    public double getTirMaxima() {
        return corridas.stream().mapToDouble(ResultadoCorridaInversion::getTirPorcentaje).max().orElse(0);
    }

    public double getTirMedia() {
        return corridas.stream().mapToDouble(ResultadoCorridaInversion::getTirPorcentaje).average().orElse(0);
    }

    public double getDesviacionEstandar() {
        double media = getTirMedia();
        double var = corridas.stream()
                .mapToDouble(c -> Math.pow(c.getTirPorcentaje() - media, 2))
                .average()
                .orElse(0);
        return Math.sqrt(var);
    }

    public double getPercentil(double p) {
        if (p < 0 || p > 1) {
            throw new IllegalArgumentException("El percentil debe estar entre 0 y 1");
        }
        List<Double> ordenados = corridas.stream()
                .map(ResultadoCorridaInversion::getTirPorcentaje)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (ordenados.isEmpty()) {
            return 0;
        }

        int index = (int) Math.ceil(p * ordenados.size()) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= ordenados.size()) {
            index = ordenados.size() - 1;
        }
        return ordenados.get(index);
    }
}
