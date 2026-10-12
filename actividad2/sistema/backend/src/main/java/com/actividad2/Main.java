package com.actividad2;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST del sistema (sin dependencias externas). Los datos se cargan del
 * dataset de prueba al iniciar y se pueden reemplazar importando el CSV
 * exportado de cada formulario (POST /api/importar).
 */
public class Main {

    private static final int PUERTO = 7070;

    // Columnas minimas que debe traer cada CSV importado
    private static final List<String> REQUERIDAS_TITULADOS = List.of(
        "edad", "genero", "formacion_complementaria", "interes_posgrado", "situacion_laboral",
        "tiempo_primer_trabajo", "likert_satisfaccion_global_formacion", "likert_concordancia_requerimientos");
    private static final List<String> REQUERIDAS_EMPLEADORES = List.of(
        "nombre_empresa", "tipo_organizacion", "tamano", "rubro", "nivel_formacion_demandado");

    private static Dataset titulados;
    private static Dataset empleadores;

    public static void main(String[] args) throws Exception {
        cargarPrueba();
        HttpServer servidor = HttpServer.create(new InetSocketAddress(PUERTO), 0);

        // Todos los GET aceptan ?desde=aaaa-mm-dd&hasta=aaaa-mm-dd para filtrar por fecha de respuesta
        get(servidor, "/api/estado", (q, t, e) -> estado(t, e));
        get(servidor, "/api/demografia", (q, t, e) -> Analisis.demografia(t));
        get(servidor, "/api/empleabilidad", (q, t, e) -> Analisis.empleabilidad(t));
        get(servidor, "/api/formacion", (q, t, e) -> Analisis.formacionContinua(t));
        get(servidor, "/api/financiamiento", (q, t, e) -> Analisis.financiamiento(t));
        get(servidor, "/api/competencias", (q, t, e) -> Analisis.competencias(t, e));
        get(servidor, "/api/cruces", (q, t, e) -> Analisis.cruces(t));
        get(servidor, "/api/empleadores", (q, t, e) -> {
            Map<String, Object> r = Analisis.empleadores(e);
            r.put("cargosHomologados", Analisis.cargosHomologados(t, e));
            return r;
        });
        get(servidor, "/api/simulacion", (q, t, e) -> Simulacion.ejecutar(t, new Simulacion.Parametros(
            limitar(entero(q, "corridas", 1000), 1, 20000),
            limitar(entero(q, "cohorte", Math.max(t.n(), 1)), 1, 5000),
            entero(q, "semilla", 12345),
            limitar(entero(q, "ajusteEmpleo", 0), 0, 100) / 100.0,
            limitar(entero(q, "ajusteTiempo", 0), 0, 100) / 100.0)));

        servidor.createContext("/api/importar", Main::importar);
        servidor.createContext("/api/restablecer", ex -> {
            if (preflight(ex)) return;
            cargarPrueba();
            responder(ex, 200, "application/json", Json.escribir(estado(titulados, empleadores)));
        });
        servidor.createContext("/api/exportar", Main::exportar);

        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Backend corriendo en http://localhost:" + PUERTO + "/api/estado");
    }

    private static void cargarPrueba() throws IOException {
        titulados = new Dataset("Dataset de prueba", Csv.leer(recurso("dataset_titulados.csv")));
        empleadores = new Dataset("Dataset de prueba", Csv.leer(recurso("dataset_empleadores.csv")));
    }

    private static String recurso(String nombre) throws IOException {
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream(nombre)) {
            if (in == null) throw new IOException("No se encontro el archivo: " + nombre);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** n filtrado, n total, origen y periodo de fechas de cada encuesta. */
    private static Map<String, Object> estado(Dataset t, Dataset e) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("titulados", resumen(t, titulados));
        r.put("empleadores", resumen(e, empleadores));
        return r;
    }

    private static Map<String, Object> resumen(Dataset filtrado, Dataset completo) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("n", filtrado.n());
        r.put("nTotal", completo.n());
        r.put("origen", completo.origen);
        r.put("periodo", completo.periodo());
        return r;
    }

    private static LocalDate fechaParam(Map<String, String> q, String clave) {
        String v = q.getOrDefault(clave, "");
        try {
            return v.isBlank() ? null : LocalDate.parse(v);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Fecha invalida en '" + clave + "': use aaaa-mm-dd");
        }
    }

    // HU5: importar el CSV exportado de Microsoft Forms / Google Forms (cuerpo = texto CSV)
    private static void importar(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        Map<String, String> q = parametros(ex);
        String tipo = q.getOrDefault("tipo", "");
        String archivo = q.getOrDefault("archivo", "CSV importado");
        try {
            String texto = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (texto.startsWith("﻿")) texto = texto.substring(1); // BOM de Excel
            Dataset nuevo = new Dataset(archivo, Csv.leer(texto));
            List<String> requeridas = tipo.equals("titulados") ? REQUERIDAS_TITULADOS : REQUERIDAS_EMPLEADORES;
            if (!tipo.equals("titulados") && !tipo.equals("empleadores")) throw new IllegalArgumentException("tipo debe ser titulados o empleadores");
            if (nuevo.n() == 0) throw new IllegalArgumentException("El archivo no tiene filas");
            List<String> faltan = requeridas.stream().filter(c -> !nuevo.columnas.contains(c)).toList();
            if (!faltan.isEmpty()) throw new IllegalArgumentException("Faltan columnas: " + String.join(", ", faltan));
            if (tipo.equals("titulados")) titulados = nuevo; else empleadores = nuevo;
            responder(ex, 200, "application/json", Json.escribir(estado(titulados, empleadores)));
        } catch (RuntimeException e) {
            responder(ex, 400, "application/json", Json.escribir(Map.of("error", e.getMessage())));
        }
    }

    // HU5: exportar datos limpios (sin columnas personales) o una matriz de frecuencia cruzada
    private static void exportar(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        Map<String, String> q = parametros(ex);
        String que = q.getOrDefault("que", "titulados");
        Dataset t = titulados.entreFechas(fechaParam(q, "desde"), fechaParam(q, "hasta"));
        Dataset e = empleadores.entreFechas(fechaParam(q, "desde"), fechaParam(q, "hasta"));
        String csv;
        String nombre;
        if (que.equals("titulados") || que.equals("empleadores")) {
            Dataset d = que.equals("titulados") ? t : e;
            List<List<String>> filas = new ArrayList<>();
            for (Map<String, String> f : d.filas) filas.add(d.columnas.stream().map(c -> d.valor(f, c)).toList());
            csv = Csv.escribir(d.columnas, filas);
            nombre = que + "_limpio.csv";
        } else {
            Analisis.Cruce c = Analisis.CRUCES.stream().filter(x -> x.id().equals(que)).findFirst().orElse(null);
            if (c == null) {
                responder(ex, 404, "application/json", Json.escribir(Map.of("error", "No existe " + que)));
                return;
            }
            csv = cruceCsv(Analisis.cruce(t, c));
            nombre = "cruce_" + que + ".csv";
        }
        ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + nombre + "\"");
        responder(ex, 200, "text/csv", csv);
    }

    @SuppressWarnings("unchecked")
    private static String cruceCsv(Map<String, Object> cruce) {
        List<String> filas = (List<String>) cruce.get("filas");
        List<String> columnas = (List<String>) cruce.get("columnas");
        int[][] fo = (int[][]) cruce.get("observada");
        double[][] fe = (double[][]) cruce.get("esperada");
        List<String> encabezado = new ArrayList<>();
        encabezado.add(cruce.get("variableFilas") + " \\ " + cruce.get("variableColumnas"));
        for (String c : columnas) {
            encabezado.add(c + " (FO)");
            encabezado.add(c + " (FE)");
        }
        List<List<String>> datos = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            List<String> fila = new ArrayList<>();
            fila.add(filas.get(i));
            for (int j = 0; j < columnas.size(); j++) {
                fila.add(String.valueOf(fo[i][j]));
                fila.add(String.format(java.util.Locale.US, "%.2f", fe[i][j]));
            }
            datos.add(fila);
        }
        datos.add(List.of("X0^2 = " + cruce.get("chiCuadrada"), "gl = " + cruce.get("gradosLibertad"), "X^2 teorico (0.05) = " + cruce.get("chiTeorico")));
        return Csv.escribir(encabezado, datos);
    }

    // ---------------- utilidades HTTP ----------------

    interface Calculo {
        Object con(Map<String, String> parametros, Dataset titulados, Dataset empleadores) throws IOException;
    }

    private static void get(HttpServer servidor, String ruta, Calculo calculo) {
        servidor.createContext(ruta, ex -> {
            if (preflight(ex)) return;
            try {
                Map<String, String> q = parametros(ex);
                LocalDate desde = fechaParam(q, "desde");
                LocalDate hasta = fechaParam(q, "hasta");
                Object r = calculo.con(q, titulados.entreFechas(desde, hasta), empleadores.entreFechas(desde, hasta));
                responder(ex, 200, "application/json", Json.escribir(r));
            } catch (IllegalArgumentException e) {
                responder(ex, 400, "application/json", Json.escribir(Map.of("error", String.valueOf(e.getMessage()))));
            } catch (RuntimeException e) {
                e.printStackTrace();
                responder(ex, 500, "application/json", Json.escribir(Map.of("error", String.valueOf(e.getMessage()))));
            }
        });
    }

    private static boolean preflight(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return true;
        }
        return false;
    }

    private static void responder(HttpExchange ex, int codigo, String tipo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", tipo + "; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static Map<String, String> parametros(HttpExchange ex) {
        Map<String, String> q = new LinkedHashMap<>();
        String crudo = ex.getRequestURI().getRawQuery();
        if (crudo == null) return q;
        for (String par : crudo.split("&")) {
            String[] kv = par.split("=", 2);
            q.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return q;
    }

    private static int entero(Map<String, String> q, String clave, int porDefecto) {
        try {
            return Integer.parseInt(q.getOrDefault(clave, String.valueOf(porDefecto)));
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    private static int limitar(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
