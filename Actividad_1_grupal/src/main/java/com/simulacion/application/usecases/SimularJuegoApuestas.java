package com.simulacion.application.usecases;

import com.simulacion.domain.model.*;
import com.simulacion.infrastructure.random.GeneradorAleatorio;

import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso para simular el juego de volados
 * Aplica Single Responsibility Principle
 */
public class SimularJuegoApuestas {
    private final GeneradorAleatorio generador;

    public SimularJuegoApuestas(GeneradorAleatorio generador) {
        this.generador = generador;
    }

    /**
     * Ejecuta la simulación completa del juego de volados
     */
    public ResultadoSimulacionApuestas ejecutar(ConfiguracionApuestas config) {
        List<ResultadoCorrida> corridas = new ArrayList<>();

        for (int i = 1; i <= config.getNumeroCorridas(); i++) {
            ResultadoCorrida corrida = simularUnaCorrida(i, config);
            corridas.add(corrida);
        }

        return new ResultadoSimulacionApuestas(config, corridas);
    }

    /**
     * Simula una corrida completa hasta llegar a la meta o quebrar
     */
    private ResultadoCorrida simularUnaCorrida(int numeroCorrida, ConfiguracionApuestas config) {
        List<ResultadoVolado> volados = new ArrayList<>();
        int cantidadActual = config.getCantidadInicial();
        int siguienteApuesta = config.getApuestaInicial();

        while (cantidadActual < config.getMeta() && cantidadActual > 0) {
            double numeroAleatorio = generador.nextDouble();
            // Evento Bernoulli: ganar si U(0,1) < p.
            boolean gano = numeroAleatorio < config.getProbabilidadGanar();

            int cantidadAntes = cantidadActual;
            
            // Ajustar apuesta si es mayor que lo que se tiene
            int apuestaReal = Math.min(siguienteApuesta, cantidadActual);

            if (gano) {
                cantidadActual += apuestaReal;
                // Politica del contexto: al ganar, volver a apuesta base X.
                siguienteApuesta = config.getApuestaInicial(); // Resetear apuesta
            } else {
                cantidadActual -= apuestaReal;
                // Politica del contexto: al perder, doblar apuesta (2X, 4X, ...).
                siguienteApuesta = apuestaReal * 2; // Doblar la apuesta
            }

            ResultadoVolado volado = new ResultadoVolado(
                numeroAleatorio,
                cantidadAntes,
                apuestaReal,
                gano,
                cantidadActual
            );
            volados.add(volado);
        }

        ResultadoCorrida.EstadoFinal estado = cantidadActual >= config.getMeta()
            ? ResultadoCorrida.EstadoFinal.ALCANZA_META
            : ResultadoCorrida.EstadoFinal.QUIEBRA;

        return new ResultadoCorrida(numeroCorrida, volados, estado);
    }
}
