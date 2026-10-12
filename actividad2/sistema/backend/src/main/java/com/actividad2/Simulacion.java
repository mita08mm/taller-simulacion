package com.actividad2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulacion de cohortes de titulados para los dos indicadores literales de
 * ARCU-SUR 3.2.3: tasa de empleo y tiempo medio hasta el primer empleo.
 *
 * - Numeros aleatorios: generador congruencial mixto (apuntes, Cap. 2).
 * - Variables: Transformada Inversa discreta sobre las frecuencias observadas
 *   en la encuesta (mismo procedimiento que p3/p4 de Actividad 1).
 * - Numero de corridas e IC del resultado: referencias/cap6 (6.1).
 */
public class Simulacion {

    /**
     * x_{n+1} = (a*x_n + c) mod m, con m = 2^32, a = 1664525, c = 1013904223.
     * Cumple las tres propiedades de periodo completo de los apuntes:
     * i) c es impar -> primo relativo con m = 2^32;
     * ii) el unico primo que divide a m es 2 y a-1 = 1664524 es par;
     * iii) m es multiplo de 4 y a-1 = 4 * 416131 tambien.
     */
    public static class Gcm {
        private static final long A = 1664525L;
        private static final long C = 1013904223L;
        private static final long M = 1L << 32;
        private long x;

        public Gcm(long semilla) {
            this.x = Math.floorMod(semilla, M);
        }

        public double siguiente() {
            x = (A * x + C) % M;
            return (double) x / M;
        }
    }

    /** Tabla de Transformada Inversa discreta: p(x), F(x) y el rango de R que le corresponde. */
    public static class TablaTI {
        final List<String> codigos = new ArrayList<>();
        final List<String> etiquetas = new ArrayList<>();
        final List<Double> probabilidades = new ArrayList<>();
        final List<Double> acumuladas = new ArrayList<>();

        TablaTI(String columna, Map<String, Double> probabilidad) {
            double acum = 0;
            for (Map.Entry<String, Double> e : probabilidad.entrySet()) {
                if (e.getValue() <= 0) continue;
                acum += e.getValue();
                codigos.add(e.getKey());
                etiquetas.add(Catalogo.etiqueta(columna, e.getKey()));
                probabilidades.add(e.getValue());
                acumuladas.add(acum);
            }
            acumuladas.set(acumuladas.size() - 1, 1.0); // evita errores de redondeo
        }

        /** x = F^{-1}(R): primera categoria cuya acumulada supera a R. */
        String generar(double r) {
            for (int i = 0; i < acumuladas.size(); i++) {
                if (r < acumuladas.get(i)) return codigos.get(i);
            }
            return codigos.get(codigos.size() - 1);
        }

        List<Map<String, Object>> comoLista() {
            List<Map<String, Object>> filas = new ArrayList<>();
            double anterior = 0;
            for (int i = 0; i < codigos.size(); i++) {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("codigo", codigos.get(i));
                f.put("etiqueta", etiquetas.get(i));
                f.put("probabilidad", probabilidades.get(i));
                f.put("acumulada", acumuladas.get(i));
                f.put("rangoDesde", anterior);
                f.put("rangoHasta", acumuladas.get(i));
                anterior = acumuladas.get(i);
                filas.add(f);
            }
            return filas;
        }
    }

    public record Parametros(int corridas, int tamanoCohorte, long semilla, double ajusteEmpleo, double ajusteTiempo) {}

    static Map<String, Double> probabilidadesObservadas(List<String> valores, Iterable<String> orden) {
        Map<String, Double> p = new LinkedHashMap<>();
        for (String c : orden) p.put(c, 0.0);
        for (String v : valores) p.merge(v, 1.0, Double::sum);
        p.replaceAll((k, v) -> v / valores.size());
        return p;
    }

    /** Escenario: mueve una fraccion de "no trabaja" a "trabaja" y de los tiempos > 4 meses a "1–4 meses". */
    static Map<String, Double> aplicarAjuste(Map<String, Double> base, List<String> desde, String hacia, double fraccion) {
        Map<String, Double> p = new LinkedHashMap<>(base);
        double movido = 0;
        for (String d : desde) {
            double quitar = p.get(d) * fraccion;
            p.put(d, p.get(d) - quitar);
            movido += quitar;
        }
        p.put(hacia, p.get(hacia) + movido);
        return p;
    }

    public static Map<String, Object> ejecutar(Dataset d, Parametros par) {
        if (d.valores("situacion_laboral").isEmpty() || d.valores("tiempo_primer_trabajo").isEmpty()) {
            throw new IllegalArgumentException("No hay respuestas suficientes en el periodo elegido para simular");
        }
        Map<String, Double> pSituacion = probabilidadesObservadas(d.valores("situacion_laboral"), Catalogo.etiquetas("situacion_laboral").keySet());
        Map<String, Double> pTiempo = probabilidadesObservadas(d.valores("tiempo_primer_trabajo"), Catalogo.MESES_PRIMER_TRABAJO.keySet());

        TablaTI situacionBase = new TablaTI("situacion_laboral", pSituacion);
        TablaTI tiempoBase = new TablaTI("tiempo_primer_trabajo", pTiempo);
        TablaTI situacionEsc = new TablaTI("situacion_laboral",
            aplicarAjuste(pSituacion, List.of("no_trabajo"), "trabajando", par.ajusteEmpleo()));
        TablaTI tiempoEsc = new TablaTI("tiempo_primer_trabajo",
            aplicarAjuste(pTiempo, List.of("4_8_meses", "8_12_meses", "mas_12_meses"), "1_4_meses", par.ajusteTiempo()));

        // Mismos numeros aleatorios para base y escenario, para que la diferencia se deba solo al ajuste.
        Map<String, Object> base = correr(situacionBase, tiempoBase, par, true);
        Map<String, Object> escenario = correr(situacionEsc, tiempoEsc, par, false);

        Map<String, Object> tablas = new LinkedHashMap<>();
        tablas.put("situacionBase", situacionBase.comoLista());
        tablas.put("tiempoBase", tiempoBase.comoLista());
        tablas.put("situacionEscenario", situacionEsc.comoLista());
        tablas.put("tiempoEscenario", tiempoEsc.comoLista());

        Map<String, Object> r = new LinkedHashMap<>();
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("corridas", par.corridas());
        p.put("tamanoCohorte", par.tamanoCohorte());
        p.put("semilla", par.semilla());
        p.put("ajusteEmpleo", par.ajusteEmpleo());
        p.put("ajusteTiempo", par.ajusteTiempo());
        r.put("parametros", p);
        r.put("generador", "x(n+1) = (1664525·x(n) + 1013904223) mod 2^32 ; R = x(n+1) / 2^32");
        r.put("tablasTI", tablas);
        r.put("base", base);
        r.put("escenario", escenario);
        return r;
    }

    private static Map<String, Object> correr(TablaTI situacion, TablaTI tiempo, Parametros par, boolean detalle) {
        Gcm gcm = new Gcm(par.semilla());
        List<Double> tasas = new ArrayList<>();
        List<Double> tiempos = new ArrayList<>();
        List<Map<String, Object>> porCorrida = new ArrayList<>();
        List<Map<String, Object>> primeraCohorte = new ArrayList<>();

        for (int corrida = 1; corrida <= par.corridas(); corrida++) {
            int empleados = 0;
            int enOrganizacion = 0;
            double sumaMeses = 0;
            for (int t = 1; t <= par.tamanoCohorte(); t++) {
                double r1 = gcm.siguiente();
                double r2 = gcm.siguiente(); // se genera siempre: base y escenario consumen la misma secuencia
                String s = situacion.generar(r1);
                String tiempoCat = "";
                Double meses = null;
                if (!s.equals("no_trabajo")) empleados++;
                if (s.equals("trabajando")) {
                    tiempoCat = tiempo.generar(r2);
                    meses = Catalogo.MESES_PRIMER_TRABAJO.get(tiempoCat);
                    enOrganizacion++;
                    sumaMeses += meses;
                }
                if (detalle && corrida == 1 && t <= 10) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("titulado", t);
                    fila.put("r1", r1);
                    fila.put("situacion", Catalogo.etiqueta("situacion_laboral", s));
                    fila.put("r2", s.equals("trabajando") ? r2 : null);
                    fila.put("tiempo", tiempoCat.isEmpty() ? "—" : Catalogo.etiqueta("tiempo_primer_trabajo", tiempoCat));
                    fila.put("meses", meses);
                    primeraCohorte.add(fila);
                }
            }
            double tasa = (double) empleados / par.tamanoCohorte();
            double tiempoMedio = enOrganizacion == 0 ? 0 : sumaMeses / enOrganizacion;
            tasas.add(tasa);
            tiempos.add(tiempoMedio);
            if (corrida <= 25) {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("corrida", corrida);
                fila.put("empleados", empleados);
                fila.put("tasaEmpleo", tasa);
                fila.put("enOrganizacion", enOrganizacion);
                fila.put("tiempoMedio", tiempoMedio);
                porCorrida.add(fila);
            }
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("tasaEmpleo", Estadistica.intervaloConfianza95(tasas));
        r.put("tiempoMedio", Estadistica.intervaloConfianza95(tiempos));
        r.put("histogramaTasa", histograma(tasas, 0, 1, 20));
        r.put("histogramaTiempo", histograma(tiempos, 0, 12, 24));
        r.put("corridas", porCorrida);
        if (detalle) r.put("primeraCohorte", primeraCohorte);
        return r;
    }

    private static Map<String, Object> histograma(List<Double> datos, double min, double max, int clases) {
        int[] conteo = new int[clases];
        double ancho = (max - min) / clases;
        for (double v : datos) {
            int i = (int) Math.floor((v - min) / ancho);
            conteo[Math.max(0, Math.min(clases - 1, i))]++;
        }
        List<Double> marcas = new ArrayList<>();
        List<Integer> frecuencias = new ArrayList<>();
        for (int i = 0; i < clases; i++) {
            marcas.add(min + ancho * (i + 0.5));
            frecuencias.add(conteo[i]);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("marcas", marcas);
        r.put("frecuencias", frecuencias);
        return r;
    }
}
