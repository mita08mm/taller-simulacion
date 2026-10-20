package com.actividad2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Genera los desarrollos paso a paso del informe (tex/generado/*.tex) a partir
 * de los mismos calculos del sistema, con el formato de los ejercicios de
 * referencias/apuntes_simulacion.tex (Paso 1 ... Paso 6).
 *
 * Uso (desde la base de datos, con db.properties):
 *   java -cp target/classes:src/main/resources:lib/postgresql-42.7.5.jar com.actividad2.ReporteTex <carpeta_salida> [- desde hasta]
 * Uso (desde archivos CSV):
 *   java -cp ... com.actividad2.ReporteTex <carpeta_salida> titulados.csv empleadores.csv [desde hasta]
 * Fechas en aaaa-mm-dd, o "-" para no limitar.
 */
public class ReporteTex {

    private static Dataset titulados;
    private static Dataset empleadores;
    private static boolean prueba;

    public static void main(String[] args) throws Exception {
        Path salida = Path.of(args.length > 0 ? args[0] : "../../tex/generado");
        Files.createDirectories(salida);
        boolean desdeCsv = args.length >= 3 && !args[1].equals("-");
        int iFechas;
        if (desdeCsv) {
            titulados = new Dataset(args[1], Csv.leer(Files.readString(Path.of(args[1]))));
            empleadores = new Dataset(args[2], Csv.leer(Files.readString(Path.of(args[2]))));
            iFechas = 3;
        } else {
            Db.iniciar();
            titulados = Respuestas.dataset("titulados");
            empleadores = Respuestas.dataset("empleadores");
            iFechas = 1;
        }
        // Las marcas [Datos de prueba] solo aparecen si la base todavía tiene respuestas de prueba
        prueba = titulados.origen.contains("prueba") || empleadores.origen.contains("prueba");
        if (args.length >= iFechas + 2) {
            java.time.LocalDate desde = args[iFechas].equals("-") ? null : java.time.LocalDate.parse(args[iFechas]);
            java.time.LocalDate hasta = args[iFechas + 1].equals("-") ? null : java.time.LocalDate.parse(args[iFechas + 1]);
            titulados = titulados.entreFechas(desde, hasta);
            empleadores = empleadores.entreFechas(desde, hasta);
        }
        if (titulados.n() == 0) throw new IllegalStateException("No hay respuestas de titulados para generar el informe");

        escribir(salida, "periodo.tex", periodo());
        escribir(salida, "demografia.tex", demografia());
        escribir(salida, "tasa_empleo.tex", tasaEmpleo());
        escribir(salida, "ic_tiempo_primer_empleo.tex", icTiempoPrimerEmpleo());
        escribir(salida, "ic_vida_profesional.tex", icNumerica("anios_vida_profesional", "años de vida profesional", "años"));
        escribir(salida, "ic_desempleo.tex", icNumerica("anios_desempleado", "años desempleado", "años"));
        escribir(salida, "destino_laboral.tex", destinoLaboral());
        escribir(salida, "formacion.tex", formacion());
        escribir(salida, "valoracion.tex", valoracion());
        for (Analisis.Cruce c : Analisis.CRUCES) {
            escribir(salida, "cruce_" + c.id() + ".tex", cruce(Analisis.cruce(titulados, c)));
        }
        escribir(salida, "resumen_cruces.tex", resumenCruces());
        escribir(salida, "criterios_arcusur.tex", criterios());
        escribir(salida, "empleadores.tex", empleadores());
        escribir(salida, "preguntas_abiertas.tex", preguntasAbiertas());
        System.out.println("Fragmentos generados en " + salida.toAbsolutePath());
        System.exit(0); // cierra las conexiones del pool
    }

    // ===================== utilidades =====================

    private static void escribir(Path dir, String nombre, String contenido) throws IOException {
        String cabecera = "% Generado por sistema/backend ReporteTex.java -- no editar a mano.\n"
            + "% Datos: " + titulados.origen + " (titulados) / " + empleadores.origen + " (empleadores)\n";
        Files.writeString(dir.resolve(nombre), cabecera + contenido.replaceAll("\n{3,}", "\n\n"), StandardCharsets.UTF_8);
    }

    private static String n(double v, int dec) {
        return String.format(Locale.US, "%." + dec + "f", v);
    }

    private static String miles(long v) {
        return String.format(Locale.US, "%,d", v).replace(",", "\\,");
    }

    /** Escapa texto para LaTeX. */
    private static String t(String s) {
        return s.replace("\\", "\\textbackslash{}").replace("&", "\\&").replace("%", "\\%").replace("$", "\\$")
            .replace("#", "\\#").replace("_", "\\_").replace("{", "\\{").replace("}", "\\}")
            .replace("×", "$\\times$").replace("<", "$<$").replace(">", "$>$");
    }

    private static String marca() {
        return prueba ? " \\datosprueba" : "";
    }

    private static String si(boolean condicion, String verdad, String falso) {
        return condicion ? verdad : falso;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lista(Object o) {
        return (List<Map<String, Object>>) o;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object o) {
        return (Map<String, Object>) o;
    }

    private static double d(Object o) {
        return ((Number) o).doubleValue();
    }

    private static int i(Object o) {
        return ((Number) o).intValue();
    }

    /** Tabla de frecuencias con el calculo de la relativa (f_i / n) a la vista. */
    private static String tablaFrecuencias(String titulo, List<Map<String, Object>> frec, String encabezado, int n, boolean omitirCeros) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{mitabla}{").append(titulo).append(marca()).append("}\n");
        sb.append("\\begin{tabular}{lccc}\n\\toprule\n");
        sb.append(encabezado).append(" & $f_i$ & $f_i / n$ & \\% \\\\\n\\midrule\n");
        int suma = 0;
        for (Map<String, Object> f : frec) {
            int fi = i(f.get("frecuencia"));
            if (omitirCeros && fi == 0) continue;
            suma += fi;
            sb.append(t((String) f.get("etiqueta"))).append(" & ").append(fi).append(" & $").append(fi).append("/").append(n)
              .append(" = ").append(n(n == 0 ? 0 : (double) fi / n, 3)).append("$ & ").append(n(n == 0 ? 0 : 100.0 * fi / n, 1)).append(" \\\\\n");
        }
        sb.append("\\midrule\nTotal & ").append(suma).append(" & & ").append(n(n == 0 ? 0 : 100.0 * suma / n, 1)).append(" \\\\\n");
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
        Map<String, Object> moda = moda(frec);
        if (moda != null && n > 0) {
            int fm = i(moda.get("frecuencia"));
            sb.append("\\textit{Lectura:} la respuesta más frecuente es ``").append(t((String) moda.get("etiqueta"))).append("'' ($f_i = ")
              .append(fm).append("$, ").append(n(100.0 * fm / n, 1)).append("\\% de ").append(n).append(").\n\n");
        }
        return sb.toString();
    }

    private static Map<String, Object> moda(List<Map<String, Object>> frec) {
        Map<String, Object> m = null;
        for (Map<String, Object> f : frec) if (m == null || i(f.get("frecuencia")) > i(m.get("frecuencia"))) m = f;
        return m;
    }

    private record Fila(String etiqueta, double x, int f) {}

    /**
     * Intervalo de confianza de la media, Paso 1..6, con la tabla de calculo
     * (x_i, f_i, f_i x_i, (x_i - xbar)^2, f_i (x_i - xbar)^2).
     */
    private static String icPasoAPaso(String variable, String unidad, List<Fila> filas, String notaPaso1) {
        int nTot = filas.stream().mapToInt(Fila::f).sum();
        double sumaFx = filas.stream().mapToDouble(r -> r.x() * r.f()).sum();
        double xbar = sumaFx / nTot;
        double sumaDesv = filas.stream().mapToDouble(r -> r.f() * (r.x() - xbar) * (r.x() - xbar)).sum();
        double s = Math.sqrt(sumaDesv / (nTot - 1));
        int gl = nTot - 1;
        double tv = Estadistica.tValor95(gl);
        double margen = s / Math.sqrt(nTot) * tv;

        StringBuilder sb = new StringBuilder();
        sb.append("\\textit{Paso 1. Contar con la muestra y determinar su tamaño.} ").append(notaPaso1)
          .append(" El tamaño de la muestra es $n = ").append(nTot).append("$.\n\n");

        sb.append("\\begin{mitabla}{Cálculo de la media y la desviación estándar de ").append(variable).append(marca()).append("}\n");
        sb.append("\\begin{tabular}{lccccc}\n\\toprule\n");
        sb.append("Respuesta & $x_i$ & $f_i$ & $f_i\\,x_i$ & $(x_i-\\bar{x})^2$ & $f_i\\,(x_i-\\bar{x})^2$ \\\\\n\\midrule\n");
        for (Fila r : filas) {
            if (r.f() == 0) continue;
            double dev = (r.x() - xbar) * (r.x() - xbar);
            sb.append(t(r.etiqueta())).append(" & ").append(fmtX(r.x())).append(" & ").append(r.f()).append(" & ")
              .append(n(r.x() * r.f(), 2)).append(" & ").append(n(dev, 4)).append(" & ").append(n(r.f() * dev, 4)).append(" \\\\\n");
        }
        sb.append("\\midrule\n$\\Sigma$ & & ").append(nTot).append(" & ").append(n(sumaFx, 2)).append(" & & ").append(n(sumaDesv, 4)).append(" \\\\\n");
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");

        sb.append("\\textit{Paso 2. Calcular la media muestral.}\n");
        sb.append("\\[\n\\bar{x} = \\frac{\\sum f_i\\,x_i}{n} = \\frac{").append(n(sumaFx, 2)).append("}{").append(nTot).append("} = ")
          .append(n(xbar, 4)).append("\\ \\text{").append(unidad).append("}\n\\]\n\n");

        sb.append("\\textit{Paso 3. Calcular la desviación estándar muestral} (Coss Bu, ecuación 6.5).\n");
        sb.append("\\[\nS = \\sqrt{\\frac{\\sum f_i\\,(x_i-\\bar{x})^2}{n-1}} = \\sqrt{\\frac{").append(n(sumaDesv, 4)).append("}{")
          .append(gl).append("}} = ").append(n(s, 4)).append("\n\\]\n\n");

        sb.append("\\textit{Paso 4. Determinar el estadístico teórico.} Con $\\alpha = 0.05$ y $n-1 = ").append(gl)
          .append("$ grados de libertad, ");
        if (gl <= 30) {
            sb.append("de la tabla de la distribución $t$ de Student: $t_{").append(gl).append(",\\,0.025} = ").append(n(tv, 3)).append("$.\n\n");
        } else {
            sb.append("como los grados de libertad son más de 30 la distribución $t$ se aproxima con la normal, igual que en la prueba de los promedios: $t_{")
              .append(gl).append(",\\,0.025} \\approx z_{0.025} = 1.96$.\n\n");
        }

        sb.append("\\textit{Paso 5. Calcular el margen de error.}\n");
        sb.append("\\[\n\\frac{S}{\\sqrt{n}}\\,t_{n-1,\\,\\alpha/2} = \\frac{").append(n(s, 4)).append("}{\\sqrt{").append(nTot).append("}}\\times ")
          .append(n(tv, 3)).append(" = ").append(n(s / Math.sqrt(nTot), 4)).append(" \\times ").append(n(tv, 3)).append(" = ").append(n(margen, 4)).append("\n\\]\n\n");

        sb.append("\\textit{Paso 6. Construir el intervalo y concluir.}\n");
        sb.append("\\[\n").append(n(xbar, 4)).append(" - ").append(n(margen, 4)).append(" \\;\\le\\; \\mu \\;\\le\\; ")
          .append(n(xbar, 4)).append(" + ").append(n(margen, 4)).append(" \\quad\\Longrightarrow\\quad ")
          .append(n(xbar - margen, 2)).append(" \\le \\mu \\le ").append(n(xbar + margen, 2)).append("\n\\]\n\n");
        sb.append("Con un 95\\% de confianza, la media de ").append(variable).append(" de todos los titulados está entre ")
          .append(n(xbar - margen, 2)).append(" y ").append(n(xbar + margen, 2)).append(" ").append(unidad).append(".").append(marca()).append("\n\n");
        return sb.toString();
    }

    private static String fmtX(double x) {
        return x == Math.rint(x) ? String.valueOf((long) x) : n(x, 1);
    }

    // ===================== Titulados =====================

    private static String fechaTexto(Object iso) {
        if (iso == null) return "---";
        String[] p = ((String) iso).split("-");
        String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
        return Integer.parseInt(p[2]) + " de " + meses[Integer.parseInt(p[1]) - 1] + " de " + p[0];
    }

    /** Periodo de recoleccion: fecha de la primera y la ultima respuesta de cada encuesta. */
    private static String periodo() {
        Map<String, Object> pt = titulados.periodo();
        Map<String, Object> pe = empleadores.periodo();
        StringBuilder sb = new StringBuilder();
        sb.append("Se analizan ").append(titulados.n()).append(" respuestas de titulados, recibidas entre el ").append(fechaTexto(pt.get("desde")))
          .append(" y el ").append(fechaTexto(pt.get("hasta"))).append(", y ").append(empleadores.n()).append(" respuestas de empleadores, recibidas entre el ")
          .append(fechaTexto(pe.get("desde"))).append(" y el ").append(fechaTexto(pe.get("hasta"))).append(". La fecha de cada respuesta proviene del registro automático del formulario.");
        int sinFecha = (Integer) pt.get("sinFecha") + (Integer) pe.get("sinFecha");
        if (sinFecha > 0) sb.append(" ").append(sinFecha).append(" respuestas no tienen fecha registrada.");
        return sb.append(marca()).append("\n\n").toString();
    }

    private static String demografia() {
        Map<String, Object> d = Analisis.demografia(titulados);
        int n = titulados.n();
        return tablaFrecuencias("Distribución por edad ($n=" + n + "$)", lista(d.get("edad")), "Edad", n, false)
            + tablaFrecuencias("Distribución por género ($n=" + n + "$)", lista(d.get("genero")), "Género", n, false);
    }

    private static String tasaEmpleo() {
        Map<String, Object> e = Analisis.empleabilidad(titulados);
        List<Map<String, Object>> sit = lista(e.get("situacionLaboral"));
        int n = titulados.valores("situacion_laboral").size();
        int trab = i(sit.get(0).get("frecuencia"));
        int empr = i(sit.get(1).get("frecuencia"));
        StringBuilder sb = new StringBuilder();
        sb.append("\\textit{Paso 1. Contar con la muestra y determinar su tamaño.} Respondieron la pregunta de situación laboral $n = ")
          .append(n).append("$ titulados.\n\n");
        sb.append("\\textit{Paso 2. Contar la frecuencia de cada respuesta.}\n\n");
        sb.append(tablaFrecuencias("Situación laboral actual ($n=" + n + "$)", sit, "Situación laboral", n, false));
        sb.append("\\textit{Paso 3. Calcular la tasa de empleo.} Se consideran empleados quienes trabajan en una organización y quienes tienen un emprendimiento propio:\n");
        sb.append("\\[\n\\text{Tasa de empleo} = \\frac{").append(trab).append(" + ").append(empr).append("}{").append(n).append("} = \\frac{")
          .append(trab + empr).append("}{").append(n).append("} = ").append(n((double) (trab + empr) / n, 4)).append(" = ")
          .append(n(100.0 * (trab + empr) / n, 1)).append("\\%\n\\]").append(marca()).append("\n\n");
        return sb.toString();
    }

    private static String icTiempoPrimerEmpleo() {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        Catalogo.MESES_PRIMER_TRABAJO.keySet().forEach(k -> conteo.put(k, 0));
        titulados.valores("tiempo_primer_trabajo").forEach(v -> conteo.merge(v, 1, Integer::sum));
        List<Fila> filas = new ArrayList<>();
        conteo.forEach((k, f) -> filas.add(new Fila(Catalogo.etiqueta("tiempo_primer_trabajo", k), Catalogo.MESES_PRIMER_TRABAJO.get(k), f)));
        return icPasoAPaso("tiempo hasta el primer empleo", "meses", filas,
            "Solo responden quienes trabajan en una organización. Cada rango se reemplaza por su punto medio en meses ($x_i$); ``más de 12 meses'' se toma como 12, por lo que la media es una cota inferior.");
    }

    private static String icNumerica(String columna, String variable, String unidad) {
        TreeMap<Double, Integer> conteo = new TreeMap<>();
        for (Map<String, String> f : titulados.filas) {
            String v = titulados.valor(f, "anios_vida_profesional");
            String u = titulados.valor(f, "anios_desempleado");
            if (v.isEmpty() || u.isEmpty() || Double.parseDouble(u) > Double.parseDouble(v)) continue;
            conteo.merge(Double.parseDouble(titulados.valor(f, columna)), 1, Integer::sum);
        }
        List<Fila> filas = new ArrayList<>();
        conteo.forEach((x, f) -> filas.add(new Fila(fmtX(x) + " " + unidad, x, f)));
        Map<String, Object> e = Analisis.empleabilidad(titulados);
        int descartadas = i(e.get("filasInconsistentesDescartadas"));
        return icPasoAPaso(variable, unidad, filas,
            "Los valores son numéricos y se agrupan por valor repetido ($x_i$ con su frecuencia $f_i$). Se descartan las filas en que los años desempleado superan los años de vida profesional (" + descartadas + " en esta muestra).");
    }

    private static String destinoLaboral() {
        Map<String, Object> dest = mapa(Analisis.empleabilidad(titulados).get("destinoLaboral"));
        int n = titulados.valores("sector_publico_privado", f -> "trabajando".equals(f.get("situacion_laboral"))).size();
        return tablaFrecuencias("Sector de la organización (trabajan en una organización, $n=" + n + "$)", lista(dest.get("sector_publico_privado")), "Sector", n, false)
            + tablaFrecuencias("Rubro de la organización ($n=" + n + "$)", lista(dest.get("rubro")), "Rubro", n, false)
            + tablaFrecuencias("Cargo que desempeña ($n=" + n + "$)", lista(dest.get("cargo")), "Cargo", n, false)
            + tablaFrecuencias("Remuneración mensual ($n=" + n + "$)", lista(dest.get("remuneracion_promedio")), "Remuneración", n, false);
    }

    private static String formacion() {
        Map<String, Object> f = Analisis.formacionContinua(titulados);
        Map<String, Object> fin = Analisis.financiamiento(titulados);
        Map<String, Object> fc = mapa(f.get("conFormacionComplementaria"));
        Map<String, Object> ip = mapa(f.get("interesPosgrado"));
        int nfc = i(fc.get("total"));
        int nInt = i(ip.get("casos"));
        int nCurso = i(fc.get("casos"));
        StringBuilder sb = new StringBuilder();
        sb.append("Proporción con formación complementaria: $").append(i(fc.get("casos"))).append("/").append(nfc).append(" = ")
          .append(n(d(fc.get("proporcion")), 3)).append("$ (").append(n(100 * d(fc.get("proporcion")), 1)).append("\\%). ");
        sb.append("Proporción con interés en un posgrado: $").append(nInt).append("/").append(i(ip.get("total"))).append(" = ")
          .append(n(d(ip.get("proporcion")), 3)).append("$ (").append(n(100 * d(ip.get("proporcion")), 1)).append("\\%).").append(marca()).append("\n\n");
        sb.append(tablaFrecuencias("Área de interés para un posgrado (interesados, $n=" + nInt + "$)", lista(f.get("areaInteres")), "Área", nInt, false));
        sb.append(tablaFrecuencias("Institución donde estudiaría el posgrado ($n=" + nInt + "$)", lista(f.get("organizacionInteres")), "Institución", nInt, false));
        sb.append(tablaFrecuencias("Nivel del programa cursado (con formación complementaria, $n=" + nCurso + "$)", lista(f.get("programaNivel")), "Nivel", nCurso, false));
        sb.append(tablaFrecuencias("Fuente de financiamiento del programa cursado ($n=" + nCurso + "$)", lista(fin.get("programaFinanciamiento")), "Financiamiento", nCurso, false));
        return sb.toString();
    }

    private static List<Fila> filasLikert(String columna, List<String> valores) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (String k : List.of("1", "2", "3", "4")) conteo.put(k, 0);
        valores.forEach(v -> conteo.merge(v, 1, Integer::sum));
        List<Fila> filas = new ArrayList<>();
        conteo.forEach((k, f) -> filas.add(new Fila(Catalogo.etiqueta(columna, k), Double.parseDouble(k), f)));
        return filas;
    }

    private static String valoracion() {
        StringBuilder sb = new StringBuilder();
        sb.append("\\paragraph{Grado de acuerdo con estar satisfecho con la formación.}\n\n");
        sb.append(icPasoAPaso("la satisfacción con la formación (escala 1--4)", "puntos",
            filasLikert("likert_satisfaccion_global_formacion", titulados.valores("likert_satisfaccion_global_formacion")),
            "Cada respuesta se codifica de 1 (totalmente en desacuerdo) a 4 (totalmente de acuerdo)."));
        sb.append("\\paragraph{Concordancia entre la formación y los requerimientos del mercado.}\n\n");
        sb.append(icPasoAPaso("la concordancia formación--mercado (escala 1--4)", "puntos",
            filasLikert("likert_concordancia_requerimientos", titulados.valores("likert_concordancia_requerimientos")),
            "Misma codificación de 1 a 4."));
        sb.append("\\paragraph{Pertinencia de las labores del puesto actual.}\n\n");
        sb.append(icPasoAPaso("la pertinencia del puesto (escala 1--4)", "puntos",
            filasLikert("likert_pertinencia_puesto", titulados.valores("likert_pertinencia_puesto", f -> "trabajando".equals(f.get("situacion_laboral")))),
            "Solo responden quienes trabajan en una organización; misma codificación de 1 a 4."));
        Map<String, Object> c = Analisis.competencias(titulados, empleadores);
        int n = titulados.n();
        sb.append("\\paragraph{Aspectos de la Carrera, asignaturas.} Son preguntas de elección múltiple: cada titulado puede marcar varias opciones, por lo que $f_i/n$ se calcula sobre el número de titulados ($n = ")
          .append(n).append("$) y los porcentajes pueden sumar más de 100.\n\n");
        sb.append(tablaFrecuencias("Aspectos de mayor utilidad ($n=" + n + "$)", lista(c.get("aspectosUtilidad")), "Aspecto", n, false).replace("\\midrule\nTotal", "\\midrule\nMenciones"));
        sb.append(tablaFrecuencias("Aspectos que pueden mejorarse ($n=" + n + "$)", lista(c.get("aspectosMejorar")), "Aspecto", n, false).replace("\\midrule\nTotal", "\\midrule\nMenciones"));
        sb.append(tablaFrecuencias("Asignaturas con ventaja competitiva ($n=" + n + "$)", lista(c.get("asignaturasVentaja")), "Asignatura", n, false).replace("\\midrule\nTotal", "\\midrule\nMenciones"));
        sb.append(tablaFrecuencias("Asignaturas poco útiles ($n=" + n + "$)", lista(c.get("asignaturasPocoUtiles")), "Asignatura", n, false).replace("\\midrule\nTotal", "\\midrule\nMenciones"));
        return sb.toString();
    }

    // ===================== Cruces (prueba chi-cuadrada) =====================

    @SuppressWarnings("unchecked")
    private static String cruce(Map<String, Object> c) {
        List<String> filas = (List<String>) c.get("filas");
        List<String> cols = (List<String>) c.get("columnas");
        int[][] fo = (int[][]) c.get("observada");
        double[][] fe = (double[][]) c.get("esperada");
        List<Integer> tf = (List<Integer>) c.get("totalFila");
        List<Integer> tc = (List<Integer>) c.get("totalColumna");
        int nTot = i(c.get("n"));
        int k = filas.size();
        int m = cols.size();
        String id = (String) c.get("id");

        StringBuilder sb = new StringBuilder();
        sb.append("\\subsubsection*{").append(id).append(". ").append(t((String) c.get("titulo"))).append("}\n\n");
        sb.append("\\textit{Propósito.} ").append(t((String) c.get("proposito"))).append(" Respalda el criterio ARCU-SUR ").append(t((String) c.get("criterio")))
          .append(". Se aplica sobre: ").append(t(((String) c.get("poblacion")).toLowerCase())).append(", con nivel de significancia $\\alpha = 0.05$.\n\n");

        sb.append("\\textit{Paso 1. Contar con la muestra: tabla de contingencia} (frecuencia observada $FO_{ij}$, $N = ").append(nTot).append("$).\n\n");
        String cabecera = t((String) c.get("variableFilas")) + " & " + String.join(" & ", cols.stream().map(ReporteTex::t).toList()) + " & Total fila \\\\\n";
        sb.append("\\begin{mitabla}{").append(id).append(": frecuencia observada $FO_{ij}$").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{l").append("c".repeat(m)).append("|c}\n\\toprule\n").append(cabecera).append("\\midrule\n");
        for (int a = 0; a < k; a++) {
            sb.append(t(filas.get(a)));
            for (int b = 0; b < m; b++) sb.append(" & ").append(fo[a][b]);
            sb.append(" & ").append(tf.get(a)).append(" \\\\\n");
        }
        sb.append("\\midrule\nTotal columna");
        for (int b = 0; b < m; b++) sb.append(" & ").append(tc.get(b));
        sb.append(" & $N=").append(nTot).append("$ \\\\\n\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");

        sb.append("\\textit{Paso 2. Calcular la frecuencia esperada} de cada celda, $FE_{ij} = (\\text{total fila } i)(\\text{total columna } j)/N$. Por ejemplo, para la celda ``")
          .append(t(filas.get(0))).append(" -- ").append(t(cols.get(0))).append("'': $FE_{11} = ").append(tf.get(0)).append(" \\times ").append(tc.get(0))
          .append(" / ").append(nTot).append(" = ").append(n(fe[0][0], 4)).append("$.\n\n");
        sb.append("\\begin{mitabla}{").append(id).append(": frecuencia esperada $FE_{ij}$").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{l").append("c".repeat(m)).append("}\n\\toprule\n")
          .append(t((String) c.get("variableFilas"))).append(" & ").append(String.join(" & ", cols.stream().map(ReporteTex::t).toList())).append(" \\\\\n\\midrule\n");
        for (int a = 0; a < k; a++) {
            sb.append(t(filas.get(a)));
            for (int b = 0; b < m; b++) sb.append(" & $").append(tf.get(a)).append("\\cdot").append(tc.get(b)).append("/").append(nTot).append(" = ").append(n(fe[a][b], 2)).append("$");
            sb.append(" \\\\\n");
        }
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");

        sb.append("\\textit{Paso 3. Calcular el estadístico chi-cuadrado,} sumando $(FO_{ij}-FE_{ij})^2/FE_{ij}$ de cada celda.\n\n");
        sb.append("\\begin{mitabla}{").append(id).append(": cálculo de $X_0^2$ celda por celda").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{L{5.4cm}cccc}\n\\toprule\nCelda & $FO_{ij}$ & $FE_{ij}$ & $(FO_{ij}-FE_{ij})^2$ & $(FO_{ij}-FE_{ij})^2/FE_{ij}$ \\\\\n\\midrule\n");
        double suma = 0;
        for (int a = 0; a < k; a++) {
            for (int b = 0; b < m; b++) {
                double dif2 = (fo[a][b] - fe[a][b]) * (fo[a][b] - fe[a][b]);
                double aporte = dif2 / fe[a][b];
                suma += aporte;
                sb.append(t(filas.get(a))).append(" -- ").append(t(cols.get(b))).append(" & ").append(fo[a][b]).append(" & ").append(n(fe[a][b], 4))
                  .append(" & ").append(n(dif2, 4)).append(" & ").append(n(aporte, 4)).append(" \\\\\n");
            }
        }
        sb.append("\\midrule\n\\multicolumn{4}{r}{$X_0^2 = \\sum$} & ").append(n(suma, 4)).append(" \\\\\n\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");

        int gl = i(c.get("gradosLibertad"));
        double teo = d(c.get("chiTeorico"));
        boolean indep = (Boolean) c.get("independientes");
        sb.append("\\textit{Paso 4. Determinar el estadístico teórico.} Con $k = ").append(k).append("$ filas y $m = ").append(m)
          .append("$ columnas, los grados de libertad son $(k-1)(m-1) = (").append(k).append("-1)(").append(m).append("-1) = ").append(gl)
          .append("$. De la tabla de chi-cuadrada: $X_{0.05,\\,").append(gl).append("}^2 = ").append(n(teo, 3)).append("$.\n\n");
        sb.append("\\textit{Paso 5. Establecer la relación entre estadísticos:} $X_0^2 ").append(si(indep, "<", "\\ge")).append(" X_{0.05,\\,").append(gl)
          .append("}^2 \\;\\Rightarrow\\; ").append(n(suma, 4)).append(si(indep, " < ", " \\ge ")).append(n(teo, 3)).append("$.\n\n");
        sb.append("\\textit{Paso 6. Conclusión.}\n\n\\begin{center}\n\\begin{tabular}{l}\n");
        sb.append("Si ($X_0^2 < X_{0.05,\\,").append(gl).append("}^2$)\\\\\n");
        sb.append("\\quad ").append(si(indep, "\\fbox{Independencia}", "Independencia")).append("\\\\\nSino\\\\\n");
        sb.append("\\quad ").append(si(indep, "Relación entre las variables", "\\fbox{Relación entre las variables}")).append("\\\\\nFinSi\n");
        sb.append("\\end{tabular}\n\\end{center}\n\n");
        if (indep) {
            sb.append("No se puede rechazar que ").append(t(((String) c.get("variableFilas")).toLowerCase())).append(" y ")
              .append(t(((String) c.get("variableColumnas")).toLowerCase())).append(" sean independientes.");
        } else {
            sb.append("Se rechaza la independencia: hay evidencia de que ").append(t(((String) c.get("variableFilas")).toLowerCase())).append(" y ")
              .append(t(((String) c.get("variableColumnas")).toLowerCase())).append(" están relacionadas.");
            sb.append(" Proporción por fila en la primera columna (``").append(t(cols.get(0))).append("''): ");
            List<String> partes = new ArrayList<>();
            for (int a = 0; a < k; a++) {
                partes.add(t(filas.get(a)) + " $" + fo[a][0] + "/" + tf.get(a) + " = " + n(100.0 * fo[a][0] / tf.get(a), 1) + "\\%$");
            }
            sb.append(String.join("; ", partes)).append(".");
        }
        sb.append(marca()).append("\n\n");
        int menor5 = i(c.get("celdasFeMenor5"));
        if (menor5 > 0) {
            sb.append("\\textit{Observación:} ").append(menor5).append(" de ").append(k * m)
              .append(" celdas tienen $FE < 5$, por lo que la prueba pierde confiabilidad y la conclusión debe tomarse con cautela.\n\n");
        }
        return sb.toString();
    }

    private static String resumenCruces() {
        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{mitabla}{Resumen de las pruebas de independencia ($\\alpha = 0.05$)").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{clcccccc}\n\\toprule\nId & Población & $N$ & $X_0^2$ & gl & $X_{0.05,\\,gl}^2$ & $FE<5$ & Conclusión \\\\\n\\midrule\n");
        for (Map<String, Object> c : Analisis.cruces(titulados)) {
            boolean indep = (Boolean) c.get("independientes");
            sb.append(c.get("id")).append(" & ").append(t((String) c.get("poblacion"))).append(" & ").append(c.get("n")).append(" & ")
              .append(n(d(c.get("chiCuadrada")), 3)).append(" & ").append(c.get("gradosLibertad")).append(" & ").append(n(d(c.get("chiTeorico")), 3))
              .append(" & ").append(c.get("celdasFeMenor5")).append("/").append(c.get("totalCeldas")).append(" & ")
              .append(indep ? "Independientes" : "\\textbf{Relacionadas}").append(" \\\\\n");
        }
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
        return sb.toString();
    }

    // ===================== Respuesta a los criterios ARCU-SUR =====================

    private static double acuerdo(Map<String, Object> likert) {
        return lista(likert.get("frecuencias")).stream().filter(f -> List.of("3", "4").contains(f.get("codigo")))
            .mapToDouble(f -> d(f.get("relativa"))).sum();
    }

    private static String pct(double v) {
        return n(100 * v, 1) + "\\%";
    }

    private static String valoracion(List<String> favorables, List<String> desfavorables) {
        StringBuilder sb = new StringBuilder("\\textbf{Valoración.} ");
        sb.append(favorables.isEmpty() ? "No se identifican aspectos favorables con la regla adoptada. " : "Aspectos favorables: " + String.join("; ", favorables) + ". ");
        sb.append(desfavorables.isEmpty() ? "No se identifican aspectos desfavorables." : "Aspectos desfavorables: " + String.join("; ", desfavorables) + ".");
        return sb.append(marca()).append("\n\n").toString();
    }

    private static void clasificar(double proporcion, String texto, List<String> fav, List<String> des) {
        (proporcion > 0.5 ? fav : des).add(texto + " (" + pct(proporcion) + ")");
    }

    @SuppressWarnings("unchecked")
    private static String criterios() {
        Map<String, Object> emp = Analisis.empleabilidad(titulados);
        Map<String, Object> dem = Analisis.demografia(titulados);
        Map<String, Object> form = Analisis.formacionContinua(titulados);
        Map<String, Object> comp = Analisis.competencias(titulados, empleadores);
        Map<String, Object> empl = Analisis.empleadores(empleadores);
        Map<String, Map<String, Object>> cruces = new LinkedHashMap<>();
        Analisis.cruces(titulados).forEach(c -> cruces.put((String) c.get("id"), c));
        java.util.function.Function<String, String> cr = id -> {
            Map<String, Object> c = cruces.get(id);
            return id + " (" + ((Boolean) c.get("independientes") ? "sin relación significativa" : "relación significativa") + ", $X_0^2 = "
                + n(d(c.get("chiCuadrada")), 3) + "$ frente a $" + n(d(c.get("chiTeorico")), 3) + "$)";
        };

        StringBuilder sb = new StringBuilder();

        // 3.2.3
        Map<String, Object> tasa = mapa(emp.get("tasaEmpleo"));
        Map<String, Object> tpe = mapa(mapa(emp.get("tiempoPrimerEmpleo")).get("mesesIC95"));
        Map<String, Object> pert = mapa(emp.get("pertinenciaPuesto"));
        Map<String, Object> conc = mapa(emp.get("concordanciaRequerimientos"));
        List<Map<String, Object>> sector = lista(mapa(emp.get("destinoLaboral")).get("sector_publico_privado"));
        Map<String, Object> rubro = moda(lista(mapa(emp.get("destinoLaboral")).get("rubro")));
        sb.append("\\paragraph{3.2.3 Condiciones de empleo.} \\textbf{Qué pide:} que el diseño de la carrera y la calidad de la formación se reflejen en las condiciones de empleo de los graduados; sus indicadores son el tiempo medio para obtener el primer empleo, el destino laboral y la tasa de empleo, y la concordancia entre titulación y empleo. ");
        sb.append("\\textbf{Evidencia del estudio:} tasa de empleo de ").append(pct(d(tasa.get("proporcion")))).append(" (").append(tasa.get("casos")).append(" de ").append(tasa.get("total"))
          .append(", Sección~\\ref{sec:titulados}); tiempo medio para obtener el primer empleo de ").append(n(d(tpe.get("media")), 2)).append(" meses, con IC 95\\% $[")
          .append(n(d(tpe.get("limiteInferior")), 2)).append(",\\ ").append(n(d(tpe.get("limiteSuperior")), 2)).append("]$; el ").append(pct(d(sector.get(1).get("relativa"))))
          .append(" de quienes trabajan en una organización lo hace en el sector privado y el rubro más frecuente es ``").append(t((String) rubro.get("etiqueta")))
          .append("''; cruces ").append(cr.apply("C2")).append(" y ").append(cr.apply("C6")).append(". El tiempo medio no tiene un valor de referencia en ARCU-SUR, por lo que queda como línea base para el seguimiento.\n\n");
        List<String> fav = new ArrayList<>(), des = new ArrayList<>();
        clasificar(d(tasa.get("proporcion")), "titulados empleados", fav, des);
        clasificar(acuerdo(pert), "titulados que consideran pertinentes a su formación las labores de su puesto", fav, des);
        clasificar(acuerdo(conc), "titulados que perciben concordancia entre su formación y los requerimientos del mercado", fav, des);
        sb.append(valoracion(fav, des));

        // 3.2.1
        Map<String, Object> sat = mapa(comp.get("satisfaccionGlobal"));
        Map<String, Object> mej = lista(comp.get("aspectosMejorar")).get(0);
        sb.append("\\paragraph{3.2.1 Resultados.} \\textbf{Qué pide:} evaluar el resultado del proceso formativo y utilizar esa evaluación para realizar ajustes. ");
        sb.append("\\textbf{Evidencia del estudio:} el ").append(pct(acuerdo(sat))).append(" de los titulados está de acuerdo con estar satisfecho con la formación recibida (media ")
          .append(n(d(mapa(sat.get("media")).get("media")), 2)).append(" en la escala de 1 a 4); el aspecto a mejorar más señalado es ``").append(t((String) mej.get("etiqueta")))
          .append("'' (").append(pct(d(mej.get("relativa")))).append("); cruce ").append(cr.apply("C1")).append(".\n\n");
        fav = new ArrayList<>(); des = new ArrayList<>();
        clasificar(acuerdo(sat), "titulados satisfechos con la formación", fav, des);
        sb.append(valoracion(fav, des));

        // 3.2.2
        Map<String, Object> red = mapa(dem.get("redContactos"));
        sb.append("\\paragraph{3.2.2 Vinculación y seguimiento a los graduados.} \\textbf{Qué pide:} un sistema de seguimiento de los graduados que permita conocer sus condiciones de empleo, e instancias de participación de los graduados para mejorar la carrera. ");
        sb.append("\\textbf{Evidencia del estudio:} la encuesta aplicada y la herramienta desarrollada constituyen un mecanismo de seguimiento: cada nueva aplicación se importa y los indicadores se recalculan. El ")
          .append(pct(d(red.get("proporcion")))).append(" de los titulados manifiesta interés en formar parte de una red oficial de contactos de la Carrera, lo que da una base para esa participación.\n\n");
        fav = new ArrayList<>(); des = new ArrayList<>();
        clasificar(d(red.get("proporcion")), "titulados interesados en la red de contactos", fav, des);
        sb.append(valoracion(fav, des));

        // 2.1.2
        List<Map<String, Object>> perfil = lista(empl.get("perfil"));
        sb.append("\\paragraph{2.1.2 Perfil de egreso.} \\textbf{Qué pide:} coherencia entre el perfil de egreso y la demanda de competencias profesionales expresada por agentes sociales relevantes. ");
        sb.append("\\textbf{Evidencia del estudio:} valoración de los empleadores en la escala de perfil profesional (Sección~\\ref{sec:empleadores}), donde se considera de acuerdo la suma de ``totalmente'' y ``parcialmente de acuerdo''.\n\n");
        fav = new ArrayList<>(); des = new ArrayList<>();
        for (Map<String, Object> p : perfil) {
            String cod = (String) p.get("codigo");
            if (List.of("perfil_coherente_desempeno", "perfil_competencias_laborales", "perfil_valores", "perfil_titulo_consistente", "perfil_conoce_perfil").contains(cod)) {
                clasificar(d(p.get("porcentajeAcuerdo")), "``" + t((String) p.get("criterio")) + "''", fav, des);
            }
        }
        sb.append(valoracion(fav, des));

        // 2.1.7
        Map<String, Object> vent = lista(comp.get("asignaturasVentaja")).get(0);
        Map<String, Object> poco = lista(comp.get("asignaturasPocoUtiles")).get(0);
        Map<String, Object> actualizar = lista(comp.get("aspectosMejorar")).stream().filter(f -> "actualizacion_plan_estudios".equals(f.get("codigo"))).findFirst().orElse(null);
        sb.append("\\paragraph{2.1.7 Actualización curricular.} \\textbf{Qué pide:} mecanismos permanentes de actualización del currículo, en consonancia con los desarrollos profesionales. ");
        sb.append("\\textbf{Evidencia del estudio:} la asignatura más señalada como ventaja competitiva es ``").append(t((String) vent.get("etiqueta"))).append("'' (")
          .append(pct(d(vent.get("relativa")))).append(") y la más señalada como poco útil es ``").append(t((String) poco.get("etiqueta"))).append("'' (").append(pct(d(poco.get("relativa"))))
          .append(")");
        if (actualizar != null) sb.append("; el ").append(pct(d(actualizar.get("relativa")))).append(" señala la actualización del plan de estudios como aspecto a mejorar");
        sb.append(". Las tecnologías y habilidades mencionadas por los empleadores (Sección~\\ref{sec:empleadores}) son un insumo directo para esa actualización.").append(marca()).append("\n\n");

        // 2.4.1 / 1.1.5
        Map<String, Object> fc = mapa(form.get("conFormacionComplementaria"));
        Map<String, Object> ip = mapa(form.get("interesPosgrado"));
        Map<String, Object> area = moda(lista(form.get("areaInteres")));
        int umss = lista(form.get("organizacionInteres")).stream().filter(f -> List.of("EUPG", "Direccion_Posgrado_FCyT").contains(f.get("codigo"))).mapToInt(f -> i(f.get("frecuencia"))).sum();
        int totalOrg = lista(form.get("organizacionInteres")).stream().mapToInt(f -> i(f.get("frecuencia"))).sum();
        Map<String, Object> oferta = mapa(form.get("ofertaPosgradoFcyt"));
        sb.append("\\paragraph{2.4.1 Cursos de actualización profesional y 1.1.5 Posgrado.} \\textbf{Qué piden:} extender los conocimientos hacia los graduados mediante cursos de actualización y programas de posgrado afines a la carrera, y promover estudios de posgrado. ");
        sb.append("\\textbf{Evidencia del estudio:} el ").append(pct(d(fc.get("proporcion")))).append(" tiene o cursa formación complementaria y el ").append(pct(d(ip.get("proporcion"))))
          .append(" está interesado en un posgrado del área; el área más solicitada es ``").append(t((String) area.get("etiqueta"))).append("''; de los interesados, el ")
          .append(pct(totalOrg == 0 ? 0 : (double) umss / totalOrg)).append(" elegiría la UMSS (EUPG o FCyT); cruces ").append(cr.apply("C3")).append(", ").append(cr.apply("C4"))
          .append(" y ").append(cr.apply("C5")).append(".\n\n");
        fav = new ArrayList<>(); des = new ArrayList<>();
        clasificar(d(ip.get("proporcion")), "titulados interesados en un posgrado", fav, des);
        clasificar(acuerdo(oferta), "titulados que consideran afín la oferta de posgrado de la FCyT", fav, des);
        clasificar(totalOrg == 0 ? 0 : (double) umss / totalOrg, "interesados que elegirían la UMSS para su posgrado", fav, des);
        sb.append(valoracion(fav, des));

        // 2.4.2
        double contrato = lista(empl.get("contrato_ultimos_5_anios")).stream().filter(f -> "SI".equals(f.get("codigo"))).mapToDouble(f -> d(f.get("relativa"))).findFirst().orElse(0);
        sb.append("\\paragraph{2.4.2 Relaciones con el sector público y privado.} \\textbf{Qué pide:} que la institución establezca relaciones con empresas y organizaciones públicas y privadas para cooperar en actividades conjuntas. ");
        sb.append("\\textbf{Evidencia del estudio:} encuesta a organizaciones de ambos sectores (Sección~\\ref{sec:empleadores}).\n\n");
        fav = new ArrayList<>(); des = new ArrayList<>();
        clasificar(contrato, "organizaciones que contrataron titulados en los últimos 5 años", fav, des);
        for (Map<String, Object> p : perfil) {
            if (Criterios.ITEMS_RELACION.contains(p.get("codigo"))) {
                clasificar(d(p.get("porcentajeAcuerdo")), "``" + t((String) p.get("criterio")) + "''", fav, des);
            }
        }
        sb.append(valoracion(fav, des));

        // 1.2.2
        sb.append("\\paragraph{1.2.2 Sistemas de información para la toma de decisiones.} \\textbf{Qué pide:} sistemas con información relevante, confiable y actualizada que respalden las decisiones institucionales. ");
        sb.append("\\textbf{Evidencia del estudio:} la herramienta desarrollada importa las respuestas, elimina los datos personales, calcula los indicadores y cruces de este informe y exporta los resultados, por lo que puede usarse de forma permanente para actualizar esta información.\n\n");
        return sb.toString();
    }

    // ===================== Empleadores =====================

    private static String empleadores() {
        Map<String, Object> e = Analisis.empleadores(empleadores);
        int n = empleadores.n();
        StringBuilder sb = new StringBuilder();
        sb.append(tablaFrecuencias("Tipo de organización ($n=" + n + "$)", lista(e.get("tipo_organizacion")), "Tipo", n, false));
        sb.append(tablaFrecuencias("Tamaño de la organización ($n=" + n + "$)", lista(e.get("tamano")), "Tamaño", n, false));
        sb.append(tablaFrecuencias("Contrató Ingenieros de Sistemas en los últimos 5 años ($n=" + n + "$)", lista(e.get("contrato_ultimos_5_anios")), "Respuesta", n, false));
        sb.append(tablaFrecuencias("Posibilidad de contratar en los próximos años ($n=" + n + "$)", lista(e.get("probabilidad_contratacion")), "Respuesta", n, false));
        sb.append(tablaFrecuencias("Nivel de formación que demandan ($n=" + n + "$)", lista(e.get("nivel_formacion_demandado")), "Nivel", n, false));
        sb.append(tablaFrecuencias("Cargos que desempeñan los titulados (elección múltiple, $n=" + n + "$)", lista(e.get("cargos")), "Cargo", n, false).replace("\\midrule\nTotal", "\\midrule\nMenciones"));

        sb.append("\\begin{mitabla}{Formación y perfil profesional: frecuencia $f_i$ y porcentaje $f_i/n$ por opción ($n=").append(n).append("$)").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{L{4.8cm}C{2.1cm}C{2.1cm}C{2.3cm}C{1.5cm}}\n\\toprule\nCriterio & Tot.\\ de acuerdo & Parc.\\ de acuerdo & Tot.\\ en desacuerdo & No sabe \\\\\n\\midrule\n");
        for (Map<String, Object> p : lista(e.get("perfil"))) {
            sb.append(t((String) p.get("criterio")));
            for (Map<String, Object> f : lista(p.get("frecuencias"))) {
                int fi = i(f.get("frecuencia"));
                sb.append(" & ").append(fi).append(" (").append(n(100.0 * fi / n, 0)).append("\\%)");
            }
            sb.append(" \\\\\n");
        }
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
        return sb.toString();
    }

    // ===================== Preguntas abiertas de empleadores =====================

    private static String preguntasAbiertas() {
        Map<String, String[]> preguntas = new LinkedHashMap<>();
        preguntas.put("areas_emergentes", new String[]{"Áreas de conocimiento emergentes", "Pregunta 10"});
        preguntas.put("herramientas", new String[]{"Herramientas tecnológicas", "Pregunta 11"});
        preguntas.put("habilidades", new String[]{"Habilidades y competencias", "Pregunta 12"});
        Map<String, List<String[]>> diccionario = Abiertas.diccionario();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> q : preguntas.entrySet()) {
            List<String> respuestas = empleadores.valores(q.getKey());
            int n = respuestas.size();
            List<Map<String, Object>> frec = Abiertas.codificar(q.getKey(), respuestas);
            sb.append("\\begin{mitabla}{").append(q.getValue()[0]).append(" mencionadas por los empleadores (").append(q.getValue()[1]).append(", $n=").append(n).append("$)").append(marca()).append("}\n");
            sb.append("\\begin{tabular}{L{9cm}cc}\n\\toprule\nCategoría & $f_i$ & $f_i/n$ \\\\\n\\midrule\n");
            for (Map<String, Object> f : frec) {
                int fi = i(f.get("frecuencia"));
                if (fi == 0) continue;
                if ("sin_categoria".equals(f.get("codigo"))) sb.append("\\midrule\n");
                sb.append(t((String) f.get("etiqueta"))).append(" & ").append(fi).append(" & $").append(fi).append("/").append(n).append(" = ")
                  .append(n(n == 0 ? 0 : (double) fi / n, 3)).append("$ \\\\\n");
            }
            sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
            Map<String, Object> primera = frec.get(0);
            sb.append("\\textit{Lectura:} la categoría más mencionada es ``").append(t((String) primera.get("etiqueta"))).append("'', presente en ")
              .append(i(primera.get("frecuencia"))).append(" de las ").append(n).append(" respuestas (").append(n(100.0 * i(primera.get("frecuencia")) / Math.max(n, 1), 1))
              .append("\\%). Como una respuesta puede mencionar varias categorías, la columna $f_i$ no suma $n$.\n\n");
        }
        // Regla de codificación: una tabla por pregunta (juntas no caben en una página)
        for (Map.Entry<String, String[]> q : preguntas.entrySet()) {
            sb.append("\\begin{mitabla}{Regla de codificación, ").append(q.getValue()[1].toLowerCase()).append(": palabras clave por categoría (sin acentos; $^{*}$ = cualquier terminación)}\n");
            sb.append("\\begin{tabular}{L{5.2cm}L{9.4cm}}\n\\toprule\nCategoría & Palabras clave \\\\\n\\midrule\n");
            for (String[] c : diccionario.get(q.getKey())) sb.append(t(c[0])).append(" & ").append(t(c[1]).replace("*", "$^{*}$")).append(" \\\\\n");
            sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
        }
        return sb.toString();
    }
}
