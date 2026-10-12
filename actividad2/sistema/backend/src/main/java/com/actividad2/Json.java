package com.actividad2;

import java.util.List;
import java.util.Map;

/** Serializador JSON minimo, sin dependencias externas (Map, List, String, Number, Boolean). */
public class Json {

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
                default -> sb.append(c);
            }
        }
        sb.append('"');
    }
}
