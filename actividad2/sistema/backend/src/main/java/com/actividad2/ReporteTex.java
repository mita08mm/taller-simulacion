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
        escribirTodo(salida);
        System.out.println("Fragmentos generados en " + salida.toAbsolutePath());
        System.exit(0); // cierra las conexiones del pool
    }

    /**
     * Desde el panel: regenera los fragmentos con las respuestas actuales de la base y compila el informe con
     * latexmk. Devuelve el resultado de la compilación. Se sincroniza porque usa los datasets estáticos.
     */
    public static synchronized Map<String, Object> actualizarInforme(Path carpetaTex) throws IOException, InterruptedException {
        titulados = Respuestas.dataset("titulados");
        empleadores = Respuestas.dataset("empleadores");
        prueba = titulados.origen.contains("prueba") || empleadores.origen.contains("prueba");
        escribirTodo(carpetaTex.resolve("generado"));
        Process proc = new ProcessBuilder("latexmk", "-pdf", "-interaction=nonstopmode", "Informe.tex")
            .directory(carpetaTex.toFile()).redirectErrorStream(true).start();
        String salida = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean ok = proc.waitFor() == 0 && Files.exists(carpetaTex.resolve("pdf/Informe.pdf"));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", ok);
        r.put("titulados", titulados.n());
        r.put("empleadores", empleadores.n());
        r.put("mensaje", ok ? "Informe actualizado con " + titulados.n() + " titulados y " + empleadores.n() + " empleadores."
            : "Los fragmentos se generaron, pero la compilación falló: " + salida.substring(Math.max(0, salida.length() - 600)));
        return r;
    }

    private static void escribirTodo(Path salida) throws IOException {
        Files.createDirectories(salida);
        if (titulados.n() == 0) throw new IllegalStateException("No hay respuestas de titulados para generar el informe");
        escribir(salida, "periodo.tex", periodo());
        escribir(salida, "demografia.tex", demografia());
        escribir(salida, "tasa_empleo.tex", tasaEmpleo());
        escribir(salida, "ic_tiempo_primer_empleo.tex", icTiempoPrimerEmpleo());
        escribir(salida, "ic_vida_profesional.tex", icNumerica("anios_vida_profesional", "años de vida profesional", "años"));
        escribir(salida, "ic_desempleo.tex", icNumerica("anios_desempleado", "años desempleado", "años"));
        escribir(salida, "experiencia.tex", experiencia());
        escribir(salida, "destino_laboral.tex", destinoLaboral());
        escribir(salida, "formacion.tex", formacion());
        escribir(salida, "valoracion.tex", valoracion());
        for (Analisis.Cruce c : Analisis.CRUCES) {
            escribir(salida, "cruce_" + c.id() + ".tex", cruce(Analisis.cruce(titulados, c)));
        }
        escribir(salida, "resumen_cruces.tex", resumenCruces());
        for (Analisis.Uniformidad u : Analisis.UNIFORMIDAD) {
            Dataset d = u.encuesta().equals("titulados") ? titulados : empleadores;
            escribir(salida, "uniformidad_" + u.id() + ".tex", uniformidad(Analisis.uniformidad(d, u)));
        }
        escribir(salida, "resumen_uniformidad.tex", resumenUniformidad());
        escribir(salida, "criterios_arcusur.tex", criterios());
        escribir(salida, "empleadores.tex", empleadores());
        escribir(salida, "preguntas_abiertas.tex", preguntasAbiertas());
        escribir(salida, "organizaciones.tex", organizaciones());
        // Textos del informe que citan resultados: se regeneran con los datos, nada se escribe a mano
        escribir(salida, "valores.tex", valores());
        escribir(salida, "resumen_resultados.tex", resumenResultados());
        escribir(salida, "abiertas_interpretacion.tex", abiertasInterpretacion());
        escribir(salida, "conclusiones.tex", conclusiones());
        escribir(salida, "propuestas.tex", propuestas());
        escribir(salida, "multivariable.tex", multivariable());
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

    /** HU1: mediana de los años de vida profesional (paso a paso) y segmentación junior / consolidados. */
    @SuppressWarnings("unchecked")
    private static String experiencia() {
        Map<String, Object> e = Analisis.empleabilidad(titulados);
        Object med = e.get("medianaVidaProfesional");
        if (med == null) return "No hay respuestas para calcular la mediana.\n\n";
        double m = d(med);
        int n = i(mapa(e.get("aniosVidaProfesional")).get("n"));
        StringBuilder sb = new StringBuilder();
        sb.append("\\textit{Paso 1. Ordenar los datos.} Se ordenan de menor a mayor los ").append(n)
          .append(" valores de años de vida profesional usados en el intervalo anterior.\n\n");
        sb.append("\\textit{Paso 2. Ubicar el centro.} ");
        if (n % 2 == 1) {
            sb.append("Como $n = ").append(n).append("$ es impar, la mediana es el valor en la posición $\\frac{n+1}{2} = ").append((n + 1) / 2).append("$.\n\n");
        } else {
            sb.append("Como $n = ").append(n).append("$ es par, la mediana es el promedio de los valores en las posiciones $\\frac{n}{2} = ").append(n / 2)
              .append("$ y $\\frac{n}{2}+1 = ").append(n / 2 + 1).append("$.\n\n");
        }
        double media = d(mapa(e.get("aniosVidaProfesional")).get("media"));
        sb.append("\\textit{Paso 3. Resultado.} Mediana $= ").append(fmtX(m)).append("$ años, frente a una media de $").append(n(media, 2))
          .append("$ años. ").append(media > m ? "La media es mayor que la mediana porque unos pocos titulados con muchos años de experiencia la elevan; la mediana describe mejor al titulado típico." : "Media y mediana son cercanas, por lo que los datos no están muy sesgados.").append(marca()).append("\n\n");
        sb.append("El enunciado pide distinguir a los profesionales junior de los mandos consolidados. Se usa la mediana como corte: la mitad con menos experiencia frente a la mitad con más. Para cada grupo se indica qué proporción de quienes declararon un cargo ocupa un cargo ejecutivo.\n\n");
        sb.append("\\begin{mitabla}{Titulados por experiencia profesional (corte en la mediana, $n=").append(n).append("$)").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{llccc}\n\\toprule\nGrupo & Años de vida profesional & $f_i$ & $f_i/n$ & Cargo ejecutivo \\\\\n\\midrule\n");
        for (Map<String, Object> g : (List<Map<String, Object>>) e.get("segmentosExperiencia")) {
            int c = i(g.get("casos")), cc = i(g.get("conCargo")), ej = i(g.get("ejecutivos"));
            sb.append(g.get("segmento")).append(" & ").append(t((String) g.get("criterio"))).append(" & ").append(c).append(" & $").append(c).append("/").append(n).append(" = ").append(n(n == 0 ? 0 : (double) c / n, 3))
              .append("$ & ").append(cc == 0 ? "---" : ej + " de " + cc + " (" + n(100.0 * ej / cc, 1) + "\\%)").append(" \\\\\n");
        }
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
        return sb.toString();
    }

    /** HU2: qué tan concentradas están las preferencias en las dos áreas más elegidas (el enunciado usa el 70\% como referencia). */
    private static String concentracion(List<Map<String, Object>> areas, int n) {
        List<Map<String, Object>> orden = new ArrayList<>(areas);
        orden.sort((a, b) -> Integer.compare(i(b.get("frecuencia")), i(a.get("frecuencia"))));
        if (orden.size() < 2 || n == 0) return "";
        int f1 = i(orden.get(0).get("frecuencia")), f2 = i(orden.get(1).get("frecuencia"));
        double p = (double) (f1 + f2) / n;
        return "\\textit{Concentración de las preferencias.} Las dos áreas más elegidas, ``" + t((String) orden.get(0).get("etiqueta")) + "'' y ``"
            + t((String) orden.get(1).get("etiqueta")) + "'', reúnen $\\frac{" + f1 + " + " + f2 + "}{" + n + "} = " + n(p, 3) + "$ (" + n(100 * p, 1)
            + "\\%) de las preferencias. El enunciado toma como referencia una concentración mayor al 70\\% para justificar la apertura de programas modulares; "
            + (p > 0.7 ? "esta muestra la supera." : "esta muestra queda " + (p > 0.6 ? "cerca de ese valor, sin alcanzarlo" : "por debajo") + ", por lo que la demanda está más repartida entre áreas.")
            + marca() + "\n\n";
    }

    /** Organizaciones encuestadas, como pide el enunciado (individualizar las empresas), sin atribuirles resultados. */
    @SuppressWarnings("unchecked")
    private static String organizaciones() {
        List<Map<String, Object>> empresas = new ArrayList<>((List<Map<String, Object>>) Analisis.empleadores(empleadores).get("empresas"));
        empresas.sort((a, b) -> ((String) a.get("nombre")).compareToIgnoreCase((String) b.get("nombre")));
        long publicas = empresas.stream().filter(e -> "Público".equals(e.get("tipo"))).count();
        long privadas = empresas.stream().filter(e -> "Privado".equals(e.get("tipo"))).count();
        long software = empresas.stream().filter(e -> String.valueOf(e.get("rubro")).toLowerCase().contains("software")).count();
        StringBuilder sb = new StringBuilder();
        sb.append("Se encuestaron ").append(empresas.size()).append(" organizaciones: ").append(publicas).append(" del ámbito público y ").append(privadas)
          .append(" del privado; ").append(software).append(" declaran como rubro principal el desarrollo de software. Con ello se cumple el mínimo del enunciado (al menos tres empresas de desarrollo de software, una pública y una privada). La lista respalda la toma de datos en campo; ningún resultado de este informe se atribuye a una organización con nombre.\n\n");
        sb.append("{\\small\n\\begin{longtable}{rL{6.2cm}lL{4.6cm}}\n\\caption{Organizaciones encuestadas}\\\\\n\\toprule\nN.º & Organización & Tipo & Rubro \\\\\n\\midrule\n\\endfirsthead\n");
        sb.append("\\toprule\nN.º & Organización & Tipo & Rubro \\\\\n\\midrule\n\\endhead\n\\bottomrule\n\\endlastfoot\n");
        int k = 1;
        for (Map<String, Object> e : empresas) {
            sb.append(k++).append(" & ").append(t(((String) e.get("nombre")).trim())).append(" & ").append(t(String.valueOf(e.get("tipo")))).append(" & ")
              .append(t(String.valueOf(e.get("rubro")))).append(" \\\\\n");
        }
        sb.append("\\end{longtable}\n}\n\n");
        return sb.toString();
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
        sb.append(concentracion(lista(f.get("areaInteres")), nInt));
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

    /**
     * Un cruce del estudio con el mismo algoritmo de la Formulación (Sección 2): Paso 1 describir el problema
     * e hipótesis, datos, Paso 2 tabla de contingencia, Paso 3 FE, Paso 4 X0^2, Paso 5 teórico, Paso 6 conclusión.
     */
    @SuppressWarnings("unchecked")
    private static String cruce(Map<String, Object> c) {
        List<String> filas = (List<String>) c.get("filas");
        List<String> cols = (List<String>) c.get("columnas");
        int[][] fo = (int[][]) c.get("observada");
        double[][] fe = (double[][]) c.get("esperada");
        List<Integer> tf = (List<Integer>) c.get("totalFila");
        List<Integer> tc = (List<Integer>) c.get("totalColumna");
        int nTot = i(c.get("n"));
        int f = filas.size();
        int m = cols.size();
        String id = (String) c.get("id");
        String vx = t((String) c.get("variableFilas")), vy = t((String) c.get("variableColumnas"));
        String esquina = "$X$: " + vx + " \\textbackslash{} $Y$: " + vy;
        String cabeceraY = String.join(" & ", cols.stream().map(ReporteTex::t).toList());
        // Anchos fijos para que los nombres largos de las categorías pasen de línea en vez de salirse del margen
        String colY = "C{" + n(Math.min(3.0, 10.0 / m), 2) + "cm}";
        String colY4 = "C{" + n(Math.min(3.2, 10.4 / m), 2) + "cm}";

        StringBuilder sb = new StringBuilder();
        sb.append("\\subsubsection*{").append(id).append(". ").append(t((String) c.get("titulo"))).append("}\n\n");

        // Paso 1: problema e hipótesis
        sb.append("\\textit{Paso 1. Describir el problema.} ").append(t((String) c.get("proposito"))).append("\n\n");
        sb.append("\\begin{itemize}[nosep]\n");
        sb.append("  \\item Variable independiente $X$: ").append(vx).append(".\n");
        sb.append("  \\item Variable dependiente $Y$: ").append(vy).append(".\n");
        sb.append("  \\item Población: ").append(t(((String) c.get("poblacion")).toLowerCase())).append(" ($N = ").append(nTot)
          .append("$). Criterio ARCU-SUR: ").append(t((String) c.get("criterio"))).append(". Nivel de significancia $\\alpha = 0.05$.\n");
        if (c.get("nota") != null) sb.append("  \\item ").append(t((String) c.get("nota"))).append("\n");
        sb.append("  \\item $H_0$: ").append(t((String) c.get("hipotesisNula"))).append("\n");
        sb.append("  \\item $H_1$: ").append(t((String) c.get("hipotesisAlterna"))).append("\n");
        sb.append("\\end{itemize}\n\n");

        // Datos (primeros titulados)
        List<List<String>> muestra = (List<List<String>>) c.get("muestra");
        if (muestra != null && !muestra.isEmpty()) {
            sb.append("\\textit{Datos.} Primeros ").append(muestra.size()).append(" de los ").append(nTot)
              .append(" titulados que respondieron ambas preguntas (sin datos personales).\n\n");
            sb.append("\\begin{mitabla}{").append(id).append(": datos de los primeros titulados}\n\\begin{tabular}{lll}\n\\toprule\nTitulado & $X$: ").append(vx)
              .append(" & $Y$: ").append(vy).append(" \\\\\n\\midrule\n");
            for (List<String> fila : muestra) sb.append(fila.get(0)).append(" & ").append(t(fila.get(1))).append(" & ").append(t(fila.get(2))).append(" \\\\\n");
            sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
        }

        // Paso 2: tabla de contingencia
        sb.append("\\textit{Paso 2. Tabla de contingencia} (frecuencia observada $FO_{ij}$, $N = ").append(nTot).append("$). Cada celda cuenta a los titulados que respondieron a la vez la fila ($X$) y la columna ($Y$).\n\n");
        sb.append("\\begin{mitabla}{").append(id).append(": tabla de contingencia, frecuencia observada $FO_{ij}$").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{L{3.8cm}").append(colY.repeat(m)).append("|c}\n\\toprule\n").append(esquina).append(" & ").append(cabeceraY).append(" & Total \\\\\n\\midrule\n");
        for (int a = 0; a < f; a++) {
            sb.append(t(filas.get(a)));
            for (int b = 0; b < m; b++) sb.append(" & ").append(fo[a][b]);
            sb.append(" & ").append(tf.get(a)).append(" \\\\\n");
        }
        sb.append("\\midrule\nTotal");
        for (int b = 0; b < m; b++) sb.append(" & ").append(tc.get(b));
        sb.append(" & $N = ").append(nTot).append("$ \\\\\n\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");

        // Paso 3: frecuencia esperada
        sb.append("\\textit{Paso 3. Frecuencia esperada.} Es lo que habría en cada celda si $X$ e $Y$ fueran independientes. Por ejemplo, para la celda ")
          .append(t(filas.get(0))).append(" y ").append(t(cols.get(0))).append(":\n\\[\nFE_{11} = \\frac{(").append(tf.get(0)).append(")(").append(tc.get(0))
          .append(")}{").append(nTot).append("} = ").append(n(fe[0][0], 2)).append("\n\\]\n\n");
        sb.append("\\begin{mitabla}{").append(id).append(": frecuencia esperada $FE_{ij}$").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{L{3.8cm}").append(colY.repeat(m)).append("}\n\\toprule\n").append(esquina).append(" & ").append(cabeceraY).append(" \\\\\n\\midrule\n");
        for (int a = 0; a < f; a++) {
            sb.append(t(filas.get(a)));
            for (int b = 0; b < m; b++) sb.append(" & $").append(tf.get(a)).append("\\cdot").append(tc.get(b)).append("/").append(nTot).append(" = ").append(n(fe[a][b], 2)).append("$");
            sb.append(" \\\\\n");
        }
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");

        // Paso 4: X0^2 en la misma grilla
        sb.append("\\textit{Paso 4. Estadístico chi-cuadrado.} En cada celda se calcula $\\frac{(FO_{ij}-FE_{ij})^2}{FE_{ij}}$ y luego se suman todas.\n\n");
        sb.append("\\begin{mitabla}{").append(id).append(": cálculo de $X_0^2$ celda por celda").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{L{3.2cm}").append(colY4.repeat(m)).append("|c}\n\\toprule\n").append(esquina).append(" & ").append(cabeceraY).append(" & Suma fila \\\\\n\\midrule\n");
        double suma = 0;
        List<String> sumasFila = new ArrayList<>();
        for (int a = 0; a < f; a++) {
            sb.append(t(filas.get(a)));
            double filaSuma = 0;
            for (int b = 0; b < m; b++) {
                double aporte = (fo[a][b] - fe[a][b]) * (fo[a][b] - fe[a][b]) / fe[a][b];
                filaSuma += aporte;
                sb.append(" & $\\dfrac{(").append(fo[a][b]).append("-").append(n(fe[a][b], 2)).append(")^2}{").append(n(fe[a][b], 2)).append("}$\\newline $= ").append(n(aporte, 4)).append("$");
            }
            suma += filaSuma;
            sumasFila.add(n(filaSuma, 4));
            sb.append(" & $").append(n(filaSuma, 4)).append("$ \\\\[6pt]\n");
        }
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
        sb.append("\\[\nX_0^2 = ").append(String.join(" + ", sumasFila)).append(" = ").append(n(suma, 4)).append("\n\\]\n\n");

        // Paso 5: teórico
        int gl = i(c.get("gradosLibertad"));
        double teo = d(c.get("chiTeorico"));
        boolean indep = (Boolean) c.get("independientes");
        sb.append("\\textit{Paso 5. Estadístico teórico.} Con $f = ").append(f).append("$ filas y $c = ").append(m).append("$ columnas: $gl = (").append(f)
          .append("-1)(").append(m).append("-1) = ").append(gl).append("$. De la tabla chi-cuadrada, con $\\alpha = 0.05$: $X^2_{").append(gl).append(",\\,0.05} = ")
          .append(n(teo, 3)).append("$.\n\n");

        // Paso 6: relación y conclusión
        sb.append("\\textit{Paso 6. Relación entre estadísticos y conclusión.}\n\n\\begin{center}\n\\begin{tabular}{l}\n");
        sb.append("$").append(n(suma, 4)).append(indep ? " < " : " \\ge ").append(n(teo, 3)).append(" \\rightarrow$ ").append(indep ? "verdad" : "falso").append("\\\\\n");
        sb.append("Si ($X_0^2 < X^2_{(f-1)(c-1),\\,\\alpha}$)\\\\\n");
        sb.append("\\quad ").append(indep ? "\\fbox{``Independencia''}" : "``Independencia''").append("\\\\\nSino\\\\\n");
        sb.append("\\quad ").append(indep ? "NO ``Independencia''" : "\\fbox{NO ``Independencia''}").append("\\\\\nFinSi\n\\end{tabular}\n\\end{center}\n\n");
        if (indep) {
            sb.append("No se rechaza $H_0$: con estos datos, «").append(vy).append("» no depende de «").append(vx).append("».");
        } else {
            sb.append("Se rechaza $H_0$ y se acepta $H_1$: «").append(vy).append("» depende de «").append(vx).append("»")
              .append(". Porcentaje de cada fila en la columna ``").append(t(cols.get(0))).append("'': ");
            List<String> partes = new ArrayList<>();
            for (int a = 0; a < f; a++) partes.add(t(filas.get(a)) + " $" + fo[a][0] + "/" + tf.get(a) + " = " + n(100.0 * fo[a][0] / tf.get(a), 1) + "\\%$");
            sb.append(String.join("; ", partes)).append(".");
        }
        sb.append(marca()).append("\n\n");
        int menor5 = i(c.get("celdasFeMenor5"));
        if (menor5 > 0) {
            sb.append("\\textit{Observación:} ").append(menor5).append(" de ").append(f * m)
              .append(" celdas tienen $FE < 5$, por lo que la prueba pierde confiabilidad y la conclusión debe tomarse con cautela.\n\n");
        }
        if (c.get("recomendacion") != null) sb.append("\\textit{Recomendación.} ").append(t((String) c.get("recomendacion"))).append("\n\n");
        return sb.toString();
    }

    // ===================== Principio de uniformidad: prueba de las frecuencias =====================

    /** Desarrollo de una prueba de las frecuencias con los 6 pasos de los apuntes de clase (cap. 3). */
    private static String uniformidad(Map<String, Object> u) {
        String id = (String) u.get("id");
        int nTot = i(u.get("n")), k = i(u.get("k"));
        String variable = t((String) u.get("variable"));
        StringBuilder sb = new StringBuilder();
        sb.append("\\subsubsection*{").append(id).append(". ").append(t((String) u.get("titulo"))).append("}\n\n");
        if (u.get("uniforme") == null) {
            return sb.append("No hay respuestas suficientes en este período para aplicar la prueba.\n\n").toString();
        }
        List<String> cats = (List<String>) u.get("categorias");
        List<Integer> fo = (List<Integer>) u.get("observada");
        List<Double> aportes = (List<Double>) u.get("aportes");
        double fe = d(u.get("esperada")), x0 = d(u.get("chiCuadrada")), teo = d(u.get("chiTeorico"));
        int gl = i(u.get("gradosLibertad"));
        boolean uniforme = (Boolean) u.get("uniforme");

        sb.append(t((String) u.get("proposito"))).append("\n\n");
        sb.append("\\textit{Paso 1. Contar con la muestra y determinar su tamaño.} Población: ")
          .append(t(((String) u.get("poblacion")).toLowerCase())).append("; tamaño $n = ").append(nTot)
          .append("$; opciones de la pregunta $k = ").append(k).append("$; $\\alpha = 0.05$.\n\n");
        sb.append("\\begin{itemize}[nosep]\n");
        sb.append("  \\item $H_0$: ").append(t((String) u.get("hipotesisNula"))).append("\n");
        sb.append("  \\item $H_1$: ").append(t((String) u.get("hipotesisAlterna"))).append("\n");
        sb.append("\\end{itemize}\n\n");

        sb.append("\\textit{Paso 2. Calcular la frecuencia esperada.} Si las respuestas se repartieran por igual, cada opción tendría:\n");
        sb.append("\\[\nFE = \\frac{n}{k} = \\frac{").append(nTot).append("}{").append(k).append("} = ").append(n(fe, 2)).append("\n\\]\n\n");

        sb.append("\\textit{Paso 3. Calcular el estadístico chi-cuadrado.} Se cuenta la frecuencia observada $FO_i$ de cada opción y se calcula $\\frac{(FO_i - FE)^2}{FE}$.\n\n");
        sb.append("\\begin{mitabla}{").append(id).append(": prueba de las frecuencias, ").append(variable).append(marca()).append("}\n");
        sb.append("\\begin{tabular}{L{6cm}ccc}\n\\toprule\nOpción & $FO_i$ & $FE$ & $\\frac{(FO_i-FE)^2}{FE}$ \\\\\n\\midrule\n");
        for (int a = 0; a < k; a++) {
            sb.append(t(cats.get(a))).append(" & ").append(fo.get(a)).append(" & ").append(n(fe, 2)).append(" & $\\frac{(")
              .append(fo.get(a)).append("-").append(n(fe, 2)).append(")^2}{").append(n(fe, 2)).append("} = ").append(n(aportes.get(a), 4)).append("$ \\\\[3pt]\n");
        }
        sb.append("\\midrule\nTotal & ").append(nTot).append(" & ").append(nTot).append(" & $x_0^2 = ").append(n(x0, 4)).append("$ \\\\\n");
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
        sb.append("\\[\nx_0^2 = \\sum_{i=1}^{").append(k).append("} \\frac{(FO_i - FE)^2}{FE} = ").append(n(x0, 4)).append("\n\\]\n\n");

        sb.append("\\textit{Paso 4. Determinar el estadístico teórico.} $x^2_{\\alpha,\\,k-1} = x^2_{0.05,\\,").append(k).append("-1} = x^2_{0.05,\\,")
          .append(gl).append("} = ").append(n(teo, 3)).append("$ (tabla chi-cuadrada).\n\n");
        sb.append("\\textit{Paso 5. Establecer la relación entre estadísticos.} $x_0^2 < x^2_{\\alpha,\\,k-1}$ $\\rightarrow$ $")
          .append(n(x0, 4)).append(uniforme ? " < " : " \\ge ").append(n(teo, 3)).append("$ $\\rightarrow$ ").append(uniforme ? "sí" : "no").append(".\n\n");

        sb.append("\\textit{Paso 6. Conclusión.}\n\n\\begin{center}\n\\begin{tabular}{l}\n");
        sb.append("Si ($x_0^2 < x^2_{\\alpha,\\,k-1}$)\\\\\n");
        sb.append("\\quad ").append(uniforme ? "\\fbox{``Uniformidad''}" : "``Uniformidad''").append("\\\\\nSino\\\\\n");
        sb.append("\\quad ").append(uniforme ? "No ``Uniformidad''" : "\\fbox{No ``Uniformidad''}").append("\\\\\nFinSi\n\\end{tabular}\n\\end{center}\n\n");
        String masElegida = t((String) u.get("masElegida"));
        String datoMayor = "«" + masElegida + "» (" + i(u.get("frecuenciaMasElegida")) + " de " + nTot + ", "
            + pct(d(u.get("proporcionMasElegida"))) + ")";
        if (uniforme) {
            sb.append("No se rechaza $H_0$: las respuestas se reparten de forma pareja entre las ").append(k)
              .append(" opciones; la diferencia que se ve (la más elegida es ").append(datoMayor).append(") puede deberse al azar.");
        } else {
            sb.append("Se rechaza $H_0$: las respuestas no se reparten por igual, hay opciones preferidas. La más elegida es ")
              .append(datoMayor).append(".");
        }
        sb.append(marca()).append("\n\n");
        if (Boolean.TRUE.equals(u.get("feMenor5"))) {
            sb.append("\\textit{Observación:} $FE = ").append(n(fe, 2))
              .append(" < 5$: hay pocas respuestas para tantas opciones, por lo que la conclusión debe tomarse con cautela.\n\n");
        }
        if (u.get("recomendacion") != null) sb.append("\\textit{Recomendación.} ").append(t((String) u.get("recomendacion"))).append("\n\n");
        return sb.toString();
    }

    private static String resumenUniformidad() {
        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{mitabla}{Resumen de las pruebas de las frecuencias, principio de uniformidad ($\\alpha = 0.05$)").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{cL{4.4cm}cccccL{3.2cm}}\n\\toprule\nId & Pregunta & $n$ & $k$ & $FE$ & $x_0^2$ & $x_{0.05,\\,k-1}^2$ & Conclusión \\\\\n\\midrule\n");
        for (Map<String, Object> u : Analisis.uniformidades(titulados, empleadores)) {
            sb.append(u.get("id")).append(" & ").append(t((String) u.get("titulo"))).append(" & ").append(u.get("n")).append(" & ").append(u.get("k")).append(" & ");
            if (u.get("uniforme") == null) {
                sb.append("-- & -- & -- & Sin datos \\\\\n");
                continue;
            }
            boolean uniforme = (Boolean) u.get("uniforme");
            sb.append(n(d(u.get("esperada")), 2)).append(" & ").append(n(d(u.get("chiCuadrada")), 3)).append(" & ").append(n(d(u.get("chiTeorico")), 3)).append(" & ")
              .append(uniforme ? "Uniformidad" : "\\textbf{No uniformidad}: «" + t((String) u.get("masElegida")) + "»").append(" \\\\\n");
        }
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
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
          .append("''; cruces ").append(cr.apply("C2")).append(", ").append(cr.apply("C6")).append(", ").append(cr.apply("C9")).append(", ")
          .append(cr.apply("C10")).append(" y ").append(cr.apply("C12")).append(". El tiempo medio no tiene un valor de referencia en ARCU-SUR, por lo que queda como línea base para el seguimiento.\n\n");
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
            if (Criterios.ITEMS_PERFIL_EGRESO.contains(cod)) {
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
          .append(", ").append(cr.apply("C5")).append(", ").append(cr.apply("C7")).append(", ").append(cr.apply("C8")).append(", ").append(cr.apply("C11"))
          .append(" y ").append(cr.apply("C13")).append(".\n\n");
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

        // Dos tablas, igual que en el panel: perfil de egreso (2.1.2) y relación con la Carrera (2.4.2); juntas no caben en una página
        String[][] grupos = {{"Perfil de egreso (criterio 2.1.2)", "egreso"}, {"Relación con la Carrera (criterio 2.4.2)", "relacion"}};
        for (String[] g : grupos) {
            List<String> codigos = g[1].equals("egreso") ? Criterios.ITEMS_PERFIL_EGRESO : Criterios.ITEMS_RELACION;
            sb.append("\\begin{mitabla}{").append(g[0]).append(": frecuencia $f_i$ y porcentaje $f_i/n$ por opción ($n=").append(n).append("$)").append(marca()).append("}\n");
            sb.append("\\begin{tabular}{L{5.6cm}C{2.0cm}C{2.0cm}C{2.1cm}C{1.5cm}}\n\\toprule\nAfirmación & Tot.\\ de acuerdo & Parc.\\ de acuerdo & Tot.\\ en desacuerdo & No sabe \\\\\n\\midrule\n");
            for (Map<String, Object> p : lista(e.get("perfil"))) {
                if (!codigos.contains(p.get("codigo"))) continue;
                sb.append(t((String) p.get("criterio")));
                for (Map<String, Object> f : lista(p.get("frecuencias"))) {
                    int fi = i(f.get("frecuencia"));
                    sb.append(" & ").append(fi).append(" (").append(n(100.0 * fi / n, 0)).append("\\%)");
                }
                sb.append(" \\\\\n");
            }
            sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");
        }
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

    // ===================== Textos dinámicos (resumen, conclusiones, propuestas) =====================

    private static String fmtN(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : n(v, 2);
    }

    /** Proporción de una categoría (por código) dentro de una lista de frecuencias. */
    private static double prop(List<Map<String, Object>> frec, String codigo) {
        return frec.stream().filter(f -> codigo.equals(f.get("codigo"))).mapToDouble(f -> d(f.get("relativa"))).findFirst().orElse(0);
    }

    private static List<Map<String, Object>> ordenDesc(List<Map<String, Object>> frec) {
        List<Map<String, Object>> o = new ArrayList<>(frec);
        o.sort((a, b) -> Integer.compare(i(b.get("frecuencia")), i(a.get("frecuencia"))));
        return o;
    }

    private static Map<String, Map<String, Object>> crucesPorId() {
        Map<String, Map<String, Object>> m = new LinkedHashMap<>();
        Analisis.cruces(titulados).forEach(c -> m.put((String) c.get("id"), c));
        return m;
    }

    private static boolean dependiente(Map<String, Object> c) {
        return Boolean.FALSE.equals(c.get("independientes"));
    }

    /** Columna de Y en la que más difieren las filas, con el porcentaje de cada fila: así se lee la dependencia. */
    @SuppressWarnings("unchecked")
    private static String lecturaDependencia(Map<String, Object> c) {
        List<String> filas = (List<String>) c.get("filas");
        List<String> cols = (List<String>) c.get("columnas");
        int[][] fo = (int[][]) c.get("observada");
        List<Integer> tf = (List<Integer>) c.get("totalFila");
        int mejor = 0;
        double rango = -1;
        for (int j = 0; j < cols.size(); j++) {
            double min = 1, max = 0;
            for (int a = 0; a < filas.size(); a++) {
                double p = tf.get(a) == 0 ? 0 : (double) fo[a][j] / tf.get(a);
                min = Math.min(min, p);
                max = Math.max(max, p);
            }
            if (max - min > rango) { rango = max - min; mejor = j; }
        }
        List<String> partes = new ArrayList<>();
        for (int a = 0; a < filas.size(); a++) partes.add(t(filas.get(a)) + " " + pct(tf.get(a) == 0 ? 0 : (double) fo[a][mejor] / tf.get(a)));
        String lectura = "el porcentaje que respondió ``" + t(cols.get(mejor)) + "'' en cada grupo de ``" + t((String) c.get("variableFilas")) + "'' es: " + String.join("; ", partes);
        int bajos = i(c.get("celdasFeMenor5"));
        if (bajos > 0) lectura += " (" + bajos + " de " + i(c.get("totalCeldas")) + " celdas con $FE<5$, conclusión tentativa)";
        return lectura;
    }

    /** Categoría de Y más elegida en cada fila de X: «fila»: «columna» (porcentaje). */
    @SuppressWarnings("unchecked")
    private static String masElegidaPorFila(Map<String, Object> c) {
        List<String> filas = (List<String>) c.get("filas");
        List<String> cols = (List<String>) c.get("columnas");
        int[][] fo = (int[][]) c.get("observada");
        List<Integer> tf = (List<Integer>) c.get("totalFila");
        List<String> partes = new ArrayList<>();
        for (int a = 0; a < filas.size(); a++) {
            int jm = 0;
            for (int j = 1; j < cols.size(); j++) if (fo[a][j] > fo[a][jm]) jm = j;
            partes.add(t(filas.get(a)) + ": " + t(cols.get(jm)) + " (" + pct(tf.get(a) == 0 ? 0 : (double) fo[a][jm] / tf.get(a)) + ")");
        }
        return String.join("; ", partes);
    }

    /** Fila de X con mayor porcentaje en una columna de Y dada. */
    @SuppressWarnings("unchecked")
    private static String[] filaConMas(Map<String, Object> c, String columna) {
        List<String> filas = (List<String>) c.get("filas");
        List<String> cols = (List<String>) c.get("columnas");
        int[][] fo = (int[][]) c.get("observada");
        List<Integer> tf = (List<Integer>) c.get("totalFila");
        int j = cols.indexOf(columna);
        if (j < 0) return null;
        int am = 0;
        double pm = -1;
        for (int a = 0; a < filas.size(); a++) {
            double p = tf.get(a) == 0 ? 0 : (double) fo[a][j] / tf.get(a);
            if (p > pm) { pm = p; am = a; }
        }
        return new String[]{t(filas.get(am)), pct(pm)};
    }

    private static double propUmss(Map<String, Object> form) {
        List<Map<String, Object>> org = lista(form.get("organizacionInteres"));
        int umss = org.stream().filter(f -> List.of("EUPG", "Direccion_Posgrado_FCyT").contains(f.get("codigo"))).mapToInt(f -> i(f.get("frecuencia"))).sum();
        int tot = org.stream().mapToInt(f -> i(f.get("frecuencia"))).sum();
        return tot == 0 ? 0 : (double) umss / tot;
    }

    private static Map<String, Object> criterio(List<Map<String, Object>> crits, String id) {
        return crits.stream().filter(c -> id.equals(c.get("id"))).findFirst().orElse(Map.of());
    }

    private static String aspectos(List<Map<String, Object>> lista) {
        return String.join("; ", lista.stream().map(a -> "``" + t((String) a.get("texto")) + "'' (" + pct(d(a.get("proporcion"))) + ")").toList());
    }

    private static String valores() {
        return "\\newcommand{\\nTitulados}{" + titulados.n() + "}\n\\newcommand{\\nEmpleadores}{" + empleadores.n() + "}\n";
    }

    @SuppressWarnings("unchecked")
    private static String resumenResultados() {
        Map<String, Object> emp = Analisis.empleabilidad(titulados);
        Map<String, Object> comp = Analisis.competencias(titulados, empleadores);
        Map<String, Object> form = Analisis.formacionContinua(titulados);
        List<Map<String, Object>> crits = Criterios.evaluar(titulados, empleadores);
        Map<String, Object> tpe = mapa(mapa(emp.get("tiempoPrimerEmpleo")).get("mesesIC95"));
        Map<String, Object> mej = lista(comp.get("aspectosMejorar")).get(0);
        Map<String, Object> area = moda(lista(form.get("areaInteres")));
        double umss = propUmss(form);
        List<Double> perfil = new ArrayList<>();
        Map<String, Object> c212 = criterio(crits, "2.1.2");
        for (String k : List.of("favorables", "desfavorables")) for (Map<String, Object> a : (List<Map<String, Object>>) c212.getOrDefault(k, List.of())) perfil.add(d(a.get("proporcion")));
        Map<String, Object> c242 = criterio(crits, "2.4.2");
        int des242 = ((List<?>) c242.getOrDefault("desfavorables", List.of())).size();
        int tot242 = des242 + ((List<?>) c242.getOrDefault("favorables", List.of())).size();
        Map<String, Map<String, Object>> cruces = crucesPorId();
        List<String> dep = cruces.values().stream().filter(ReporteTex::dependiente)
            .map(c -> "``" + t((String) c.get("variableFilas")) + "'' con ``" + t((String) c.get("variableColumnas")) + "'' (" + c.get("id") + ")").toList();
        StringBuilder sb = new StringBuilder();
        sb.append("Con ").append(titulados.n()).append(" titulados y ").append(empleadores.n()).append(" organizaciones empleadoras, el ")
          .append(pct(d(mapa(emp.get("tasaEmpleo")).get("proporcion")))).append(" de los titulados trabaja o emprende y tarda en promedio ")
          .append(n(d(tpe.get("media")), 2)).append(" meses en conseguir su primer empleo (IC 95\\%: ").append(n(d(tpe.get("limiteInferior")), 2))
          .append(" a ").append(n(d(tpe.get("limiteSuperior")), 2)).append(" meses); el ").append(pct(acuerdo(mapa(emp.get("pertinenciaPuesto")))))
          .append(" considera que sus labores son pertinentes a su formación y el ").append(pct(acuerdo(mapa(comp.get("satisfaccionGlobal")))))
          .append(" está satisfecho con ella, y lo que más se pide mejorar es ``").append(t((String) mej.get("etiqueta"))).append("'' (")
          .append(pct(d(mej.get("relativa")))).append("). El ").append(pct(d(mapa(form.get("interesPosgrado")).get("proporcion"))))
          .append(" quiere hacer un posgrado, sobre todo en ``").append(t((String) area.get("etiqueta"))).append("'', ")
          .append(umss > 0.5 ? "y el " : "pero solo el ").append(pct(umss)).append(" de ellos lo haría en la UMSS. ");
        if (!perfil.isEmpty()) {
            sb.append("Los empleadores valoran el perfil profesional (entre el ").append(pct(perfil.stream().mapToDouble(x -> x).min().orElse(0)))
              .append(" y el ").append(pct(perfil.stream().mapToDouble(x -> x).max().orElse(0))).append(" de acuerdo)");
            sb.append(des242 > 0 ? ", pero en la relación con la Carrera " + des242 + " de " + tot242 + " aspectos tienen menos de la mitad de acuerdo. " : ". ");
        }
        sb.append("De los ").append(cruces.size()).append(" cruces de variables, ").append(dep.size()).append(dep.size() == 1 ? " muestra" : " muestran")
          .append(" dependencia significativa al 5\\%").append(dep.isEmpty() ? "." : ": " + String.join(", ", dep) + ".").append(marca());
        return sb.toString();
    }

    private static String abiertasInterpretacion() {
        Map<String, Object> ab = mapa(Analisis.empleadores(empleadores).get("preguntasAbiertas"));
        java.util.function.Function<String, List<Map<String, Object>>> top = k -> lista(ab.get(k)).stream()
            .filter(f -> !"sin_categoria".equals(f.get("codigo")) && i(f.get("frecuencia")) > 0).toList();
        List<Map<String, Object>> areas = top.apply("areas_emergentes"), herr = top.apply("herramientas"), hab = top.apply("habilidades");
        java.util.function.Function<Map<String, Object>, String> c = f -> "``" + t((String) f.get("etiqueta")) + "'' (" + pct(d(f.get("relativa"))) + " de las respuestas)";
        StringBuilder sb = new StringBuilder();
        if (!areas.isEmpty()) {
            sb.append("Como área de conocimiento emergente, la categoría más mencionada es ").append(c.apply(areas.get(0)));
            if (areas.size() > 1) sb.append(", seguida de ").append(c.apply(areas.get(1)));
            sb.append(". ");
        }
        if (!herr.isEmpty()) sb.append("En herramientas, lo más pedido es ").append(c.apply(herr.get(0))).append(". ");
        if (!hab.isEmpty()) {
            sb.append("En habilidades, lo más mencionado es ").append(c.apply(hab.get(0)));
            if (hab.size() > 1) sb.append(", seguido de ").append(c.apply(hab.get(1)));
            sb.append(".");
        }
        return sb.append(marca()).append("\n").toString();
    }

    @SuppressWarnings("unchecked")
    private static String conclusiones() {
        Map<String, Object> emp = Analisis.empleabilidad(titulados);
        Map<String, Object> comp = Analisis.competencias(titulados, empleadores);
        Map<String, Object> form = Analisis.formacionContinua(titulados);
        Map<String, Object> fin = Analisis.financiamiento(titulados);
        Map<String, Object> empl = Analisis.empleadores(empleadores);
        List<Map<String, Object>> crits = Criterios.evaluar(titulados, empleadores);
        Map<String, Map<String, Object>> cruces = crucesPorId();
        StringBuilder sb = new StringBuilder("\\begin{enumerate}\n");

        // 3.2.3
        Map<String, Object> c323 = criterio(crits, "3.2.3");
        List<Map<String, Object>> fav = (List<Map<String, Object>>) c323.getOrDefault("favorables", List.of());
        List<Map<String, Object>> des = (List<Map<String, Object>>) c323.getOrDefault("desfavorables", List.of());
        Map<String, Object> tpe = mapa(mapa(emp.get("tiempoPrimerEmpleo")).get("mesesIC95"));
        Map<String, Object> dest = mapa(emp.get("destinoLaboral"));
        Map<String, Object> rubro = moda(lista(dest.get("rubro")));
        sb.append("  \\item \\textbf{Condiciones de empleo (3.2.3).} ").append(fav.size()).append(" de ").append(fav.size() + des.size())
          .append(" indicadores del criterio son favorables");
        if (!fav.isEmpty()) sb.append(": ").append(aspectos(fav));
        sb.append(des.isEmpty() ? ". " : "; desfavorables: " + aspectos(des) + ". ");
        sb.append("El tiempo medio para conseguir el primer empleo es de ").append(n(d(tpe.get("media")), 2)).append(" meses (IC 95\\%: ")
          .append(n(d(tpe.get("limiteInferior")), 2)).append(" a ").append(n(d(tpe.get("limiteSuperior")), 2))
          .append("), y queda como línea base porque ARCU-SUR no fija un valor de referencia. El ")
          .append(pct(prop(lista(dest.get("sector_publico_privado")), "Privado"))).append(" de quienes trabajan en una organización lo hace en el sector privado, principalmente en ``")
          .append(t((String) rubro.get("etiqueta"))).append("''.");
        Object med = emp.get("medianaVidaProfesional");
        if (med != null) {
            double media = d(mapa(emp.get("aniosVidaProfesional")).get("media"));
            sb.append(" La mediana de experiencia es de ").append(fmtN(d(med))).append(" años (media ").append(n(media, 2)).append(")");
            List<Map<String, Object>> seg = lista(emp.get("segmentosExperiencia"));
            if (seg.size() == 2 && i(seg.get(0).get("conCargo")) > 0 && i(seg.get(1).get("conCargo")) > 0) {
                sb.append("; ocupa un cargo ejecutivo el ").append(pct((double) i(seg.get(1).get("ejecutivos")) / i(seg.get(1).get("conCargo"))))
                  .append(" de los consolidados que declararon un cargo, frente al ").append(pct((double) i(seg.get(0).get("ejecutivos")) / i(seg.get(0).get("conCargo"))))
                  .append(" de los junior");
            }
            sb.append(".");
        }
        sb.append("\n");

        // 3.2.1 y 2.1.7
        Map<String, Object> mej = lista(comp.get("aspectosMejorar")).get(0);
        Map<String, Object> vent = lista(comp.get("asignaturasVentaja")).get(0);
        List<Map<String, Object>> areasEmp = lista(mapa(empl.get("preguntasAbiertas")).get("areas_emergentes")).stream()
            .filter(f -> !"sin_categoria".equals(f.get("codigo")) && i(f.get("frecuencia")) > 0).limit(2).toList();
        double sat = acuerdo(mapa(comp.get("satisfaccionGlobal")));
        sb.append("  \\item \\textbf{Resultados de la formación (3.2.1) y actualización curricular (2.1.7).} El ").append(pct(sat))
          .append(" está satisfecho con la formación recibida, ").append(sat > 0.5 ? "pero" : "y").append(" el ").append(pct(d(mej.get("relativa"))))
          .append(" señala ``").append(t((String) mej.get("etiqueta"))).append("'' como el aspecto a mejorar más importante. ``")
          .append(t((String) vent.get("etiqueta"))).append("'' es la asignatura que más ventaja da en el trabajo (").append(pct(d(vent.get("relativa")))).append(")");
        if (!areasEmp.isEmpty()) sb.append(", y los empleadores piden reforzar sobre todo ").append(String.join(" y ", areasEmp.stream().map(f -> "``" + t((String) f.get("etiqueta")) + "''").toList()));
        sb.append(".\n");

        // 2.1.2
        Map<String, Object> c212 = criterio(crits, "2.1.2");
        List<Map<String, Object>> f212 = (List<Map<String, Object>>) c212.getOrDefault("favorables", List.of());
        List<Map<String, Object>> d212 = (List<Map<String, Object>>) c212.getOrDefault("desfavorables", List.of());
        sb.append("  \\item \\textbf{Perfil de egreso (2.1.2).} ").append(f212.size()).append(" de ").append(f212.size() + d212.size())
          .append(" ítems del perfil son favorables");
        if (!f212.isEmpty()) sb.append(": ").append(aspectos(f212));
        sb.append(d212.isEmpty() ? "." : "; desfavorables: " + aspectos(d212) + ".").append("\n");

        // 2.4.1
        Map<String, Object> c241 = criterio(crits, "2.4.1");
        List<Map<String, Object>> d241 = (List<Map<String, Object>>) c241.getOrDefault("desfavorables", List.of());
        Map<String, Object> area = moda(lista(form.get("areaInteres")));
        int nInt = i(mapa(form.get("interesPosgrado")).get("casos"));
        List<Map<String, Object>> areas = ordenDesc(lista(form.get("areaInteres")));
        double conc = areas.size() < 2 || nInt == 0 ? 0 : (double) (i(areas.get(0).get("frecuencia")) + i(areas.get(1).get("frecuencia"))) / nInt;
        sb.append("  \\item \\textbf{Posgrado (2.4.1 y 1.1.5).} El ").append(pct(d(mapa(form.get("interesPosgrado")).get("proporcion"))))
          .append(" quiere un posgrado, sobre todo en ``").append(t((String) area.get("etiqueta"))).append("'' (").append(pct(nInt == 0 ? 0 : (double) i(area.get("frecuencia")) / nInt))
          .append(" de los interesados); las dos áreas más elegidas reúnen el ").append(pct(conc)).append(" de las preferencias, ")
          .append(conc > 0.7 ? "más del 70\\% que el enunciado toma como referencia para abrir programas modulares" : "frente al 70\\% que el enunciado toma como referencia para abrir programas modulares")
          .append(". El ").append(pct(acuerdo(mapa(form.get("ofertaPosgradoFcyt"))))).append(" considera afín la oferta de la FCyT y el ").append(pct(propUmss(form)))
          .append(" de los interesados la elegiría");
        sb.append(d241.isEmpty() ? "." : "; aspectos desfavorables del criterio: " + aspectos(d241) + ".");
        sb.append(" El ").append(pct(prop(lista(fin.get("programaFinanciamiento")), "recursos_propios"))).append(" de quienes ya cursaron un programa lo pagó con recursos propios.\n");

        // 2.4.2
        Map<String, Object> c242 = criterio(crits, "2.4.2");
        List<Map<String, Object>> f242 = (List<Map<String, Object>>) c242.getOrDefault("favorables", List.of());
        List<Map<String, Object>> d242 = (List<Map<String, Object>>) c242.getOrDefault("desfavorables", List.of());
        sb.append("  \\item \\textbf{Relación con las organizaciones (2.4.2).} ").append(f242.size()).append(" de ").append(f242.size() + d242.size())
          .append(" aspectos son favorables");
        if (!f242.isEmpty()) sb.append(": ").append(aspectos(f242));
        sb.append(d242.isEmpty() ? "." : ". Con menos de la mitad de acuerdo: " + aspectos(d242) + ".").append("\n");

        // 3.2.2
        Map<String, Object> red = mapa(Analisis.demografia(titulados).get("redContactos"));
        sb.append("  \\item \\textbf{Seguimiento a graduados (3.2.2).} El ").append(pct(d(red.get("proporcion"))))
          .append(" quiere formar parte de una red de contactos de la Carrera, y la herramienta permite recibir respuestas de forma continua y recalcular los indicadores.\n");

        // Cruces
        List<Map<String, Object>> dep = cruces.values().stream().filter(ReporteTex::dependiente).toList();
        List<Map<String, Object>> ind = cruces.values().stream().filter(c -> Boolean.TRUE.equals(c.get("independientes"))).toList();
        sb.append("  \\item \\textbf{Cruces de variables.} ").append(dep.size()).append(" de los ").append(cruces.size())
          .append(" cruces muestran dependencia significativa al 5\\% (se rechaza $H_0$)");
        if (!dep.isEmpty()) {
            sb.append(":\n  \\begin{itemize}[nosep]\n");
            for (Map<String, Object> c : dep) {
                sb.append("    \\item \\emph{").append(t((String) c.get("titulo"))).append(" (").append(c.get("id")).append("):} ``")
                  .append(t((String) c.get("variableColumnas"))).append("'' depende de ``").append(t((String) c.get("variableFilas")))
                  .append("''; ").append(lecturaDependencia(c)).append(".\n");
            }
            sb.append("  \\end{itemize}\n  ");
        } else {
            sb.append(". ");
        }
        if (!ind.isEmpty()) {
            sb.append("En los otros ").append(ind.size()).append(" no se rechaza la independencia: ")
              .append(String.join(", ", ind.stream().map(c -> t((String) c.get("titulo")) + " (" + c.get("id") + ")").toList())).append(".");
        }
        sb.append(marca()).append("\n\\end{enumerate}\n");
        return sb.toString();
    }

    private static String propuestas() {
        Map<String, Object> comp = Analisis.competencias(titulados, empleadores);
        Map<String, Object> form = Analisis.formacionContinua(titulados);
        Map<String, Object> fin = Analisis.financiamiento(titulados);
        Map<String, Object> empl = Analisis.empleadores(empleadores);
        List<Map<String, Object>> crits = Criterios.evaluar(titulados, empleadores);
        Map<String, Map<String, Object>> cr = crucesPorId();
        StringBuilder sb = new StringBuilder("\\begin{itemize}\n");

        // Becas (HU3, C7)
        sb.append("  \\item \\textbf{Becas y alianzas de financiamiento:} el ").append(pct(prop(lista(fin.get("programaFinanciamiento")), "recursos_propios")))
          .append(" de quienes cursaron un programa lo financió con recursos propios");
        Map<String, Object> c7 = cr.get("C7");
        if (c7 != null && dependiente(c7)) {
            String[] f = filaConMas(c7, "Con beca (total o parcial)");
            if (f != null) sb.append(", y las pocas becas se concentran en ``").append(f[0]).append("'' (").append(f[1]).append(" con beca, C7)");
        }
        sb.append(". Buscar financiamiento a través de organismos internacionales y convenios con instituciones y empresas para ampliar las becas (HU3).\n");

        // Oferta de posgrado (C8, C11)
        Map<String, Object> c8 = cr.get("C8"), c11 = cr.get("C11");
        List<Map<String, Object>> areas = ordenDesc(lista(form.get("areaInteres")));
        String topAreas = String.join(", ", areas.stream().limit(3).map(f -> "``" + t((String) f.get("etiqueta")) + "''").toList());
        double umss = propUmss(form);
        sb.append("  \\item \\textbf{Posgrados modulares} en las áreas de mayor interés (").append(topAreas).append(")");
        if (umss < 0.5) sb.append(", para captar al ").append(pct(1 - umss)).append(" de interesados que hoy elegiría otra institución");
        sb.append(" (criterio 2.4.1)");
        if (c8 != null && dependiente(c8)) sb.append(", orientados por grupo de edad (C8): ").append(masElegidaPorFila(c8));
        if (c11 != null && dependiente(c11)) sb.append("; y rutas de continuidad (diplomado, especialidad, maestría) para quienes ya cursaron un programa (C11)");
        sb.append(".\n");

        // Etapa profesional (C9, C10)
        Map<String, Object> c9 = cr.get("C9"), c10 = cr.get("C10");
        if ((c9 != null && dependiente(c9)) || (c10 != null && dependiente(c10))) {
            sb.append("  \\item \\textbf{Formación según la etapa profesional}: especializaciones técnicas para los titulados junior y formación en gestión y dirección para los consolidados, porque la experiencia influye en ")
              .append(c9 != null && dependiente(c9) && c10 != null && dependiente(c10) ? "la remuneración (C9) y en el cargo (C10)" : c9 != null && dependiente(c9) ? "la remuneración (C9)" : "el cargo (C10)").append(".\n");
        }

        // Inserción laboral (C12)
        Map<String, Object> c12 = cr.get("C12");
        if (c12 != null && dependiente(c12)) {
            String[] f = filaConMas(c12, "No trabaja");
            if (f != null) sb.append("  \\item \\textbf{Inserción laboral} (C12): reforzar la bolsa de trabajo y la vinculación con empleadores para el grupo de ``")
                .append(f[0]).append("'', donde el ").append(f[1]).append(" no trabaja.\n");
        }

        // Consulta a empleadores (2.4.2)
        Map<String, Object> c242 = criterio(crits, "2.4.2");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> d242 = new ArrayList<>((List<Map<String, Object>>) c242.getOrDefault("desfavorables", List.of()));
        if (!d242.isEmpty()) {
            d242.sort((a, b) -> Double.compare(d(a.get("proporcion")), d(b.get("proporcion"))));
            sb.append("  \\item \\textbf{Consulta periódica a empleadores}: incorporar a las organizaciones en los aspectos con menor acuerdo del criterio 2.4.2, en especial ")
              .append(aspectos(d242.subList(0, Math.min(2, d242.size())))).append(".\n");
        }

        // Actualización del plan (2.1.7)
        List<Map<String, Object>> areasEmp = lista(mapa(empl.get("preguntasAbiertas")).get("areas_emergentes")).stream()
            .filter(f -> !"sin_categoria".equals(f.get("codigo")) && i(f.get("frecuencia")) > 0).limit(3).toList();
        double actualizar = prop(lista(comp.get("aspectosMejorar")), "actualizacion_plan_estudios");
        sb.append("  \\item \\textbf{Actualización del plan de estudios}, que pide el ").append(pct(actualizar)).append(" de los titulados");
        if (!areasEmp.isEmpty()) sb.append(", con los contenidos que más mencionan los empleadores (").append(String.join(", ", areasEmp.stream().map(f -> "``" + t((String) f.get("etiqueta")) + "''").toList())).append(")");
        sb.append(" (criterio 2.1.7).\n");

        // Observatorio (3.2.2)
        Map<String, Object> red = mapa(Analisis.demografia(titulados).get("redContactos"));
        sb.append("  \\item \\textbf{Observatorio permanente de titulados}: mantener abierto el formulario de la herramienta y aprovechar el interés en la red de contactos (")
          .append(pct(d(red.get("proporcion")))).append(") para aplicar la encuesta de forma periódica (criterio 3.2.2).\n");
        sb.append("\\end{itemize}\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String multivariable() {
        Map<String, Object> m = Analisis.multivariable(titulados);
        List<Map<String, Object>> filas = (List<Map<String, Object>>) m.get("filas");
        if (filas.isEmpty()) return "No hay suficientes respuestas con nivel, financiamiento y años de vida profesional a la vez para este análisis.\n\n";
        int nTotal = i(m.get("nTotal"));
        double zPromGeneral = d(m.get("zPromGeneral"));

        StringBuilder sb = new StringBuilder();
        sb.append("\\textit{Paso 1. Variables.} Sobre los titulados que hicieron formación ")
          .append("complementaria, se cruzan dos variables cualitativas -- $X$ = nivel del ")
          .append("programa cursado, $Y$ = fuente de financiamiento -- con una variable ")
          .append("cuantitativa -- $Z$ = años de vida profesional --, siguiendo la tabulación ")
          .append("cruzada multivariada de la asignatura: frecuencia ($n$) y promedio de $Z$ ")
          .append("($\\bar{Z}$) en cada combinación $X$--$Y$ que aparece en los datos.\n\n");

        sb.append("\\textit{Paso 2. Tabulación cruzada de frecuencias y antigüedad promedio.}\n\n");
        sb.append("\\begin{mitabla}{Nivel del programa ($X$) y fuente de financiamiento ($Y$), con antigüedad promedio ($n=").append(nTotal).append("$)").append(marca()).append("}\n");
        sb.append("\\begin{tabular}{llcc}\n\\toprule\n$X$ (nivel) & $Y$ (financiamiento) & $n$ & $\\bar{Z}$ (años) \\\\\n\\midrule\n");
        for (Map<String, Object> f : filas) {
            sb.append(t((String) f.get("x"))).append(" & ").append(t((String) f.get("y"))).append(" & ")
              .append(i(f.get("n"))).append(" & ").append(n(d(f.get("zProm")), 2)).append(" \\\\\n");
        }
        sb.append("\\midrule\n\\multicolumn{2}{l}{Total / Promedio general} & ").append(nTotal).append(" & ").append(n(zPromGeneral, 2)).append(" \\\\\n");
        sb.append("\\bottomrule\n\\end{tabular}\n\\end{mitabla}\n\n");

        sb.append("\\textit{Paso 3. Hallazgos por categoría de $X$.} Para cada nivel de programa, ")
          .append("la fuente de financiamiento más frecuente y si su antigüedad promedio queda por ")
          .append("encima o por debajo del promedio general (").append(n(zPromGeneral, 2)).append(" años):\n\n");
        sb.append("\\begin{itemize}\n");
        for (Map<String, Object> h : (List<Map<String, Object>>) m.get("hallazgos")) {
            boolean sobre = (boolean) h.get("sobrePromedio");
            sb.append("  \\item \\textbf{").append(t((String) h.get("x"))).append("} ($n=").append(i(h.get("n"))).append("$): ")
              .append("antigüedad promedio de ").append(n(d(h.get("zProm")), 2)).append(" años, ")
              .append(sobre ? "por encima" : "por debajo").append(" del promedio general. ")
              .append("La fuente de financiamiento más frecuente es ").append(t(((String) h.get("financiamientoFrecuente")).toLowerCase()))
              .append(" (").append(n(100 * d(h.get("pctFinanciamientoFrecuente")), 1)).append("\\% de este grupo).\n");
        }
        sb.append("\\end{itemize}\n\n");
        sb.append("\\textit{Interpretación.} A mayor nivel del programa cursado, mayor la ")
          .append("antigüedad promedio de quienes lo cursaron: quienes ya acumularon más años de ")
          .append("vida profesional son los que llegan a programas de nivel más alto (maestría o ")
          .append("doctorado), mientras que los titulados más recientes se concentran en diplomados. ")
          .append("Los recursos propios dominan el financiamiento en todos los niveles.").append(marca()).append("\n\n");
        return sb.toString();
    }
}
