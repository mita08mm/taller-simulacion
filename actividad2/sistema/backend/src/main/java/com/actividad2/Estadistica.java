package com.actividad2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formulas de referencias/cap6_analisis_resultados.tex (6.1, intervalo de
 * confianza) y de la prueba de las frecuencias (chi-cuadrado) de
 * referencias/apuntes_simulacion.tex, extendida a tabla de contingencia
 * (FE_ij = fila * columna / N -- generalizacion estandar, no textual en
 * las referencias, pendiente de confirmar con el docente).
 */
public class Estadistica {

    // t_{n-1, 0.025} (IC 95%), gl 1..30. Para gl > 30 se usa z = 1.96.
    private static final double[] T_95 = {
        0, 12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262,
        2.228, 2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093,
        2.086, 2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045,
        2.042
    };

    // X^2_{0.05, gl} de tabla, gl 1..30.
    private static final double[] CHI_05 = {
        0, 3.841, 5.991, 7.815, 9.488, 11.070, 12.592, 14.067, 15.507, 16.919,
        18.307, 19.675, 21.026, 22.362, 23.685, 24.996, 26.296, 27.587, 28.869, 30.144,
        31.410, 32.671, 33.924, 35.172, 36.415, 37.652, 38.885, 40.113, 41.337, 42.557,
        43.773
    };

    public static double media(List<Double> datos) {
        double suma = 0;
        for (double x : datos) suma += x;
        return suma / datos.size();
    }

    // S^2 = (1/(n-1)) * sum (Xi - xbar)^2 -- cap6, ecuacion (6.5)
    public static double desviacionEstandar(List<Double> datos) {
        if (datos.size() < 2) return 0;
        double xbar = media(datos);
        double suma = 0;
        for (double x : datos) suma += (x - xbar) * (x - xbar);
        return Math.sqrt(suma / (datos.size() - 1));
    }

    /** Mediana: el valor del centro de los datos ordenados (promedio de los dos centrales si n es par). */
    public static double mediana(List<Double> datos) {
        if (datos.isEmpty()) return Double.NaN;
        List<Double> orden = datos.stream().sorted().toList();
        int n = orden.size();
        return n % 2 == 1 ? orden.get(n / 2) : (orden.get(n / 2 - 1) + orden.get(n / 2)) / 2;
    }

    public static double tValor95(int gl) {
        return (gl >= 1 && gl <= 30) ? T_95[gl] : 1.960;
    }

    public static double chiTeorico05(int gl) {
        return (gl >= 1 && gl <= 30) ? CHI_05[gl] : Double.NaN;
    }

    // xbar +- (S/raiz(n)) * t_{n-1, alpha/2} -- cap6, despues de (6.6)
    public static Map<String, Object> intervaloConfianza95(List<Double> datos) {
        Map<String, Object> r = new LinkedHashMap<>();
        int n = datos.size();
        r.put("n", n);
        if (n < 2) {
            r.put("media", n == 1 ? datos.get(0) : null);
            return r;
        }
        double xbar = media(datos);
        double s = desviacionEstandar(datos);
        double t = tValor95(n - 1);
        double margen = (s / Math.sqrt(n)) * t;
        r.put("media", xbar);
        r.put("desviacion", s);
        r.put("t", t);
        r.put("margenError", margen);
        r.put("limiteInferior", xbar - margen);
        r.put("limiteSuperior", xbar + margen);
        return r;
    }

    /** Frecuencia absoluta y relativa, en el orden del formulario (columna de Catalogo). */
    public static List<Map<String, Object>> frecuencias(String columna, List<String> valores) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        Catalogo.etiquetas(columna).keySet().forEach(c -> conteo.put(c, 0));
        for (String v : valores) conteo.merge(v, 1, Integer::sum);

        int total = valores.size();
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Map.Entry<String, Integer> e : conteo.entrySet()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", e.getKey());
            fila.put("etiqueta", Catalogo.etiqueta(columna, e.getKey()));
            fila.put("frecuencia", e.getValue());
            fila.put("relativa", total == 0 ? 0.0 : (double) e.getValue() / total);
            lista.add(fila);
        }
        return lista;
    }

    /** Frecuencias de una escala Likert 1..4 mas su media e IC (PLAN.md 5.6, T7/T14). */
    public static Map<String, Object> likert(String columna, List<String> valores) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("n", valores.size());
        r.put("frecuencias", frecuencias(columna, valores));
        List<Double> numeros = valores.stream().map(Double::parseDouble).toList();
        r.put("media", numeros.isEmpty() ? null : intervaloConfianza95(numeros));
        return r;
    }

    public static Map<String, Object> proporcion(int casos, int total) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("casos", casos);
        r.put("total", total);
        r.put("proporcion", total == 0 ? 0.0 : (double) casos / total);
        return r;
    }

    /**
     * Prueba de independencia sobre una tabla de contingencia.
     * Recibe pares (categoria A, categoria B) ya agrupados; las categorias se
     * ordenan segun las listas dadas y se descartan filas/columnas sin datos.
     */
    public static Map<String, Object> chiCuadrada(List<String[]> pares, List<String> ordenA, List<String> ordenB) {
        List<String> catsA = new ArrayList<>();
        List<String> catsB = new ArrayList<>();
        for (String a : ordenA) if (pares.stream().anyMatch(p -> p[0].equals(a))) catsA.add(a);
        for (String b : ordenB) if (pares.stream().anyMatch(p -> p[1].equals(b))) catsB.add(b);

        int k = catsA.size();
        int m = catsB.size();
        if (pares.isEmpty() || k < 2 || m < 2) {
            // Sin datos suficientes (por ejemplo, un periodo sin respuestas): no se aplica la prueba
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("n", pares.size());
            r.put("filas", catsA);
            r.put("columnas", catsB);
            r.put("observada", new int[0][0]);
            r.put("esperada", new double[0][0]);
            r.put("totalFila", List.of());
            r.put("totalColumna", List.of());
            r.put("chiCuadrada", null);
            r.put("gradosLibertad", 0);
            r.put("alfa", 0.05);
            r.put("chiTeorico", null);
            r.put("independientes", null);
            r.put("celdasFeMenor5", 0);
            r.put("totalCeldas", 0);
            return r;
        }
        int[][] fo = new int[k][m];
        for (String[] p : pares) fo[catsA.indexOf(p[0])][catsB.indexOf(p[1])]++;

        int n = pares.size();
        int[] totalFila = new int[k];
        int[] totalColumna = new int[m];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < m; j++) {
                totalFila[i] += fo[i][j];
                totalColumna[j] += fo[i][j];
            }
        }

        // FE_ij = (total fila i)(total columna j) / N ; X0^2 = sum (FO-FE)^2 / FE
        double[][] fe = new double[k][m];
        double x0 = 0;
        int celdasFeMenor5 = 0;
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < m; j++) {
                fe[i][j] = (double) totalFila[i] * totalColumna[j] / n;
                if (fe[i][j] < 5) celdasFeMenor5++;
                double d = fo[i][j] - fe[i][j];
                x0 += d * d / fe[i][j];
            }
        }

        int gl = (k - 1) * (m - 1);
        double teorico = chiTeorico05(gl);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("n", n);
        r.put("filas", catsA);
        r.put("columnas", catsB);
        r.put("observada", fo);
        r.put("esperada", fe);
        r.put("totalFila", toList(totalFila));
        r.put("totalColumna", toList(totalColumna));
        r.put("chiCuadrada", gl > 0 ? x0 : null);
        r.put("gradosLibertad", gl);
        r.put("alfa", 0.05);
        r.put("chiTeorico", gl > 0 ? teorico : null);
        r.put("independientes", gl > 0 ? x0 < teorico : null);
        r.put("celdasFeMenor5", celdasFeMenor5);
        r.put("totalCeldas", k * m);
        return r;
    }

    private static List<Integer> toList(int[] arreglo) {
        List<Integer> lista = new ArrayList<>();
        for (int v : arreglo) lista.add(v);
        return lista;
    }
}
