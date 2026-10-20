package com.actividad2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga inicial de las dos encuestas (Anexos de actvidad2.tex) cuando la base
 * está vacía. Los códigos de preguntas y opciones son los mismos que usa el
 * análisis (Catalogo.java), así que las respuestas del formulario propio, las
 * importadas de Forms y las del dataset de prueba se analizan igual.
 *
 * Diferencia con Microsoft Forms: se agrega "año de titulación" (análisis por
 * cohorte, criterio ARCU-SUR 3.2.1). La rama "no trabaja" -> "¿ha tenido algún
 * trabajo antes?" queda como estaba: su continuación nunca se capturó.
 */
public class Semilla {

    // ---------- pequeño constructor de encuestas ----------
    record Op(String codigo, String etiqueta) {}

    static final class P {
        String codigo, texto, tipo, ayuda, dependeDe, dependeValores;
        boolean obligatoria, contacto;
        Integer max;
        Double minimo, maximo;
        List<Op> opciones = new ArrayList<>();

        P op(String codigo, String etiqueta) { opciones.add(new Op(codigo, etiqueta)); return this; }
        P si(String pregunta, String valores) { dependeDe = pregunta; dependeValores = valores; return this; }
        P opcional() { obligatoria = false; return this; }
        P ayuda(String a) { ayuda = a; return this; }
        P max(int m) { max = m; return this; }
        P rango(double a, double b) { minimo = a; maximo = b; return this; }
        P contacto() { contacto = true; obligatoria = false; return this; }
        P siNo() { return op("SI", "Sí").op("NO", "No"); }
        P acuerdo4() {
            return op("1", "Totalmente en desacuerdo").op("2", "En desacuerdo").op("3", "De acuerdo").op("4", "Totalmente de acuerdo");
        }
    }

    static final class S {
        String titulo;
        List<P> preguntas = new ArrayList<>();

        P p(String codigo, String tipo, String texto) {
            P p = new P();
            p.codigo = codigo;
            p.tipo = tipo;
            p.texto = texto;
            p.obligatoria = !tipo.equals("texto");
            preguntas.add(p);
            return p;
        }
    }

    static final class E {
        String codigo, nombre, introduccion;
        List<S> secciones = new ArrayList<>();

        S s(String titulo) {
            S s = new S();
            s.titulo = titulo;
            secciones.add(s);
            return s;
        }
    }

    // ---------- carga ----------
    public static void cargarSiFalta() {
        Db.transaccion(c -> {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM encuesta")) {
                rs.next();
                if (rs.getInt(1) > 0) return null;
            }
            guardar(c, titulados());
            guardar(c, empleadores());
            return null;
        });
    }

    private static void guardar(Connection c, E e) throws SQLException {
        int encuestaId;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO encuesta (codigo, nombre, introduccion) VALUES (?, ?, ?) RETURNING id")) {
            ps.setString(1, e.codigo);
            ps.setString(2, e.nombre);
            ps.setString(3, e.introduccion);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); encuestaId = rs.getInt(1); }
        }
        int ordenSeccion = 1;
        for (S s : e.secciones) {
            int seccionId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO seccion (encuesta_id, orden, titulo) VALUES (?, ?, ?) RETURNING id")) {
                ps.setInt(1, encuestaId);
                ps.setInt(2, ordenSeccion++);
                ps.setString(3, s.titulo);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); seccionId = rs.getInt(1); }
            }
            int ordenPregunta = 1;
            for (P p : s.preguntas) {
                int preguntaId;
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO pregunta (encuesta_id, seccion_id, codigo, texto, ayuda, tipo, obligatoria, orden,
                                              depende_de, depende_valores, max_selecciones, valor_minimo, valor_maximo, es_contacto)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""")) {
                    ps.setInt(1, encuestaId);
                    ps.setInt(2, seccionId);
                    ps.setString(3, p.codigo);
                    ps.setString(4, p.texto);
                    ps.setString(5, p.ayuda);
                    ps.setString(6, p.tipo);
                    ps.setBoolean(7, p.obligatoria);
                    ps.setInt(8, ordenPregunta++);
                    ps.setString(9, p.dependeDe);
                    ps.setString(10, p.dependeValores);
                    ps.setObject(11, p.max);
                    ps.setObject(12, p.minimo);
                    ps.setObject(13, p.maximo);
                    ps.setBoolean(14, p.contacto);
                    try (ResultSet rs = ps.executeQuery()) { rs.next(); preguntaId = rs.getInt(1); }
                }
                int ordenOpcion = 1;
                for (Op o : p.opciones) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO opcion (pregunta_id, codigo, etiqueta, orden) VALUES (?, ?, ?, ?)")) {
                        ps.setInt(1, preguntaId);
                        ps.setString(2, o.codigo());
                        ps.setString(3, o.etiqueta());
                        ps.setInt(4, ordenOpcion++);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    // ---------- Encuesta a titulados ----------
    static E titulados() {
        E e = new E();
        e.codigo = "titulados";
        e.nombre = "Encuesta de opinión a titulados de Ingeniería en Sistemas";
        e.introduccion = "Estimad@ Ingenier@: La Carrera de Ingeniería en Sistemas está en proceso de Autoevaluación para la "
            + "elaboración de un PLAN DE DESARROLLO ESTRATÉGICO en el marco de una futura postulación de acreditación ante el "
            + "ARCUSUR. Por lo que debido a la gran importancia de este proceso le pedimos por favor que pueda ayudarnos "
            + "compartiendo su opinión sobre varios aspectos importantes de la Carrera.";
        String organizaciones = "EUPG|Escuela Universitaria de Posgrado (EUPG);Direccion_Posgrado_FCyT|Dirección de Posgrado FCyT;IBNORCA|IBNORCA;"
            + "Universidades_internacionales|Universidades internacionales;Universidades_publicas_nacionales|Universidades públicas nacionales;"
            + "Universidades_privadas_nacionales|Universidades privadas nacionales;Otro|Otro";

        S s0 = e.s("Datos generales");
        s0.p("edad", "unica", "Edad que tiene actualmente")
            .op("15_18", "15 - 18 años").op("19_22", "19 - 22 años").op("23_26", "23 - 26 años").op("27_30", "27 - 30 años")
            .op("31_34", "31 - 34 años").op("35_o_mas", "35 años o más");
        s0.p("genero", "unica", "Género").op("Hombre", "Hombre").op("Mujer", "Mujer").op("Otro", "Otro");
        s0.p("anio_titulacion", "numero", "¿En qué año se tituló de la Carrera de Ingeniería de Sistemas?")
            .rango(1980, Year.now().getValue())
            .ayuda("Permite comparar los resultados entre cohortes de titulados.");

        S s1 = e.s("Formación complementaria y posgrado");
        s1.p("formacion_complementaria", "unica", "¿Ha realizado o se encuentra realizando algún programa de formación "
            + "complementaria (posgrado, diplomado, doble titulación, especialización, etc.)?").siNo();
        s1.p("programa_nivel", "unica", "¿Cuál es el programa de formación complementaria de mayor nivel que ha concluido "
            + "o que se encuentra realizando?").si("formacion_complementaria", "SI")
            .op("Diplomado", "Diplomado").op("Especialidad", "Especialidad").op("Maestria", "Maestría")
            .op("Doctorado", "Doctorado").op("Pos_doctorado", "Pos doctorado");
        opciones(s1.p("programa_donde", "unica", "¿Dónde ha cursado el programa indicado?").si("formacion_complementaria", "SI"), organizaciones);
        s1.p("programa_financiamiento", "unica", "¿Cuál fue la fuente de financiamiento para cursar el programa indicado?")
            .si("formacion_complementaria", "SI")
            .op("recursos_propios", "Financiado con recursos propios").op("beca_total", "Total financiamiento por medio de una beca")
            .op("beca_parcial", "Financiado parcialmente por medio de una beca");
        s1.p("programa_nombre", "texto", "Indique el nombre del programa").si("formacion_complementaria", "SI");
        s1.p("interes_posgrado", "unica", "¿Estaría interesado en realizar estudios en algún programa de posgrado dentro de "
            + "las áreas de la Ingeniería en Sistemas?").siNo();
        s1.p("area_interes_posgrado", "unica", "¿En cuál de las siguientes áreas le interesaría realizar el programa?")
            .si("interes_posgrado", "SI")
            .op("IA", "IA").op("Robotica", "Robótica").op("Ingenieria_Software", "Ingeniería de Software")
            .op("Base_Datos", "Base de Datos").op("Redes_Datos_Seguridad", "Redes de Datos y Seguridad");
        opciones(s1.p("organizacion_interes_posgrado", "unica", "En caso de realizar un estudio de posgrado ¿en qué organización "
            + "educativa optaría por realizar sus estudios?").si("interes_posgrado", "SI"), organizaciones);
        s1.p("likert_oferta_posgrado_fcyt", "escala", "¿Considera que existen programas de formación de posgrado ofertados en la "
            + "Dirección de Posgrado de la FCyT que son afines a las áreas de especialidad de un Ingeniero en Sistemas?").acuerdo4();

        S s2 = e.s("Trayectoria profesional");
        s2.p("anios_vida_profesional", "numero", "¿Cuántos son los años de vida profesional que lleva hasta la fecha?").rango(0, 60);
        s2.p("anios_desempleado", "numero", "De la cantidad de tiempo respondida en la pregunta anterior, ¿qué cantidad corresponde "
            + "al total de tiempo en que se ha encontrado desempleado?").rango(0, 60).ayuda("En años.");
        s2.p("situacion_laboral", "unica", "¿Cuál de las siguientes afirmaciones describe su situación laboral actual?")
            .op("trabajando", "Actualmente me encuentro trabajando en una organización").op("no_trabajo", "Actualmente NO trabajo")
            .op("emprendimiento", "Actualmente dedico mi tiempo COMPLETO a un emprendimiento propio");
        String trabaja = "trabajando";
        s2.p("empresa_nombre", "texto", "Nombre de la empresa u organización donde trabaja actualmente").si("situacion_laboral", trabaja);
        s2.p("rubro", "unica", "¿Cuál es el rubro de la organización donde trabaja actualmente?").si("situacion_laboral", trabaja)
            .op("Telecomunicaciones", "Telecomunicaciones").op("Educacion", "Educación").op("Desarrollo_Software", "Desarrollo de Software")
            .op("Redes_Computadoras", "Redes de Computadoras").op("Salud", "Salud").op("Banca", "Banca").op("Otro", "Otro");
        s2.p("rubro_otro", "texto", "Otro rubro").si("situacion_laboral", trabaja);
        s2.p("sector_publico_privado", "unica", "El trabajo en el que se encuentra, ¿a qué sector corresponde?").si("situacion_laboral", trabaja)
            .op("Publico", "Público").op("Privado", "Privado");
        s2.p("departamento_bolivia", "unica", "¿En qué departamento de Bolivia trabaja?").si("situacion_laboral", trabaja)
            .op("Cochabamba", "Cochabamba").op("Chuquisaca", "Chuquisaca").op("Tarija", "Tarija").op("La_Paz", "La Paz").op("Oruro", "Oruro")
            .op("Potosi", "Potosí").op("Santa_Cruz", "Santa Cruz").op("Beni", "Beni").op("Pando", "Pando").op("Extranjero", "Extranjero");
        s2.p("area_organizacion", "multiple", "¿Cuál es el área dentro de la organización donde desarrolla sus actividades profesionales?")
            .si("situacion_laboral", trabaja)
            .op("Contabilidad", "Contabilidad").op("Administracion", "Administración").op("Computo", "Cómputo")
            .op("Desarrollo", "Desarrollo").op("RRHH", "R.R.H.H.").op("Otra", "Otra");
        s2.p("area_otra", "texto", "Otra área").si("situacion_laboral", trabaja);
        s2.p("cargo", "unica", "¿Cuál es el cargo que desempeña en su actual trabajo?").si("situacion_laboral", trabaja)
            .op("Ejecutivo", "Ejecutivo").op("Tecnico", "Técnico").op("Desarrollo", "Desarrollo").op("Help_Desk", "Help Desk").op("Otro", "Otro");
        s2.p("cargo_otro", "texto", "Otro cargo").si("situacion_laboral", trabaja);
        s2.p("likert_pertinencia_puesto", "escala", "En su actual puesto de trabajo las labores que se encuentra desempeñando son "
            + "pertinentes a su formación como Ingeniero de Sistemas").si("situacion_laboral", trabaja).acuerdo4();
        s2.p("antiguedad_trabajo", "unica", "¿Qué antigüedad tiene en su actual trabajo?").si("situacion_laboral", trabaja)
            .op("menos_1", "Menos de 1 año").op("1_2", "Entre 1 a 2 años").op("2_5", "Entre 2 a 5 años").op("5_8", "Entre 5 a 8 años")
            .op("8_11", "Entre 8 a 11 años").op("11_15", "Entre 11 a 15 años").op("15_20", "Entre 15 a 20 años")
            .op("20_25", "Entre 20 a 25 años").op("mas_25", "Más de 25 años");
        s2.p("medio_obtuvo_trabajo", "unica", "¿A través de qué medio obtuvo el trabajo que actualmente desempeña?").si("situacion_laboral", trabaja)
            .op("medios_escritos_audiovisuales", "Medios escritos o audiovisuales").op("internet_redes_sociales", "Internet y redes sociales")
            .op("postulacion_competitiva", "Postulación competitiva (convocatorias públicas y privadas)")
            .op("recomendacion_terceros", "Recomendación de terceros").op("otros", "Otros");
        s2.p("remuneracion_promedio", "unica", "Señale la remuneración promedio mensual que está percibiendo en su actual trabajo")
            .si("situacion_laboral", trabaja)
            .op("menor_2500", "Menor a Bs. 2.500").op("2500_3500", "Entre Bs. 2.500 - Bs. 3.500").op("3501_5000", "Entre Bs. 3.501 - Bs. 5.000")
            .op("5001_7000", "Entre Bs. 5.001 - Bs. 7.000").op("7001_10000", "Entre Bs. 7.001 - Bs. 10.000").op("mas_10000", "Más de Bs. 10.000");
        String emprende = "emprendimiento";
        s2.p("origen_emprendimiento", "unica", "¿Cuál es el origen del desarrollo de su propio emprendimiento?").si("situacion_laboral", emprende)
            .op("idea_propia", "Idea propia").op("idea_conjunta", "Idea conjunta con compañeros").op("startup", "StartUp")
            .op("spin_off", "Spin Off (derivado de un proyecto de la Universidad)").op("otro", "Otro");
        s2.p("tipo_entregable", "unica", "¿Qué tipo de entregable genera el negocio?").si("situacion_laboral", emprende)
            .op("producto", "Producto").op("servicio", "Servicio").op("ambos", "Ambos");
        s2.p("financiamiento_externo_negocio", "unica", "¿Ha requerido financiamiento externo para su negocio?").si("situacion_laboral", emprende)
            .op("prestamos_bancarios", "Sí, mediante préstamos bancarios").op("inversores", "Sí, mediante inversores")
            .op("financiamiento_propio", "No, financiamiento propio");
        s2.p("likert_satisfaccion_negocio", "escala", "¿Qué tan satisfecho está con el rendimiento actual de su negocio?")
            .si("situacion_laboral", emprende).op("1", "Insatisfecho").op("2", "Algo satisfecho").op("3", "Satisfecho");
        s2.p("likert_importancia_formacion_negocio", "escala", "¿Qué tan importante considera que fue su formación en Ingeniería en "
            + "Sistemas para iniciar y gestionar su negocio?").si("situacion_laboral", emprende)
            .op("1", "No es importante").op("2", "Poco importante").op("3", "Importante").op("4", "Muy importante");
        String noTrabaja = "no_trabajo";
        s2.p("razon_no_trabaja", "unica", "Indique cuál es la razón por la que actualmente no trabaja en un empleo relacionado con su "
            + "formación como Ingeniero de Sistemas").si("situacion_laboral", noTrabaja)
            .op("demanda_laboral_inexistente", "Demanda laboral inexistente")
            .op("incumplimiento_requisitos_experiencia", "Incumplimiento de requisitos de experiencia previa")
            .op("incumplimiento_requisitos_habilidades", "Incumplimiento de requisitos en habilidades específicas (idiomas, software, etc.)")
            .op("red_contactos_limitada", "Red de contactos limitada").op("problemas_presentacion_cv", "Problemas de presentación del CV")
            .op("habilidades_entrevistas", "Habilidades deficientes en las entrevistas")
            .op("ofertas_remuneracion_baja", "Ofertas laborales con remuneración muy bajas").op("otro", "Otro");
        s2.p("ha_tenido_trabajo_antes", "unica", "Si bien actualmente no trabaja, ¿ha tenido algún trabajo antes?")
            .si("situacion_laboral", noTrabaja).siNo();

        S s3 = e.s("Primer empleo");
        s3.p("primer_empleo", "unica", "¿El trabajo que ejerce actualmente es su primer empleo?").siNo();
        s3.p("tiempo_primer_trabajo", "unica", "Luego de titularse, ¿cuánto tiempo se demoró en conseguir su primer trabajo relacionado "
            + "con su formación como Ingeniero en Sistemas?").si("situacion_laboral", trabaja)
            .op("ya_trabajaba", "Ya trabajaba antes de titularse").op("menos_1_mes", "Menos de 1 mes").op("1_4_meses", "Entre 1 - 4 meses")
            .op("4_8_meses", "Entre 4 - 8 meses").op("8_12_meses", "Entre 8 - 12 meses").op("mas_12_meses", "Más de 12 meses");
        s3.p("cantidad_empleos", "unica", "Desde que se tituló, ¿en cuántos empleos usted se ha desempeñado? (incluyendo su actual "
            + "empleo si corresponde)").si("situacion_laboral", trabaja)
            .op("1", "1").op("2", "2").op("3", "3").op("mas_3", "Más de 3");

        S s4 = e.s("Satisfacción con la formación");
        s4.p("likert_satisfaccion_global_formacion", "escala", "De manera global, ¿cuál es el grado de satisfacción que ha desarrollado "
            + "con la formación profesional recibida en la Carrera de Ingeniería de Sistemas?").acuerdo4();
        s4.p("likert_concordancia_requerimientos", "escala", "Considera que existe concordancia entre la formación profesional recibida "
            + "en la Carrera de Ingeniería de Sistemas y los requerimientos del mercado laboral").acuerdo4();
        s4.p("aspectos_utilidad", "multiple", "¿Qué aspectos de la Carrera les resultaron de bastante utilidad?")
            .op("aplicacion_practica", "La aplicación práctica de conceptos teóricos en proyectos reales")
            .op("pasantias_practicas", "La oportunidad de realizar pasantías o prácticas profesionales")
            .op("distribucion_horarios", "La distribución de los horarios de clases")
            .op("habilidades_analiticas", "El desarrollo de habilidades analíticas y de resolución de problemas")
            .op("equipos_multidisciplinarios", "La colaboración de equipos multidisciplinarios para resolver problemas complejos")
            .op("herramientas_software_industria", "La enseñanza de herramientas y software específico de la industria");
        s4.p("aspectos_mejorar", "multiple", "¿Qué aspectos de la Carrera considera que pueden mejorarse?")
            .op("integracion_teoria_practica", "Mayor integración de la teoría con aplicaciones prácticas en el plan de estudios")
            .op("relacionamiento_empresas", "Mayor relacionamiento del estudiante con las empresas por medio de prácticas profesionales")
            .op("distribucion_horarios", "La distribución de horarios de clases (horario para trabajadores, módulos virtuales, etc.)")
            .op("actualizacion_plan_estudios", "Actualización constante del plan de estudios para atender tendencias actuales")
            .op("actividades_extracurriculares", "Incremento de actividades extracurriculares (investigación, servicio social, etc.)")
            .op("mas_especializacion", "Ofrecimiento de más opciones de especialización");
        s4.p("asignaturas_ventaja", "multiple", "Indique 5 asignaturas cursadas que considera que le brindaron una ventaja competitiva").max(5)
            .op("Introduccion", "Introducción").op("Base_Datos", "Base de Datos").op("Inteligencia_Artificial", "Inteligencia Artificial")
            .op("Redes_Computadora", "Redes de Computadora").op("Electivas", "Electivas").op("T.I.S.", "T.I.S.")
            .op("Basicas", "Básicas (Física, Cálculo, Álgebras)").op("Planificacion_Proyectos_Gestion", "Planificación de Proyectos y Gestión")
            .op("Tecnologia_Redes_Avanzadas", "Tecnología Redes Avanzadas");
        s4.p("asignatura_ventaja_otra", "texto", "Otra asignatura que le brindó ventaja competitiva");
        s4.p("asignaturas_poco_utiles", "multiple", "Indique 5 asignaturas cursadas que no le resultaron de mucha utilidad").max(5)
            .op("Basicas", "Básicas (Física, Cálculo, Álgebras)").op("Aplic_Interactivas_TV_Digital", "Aplic. Interactivas para Televisión Digital")
            .op("Telefonia_IP", "Telefonía IP").op("Web_Semanticas", "Web Semánticas").op("Graficacion_Computadora", "Graficación por Computadora")
            .op("Robotica", "Robótica").op("Taller_Programacion_Bajo_Nivel", "Taller de Programación en Bajo Nivel")
            .op("Teoria_Automatas_Leng_Formales", "Teoría de Autómatas y Leng. Formales").op("Programacion_Funcional", "Programación Funcional")
            .op("Teoria_Grafos", "Teoría de Grafos").op("Contabilidad_Basica", "Contabilidad Básica");
        s4.p("asignatura_poco_util_otra", "texto", "Otra asignatura que no le resultó de mucha utilidad");

        S s5 = e.s("Red de contactos");
        s5.p("red_contactos_interes", "unica", "¿Le interesaría formar parte una red de contactos oficial de la Carrera de Ingeniería en "
            + "Sistemas para compartir experiencias, proyectos y ofertas laborales?").siNo();
        s5.p("email", "email", "Email de contacto").si("red_contactos_interes", "SI").contacto();
        s5.p("telefono", "telefono", "Número telefónico de contacto, de preferencia con WhatsApp").si("red_contactos_interes", "SI").contacto();
        s5.p("linkedin", "texto", "Link de LinkedIn").si("red_contactos_interes", "SI").contacto();
        return e;
    }

    // ---------- Encuesta a empleadores ----------
    static E empleadores() {
        E e = new E();
        e.codigo = "empleadores";
        e.nombre = "Encuesta a empleadores";
        e.introduccion = "La Carrera de Ingeniería de Sistemas de la Facultad de Ciencias y Tecnología (UMSS), en el marco de su proceso "
            + "de Autoevaluación Académica, desarrolla la etapa de recopilación de información primaria mediante la aplicación del "
            + "presente cuestionario dirigido a empleadores de profesionales titulados de la Carrera. La información será utilizada "
            + "exclusivamente con fines académicos e institucionales.";

        S s0 = e.s("Datos de la organización");
        s0.p("correo", "email", "Correo").contacto();
        s0.p("nombre_empresa", "texto", "Nombre de la Empresa o Institución").obligatoria = true;
        s0.p("tipo_organizacion", "unica", "Tipo de organización").op("Publico", "Público").op("Privado", "Privado");
        s0.p("tamano", "unica", "Tamaño de la organización")
            .op("Grande", "Grande (100 funcionarios o más)").op("Mediana", "Mediana (entre 31 a 99 funcionarios)")
            .op("Pequena", "Pequeña (entre 11 a 30 funcionarios)").op("Micro", "Micro (hasta 10 funcionarios)");
        P rubro = s0.p("rubro", "unica", "Indique el rubro o sector principal de la organización en la que se desempeña laboralmente el "
            + "profesional titulado de la Carrera de Ingeniería de Sistemas");
        // El código de cada rubro es el mismo texto del formulario (así se muestra en el análisis)
        for (String r : List.of("Desarrollo de software", "Servicios de tecnologías de información (TI)", "Telecomunicaciones y redes",
                "Soporte técnico y mantenimiento de sistemas", "Consultoría en sistemas / transformación digital",
                "Banca y servicios financieros", "Instituciones públicas / gobierno", "Educación e investigación",
                "Industria y manufactura", "Comercio y comercio electrónico", "Salud", "Seguridad informática / ciberseguridad",
                "Análisis de datos / inteligencia de negocios", "Startups o emprendimientos tecnológicos", "Otros")) {
            rubro.op(r, r);
        }

        S s1 = e.s("Empleabilidad");
        s1.p("contrato_ultimos_5_anios", "unica", "¿La organización ha contratado Ingenieros de la carrera de Sistemas en los últimos 5 años?").siNo();
        s1.p("probabilidad_contratacion", "unica", "Considerando las necesidades actuales y proyectadas, ¿existe la posibilidad de "
            + "incorporar Ingenieros de Sistemas en los próximos años?")
            .op("alta", "Sí, existe una alta probabilidad de contratación")
            .op("depende_crecimiento", "Sí, dependiendo del crecimiento o nuevos proyectos de la organización")
            .op("no_prevista", "No se tiene prevista la contratación actualmente").op("no_requiere", "No requiere profesionales del área");
        s1.p("nivel_formacion_demandado", "unica", "¿Qué nivel de formación académica o actualización profesional demanda su "
            + "organización para contratar nuevos profesionales del área?")
            .op("Licenciatura", "Licenciatura").op("Diplomado", "Diplomado").op("Especialidad", "Especialidad").op("Maestria", "Maestría")
            .op("Doctorado", "Doctorado").op("Post-doctorado", "Post-doctorado");
        s1.p("medio_convocatoria", "unica", "¿A través de qué medio convoca a profesionales en su organización?")
            .op("medios_escritos_audiovisuales", "Medios escritos y audiovisuales").op("internet_redes_sociales", "Internet y redes sociales")
            .op("postulacion_competitiva", "Postulación competitiva (convocatorias públicas y privadas)")
            .op("oferta_profesional_personal", "Oferta profesional personal").op("recomendacion_terceros", "Recomendación de terceros")
            .op("otros", "Otros");
        s1.p("cargos", "multiple", "¿Qué tipo de cargos desempeñan los profesionales titulados de la Carrera en su organización?")
            .op("apoyo_asistencia", "De apoyo / asistencia tecnológica")
            .op("tecnico", "Técnico (desarrollo de software, administración de sistemas, redes, bases de datos, soporte TI)")
            .op("analista", "Analista (sistemas, datos, procesos o soluciones tecnológicas)")
            .op("especialista", "Especialista (ciberseguridad, arquitectura de software, infraestructura tecnológica, entre otros)")
            .op("supervisor_coordinador", "Supervisor / coordinador de área tecnológica")
            .op("jefatura_gerencial", "Jefatura o gerencial (gestión de proyectos, dirección de TI, transformación digital)")
            .op("consultor", "Consultor tecnológico").op("otro", "Otro");
        s1.p("areas_emergentes", "texto", "Desde su experiencia en el sector laboral, ¿qué nuevas áreas de conocimiento, tecnologías o "
            + "tendencias emergentes considera importantes para la formación del profesional?");
        s1.p("herramientas", "texto", "¿Cuáles son las nuevas herramientas tecnológicas (software, plataformas o entornos de desarrollo) "
            + "que debería conocer y manejar el profesional titulado?");
        s1.p("habilidades", "texto", "Desde su experiencia, ¿qué habilidades, competencias y destrezas considera fundamentales que debe "
            + "desarrollar el profesional de Ingeniería de Sistemas?");

        S s2 = e.s("Formación y perfil profesional");
        for (var item : List.of(
                new String[]{"perfil_confianza", "La Carrera de Ingeniería de Sistemas de la UMSS brinda confianza a nuestra organización como formadora de profesionales del área tecnológica."},
                new String[]{"perfil_titulo_consistente", "Considero que el título profesional otorgado por la Carrera es consistente con el nivel de formación y las competencias demostradas por sus titulados."},
                new String[]{"perfil_consulta_autoridades", "Las autoridades de la Carrera consultan regularmente nuestra opinión como empleadores respecto al perfil profesional y las competencias requeridas en el ámbito laboral."},
                new String[]{"perfil_conoce_perfil", "Conozco el perfil profesional y las competencias de egreso declaradas por la Carrera."},
                new String[]{"perfil_coherente_desempeno", "El perfil profesional declarado por la Carrera es coherente con el desempeño laboral demostrado por sus titulados."},
                new String[]{"perfil_desempeno_destacado", "La formación proporcionada por la Carrera permite a sus titulados alcanzar un desempeño destacado tanto en el ámbito profesional como en su formación integral."},
                new String[]{"perfil_opinion_practicas", "La Carrera considera nuestra opinión como empleadores e incorpora las necesidades del sector laboral en la definición y mejora de actividades de formación práctica, pasantías y modalidades de titulación."},
                new String[]{"perfil_competencias_laborales", "El desempeño profesional de los titulados evidencia el desarrollo de competencias laborales como: comunicación efectiva, análisis y resolución de problemas, pensamiento crítico, aprendizaje continuo, trabajo en equipo, iniciativa personal y manejo adecuado de tecnologías de información."},
                new String[]{"perfil_valores", "Los profesionales titulados demuestran valores y actitudes relacionados con la ética profesional, responsabilidad social, inclusión, diversidad, respeto a los derechos humanos y compromiso con el medio ambiente."},
                new String[]{"perfil_participo_retroalimentacion", "He participado en procesos de recopilación de información, evaluación o retroalimentación relacionados con el desempeño laboral de los titulados de la Carrera."},
                // Criterios que tiene el formulario real de Google Forms y no figuraban en el Anexo del enunciado
                new String[]{"perfil_vinculos_oportunidades", "La Carrera de Ingeniería de Sistemas mantiene vínculos con organizaciones e instituciones que generan oportunidades laborales para sus titulados."},
                new String[]{"perfil_participo_vinculacion_entorno", "He participado en actividades de vinculación con el entorno laboral y tecnológico organizadas por la Carrera de Ingeniería de Sistemas o la Facultad de Ciencias y Tecnología."},
                new String[]{"perfil_participo_difusion", "He participado en actividades de difusión de oportunidades laborales, convocatorias, pasantías o prácticas profesionales organizadas por la Carrera de Ingeniería de Sistemas."},
                new String[]{"perfil_participo_vinculacion_profesionales", "He participado en actividades de vinculación entre profesionales del sector tecnológico, empleadores, docentes y estudiantes de la Carrera de Ingeniería de Sistemas para el intercambio de conocimientos, experiencias y desarrollo de trabajos conjuntos."},
                new String[]{"perfil_consultado_desempeno", "Como empleador, he sido consultado sobre el desempeño profesional de los titulados de Ingeniería de Sistemas, con el propósito de retroalimentar el perfil profesional y el plan de estudios de la Carrera."},
                new String[]{"perfil_consultada_pertinencia", "Como empleador, la Carrera de Ingeniería de Sistemas ha consultado nuestra opinión respecto a la pertinencia de la formación impartida y su correspondencia con las necesidades del sector tecnológico y laboral."},
                new String[]{"perfil_recurre_umss", "Cuando nuestra organización requiere profesionales del área de Sistemas, recurrimos a la Universidad Mayor de San Simón y a la Carrera de Ingeniería de Sistemas como fuente de búsqueda de talento profesional."},
                new String[]{"perfil_consultado_autoevaluacion", "He sido consultado periódicamente por la Carrera de Ingeniería de Sistemas durante sus procesos de autoevaluación y mejora continua."})) {
            s2.p(item[0], "escala", item[1])
                .op("Totalmente_de_acuerdo", "Totalmente de acuerdo").op("Parcialmente_de_acuerdo", "Parcialmente de acuerdo")
                .op("Totalmente_en_desacuerdo", "Totalmente desacuerdo").op("No_sabe", "No sabe");
        }
        return e;
    }

    /** La encuesta con el mismo formato que Encuestas.estructura(), armada en memoria (para simular sin base de datos). */
    public static java.util.Map<String, Object> estructuraLocal(String codigo) {
        E e = codigo.equals("titulados") ? titulados() : empleadores();
        List<java.util.Map<String, Object>> secciones = new ArrayList<>();
        for (S s : e.secciones) {
            List<java.util.Map<String, Object>> preguntas = new ArrayList<>();
            for (P p : s.preguntas) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("codigo", p.codigo);
                m.put("texto", p.texto);
                m.put("tipo", p.tipo);
                m.put("obligatoria", p.obligatoria);
                m.put("activa", true);
                m.put("esContacto", p.contacto);
                m.put("dependeDe", p.dependeDe);
                m.put("dependeValores", p.dependeValores == null ? List.of() : List.of(p.dependeValores.split(",")));
                m.put("maxSelecciones", p.max);
                m.put("valorMinimo", p.minimo);
                m.put("valorMaximo", p.maximo);
                List<java.util.Map<String, Object>> ops = new ArrayList<>();
                for (Op o : p.opciones) ops.add(java.util.Map.of("codigo", o.codigo(), "etiqueta", o.etiqueta(), "activa", true));
                m.put("opciones", ops);
                preguntas.add(m);
            }
            secciones.add(java.util.Map.of("titulo", s.titulo, "preguntas", preguntas));
        }
        return java.util.Map.of("codigo", e.codigo, "secciones", secciones);
    }

    private static void opciones(P p, String lista) {
        for (String par : lista.split(";")) {
            String[] cv = par.split("\\|");
            p.op(cv[0], cv[1]);
        }
    }
}
