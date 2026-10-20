package com.actividad2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Codificación de las preguntas abiertas de empleadores (10, 11 y 12) en
 * categorías, por palabras clave. Una respuesta cuenta una vez en cada
 * categoría que menciona (puede caer en varias); la que no menciona ninguna
 * se reporta como "sin categoría". Las palabras se buscan en el texto sin
 * acentos y en minúsculas; una palabra que termina en "*" acepta cualquier
 * terminación (ej. "automatiz*" = automatización, automatizar).
 */
public class Abiertas {

    record Categoria(String codigo, String etiqueta, List<Pattern> claves) {}

    private static Categoria cat(String codigo, String etiqueta, String... claves) {
        List<Pattern> p = new ArrayList<>();
        for (String c : claves) {
            String k = Pattern.quote(c.replace("*", ""));
            p.add(Pattern.compile("\\b" + k + (c.endsWith("*") ? "" : "\\b")));
        }
        return new Categoria(codigo, etiqueta, p);
    }

    static final Map<String, List<Categoria>> CATEGORIAS = Map.of(
        "areas_emergentes", List.of(
            cat("ia", "Inteligencia artificial y aprendizaje automático", "inteligencia artificial", "inteligente artificial", "ia", "machine learning", "aprendizaje automatico", "agente*", "llm", "ml"),
            cat("seguridad", "Ciberseguridad y seguridad de la información", "ciberseguridad", "seguridad", "ciberdefensa", "hacking", "forens*", "forenc*"),
            cat("datos", "Análisis de datos, big data y BI", "dato*", "big data", "data", "analitica", "business intelligence", "bi", "mineria"),
            cat("nube", "Computación en la nube", "nube", "cloud"),
            cat("automatizacion", "Automatización de procesos", "automatiz*", "rpa"),
            cat("software", "Desarrollo e ingeniería de software", "desarrollo", "software", "programacion", "web", "movil*", "apps", "aplicaciones", "arquitectura"),
            cat("infraestructura", "Redes, infraestructura e IoT", "redes", "infraestructura", "servidores", "iot", "internet de las cosas", "telecomunicaciones", "embebidos"),
            cat("gestion", "Gestión, negocio y sistemas empresariales", "negocio*", "gestion", "proyectos", "erp", "crm", "ventas", "contab*", "administra*", "agil*", "product owner")),
        "herramientas", List.of(
            cat("ia", "Herramientas y agentes de IA", "inteligencia artificial", "ia", "claude", "copilot", "chatgpt", "gpt", "gemini", "antigravity", "cursor", "agente*", "agentic*", "ml", "llm", "machine learning"),
            cat("nube", "Servicios en la nube", "nube", "cloud", "aws", "azure", "gcp", "google cloud"),
            cat("lenguajes", "Lenguajes y frameworks de programación", "python", "phyton", "java", "kotlin", "c", "rust", "lenguaje*", "entornos de desarrollo", "ide", "javascript", "typescript", "php", "net", "react", "angular", "vue", "flutter", "node", "framework*", "spring", "laravel", "django"),
            cat("versiones", "Control de versiones (Git)", "git", "github", "gitlab"),
            cat("devops", "DevOps, contenedores y Linux", "devops", "docker", "kubernetes", "contenedor*", "linux", "ci", "cd"),
            cat("datos", "Bases de datos y análisis de datos", "base de datos", "bases de datos", "sql", "power bi", "dato*", "big data", "data"),
            cat("seguridad", "Ciberseguridad", "ciberseguridad", "seguridad"),
            cat("proyectos", "Gestión ágil de proyectos", "jira", "trello", "scrum", "agil*", "proyectos"),
            cat("web_movil", "Desarrollo web y móvil", "web", "movil*", "apps", "aplicaciones"),
            cat("empresariales", "Sistemas empresariales (ERP, CRM, contables)", "erp", "crm", "sap", "odoo", "contab*", "ventas", "bim")),
        "habilidades", List.of(
            cat("equipo", "Trabajo en equipo", "equipo*", "colabora*", "grupo*"),
            cat("comunicacion", "Comunicación", "comunica*", "expresion", "presentacion", "oratoria"),
            cat("problemas", "Resolución de problemas y pensamiento crítico", "problema*", "critico", "analitic*", "analisis", "abstraccion", "logic*"),
            cat("aprendizaje", "Adaptación y aprendizaje continuo", "adapta*", "aprend*", "actualiz*", "autodidact*", "nuevas tecnologias", "versatil*", "multitask", "bajo presion"),
            cat("iniciativa", "Proactividad, iniciativa y creatividad", "proactiv*", "iniciativa", "creativ*", "innova*"),
            cat("responsabilidad", "Responsabilidad, ética y compromiso", "responsab*", "etica", "compromiso", "puntual*", "valores", "honest*", "atencion al detalle", "disciplina", "constancia", "dedicacion", "confiab*", "respeto"),
            cat("ingles", "Inglés", "ingles"),
            cat("tecnicas", "Conocimiento técnico específico (IA, seguridad, arquitectura)", "ia", "inteligencia artificial", "seguridad", "ciberseguridad", "arquitectura", "programacion", "tecnic*", "base de datos", "bases de datos", "redes", "red", "diagnostic*", "monitoreo"),
            cat("negocio", "Visión de negocio y atención al cliente", "negocio*", "cliente*", "requerimientos", "venderse", "marketing"))
    );

    /**
     * Frecuencia de cada categoría (respuestas que la mencionan) y relativa sobre
     * el total de respuestas de la pregunta. Al final, "sin categoría".
     */
    public static List<Map<String, Object>> codificar(String pregunta, List<String> respuestas) {
        List<Categoria> categorias = CATEGORIAS.get(pregunta);
        int[] cuenta = new int[categorias.size()];
        int sinCategoria = 0;
        for (String r : respuestas) {
            String n = FormsImport.normalizar(r);
            boolean alguna = false;
            for (int i = 0; i < categorias.size(); i++) {
                if (categorias.get(i).claves().stream().anyMatch(p -> p.matcher(n).find())) {
                    cuenta[i]++;
                    alguna = true;
                }
            }
            if (!alguna) sinCategoria++;
        }
        List<Map<String, Object>> lista = new ArrayList<>();
        for (int i = 0; i < categorias.size(); i++) lista.add(fila(categorias.get(i).codigo(), categorias.get(i).etiqueta(), cuenta[i], respuestas.size()));
        lista.sort((a, b) -> Integer.compare((Integer) b.get("frecuencia"), (Integer) a.get("frecuencia")));
        lista.add(fila("sin_categoria", "Sin categoría (respuesta no clasificable)", sinCategoria, respuestas.size()));
        return lista;
    }

    private static Map<String, Object> fila(String codigo, String etiqueta, int frecuencia, int n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", codigo);
        m.put("etiqueta", etiqueta);
        m.put("frecuencia", frecuencia);
        m.put("relativa", n == 0 ? 0.0 : (double) frecuencia / n);
        return m;
    }

    /** Palabras clave de cada categoría, para documentar la regla en el informe. */
    public static Map<String, List<String[]>> diccionario() {
        Map<String, List<String[]>> r = new LinkedHashMap<>();
        CATEGORIAS.forEach((pregunta, cats) -> {
            List<String[]> l = new ArrayList<>();
            for (Categoria c : cats) {
                List<String> claves = c.claves().stream().map(p -> {
                    String patron = p.pattern();
                    String limpio = patron.replace("\\b", "").replace("\\Q", "").replace("\\E", "");
                    return patron.endsWith("\\b") ? limpio : limpio + "*";
                }).toList();
                l.add(new String[]{c.etiqueta(), String.join(", ", claves)});
            }
            r.put(pregunta, l);
        });
        return r;
    }

    /** Revisión por consola: frecuencias por categoría y respuestas que no cayeron en ninguna. */
    public static void main(String[] args) throws Exception {
        Db.iniciar();
        Dataset e = Respuestas.dataset("empleadores");
        for (String col : CATEGORIAS.keySet().stream().sorted().toList()) {
            List<String> respuestas = e.valores(col);
            System.out.println("## " + col + " (n = " + respuestas.size() + ")");
            for (Map<String, Object> m : codificar(col, respuestas)) System.out.println("   " + m.get("frecuencia") + "  " + m.get("etiqueta"));
            for (String r : respuestas) {
                String n = FormsImport.normalizar(r);
                if (CATEGORIAS.get(col).stream().noneMatch(c -> c.claves().stream().anyMatch(p -> p.matcher(n).find()))) System.out.println("      sin categoría: " + r);
            }
        }
        System.exit(0);
    }
}
