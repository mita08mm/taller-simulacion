package com.actividad2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Revisión de calidad de las respuestas guardadas: señala las que conviene
 * mirar antes de analizar (casi vacías, con un nombre de organización sin
 * sentido o repetidas). No borra nada: el usuario decide en el panel.
 */
public class Revision {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final int MINIMO_RESPUESTAS = 5;
    // Sufijos legales y palabras genéricas que no distinguen a una organización ("Comteco RL" = "COMTECO")
    private static final Set<String> SUFIJOS = Set.of("srl", "s", "r", "l", "ltda", "sa", "rl", "corp", "corporation", "sl", "inc", "software", "group");

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> revisar(String encuesta) {
        List<Map<String, Object>> respuestas = Db.usar(c -> {
            List<Map<String, Object>> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT r.id, r.fecha, r.origen, r.datos::text AS datos FROM respuesta r
                    JOIN encuesta e ON e.id = r.encuesta_id WHERE e.codigo = ? ORDER BY r.fecha, r.id""")) {
                ps.setString(1, encuesta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", rs.getLong("id"));
                        m.put("fecha", rs.getTimestamp("fecha").toInstant().atZone(ZONA).toLocalDate().toString());
                        m.put("origen", rs.getString("origen"));
                        m.put("datos", Json.leer(rs.getString("datos")));
                        lista.add(m);
                    }
                }
            }
            return lista;
        });

        List<Map<String, Object>> avisos = new ArrayList<>();
        Map<String, List<Map<String, Object>>> grupos = new LinkedHashMap<>();
        for (Map<String, Object> r : respuestas) {
            Map<String, Object> datos = (Map<String, Object>) r.get("datos");
            if (datos.size() < MINIMO_RESPUESTAS) {
                avisos.add(aviso(r, "Casi vacía", "Solo responde " + datos.size() + (datos.size() == 1 ? " pregunta" : " preguntas"), null));
            }
            String clave;
            if (encuesta.equals("empleadores")) {
                String nombre = String.valueOf(datos.getOrDefault("nombre_empresa", "")).trim();
                if (sinSentido(nombre)) avisos.add(aviso(r, "Nombre sin sentido", "Organización: «" + nombre + "»", null));
                clave = claveOrganizacion(nombre);
                if (clave.isEmpty()) continue;
            } else {
                // titulados: dos envíos con exactamente las mismas respuestas
                clave = Json.escribir(new java.util.TreeMap<>(datos));
            }
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(r);
        }
        int numero = 1;
        for (List<Map<String, Object>> g : grupos.values()) {
            if (g.size() < 2) continue;
            String etiqueta = "Grupo " + numero++;
            for (Map<String, Object> r : g) {
                Map<String, Object> datos = (Map<String, Object>) r.get("datos");
                String detalle = encuesta.equals("empleadores")
                    ? "«" + datos.get("nombre_empresa") + "» aparece " + g.size() + " veces (puede ser otra persona de la misma organización)"
                    : "Respuestas idénticas a otras " + (g.size() - 1);
                avisos.add(aviso(r, "Posible duplicado", detalle, etiqueta));
            }
        }
        return avisos;
    }

    private static Map<String, Object> aviso(Map<String, Object> r, String motivo, String detalle, String grupo) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", r.get("id"));
        a.put("fecha", r.get("fecha"));
        a.put("origen", r.get("origen"));
        a.put("motivo", motivo);
        a.put("detalle", detalle);
        a.put("grupo", grupo);
        return a;
    }

    /** Muy corto o una sola letra repetida ("G", "lll"). Las siglas como "MTM" no se marcan. */
    static boolean sinSentido(String texto) {
        String n = FormsImport.normalizar(texto).replace(" ", "");
        if (n.length() < 2) return true;
        return n.chars().distinct().count() == 1;
    }

    /** Nombre normalizado sin sufijos legales, para reconocer la misma organización escrita distinto. */
    static String claveOrganizacion(String nombre) {
        List<String> palabras = new ArrayList<>();
        for (String w : FormsImport.normalizar(nombre).split(" ")) if (!w.isBlank() && !SUFIJOS.contains(w)) palabras.add(w);
        return String.join(" ", palabras);
    }
}
