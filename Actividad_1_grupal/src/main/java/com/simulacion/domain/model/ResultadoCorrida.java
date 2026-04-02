package com.simulacion.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa el resultado completo de una corrida del juego de volados
 * Una corrida termina cuando se alcanza la meta o se quiebra
 */
public class ResultadoCorrida {
    private final int numeroCorrida;
    private final List<ResultadoVolado> volados;
    private final EstadoFinal estadoFinal;

    public enum EstadoFinal {
        ALCANZA_META,
        QUIEBRA
    }

    public ResultadoCorrida(int numeroCorrida, List<ResultadoVolado> volados, EstadoFinal estadoFinal) {
        this.numeroCorrida = numeroCorrida;
        this.volados = new ArrayList<>(volados);
        this.estadoFinal = estadoFinal;
    }

    public int getNumeroCorrida() {
        return numeroCorrida;
    }

    public List<ResultadoVolado> getVolados() {
        return Collections.unmodifiableList(volados);
    }

    public EstadoFinal getEstadoFinal() {
        return estadoFinal;
    }

    public boolean alcanzaMeta() {
        return estadoFinal == EstadoFinal.ALCANZA_META;
    }

    public int getCantidadVolados() {
        return volados.size();
    }
}
