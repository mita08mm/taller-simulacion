package com.actividad2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Respuestas guardadas en la base: envío del formulario (con validación), CRUD
 * para el panel, importación de CSV y conversión al Dataset que usa el análisis.
 *
 * Cada respuesta es un documento JSONB {codigo_pregunta: valor | [valores]}.
 * Los datos de contacto van a la tabla contacto y nunca entran al análisis.
 */
public class Respuestas {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final Set<String> ORIGENES = Set.of("formulario", "importado", "prueba");

    // El análisis lee todas las respuestas; se guardan en memoria hasta que llegue o cambie una.
    private static final Map<String, Dataset> CACHE = new ConcurrentHashMap<>();

    /** Errores de validación por pregunta: se responden con 400 y la lista de errores. */
    public static class Invalida extends IllegalArgumentException {
        public final Map<String, String> errores;

        public Invalida(Map<String, String> errores) {
            super("La respuesta tiene " + errores.size() + " error(es)");
            this.errores = errores;
        }
    }

    // ===================== Envío del formulario =====================

    /** Valida y guarda un envío del formulario público. Devuelve el id de la respuesta. */
    public static long enviar(String encuesta, Map<String, Object> valores) {
        Map<String, Object> estructura = Encuestas.estructura(encuesta, false);
        Validado v = validar(estructura, valores, true);
        long id = Db.transaccion(c -> insertar(c, (Integer) estructura.get("id"), (Integer) estructura.get("version"),
            "formulario", null, v.datos(), v.contacto()));
        CACHE.remove(encuesta);
        return id;
    }

    record Validado(Map<String, Object> datos, Map<String, String> contacto) {}

    /**
     * Recorre las preguntas en orden. Una pregunta cuenta solo si está visible, es
     * decir, si la pregunta de la que depende tiene uno de los valores que la
     * habilitan; las respuestas a preguntas ocultas se descartan.
     */
    @SuppressWarnings("unchecked")
    static Validado validar(Map<String, Object> estructura, Map<String, Object> valores, boolean exigirObligatorias) {
        Map<String, Object> datos = new LinkedHashMap<>();
        Map<String, String> contacto = new LinkedHashMap<>();
        Map<String, String> errores = new LinkedHashMap<>();

        for (Map<String, Object> s : (List<Map<String, Object>>) estructura.get("secciones")) {
            for (Map<String, Object> p : (List<Map<String, Object>>) s.get("preguntas")) {
                if (!Boolean.TRUE.equals(p.get("activa"))) continue;
                String codigo = (String) p.get("codigo");
                if (!visible(p, datos)) continue;
                Object crudo = valores.get(codigo);
                boolean vacio = crudo == null || (crudo instanceof String str && str.isBlank()) || (crudo instanceof List<?> l && l.isEmpty());
                if (vacio) {
                    if (exigirObligatorias && Boolean.TRUE.equals(p.get("obligatoria"))) errores.put(codigo, "Esta pregunta es obligatoria");
                    continue;
                }
                try {
                    Object valor = convertir(p, crudo);
                    if (Boolean.TRUE.equals(p.get("esContacto"))) contacto.put(codigo, String.valueOf(valor));
                    else datos.put(codigo, valor);
                } catch (IllegalArgumentException e) {
                    errores.put(codigo, e.getMessage());
                }
            }
        }
        if (!errores.isEmpty()) throw new Invalida(errores);
        return new Validado(datos, contacto);
    }

    @SuppressWarnings("unchecked")
    static boolean visible(Map<String, Object> p, Map<String, Object> respondidas) {
        String depende = (String) p.get("dependeDe");
        if (depende == null || depende.isBlank()) return true;
        List<String> habilitan = (List<String>) p.get("dependeValores");
        Object valor = respondidas.get(depende);
        if (valor == null) return false;
        if (habilitan == null || habilitan.isEmpty()) return true;
        if (valor instanceof List<?> l) return l.stream().anyMatch(x -> habilitan.contains(String.valueOf(x)));
        return habilitan.contains(String.valueOf(valor));
    }

    @SuppressWarnings("unchecked")
    private static Object convertir(Map<String, Object> p, Object crudo) {
        String tipo = (String) p.get("tipo");
        List<String> opciones = ((List<Map<String, Object>>) p.get("opciones")).stream()
            .filter(o -> !Boolean.FALSE.equals(o.get("activa"))).map(o -> (String) o.get("codigo")).toList();
        switch (tipo) {
            case "unica", "escala" -> {
                String v = String.valueOf(crudo).trim();
                if (!opciones.contains(v)) throw new IllegalArgumentException("Opción no válida");
                return v;
            }
            case "multiple" -> {
                List<String> lista = crudo instanceof List<?> l ? l.stream().map(String::valueOf).toList()
                    : List.of(String.valueOf(crudo).split("\\|"));
                List<String> sinRepetir = lista.stream().map(String::trim).filter(x -> !x.isEmpty()).distinct().toList();
                for (String x : sinRepetir) if (!opciones.contains(x)) throw new IllegalArgumentException("Opción no válida: " + x);
                Object max = p.get("maxSelecciones");
                if (max != null && sinRepetir.size() > ((Number) max).intValue()) {
                    throw new IllegalArgumentException("Puede elegir como máximo " + ((Number) max).intValue() + " opciones");
                }
                return sinRepetir;
            }
            case "numero" -> {
                double n;
                try {
                    n = Double.parseDouble(String.valueOf(crudo).trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Debe ser un número");
                }
                Object min = p.get("valorMinimo");
                Object max = p.get("valorMaximo");
                if (min != null && n < ((Number) min).doubleValue()) throw new IllegalArgumentException("El valor mínimo es " + fmt(((Number) min).doubleValue()));
                if (max != null && n > ((Number) max).doubleValue()) throw new IllegalArgumentException("El valor máximo es " + fmt(((Number) max).doubleValue()));
                return n == Math.rint(n) ? (Object) (long) n : (Object) n;
            }
            case "email" -> {
                String v = String.valueOf(crudo).trim();
                if (!v.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new IllegalArgumentException("Correo no válido");
                return v;
            }
            default -> {
                String v = String.valueOf(crudo).trim();
                if (v.length() > 1000) throw new IllegalArgumentException("Texto demasiado largo (máximo 1000 caracteres)");
                return v;
            }
        }
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static long insertar(Connection c, int encuestaId, int version, String origen, LocalDate fecha,
                                 Map<String, Object> datos, Map<String, String> contacto) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO respuesta (encuesta_id, version, fecha, origen, datos)
                VALUES (?, ?, coalesce(?, now()), ?, ?::jsonb) RETURNING id""")) {
            ps.setInt(1, encuestaId);
            ps.setInt(2, version);
            ps.setTimestamp(3, fecha == null ? null : Timestamp.valueOf(fecha.atTime(12, 0)));
            ps.setString(4, origen);
            ps.setString(5, Json.escribir(datos));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); id = rs.getLong(1); }
        }
        if (!contacto.isEmpty()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO contacto (respuesta_id, email, telefono, linkedin) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, id);
                ps.setString(2, contacto.getOrDefault("email", contacto.get("correo")));
                ps.setString(3, contacto.get("telefono"));
                ps.setString(4, contacto.get("linkedin"));
                ps.executeUpdate();
            }
        }
        return id;
    }

    // ===================== Lectura para el análisis =====================

    /** Respuestas de una encuesta como Dataset (una fila por persona, listas unidas con '|'). */
    public static Dataset dataset(String encuesta) {
        return CACHE.computeIfAbsent(encuesta, e -> Db.usar(c -> {
            List<Map<String, String>> filas = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT r.id, r.fecha, r.datos::text AS datos FROM respuesta r JOIN encuesta e ON e.id = r.encuesta_id
                    WHERE e.codigo = ? ORDER BY r.fecha, r.id""")) {
                ps.setString(1, e);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> fila = new LinkedHashMap<>();
                        fila.put("id", String.valueOf(rs.getLong("id")));
                        fila.put(Dataset.FECHA, rs.getTimestamp("fecha").toInstant().atZone(ZONA).toLocalDate().toString());
                        @SuppressWarnings("unchecked")
                        Map<String, Object> datos = (Map<String, Object>) Json.leer(rs.getString("datos"));
                        datos.forEach((k, v) -> fila.put(k, texto(v)));
                        filas.add(fila);
                    }
                }
            }
            return new Dataset(origenes(c, e), filas);
        }));
    }

    private static String texto(Object v) {
        if (v == null) return "";
        if (v instanceof List<?> l) return l.stream().map(Respuestas::texto).collect(Collectors.joining("|"));
        if (v instanceof Double d && d == Math.rint(d)) return String.valueOf(d.longValue());
        return String.valueOf(v);
    }

    /** Descripción del origen de los datos, por ejemplo "Base de datos: 12 formulario · 100 prueba". */
    private static String origenes(Connection c, String encuesta) throws SQLException {
        List<String> partes = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT r.origen, count(*) FROM respuesta r JOIN encuesta e ON e.id = r.encuesta_id
                WHERE e.codigo = ? GROUP BY r.origen ORDER BY r.origen""")) {
            ps.setString(1, encuesta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) partes.add(rs.getInt(2) + " " + rs.getString(1));
            }
        }
        return "Base de datos" + (partes.isEmpty() ? " (sin respuestas)" : ": " + String.join(" · ", partes));
    }

    static void invalidar() {
        CACHE.clear();
    }

    // ===================== CRUD para el panel =====================

    public static Map<String, Object> listar(String encuesta, LocalDate desde, LocalDate hasta, String origen, int pagina, int tamano) {
        return Db.usar(c -> {
            String where = " WHERE e.codigo = ? AND (?::date IS NULL OR (r.fecha AT TIME ZONE 'America/La_Paz')::date >= ?::date)"
                + " AND (?::date IS NULL OR (r.fecha AT TIME ZONE 'America/La_Paz')::date <= ?::date) AND (?::text IS NULL OR r.origen = ?)";
            Map<String, Object> res = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM respuesta r JOIN encuesta e ON e.id = r.encuesta_id" + where)) {
                parametrosFiltro(ps, encuesta, desde, hasta, origen);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); res.put("total", rs.getInt(1)); }
            }
            List<Map<String, Object>> filas = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT r.id, r.fecha, r.origen, r.version, r.datos::text AS datos,
                           (ct.respuesta_id IS NOT NULL) AS tiene_contacto
                    FROM respuesta r JOIN encuesta e ON e.id = r.encuesta_id
                    LEFT JOIN contacto ct ON ct.respuesta_id = r.id""" + where + " ORDER BY r.fecha DESC, r.id DESC LIMIT ? OFFSET ?")) {
                parametrosFiltro(ps, encuesta, desde, hasta, origen);
                ps.setInt(8, tamano);
                ps.setInt(9, Math.max(0, pagina - 1) * tamano);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> f = new LinkedHashMap<>();
                        f.put("id", rs.getLong("id"));
                        f.put("fecha", rs.getTimestamp("fecha").toInstant().atZone(ZONA).toLocalDateTime().toString().replace('T', ' '));
                        f.put("origen", rs.getString("origen"));
                        f.put("version", rs.getInt("version"));
                        f.put("datos", Json.leer(rs.getString("datos")));
                        f.put("tieneContacto", rs.getBoolean("tiene_contacto"));
                        filas.add(f);
                    }
                }
            }
            res.put("filas", filas);
            return res;
        });
    }

    private static void parametrosFiltro(PreparedStatement ps, String encuesta, LocalDate desde, LocalDate hasta, String origen) throws SQLException {
        ps.setString(1, encuesta);
        ps.setObject(2, desde);
        ps.setObject(3, desde);
        ps.setObject(4, hasta);
        ps.setObject(5, hasta);
        ps.setString(6, origen);
        ps.setString(7, origen);
    }

    public static Map<String, Object> obtener(long id) {
        return Db.usar(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT r.id, e.codigo AS encuesta, r.fecha, r.origen, r.version, r.datos::text AS datos,
                           ct.email, ct.telefono, ct.linkedin
                    FROM respuesta r JOIN encuesta e ON e.id = r.encuesta_id
                    LEFT JOIN contacto ct ON ct.respuesta_id = r.id WHERE r.id = ?""")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new Encuestas.NoEncontrado("No existe la respuesta " + id);
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", rs.getLong("id"));
                    r.put("encuesta", rs.getString("encuesta"));
                    r.put("fecha", rs.getTimestamp("fecha").toInstant().atZone(ZONA).toLocalDateTime().toString().replace('T', ' '));
                    r.put("origen", rs.getString("origen"));
                    r.put("version", rs.getInt("version"));
                    r.put("datos", Json.leer(rs.getString("datos")));
                    Map<String, Object> ct = new LinkedHashMap<>();
                    ct.put("email", rs.getString("email"));
                    ct.put("telefono", rs.getString("telefono"));
                    ct.put("linkedin", rs.getString("linkedin"));
                    r.put("contacto", ct);
                    return r;
                }
            }
        });
    }

    /** Corrige una respuesta (desde el panel): misma validación del formulario, sin exigir obligatorias. */
    public static Map<String, Object> editar(long id, Map<String, Object> valores) {
        Map<String, Object> actual = obtener(id);
        Map<String, Object> estructura = Encuestas.estructura((String) actual.get("encuesta"), true);
        Validado v = validar(estructura, valores, false);
        Db.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE respuesta SET datos = ?::jsonb WHERE id = ?")) {
                ps.setString(1, Json.escribir(v.datos()));
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return null;
        });
        invalidar();
        return Map.of("id", id);
    }

    public static Map<String, Object> eliminar(long id) {
        int n = Db.usar(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM respuesta WHERE id = ?")) {
                ps.setLong(1, id);
                return ps.executeUpdate();
            }
        });
        if (n == 0) throw new Encuestas.NoEncontrado("No existe la respuesta " + id);
        invalidar();
        return Map.of("id", id, "resultado", "eliminada");
    }

    /** Borra todas las respuestas de un origen (por ejemplo, el dataset de prueba). */
    public static Map<String, Object> eliminarPorOrigen(String encuesta, String origen) {
        if (!ORIGENES.contains(origen)) throw new IllegalArgumentException("Origen no válido: " + origen);
        int n = Db.usar(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    DELETE FROM respuesta WHERE origen = ? AND (?::text IS NULL OR encuesta_id = (SELECT id FROM encuesta WHERE codigo = ?))""")) {
                ps.setString(1, origen);
                ps.setString(2, encuesta);
                ps.setString(3, encuesta);
                return ps.executeUpdate();
            }
        });
        invalidar();
        return Map.of("eliminadas", n);
    }

    /** Respuestas por día de los últimos días, para el panel. */
    public static List<Map<String, Object>> porDia(String encuesta, int dias) {
        return Db.usar(c -> {
            List<Map<String, Object>> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT (r.fecha AT TIME ZONE 'America/La_Paz')::date AS dia, count(*) AS n
                    FROM respuesta r JOIN encuesta e ON e.id = r.encuesta_id
                    WHERE e.codigo = ? AND r.origen = 'formulario' AND r.fecha >= now() - make_interval(days => ?)
                    GROUP BY 1 ORDER BY 1""")) {
                ps.setString(1, encuesta);
                ps.setInt(2, dias);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) lista.add(Map.of("dia", rs.getDate("dia").toString(), "n", rs.getInt("n")));
                }
            }
            return lista;
        });
    }

    // ===================== Importación =====================

    /**
     * Guarda en la base las filas de un CSV (importado de Forms o el dataset de
     * prueba). Solo se guardan columnas que son preguntas de la encuesta; las de
     * contacto van a su tabla. No exige obligatorias: Forms ya tiene sus reglas.
     */
    @SuppressWarnings("unchecked")
    public static int importar(String encuesta, List<Map<String, String>> filasCsv, String origen) {
        if (!ORIGENES.contains(origen)) throw new IllegalArgumentException("Origen no válido: " + origen);
        Map<String, Object> estructura = Encuestas.estructura(encuesta, true);
        Map<String, Map<String, Object>> preguntas = new LinkedHashMap<>();
        for (Map<String, Object> s : (List<Map<String, Object>>) estructura.get("secciones")) {
            for (Map<String, Object> p : (List<Map<String, Object>>) s.get("preguntas")) preguntas.put((String) p.get("codigo"), p);
        }
        int n = Db.transaccion(c -> {
            int insertadas = 0;
            for (Map<String, String> fila : filasCsv) {
                // El Dataset normaliza la fecha de Forms y quita contactos; aquí se leen los contactos antes
                Map<String, String> contacto = new LinkedHashMap<>();
                for (String k : List.of("email", "correo", "telefono", "linkedin")) {
                    String v = fila.get(k);
                    if (v != null && !v.isBlank()) contacto.put(k, v.trim());
                }
                Dataset d = new Dataset("csv", List.of(new LinkedHashMap<>(fila)));
                Map<String, String> limpia = d.filas.get(0);
                LocalDate fecha = Dataset.fecha(limpia);
                Map<String, Object> datos = new LinkedHashMap<>();
                limpia.forEach((k, v) -> {
                    Map<String, Object> p = preguntas.get(k);
                    if (p == null || v == null || v.isBlank()) return;
                    switch ((String) p.get("tipo")) {
                        case "multiple" -> datos.put(k, List.of(v.split("\\|")).stream().map(String::trim).filter(x -> !x.isEmpty()).toList());
                        case "numero" -> {
                            try {
                                double x = Double.parseDouble(v.replace(',', '.'));
                                datos.put(k, x == Math.rint(x) ? (Object) (long) x : (Object) x);
                            } catch (NumberFormatException e) {
                                datos.put(k, v);
                            }
                        }
                        default -> datos.put(k, v.trim());
                    }
                });
                insertar(c, (Integer) estructura.get("id"), (Integer) estructura.get("version"), origen, fecha, datos, contacto);
                insertadas++;
            }
            return insertadas;
        });
        invalidar();
        return n;
    }

    /**
     * Guarda las filas ya traducidas por FormsImport (origen 'importado'). Si
     * reemplazar es true, antes borra las importadas de esa encuesta, para que
     * volver a subir el mismo archivo no duplique respuestas.
     */
    public static int guardarImportadas(String encuesta, List<FormsImport.Fila> filas, boolean reemplazar) {
        Map<String, Object> estructura = Encuestas.estructura(encuesta, true);
        int n = Db.transaccion(c -> {
            if (reemplazar) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM respuesta WHERE origen = 'importado' AND encuesta_id = ?")) {
                    ps.setInt(1, (Integer) estructura.get("id"));
                    ps.executeUpdate();
                }
            }
            int insertadas = 0;
            for (FormsImport.Fila f : filas) {
                insertar(c, (Integer) estructura.get("id"), (Integer) estructura.get("version"), "importado", f.fecha(), f.datos(), f.contacto());
                insertadas++;
            }
            return insertadas;
        });
        invalidar();
        return n;
    }
}
