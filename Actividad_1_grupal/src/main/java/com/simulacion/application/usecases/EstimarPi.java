package com.simulacion.application.usecases;

import com.simulacion.domain.model.PuntoSimulado;
import com.simulacion.domain.model.ResultadoEstimacionPi;
import com.simulacion.infrastructure.random.GeneradorAleatorio;

import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso para estimar el valor de π usando el método de Monte Carlo
 * Aplica Single Responsibility Principle
 */
public class EstimarPi {
    private final GeneradorAleatorio generador;

    public EstimarPi(GeneradorAleatorio generador) {
        this.generador = generador;
    }

    /**
     * Ejecuta la simulación para estimar π
     * @param numeroPuntos Cantidad de puntos a simular
     */
    public ResultadoEstimacionPi ejecutar(int numeroPuntos) {
        if (numeroPuntos <= 0) {
            throw new IllegalArgumentException("El número de puntos debe ser mayor que 0");
        }

        List<PuntoSimulado> puntos = new ArrayList<>();

        for (int i = 1; i <= numeroPuntos; i++) {
            double r1 = generador.nextDouble();
            double r2 = generador.nextDouble();
            
            PuntoSimulado punto = new PuntoSimulado(i, r1, r2);
            puntos.add(punto);
        }

        return new ResultadoEstimacionPi(puntos);
    }

    /**
     * Calcula el número de corridas necesarias para alcanzar una precisión específica
     * basado en el teorema del límite central
     * 
     * @param errorMaximo Error máximo permitido (ej: 0.1)
     * @param nivelConfianza Nivel de confianza (ej: 0.95 para 95%)
     * @return Número de corridas recomendadas
     */
    public int calcularCorridasNecesarias(double errorMaximo, double nivelConfianza) {
        // Para 95% de confianza, Z = 1.96
        double z = 1.96;
        if (nivelConfianza >= 0.99) {
            z = 2.576;
        } else if (nivelConfianza >= 0.95) {
            z = 1.96;
        } else if (nivelConfianza >= 0.90) {
            z = 1.645;
        }

        // Varianza de x/n es π/4(1 - π/4)/n
        // Fórmula: n = 4π(1-π/4) * Z² / error²
        double pi = Math.PI;
        double n = (4 * pi * (1 - pi/4) * z * z) / (errorMaximo * errorMaximo);
        
        return (int) Math.ceil(n);
    }
}
