package com.actividad2;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Codigos de cada pregunta del formulario -> etiqueta legible, en el mismo
 * orden en que aparecen en el formulario (Anexos de actvidad2.tex).
 * Tambien guarda las agrupaciones decididas en PLAN.md 5.3 y 5.6.
 */
public class Catalogo {

    private static final Map<String, Map<String, String>> ETIQUETAS = new LinkedHashMap<>();

    private static void def(String columna, String... codigoEtiqueta) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < codigoEtiqueta.length; i += 2) m.put(codigoEtiqueta[i], codigoEtiqueta[i + 1]);
        ETIQUETAS.put(columna, m);
    }

    static {
        String[] siNo = {"SI", "Sí", "NO", "No"};
        String[] likert4 = {"1", "Totalmente en desacuerdo", "2", "En desacuerdo", "3", "De acuerdo", "4", "Totalmente de acuerdo"};
        String[] organizaciones = {
            "EUPG", "EUPG", "Direccion_Posgrado_FCyT", "Dirección de Posgrado FCyT", "IBNORCA", "IBNORCA",
            "Universidades_internacionales", "Univ. internacionales", "Universidades_publicas_nacionales", "Univ. públicas nacionales",
            "Universidades_privadas_nacionales", "Univ. privadas nacionales", "Otro", "Otro"};

        // ---- Titulados ----
        def("edad", "15_18", "15–18", "19_22", "19–22", "23_26", "23–26", "27_30", "27–30", "31_34", "31–34", "35_o_mas", "35 o más");
        def("genero", "Hombre", "Hombre", "Mujer", "Mujer", "Otro", "Otro");
        def("formacion_complementaria", siNo);
        def("programa_nivel", "Diplomado", "Diplomado", "Especialidad", "Especialidad", "Maestria", "Maestría", "Doctorado", "Doctorado", "Pos_doctorado", "Pos doctorado");
        def("programa_donde", organizaciones);
        def("programa_financiamiento", "recursos_propios", "Recursos propios", "beca_total", "Beca total", "beca_parcial", "Beca parcial");
        def("interes_posgrado", siNo);
        def("area_interes_posgrado", "IA", "IA", "Robotica", "Robótica", "Ingenieria_Software", "Ing. de Software", "Base_Datos", "Base de Datos", "Redes_Datos_Seguridad", "Redes y Seguridad");
        def("organizacion_interes_posgrado", organizaciones);
        def("likert_oferta_posgrado_fcyt", likert4);
        def("situacion_laboral", "trabajando", "Trabaja en una organización", "emprendimiento", "Emprendimiento propio", "no_trabajo", "No trabaja");
        def("rubro", "Telecomunicaciones", "Telecomunicaciones", "Educacion", "Educación", "Desarrollo_Software", "Desarrollo de Software", "Redes_Computadoras", "Redes de Computadoras", "Salud", "Salud", "Banca", "Banca", "Otro", "Otro");
        def("sector_publico_privado", "Publico", "Público", "Privado", "Privado");
        def("departamento_bolivia", "Cochabamba", "Cochabamba", "Chuquisaca", "Chuquisaca", "Tarija", "Tarija", "La_Paz", "La Paz", "Oruro", "Oruro", "Potosi", "Potosí", "Santa_Cruz", "Santa Cruz", "Beni", "Beni", "Pando", "Pando", "Extranjero", "Extranjero");
        def("area_organizacion", "Contabilidad", "Contabilidad", "Administracion", "Administración", "Computo", "Cómputo", "Desarrollo", "Desarrollo", "RRHH", "RR.HH.", "Otra", "Otra");
        def("cargo", "Ejecutivo", "Ejecutivo", "Tecnico", "Técnico", "Desarrollo", "Desarrollo", "Help_Desk", "Help Desk", "Otro", "Otro");
        def("likert_pertinencia_puesto", likert4);
        def("antiguedad_trabajo", "menos_1", "< 1 año", "1_2", "1–2", "2_5", "2–5", "5_8", "5–8", "8_11", "8–11", "11_15", "11–15", "15_20", "15–20", "20_25", "20–25", "mas_25", "> 25");
        def("medio_obtuvo_trabajo", "medios_escritos_audiovisuales", "Medios escritos/audiovisuales", "internet_redes_sociales", "Internet y redes sociales", "postulacion_competitiva", "Postulación competitiva", "recomendacion_terceros", "Recomendación de terceros", "otros", "Otros");
        def("remuneracion_promedio", "menor_2500", "< Bs 2.500", "2500_3500", "Bs 2.500–3.500", "3501_5000", "Bs 3.501–5.000", "5001_7000", "Bs 5.001–7.000", "7001_10000", "Bs 7.001–10.000", "mas_10000", "> Bs 10.000");
        def("origen_emprendimiento", "idea_propia", "Idea propia", "idea_conjunta", "Idea conjunta", "startup", "StartUp", "spin_off", "Spin Off", "otro", "Otro");
        def("tipo_entregable", "producto", "Producto", "servicio", "Servicio", "ambos", "Ambos");
        def("financiamiento_externo_negocio", "prestamos_bancarios", "Préstamos bancarios", "inversores", "Inversores", "financiamiento_propio", "Financiamiento propio");
        def("likert_satisfaccion_negocio", "1", "Insatisfecho", "2", "Algo satisfecho", "3", "Satisfecho");
        def("likert_importancia_formacion_negocio", "1", "No es importante", "2", "Poco importante", "3", "Importante", "4", "Muy importante");
        def("razon_no_trabaja",
            "demanda_laboral_inexistente", "Demanda laboral inexistente", "incumplimiento_requisitos_experiencia", "Falta de experiencia previa",
            "incumplimiento_requisitos_habilidades", "Falta de habilidades específicas", "red_contactos_limitada", "Red de contactos limitada",
            "problemas_presentacion_cv", "Problemas con el CV", "habilidades_entrevistas", "Entrevistas", "ofertas_remuneracion_baja", "Remuneración muy baja", "otro", "Otro");
        def("ha_tenido_trabajo_antes", siNo);
        def("primer_empleo", siNo);
        def("tiempo_primer_trabajo", "ya_trabajaba", "Ya trabajaba", "menos_1_mes", "< 1 mes", "1_4_meses", "1–4 meses", "4_8_meses", "4–8 meses", "8_12_meses", "8–12 meses", "mas_12_meses", "> 12 meses");
        def("cantidad_empleos", "1", "1", "2", "2", "3", "3", "mas_3", "Más de 3");
        def("likert_satisfaccion_global_formacion", likert4);
        def("likert_concordancia_requerimientos", likert4);
        def("aspectos_utilidad",
            "aplicacion_practica", "Aplicación práctica en proyectos reales", "pasantias_practicas", "Pasantías o prácticas",
            "distribucion_horarios", "Distribución de horarios", "habilidades_analiticas", "Habilidades analíticas",
            "equipos_multidisciplinarios", "Equipos multidisciplinarios", "herramientas_software_industria", "Herramientas de la industria");
        def("aspectos_mejorar",
            "integracion_teoria_practica", "Integrar teoría y práctica", "relacionamiento_empresas", "Relación con empresas (prácticas)",
            "distribucion_horarios", "Distribución de horarios", "actualizacion_plan_estudios", "Actualizar el plan de estudios",
            "actividades_extracurriculares", "Actividades extracurriculares", "mas_especializacion", "Más opciones de especialización");
        def("asignaturas_ventaja",
            "Introduccion", "Introducción", "Base_Datos", "Base de Datos", "Inteligencia_Artificial", "Inteligencia Artificial",
            "Redes_Computadora", "Redes de Computadora", "Electivas", "Electivas", "T.I.S.", "T.I.S.", "Basicas", "Básicas",
            "Planificacion_Proyectos_Gestion", "Planif. de Proyectos y Gestión", "Tecnologia_Redes_Avanzadas", "Tecnología Redes Avanzadas");
        def("asignaturas_poco_utiles",
            "Basicas", "Básicas", "Aplic_Interactivas_TV_Digital", "Aplic. Interactivas TV Digital", "Telefonia_IP", "Telefonía IP",
            "Web_Semanticas", "Web Semánticas", "Graficacion_Computadora", "Graficación por Computadora", "Robotica", "Robótica",
            "Taller_Programacion_Bajo_Nivel", "Taller Prog. Bajo Nivel", "Teoria_Automatas_Leng_Formales", "Teoría de Autómatas",
            "Programacion_Funcional", "Programación Funcional", "Teoria_Grafos", "Teoría de Grafos", "Contabilidad_Basica", "Contabilidad Básica");
        def("red_contactos_interes", siNo);

        // ---- Empleadores ----
        def("tipo_organizacion", "Publico", "Público", "Privado", "Privado");
        def("tamano", "Grande", "Grande (100+)", "Mediana", "Mediana (31–99)", "Pequena", "Pequeña (11–30)", "Micro", "Micro (hasta 10)");
        def("contrato_ultimos_5_anios", siNo);
        def("probabilidad_contratacion", "alta", "Alta probabilidad", "depende_crecimiento", "Depende del crecimiento", "no_prevista", "No prevista", "no_requiere", "No requiere");
        def("nivel_formacion_demandado", "Licenciatura", "Licenciatura", "Diplomado", "Diplomado", "Especialidad", "Especialidad", "Maestria", "Maestría", "Doctorado", "Doctorado", "Post-doctorado", "Post-doctorado");
        def("medio_convocatoria", "medios_escritos_audiovisuales", "Medios escritos/audiovisuales", "internet_redes_sociales", "Internet y redes sociales", "postulacion_competitiva", "Postulación competitiva", "oferta_profesional_personal", "Oferta profesional personal", "recomendacion_terceros", "Recomendación de terceros", "otros", "Otros");
        def("cargos", "apoyo_asistencia", "Apoyo / asistencia", "tecnico", "Técnico", "analista", "Analista", "especialista", "Especialista", "supervisor_coordinador", "Supervisor / coordinador", "jefatura_gerencial", "Jefatura / gerencial", "consultor", "Consultor", "otro", "Otro");
        def("escala_empleador", "Totalmente_de_acuerdo", "Totalmente de acuerdo", "Parcialmente_de_acuerdo", "Parcialmente de acuerdo", "Totalmente_en_desacuerdo", "Totalmente en desacuerdo", "No_sabe", "No sabe");
    }

    /** Los 10 criterios de "Formación y perfil profesional" de la encuesta a empleadores. */
    public static final Map<String, String> ITEMS_PERFIL = new LinkedHashMap<>();
    static {
        ITEMS_PERFIL.put("perfil_confianza", "La Carrera brinda confianza como formadora");
        ITEMS_PERFIL.put("perfil_titulo_consistente", "El título es consistente con las competencias");
        ITEMS_PERFIL.put("perfil_consulta_autoridades", "Las autoridades consultan nuestra opinión");
        ITEMS_PERFIL.put("perfil_conoce_perfil", "Conozco el perfil profesional declarado");
        ITEMS_PERFIL.put("perfil_coherente_desempeno", "El perfil es coherente con el desempeño");
        ITEMS_PERFIL.put("perfil_desempeno_destacado", "Desempeño destacado profesional e integral");
        ITEMS_PERFIL.put("perfil_opinion_practicas", "Consideran nuestra opinión en prácticas y pasantías");
        ITEMS_PERFIL.put("perfil_competencias_laborales", "Evidencian competencias laborales");
        ITEMS_PERFIL.put("perfil_valores", "Demuestran valores y ética profesional");
        ITEMS_PERFIL.put("perfil_participo_retroalimentacion", "He participado en procesos de retroalimentación");
    }

    /** Punto medio en meses de cada rango de tiempo_primer_trabajo (PLAN.md 5.6, T4). */
    public static final Map<String, Double> MESES_PRIMER_TRABAJO = new LinkedHashMap<>();
    static {
        MESES_PRIMER_TRABAJO.put("ya_trabajaba", 0.0);
        MESES_PRIMER_TRABAJO.put("menos_1_mes", 0.5);
        MESES_PRIMER_TRABAJO.put("1_4_meses", 2.5);
        MESES_PRIMER_TRABAJO.put("4_8_meses", 6.0);
        MESES_PRIMER_TRABAJO.put("8_12_meses", 10.0);
        MESES_PRIMER_TRABAJO.put("mas_12_meses", 12.0); // sin limite superior: cota inferior
    }

    public static Map<String, String> etiquetas(String columna) {
        if (ITEMS_PERFIL.containsKey(columna)) return ETIQUETAS.get("escala_empleador");
        return ETIQUETAS.getOrDefault(columna, Map.of());
    }

    public static String etiqueta(String columna, String codigo) {
        return etiquetas(columna).getOrDefault(codigo, codigo.replace('_', ' '));
    }

    // ---- Agrupaciones para los cruces (PLAN.md 5.3) ----

    public static String likertAgrupado(String codigo) {
        return switch (codigo) {
            case "1", "2" -> "En desacuerdo";
            case "3", "4" -> "De acuerdo";
            default -> "";
        };
    }

    public static String remuneracionAgrupada(String codigo) {
        return switch (codigo) {
            case "menor_2500", "2500_3500", "3501_5000" -> "Hasta Bs 5.000";
            case "5001_7000", "7001_10000", "mas_10000" -> "Más de Bs 5.000";
            default -> "";
        };
    }

    public static String tiempoAgrupado(String codigo) {
        return switch (codigo) {
            case "ya_trabajaba", "menos_1_mes", "1_4_meses" -> "Hasta 4 meses";
            case "4_8_meses", "8_12_meses", "mas_12_meses" -> "Más de 4 meses";
            default -> "";
        };
    }

    public static String organizacionAgrupada(String codigo) {
        return switch (codigo) {
            case "EUPG", "Direccion_Posgrado_FCyT" -> "UMSS (EUPG o FCyT)";
            case "" -> "";
            default -> "Otra institución";
        };
    }

    public static String nivelAgrupado(String codigo) {
        return switch (codigo) {
            case "Diplomado", "Especialidad" -> "Diplomado o Especialidad";
            case "Maestria", "Doctorado", "Pos_doctorado" -> "Maestría o superior";
            default -> "";
        };
    }

    public static String edadAgrupada(String codigo) {
        return switch (codigo) {
            case "15_18", "19_22", "23_26" -> "26 o menos";
            case "27_30", "31_34" -> "27–34";
            case "35_o_mas" -> "35 o más";
            default -> "";
        };
    }

    /** Homologacion de cargos titulados/empleadores (PLAN.md 5.6, T9). */
    public static String cargoHomologado(String codigo) {
        return switch (codigo) {
            case "Help_Desk", "apoyo_asistencia" -> "Soporte";
            case "Tecnico", "Desarrollo", "tecnico" -> "Técnico / Desarrollo";
            case "analista", "especialista", "consultor" -> "Analista / Especialista";
            case "Ejecutivo", "supervisor_coordinador", "jefatura_gerencial" -> "Gestión / Jefatura";
            case "" -> "";
            default -> "Otro";
        };
    }
}
