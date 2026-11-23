package com.actividad2;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estructura de las encuestas (secciones, preguntas, opciones) y su CRUD para el
 * editor. La estructura pública se guarda en memoria: el formulario la pide en
 * cada visita y así la base se consulta una sola vez hasta que el editor cambie
 * algo.
 *
 * Reglas para no perder datos ya respondidos:
 *  - una pregunta u opción con respuestas no se borra: se desactiva;
 *  - a una pregunta con respuestas no se le cambia el código ni el tipo;
 *  - cada cambio de estructura sube la versión de la encuesta, y cada respuesta
 *    guarda la versión con que fue contestada.
 */
public class Encuestas {

    static final Set<String> TIPOS = Set.of("unica", "multiple", "numero", "texto", "escala", "email", "telefono");
    static final Set<String> CON_OPCIONES = Set.of("unica", "multiple", "escala");
    private static final Map<String, Map<String, Object>> CACHE = new ConcurrentHashMap<>();

    // ===================== Lectura =====================

    /** Encuesta con sus secciones, preguntas y opciones. Pública: solo lo activo; admin: todo con conteos de uso. */
    public static Map<String, Object> estructura(String codigo, boolean admin) {
        if (!admin) {
            Map<String, Object> enCache = CACHE.get(codigo);
            if (enCache != null) return enCache;
        }
        Map<String, Object> r = Db.usar(c -> leer(c, codigo, admin));
        if (!admin) CACHE.put(codigo, r);
        return r;
    }

    static void invalidar() {
        CACHE.clear();
    }

    private static Map<String, Object> leer(Connection c, String codigo, boolean admin) throws SQLException {
        Map<String, Object> e = new LinkedHashMap<>();
        int encuestaId;
        try (PreparedStatement ps = c.prepareStatement("SELECT id, codigo, nombre, introduccion, version FROM encuesta WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NoEncontrado("No existe la encuesta '" + codigo + "'");
                encuestaId = rs.getInt("id");
                e.put("id", encuestaId);
                e.put("codigo", rs.getString("codigo"));
                e.put("nombre", rs.getString("nombre"));
                e.put("introduccion", rs.getString("introduccion"));
                e.put("version", rs.getInt("version"));
            }
        }
        Map<Integer, Map<String, Object>> secciones = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT id, orden, titulo FROM seccion WHERE encuesta_id = ? ORDER BY orden, id")) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("id", rs.getInt("id"));
                    s.put("orden", rs.getInt("orden"));
                    s.put("titulo", rs.getString("titulo"));
                    s.put("preguntas", new ArrayList<Map<String, Object>>());
                    secciones.put(rs.getInt("id"), s);
                }
            }
        }
        Map<Integer, Map<String, Object>> preguntas = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM pregunta WHERE encuesta_id = ? " + (admin ? "" : "AND activa ") + "ORDER BY orden, id")) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> p = preguntaDesde(rs);
                    preguntas.put(rs.getInt("id"), p);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> lista = (List<Map<String, Object>>) secciones.get(rs.getInt("seccion_id")).get("preguntas");
                    lista.add(p);
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT o.* FROM opcion o JOIN pregunta p ON p.id = o.pregunta_id
                WHERE p.encuesta_id = ?""" + (admin ? "" : " AND o.activa") + " ORDER BY o.orden, o.id")) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> p = preguntas.get(rs.getInt("pregunta_id"));
                    if (p == null) continue;
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("id", rs.getInt("id"));
                    o.put("codigo", rs.getString("codigo"));
                    o.put("etiqueta", rs.getString("etiqueta"));
                    o.put("orden", rs.getInt("orden"));
                    o.put("activa", rs.getBoolean("activa"));
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> ops = (List<Map<String, Object>>) p.get("opciones");
                    ops.add(o);
                }
            }
        }
        if (admin) agregarConteos(c, encuestaId, preguntas.values());
        e.put("secciones", new ArrayList<>(secciones.values()));
        return e;
    }

    private static Map<String, Object> preguntaDesde(ResultSet rs) throws SQLException {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", rs.getInt("id"));
        p.put("seccionId", rs.getInt("seccion_id"));
        p.put("codigo", rs.getString("codigo"));
        p.put("texto", rs.getString("texto"));
        p.put("ayuda", rs.getString("ayuda"));
        p.put("tipo", rs.getString("tipo"));
        p.put("obligatoria", rs.getBoolean("obligatoria"));
        p.put("orden", rs.getInt("orden"));
        p.put("dependeDe", rs.getString("depende_de"));
        String valores = rs.getString("depende_valores");
        p.put("dependeValores", valores == null || valores.isBlank() ? List.of() : Arrays.stream(valores.split(",")).map(String::trim).toList());
        p.put("maxSelecciones", rs.getObject("max_selecciones"));
        p.put("valorMinimo", numero(rs.getBigDecimal("valor_minimo")));
        p.put("valorMaximo", numero(rs.getBigDecimal("valor_maximo")));
        p.put("esContacto", rs.getBoolean("es_contacto"));
        p.put("activa", rs.getBoolean("activa"));
        p.put("opciones", new ArrayList<Map<String, Object>>());
        return p;
    }

    private static Double numero(BigDecimal b) {
        return b == null ? null : b.doubleValue();
    }

    /**
     * Cuántas respuestas tiene cada pregunta y cada opción (para saber qué se puede borrar).
     * Son dos consultas agrupadas para toda la encuesta: antes se hacía un count(*) por pregunta
     * y otro por opción (más de 200 consultas, medio segundo en cada recarga del editor).
     * Cuentan lo mismo que contarUsosPregunta y contarUsosOpcion, que se siguen usando al editar o borrar.
     */
    @SuppressWarnings("unchecked")
    private static void agregarConteos(Connection c, int encuestaId, Iterable<Map<String, Object>> preguntas) throws SQLException {
        // Respuestas en que aparece cada pregunta (igual que datos -> pregunta IS NOT NULL)
        Map<String, Integer> porPregunta = new java.util.HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT e.key, count(*) FROM respuesta r CROSS JOIN LATERAL jsonb_each(r.datos) e
                WHERE r.encuesta_id = ? AND jsonb_typeof(r.datos) = 'object' GROUP BY e.key""")) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) porPregunta.put(rs.getString(1), rs.getInt(2)); }
        }
        // Respuestas que eligieron cada opción: el valor es esa opción (texto) o una lista que la contiene
        // (igual que datos -> pregunta @> '"opcion"'); DISTINCT para no contar dos veces una lista repetida
        Map<String, Integer> porOpcion = new java.util.HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT e.key, x.valor, count(DISTINCT r.id)
                FROM respuesta r
                CROSS JOIN LATERAL jsonb_each(r.datos) e
                CROSS JOIN LATERAL (
                    SELECT e.value #>> '{}' AS valor WHERE jsonb_typeof(e.value) = 'string'
                    UNION ALL
                    SELECT el #>> '{}' FROM jsonb_array_elements(
                        CASE WHEN jsonb_typeof(e.value) = 'array' THEN e.value ELSE '[]'::jsonb END) el
                    WHERE jsonb_typeof(el) = 'string'
                ) x
                WHERE r.encuesta_id = ? AND jsonb_typeof(r.datos) = 'object'
                GROUP BY e.key, x.valor""")) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) porOpcion.put(rs.getString(1) + "\u0000" + rs.getString(2), rs.getInt(3)); }
        }
        for (Map<String, Object> p : preguntas) {
            String codigo = (String) p.get("codigo");
            p.put("respuestas", (Boolean) p.get("esContacto") ? 0 : porPregunta.getOrDefault(codigo, 0));
            for (Map<String, Object> o : (List<Map<String, Object>>) p.get("opciones")) {
                o.put("respuestas", porOpcion.getOrDefault(codigo + "\u0000" + o.get("codigo"), 0));
            }
        }
    }

    static int contarUsosPregunta(Connection c, int encuestaId, String codigoPregunta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM respuesta WHERE encuesta_id = ? AND datos -> ? IS NOT NULL")) {
            ps.setInt(1, encuestaId);
            ps.setString(2, codigoPregunta);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    // datos -> 'pregunta' @> '"opcion"' es verdadero si el valor es esa opción o si es una lista que la contiene
    static int contarUsosOpcion(Connection c, int encuestaId, String codigoPregunta, String codigoOpcion) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM respuesta WHERE encuesta_id = ? AND datos -> ? @> to_jsonb(?::text)")) {
            ps.setInt(1, encuestaId);
            ps.setString(2, codigoPregunta);
            ps.setString(3, codigoOpcion);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** Lista de encuestas con el número de respuestas por origen. */
    public static List<Map<String, Object>> listar() {
        return Db.usar(c -> {
            List<Map<String, Object>> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT e.id, e.codigo, e.nombre, e.version,
                           count(r.id) FILTER (WHERE r.origen = 'formulario') AS formulario,
                           count(r.id) FILTER (WHERE r.origen = 'importado')  AS importado,
                           count(r.id) FILTER (WHERE r.origen = 'prueba')     AS prueba
                    FROM encuesta e LEFT JOIN respuesta r ON r.encuesta_id = e.id
                    GROUP BY e.id ORDER BY e.id""");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("id", rs.getInt("id"));
                    e.put("codigo", rs.getString("codigo"));
                    e.put("nombre", rs.getString("nombre"));
                    e.put("version", rs.getInt("version"));
                    e.put("respuestas", Map.of("formulario", rs.getInt("formulario"), "importado", rs.getInt("importado"), "prueba", rs.getInt("prueba")));
                    lista.add(e);
                }
            }
            return lista;
        });
    }

    // ===================== CRUD de secciones =====================

    public static Map<String, Object> crearSeccion(Map<String, Object> d) {
        String encuesta = texto(d, "encuesta", true);
        String titulo = texto(d, "titulo", true);
        return cambio(c -> {
            int encuestaId = idEncuesta(c, encuesta);
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO seccion (encuesta_id, orden, titulo)
                    VALUES (?, (SELECT coalesce(max(orden), 0) + 1 FROM seccion WHERE encuesta_id = ?), ?) RETURNING id""")) {
                ps.setInt(1, encuestaId);
                ps.setInt(2, encuestaId);
                ps.setString(3, titulo);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return Map.of("id", rs.getInt(1)); }
            }
        }, encuesta);
    }

    public static Map<String, Object> editarSeccion(int id, Map<String, Object> d) {
        return cambio(c -> {
            String encuesta = encuestaDeSeccion(c, id);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE seccion SET titulo = coalesce(?, titulo), orden = coalesce(?, orden) WHERE id = ?")) {
                ps.setString(1, texto(d, "titulo", false));
                ps.setObject(2, entero(d, "orden"));
                ps.setInt(3, id);
                ps.executeUpdate();
            }
            subirVersion(c, encuesta);
            return Map.of("id", id);
        }, null);
    }

    public static Map<String, Object> borrarSeccion(int id) {
        return cambio(c -> {
            String encuesta = encuestaDeSeccion(c, id);
            try (PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM pregunta WHERE seccion_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) throw new IllegalArgumentException("La sección tiene preguntas: muévalas o elimínelas primero");
                }
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM seccion WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            subirVersion(c, encuesta);
            return Map.of("id", id, "resultado", "eliminada");
        }, null);
    }

    // ===================== CRUD de preguntas =====================

    public static Map<String, Object> crearPregunta(Map<String, Object> d) {
        String encuesta = texto(d, "encuesta", true);
        String codigo = codigoValido(texto(d, "codigo", true));
        String tipo = tipoValido(texto(d, "tipo", true));
        int seccionId = enteroObligatorio(d, "seccionId");
        String dependeDe = texto(d, "dependeDe", false);
        validarRango(decimal(d, "valorMinimo"), decimal(d, "valorMaximo"));
        return cambio(c -> {
            int encuestaId = idEncuesta(c, encuesta);
            if (!encuesta.equals(encuestaDeSeccion(c, seccionId))) throw new IllegalArgumentException("La sección no pertenece a la encuesta");
            validarDependencia(c, encuestaId, Set.of(codigo), dependeDe);
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO pregunta (encuesta_id, seccion_id, codigo, texto, ayuda, tipo, obligatoria, orden,
                                          depende_de, depende_valores, max_selecciones, valor_minimo, valor_maximo, es_contacto, activa)
                    VALUES (?, ?, ?, ?, ?, ?, ?, (SELECT coalesce(max(orden), 0) + 1 FROM pregunta WHERE seccion_id = ?),
                            ?, ?, ?, ?, ?, ?, ?) RETURNING id""")) {
                ps.setInt(1, encuestaId);
                ps.setInt(2, seccionId);
                ps.setString(3, codigo);
                ps.setString(4, texto(d, "texto", true));
                ps.setString(5, texto(d, "ayuda", false));
                ps.setString(6, tipo);
                ps.setBoolean(7, Boolean.TRUE.equals(d.get("obligatoria")));
                ps.setInt(8, seccionId);
                ps.setString(9, dependeDe);
                ps.setString(10, dependeDe == null ? null : valoresDependencia(d));
                ps.setObject(11, entero(d, "maxSelecciones"));
                ps.setObject(12, decimal(d, "valorMinimo"));
                ps.setObject(13, decimal(d, "valorMaximo"));
                ps.setBoolean(14, Boolean.TRUE.equals(d.get("esContacto")));
                ps.setBoolean(15, !Boolean.FALSE.equals(d.get("activa")));
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return Map.of("id", rs.getInt(1)); }
            } catch (SQLException ex) {
                if ("23505".equals(ex.getSQLState())) throw new IllegalArgumentException("Ya existe una pregunta con el código '" + codigo + "'");
                throw ex;
            }
        }, encuesta);
    }

    public static Map<String, Object> editarPregunta(int id, Map<String, Object> d) {
        return cambio(c -> {
            Map<String, Object> actual = preguntaPorId(c, id);
            int encuestaId = (Integer) actual.get("encuestaId");
            String codigoActual = (String) actual.get("codigo");
            String codigo = d.containsKey("codigo") ? codigoValido(texto(d, "codigo", true)) : codigoActual;
            String tipo = d.containsKey("tipo") ? tipoValido(texto(d, "tipo", true)) : (String) actual.get("tipo");
            boolean cambiaClave = !codigo.equals(codigoActual) || !tipo.equals(actual.get("tipo"));
            if (cambiaClave && contarUsosPregunta(c, encuestaId, codigoActual) > 0) {
                throw new IllegalArgumentException("La pregunta ya tiene respuestas: no se puede cambiar su código ni su tipo. "
                    + "Desactívela y cree una nueva.");
            }
            int seccionActual = (Integer) actual.get("seccionId");
            int seccionId = d.containsKey("seccionId") ? enteroObligatorio(d, "seccionId") : seccionActual;
            if (seccionId != seccionActual && encuestaId != idEncuesta(c, encuestaDeSeccion(c, seccionId))) {
                throw new IllegalArgumentException("La sección no pertenece a la encuesta");
            }
            // Al cambiar de sección va al final de la nueva: conservar el orden viejo podía empatar con otra pregunta
            int orden = d.containsKey("orden") ? enteroObligatorio(d, "orden")
                : seccionId != seccionActual ? siguienteOrden(c, seccionId) : (Integer) actual.get("orden");
            String dependeActual = (String) actual.get("dependeDe");
            String dependeDe = d.containsKey("dependeDe") ? texto(d, "dependeDe", false) : dependeActual;
            if (!java.util.Objects.equals(dependeDe, dependeActual) || !codigo.equals(codigoActual)) {
                validarDependencia(c, encuestaId, Set.of(codigo, codigoActual), dependeDe);
            }
            Object min = d.containsKey("valorMinimo") ? decimal(d, "valorMinimo") : actual.get("valorMinimo");
            Object max = d.containsKey("valorMaximo") ? decimal(d, "valorMaximo") : actual.get("valorMaximo");
            validarRango(min == null ? null : new BigDecimal(String.valueOf(min)), max == null ? null : new BigDecimal(String.valueOf(max)));
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE pregunta SET codigo = ?, tipo = ?, texto = ?, ayuda = ?, obligatoria = ?, orden = ?, seccion_id = ?,
                           depende_de = ?, depende_valores = ?, max_selecciones = ?, valor_minimo = ?, valor_maximo = ?,
                           es_contacto = ?, activa = ?
                    WHERE id = ?""")) {
                ps.setString(1, codigo);
                ps.setString(2, tipo);
                ps.setString(3, d.containsKey("texto") ? texto(d, "texto", true) : (String) actual.get("texto"));
                ps.setString(4, d.containsKey("ayuda") ? texto(d, "ayuda", false) : (String) actual.get("ayuda"));
                ps.setBoolean(5, d.containsKey("obligatoria") ? Boolean.TRUE.equals(d.get("obligatoria")) : (Boolean) actual.get("obligatoria"));
                ps.setInt(6, orden);
                ps.setInt(7, seccionId);
                ps.setString(8, dependeDe);
                ps.setString(9, dependeDe == null ? null
                    : d.containsKey("dependeValores") ? valoresDependencia(d) : (String) actual.get("dependeValores"));
                ps.setObject(10, d.containsKey("maxSelecciones") ? entero(d, "maxSelecciones") : actual.get("maxSelecciones"));
                ps.setObject(11, min);
                ps.setObject(12, max);
                ps.setBoolean(13, d.containsKey("esContacto") ? Boolean.TRUE.equals(d.get("esContacto")) : (Boolean) actual.get("esContacto"));
                ps.setBoolean(14, d.containsKey("activa") ? !Boolean.FALSE.equals(d.get("activa")) : (Boolean) actual.get("activa"));
                ps.setInt(15, id);
                ps.executeUpdate();
            } catch (SQLException ex) {
                if ("23505".equals(ex.getSQLState())) throw new IllegalArgumentException("Ya existe una pregunta con el código '" + codigo + "'");
                throw ex;
            }
            // Las condiciones de otras preguntas se guardan por código: si cambia, se arrastra el código nuevo
            if (!codigo.equals(codigoActual)) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE pregunta SET depende_de = ? WHERE encuesta_id = ? AND depende_de = ?")) {
                    ps.setString(1, codigo);
                    ps.setInt(2, encuestaId);
                    ps.setString(3, codigoActual);
                    ps.executeUpdate();
                }
            }
            // Si dejó de ser de opciones (solo posible sin respuestas), sus opciones ya no tienen sentido
            if (!tipo.equals(actual.get("tipo")) && !CON_OPCIONES.contains(tipo)) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM opcion WHERE pregunta_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }
            subirVersion(c, encuestaId);
            return Map.of("id", id);
        }, null);
    }

    public static Map<String, Object> borrarPregunta(int id) {
        return cambio(c -> {
            Map<String, Object> actual = preguntaPorId(c, id);
            int encuestaId = (Integer) actual.get("encuestaId");
            String resultado;
            if (contarUsosPregunta(c, encuestaId, (String) actual.get("codigo")) > 0) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE pregunta SET activa = FALSE WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                resultado = "desactivada";
            } else {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM opcion WHERE pregunta_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM pregunta WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                resultado = "eliminada";
            }
            subirVersion(c, encuestaId);
            return Map.of("id", id, "resultado", resultado);
        }, null);
    }

    // ===================== CRUD de opciones =====================

    public static Map<String, Object> crearOpcion(Map<String, Object> d) {
        int preguntaId = enteroObligatorio(d, "preguntaId");
        String codigo = codigoOpcionValido(texto(d, "codigo", true));
        String etiqueta = texto(d, "etiqueta", true);
        return cambio(c -> {
            Map<String, Object> p = preguntaPorId(c, preguntaId);
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO opcion (pregunta_id, codigo, etiqueta, orden)
                    VALUES (?, ?, ?, (SELECT coalesce(max(orden), 0) + 1 FROM opcion WHERE pregunta_id = ?)) RETURNING id""")) {
                ps.setInt(1, preguntaId);
                ps.setString(2, codigo);
                ps.setString(3, etiqueta);
                ps.setInt(4, preguntaId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    subirVersion(c, (Integer) p.get("encuestaId"));
                    return Map.of("id", rs.getInt(1));
                }
            } catch (SQLException ex) {
                if ("23505".equals(ex.getSQLState())) throw new IllegalArgumentException("La pregunta ya tiene una opción con el código '" + codigo + "'");
                throw ex;
            }
        }, null);
    }

    public static Map<String, Object> editarOpcion(int id, Map<String, Object> d) {
        return cambio(c -> {
            Map<String, Object> o = opcionPorId(c, id);
            int encuestaId = (Integer) o.get("encuestaId");
            String codigoActual = (String) o.get("codigo");
            String codigo = d.containsKey("codigo") ? codigoOpcionValido(texto(d, "codigo", true)) : codigoActual;
            if (!codigo.equals(codigoActual) && contarUsosOpcion(c, encuestaId, (String) o.get("pregunta"), codigoActual) > 0) {
                throw new IllegalArgumentException("La opción ya fue elegida en respuestas: no se puede cambiar su código (sí su texto).");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE opcion SET codigo = ?, etiqueta = ?, orden = ?, activa = ? WHERE id = ?")) {
                ps.setString(1, codigo);
                ps.setString(2, d.containsKey("etiqueta") ? texto(d, "etiqueta", true) : (String) o.get("etiqueta"));
                ps.setInt(3, d.containsKey("orden") ? enteroObligatorio(d, "orden") : (Integer) o.get("orden"));
                ps.setBoolean(4, d.containsKey("activa") ? !Boolean.FALSE.equals(d.get("activa")) : (Boolean) o.get("activa"));
                ps.setInt(5, id);
                ps.executeUpdate();
            }
            subirVersion(c, encuestaId);
            return Map.of("id", id);
        }, null);
    }

    public static Map<String, Object> borrarOpcion(int id) {
        return cambio(c -> {
            Map<String, Object> o = opcionPorId(c, id);
            int encuestaId = (Integer) o.get("encuestaId");
            String resultado;
            if (contarUsosOpcion(c, encuestaId, (String) o.get("pregunta"), (String) o.get("codigo")) > 0) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE opcion SET activa = FALSE WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                resultado = "desactivada";
            } else {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM opcion WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                resultado = "eliminada";
            }
            subirVersion(c, encuestaId);
            return Map.of("id", id, "resultado", resultado);
        }, null);
    }

    // ===================== Orden (arrastrar en el editor) =====================

    /**
     * Guarda de una sola vez el orden que quedó al arrastrar: secciones y, dentro de
     * cada una, sus preguntas (una pregunta puede haber pasado a otra sección).
     * Se exige la lista completa para que el orden quede 1, 2, 3... sin empates.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> reordenar(String encuesta, Map<String, Object> d) {
        List<Object> secciones = lista(d, "secciones");
        return cambio(c -> {
            int encuestaId = idEncuesta(c, encuesta);
            Set<Integer> seccionesEsperadas = ids(c, "SELECT id FROM seccion WHERE encuesta_id = ?", encuestaId);
            Set<Integer> preguntasEsperadas = ids(c, "SELECT id FROM pregunta WHERE encuesta_id = ?", encuestaId);
            Set<Integer> seccionesVistas = new HashSet<>(), preguntasVistas = new HashSet<>();
            try (PreparedStatement ps = c.prepareStatement("UPDATE seccion SET orden = ? WHERE id = ?");
                 PreparedStatement pp = c.prepareStatement("UPDATE pregunta SET seccion_id = ?, orden = ? WHERE id = ?")) {
                int i = 0;
                for (Object o : secciones) {
                    if (!(o instanceof Map)) throw new IllegalArgumentException("Cada sección debe ser un objeto {id, preguntas}");
                    Map<String, Object> s = (Map<String, Object>) o;
                    int seccionId = enteroObligatorio(s, "id");
                    seccionesVistas.add(seccionId);
                    ps.setInt(1, ++i);
                    ps.setInt(2, seccionId);
                    ps.addBatch();
                    int j = 0;
                    for (Object v : lista(s, "preguntas")) {
                        int preguntaId = aEntero(v, "preguntas");
                        if (!preguntasVistas.add(preguntaId)) throw new IllegalArgumentException("La pregunta " + preguntaId + " aparece dos veces");
                        pp.setInt(1, seccionId);
                        pp.setInt(2, ++j);
                        pp.setInt(3, preguntaId);
                        pp.addBatch();
                    }
                }
                if (i != seccionesVistas.size() || !seccionesVistas.equals(seccionesEsperadas) || !preguntasVistas.equals(preguntasEsperadas)) {
                    throw new IllegalArgumentException("El orden enviado no coincide con la encuesta guardada: recargue el editor");
                }
                ps.executeBatch();
                pp.executeBatch();
            }
            return Map.of("resultado", "reordenada");
        }, encuesta);
    }

    /** Orden de las opciones de una pregunta (lista completa de ids en el orden nuevo). */
    public static Map<String, Object> reordenarOpciones(int preguntaId, Map<String, Object> d) {
        List<Object> lista = lista(d, "ids");
        return cambio(c -> {
            Map<String, Object> p = preguntaPorId(c, preguntaId);
            Set<Integer> esperadas = ids(c, "SELECT id FROM opcion WHERE pregunta_id = ?", preguntaId);
            Set<Integer> vistas = new HashSet<>();
            try (PreparedStatement ps = c.prepareStatement("UPDATE opcion SET orden = ? WHERE id = ?")) {
                int i = 0;
                for (Object v : lista) {
                    int opcionId = aEntero(v, "ids");
                    vistas.add(opcionId);
                    ps.setInt(1, ++i);
                    ps.setInt(2, opcionId);
                    ps.addBatch();
                }
                if (i != vistas.size() || !vistas.equals(esperadas)) {
                    throw new IllegalArgumentException("El orden enviado no coincide con las opciones guardadas: recargue el editor");
                }
                ps.executeBatch();
            }
            subirVersion(c, (Integer) p.get("encuestaId"));
            return Map.of("resultado", "reordenada");
        }, null);
    }

    // ===================== utilidades =====================

    /**
     * La condición tiene que apuntar a otra pregunta con opciones, y no puede formar
     * un ciclo (A depende de B y B de A): en ese caso ninguna de las dos se mostraría.
     */
    private static void validarDependencia(Connection c, int encuestaId, Set<String> propios, String dependeDe) throws SQLException {
        if (dependeDe == null) return;
        if (propios.contains(dependeDe)) throw new IllegalArgumentException("Una pregunta no puede depender de sí misma");
        Set<String> recorridos = new HashSet<>();
        String actual = dependeDe;
        try (PreparedStatement ps = c.prepareStatement("SELECT tipo, depende_de FROM pregunta WHERE encuesta_id = ? AND codigo = ?")) {
            while (actual != null && recorridos.add(actual)) {
                ps.setInt(1, encuestaId);
                ps.setString(2, actual);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        if (actual.equals(dependeDe)) throw new IllegalArgumentException("No existe la pregunta '" + dependeDe + "' de la que depende");
                        return;
                    }
                    if (actual.equals(dependeDe) && !CON_OPCIONES.contains(rs.getString("tipo"))) {
                        throw new IllegalArgumentException("Solo se puede depender de una pregunta con opciones");
                    }
                    actual = rs.getString("depende_de");
                }
                if (actual != null && propios.contains(actual)) {
                    throw new IllegalArgumentException("Condición circular: '" + dependeDe + "' ya depende (directa o indirectamente) de esta pregunta");
                }
            }
        }
    }

    private static void validarRango(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("El valor mínimo no puede ser mayor que el máximo");
        }
    }

    private static int siguienteOrden(Connection c, int seccionId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT coalesce(max(orden), 0) + 1 FROM pregunta WHERE seccion_id = ?")) {
            ps.setInt(1, seccionId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static Set<Integer> ids(Connection c, String sql, int parametro) throws SQLException {
        Set<Integer> r = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, parametro);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) r.add(rs.getInt(1)); }
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> lista(Map<String, Object> d, String clave) {
        if (!(d.get(clave) instanceof List<?> l)) throw new IllegalArgumentException("El campo '" + clave + "' debe ser una lista");
        return (List<Object>) l;
    }

    /** Ejecuta un cambio de estructura en una transacción y vacía la caché del formulario. */
    private static Map<String, Object> cambio(Db.Trabajo<Map<String, Object>> trabajo, String encuestaParaVersion) {
        Map<String, Object> r = Db.transaccion(c -> {
            Map<String, Object> res = trabajo.con(c);
            if (encuestaParaVersion != null) subirVersion(c, encuestaParaVersion);
            return res;
        });
        invalidar();
        return r;
    }

    private static void subirVersion(Connection c, String encuesta) throws SQLException {
        subirVersion(c, idEncuesta(c, encuesta));
    }

    private static void subirVersion(Connection c, int encuestaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE encuesta SET version = version + 1 WHERE id = ?")) {
            ps.setInt(1, encuestaId);
            ps.executeUpdate();
        }
    }

    static int idEncuesta(Connection c, String codigo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM encuesta WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NoEncontrado("No existe la encuesta '" + codigo + "'");
                return rs.getInt(1);
            }
        }
    }

    private static String encuestaDeSeccion(Connection c, int seccionId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT e.codigo FROM seccion s JOIN encuesta e ON e.id = s.encuesta_id WHERE s.id = ?")) {
            ps.setInt(1, seccionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NoEncontrado("No existe la sección " + seccionId);
                return rs.getString(1);
            }
        }
    }

    private static Map<String, Object> preguntaPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM pregunta WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NoEncontrado("No existe la pregunta " + id);
                Map<String, Object> p = preguntaDesde(rs);
                p.put("encuestaId", rs.getInt("encuesta_id"));
                p.put("dependeValores", rs.getString("depende_valores"));
                return p;
            }
        }
    }

    private static Map<String, Object> opcionPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT o.*, p.codigo AS pregunta, p.encuesta_id FROM opcion o JOIN pregunta p ON p.id = o.pregunta_id WHERE o.id = ?""")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NoEncontrado("No existe la opción " + id);
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("codigo", rs.getString("codigo"));
                o.put("etiqueta", rs.getString("etiqueta"));
                o.put("orden", rs.getInt("orden"));
                o.put("activa", rs.getBoolean("activa"));
                o.put("pregunta", rs.getString("pregunta"));
                o.put("encuestaId", rs.getInt("encuesta_id"));
                return o;
            }
        }
    }

    static String texto(Map<String, Object> d, String clave, boolean obligatorio) {
        Object v = d.get(clave);
        String s = v == null ? null : String.valueOf(v).trim();
        if (s != null && s.isEmpty()) s = null;
        if (obligatorio && s == null) throw new IllegalArgumentException("Falta el campo '" + clave + "'");
        return s;
    }

    private static Integer entero(Map<String, Object> d, String clave) {
        Object v = d.get(clave);
        if (v == null || String.valueOf(v).isBlank()) return null;
        return aEntero(v, clave);
    }

    private static int aEntero(Object v, String clave) {
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + clave + "' debe ser un número entero");
        }
    }

    private static int enteroObligatorio(Map<String, Object> d, String clave) {
        Integer v = entero(d, clave);
        if (v == null) throw new IllegalArgumentException("Falta el campo '" + clave + "'");
        return v;
    }

    private static BigDecimal decimal(Map<String, Object> d, String clave) {
        Object v = d.get(clave);
        if (v == null || String.valueOf(v).isBlank()) return null;
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + clave + "' debe ser un número");
        }
    }

    private static String valoresDependencia(Map<String, Object> d) {
        Object v = d.get("dependeValores");
        if (v == null) return null;
        if (v instanceof List<?> l) return l.isEmpty() ? null : String.join(",", l.stream().map(String::valueOf).toList());
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String codigoValido(String codigo) {
        if (!codigo.matches("[a-z][a-z0-9_]{0,59}")) {
            throw new IllegalArgumentException("El código de la pregunta solo puede tener minúsculas, números y '_' (ej. anio_titulacion)");
        }
        return codigo;
    }

    private static String codigoOpcionValido(String codigo) {
        if (codigo.length() > 80 || codigo.contains(",") || codigo.contains("|")) {
            throw new IllegalArgumentException("El código de la opción no puede tener ',' ni '|' ni más de 80 caracteres");
        }
        return codigo;
    }

    private static String tipoValido(String tipo) {
        if (!TIPOS.contains(tipo)) throw new IllegalArgumentException("Tipo de pregunta no válido: " + tipo);
        return tipo;
    }

    /** Para responder 404 en vez de 400. */
    public static class NoEncontrado extends RuntimeException {
        public NoEncontrado(String mensaje) {
            super(mensaje);
        }
    }
}
