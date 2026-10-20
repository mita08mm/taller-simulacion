package com.actividad2;

import java.util.List;
import java.util.Map;

/** JSON minimo, sin dependencias externas: escribe y lee Map, List, String, Number, Boolean y null. */
public class Json {

    /** Lee un texto JSON y lo convierte en Map, List, String, Double, Boolean o null. */
    public static Object leer(String texto) {
        Lector l = new Lector(texto);
        l.espacios();
        Object v = l.valor();
        l.espacios();
        if (l.i < texto.length()) throw l.error("caracteres sobrantes");
        return v;
    }

    private static final class Lector {
        private final String s;
        private int i;

        Lector(String s) {
            this.s = s;
        }

        IllegalArgumentException error(String motivo) {
            return new IllegalArgumentException("JSON invalido (" + motivo + ") en la posicion " + i);
        }

        void espacios() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        Object valor() {
            if (i >= s.length()) throw error("fin inesperado");
            char c = s.charAt(i);
            if (c == '{') return objeto();
            if (c == '[') return arreglo();
            if (c == '"') return cadena();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return numero();
        }

        java.util.Map<String, Object> objeto() {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            i++;
            espacios();
            if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
            while (true) {
                espacios();
                if (i >= s.length() || s.charAt(i) != '"') throw error("se esperaba una clave");
                String clave = cadena();
                espacios();
                if (i >= s.length() || s.charAt(i) != ':') throw error("se esperaba ':'");
                i++;
                espacios();
                m.put(clave, valor());
                espacios();
                if (i >= s.length()) throw error("objeto sin cerrar");
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw error("se esperaba ',' o '}'");
            }
        }

        java.util.List<Object> arreglo() {
            java.util.List<Object> lista = new java.util.ArrayList<>();
            i++;
            espacios();
            if (i < s.length() && s.charAt(i) == ']') { i++; return lista; }
            while (true) {
                espacios();
                lista.add(valor());
                espacios();
                if (i >= s.length()) throw error("arreglo sin cerrar");
                char c = s.charAt(i++);
                if (c == ']') return lista;
                if (c != ',') throw error("se esperaba ',' o ']'");
            }
        }

        String cadena() {
            StringBuilder sb = new StringBuilder();
            i++;
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                if (i >= s.length()) break;
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw error("escape unicode incompleto");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> sb.append(e); // \" \\ \/
                }
            }
            throw error("cadena sin cerrar");
        }

        Double numero() {
            int inicio = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (inicio == i) throw error("valor no reconocido");
            try {
                return Double.parseDouble(s.substring(inicio, i));
            } catch (NumberFormatException e) {
                throw error("numero mal formado");
            }
        }
    }

    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribirValor(valor, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void escribirValor(Object valor, StringBuilder sb) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof Map<?, ?> mapa) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                if (!primero) sb.append(',');
                primero = false;
                escribirString(String.valueOf(entrada.getKey()), sb);
                sb.append(':');
                escribirValor(entrada.getValue(), sb);
            }
            sb.append('}');
        } else if (valor instanceof List<?> lista) {
            sb.append('[');
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0) sb.append(',');
                escribirValor(lista.get(i), sb);
            }
            sb.append(']');
        } else if (valor instanceof int[][] matriz) {
            sb.append('[');
            for (int i = 0; i < matriz.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('[');
                for (int j = 0; j < matriz[i].length; j++) {
                    if (j > 0) sb.append(',');
                    sb.append(matriz[i][j]);
                }
                sb.append(']');
            }
            sb.append(']');
        } else if (valor instanceof double[][] matriz) {
            sb.append('[');
            for (int i = 0; i < matriz.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('[');
                for (int j = 0; j < matriz[i].length; j++) {
                    if (j > 0) sb.append(',');
                    escribirValor(matriz[i][j], sb);
                }
                sb.append(']');
            }
            sb.append(']');
        } else if (valor instanceof String s) {
            escribirString(s, sb);
        } else if (valor instanceof Double dd && (dd.isNaN() || dd.isInfinite())) {
            sb.append("null");
        } else if (valor instanceof Number || valor instanceof Boolean) {
            sb.append(valor);
        } else {
            escribirString(valor.toString(), sb);
        }
    }

    private static void escribirString(String s, StringBuilder sb) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
