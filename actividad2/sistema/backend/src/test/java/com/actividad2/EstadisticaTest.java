package com.actividad2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de caja blanca de Estadistica.java: se conocen los calculos
 * internos (media, S, chi-cuadrada) y se verifican contra valores calculados
 * a mano o con el ejemplo ilustrativo de Informe.tex (seccion 2.5).
 */
class EstadisticaTest {

    @Test
    void mediaDeUnaLista() {
        assertEquals(2.1, Estadistica.media(List.of(1.0, 2.0, 2.0, 3.0, 1.0, 4.0, 2.0, 3.0, 2.0, 1.0)), 1e-9);
    }

    @Test
    void desviacionEstandarConUnSoloDatoEsCero() {
        assertEquals(0, Estadistica.desviacionEstandar(List.of(5.0)));
    }

    @Test
    void desviacionEstandarCalculada() {
        // mismos 10 datos que mediaDeUnaLista(): S = sqrt(8.9/9) = 0.9944...
        List<Double> datos = List.of(1.0, 2.0, 2.0, 3.0, 1.0, 4.0, 2.0, 3.0, 2.0, 1.0);
        assertEquals(0.9944, Estadistica.desviacionEstandar(datos), 1e-4);
    }

    @Test
    void medianaImpar() {
        assertEquals(3.0, Estadistica.mediana(List.of(5.0, 1.0, 3.0, 2.0, 4.0)));
    }

    @Test
    void medianaPar() {
        assertEquals(2.5, Estadistica.mediana(List.of(1.0, 2.0, 3.0, 4.0)));
    }

    @Test
    void medianaListaVaciaEsNaN() {
        assertTrue(Double.isNaN(Estadistica.mediana(List.of())));
    }

    @Test
    void tValor95DentroDeTabla() {
        assertEquals(2.262, Estadistica.tValor95(9), 1e-9); // gl=9, igual que el ejemplo del informe
    }

    @Test
    void tValor95FueraDeTablaUsaZ() {
        assertEquals(1.960, Estadistica.tValor95(31));
        assertEquals(1.960, Estadistica.tValor95(0)); // gl invalido tambien cae al caso por defecto
    }

    @Test
    void chiTeorico05DentroYFueraDeTabla() {
        assertEquals(15.507, Estadistica.chiTeorico05(8), 1e-9);
        assertTrue(Double.isNaN(Estadistica.chiTeorico05(31)));
    }

    @Test
    void intervaloConfianzaConMenosDeDosDatos() {
        Map<String, Object> r0 = Estadistica.intervaloConfianza95(List.of());
        assertEquals(0, r0.get("n"));
        assertNull(r0.get("media"));

        Map<String, Object> r1 = Estadistica.intervaloConfianza95(List.of(7.0));
        assertEquals(1, r1.get("n"));
        assertEquals(7.0, r1.get("media"));
        assertNull(r1.get("limiteInferior")); // con un solo dato no hay margen de error
    }

    @Test
    void intervaloConfianza95ConElEjemploDelInforme() {
        // Mismo ejemplo de Informe.tex 2.5 (tiempo al primer empleo, n=10): x=2.1, S=0.9944, t_9=2.262
        List<Double> datos = List.of(1.0, 2.0, 2.0, 3.0, 1.0, 4.0, 2.0, 3.0, 2.0, 1.0);
        Map<String, Object> r = Estadistica.intervaloConfianza95(datos);
        assertEquals(10, r.get("n"));
        assertEquals(2.1, (double) r.get("media"), 1e-9);
        assertEquals(0.71, (double) r.get("margenError"), 0.01);
        assertEquals(1.39, (double) r.get("limiteInferior"), 0.01);
        assertEquals(2.81, (double) r.get("limiteSuperior"), 0.01);
    }

    @Test
    void proporcionConTotalCero() {
        Map<String, Object> r = Estadistica.proporcion(0, 0);
        assertEquals(0.0, r.get("proporcion"));
    }

    @Test
    void proporcionNormal() {
        Map<String, Object> r = Estadistica.proporcion(83, 100);
        assertEquals(0.83, (double) r.get("proporcion"), 1e-9);
    }

    @Test
    void chiCuadradaConMenosDeDosCategoriasNoAplicaLaPrueba() {
        // Solo una categoria en B: no se puede construir una tabla 2x2, la funcion debe avisar en vez de fallar
        List<String[]> pares = List.of(new String[]{"a", "x"}, new String[]{"b", "x"});
        Map<String, Object> r = Estadistica.chiCuadrada(pares, List.of("a", "b"), List.of("x", "y"));
        assertNull(r.get("chiCuadrada"));
        assertNull(r.get("independientes"));
        assertEquals(0, r.get("gradosLibertad"));
    }

    @Test
    void chiCuadradaDelEjemploDelInforme() {
        // Reconstruye el ejemplo ilustrativo del Informe.tex 2.5 (financiamiento x programa, N=30, X0^2 ~= 2.21)
        List<String> financiamiento = List.of("recursos", "beca_total", "beca_parcial");
        List<String> programa = List.of("diplomado", "especialidad", "maestria", "doctorado", "posdoctorado");
        int[][] fo = {
            {2, 1, 4, 1, 0},
            {1, 2, 3, 1, 1},
            {3, 2, 5, 2, 2},
        };
        List<String[]> pares = new java.util.ArrayList<>();
        for (int i = 0; i < fo.length; i++)
            for (int j = 0; j < fo[i].length; j++)
                for (int k = 0; k < fo[i][j]; k++)
                    pares.add(new String[]{financiamiento.get(i), programa.get(j)});

        Map<String, Object> r = Estadistica.chiCuadrada(pares, financiamiento, programa);
        assertEquals(30, r.get("n"));
        assertEquals(8, r.get("gradosLibertad")); // (3-1)(5-1)
        assertEquals(2.21, (double) r.get("chiCuadrada"), 0.01);
        assertEquals(15.507, (double) r.get("chiTeorico"), 1e-9);
        assertEquals(true, r.get("independientes")); // 2.21 < 15.507
    }

    @Test
    void chiCuadradaCuentaCeldasConFrecuenciaEsperadaMenorA5() {
        // Tabla 2x2 bien desbalanceada: casi todas las celdas van a tener FE<5
        List<String[]> pares = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) pares.add(new String[]{"a", "x"});
        pares.add(new String[]{"a", "y"});
        pares.add(new String[]{"b", "x"});
        pares.add(new String[]{"b", "y"});

        Map<String, Object> r = Estadistica.chiCuadrada(pares, List.of("a", "b"), List.of("x", "y"));
        assertEquals(4, r.get("totalCeldas"));
        assertTrue((int) r.get("celdasFeMenor5") > 0);
    }

    @Test
    void pruebaFrecuenciasEjemploDelInforme() {
        // Áreas de los 10 titulados del ejemplo del docente: FE = 10/4 = 2.5 ; x0^2 = 4 * 0.25/2.5 = 0.4 < 7.815
        Map<String, Object> r = Estadistica.pruebaFrecuencias(List.of("CD", "Ciber", "SW", "Redes"), List.of(3, 2, 3, 2));
        assertEquals(2.5, (double) r.get("esperada"), 1e-9);
        assertEquals(0.4, (double) r.get("chiCuadrada"), 1e-9);
        assertEquals(3, r.get("gradosLibertad"));
        assertEquals(true, r.get("uniforme"));
    }

    @Test
    void pruebaFrecuenciasDetectaPreferencia() {
        // 20 respuestas todas en una opción de 4: FE = 5 ; x0^2 = (15^2 + 3 * 5^2)/5 = 60 > 7.815
        Map<String, Object> r = Estadistica.pruebaFrecuencias(List.of("a", "b", "c", "d"), List.of(20, 0, 0, 0));
        assertEquals(60.0, (double) r.get("chiCuadrada"), 1e-9);
        assertEquals(false, r.get("uniforme"));
    }

    @Test
    void pruebaFrecuenciasSinRespuestas() {
        assertNull(Estadistica.pruebaFrecuencias(List.of("a", "b"), List.of(0, 0)).get("uniforme"));
    }
}
