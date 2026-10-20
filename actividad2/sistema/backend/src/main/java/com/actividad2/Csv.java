package com.actividad2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lector/escritor CSV minimo (soporta campos entre comillas con comas adentro). */
public class Csv {

    public static List<Map<String, String>> leer(String texto) {
        String limpio = texto.replace("\r\n", "\n").replace('\r', '\n');
        if (limpio.startsWith("\uFEFF")) limpio = limpio.substring(1);
        // Excel en español guarda los CSV con ';': se detecta por el encabezado
        String primeraLinea = limpio.contains("\n") ? limpio.substring(0, limpio.indexOf('\n')) : limpio;
        char sep = primeraLinea.chars().filter(ch -> ch == ';').count() > primeraLinea.chars().filter(ch -> ch == ',').count() ? ';' : ',';
        List<List<String>> registros = registros(limpio, sep);
        List<Map<String, String>> filas = new ArrayList<>();
        if (registros.isEmpty()) return filas;

        List<String> encabezado = registros.get(0);
        for (int r = 1; r < registros.size(); r++) {
            List<String> valores = registros.get(r);
            boolean vacia = valores.stream().allMatch(String::isBlank);
            if (vacia) continue;
            Map<String, String> fila = new LinkedHashMap<>();
            for (int i = 0; i < encabezado.size(); i++) {
                fila.put(encabezado.get(i).trim(), i < valores.size() ? valores.get(i).trim() : "");
            }
            filas.add(fila);
        }
        return filas;
    }

    private static List<List<String>> registros(String texto, char sep) {
        List<List<String>> registros = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean enComillas = false;

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (enComillas) {
                if (c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                    campo.append('"');
                    i++;
                } else if (c == '"') {
                    enComillas = false;
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                enComillas = true;
            } else if (c == sep) {
                actual.add(campo.toString());
                campo.setLength(0);
            } else if (c == '\n') {
                actual.add(campo.toString());
                campo.setLength(0);
                registros.add(actual);
                actual = new ArrayList<>();
            } else {
                campo.append(c);
            }
        }
        if (campo.length() > 0 || !actual.isEmpty()) {
            actual.add(campo.toString());
            registros.add(actual);
        }
        return registros;
    }

    public static String escribir(List<String> columnas, List<List<String>> filas) {
        return escribir(columnas, filas, ',');
    }

    /** Con separador ';' el archivo se abre en columnas en Excel configurado en español. */
    public static String escribir(List<String> columnas, List<List<String>> filas, char separador) {
        StringBuilder sb = new StringBuilder();
        sb.append(linea(columnas, separador));
        for (List<String> fila : filas) sb.append(linea(fila, separador));
        return sb.toString();
    }

    private static String linea(List<String> valores, char separador) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) sb.append(separador);
            String v = valores.get(i) == null ? "" : valores.get(i);
            if (v.indexOf(separador) >= 0 || v.contains("\"") || v.contains("\n")) {
                sb.append('"').append(v.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(v);
            }
        }
        return sb.append('\n').toString();
    }
}
