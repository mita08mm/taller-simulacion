package com.actividad2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Indicadores (U1..U11) y cruces (C1..C7) de PLAN.md seccion 5, agrupados
 * por Historia de Usuario del enunciado.
 */
public class Analisis {

    private static final Predicate<Map<String, String>> TRABAJA_EN_ORGANIZACION =
        f -> "trabajando".equals(f.get("situacion_laboral"));

    // ---------------- HU1: antigüedad y empleabilidad (ARCU-SUR 3.2.3) ----------------
    public static Map<String, Object> empleabilidad(Dataset d) {
        Map<String, Object> r = new LinkedHashMap<>();

        List<String> situacion = d.valores("situacion_laboral");
        long empleados = situacion.stream().filter(s -> s.equals("trabajando") || s.equals("emprendimiento")).count();
        r.put("tasaEmpleo", Estadistica.proporcion((int) empleados, situacion.size()));
        r.put("situacionLaboral", Estadistica.frecuencias("situacion_laboral", situacion));

        // U2: tiempo medio hasta el primer empleo (punto medio de cada rango, en meses)
        List<String> tiempo = d.valores("tiempo_primer_trabajo");
        List<Double> meses = tiempo.stream().map(Catalogo.MESES_PRIMER_TRABAJO::get).filter(v -> v != null).toList();
        r.put("tiempoPrimerEmpleo", Map.of(
            "frecuencias", Estadistica.frecuencias("tiempo_primer_trabajo", tiempo),
            "mesesIC95", Estadistica.intervaloConfianza95(meses),
            "nota", "Solo responde la rama 'trabaja en una organización'. '> 12 meses' se toma como 12 (cota inferior)."));

        // U3: años de vida profesional y desempleado; se descartan filas inconsistentes
        List<Double> vida = new ArrayList<>();
        List<Double> desempleo = new ArrayList<>();
        int inconsistentes = 0;
        for (Map<String, String> f : d.filas) {
            String v = d.valor(f, "anios_vida_profesional");
            String u = d.valor(f, "anios_desempleado");
            if (v.isEmpty() || u.isEmpty()) continue;
            double vv = Double.parseDouble(v);
            double uu = Double.parseDouble(u);
            if (uu > vv) { inconsistentes++; continue; }
            vida.add(vv);
            desempleo.add(uu);
        }
        r.put("aniosVidaProfesional", Estadistica.intervaloConfianza95(vida));
        r.put("aniosDesempleado", Estadistica.intervaloConfianza95(desempleo));
        r.put("filasInconsistentesDescartadas", inconsistentes);

        // U4: destino laboral (solo rama "trabaja en una organización")
        Map<String, Object> destino = new LinkedHashMap<>();
        for (String col : List.of("rubro", "sector_publico_privado", "departamento_bolivia", "area_organizacion", "cargo", "remuneracion_promedio", "antiguedad_trabajo", "medio_obtuvo_trabajo")) {
            destino.put(col, Estadistica.frecuencias(col, d.valores(col, TRABAJA_EN_ORGANIZACION)));
        }
        r.put("destinoLaboral", destino);

        // U5: concordancia titulación–empleo
        r.put("pertinenciaPuesto", Estadistica.likert("likert_pertinencia_puesto", d.valores("likert_pertinencia_puesto", TRABAJA_EN_ORGANIZACION)));
        r.put("concordanciaRequerimientos", Estadistica.likert("likert_concordancia_requerimientos", d.valores("likert_concordancia_requerimientos")));

        // T5: "primer empleo" no aplica a quien hoy no trabaja
        r.put("primerEmpleo", Estadistica.frecuencias("primer_empleo", d.valores("primer_empleo", f -> !"no_trabajo".equals(f.get("situacion_laboral")))));
        r.put("cantidadEmpleos", Estadistica.frecuencias("cantidad_empleos", d.valores("cantidad_empleos")));

        Map<String, Object> emprendimiento = new LinkedHashMap<>();
        for (String col : List.of("origen_emprendimiento", "tipo_entregable", "financiamiento_externo_negocio", "likert_satisfaccion_negocio", "likert_importancia_formacion_negocio")) {
            emprendimiento.put(col, Estadistica.frecuencias(col, d.valores(col)));
        }
        r.put("emprendimiento", emprendimiento);
        r.put("razonNoTrabaja", Estadistica.frecuencias("razon_no_trabaja", d.valores("razon_no_trabaja")));
        return r;
    }

    // ---------------- HU2: formación continua y áreas de interés (2.4.1 / 1.1.5) ----------------
    public static Map<String, Object> formacionContinua(Dataset d) {
        Map<String, Object> r = new LinkedHashMap<>();
        List<String> fc = d.valores("formacion_complementaria");
        r.put("conFormacionComplementaria", Estadistica.proporcion((int) fc.stream().filter("SI"::equals).count(), fc.size()));
        r.put("programaNivel", Estadistica.frecuencias("programa_nivel", d.valores("programa_nivel")));
        r.put("programaDonde", Estadistica.frecuencias("programa_donde", d.valores("programa_donde")));
        List<String> interes = d.valores("interes_posgrado");
        r.put("interesPosgrado", Estadistica.proporcion((int) interes.stream().filter("SI"::equals).count(), interes.size()));
        r.put("areaInteres", Estadistica.frecuencias("area_interes_posgrado", d.valores("area_interes_posgrado")));
        r.put("organizacionInteres", Estadistica.frecuencias("organizacion_interes_posgrado", d.valores("organizacion_interes_posgrado")));
        r.put("ofertaPosgradoFcyt", Estadistica.likert("likert_oferta_posgrado_fcyt", d.valores("likert_oferta_posgrado_fcyt")));
        return r;
    }

    // ---------------- HU3: mecanismos de financiamiento ----------------
    public static Map<String, Object> financiamiento(Dataset d) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("programaFinanciamiento", Estadistica.frecuencias("programa_financiamiento", d.valores("programa_financiamiento")));
        r.put("financiamientoNegocio", Estadistica.frecuencias("financiamiento_externo_negocio", d.valores("financiamiento_externo_negocio")));
        r.put("nota", "Solo responden quienes tienen formación complementaria (financiamiento del programa ya cursado).");
        return r;
    }

    // ---------------- HU4: brechas en competencias (3.2.1 / 2.1.2 / 2.1.7) ----------------
    public static Map<String, Object> competencias(Dataset titulados, Dataset empleadores) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("satisfaccionGlobal", Estadistica.likert("likert_satisfaccion_global_formacion", titulados.valores("likert_satisfaccion_global_formacion")));
        r.put("concordancia", Estadistica.likert("likert_concordancia_requerimientos", titulados.valores("likert_concordancia_requerimientos")));
        r.put("aspectosUtilidad", ordenarDesc(Estadistica.frecuencias("aspectos_utilidad", titulados.valoresMultiples("aspectos_utilidad")), titulados.n()));
        r.put("aspectosMejorar", ordenarDesc(Estadistica.frecuencias("aspectos_mejorar", titulados.valoresMultiples("aspectos_mejorar")), titulados.n()));
        r.put("asignaturasVentaja", ordenarDesc(Estadistica.frecuencias("asignaturas_ventaja", titulados.valoresMultiples("asignaturas_ventaja")), titulados.n()));
        r.put("asignaturasPocoUtiles", ordenarDesc(Estadistica.frecuencias("asignaturas_poco_utiles", titulados.valoresMultiples("asignaturas_poco_utiles")), titulados.n()));
        r.put("perfilEmpleadores", perfilEmpleadores(empleadores));
        return r;
    }

    // En elección múltiple la relativa es sobre el número de personas, no de menciones.
    private static List<Map<String, Object>> ordenarDesc(List<Map<String, Object>> frec, int personas) {
        List<Map<String, Object>> copia = new ArrayList<>(frec);
        for (Map<String, Object> f : copia) f.put("relativa", personas == 0 ? 0.0 : ((Integer) f.get("frecuencia")) / (double) personas);
        copia.sort((a, b) -> Integer.compare((Integer) b.get("frecuencia"), (Integer) a.get("frecuencia")));
        return copia;
    }

    /** U11: % de cada opción por criterio; sin media porque la escala no es simétrica (PLAN.md 5.6, E1). */
    public static List<Map<String, Object>> perfilEmpleadores(Dataset d) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Map.Entry<String, String> item : Catalogo.ITEMS_PERFIL.entrySet()) {
            List<String> valores = d.valores(item.getKey());
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigo", item.getKey());
            fila.put("criterio", item.getValue());
            fila.put("n", valores.size());
            fila.put("frecuencias", Estadistica.frecuencias(item.getKey(), valores));
            long acuerdo = valores.stream().filter(v -> v.endsWith("de_acuerdo")).count();
            fila.put("porcentajeAcuerdo", valores.isEmpty() ? 0.0 : (double) acuerdo / valores.size());
            lista.add(fila);
        }
        return lista;
    }

    // ---------------- Caracterización demográfica ----------------
    public static Map<String, Object> demografia(Dataset d) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("edad", Estadistica.frecuencias("edad", d.valores("edad")));
        r.put("genero", Estadistica.frecuencias("genero", d.valores("genero")));
        List<String> red = d.valores("red_contactos_interes");
        r.put("redContactos", Estadistica.proporcion((int) red.stream().filter("SI"::equals).count(), red.size()));
        return r;
    }

    // ---------------- HU5: matrices de frecuencia cruzada (C1..C7) ----------------
    public record Cruce(String id, String titulo, String proposito, String criterio,
                        String colA, String etiquetaA, Function<String, String> grupoA, List<String> ordenA,
                        String colB, String etiquetaB, Function<String, String> grupoB, List<String> ordenB,
                        Predicate<Map<String, String>> filtro, String poblacion) {}

    private static final List<String> LIKERT_GRUPOS = List.of("En desacuerdo", "De acuerdo");

    public static final List<Cruce> CRUCES = List.of(
        new Cruce("C1", "Situación laboral × satisfacción con la formación",
            "Determinar si la valoración de la formación recibida cambia según la situación laboral actual del titulado, como evidencia del resultado del proceso formativo frente a las condiciones de empleo.", "3.2.1 / 3.2.3",
            "situacion_laboral", "Situación laboral", c -> Catalogo.etiqueta("situacion_laboral", c),
            List.of("Trabaja en una organización", "Emprendimiento propio", "No trabaja"),
            "likert_satisfaccion_global_formacion", "Satisfacción", Catalogo::likertAgrupado, LIKERT_GRUPOS,
            f -> true, "Todos los titulados"),
        new Cruce("C2", "Sector × pertinencia del puesto",
            "Determinar si la concordancia entre la titulación y el empleo depende del sector, público o privado, en que trabaja el titulado.", "3.2.3",
            "sector_publico_privado", "Sector", c -> Catalogo.etiqueta("sector_publico_privado", c), List.of("Público", "Privado"),
            "likert_pertinencia_puesto", "Pertinencia del puesto", Catalogo::likertAgrupado, LIKERT_GRUPOS,
            TRABAJA_EN_ORGANIZACION, "Trabajan en una organización"),
        new Cruce("C3", "Formación complementaria × remuneración",
            "Determinar si la formación complementaria se asocia con el nivel de remuneración, como sustento para la oferta de actualización profesional y posgrado.", "2.4.1 / 3.2.3",
            "formacion_complementaria", "Formación complementaria", c -> Catalogo.etiqueta("formacion_complementaria", c), List.of("Sí", "No"),
            "remuneracion_promedio", "Remuneración", Catalogo::remuneracionAgrupada, List.of("Hasta Bs 5.000", "Más de Bs 5.000"),
            TRABAJA_EN_ORGANIZACION, "Trabajan en una organización"),
        new Cruce("C4", "Interés en posgrado × oferta de posgrado FCyT",
            "Determinar si la percepción de que la oferta de posgrado de la FCyT es afín a la carrera depende del interés en realizar un posgrado.", "2.4.1 / 1.1.5",
            "interes_posgrado", "Interés en posgrado", c -> Catalogo.etiqueta("interes_posgrado", c), List.of("Sí", "No"),
            "likert_oferta_posgrado_fcyt", "Oferta FCyT afín", Catalogo::likertAgrupado, LIKERT_GRUPOS,
            f -> true, "Todos los titulados"),
        new Cruce("C5", "Área de interés × institución elegida",
            "Determinar si la preferencia por estudiar el posgrado en la UMSS depende del área de interés, para identificar áreas en que la oferta propia no capta la demanda.", "2.4.1 / 1.1.5",
            "area_interes_posgrado", "Área de interés", c -> Catalogo.etiqueta("area_interes_posgrado", c),
            List.of("IA", "Robótica", "Ing. de Software", "Base de Datos", "Redes y Seguridad"),
            "organizacion_interes_posgrado", "Institución", Catalogo::organizacionAgrupada, List.of("UMSS (EUPG o FCyT)", "Otra institución"),
            f -> "SI".equals(f.get("interes_posgrado")), "Interesados en posgrado"),
        new Cruce("C6", "Concordancia formación–mercado × tiempo al primer empleo",
            "Determinar si percibir la formación alineada con los requerimientos del mercado se asocia con un menor tiempo para obtener el primer empleo.", "3.2.3",
            "likert_concordancia_requerimientos", "Concordancia", Catalogo::likertAgrupado, LIKERT_GRUPOS,
            "tiempo_primer_trabajo", "Tiempo al primer empleo", Catalogo::tiempoAgrupado, List.of("Hasta 4 meses", "Más de 4 meses"),
            TRABAJA_EN_ORGANIZACION, "Trabajan en una organización"),
        new Cruce("C7", "Financiamiento × nivel del programa cursado (opcional)",
            "Determinar si la fuente de financiamiento depende del nivel del programa cursado; complementa la historia de usuario de financiamiento.", "sin indicador ARCU-SUR directo",
            "programa_financiamiento", "Financiamiento", c -> Catalogo.etiqueta("programa_financiamiento", c),
            List.of("Recursos propios", "Beca total", "Beca parcial"),
            "programa_nivel", "Nivel cursado", Catalogo::nivelAgrupado, List.of("Diplomado o Especialidad", "Maestría o superior"),
            f -> "SI".equals(f.get("formacion_complementaria")), "Con formación complementaria")
    );

    public static Map<String, Object> cruce(Dataset d, Cruce c) {
        List<String[]> pares = new ArrayList<>();
        for (Map<String, String> f : d.filas(c.filtro())) {
            String a = c.grupoA().apply(d.valor(f, c.colA()));
            String b = c.grupoB().apply(d.valor(f, c.colB()));
            if (!a.isEmpty() && !b.isEmpty() && !d.valor(f, c.colA()).isEmpty() && !d.valor(f, c.colB()).isEmpty()) {
                pares.add(new String[]{a, b});
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", c.id());
        r.put("titulo", c.titulo());
        r.put("proposito", c.proposito());
        r.put("criterio", c.criterio());
        r.put("poblacion", c.poblacion());
        r.put("variableFilas", c.etiquetaA());
        r.put("variableColumnas", c.etiquetaB());
        r.putAll(Estadistica.chiCuadrada(pares, c.ordenA(), c.ordenB()));
        return r;
    }

    public static List<Map<String, Object>> cruces(Dataset d) {
        return CRUCES.stream().map(c -> cruce(d, c)).toList();
    }

    // ---------------- Empleadores (solo descriptivo, PLAN.md 5.3) ----------------
    public static Map<String, Object> empleadores(Dataset d) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("n", d.n());
        List<Map<String, Object>> empresas = new ArrayList<>();
        for (Map<String, String> f : d.filas) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("nombre", d.valor(f, "nombre_empresa"));
            e.put("tipo", Catalogo.etiqueta("tipo_organizacion", d.valor(f, "tipo_organizacion")));
            e.put("tamano", Catalogo.etiqueta("tamano", d.valor(f, "tamano")));
            empresas.add(e);
        }
        r.put("empresas", empresas);
        for (String col : List.of("tipo_organizacion", "tamano", "rubro", "contrato_ultimos_5_anios", "probabilidad_contratacion", "nivel_formacion_demandado", "medio_convocatoria")) {
            r.put(col, Estadistica.frecuencias(col, d.valores(col)));
        }
        r.put("cargos", ordenarDesc(Estadistica.frecuencias("cargos", d.valoresMultiples("cargos")), d.n()));
        r.put("perfil", perfilEmpleadores(d));

        Map<String, Object> abiertas = new LinkedHashMap<>();
        for (String col : List.of("areas_emergentes", "herramientas", "habilidades")) {
            List<String> menciones = new ArrayList<>();
            for (String v : d.valores(col)) for (String p : v.split(";")) if (!p.isBlank()) menciones.add(p.trim());
            abiertas.put(col, ordenarDesc(Estadistica.frecuencias(col, menciones), d.n()));
        }
        r.put("preguntasAbiertas", abiertas);
        return r;
    }

    /** Comparación de cargos titulados vs empleadores con la tabla de homologación (PLAN.md 5.6, T9). */
    public static Map<String, Object> cargosHomologados(Dataset titulados, Dataset empleadores) {
        List<String> grupos = List.of("Soporte", "Técnico / Desarrollo", "Analista / Especialista", "Gestión / Jefatura", "Otro");
        Map<String, Integer> t = new LinkedHashMap<>();
        Map<String, Integer> e = new LinkedHashMap<>();
        grupos.forEach(g -> { t.put(g, 0); e.put(g, 0); });
        titulados.valores("cargo", TRABAJA_EN_ORGANIZACION).forEach(c -> t.merge(Catalogo.cargoHomologado(c), 1, Integer::sum));
        empleadores.valoresMultiples("cargos").forEach(c -> e.merge(Catalogo.cargoHomologado(c), 1, Integer::sum));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("grupos", grupos);
        r.put("titulados", new ArrayList<>(t.values()));
        r.put("empleadores", new ArrayList<>(e.values()));
        return r;
    }
}
