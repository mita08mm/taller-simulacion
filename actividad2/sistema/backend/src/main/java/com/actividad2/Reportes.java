package com.actividad2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reportes a medida: el usuario elige la encuesta, el período y las variables
 * (preguntas), y por cada variable se devuelve su tabla de frecuencias con las
 * etiquetas de la encuesta guardada en la base. Los datos de contacto nunca
 * se ofrecen como variable.
 */
public class Reportes {

    private static final Set<String> SIN_REPORTE = Set.of("email", "telefono");

    /** Variables disponibles de una encuesta, agrupadas por sección (para elegirlas en el panel). */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> variables(String encuesta) {
        List<Map<String, Object>> secciones = new ArrayList<>();
        for (Map<String, Object> s : (List<Map<String, Object>>) Encuestas.estructura(encuesta, true).get("secciones")) {
            List<Map<String, Object>> preguntas = new ArrayList<>();
            for (Map<String, Object> p : (List<Map<String, Object>>) s.get("preguntas")) {
                if (!reportable(p)) continue;
                preguntas.add(Map.of("codigo", p.get("codigo"), "texto", p.get("texto"), "tipo", p.get("tipo")));
            }
            if (!preguntas.isEmpty()) secciones.add(Map.of("titulo", s.get("titulo"), "preguntas", preguntas));
        }
        return secciones;
    }

    private static boolean reportable(Map<String, Object> p) {
        return !Boolean.TRUE.equals(p.get("esContacto")) && !SIN_REPORTE.contains(p.get("tipo"));
    }

    /** Tabla de frecuencias de cada variable elegida, en el orden en que se eligieron. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> generar(String encuesta, Dataset d, List<String> codigos) {
        Map<String, Map<String, Object>> preguntas = new LinkedHashMap<>();
        for (Map<String, Object> s : (List<Map<String, Object>>) Encuestas.estructura(encuesta, true).get("secciones")) {
            for (Map<String, Object> p : (List<Map<String, Object>>) s.get("preguntas")) preguntas.put((String) p.get("codigo"), p);
        }
        List<Map<String, Object>> variables = new ArrayList<>();
        for (String codigo : codigos) {
            Map<String, Object> p = preguntas.get(codigo);
            if (p == null || !reportable(p)) continue;
            variables.add(variable(p, d));
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("encuesta", encuesta);
        r.put("n", d.n());
        r.put("variables", variables);
        return r;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> variable(Map<String, Object> p, Dataset d) {
        String codigo = (String) p.get("codigo");
        String tipo = (String) p.get("tipo");
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("codigo", codigo);
        v.put("texto", p.get("texto"));
        v.put("tipo", tipo);
        List<String> respuestas = d.valores(codigo);
        v.put("n", respuestas.size());

        switch (tipo) {
            case "unica", "escala" -> v.put("frecuencias", porOpcion(p, respuestas, respuestas.size()));
            case "multiple" -> {
                // el porcentaje es sobre las personas que respondieron, por eso puede sumar más de 100%
                v.put("frecuencias", porOpcion(p, d.valoresMultiples(codigo), respuestas.size()));
                v.put("nota", "Elección múltiple: cada persona puede marcar varias opciones, por eso los porcentajes pueden sumar más de 100%.");
            }
            case "numero" -> {
                List<Double> numeros = new ArrayList<>();
                TreeMap<Double, Integer> conteo = new TreeMap<>();
                for (String r : respuestas) {
                    try {
                        double x = Double.parseDouble(r.replace(',', '.'));
                        numeros.add(x);
                        conteo.merge(x, 1, Integer::sum);
                    } catch (NumberFormatException ignorada) {
                        // valor no numérico: no entra al cálculo
                    }
                }
                v.put("frecuencias", conteo.size() <= 10 ? porValor(conteo, numeros.size()) : porRango(numeros));
                if (conteo.size() > 10) v.put("nota", "Los valores se agrupan en rangos de igual ancho.");
                v.put("estadistica", Estadistica.intervaloConfianza95(numeros));
            }
            default -> {
                // texto libre: preguntas abiertas de empleadores por categoría; el resto, respuestas repetidas
                if (Abiertas.CATEGORIAS.containsKey(codigo)) {
                    v.put("frecuencias", Abiertas.codificar(codigo, respuestas));
                    v.put("nota", "Respuesta abierta agrupada en categorías por palabras clave; una respuesta puede caer en varias.");
                } else {
                    Map<String, Integer> conteo = new LinkedHashMap<>();
                    for (String r : respuestas) conteo.merge(r.trim(), 1, Integer::sum);
                    List<Map<String, Object>> frec = new ArrayList<>();
                    conteo.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(15)
                        .forEach(e -> frec.add(fila(e.getKey(), e.getKey(), e.getValue(), respuestas.size())));
                    v.put("frecuencias", frec);
                    v.put("nota", "Respuesta abierta: se muestran las 15 respuestas más repetidas.");
                }
            }
        }
        return v;
    }

    private static List<Map<String, Object>> porValor(TreeMap<Double, Integer> conteo, int n) {
        List<Map<String, Object>> frec = new ArrayList<>();
        conteo.forEach((x, k) -> frec.add(fila(fmt(x), fmt(x), k, n)));
        return frec;
    }

    /** Muchos valores distintos: se agrupan en unos 6 rangos de igual ancho (ancho entero). */
    private static List<Map<String, Object>> porRango(List<Double> numeros) {
        double min = Math.floor(numeros.stream().mapToDouble(x -> x).min().orElse(0));
        double max = numeros.stream().mapToDouble(x -> x).max().orElse(0);
        int ancho = (int) Math.max(1, Math.ceil((max - min + 1) / 6));
        boolean enteros = numeros.stream().allMatch(x -> x == Math.rint(x));
        int rangos = (int) Math.ceil((max - min + 1) / ancho);
        int[] conteo = new int[rangos];
        for (double x : numeros) conteo[(int) Math.min(rangos - 1, Math.floor((x - min) / ancho))]++;
        List<Map<String, Object>> frec = new ArrayList<>();
        for (int i = 0; i < rangos; i++) {
            double a = min + (double) i * ancho, b = a + ancho;
            String etiqueta = enteros ? fmt(a) + " a " + fmt(b - 1) : fmt(a) + " a menos de " + fmt(b);
            frec.add(fila(etiqueta, etiqueta, conteo[i], numeros.size()));
        }
        return frec;
    }

    /** Frecuencia de cada opción de la pregunta (también las que nadie eligió), con la etiqueta de la encuesta. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> porOpcion(Map<String, Object> p, List<String> valores, int personas) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        Map<String, String> etiquetas = new LinkedHashMap<>();
        for (Map<String, Object> o : (List<Map<String, Object>>) p.get("opciones")) {
            conteo.put((String) o.get("codigo"), 0);
            etiquetas.put((String) o.get("codigo"), (String) o.get("etiqueta"));
        }
        for (String x : valores) conteo.merge(x, 1, Integer::sum);
        List<Map<String, Object>> lista = new ArrayList<>();
        conteo.forEach((cod, n) -> lista.add(fila(cod, etiquetas.getOrDefault(cod, Catalogo.etiqueta((String) p.get("codigo"), cod)), n, personas)));
        return lista;
    }

    private static Map<String, Object> fila(String codigo, String etiqueta, int frecuencia, int n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", codigo);
        m.put("etiqueta", etiqueta);
        m.put("frecuencia", frecuencia);
        m.put("relativa", n == 0 ? 0.0 : (double) frecuencia / n);
        return m;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** Las tablas del reporte una debajo de otra, para Excel: variable, opción, frecuencia y porcentaje. */
    @SuppressWarnings("unchecked")
    public static String csv(Map<String, Object> reporte, char sep, java.util.Locale loc) {
        List<List<String>> filas = new ArrayList<>();
        for (Map<String, Object> v : (List<Map<String, Object>>) reporte.get("variables")) {
            for (Map<String, Object> f : (List<Map<String, Object>>) v.get("frecuencias")) {
                filas.add(List.of((String) v.get("texto"), String.valueOf(f.get("etiqueta")), String.valueOf(f.get("frecuencia")),
                    String.format(loc, "%.1f", 100 * ((Number) f.get("relativa")).doubleValue()), String.valueOf(v.get("n"))));
            }
        }
        return Csv.escribir(List.of("Pregunta", "Respuesta", "Frecuencia", "Porcentaje", "Respondieron"), filas, sep);
    }

    /** Respuestas individuales de las variables elegidas, con el texto de la pregunta y de la opción (no los códigos). */
    @SuppressWarnings("unchecked")
    public static String datosCsv(String encuesta, Dataset d, List<String> codigos, char sep) {
        Map<String, Map<String, Object>> preguntas = new LinkedHashMap<>();
        for (Map<String, Object> s : (List<Map<String, Object>>) Encuestas.estructura(encuesta, true).get("secciones")) {
            for (Map<String, Object> p : (List<Map<String, Object>>) s.get("preguntas")) preguntas.put((String) p.get("codigo"), p);
        }
        List<Map<String, Object>> elegidas = codigos.stream().map(preguntas::get).filter(p -> p != null && reportable(p)).toList();
        List<String> encabezado = new ArrayList<>();
        encabezado.add("Fecha de respuesta");
        elegidas.forEach(p -> encabezado.add((String) p.get("texto")));
        List<List<String>> filas = new ArrayList<>();
        for (Map<String, String> f : d.filas) {
            List<String> fila = new ArrayList<>();
            fila.add(d.valor(f, "fecha_respuesta"));
            for (Map<String, Object> p : elegidas) {
                Map<String, String> etiquetas = new LinkedHashMap<>();
                for (Map<String, Object> o : (List<Map<String, Object>>) p.get("opciones")) etiquetas.put((String) o.get("codigo"), (String) o.get("etiqueta"));
                String valor = d.valor(f, (String) p.get("codigo"));
                List<String> partes = new ArrayList<>();
                for (String x : valor.split("\\|")) if (!x.isBlank()) partes.add(etiquetas.getOrDefault(x.trim(), x.trim()));
                fila.add(String.join("; ", partes));
            }
            filas.add(fila);
        }
        return Csv.escribir(encabezado, filas, sep);
    }
}
