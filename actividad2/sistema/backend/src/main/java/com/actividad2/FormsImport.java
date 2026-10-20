package com.actividad2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importa el CSV tal como lo exportan Microsoft Forms (titulados) y Google Forms
 * (empleadores): encabezados con el texto completo de la pregunta y valores con
 * el texto de la opción. Traduce todo a los códigos de la encuesta guardada en
 * la base y devuelve un informe de correspondencia (qué texto se tradujo a qué
 * código y qué no se pudo reconocer) para revisarlo antes de analizar.
 *
 * Uso por consola:
 *   simular (sin base):  java -cp target/classes com.actividad2.FormsImport titulados archivo.csv
 *   guardar en la base:  java -cp target/classes:lib/postgresql-42.7.5.jar com.actividad2.FormsImport titulados archivo.csv --guardar
 */
public class FormsImport {

    // ---------------- correspondencia columna del CSV -> código de pregunta ----------------
    // Se compara el encabezado normalizado (sin acentos, minúsculas, sin signos).
    // "^" = el encabezado empieza con el texto; "=" = es exactamente; "~" = lo contiene.
    private static final Map<String, Map<String, String>> COLUMNAS = Map.of(
        "titulados", columnas(
            "fecha_respuesta", "^hora de finalizacion",
            "edad", "^edad que tiene actualmente",
            "genero", "=genero",
            "formacion_complementaria", "^posterior a sus estudios de pregrado",
            "programa_nivel", "^cual es el programa de formacion complementaria",
            "programa_donde", "^donde ha cursado el programa",
            "programa_financiamiento", "^cual fue la fuente de financiamiento",
            "programa_nombre", "^indique el nombre del programa",
            "interes_posgrado", "^estaria interesado en realizar estudios",
            "area_interes_posgrado", "^en cual de las siguientes areas",
            "organizacion_interes_posgrado", "^en caso de realizar un estudio de posgrado",
            "likert_oferta_posgrado_fcyt", "^cual es su opinion personal respecto al posgrado",
            "anios_vida_profesional", "^cuantos son los anos de vida profesional",
            "anios_desempleado", "^de la cantidad de tiempo respondida",
            "situacion_laboral", "^cual de las siguientes afirmaciones describe",
            "empresa_nombre", "^nombre de la empresa u organizacion",
            "rubro", "^cual es el rubro de la organizacion",
            "rubro_otro", "=otro rubro",
            "sector_publico_privado", "^el trabajo en el que se encuentra",
            "departamento_bolivia", "^en que departamento de bolivia",
            "area_organizacion", "^cual es el area dentro de la organizacion",
            "area_otra", "=otr area",
            "cargo", "^cual es el cargo que desempena",
            "cargo_otro", "=otr cargo",
            "likert_pertinencia_puesto", "^en su actual puesto de trabajo",
            "antiguedad_trabajo", "^que antiguedad tiene",
            "medio_obtuvo_trabajo", "^a traves de que medio obtuvo",
            "remuneracion_promedio", "^senale la remuneracion promedio",
            "origen_emprendimiento", "^cual es el origen del desarrollo",
            "tipo_entregable", "^que tipo de entregable",
            "financiamiento_externo_negocio", "^ha requerido financiamiento externo",
            "likert_satisfaccion_negocio", "^que tan satisfecho esta",
            "likert_importancia_formacion_negocio", "^que tan importante considera",
            "primer_empleo", "^el trabajo que ejerce actualmente",
            "razon_no_trabaja", "^indique cual es la razon",
            "ha_tenido_trabajo_antes", "^si bien actualmente no trabaja",
            "tiempo_primer_trabajo", "^luego de titularse",
            "cantidad_empleos", "^desde que se titulo",
            "likert_satisfaccion_global_formacion", "^de manera global",
            "likert_concordancia_requerimientos", "^considera que existe concordancia",
            "aspectos_utilidad", "^que aspectos de la carrera les resultaron",
            "aspectos_mejorar", "^que aspectos de la carrera considera",
            "asignaturas_ventaja", "^indique 5 asignaturas cursadas en la carrera que considera",
            "asignatura_ventaja_otra", "=otra asignatura",
            "asignaturas_poco_utiles", "^indique 5 asignaturas cursadas en la carrera que no",
            "asignatura_poco_util_otra", "=otra asignatura 1",
            "red_contactos_interes", "^le interesaria formar parte",
            "email", "^email de contacto",
            "telefono", "^numero telefonico de contacto",
            "linkedin", "^link de linkedin"),
        "empleadores", columnas(
            "fecha_respuesta", "=marca temporal",
            "correo", "^direccion de correo electronico",
            "nombre_empresa", "^1 nombre de la empresa",
            "tipo_organizacion", "^2 tipo de organizacion",
            "tamano", "^3 tamano",
            "rubro", "^4 indique el rubro",
            "contrato_ultimos_5_anios", "^5 la organizacion ha contratado",
            "probabilidad_contratacion", "^6 considerando",
            "nivel_formacion_demandado", "^7 que nivel de formacion",
            "medio_convocatoria", "^8 a traves de que medio",
            "cargos", "^9 que tipo de cargos",
            "areas_emergentes", "^10 desde su experiencia",
            "herramientas", "^11 cuales son las nuevas herramientas",
            "habilidades", "^12 desde su experiencia",
            "perfil_confianza", "~brinda confianza",
            "perfil_titulo_consistente", "~titulo profesional otorgado",
            "perfil_consulta_autoridades", "~las autoridades de la carrera",
            "perfil_conoce_perfil", "~conozco el perfil profesional",
            "perfil_coherente_desempeno", "~es coherente con el desempeno",
            "perfil_desempeno_destacado", "~alcanzar un desempeno destacado",
            "perfil_opinion_practicas", "~considera nuestra opinion como empleadores e incorpora",
            "perfil_competencias_laborales", "~evidencia el desarrollo de competencias",
            "perfil_valores", "~demuestran valores y actitudes",
            "perfil_participo_retroalimentacion", "~he participado en procesos de recopilacion",
            "perfil_vinculos_oportunidades", "~mantiene vinculos con organizaciones",
            "perfil_participo_vinculacion_entorno", "~actividades de vinculacion con el entorno",
            "perfil_participo_difusion", "~actividades de difusion de oportunidades",
            "perfil_participo_vinculacion_profesionales", "~vinculacion entre profesionales",
            "perfil_consultado_desempeno", "~he sido consultado sobre el desempeno",
            "perfil_consultada_pertinencia", "~ha consultado nuestra opinion respecto a la pertinencia",
            "perfil_recurre_umss", "~recurrimos a la universidad",
            "perfil_consultado_autoevaluacion", "~consultado periodicamente"));

    private static Map<String, String> columnas(String... pares) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put(pares[i], pares[i + 1]);
        return m;
    }

    private static final Set<String> VACIAS = Set.of("a", "de", "del", "la", "las", "el", "los", "en", "y", "o", "por", "para",
        "un", "una", "con", "al", "su", "sus", "e", "que");

    // ---------------- resultado ----------------
    public record Fila(Map<String, Object> datos, Map<String, String> contacto, LocalDate fecha) {}

    public record Resultado(List<Fila> filas, Map<String, String> columnasUsadas, List<String> columnasSinPregunta,
                            Map<String, Map<String, String>> traducciones, Map<String, Map<String, Integer>> noReconocidos) {

        /** Informe legible de la correspondencia (para revisar antes de guardar). */
        public String informe() {
            StringBuilder sb = new StringBuilder();
            sb.append("Respuestas leídas: ").append(filas.size()).append("\n\n");
            sb.append("Columnas usadas (pregunta <- columna del CSV):\n");
            columnasUsadas.forEach((k, v) -> sb.append("  ").append(k).append(" <- ").append(recortar(v, 70)).append("\n"));
            sb.append("\nColumnas ignoradas: ").append(columnasSinPregunta.size()).append(" (Id, hora de inicio, puntos, comentarios, etc.)\n");
            sb.append("\nTraducción de textos a códigos:\n");
            traducciones.forEach((pregunta, t) -> {
                sb.append("  ").append(pregunta).append(":\n");
                t.forEach((texto, codigo) -> sb.append("      ").append(recortar(texto, 60)).append("  ->  ").append(codigo).append("\n"));
            });
            sb.append("\nValores NO reconocidos (quedan vacíos):\n");
            if (noReconocidos.isEmpty()) sb.append("  ninguno\n");
            noReconocidos.forEach((pregunta, m) -> m.forEach((texto, n) ->
                sb.append("  ").append(pregunta).append(": ").append(recortar(texto, 60)).append(" (").append(n).append(")\n")));
            return sb.toString();
        }
    }

    private static String recortar(String s, int n) {
        String t = s.replace('\n', ' ');
        return t.length() <= n ? t : t.substring(0, n - 1) + "…";
    }

    // ---------------- lectura ----------------

    /** Traduce un CSV exportado de Forms usando la estructura de la encuesta (de la base o de Semilla). */
    @SuppressWarnings("unchecked")
    public static Resultado leer(String encuesta, String textoCsv, Map<String, Object> estructura) {
        Map<String, String> reglas = COLUMNAS.get(encuesta);
        if (reglas == null) throw new IllegalArgumentException("Encuesta no válida: " + encuesta);
        List<Map<String, String>> filasCsv = Csv.leer(textoCsv);
        if (filasCsv.isEmpty()) throw new IllegalArgumentException("El archivo no tiene filas");

        Map<String, Map<String, Object>> preguntas = new LinkedHashMap<>();
        for (Map<String, Object> s : (List<Map<String, Object>>) estructura.get("secciones")) {
            for (Map<String, Object> p : (List<Map<String, Object>>) s.get("preguntas")) preguntas.put((String) p.get("codigo"), p);
        }

        // 1) Columna del CSV para cada pregunta
        Map<String, String> columnaDe = new LinkedHashMap<>();
        List<String> sinPregunta = new ArrayList<>();
        for (String encabezado : filasCsv.get(0).keySet()) {
            String n = normalizar(encabezado);
            if (n.startsWith("puntos ") || n.startsWith("comentarios ")) { sinPregunta.add(encabezado); continue; }
            String codigo = null;
            for (Map.Entry<String, String> r : reglas.entrySet()) {
                String regla = r.getValue();
                String patron = regla.substring(1);
                boolean ok = switch (regla.charAt(0)) {
                    case '^' -> n.startsWith(patron);
                    case '=' -> n.equals(patron);
                    default -> n.contains(patron);
                };
                if (ok && !columnaDe.containsKey(r.getKey())) { codigo = r.getKey(); break; }
            }
            if (codigo == null) sinPregunta.add(encabezado);
            else columnaDe.put(codigo, encabezado);
        }

        // 2) Formato de fecha de la columna (mes/día o día/mes)
        String colFecha = columnaDe.get("fecha_respuesta");
        boolean diaPrimero = false;
        if (colFecha != null) {
            for (Map<String, String> f : filasCsv) {
                String[] partes = f.getOrDefault(colFecha, "").trim().split("[ T]")[0].split("/");
                if (partes.length == 3 && entero(partes[0]) > 12) { diaPrimero = true; break; }
            }
        }

        // 3) Traducción fila por fila
        Map<String, Map<String, String>> traducciones = new TreeMap<>();
        Map<String, Map<String, Integer>> noReconocidos = new TreeMap<>();
        List<Fila> filas = new ArrayList<>();
        for (Map<String, String> f : filasCsv) {
            Map<String, Object> datos = new LinkedHashMap<>();
            Map<String, String> contacto = new LinkedHashMap<>();
            LocalDate fecha = colFecha == null ? null : fecha(f.get(colFecha), diaPrimero);
            for (Map.Entry<String, String> c : columnaDe.entrySet()) {
                String codigo = c.getKey();
                if (codigo.equals("fecha_respuesta")) continue;
                String crudo = limpiar(f.get(c.getValue()));
                if (crudo.isEmpty()) continue;
                Map<String, Object> p = preguntas.get(codigo);
                if (p == null) continue;
                if (Boolean.TRUE.equals(p.get("esContacto"))) { contacto.put(codigo, crudo); continue; }
                Object valor = traducir(p, crudo, traducciones, noReconocidos);
                if (valor != null) datos.put(codigo, valor);
            }
            filas.add(new Fila(datos, contacto, fecha));
        }
        return new Resultado(filas, columnaDe, sinPregunta, traducciones, noReconocidos);
    }

    @SuppressWarnings("unchecked")
    private static Object traducir(Map<String, Object> p, String crudo, Map<String, Map<String, String>> traducciones,
                                   Map<String, Map<String, Integer>> noReconocidos) {
        String codigo = (String) p.get("codigo");
        String tipo = (String) p.get("tipo");
        List<Map<String, Object>> opciones = (List<Map<String, Object>>) p.get("opciones");
        switch (tipo) {
            case "unica", "escala" -> {
                String c = opcion(opciones, crudo);
                anotar(codigo, crudo, c, traducciones, noReconocidos);
                return c;
            }
            case "multiple" -> {
                List<String> elegidas = new ArrayList<>();
                for (String parte : partesMultiple(crudo, opciones)) {
                    String c = opcion(opciones, parte);
                    anotar(codigo, parte, c, traducciones, noReconocidos);
                    if (c != null && !elegidas.contains(c)) elegidas.add(c);
                }
                return elegidas.isEmpty() ? null : elegidas;
            }
            case "numero" -> {
                Double n = numero(crudo);
                if (n == null) noReconocidos.computeIfAbsent(codigo, k -> new TreeMap<>()).merge(crudo, 1, Integer::sum);
                else if (!crudo.matches("\\d+([.,]\\d+)?")) traducciones.computeIfAbsent(codigo, k -> new TreeMap<>()).put(crudo, fmt(n));
                return n == null ? null : (n == Math.rint(n) ? (Object) n.longValue() : (Object) n);
            }
            default -> {
                return crudo;
            }
        }
    }

    private static void anotar(String pregunta, String texto, String codigo, Map<String, Map<String, String>> traducciones,
                               Map<String, Map<String, Integer>> noReconocidos) {
        if (codigo == null) noReconocidos.computeIfAbsent(pregunta, k -> new TreeMap<>()).merge(texto, 1, Integer::sum);
        else traducciones.computeIfAbsent(pregunta, k -> new TreeMap<>()).put(texto, codigo);
    }

    /**
     * Parte una respuesta de elección múltiple: Microsoft Forms separa con ';' o
     * exporta una lista JSON; Google Forms separa con ', ' pero los textos de las
     * opciones también tienen comas, así que ahí se buscan las opciones dentro
     * del texto (por la parte anterior al paréntesis).
     */
    private static List<String> partesMultiple(String crudo, List<Map<String, Object>> opciones) {
        String t = crudo.trim();
        if (t.startsWith("[")) {
            try {
                Object v = Json.leer(t);
                if (v instanceof List<?> l) return l.stream().map(String::valueOf).toList();
            } catch (IllegalArgumentException ignorada) {
                // no era JSON: se sigue con los otros formatos
            }
        }
        if (t.contains(";")) {
            List<String> partes = new ArrayList<>();
            for (String x : t.split(";")) if (!x.isBlank()) partes.add(x.trim());
            return partes;
        }
        String n = " " + normalizar(t) + " ";
        List<String> encontradas = new ArrayList<>();
        for (Map<String, Object> o : opciones) {
            String etiqueta = (String) o.get("etiqueta");
            String cabeza = normalizar(etiqueta.contains("(") ? etiqueta.substring(0, etiqueta.indexOf('(')) : etiqueta);
            if (!cabeza.isBlank() && n.contains(" " + cabeza + " ")) encontradas.add(etiqueta);
        }
        return encontradas.isEmpty() ? List.of(t) : encontradas;
    }

    /**
     * Opción que corresponde a un texto: primero igualdad de palabras; si no,
     * la opción con mayor coincidencia de palabras (índice de Jaccard), siempre
     * que supere a la segunda con claridad.
     */
    private static String opcion(List<Map<String, Object>> opciones, String texto) {
        Set<String> t = palabras(texto);
        if (t.isEmpty()) return null;
        String mejor = null;
        double puntaje = 0, segundo = 0;
        for (Map<String, Object> o : opciones) {
            Set<String> e = palabras((String) o.get("etiqueta"));
            Set<String> c = palabras((String) o.get("codigo"));
            if (e.equals(t) || c.equals(t)) return (String) o.get("codigo");
            double s = Math.max(jaccard(t, e), jaccard(t, c));
            // si todas las palabras de una opción corta están en el texto (ej. "EUPG"), cuenta como coincidencia fuerte
            if (!e.isEmpty() && t.containsAll(e) && e.size() <= 3) s = Math.max(s, 0.75);
            if (s > puntaje) { segundo = puntaje; puntaje = s; mejor = (String) o.get("codigo"); }
            else if (s > segundo) segundo = s;
        }
        return puntaje >= 0.34 && puntaje - segundo >= 0.15 ? mejor : null;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> i = new HashSet<>(a);
        i.retainAll(b);
        Set<String> u = new HashSet<>(a);
        u.addAll(b);
        return (double) i.size() / u.size();
    }

    private static Set<String> palabras(String s) {
        Set<String> r = new LinkedHashSet<>();
        for (String w : normalizar(s).split(" ")) if (!w.isBlank() && !VACIAS.contains(w)) r.add(w);
        return r;
    }

    /** Sin acentos, minúsculas, números con separador de miles unidos ("2.500" -> "2500") y sin signos. */
    static String normalizar(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s.replace(' ', ' '), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        t = t.replaceAll("(\\d)[.,](\\d)", "$1$2");
        t = t.replaceAll("[^a-z0-9]+", " ");
        return t.trim().replaceAll(" +", " ");
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.replace(' ', ' ').trim();
    }

    private static final Pattern NUMERO = Pattern.compile("(\\d+(?:[.,]\\d+)?)");

    /** "6", "2 años", "1.5", "6 meses" (-> 0.5 años), "Ninguno"/"Nada" (-> 0); texto sin número -> no reconocido. */
    static Double numero(String crudo) {
        String n = normalizar(crudo);
        Matcher m = NUMERO.matcher(crudo.replace(',', '.'));
        if (m.find()) {
            double v = Double.parseDouble(m.group(1));
            if (n.contains("mes")) v = Math.round(v / 12 * 100) / 100.0;
            return v;
        }
        if (n.startsWith("ningun") || n.startsWith("nada") || n.contains("siempre estuve")) return 0.0;
        return null;
    }

    private static LocalDate fecha(String crudo, boolean diaPrimero) {
        if (crudo == null || crudo.isBlank()) return null;
        String[] p = crudo.trim().split("[ T]")[0].split("[/-]");
        try {
            if (p.length != 3) return null;
            if (p[0].length() == 4) return LocalDate.of(entero(p[0]), entero(p[1]), entero(p[2]));
            int anio = entero(p[2]);
            if (anio < 100) anio += 2000;
            return diaPrimero ? LocalDate.of(anio, entero(p[1]), entero(p[0])) : LocalDate.of(anio, entero(p[0]), entero(p[1]));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int entero(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** ¿El CSV viene con encabezados de Forms (texto de la pregunta) en vez de códigos? */
    public static boolean esFormatoForms(String textoCsv) {
        String primera = textoCsv.lines().findFirst().orElse("");
        String n = normalizar(primera);
        return n.contains("hora de finalizacion") || n.contains("marca temporal");
    }

    // ---------------- consola ----------------
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: FormsImport <titulados|empleadores> <archivo.csv> [--guardar]");
            return;
        }
        String encuesta = args[0];
        String texto = Files.readString(Path.of(args[1]));
        boolean guardar = args.length > 2 && args[2].equals("--guardar");
        Map<String, Object> estructura;
        if (guardar) {
            Db.iniciar();
            Semilla.cargarSiFalta();
            estructura = Encuestas.estructura(encuesta, true);
        } else {
            estructura = Semilla.estructuraLocal(encuesta);
        }
        Resultado r = leer(encuesta, texto, estructura);
        System.out.println(r.informe());
        if (guardar) {
            int n = Respuestas.guardarImportadas(encuesta, r.filas(), true);
            System.out.println("Guardadas en la base: " + n + " respuestas (origen 'importado'; reemplazan a las importadas antes).");
            System.exit(0);
        } else {
            System.out.println("Simulación: no se guardó nada. Agregue --guardar para cargar a la base.");
        }
    }
}
