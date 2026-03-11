package com.simulacion.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa el resultado completo de una simulación de múltiples corridas
 */
public class ResultadoSimulacionApuestas {
    private final ConfiguracionApuestas configuracion;
    private final List<ResultadoCorrida> corridas;
    private final int metasAlcanzadas;
    private final int quiebras;
    private final double probabilidadExito;

    public ResultadoSimulacionApuestas(ConfiguracionApuestas configuracion,
                                      List<ResultadoCorrida> corridas) {
        this.configuracion = configuracion;
        this.corridas = new ArrayList<>(corridas);
        this.metasAlcanzadas = (int) corridas.stream()
            .filter(ResultadoCorrida::alcanzaMeta)
            .count();
        this.quiebras = corridas.size() - metasAlcanzadas;
        this.probabilidadExito = (double) metasAlcanzadas / corridas.size();
    }

    public ConfiguracionApuestas getConfiguracion() {
        return configuracion;
    }

    public List<ResultadoCorrida> getCorridas() {
        return Collections.unmodifiableList(corridas);
    }

    public int getMetasAlcanzadas() {
        return metasAlcanzadas;
    }

    public int getQuiebras() {
        return quiebras;
    }

    public double getProbabilidadExito() {
        return probabilidadExito;
    }

    public int getTotalCorridas() {
        return corridas.size();
    }
}
