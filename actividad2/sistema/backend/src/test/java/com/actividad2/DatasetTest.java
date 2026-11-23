package com.actividad2;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de caja blanca de Dataset.java: la regla de privacidad (se
 * comprueba que de verdad borra las columnas, no solo que dice hacerlo) y la
 * normalizacion de fechas ambiguas.
 */
class DatasetTest {

    private static Map<String, String> fila(String... pares) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put(pares[i], pares[i + 1]);
        return m;
    }

    @Test
    void sacaLasColumnasPersonalesAlConstruir() {
        Dataset d = new Dataset("prueba", List.of(
            fila("genero", "Mujer", "email", "a@b.com", "telefono", "123", "linkedin", "x", "correo", "y")));
        assertFalse(d.columnas.contains("email"));
        assertFalse(d.columnas.contains("telefono"));
        assertFalse(d.columnas.contains("linkedin"));
        assertFalse(d.columnas.contains("correo"));
        assertTrue(d.columnas.contains("genero"));
        assertNull(d.filas.get(0).get("email")); // no solo fuera de "columnas": el valor ya no esta en la fila
    }

    @Test
    void normalizaFechaIsoDirecta() {
        Dataset d = new Dataset("prueba", List.of(fila(Dataset.FECHA, "2026-03-05")));
        assertEquals("2026-03-05", d.filas.get(0).get(Dataset.FECHA));
    }

    @Test
    void normalizaFechaDeMicrosoftFormsConHora() {
        // "Hora de finalización" trae fecha y hora separadas por espacio; solo se queda con la fecha
        Dataset d = new Dataset("prueba", List.of(fila("Hora de finalización", "5/3/2026 10:30:00")));
        assertEquals("2026-03-05", d.filas.get(0).get(Dataset.FECHA)); // dia/mes (formato boliviano) se intenta primero
        assertFalse(d.filas.get(0).containsKey("Hora de finalización")); // la columna original no se conserva
    }

    @Test
    void normalizaFechaDeGoogleFormsConMarcaTemporal() {
        Dataset d = new Dataset("prueba", List.of(fila("Marca temporal", "2026/03/05")));
        assertEquals("2026-03-05", d.filas.get(0).get(Dataset.FECHA));
    }

    @Test
    void fechaInvalidaQuedaVacia() {
        Dataset d = new Dataset("prueba", List.of(fila(Dataset.FECHA, "no es una fecha")));
        assertEquals("", d.filas.get(0).get(Dataset.FECHA));
        assertNull(Dataset.fecha(d.filas.get(0)));
    }

    @Test
    void entreFechasFiltraInclusivo() {
        Dataset d = new Dataset("prueba", List.of(
            fila(Dataset.FECHA, "2026-01-10"),
            fila(Dataset.FECHA, "2026-02-01"),
            fila(Dataset.FECHA, "2026-03-20")));
        Dataset filtrado = d.entreFechas(LocalDate.parse("2026-01-10"), LocalDate.parse("2026-02-01"));
        assertEquals(2, filtrado.n());
    }

    @Test
    void entreFechasSinLimitesDevuelveElMismoDataset() {
        Dataset d = new Dataset("prueba", List.of(fila(Dataset.FECHA, "2026-01-10")));
        assertSame(d, d.entreFechas(null, null));
    }

    @Test
    void valoresMultiplesSeparaPorBarraYDescartaVacios() {
        Dataset d = new Dataset("prueba", List.of(
            fila("cargos", "analista|tecnico| "),
            fila("cargos", "jefatura")));
        assertEquals(List.of("analista", "tecnico", "jefatura"), d.valoresMultiples("cargos"));
    }

    @Test
    void tieneColumnasExigeTodasLasRequeridas() {
        Dataset d = new Dataset("prueba", List.of(fila("genero", "Mujer", "edad", "25")));
        assertTrue(d.tieneColumnas(List.of("genero", "edad")));
        assertFalse(d.tieneColumnas(List.of("genero", "rubro")));
    }
}
