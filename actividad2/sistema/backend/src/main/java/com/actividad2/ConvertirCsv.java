package com.actividad2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Traduce un CSV crudo de Microsoft/Google Forms a nuestro formato interno
 * (codigo_pregunta como encabezado), SIN tocar la base de datos -- usa el
 * mismo camino de simulacion de FormsImport (Semilla.estructuraLocal). Sirve
 * para alimentar a ReporteTex.java en su modo CSV.
 *
 * Uso: java -cp target/classes com.actividad2.ConvertirCsv <titulados|empleadores> <entrada.csv> <salida.csv>
 *
 * Util para regenerar tex/generado/*.tex (con ReporteTex.java, modo CSV) sin
 * conectarse a la base de datos -- esa parte del sistema la maneja otra
 * persona; esta clase solo lee archivos locales.
 */
public class ConvertirCsv {
    public static void main(String[] args) throws Exception {
        String encuesta = args[0];
        String texto = Files.readString(Path.of(args[1]));
        Map<String, Object> estructura = Semilla.estructuraLocal(encuesta);
        FormsImport.Resultado r = FormsImport.leer(encuesta, texto, estructura);

        Set<String> columnas = new LinkedHashSet<>();
        columnas.add("fecha_respuesta");
        List<Map<String, String>> filas = new ArrayList<>();
        for (FormsImport.Fila f : r.filas()) {
            Map<String, String> fila = new LinkedHashMap<>();
            fila.put("fecha_respuesta", f.fecha() == null ? "" : f.fecha().toString());
            for (Map.Entry<String, Object> e : f.datos().entrySet()) {
                Object v = e.getValue();
                String texto2 = v == null ? "" : (v instanceof List ? String.join("|", (List<String>) v) : v.toString());
                fila.put(e.getKey(), texto2);
                columnas.add(e.getKey());
            }
            filas.add(fila);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", columnas)).append("\n");
        for (Map<String, String> fila : filas) {
            boolean primero = true;
            for (String col : columnas) {
                if (!primero) sb.append(",");
                primero = false;
                String v = fila.getOrDefault(col, "");
                if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                    v = "\"" + v.replace("\"", "\"\"") + "\"";
                }
                sb.append(v);
            }
            sb.append("\n");
        }
        Files.writeString(Path.of(args[2]), sb.toString(), StandardCharsets.UTF_8);
        System.out.println("Escrito " + args[2] + " (" + filas.size() + " filas, " + columnas.size() + " columnas)");
    }
}
