package com.actividad2;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API REST del sistema. Las encuestas y respuestas viven en PostgreSQL; el
 * análisis lee las respuestas de la base (Respuestas.dataset).
 *
 * Grupos de rutas:
 *  - Formulario público: GET /api/encuestas/{encuesta}, POST /api/respuestas/{encuesta}
 *  - Panel (CRUD): /api/admin/encuestas, /api/admin/secciones, /api/admin/preguntas,
 *    /api/admin/opciones, /api/admin/respuestas
 *  - Análisis: /api/empleabilidad, /api/cruces, ... (aceptan ?desde=&hasta=)
 *  - Datos: /api/importar, /api/exportar
 */
public class Main {

    private static final int PUERTO = Integer.parseInt(System.getenv().getOrDefault("PUERTO", "7070"));
    private static final String ENCUESTA = "(titulados|empleadores)";

    // Columnas mínimas que debe traer cada CSV importado
    private static final List<String> REQUERIDAS_TITULADOS = List.of(
        "edad", "genero", "formacion_complementaria", "interes_posgrado", "situacion_laboral",
        "tiempo_primer_trabajo", "likert_satisfaccion_global_formacion", "likert_concordancia_requerimientos");
    private static final List<String> REQUERIDAS_EMPLEADORES = List.of(
        "nombre_empresa", "tipo_organizacion", "tamano", "rubro", "nivel_formacion_demandado");

    record Peticion(HttpExchange ex, Map<String, String> q, List<String> partes) {
        String parte(int i) { return partes.get(i); }
        int id(int i) { return Integer.parseInt(partes.get(i)); }

        @SuppressWarnings("unchecked")
        Map<String, Object> cuerpo() throws IOException {
            String texto = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (texto.isBlank()) return new LinkedHashMap<>();
            Object v = Json.leer(texto);
            if (!(v instanceof Map)) throw new IllegalArgumentException("Se esperaba un objeto JSON");
            return (Map<String, Object>) v;
        }

        Dataset titulados() { return Respuestas.dataset("titulados").entreFechas(fecha(q, "desde"), fecha(q, "hasta")); }
        Dataset empleadores() { return Respuestas.dataset("empleadores").entreFechas(fecha(q, "desde"), fecha(q, "hasta")); }
    }

    interface Manejador {
        Object atender(Peticion p) throws IOException;
    }

    record Ruta(String metodo, Pattern patron, Manejador manejador) {}

    private static final List<Ruta> RUTAS = new ArrayList<>();

    private static void ruta(String metodo, String patron, Manejador m) {
        RUTAS.add(new Ruta(metodo, Pattern.compile("^" + patron + "$"), m));
    }

    public static void main(String[] args) throws Exception {
        try {
            Db.iniciar();
        } catch (Exception e) {
            System.err.println("No se pudo conectar a PostgreSQL: " + e.getMessage());
            System.err.println("Revise backend/db.properties (url, user, password) o las variables DB_URL, DB_USER, DB_PASSWORD.");
            System.exit(1);
        }
        Semilla.cargarSiFalta();

        // ---------- Formulario público ----------
        ruta("GET", "/api/encuestas/" + ENCUESTA, p -> Encuestas.estructura(p.parte(0), false));
        ruta("POST", "/api/respuestas/" + ENCUESTA, p -> {
            long id = Respuestas.enviar(p.parte(0), p.cuerpo());
            return Map.of("id", id, "mensaje", "Respuesta registrada. ¡Gracias por participar!");
        });

        // ---------- Panel: CRUD de la encuesta ----------
        ruta("GET", "/api/admin/encuestas", p -> Encuestas.listar());
        ruta("GET", "/api/admin/encuestas/" + ENCUESTA, p -> Encuestas.estructura(p.parte(0), true));
        ruta("POST", "/api/admin/secciones", p -> Encuestas.crearSeccion(p.cuerpo()));
        ruta("PUT", "/api/admin/secciones/(\\d+)", p -> Encuestas.editarSeccion(p.id(0), p.cuerpo()));
        ruta("DELETE", "/api/admin/secciones/(\\d+)", p -> Encuestas.borrarSeccion(p.id(0)));
        ruta("POST", "/api/admin/preguntas", p -> Encuestas.crearPregunta(p.cuerpo()));
        ruta("PUT", "/api/admin/preguntas/(\\d+)", p -> Encuestas.editarPregunta(p.id(0), p.cuerpo()));
        ruta("DELETE", "/api/admin/preguntas/(\\d+)", p -> Encuestas.borrarPregunta(p.id(0)));
        ruta("POST", "/api/admin/opciones", p -> Encuestas.crearOpcion(p.cuerpo()));
        ruta("PUT", "/api/admin/opciones/(\\d+)", p -> Encuestas.editarOpcion(p.id(0), p.cuerpo()));
        ruta("DELETE", "/api/admin/opciones/(\\d+)", p -> Encuestas.borrarOpcion(p.id(0)));

        // ---------- Panel: CRUD de respuestas ----------
        ruta("GET", "/api/admin/respuestas", p -> Respuestas.listar(p.q().getOrDefault("encuesta", "titulados"),
            fecha(p.q(), "desde"), fecha(p.q(), "hasta"), vacioANull(p.q().get("origen")),
            entero(p.q(), "pagina", 1), limitar(entero(p.q(), "tamano", 20), 1, 200)));
        ruta("GET", "/api/admin/respuestas/revisar", p -> Revision.revisar(encuestaDe(p)));
        ruta("GET", "/api/admin/respuestas/(\\d+)", p -> Respuestas.obtener(p.id(0)));
        ruta("PUT", "/api/admin/respuestas/(\\d+)", p -> Respuestas.editar(p.id(0), p.cuerpo()));
        ruta("DELETE", "/api/admin/respuestas/(\\d+)", p -> Respuestas.eliminar(p.id(0)));
        ruta("DELETE", "/api/admin/respuestas", p -> Respuestas.eliminarPorOrigen(vacioANull(p.q().get("encuesta")),
            p.q().getOrDefault("origen", "")));
        ruta("GET", "/api/admin/respuestas-por-dia", p -> Respuestas.porDia(p.q().getOrDefault("encuesta", "titulados"),
            limitar(entero(p.q(), "dias", 30), 1, 365)));

        // ---------- Análisis (todos aceptan ?desde=aaaa-mm-dd&hasta=aaaa-mm-dd) ----------
        ruta("GET", "/api/estado", p -> estado(p));
        ruta("GET", "/api/criterios", p -> Criterios.evaluar(p.titulados(), p.empleadores()));
        ruta("GET", "/api/demografia", p -> Analisis.demografia(p.titulados()));
        ruta("GET", "/api/empleabilidad", p -> Analisis.empleabilidad(p.titulados()));
        ruta("GET", "/api/formacion", p -> Analisis.formacionContinua(p.titulados()));
        ruta("GET", "/api/financiamiento", p -> Analisis.financiamiento(p.titulados()));
        ruta("GET", "/api/competencias", p -> Analisis.competencias(p.titulados(), p.empleadores()));
        ruta("GET", "/api/cruces", p -> Analisis.cruces(p.titulados()));
        ruta("GET", "/api/variables", p -> variablesCruzables());
        ruta("GET", "/api/reporte/variables", p -> Reportes.variables(encuestaDe(p)));
        ruta("GET", "/api/reporte", p -> Reportes.generar(encuestaDe(p), datosDe(p), lista(p.q().get("variables"))));
        ruta("GET", "/api/cruce-libre", p -> Analisis.cruceLibre(p.titulados(), p.q().getOrDefault("filas", ""),
            p.q().getOrDefault("columnas", ""), !"0".equals(p.q().getOrDefault("agrupar", "1"))));
        ruta("GET", "/api/empleadores", p -> {
            Map<String, Object> r = Analisis.empleadores(p.empleadores());
            r.put("cargosHomologados", Analisis.cargosHomologados(p.titulados(), p.empleadores()));
            return r;
        });
        // ---------- Datos ----------
        ruta("POST", "/api/importar", Main::importar);
        ruta("GET", "/api/exportar", Main::exportar);

        HttpServer servidor = HttpServer.create(new InetSocketAddress(PUERTO), 0);
        servidor.createContext("/api/", Main::despachar);
        // Un hilo virtual por petición: miles de envíos simultáneos sin hacer fila
        servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        servidor.start();
        System.out.println("Backend corriendo en http://localhost:" + PUERTO + "/api/estado");
    }

    private static void despachar(HttpExchange ex) throws IOException {
        cors(ex);
        String metodo = ex.getRequestMethod().toUpperCase();
        if (metodo.equals("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }
        String ruta = ex.getRequestURI().getPath();
        if (ruta.length() > 1 && ruta.endsWith("/")) ruta = ruta.substring(0, ruta.length() - 1);
        try {
            boolean existeRuta = false;
            for (Ruta r : RUTAS) {
                Matcher m = r.patron().matcher(ruta);
                if (!m.matches()) continue;
                existeRuta = true;
                if (!r.metodo().equals(metodo)) continue;
                List<String> partes = new ArrayList<>();
                for (int i = 1; i <= m.groupCount(); i++) partes.add(m.group(i));
                Object res = r.manejador().atender(new Peticion(ex, parametros(ex), partes));
                if (res instanceof Archivo a) {
                    ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + a.nombre() + "\"");
                    responder(ex, 200, "text/csv", a.contenido());
                } else {
                    responder(ex, metodo.equals("POST") ? 201 : 200, "application/json", Json.escribir(res));
                }
                return;
            }
            responder(ex, existeRuta ? 405 : 404, "application/json",
                Json.escribir(Map.of("error", existeRuta ? "Método no permitido" : "Ruta no encontrada: " + ruta)));
        } catch (Respuestas.Invalida e) {
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("error", e.getMessage());
            cuerpo.put("errores", e.errores);
            responder(ex, 400, "application/json", Json.escribir(cuerpo));
        } catch (Encuestas.NoEncontrado e) {
            responder(ex, 404, "application/json", Json.escribir(Map.of("error", e.getMessage())));
        } catch (IllegalArgumentException e) {
            responder(ex, 400, "application/json", Json.escribir(Map.of("error", String.valueOf(e.getMessage()))));
        } catch (IllegalStateException e) {
            responder(ex, 503, "application/json", Json.escribir(Map.of("error", String.valueOf(e.getMessage()))));
        } catch (RuntimeException e) {
            e.printStackTrace();
            responder(ex, 500, "application/json", Json.escribir(Map.of("error", String.valueOf(e.getMessage()))));
        }
    }

    record Archivo(String nombre, String contenido) {}

    /** n filtrado, n total, origen y período de fechas de cada encuesta. */
    private static Map<String, Object> estado(Peticion p) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("titulados", resumen(p.titulados(), Respuestas.dataset("titulados")));
        r.put("empleadores", resumen(p.empleadores(), Respuestas.dataset("empleadores")));
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

    private static String encuestaDe(Peticion p) {
        String e = p.q().getOrDefault("encuesta", "titulados");
        if (!e.equals("titulados") && !e.equals("empleadores")) throw new IllegalArgumentException("encuesta debe ser titulados o empleadores");
        return e;
    }

    private static Dataset datosDe(Peticion p) {
        return encuestaDe(p).equals("titulados") ? p.titulados() : p.empleadores();
    }

    private static List<String> lista(String valor) {
        if (valor == null || valor.isBlank()) return List.of();
        return java.util.Arrays.stream(valor.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    private static List<Map<String, Object>> variablesCruzables() {
        List<Map<String, Object>> lista = new ArrayList<>();
        Catalogo.PREGUNTAS_CRUZABLES.forEach((col, pregunta) -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("columna", col);
            v.put("pregunta", pregunta);
            v.put("agrupable", Catalogo.agrupacion(col) != null);
            lista.add(v);
        });
        return lista;
    }

    // HU5: importar el CSV exportado de Microsoft Forms / Google Forms (cuerpo = texto CSV)
    private static Object importar(Peticion p) throws IOException {
        String tipo = p.q().getOrDefault("tipo", "");
        if (!tipo.equals("titulados") && !tipo.equals("empleadores")) throw new IllegalArgumentException("tipo debe ser titulados o empleadores");
        String texto = new String(p.ex().getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        // CSV tal como sale de Microsoft/Google Forms: se traduce texto -> código y reemplaza la importación anterior
        if (FormsImport.esFormatoForms(texto)) {
            FormsImport.Resultado res = FormsImport.leer(tipo, texto, Encuestas.estructura(tipo, true));
            int n = Respuestas.guardarImportadas(tipo, res.filas(), true);
            Map<String, Object> r = estado(p);
            r.put("importadas", n);
            r.put("informe", res.informe());
            return r;
        }
        List<Map<String, String>> filas = Csv.leer(texto);
        if (filas.isEmpty()) throw new IllegalArgumentException("El archivo no tiene filas");
        List<String> requeridas = tipo.equals("titulados") ? REQUERIDAS_TITULADOS : REQUERIDAS_EMPLEADORES;
        List<String> faltan = requeridas.stream().filter(c -> !filas.get(0).containsKey(c)).toList();
        if (!faltan.isEmpty()) throw new IllegalArgumentException("Faltan columnas: " + String.join(", ", faltan));
        int n = Respuestas.importar(tipo, filas, "importado");
        Map<String, Object> r = estado(p);
        r.put("importadas", n);
        return r;
    }

    // HU5: exportar datos limpios (sin columnas personales) o una matriz de frecuencia cruzada
    private static Object exportar(Peticion p) {
        Map<String, String> q = p.q();
        String que = q.getOrDefault("que", "titulados");
        Dataset t = p.titulados();
        // formato=excel: separador ';', decimales con coma y BOM UTF-8 (Excel en español)
        boolean excel = "excel".equals(q.get("formato"));
        char sep = excel ? ';' : ',';
        java.util.Locale loc = excel ? java.util.Locale.forLanguageTag("es-BO") : java.util.Locale.US;
        String csv;
        String nombre;
        if (que.equals("reporte") || que.equals("reporte-datos")) {
            // reporte a medida: tablas de frecuencia o respuestas individuales de las variables elegidas
            boolean tablas = que.equals("reporte");
            String enc = encuestaDe(p);
            List<String> vars = lista(q.get("variables"));
            csv = tablas ? Reportes.csv(Reportes.generar(enc, datosDe(p), vars), sep, loc) : Reportes.datosCsv(enc, datosDe(p), vars, sep);
            nombre = "reporte_" + enc + (tablas ? "_tablas" : "_datos") + ".csv";
        } else if (que.equals("libre")) {
            csv = cruceCsv(Analisis.cruceLibre(t, q.getOrDefault("filas", ""), q.getOrDefault("columnas", ""),
                !"0".equals(q.getOrDefault("agrupar", "1"))), sep, loc);
            nombre = "cruce_" + q.getOrDefault("filas", "") + "_x_" + q.getOrDefault("columnas", "") + ".csv";
        } else if (que.equals("titulados") || que.equals("empleadores")) {
            Dataset d = que.equals("titulados") ? t : p.empleadores();
            List<List<String>> filas = new ArrayList<>();
            for (Map<String, String> f : d.filas) filas.add(d.columnas.stream().map(c -> d.valor(f, c)).toList());
            csv = Csv.escribir(d.columnas, filas, sep);
            nombre = que + "_limpio.csv";
        } else {
            Analisis.Cruce c = Analisis.CRUCES.stream().filter(x -> x.id().equals(que)).findFirst()
                .orElseThrow(() -> new Encuestas.NoEncontrado("No existe " + que));
            csv = cruceCsv(Analisis.cruce(t, c), sep, loc);
            nombre = "cruce_" + que + ".csv";
        }
        return new Archivo(nombre, excel ? "﻿" + csv : csv);
    }

    @SuppressWarnings("unchecked")
    private static String cruceCsv(Map<String, Object> cruce, char sep, java.util.Locale loc) {
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
        for (int i = 0; i < fo.length; i++) {
            List<String> fila = new ArrayList<>();
            fila.add(filas.get(i));
            for (int j = 0; j < fo[i].length; j++) {
                fila.add(String.valueOf(fo[i][j]));
                fila.add(String.format(loc, "%.2f", fe[i][j]));
            }
            datos.add(fila);
        }
        Object x0 = cruce.get("chiCuadrada");
        Object teo = cruce.get("chiTeorico");
        datos.add(List.of("X0^2 = " + (x0 == null ? "-" : String.format(loc, "%.4f", (Double) x0)), "gl = " + cruce.get("gradosLibertad"),
            "X^2 teorico (0.05) = " + (teo == null ? "-" : String.format(loc, "%.3f", (Double) teo))));
        return Csv.escribir(encabezado, datos, sep);
    }

    // ---------------- utilidades HTTP ----------------

    private static void cors(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
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
            if (par.isEmpty()) continue;
            String[] kv = par.split("=", 2);
            q.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return q;
    }

    static LocalDate fecha(Map<String, String> q, String clave) {
        String v = q.getOrDefault(clave, "");
        try {
            return v.isBlank() ? null : LocalDate.parse(v);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Fecha inválida en '" + clave + "': use aaaa-mm-dd");
        }
    }

    private static String vacioANull(String v) {
        return v == null || v.isBlank() ? null : v;
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
