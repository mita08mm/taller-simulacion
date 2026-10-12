# Actividad 2 — Análisis de Mercado y Herramienta de Recolección de Datos

> Alcance: todo lo de abajo aplica al trabajo dentro de `actividad2/`.

## Contexto
- **Tema:** Análisis de Mercado sobre perfil, formación continua y empleabilidad
  de los titulados de Ingeniería en Sistemas (UMSS), enfoque ARCUSUR.
- **Uso de IA (actualizado 2026-10-12, indicación del docente):** se permite
  usar IA siempre que quede **documentado**. El informe tiene la sección
  "Declaración de Uso de Inteligencia Artificial" (antes de las Referencias)
  con una tabla tarea → qué hizo la IA → qué revisó/decidió el autor, y la
  herramienta citada en las Referencias (APA 7). Cada vez que la IA haga una
  parte nueva del trabajo, agregar la fila correspondiente. No presentar como
  propio de la IA ni del autor algo que no se pueda verificar: lo que
  dependa del autor queda como `[Completar: ...]` para que lo llene él.
  Reemplaza la regla anterior ("no mencionar IA en el informe").
- **Enunciado:** transcrito completo en `actvidad2.tex` (no tocar ese archivo
  como si fuera el informe — es solo la copia fiel de lo que pidió el docente,
  igual que `Actividad1.tex` en `actividad1/`).
- **Dos entregables distintos:**
  1. Informe escrito con la estructura oficial de 7 puntos (ver
     `actvidad2.tex`, sección "Estructura Oficial del Documento").
  2. Un sistema de software propio (no basta con formularios web + Excel —
     confirmado directamente con el docente) que importa los datos y hace el
     análisis + dashboard.

## Decisiones ya confirmadas con el docente
- **Captura de datos:** dos formularios externos distintos — titulados
  responden por Microsoft Forms (forms.cloud.microsoft), empleadores por
  Google Forms. Ambos instrumentos transcritos en `actvidad2.tex`, Anexos.
- **Empresas a encuestar:** 5 (no 3 como decía el enunciado original).
- **Sistema propio:** sí, obligatorio — Java en el backend (pedido explícito
  del docente, mismo lenguaje que `actividad1/`).
- **Stack elegido:** backend Java puro (`com.sun.net.httpserver`, sin Maven
  ni dependencias externas — se abandonó Javalin por problemas de instalación
  de Maven en la máquina); frontend Vue (Vite + pnpm).
- **Variables a analizar:** las decide el equipo (el docente lo dejó abierto),
  no vienen dadas. Propuesta inicial en `PLAN.md` sección "Variables".
- **Base del análisis (decisión del usuario, 2026-10-12):** todo el análisis
  se hace sobre las preguntas reales de los formularios (Anexos de
  `actvidad2.tex`) y sus respuestas — no sobre lo que piden literalmente las
  Historias de Usuario del enunciado. Donde la HU pide algo que el formulario
  no pregunta (año de egreso → se usa `anios_vida_profesional`; posgrado
  *deseado* y financiamiento *futuro* → solo existe nivel/financiamiento del
  programa *ya cursado*; Likert 1–5 → el formulario usa 4 niveles), manda el
  formulario.
- **Indicaciones del docente sobre el informe (2026-10-12):**
  - ARCU-SUR no se transcribe: se trata de **responder** sus criterios con el
    propio trabajo (análisis + sistema). Eso está en la sección "Respuesta a
    los Criterios ARCU-SUR" (generada en `tex/generado/criterios_arcusur.tex`):
    por criterio, qué pide (una línea), evidencia del estudio y aspectos
    favorables/desfavorables (regla explícita: favorable si > 50%).
  - Todo análisis tiene que decir **para qué se hace y qué aporta**, escrito
    como trabajo de grado (prosa), no como preguntas y respuestas. Nada sin
    justificar ni inventado. Antes de agregar una tabla o prueba nueva,
    escribir su propósito; si no responde a nada, no va.
- **El formulario no se modifica** y solo se envía a titulados de Ingeniería
  de Sistemas de la UMSS (2026-10-12). Los problemas de diseño que tiene se
  resuelven como decisiones de análisis o limitaciones declaradas: ver
  `PLAN.md` sección 5.6. No proponer cambios de preguntas.

## Archivos clave
- `actvidad2.tex` — enunciado original transcrito (fuente de verdad del
  enunciado, no editar como informe).
- `tex/Informe.tex` — el informe entregable (portada + entornos reutilizables
  de `actividad1/`: `mitabla`, `lstset`, tikz). MVP completo (2026-10-12):
  metodología según ARCU-SUR, modelo de simulación, herramienta y resultados.
  Los resultados de las secciones 3–5 salen del **dataset de prueba** y están
  marcados con `\datosprueba` / `\avisoprueba` (en negrita): hay que
  reemplazarlos con los resultados reales y vaciar esos comandos antes de
  entregar. Las secciones 3–5 incluyen con `\input` los fragmentos de
  `tex/generado/`, que genera `sistema/backend/.../ReporteTex.java` con el
  procedimiento paso a paso (formato de `referencias/apuntes_simulacion.tex`,
  pedido del usuario: cada número tiene que mostrar de dónde sale). No editar
  `tex/generado/` a mano; regenerar (ver `sistema/README.md`). El informe va
  en **blanco y negro** (sin colores: marcas en negrita, `hidelinks`). Las conclusiones (sección 6) quedan pendientes de datos reales.
- `PLAN.md` — plan de arquitectura + explicación detallada de las fórmulas
  estadísticas que se van a usar, con preguntas pendientes para el docente.
- `sistema/` — backend Java (sin Maven, `com.sun.net.httpserver` nativo del
  JDK, sin dependencias externas) + frontend Vue 3 con Naive UI (componentes)
  y ECharts (gráficos: barras, dona, mapas de calor, radar). Una pantalla por
  Historia de Usuario + empleadores + simulación + importar/exportar. Ver
  `sistema/README.md` para cómo correrlo y la tabla HU → pantalla → endpoint.
- `GLOSARIO.md` — glosario personal de términos (estadística, metodología,
  software), no es parte del informe.

## Base teórica (de `referencias/`, libro de Coss Bu)

- **Intervalo de confianza para una media** — `referencias/cap6_analisis_resultados.tex`,
  sección 6.1. Fórmula completa y ejemplo numérico explicado en `PLAN.md`
  sección 2.
- **Chi-cuadrada** — `referencias/cap3_pruebas_estadisticas.tex`, sección 3.2.
  **Importante:** esa sección solo trae la prueba de bondad de ajuste de
  **una** variable ($FE_i = N/n$). La prueba de independencia que
  necesitamos (dos variables cruzadas, tabla de contingencia,
  $FE_{ij} = \text{fila}\times\text{columna}/N$) **no está textual en el
  libro** — es la extensión estándar, pendiente de confirmar con el docente
  si se puede usar (ver `PLAN.md` sección 3 y 4, pregunta A.1).

## Fuentes oficiales de ARCUSUR
Documentos oficiales (arcusur.org) para citar en la sección 7 (Referencias,
formato APA 7) y para fundamentar el Marco Metodológico:
- Criterios de Ingeniería (mayo 2019): https://www.arcusur.org/wp-content/uploads/2024/03/Criterios_de_Ingenieria_Mayo_2019.pdf
  — define las 4 dimensiones (Contexto Institucional, Proyecto Académico,
  Comunidad Universitaria, Infraestructura) y, dentro de la Dimensión 2
  (Proyecto Académico), el criterio de pertinencia: correspondencia entre
  la formación ofrecida y las demandas del contexto social, económico y
  laboral (perfil del egresado, empleabilidad, vinculación con el sector
  productivo). Ya citado en `tex/Informe.tex`, sección 1.3 (Justificación).
- Guía de Autoevaluación de Ingeniería: https://www.arcusur.org/wp-content/uploads/2024/02/Guia_de_Autoevaluacion_Ingenieria.pdf
- Guía de Pares de Ingeniería: https://www.arcusur.org/wp-content/uploads/2024/02/Guia_de_Pares_Ingenieria.pdf

## Reglas de notación / estilo
- Mismas reglas de `act_1`/`actividad1` aplican donde correspondan: no dejar
  líneas en blanco dobles/múltiples seguidas al editar archivos.
- Citar siempre la referencia exacta (archivo + sección) al usar una
  fórmula del libro — no copiar fórmulas sin decir de dónde salen.
- Si una fórmula necesaria no está en las referencias (como el caso de la
  tabla de contingencia arriba), decirlo explícitamente y marcarlo como
  pregunta pendiente para el docente, no presentarla como si viniera del
  libro.

## Fallos / cosas a recordar
- El enunciado original decía fecha de entrega 13 de septiembre — hay que
  confirmar con el docente si sigue vigente (ver `PLAN.md` sección 4,
  pregunta D).
- `actvidad2.tex` compila en la raíz de `actividad2/` (no dentro de `tex/`),
  así que su `build/` queda en `actividad2/build/`, no en `actividad2/tex/build/`
  — el `.gitignore` de esta carpeta ya cubre ambos casos.
- Git: nunca commitear/pushear sin que el usuario lo pida explícitamente, y
  siempre pasarle el comando en vez de ejecutarlo (ver memoria global).
