package com.actividad2;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Respuestas de una encuesta (titulados o empleadores), una fila por persona.
 * Al cargar se eliminan las columnas con datos personales (PLAN.md 5.6).
 */
public class Dataset {

    public static final Set<String> COLUMNAS_PERSONALES = Set.of("email", "telefono", "linkedin", "correo");

    /** Columna con la fecha de cada respuesta, en formato ISO (aaaa-mm-dd). */
    public static final String FECHA = "fecha_respuesta";

    // Encabezados con que Microsoft Forms y Google Forms exportan la fecha de respuesta
    private static final List<String> COLUMNAS_FECHA_FORMS = List.of(
        "Hora de finalización", "Hora de finalizacion", "Completion time", "Marca temporal", "Timestamp");

    private static final List<DateTimeFormatter> FORMATOS = List.of(
        DateTimeFormatter.ofPattern("yyyy-M-d"), DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("M/d/yyyy"));

    public final String origen;
    public final List<String> columnas;
    public final List<Map<String, String>> filas;

    public Dataset(String origen, List<Map<String, String>> filasLeidas) {
        this.origen = origen;
        Set<String> cols = new LinkedHashSet<>();
        this.filas = new ArrayList<>();
        for (Map<String, String> fila : filasLeidas) {
            COLUMNAS_PERSONALES.forEach(fila::remove);
            normalizarFecha(fila);
            cols.addAll(fila.keySet());
            filas.add(fila);
        }
        this.columnas = new ArrayList<>(cols);
    }

    private Dataset(String origen, List<String> columnas, List<Map<String, String>> filas) {
        this.origen = origen;
        this.columnas = columnas;
        this.filas = filas;
    }

    /**
     * Deja la fecha de la respuesta en la columna fecha_respuesta (aaaa-mm-dd).
     * Acepta la columna de fecha de los formularios y la parte de la hora se ignora.
     * Con fechas tipo 03/04/2026 se asume dia/mes (formato de Bolivia) y, si no
     * es valida, mes/dia (formato de Forms en ingles).
     */
    private static void normalizarFecha(Map<String, String> fila) {
        String crudo = fila.getOrDefault(FECHA, "");
        for (String c : COLUMNAS_FECHA_FORMS) {
            if (crudo.isBlank() && fila.containsKey(c)) crudo = fila.remove(c);
            else fila.remove(c);
        }
        if (crudo == null || crudo.isBlank()) return;
        String soloFecha = crudo.trim().split("[ T]")[0];
        for (DateTimeFormatter f : FORMATOS) {
            try {
                fila.put(FECHA, LocalDate.parse(soloFecha, f).toString());
                return;
            } catch (DateTimeParseException ignorada) {
                // se prueba el siguiente formato
            }
        }
        fila.put(FECHA, "");
    }

    public static LocalDate fecha(Map<String, String> fila) {
        String v = fila.getOrDefault(FECHA, "");
        return v.isBlank() ? null : LocalDate.parse(v);
    }

    /** Respuestas recibidas entre dos fechas (inclusive). Sin limites devuelve el mismo dataset. */
    public Dataset entreFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) return this;
        List<Map<String, String>> seleccion = new ArrayList<>();
        for (Map<String, String> f : filas) {
            LocalDate d = fecha(f);
            if (d == null) continue;
            if (desde != null && d.isBefore(desde)) continue;
            if (hasta != null && d.isAfter(hasta)) continue;
            seleccion.add(f);
        }
        return new Dataset(origen, columnas, seleccion);
    }

    /** Fecha mas antigua, mas reciente y cuantas filas no tienen fecha. */
    public Map<String, Object> periodo() {
        LocalDate min = null, max = null;
        int sinFecha = 0;
        for (Map<String, String> f : filas) {
            LocalDate d = fecha(f);
            if (d == null) { sinFecha++; continue; }
            if (min == null || d.isBefore(min)) min = d;
            if (max == null || d.isAfter(max)) max = d;
        }
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("desde", min == null ? null : min.toString());
        r.put("hasta", max == null ? null : max.toString());
        r.put("sinFecha", sinFecha);
        return r;
    }

    public int n() {
        return filas.size();
    }

    public String valor(Map<String, String> fila, String columna) {
        String v = fila.get(columna);
        return v == null ? "" : v.trim();
    }

    /** Valores no vacios de una columna. */
    public List<String> valores(String columna) {
        return valores(columna, f -> true);
    }

    public List<String> valores(String columna, Predicate<Map<String, String>> filtro) {
        List<String> lista = new ArrayList<>();
        for (Map<String, String> fila : filas) {
            String v = valor(fila, columna);
            if (!v.isEmpty() && filtro.test(fila)) lista.add(v);
        }
        return lista;
    }

    /** Columnas de eleccion multiple: las opciones vienen separadas por "|". */
    public List<String> valoresMultiples(String columna) {
        List<String> lista = new ArrayList<>();
        for (String v : valores(columna)) {
            for (String parte : v.split("\\|")) {
                if (!parte.isBlank()) lista.add(parte.trim());
            }
        }
        return lista;
    }

    public List<Map<String, String>> filas(Predicate<Map<String, String>> filtro) {
        return filas.stream().filter(filtro).toList();
    }

    public boolean tieneColumnas(List<String> requeridas) {
        return columnas.containsAll(requeridas);
    }
}
