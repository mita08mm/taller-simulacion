package com.actividad2;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Respuesta del estudio a cada criterio ARCU-SUR: qué pide el criterio, los
 * indicadores que lo responden y la valoración (favorable si la proporción
 * supera el 50%, la misma regla de la sección "Respuesta a los Criterios" del
 * informe). El panel arma una pantalla por criterio a partir de esta lista.
 */
public class Criterios {

    // Ítems de la escala de empleadores que miden la relación de la Carrera con las organizaciones
    private static final List<String> ITEMS_PERFIL_EGRESO = List.of("perfil_coherente_desempeno", "perfil_competencias_laborales",
        "perfil_valores", "perfil_titulo_consistente", "perfil_conoce_perfil");
    static final List<String> ITEMS_RELACION = List.of("perfil_consulta_autoridades", "perfil_opinion_practicas",
        "perfil_participo_retroalimentacion", "perfil_vinculos_oportunidades", "perfil_participo_vinculacion_entorno",
        "perfil_participo_difusion", "perfil_participo_vinculacion_profesionales", "perfil_consultado_desempeno",
        "perfil_consultada_pertinencia", "perfil_recurre_umss", "perfil_consultado_autoevaluacion");

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> evaluar(Dataset titulados, Dataset empleadores) {
        Map<String, Object> emp = Analisis.empleabilidad(titulados);
        Map<String, Object> dem = Analisis.demografia(titulados);
        Map<String, Object> form = Analisis.formacionContinua(titulados);
        Map<String, Object> comp = Analisis.competencias(titulados, empleadores);
        Map<String, Object> empl = Analisis.empleadores(empleadores);
        List<Map<String, Object>> perfil = (List<Map<String, Object>>) empl.get("perfil");
        List<Map<String, Object>> lista = new ArrayList<>();

        // 3.2.3 Condiciones de empleo
        Map<String, Object> tasa = mapa(emp.get("tasaEmpleo"));
        Map<String, Object> tpe = mapa(mapa(emp.get("tiempoPrimerEmpleo")).get("mesesIC95"));
        double pert = acuerdo(mapa(emp.get("pertinenciaPuesto")));
        double conc = acuerdo(mapa(emp.get("concordanciaRequerimientos")));
        Map<String, Object> c = criterio("3.2.3", "Condiciones de empleo", "empleo",
            "Que la calidad de la formación se refleje en las condiciones de empleo de los graduados: tasa de empleo, tiempo para conseguir el primer empleo, destino laboral y concordancia entre titulación y empleo.");
        indicador(c, "Tasa de empleo", pct(d(tasa.get("proporcion"))), tasa.get("casos") + " de " + tasa.get("total") + " titulados trabajan o emprenden");
        indicador(c, "Tiempo medio al primer empleo", num(d(tpe.get("media")), 1) + " meses",
            "Entre " + num(d(tpe.get("limiteInferior")), 1) + " y " + num(d(tpe.get("limiteSuperior")), 1) + " meses con 95% de confianza");
        indicador(c, "Labores pertinentes a la formación", pct(pert), "De quienes trabajan en una organización");
        valorar(c, d(tasa.get("proporcion")), "Titulados con empleo o emprendimiento");
        valorar(c, pert, "Consideran pertinentes a su formación las labores de su puesto");
        valorar(c, conc, "Perciben concordancia entre su formación y lo que pide el mercado");
        c.put("cruces", List.of("C2", "C6"));
        lista.add(c);

        // 3.2.1 Resultados del proceso formativo
        double sat = acuerdo(mapa(comp.get("satisfaccionGlobal")));
        List<Map<String, Object>> mejorar = (List<Map<String, Object>>) comp.get("aspectosMejorar");
        List<Map<String, Object>> utiles = (List<Map<String, Object>>) comp.get("aspectosUtilidad");
        c = criterio("3.2.1", "Resultados del proceso formativo", "resultados",
            "Evaluar el resultado del proceso formativo y usar esa evaluación para hacer ajustes.");
        indicador(c, "Satisfechos con la formación", pct(sat), "De acuerdo o totalmente de acuerdo");
        if (!utiles.isEmpty()) indicador(c, "Lo más útil de la carrera", (String) utiles.get(0).get("etiqueta"), "Lo señala el " + pct(d(utiles.get(0).get("relativa"))));
        if (!mejorar.isEmpty()) indicador(c, "Lo que más piden mejorar", (String) mejorar.get(0).get("etiqueta"), "Lo señala el " + pct(d(mejorar.get(0).get("relativa"))));
        valorar(c, sat, "Titulados satisfechos con la formación recibida");
        c.put("cruces", List.of("C1"));
        lista.add(c);

        // 2.1.2 Perfil de egreso
        c = criterio("2.1.2", "Perfil de egreso", "perfil",
            "Coherencia entre el perfil de egreso y las competencias que demandan los empleadores.");
        indicador(c, "Organizaciones encuestadas", String.valueOf(empleadores.n()), "Públicas y privadas");
        Map<String, Object> coherente = item(perfil, "perfil_coherente_desempeno");
        if (coherente != null) indicador(c, "Perfil coherente con el desempeño", pct(d(coherente.get("porcentajeAcuerdo"))), "Empleadores de acuerdo (total o parcialmente)");
        Map<String, Object> licenciatura = frecuencia((List<Map<String, Object>>) empl.get("nivel_formacion_demandado"), "Licenciatura");
        if (licenciatura != null) indicador(c, "Les basta la licenciatura", pct(d(licenciatura.get("relativa"))), "Nivel de formación que demandan");
        for (Map<String, Object> p : perfil) {
            if (ITEMS_PERFIL_EGRESO.contains(p.get("codigo"))) valorar(c, d(p.get("porcentajeAcuerdo")), (String) p.get("criterio"));
        }
        lista.add(c);

        // 2.1.7 Actualización curricular
        List<Map<String, Object>> ventaja = (List<Map<String, Object>>) comp.get("asignaturasVentaja");
        List<Map<String, Object>> poco = (List<Map<String, Object>>) comp.get("asignaturasPocoUtiles");
        Map<String, Object> actualizar = frecuencia(mejorar, "actualizacion_plan_estudios");
        c = criterio("2.1.7", "Actualización curricular", "curricular",
            "Mecanismos permanentes para actualizar el plan de estudios según la evolución de la profesión.");
        if (!ventaja.isEmpty()) indicador(c, "Asignatura con más ventaja", (String) ventaja.get(0).get("etiqueta"), "La elige el " + pct(d(ventaja.get(0).get("relativa"))));
        if (!poco.isEmpty()) indicador(c, "Asignatura menos útil", (String) poco.get(0).get("etiqueta"), "La elige el " + pct(d(poco.get(0).get("relativa"))));
        if (actualizar != null) indicador(c, "Piden actualizar el plan", pct(d(actualizar.get("relativa"))), "Como aspecto a mejorar");
        c.put("nota", "Este criterio no se valora con un porcentaje: el estudio aporta insumos para la actualización (asignaturas y tecnologías que piden los empleadores).");
        lista.add(c);

        // 2.4.1 y 1.1.5 Actualización profesional y posgrado
        Map<String, Object> fc = mapa(form.get("conFormacionComplementaria"));
        Map<String, Object> ip = mapa(form.get("interesPosgrado"));
        List<Map<String, Object>> org = (List<Map<String, Object>>) form.get("organizacionInteres");
        int umss = org.stream().filter(f -> List.of("EUPG", "Direccion_Posgrado_FCyT").contains(f.get("codigo"))).mapToInt(f -> (Integer) f.get("frecuencia")).sum();
        int totalOrg = org.stream().mapToInt(f -> (Integer) f.get("frecuencia")).sum();
        double propUmss = totalOrg == 0 ? 0 : (double) umss / totalOrg;
        c = criterio("2.4.1", "Actualización profesional y posgrado", "posgrado",
            "Ofrecer a los graduados cursos de actualización y posgrados afines a la carrera, y promover estudios de posgrado (incluye el criterio 1.1.5).");
        indicador(c, "Con formación complementaria", pct(d(fc.get("proporcion"))), fc.get("casos") + " de " + fc.get("total") + " titulados");
        indicador(c, "Interesados en un posgrado", pct(d(ip.get("proporcion"))), ip.get("casos") + " de " + ip.get("total") + " titulados");
        indicador(c, "Lo estudiarían en la UMSS", pct(propUmss), "EUPG o Dirección de Posgrado FCyT");
        valorar(c, d(ip.get("proporcion")), "Titulados interesados en un posgrado");
        valorar(c, acuerdo(mapa(form.get("ofertaPosgradoFcyt"))), "Consideran afín la oferta de posgrado de la FCyT");
        valorar(c, propUmss, "Interesados que elegirían la UMSS");
        c.put("cruces", List.of("C3", "C4", "C5", "C7"));
        lista.add(c);

        // 3.2.2 Seguimiento a graduados
        Map<String, Object> red = mapa(dem.get("redContactos"));
        c = criterio("3.2.2", "Seguimiento a graduados", "seguimiento",
            "Un sistema de seguimiento que permita conocer la situación de los graduados e instancias para que participen en la mejora de la carrera.");
        indicador(c, "Respuestas de titulados", String.valueOf(titulados.n()), periodo(titulados));
        indicador(c, "Respuestas de empleadores", String.valueOf(empleadores.n()), periodo(empleadores));
        indicador(c, "Quieren integrar la red de la Carrera", pct(d(red.get("proporcion"))), red.get("casos") + " de " + red.get("total") + " titulados");
        valorar(c, d(red.get("proporcion")), "Titulados interesados en una red oficial de contactos");
        c.put("respuestasPorMes", porMes(titulados, empleadores));
        lista.add(c);

        // 2.4.2 Relaciones con el sector público y privado
        Map<String, Object> contrato = frecuencia((List<Map<String, Object>>) empl.get("contrato_ultimos_5_anios"), "SI");
        Map<String, Object> alta = frecuencia((List<Map<String, Object>>) empl.get("probabilidad_contratacion"), "alta");
        Map<String, Object> recurre = item(perfil, "perfil_recurre_umss");
        c = criterio("2.4.2", "Relaciones con el sector público y privado", "relaciones",
            "Relaciones con empresas y organizaciones públicas y privadas para cooperar en actividades conjuntas.");
        if (contrato != null) indicador(c, "Contrataron titulados (últimos 5 años)", pct(d(contrato.get("relativa"))), "De las organizaciones encuestadas");
        if (alta != null) indicador(c, "Alta probabilidad de contratar", pct(d(alta.get("relativa"))), "En los próximos años");
        if (recurre != null) indicador(c, "Recurren a la UMSS para contratar", pct(d(recurre.get("porcentajeAcuerdo"))), "Empleadores de acuerdo");
        if (contrato != null) valorar(c, d(contrato.get("relativa")), "Organizaciones que contrataron titulados en los últimos 5 años");
        for (Map<String, Object> p : perfil) {
            if (ITEMS_RELACION.contains(p.get("codigo"))) valorar(c, d(p.get("porcentajeAcuerdo")), (String) p.get("criterio"));
        }
        lista.add(c);
        return lista;
    }

    // ---------------- armado ----------------

    private static Map<String, Object> criterio(String id, String nombre, String pantalla, String quePide) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("id", id);
        c.put("nombre", nombre);
        c.put("pantalla", pantalla);
        c.put("quePide", quePide);
        c.put("indicadores", new ArrayList<Map<String, Object>>());
        c.put("favorables", new ArrayList<Map<String, Object>>());
        c.put("desfavorables", new ArrayList<Map<String, Object>>());
        c.put("cruces", List.of());
        return c;
    }

    @SuppressWarnings("unchecked")
    private static void indicador(Map<String, Object> c, String titulo, String valor, String detalle) {
        ((List<Map<String, Object>>) c.get("indicadores")).add(Map.of("titulo", titulo, "valor", valor, "detalle", detalle));
    }

    @SuppressWarnings("unchecked")
    private static void valorar(Map<String, Object> c, double proporcion, String texto) {
        ((List<Map<String, Object>>) c.get(proporcion > 0.5 ? "favorables" : "desfavorables"))
            .add(Map.of("texto", texto, "proporcion", proporcion));
    }

    /** Proporción "de acuerdo" en las escalas de 4 niveles del formulario de titulados (códigos 3 y 4). */
    @SuppressWarnings("unchecked")
    private static double acuerdo(Map<String, Object> likert) {
        return ((List<Map<String, Object>>) likert.get("frecuencias")).stream()
            .filter(f -> List.of("3", "4").contains(f.get("codigo"))).mapToDouble(f -> d(f.get("relativa"))).sum();
    }

    private static Map<String, Object> item(List<Map<String, Object>> perfil, String codigo) {
        return perfil.stream().filter(p -> codigo.equals(p.get("codigo"))).findFirst().orElse(null);
    }

    private static Map<String, Object> frecuencia(List<Map<String, Object>> lista, String codigo) {
        return lista == null ? null : lista.stream().filter(f -> codigo.equals(f.get("codigo"))).findFirst().orElse(null);
    }

    private static String periodo(Dataset d) {
        LocalDate min = null, max = null;
        for (Map<String, String> f : d.filas(x -> true)) {
            LocalDate x = Dataset.fecha(f);
            if (x == null) continue;
            if (min == null || x.isBefore(min)) min = x;
            if (max == null || x.isAfter(max)) max = x;
        }
        return min == null ? "Sin fecha" : "Del " + min + " al " + max;
    }

    /** Respuestas recibidas por mes (aaaa-mm), para ver si el seguimiento es continuo. */
    private static List<Map<String, Object>> porMes(Dataset titulados, Dataset empleadores) {
        TreeMap<String, int[]> meses = new TreeMap<>();
        for (Map<String, String> f : titulados.filas(x -> true)) {
            LocalDate x = Dataset.fecha(f);
            if (x != null) meses.computeIfAbsent(x.toString().substring(0, 7), k -> new int[2])[0]++;
        }
        for (Map<String, String> f : empleadores.filas(x -> true)) {
            LocalDate x = Dataset.fecha(f);
            if (x != null) meses.computeIfAbsent(x.toString().substring(0, 7), k -> new int[2])[1]++;
        }
        List<Map<String, Object>> r = new ArrayList<>();
        meses.forEach((mes, n) -> r.add(Map.of("mes", mes, "titulados", n[0], "empleadores", n[1])));
        return r;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object o) {
        return (Map<String, Object>) o;
    }

    private static double d(Object o) {
        return o == null ? 0 : ((Number) o).doubleValue();
    }

    private static String pct(double v) {
        return String.format(Locale.US, "%.1f%%", 100 * v);
    }

    private static String num(double v, int dec) {
        return String.format(Locale.US, "%." + dec + "f", v);
    }
}
