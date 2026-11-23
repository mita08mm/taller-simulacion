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

        // HU1: mediana de los años de vida profesional y segmentación junior / consolidados.
        // El corte es la propia mediana (la mitad con menos experiencia frente a la otra mitad), no un umbral fijado a mano.
        double mediana = Estadistica.mediana(vida);
        r.put("medianaVidaProfesional", vida.isEmpty() ? null : mediana);
        List<Map<String, Object>> segmentos = new ArrayList<>();
        if (!vida.isEmpty()) {
            for (boolean junior : new boolean[]{true, false}) {
                int casos = 0, conCargo = 0, ejecutivos = 0;
                for (Map<String, String> f : d.filas) {
                    // mismas filas que la media y la mediana: con ambos datos y sin incoherencias
                    String v = d.valor(f, "anios_vida_profesional");
                    String u = d.valor(f, "anios_desempleado");
                    if (v.isEmpty() || u.isEmpty() || Double.parseDouble(u) > Double.parseDouble(v)) continue;
                    double x = Double.parseDouble(v);
                    if ((x <= mediana) != junior) continue;
                    casos++;
                    String cargo = d.valor(f, "cargo");
                    if (!cargo.isEmpty()) { conCargo++; if ("Ejecutivo".equals(cargo)) ejecutivos++; }
                }
                Map<String, Object> seg = new LinkedHashMap<>();
                seg.put("segmento", junior ? "Junior" : "Consolidado");
                seg.put("criterio", (junior ? "Hasta " : "Más de ") + (mediana == Math.rint(mediana) ? String.valueOf((long) mediana) : String.valueOf(mediana)) + " años");
                seg.put("casos", casos);
                seg.put("conCargo", conCargo);
                seg.put("ejecutivos", ejecutivos);
                segmentos.add(seg);
            }
        }
        r.put("segmentosExperiencia", segmentos);

        // U4: destino laboral (solo rama "trabaja en una organización")
        Map<String, Object> destino = new LinkedHashMap<>();
        for (String col : List.of("rubro", "sector_publico_privado", "departamento_bolivia", "cargo", "remuneracion_promedio", "antiguedad_trabajo", "medio_obtuvo_trabajo")) {
            destino.put(col, Estadistica.frecuencias(col, d.valores(col, TRABAJA_EN_ORGANIZACION)));
        }
        // el área es de elección múltiple: cada persona puede aportar más de una área
        destino.put("area_organizacion", Estadistica.frecuencias("area_organizacion", d.valoresMultiples("area_organizacion", TRABAJA_EN_ORGANIZACION)));
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

    // ---------------- HU5: tablas de contingencia (cruces del estudio) ----------------
    // Cada cruce sigue el procedimiento de clase: X = variable independiente (filas), Y = variable
    // dependiente (columnas), hipótesis H0/H1, FO, FE, X0^2, valor teórico, conclusión y recomendación.
    public record Cruce(String id, String titulo, String proposito, String criterio,
                        String colA, String etiquetaA, Function<String, String> grupoA, List<String> ordenA,
                        String colB, String etiquetaB, Function<String, String> grupoB, List<String> ordenB,
                        Predicate<Map<String, String>> filtro, String poblacion,
                        String recomendacionRelacion, String recomendacionIndependencia) {}

    private static final List<String> LIKERT_GRUPOS = List.of("En desacuerdo", "De acuerdo");
    private static final List<String> SI_NO = List.of("Sí", "No");
    /** Columna derivada: titulado junior o consolidado según los años de vida profesional (corte en la mediana). */
    static final String EXPERIENCIA = "experiencia";
    private static final List<String> EXPERIENCIA_GRUPOS = List.of("Junior", "Consolidado");
    private static final Function<String, String> MISMO = v -> v;

    private static String financiamientoAgrupado(String codigo) {
        return switch (codigo) {
            case "recursos_propios" -> "Recursos propios";
            case "beca_total", "beca_parcial" -> "Con beca (total o parcial)";
            default -> "";
        };
    }

    /** Áreas agrupadas por afinidad, porque Robótica y Base de Datos tienen muy pocas respuestas por separado. */
    private static String areaAgrupada(String codigo) {
        return switch (codigo) {
            case "IA", "Robotica" -> "IA y Robótica";
            case "Ingenieria_Software", "Base_Datos" -> "Software y Base de Datos";
            case "Redes_Datos_Seguridad" -> "Redes y Seguridad";
            default -> "";
        };
    }

    private static String cargoEjecutivo(String codigo) {
        return codigo.isEmpty() ? "" : "Ejecutivo".equals(codigo) ? "Cargo ejecutivo" : "Otro cargo";
    }

    public static final List<Cruce> CRUCES = List.of(
        new Cruce("C1", "Situación laboral × satisfacción con la formación",
            "Determinar si la valoración de la formación recibida cambia según la situación laboral actual del titulado, como evidencia del resultado del proceso formativo frente a las condiciones de empleo.", "3.2.1",
            "situacion_laboral", "Situación laboral", c -> Catalogo.etiqueta("situacion_laboral", c),
            List.of("Trabaja en una organización", "Emprendimiento propio", "No trabaja"),
            "likert_satisfaccion_global_formacion", "Satisfacción con la formación", Catalogo::likertAgrupado, LIKERT_GRUPOS,
            f -> true, "Todos los titulados",
            "La valoración de la formación cambia según la situación laboral: revisar el plan de estudios con los grupos menos satisfechos (por ejemplo, quienes no trabajan).",
            "La satisfacción con la formación no depende de la situación laboral: las mejoras del plan de estudios pueden dirigirse a todos los titulados por igual."),
        new Cruce("C2", "Sector × pertinencia del puesto",
            "Determinar si la concordancia entre la titulación y el empleo depende del sector, público o privado, en que trabaja el titulado.", "3.2.3",
            "sector_publico_privado", "Sector", c -> Catalogo.etiqueta("sector_publico_privado", c), List.of("Público", "Privado"),
            "likert_pertinencia_puesto", "Pertinencia del puesto", Catalogo::likertAgrupado, LIKERT_GRUPOS,
            TRABAJA_EN_ORGANIZACION, "Trabajan en una organización",
            "La pertinencia del puesto depende del sector: priorizar convenios de prácticas y vinculación con el sector de menor acuerdo.",
            "La pertinencia del puesto es parecida en el sector público y en el privado: los convenios de prácticas pueden abarcar ambos sectores."),
        new Cruce("C3", "Formación complementaria × remuneración",
            "Determinar si la formación complementaria se asocia con el nivel de remuneración, como sustento para la oferta de actualización profesional y posgrado.", "2.4.1",
            "formacion_complementaria", "Formación complementaria", c -> Catalogo.etiqueta("formacion_complementaria", c), SI_NO,
            "remuneracion_promedio", "Remuneración", Catalogo::remuneracionAgrupada, List.of("Hasta Bs 5.000", "Más de Bs 5.000"),
            TRABAJA_EN_ORGANIZACION, "Trabajan en una organización",
            "La formación complementaria se asocia con una mayor remuneración: difundir ese resultado al promocionar la oferta de posgrado.",
            "La formación complementaria no se asocia con la remuneración en esta muestra: la oferta de posgrado debe justificarse por actualización y pertinencia, no por una promesa salarial."),
        new Cruce("C4", "Interés en posgrado × oferta de posgrado FCyT",
            "Determinar si la percepción de que la oferta de posgrado de la FCyT es afín a la carrera depende del interés en realizar un posgrado.", "2.4.1",
            "interes_posgrado", "Interés en posgrado", c -> Catalogo.etiqueta("interes_posgrado", c), SI_NO,
            "likert_oferta_posgrado_fcyt", "Oferta FCyT afín", Catalogo::likertAgrupado, LIKERT_GRUPOS,
            f -> true, "Todos los titulados",
            "Interesados y no interesados perciben distinto la oferta de la FCyT: dirigir la difusión de los programas al grupo con menor acuerdo.",
            "La percepción de la oferta de la FCyT no depende del interés en un posgrado: la difusión puede dirigirse a todos los titulados."),
        new Cruce("C5", "Área de interés × institución elegida",
            "Determinar si la preferencia por estudiar el posgrado en la UMSS depende del área de interés, para identificar áreas en que la oferta propia no capta la demanda.", "2.4.1",
            "area_interes_posgrado", "Área de interés", c -> Catalogo.etiqueta("area_interes_posgrado", c),
            List.of("IA", "Robótica", "Ing. de Software", "Base de Datos", "Redes y Seguridad"),
            "organizacion_interes_posgrado", "Institución", Catalogo::organizacionAgrupada, List.of("UMSS (EUPG o FCyT)", "Otra institución"),
            f -> "SI".equals(f.get("interes_posgrado")), "Interesados en posgrado",
            "La elección de la UMSS depende del área: abrir o reforzar en la UMSS los programas de las áreas en que los interesados prefieren otra institución.",
            "La preferencia por la UMSS es parecida en todas las áreas: la captación debe mejorarse de forma general (costo, horarios, modalidad), no área por área."),
        new Cruce("C6", "Concordancia formación–mercado × tiempo al primer empleo",
            "Determinar si percibir la formación alineada con los requerimientos del mercado se asocia con un menor tiempo para obtener el primer empleo.", "3.2.3",
            "likert_concordancia_requerimientos", "Concordancia", Catalogo::likertAgrupado, LIKERT_GRUPOS,
            "tiempo_primer_trabajo", "Tiempo al primer empleo", Catalogo::tiempoAgrupado, List.of("Hasta 4 meses", "Más de 4 meses"),
            TRABAJA_EN_ORGANIZACION, "Trabajan en una organización",
            "La concordancia con el mercado se asocia con un ingreso más rápido al empleo: alinear el plan de estudios con los requerimientos del sector productivo.",
            "El tiempo al primer empleo no depende de la concordancia percibida: la inserción rápida se explica por otros factores, como que muchos titulados ya trabajaban antes de titularse."),
        new Cruce("C7", "Nivel del programa cursado × fuente de financiamiento",
            "Establecer si la fuente de financiamiento (variable dependiente) depende del nivel del programa de formación complementaria cursado (variable independiente). Como las becas son pocas, el financiamiento se agrupa en recursos propios y con beca.", "2.4.1",
            "programa_nivel", "Nivel cursado", Catalogo::nivelAgrupado, List.of("Diplomado o Especialidad", "Maestría o superior"),
            "programa_financiamiento", "Financiamiento", Analisis::financiamientoAgrupado, List.of("Recursos propios", "Con beca (total o parcial)"),
            f -> "SI".equals(f.get("formacion_complementaria")), "Con formación complementaria",
            "Las becas se concentran en algunos niveles: gestionar becas y convenios de financiamiento (organismos internacionales, instituciones, empresas) para los niveles que hoy se pagan casi solo con recursos propios.",
            "La forma de financiamiento es parecida en todos los niveles: buscar financiamiento a través de organismos internacionales y convenios para contar con becas en todos los programas."),
        new Cruce("C8", "Edad × área de interés del posgrado",
            "Analizar el patrón demográfico: establecer si el área en que el titulado quiere hacer un posgrado depende de su edad, para orientar la oferta a cada grupo.", "2.4.1",
            "edad", "Edad", Catalogo::edadAgrupada, List.of("26 o menos", "27–34", "35 o más"),
            "area_interes_posgrado", "Área de interés", Analisis::areaAgrupada, List.of("IA y Robótica", "Software y Base de Datos", "Redes y Seguridad"),
            f -> "SI".equals(f.get("interes_posgrado")), "Interesados en posgrado",
            "El área de interés cambia con la edad: orientar la oferta de posgrado por grupo de edad, con el área que más elige cada grupo (ver la tabla de contingencia).",
            "El área de interés no depende de la edad: la oferta puede organizarse por área sin distinguir grupos de edad."),
        new Cruce("C9", "Experiencia profesional × remuneración",
            "Establecer si la remuneración depende de la experiencia del titulado (junior o consolidado, con corte en la mediana de los años de vida profesional), para conocer el grado de madurez profesional de la muestra.", "3.2.3",
            EXPERIENCIA, "Experiencia", MISMO, EXPERIENCIA_GRUPOS,
            "remuneracion_promedio", "Remuneración", Catalogo::remuneracionAgrupada, List.of("Hasta Bs 5.000", "Más de Bs 5.000"),
            TRABAJA_EN_ORGANIZACION, "Trabajan en una organización",
            "La remuneración crece con la experiencia: ofrecer a los titulados junior programas de especialización inicial que aceleren su desarrollo profesional.",
            "La remuneración no depende de la experiencia: la diferencia salarial se explica por otros factores, como el rubro o el cargo."),
        new Cruce("C10", "Experiencia profesional × cargo",
            "Establecer si ocupar un cargo ejecutivo depende de la experiencia (junior o consolidado), para distinguir a los profesionales junior de los mandos consolidados.", "3.2.3",
            EXPERIENCIA, "Experiencia", MISMO, EXPERIENCIA_GRUPOS,
            "cargo", "Cargo", Analisis::cargoEjecutivo, List.of("Cargo ejecutivo", "Otro cargo"),
            TRABAJA_EN_ORGANIZACION, "Trabajan en una organización",
            "Los cargos ejecutivos se concentran en los titulados consolidados: ofrecerles formación en gestión y dirección, y a los junior especializaciones técnicas.",
            "El cargo no depende de la experiencia: la formación en gestión puede ofrecerse a todos los titulados por igual."),
        new Cruce("C11", "Formación complementaria × interés en posgrado",
            "Establecer si haber cursado formación complementaria influye en el interés por seguir estudiando un posgrado.", "2.4.1",
            "formacion_complementaria", "Formación complementaria", c -> Catalogo.etiqueta("formacion_complementaria", c), SI_NO,
            "interes_posgrado", "Interés en posgrado", c -> Catalogo.etiqueta("interes_posgrado", c), SI_NO,
            f -> true, "Todos los titulados",
            "Quienes ya cursaron formación complementaria quieren seguir estudiando: ofrecer rutas de continuidad (diplomado, especialidad, maestría) a quienes ya terminaron un programa.",
            "El interés en un posgrado no depende de haber cursado formación complementaria: la oferta debe llegar por igual a quienes nunca hicieron un programa."),
        new Cruce("C12", "Edad × situación laboral",
            "Analizar el patrón demográfico: establecer si la situación laboral del titulado depende de su edad.", "3.2.3",
            "edad", "Edad", Catalogo::edadAgrupada, List.of("26 o menos", "27–34", "35 o más"),
            "situacion_laboral", "Situación laboral", c -> Catalogo.etiqueta("situacion_laboral", c),
            List.of("Trabaja en una organización", "Emprendimiento propio", "No trabaja"),
            f -> true, "Todos los titulados",
            "La situación laboral cambia con la edad: reforzar la vinculación con empleadores y la bolsa de trabajo para el grupo de edad con más titulados sin trabajo.",
            "La situación laboral no depende de la edad: las acciones de inserción laboral pueden dirigirse a todos los titulados."),
        new Cruce("C13", "Género × interés en posgrado",
            "Analizar el patrón demográfico: establecer si el interés en realizar un posgrado depende del género.", "2.4.1",
            "genero", "Género", c -> Catalogo.etiqueta("genero", c), List.of("Hombre", "Mujer"),
            "interes_posgrado", "Interés en posgrado", c -> Catalogo.etiqueta("interes_posgrado", c), SI_NO,
            f -> true, "Todos los titulados",
            "El interés en un posgrado difiere por género: revisar si la oferta (horarios, modalidad, becas) es igual de accesible para hombres y mujeres.",
            "El interés en un posgrado es parecido en hombres y mujeres: la oferta no necesita diferenciarse por género.")
    );

    /** Mediana de los años de vida profesional (mismas filas que el intervalo de confianza). */
    static double medianaExperiencia(Dataset d) {
        List<Double> vida = new ArrayList<>();
        for (Map<String, String> f : d.filas) {
            String v = d.valor(f, "anios_vida_profesional"), u = d.valor(f, "anios_desempleado");
            if (v.isEmpty() || u.isEmpty() || Double.parseDouble(u) > Double.parseDouble(v)) continue;
            vida.add(Double.parseDouble(v));
        }
        return vida.isEmpty() ? Double.NaN : Estadistica.mediana(vida);
    }

    private static String valorCruce(Dataset d, Map<String, String> f, String col, double mediana) {
        if (!col.equals(EXPERIENCIA)) return d.valor(f, col);
        String v = d.valor(f, "anios_vida_profesional"), u = d.valor(f, "anios_desempleado");
        if (v.isEmpty() || u.isEmpty() || Double.parseDouble(u) > Double.parseDouble(v)) return "";
        return Double.parseDouble(v) <= mediana ? "Junior" : "Consolidado";
    }

    public static Map<String, Object> cruce(Dataset d, Cruce c) {
        double mediana = c.colA().equals(EXPERIENCIA) || c.colB().equals(EXPERIENCIA) ? medianaExperiencia(d) : Double.NaN;
        List<String[]> pares = new ArrayList<>();
        for (Map<String, String> f : d.filas(c.filtro())) {
            String crudoA = valorCruce(d, f, c.colA(), mediana), crudoB = valorCruce(d, f, c.colB(), mediana);
            String a = c.grupoA().apply(crudoA);
            String b = c.grupoB().apply(crudoB);
            if (!a.isEmpty() && !b.isEmpty() && !crudoA.isEmpty() && !crudoB.isEmpty()) pares.add(new String[]{a, b});
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", c.id());
        r.put("titulo", c.titulo());
        r.put("proposito", c.proposito());
        r.put("criterio", c.criterio());
        r.put("poblacion", c.poblacion());
        r.put("variableFilas", c.etiquetaA());
        r.put("variableColumnas", c.etiquetaB());
        if (!Double.isNaN(mediana)) {
            String m = mediana == Math.rint(mediana) ? String.valueOf((long) mediana) : String.valueOf(mediana);
            r.put("nota", "Junior: hasta " + m + " años de vida profesional (la mediana); consolidado: más de " + m + " años.");
        }
        r.putAll(Estadistica.chiCuadrada(pares, c.ordenA(), c.ordenB()));
        Object indep = r.get("independientes");
        String recomendacion = indep == null ? null : (Boolean) indep ? c.recomendacionIndependencia() : c.recomendacionRelacion();
        if (recomendacion != null) r.put("recomendacion", recomendacion);
        agregarProcedimiento(r, pares);
        return r;
    }

    /** Hipótesis y muestra de los datos (T1, T2, ...), como en el procedimiento de clase. */
    static void agregarProcedimiento(Map<String, Object> r, List<String[]> pares) {
        String x = (String) r.get("variableFilas"), y = (String) r.get("variableColumnas");
        r.put("hipotesisNula", "Las variables «" + x + "» (X) y «" + y + "» (Y) son independientes.");
        r.put("hipotesisAlterna", "Existe dependencia significativa entre las variables «" + x + "» (X) y «" + y + "» (Y).");
        List<List<String>> muestra = new ArrayList<>();
        for (int k = 0; k < Math.min(10, pares.size()); k++) muestra.add(List.of("T" + (k + 1), pares.get(k)[0], pares.get(k)[1]));
        r.put("muestra", muestra);
    }

    public static List<Map<String, Object>> cruces(Dataset d) {
        return CRUCES.stream().map(c -> cruce(d, c)).toList();
    }

    /**
     * Cruce elegido por el usuario entre dos preguntas cualesquiera de opción única.
     * Con agrupar = true se aplican las mismas agrupaciones de los cruces C1–C7.
     */
    public static Map<String, Object> cruceLibre(Dataset d, String colA, String colB, boolean agrupar) {
        if (!Catalogo.PREGUNTAS_CRUZABLES.containsKey(colA) || !Catalogo.PREGUNTAS_CRUZABLES.containsKey(colB)) {
            throw new IllegalArgumentException("Pregunta no disponible para cruzar");
        }
        if (colA.equals(colB)) throw new IllegalArgumentException("Elija dos preguntas distintas");
        Function<String, String> gA = agrupar && Catalogo.agrupacion(colA) != null ? Catalogo.agrupacion(colA) : c -> Catalogo.etiqueta(colA, c);
        Function<String, String> gB = agrupar && Catalogo.agrupacion(colB) != null ? Catalogo.agrupacion(colB) : c -> Catalogo.etiqueta(colB, c);
        List<String> ordenA = agrupar && Catalogo.agrupacion(colA) != null ? Catalogo.ordenAgrupado(colA)
            : Catalogo.etiquetas(colA).keySet().stream().map(c -> Catalogo.etiqueta(colA, c)).toList();
        List<String> ordenB = agrupar && Catalogo.agrupacion(colB) != null ? Catalogo.ordenAgrupado(colB)
            : Catalogo.etiquetas(colB).keySet().stream().map(c -> Catalogo.etiqueta(colB, c)).toList();
        String a = Catalogo.PREGUNTAS_CRUZABLES.get(colA);
        String b = Catalogo.PREGUNTAS_CRUZABLES.get(colB);
        Cruce c = new Cruce("libre", a + " × " + b,
            "Determinar si " + a.toLowerCase() + " y " + b.toLowerCase() + " están relacionadas (cruce definido por el usuario).",
            "—", colA, a, gA, ordenA, colB, b, gB, ordenB, f -> true, "Titulados que respondieron ambas preguntas", null, null);
        return cruce(d, c);
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
            e.put("rubro", Catalogo.etiqueta("rubro", d.valor(f, "rubro")));
            empresas.add(e);
        }
        r.put("empresas", empresas);
        for (String col : List.of("tipo_organizacion", "tamano", "rubro", "contrato_ultimos_5_anios", "probabilidad_contratacion", "nivel_formacion_demandado", "medio_convocatoria")) {
            r.put(col, Estadistica.frecuencias(col, d.valores(col)));
        }
        r.put("cargos", ordenarDesc(Estadistica.frecuencias("cargos", d.valoresMultiples("cargos")), d.n()));
        r.put("perfil", perfilEmpleadores(d));

        Map<String, Object> abiertas = new LinkedHashMap<>();
        // Texto libre: se codifica en categorías por palabras clave (Abiertas.java)
        for (String col : List.of("areas_emergentes", "herramientas", "habilidades")) {
            abiertas.put(col, Abiertas.codificar(col, d.valores(col)));
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

    // ---------------- Principio de uniformidad: prueba de las frecuencias ----------------
    // Apuntes cap. 3: con FE = n/k se verifica si las respuestas se reparten por igual entre
    // las k opciones de la pregunta ("Uniformidad") o si hay opciones preferidas ("No uniformidad").
    public record Uniformidad(String id, String titulo, String encuesta, String columna, String variable,
                              Predicate<Map<String, String>> filtro, String poblacion, String pantalla, String proposito,
                              String recomendacionPreferencia, String recomendacionPareja) {}

    private static final Predicate<Map<String, String>> INTERESADOS_POSGRADO = f -> "SI".equals(f.get("interes_posgrado"));

    public static final List<Uniformidad> UNIFORMIDAD = List.of(
        new Uniformidad("F1", "Área de interés para el posgrado", "titulados", "area_interes_posgrado", "Área de interés",
            INTERESADOS_POSGRADO, "Interesados en posgrado", "posgrado",
            "Verificar si los titulados interesados en un posgrado se reparten por igual entre las áreas temáticas o si hay áreas preferidas.",
            "Hay áreas preferidas: priorizar la apertura de programas de posgrado en el área más elegida.",
            "El interés se reparte de forma pareja: la oferta de posgrado debería cubrir varias áreas y no concentrarse en una sola."),
        new Uniformidad("F2", "Institución preferida para el posgrado", "titulados", "organizacion_interes_posgrado", "Institución",
            INTERESADOS_POSGRADO, "Interesados en posgrado", "posgrado",
            "Verificar si la preferencia por la institución donde estudiar el posgrado se reparte por igual o se concentra en algunas.",
            "La preferencia se concentra en algunas instituciones: si la UMSS no es la más elegida, revisar costo, horarios y modalidad de su oferta.",
            "Ninguna institución se destaca: la UMSS puede captar a los interesados si mejora la difusión de sus programas."),
        new Uniformidad("F3", "Nivel del programa cursado", "titulados", "programa_nivel", "Nivel del programa",
            f -> true, "Titulados con formación complementaria", "posgrado",
            "Verificar si los programas de formación complementaria cursados se reparten por igual entre los niveles o si hay niveles más cursados.",
            "Hay niveles más cursados: la oferta de actualización debe reforzar esos niveles, que son los que los titulados realmente siguen.",
            "Los niveles se cursan por igual: mantener una oferta equilibrada entre diplomados, especialidades y maestrías."),
        new Uniformidad("F4", "Medio por el que obtuvo el trabajo", "titulados", "medio_obtuvo_trabajo", "Medio",
            TRABAJA_EN_ORGANIZACION, "Trabajan en una organización", "empleo",
            "Verificar si los titulados consiguen trabajo por todos los medios por igual o si hay medios que predominan.",
            "Hay medios que predominan: la Carrera debe apoyar la inserción laboral por esos medios (por ejemplo, bolsa de trabajo o red de contactos).",
            "Ningún medio predomina: la inserción laboral debe apoyarse por varios medios a la vez."),
        new Uniformidad("F5", "Satisfacción global con la formación", "titulados", "likert_satisfaccion_global_formacion", "Satisfacción",
            f -> true, "Todos los titulados", "resultados",
            "Verificar si las respuestas de satisfacción con la formación se reparten por igual entre las cuatro opciones o se concentran en alguna.",
            "Las respuestas se concentran en algunas opciones: la opinión de los titulados es clara y sirve como evidencia del resultado formativo.",
            "Las respuestas se reparten por igual: no hay una opinión dominante sobre la formación y conviene indagar las causas."),
        new Uniformidad("F6", "Nivel de formación que piden los empleadores", "empleadores", "nivel_formacion_demandado", "Nivel demandado",
            f -> true, "Organizaciones encuestadas", "perfil",
            "Verificar si los empleadores piden por igual todos los niveles de formación o si hay un nivel que predomina.",
            "Hay un nivel que predomina: el perfil de egreso y la oferta de posgrado deben responder a ese nivel de formación.",
            "Los empleadores piden todos los niveles por igual: la Carrera debe ofrecer una ruta de formación continua en todos los niveles."),
        new Uniformidad("F7", "Probabilidad de contratar titulados", "empleadores", "probabilidad_contratacion", "Probabilidad",
            f -> true, "Organizaciones encuestadas", "relaciones",
            "Verificar si las organizaciones se reparten por igual entre las opciones de probabilidad de contratación o si alguna predomina.",
            "Hay una respuesta que predomina: usarla para estimar la demanda de titulados en los próximos años.",
            "Las respuestas se reparten por igual: la demanda futura de titulados es incierta."),
        new Uniformidad("F8", "Medio de convocatoria de las organizaciones", "empleadores", "medio_convocatoria", "Medio de convocatoria",
            f -> true, "Organizaciones encuestadas", "relaciones",
            "Verificar si las organizaciones convocan profesionales por todos los medios por igual o si hay medios preferidos.",
            "Hay medios preferidos: la Carrera debe publicar sus titulados y su bolsa de trabajo en esos medios.",
            "Ningún medio predomina: la difusión de los titulados debe hacerse por varios medios.")
    );

    public static Map<String, Object> uniformidad(Dataset d, Uniformidad u) {
        List<String> valores = d.valores(u.columna(), u.filtro());
        Map<String, Integer> conteo = new LinkedHashMap<>();
        Catalogo.etiquetas(u.columna()).keySet().forEach(c -> conteo.put(c, 0));
        for (String v : valores) if (conteo.containsKey(v)) conteo.merge(v, 1, Integer::sum);
        List<String> categorias = conteo.keySet().stream().map(c -> Catalogo.etiqueta(u.columna(), c)).toList();
        List<Integer> fo = new ArrayList<>(conteo.values());

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", u.id());
        r.put("titulo", u.titulo());
        r.put("encuesta", u.encuesta());
        r.put("variable", u.variable());
        r.put("poblacion", u.poblacion());
        r.put("pantalla", u.pantalla());
        r.put("proposito", u.proposito());
        r.putAll(Estadistica.pruebaFrecuencias(categorias, fo));
        int k = categorias.size();
        r.put("hipotesisNula", "Las respuestas de «" + u.variable() + "» se reparten por igual entre las " + k + " opciones (uniformidad).");
        r.put("hipotesisAlterna", "Las respuestas de «" + u.variable() + "» no se reparten por igual: hay opciones preferidas.");
        int n = (int) r.get("n");
        if (n > 0) {
            int iMax = 0;
            for (int i = 1; i < k; i++) if (fo.get(i) > fo.get(iMax)) iMax = i;
            r.put("masElegida", categorias.get(iMax));
            r.put("frecuenciaMasElegida", fo.get(iMax));
            r.put("proporcionMasElegida", (double) fo.get(iMax) / n);
        }
        Object uniforme = r.get("uniforme");
        if (uniforme != null) r.put("recomendacion", (Boolean) uniforme ? u.recomendacionPareja() : u.recomendacionPreferencia());
        return r;
    }

    public static List<Map<String, Object>> uniformidades(Dataset titulados, Dataset empleadores) {
        return UNIFORMIDAD.stream().map(u -> uniformidad(u.encuesta().equals("titulados") ? titulados : empleadores, u)).toList();
    }

    /**
     * Análisis exploratorio multivariable (referencias/analisis_exploratorio_multivariable.tex,
     * ejemplo del docente): tabulación cruzada de dos variables cualitativas (X, Y) con la
     * frecuencia y el promedio de una variable cuantitativa (Z) en cada combinación que aparece.
     * Aquí: X = nivel del programa cursado, Y = fuente de financiamiento, Z = años de vida
     * profesional (solo quienes hicieron formación complementaria).
     */
    public static Map<String, Object> multivariable(Dataset d) {
        record Grupo(String x, String y, List<Double> z) {}
        Map<String, Grupo> grupos = new LinkedHashMap<>();
        for (Map<String, String> f : d.filas(x -> true)) {
            String x = d.valor(f, "programa_nivel"), y = d.valor(f, "programa_financiamiento"),
                zTxt = d.valor(f, "anios_vida_profesional");
            if (x.isEmpty() || y.isEmpty() || zTxt.isEmpty()) continue;
            double z;
            try { z = Double.parseDouble(zTxt); } catch (NumberFormatException e) { continue; }
            grupos.computeIfAbsent(x + "|" + y, k -> new Grupo(
                Catalogo.etiqueta("programa_nivel", x), Catalogo.etiqueta("programa_financiamiento", y), new ArrayList<>()
            )).z().add(z);
        }
        List<Map<String, Object>> filas = grupos.values().stream()
            .sorted((a, b) -> b.z().size() - a.z().size())
            .map(g -> {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("x", g.x());
                fila.put("y", g.y());
                fila.put("n", g.z().size());
                fila.put("zProm", g.z().stream().mapToDouble(v -> v).average().orElse(0));
                return fila;
            }).toList();

        int nTotal = filas.stream().mapToInt(f -> (int) f.get("n")).sum();
        double zPromGeneral = filas.stream().mapToDouble(f -> (int) f.get("n") * (double) f.get("zProm")).sum() / nTotal;

        // Por cada categoria de X: n, financiamiento mas frecuente, Z promedio de esa categoria
        // y si queda por encima o por debajo del promedio general (hechos, no interpretacion).
        Map<String, List<Map<String, Object>>> porX = new LinkedHashMap<>();
        for (Map<String, Object> f : filas) porX.computeIfAbsent((String) f.get("x"), k -> new ArrayList<>()).add(f);
        List<Map<String, Object>> hallazgos = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : porX.entrySet()) {
            List<Map<String, Object>> deX = e.getValue();
            int nX = deX.stream().mapToInt(f -> (int) f.get("n")).sum();
            double zPromX = deX.stream().mapToDouble(f -> (int) f.get("n") * (double) f.get("zProm")).sum() / nX;
            Map<String, Object> financiamientoFrecuente = deX.stream().max((a, b) -> (int) a.get("n") - (int) b.get("n")).orElseThrow();
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("x", e.getKey());
            h.put("n", nX);
            h.put("zProm", zPromX);
            h.put("sobrePromedio", zPromX > zPromGeneral);
            h.put("financiamientoFrecuente", financiamientoFrecuente.get("y"));
            h.put("pctFinanciamientoFrecuente", (double) (int) financiamientoFrecuente.get("n") / nX);
            hallazgos.add(h);
        }
        hallazgos.sort((a, b) -> Double.compare((double) b.get("zProm"), (double) a.get("zProm")));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("filas", filas);
        r.put("nTotal", nTotal);
        r.put("zPromGeneral", zPromGeneral);
        r.put("hallazgos", hallazgos);
        return r;
    }
}
