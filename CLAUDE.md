# Taller de Simulación — Actividad 1

> Alcance: todo lo de abajo aplica al trabajo dentro de `actividad1/`. Vive en la raíz del repo (no dentro de `actividad1/`) para no mezclarse visualmente con la entrega.

## Contexto
- **Materia:** Taller de Simulación de Sistemas — UMSS
- **Docente:** Henrry Frank Villarroel Tapia
- **Tarea:** Resolver `tex/Actividad1.tex` (4 problemas) usando los métodos enseñados en clase
- **Importante:** El docente no quiere trabajos generados por IA. Todo debe seguir estrictamente `tex/Apunte.tex`.

## Estructura del entorno
- Cada actividad tiene 3 LaTeX independientes: `Apunte.tex` (apuntes/fuente de verdad), `Teorema.tex` (referencia, ej. Coss Bu), `Informe.tex` (el trabajo entregable, desglosado en secciones).
- `tex/Consulta_P3.tex` — **no es parte del informe entregable**. Modelo de solución de p3 en el mismo formato Paso 1–5 que usan los ejemplos de `Apunte.tex`/`Teorema.tex`/`referencias/cap5_simulacion.tex` (sin título ni relleno), con el porqué de cada operación (sumar, dividir, promediar, decidir) explicado dentro de cada paso. Es la base sobre la que se construye todo lo demás — antes de tocar `Informe.tex`, validar este modelo primero. Cuando se arme el mismo tipo de documento para p4, va en un archivo separado (`Consulta_P4.tex`), no junto con p3.
- `referencias/cap4_simu.tex` (en la **raíz del repo**, no dentro de `actividad1/`) — Capítulo 4 completo de Coss Bu ("Generación de Variables Aleatorias No-Uniformes"). Trae la teoría formal de Transformada Inversa, Rechazo, Composición y **Procedimientos Especiales** (Normal vía teorema del límite central, Erlang, Binomial, Poisson vía relación con la exponencial) — es la fuente original de la que salen los ejemplos que ya están en `Apunte.tex`/`Teorema.tex`. Vive en la raíz porque es material de referencia general (no específico de `actividad1`), útil para cualquier actividad del repo. No es parte de ningún informe entregable.
- `referencias/cap5_simulacion.tex` (raíz del repo, mismo motivo) — Capítulo 5 completo de Coss Bu ("Aplicaciones de Simulación"). Contiene el **Ejemplo 5.5 "Sistema de inventarios"**, que trae la metodología completa (tablas TI para demanda/entrega, tabla de simulación mensual, tabla de costos, optimización de q y R) directamente aplicable a p3/p4 de `actividad1`. Los problemas **5.10 y 5.11** de ese mismo capítulo son literalmente p3 y p4 de `actividad1` (mismo enunciado) — usar como referencia de valores/redacción si hay dudas de transcripción en `actividad1/tex/Actividad1.tex`. `referencias/` tiene su propio `.latexmkrc` y `.gitignore` (mismo patrón que `actividad1/tex/`: solo `.tex` se trackea, los PDF se ignoran a nivel repo y la basura de compilación cae a `build/`).
- `referencias/cap1_introduccion.tex`, `cap2_numeros_rectangulares.tex`, `cap3_pruebas_estadisticas.tex`, `cap6_analisis_resultados.tex`, `cap7_lenguajes_simulacion.tex` — resto de capítulos del libro de Coss Bu. Se convirtieron a partir de 5 PDFs escaneados que el usuario subió a `referencias/` (`Simulacion - Un enfoque práctico - Raul Coss Bu-<rango>.pdf`); esos PDFs de origen ya se borraron una vez terminada la conversión — el contenido vive solo en los `.tex`. Mismo estilo que cap4/cap5 (`report`, `[H]`), pero **sin** `\tableofcontents` (el usuario lo pidió y luego lo quitó porque no funcionaba bien) y **sin** portada de página completa (solo un encabezado compacto centrado: número de capítulo + título, sin repetir "Raúl Coss Bu / Simulación — Un enfoque práctico" en cada archivo — el usuario lo pidió quitar por ser redundante). Por instrucción explícita del usuario, se **omitieron** la portada y el índice/tabla de contenidos *del libro* (los enlaces de número de página del PDF no funcionan como hipervínculos reales en la conversión, así que no aportan nada). Figuras/diagramas del libro (curvas, árboles, diagramas de bloques GPSS) se representan como `\fbox{...}` con una descripción entre corchetes, no como imagen — no se extrajeron capturas de esas páginas. **Hueco de contenido conocido:** en `cap6_analisis_resultados.tex`, el PDF fuente (páginas del libro 94–104 del rango subido) saltaba de la página impresa 114 a la 117 — faltan las páginas 115–116 del libro (donde Coss Bu desarrolla $S_E^2$, $S_C^2$, $S_{EC}$ y las ecuaciones 6.14–6.27 antes de la fórmula final del intervalo de confianza). Queda marcado con una nota dentro del propio `.tex`; si se necesita ese desarrollo completo, habría que volver a escanear/subir esas dos páginas del libro.
- Solo `.tex` debe quedar trackeado al abrir `tex/`; el PDF compilado sigue generándose en `pdf/` para uso local, pero ya no se sube a git (ver `.gitignore` de la raíz del repo — `*.pdf` ignorado en todo el repositorio desde la limpieza de 2026-10-07). Todo lo demás (aux/log/fls/fdb_latexmk/toc/out) cae en `build/` y también está en `.gitignore`.
- `tex/.latexmkrc` fuerza esto (`out_dir = pdf`, `aux_dir = build`) sin importar si compilas desde VS Code o terminal.
- Convención de nombres `pN_<nombre_completo>` aplicada de forma consistente entre `tex/secciones/`, `tex/capturas_excel/`, `tex/capturas_pseint/` y `java/`:
  - `p1_transformada_inversa`
  - `p2_composicion`
  - `p3_inventario_simple` (Parte 2, sin espera del cliente) — completo: modelo, Java, validación, resultado (q≈60, R=12, costo≈$1,838)
  - `p4_inventario_con_espera` (Parte 2, con tiempo de espera del cliente) — completo: modelo, Java, validación, resultado (q≈180, R=115, costo≈$12,100)
- `java/` — implementaciones Java de cada problema (siempre Java, no otro lenguaje). Carpeta plana, sin subcarpetas `pN_.../` (se aplanó para subir a Classroom): `TransformadaInversa.java` (p1), `Composicion.java`/`SimularComposicion.java` (p2), `InventarioSimple.java` (p3), `InventarioConEspera.java` (p4).

## Archivos clave
- `tex/Apunte.tex` — fuente de verdad: métodos, notación, pasos, fórmulas
- `tex/Actividad1.tex` — enunciado original con los 4 problemas
- `tex/Informe.tex` — documento principal (incluye las secciones vía `\input`)
- `tex/secciones/p1_transformada_inversa.tex` — Parte 1, Problema 1
- `tex/secciones/p2_composicion.tex` — Parte 1, Problema 2
- `tex/secciones/p3_inventario_simple.tex` — Parte 2, Problema 1 (completo)
- `tex/secciones/p4_inventario_con_espera.tex` — Parte 2, Problema 2 (completo)

## Métodos del Apunte.tex
Existen 3 métodos: **Transformada Inversa (TI)**, **Rechazo** y **Composición**.
En Composición siempre se aplica TI internamente.

Parte 1 (sin cambios):
1. Transformada Inversa — `f(x) = (x-3)²/18`
2. Composición + TI — distribución triangular simbólica (a, b, c)

Parte 2 (cambió, ver `tex/Actividad1.tex`): 2 problemas de inventario (demanda diaria + tiempo de entrega, hallar cantidad óptima a ordenar `q` y punto de reorden `R`); el segundo agrega tiempo de espera del cliente. Método: Transformada Inversa discreta (como el Ejemplo 4.4 de `Teorema.tex`) sobre demanda/entrega/espera, combinada con la lógica de simulación día a día del Ejemplo 5.5 de `referencias/cap5_simulacion.tex` (estado = inventario, que evoluciona con costos de ordenar/inventario/faltante) y optimización de `(q,R)` probando varios pares.

**Corrección de datos en p4 (2026-08-31):** `Actividad1.tex` traía "Costo de ordenar" duplicado (\$50 y \$100) por error de transcripción — se dejó \$100/orden (coincide con el problema 5.11 original de Coss Bu). También se corrigieron a los valores del libro: costo de inventario \$52/unidad/año (no \$26) e inventario inicial 100 unidades (no 15). El usuario confirmó que el enunciado es copia del libro y que el único error real era el de costo de ordenar.

## Estructura obligatoria de cada sección (actualizada, apuntes de clase)
Se repite completa por cada problema (p1, p2, p3, p4). No agregar subsecciones fuera de estas, ni repetir contenido:

```
#.1 Introducción / antecedentes   (puede ser solo uno de los dos, o ambos)
#.2 Descripción del problema      (copia de Actividad1.tex)
#.3 Propuesta de solución         (aquí va el modelo: Pasos 1–4 del método, Modelamiento)
#.4 Objetivos                     (general + específicos; específicos: máximo 2)
#.5 Desarrollo de la solución     (código: Algoritmo/Paso 5, Diagrama de flujo, Tabla de
                                    simulación, Implementación Java/Excel)
#.6 Validación                    (Prueba de escritorio: n=25 tabla completa + n≥50 resumen)
#.7 Interpretación                (incluye Resultados: qué dieron las corridas, antes de
                                    interpretarlos — ambos ameritan su propio espacio aquí)
#.8 Conclusión
```

**Bibliografía: una sola para todo el Informe** (al final de `Informe.tex`, no una por problema — corregido, antes decía "por cada problema"). Referencias reales (ej. Coss Bu), no inventadas.

**Reemplaza** la estructura vieja de 7 partes (Descripción, Modelamiento, Algoritmo, Diagrama
de flujo, Tabla de simulación, Prueba de escritorio, Implementación) — ese contenido no se
pierde, se reubica dentro de los puntos 3, 5 y 6 de arriba.

**Recomendaciones del docente (aplican a toda sección):**
- No usar términos/técnicas que no se vieron en clase, apuntes o referencias — nada inventado.
- Los resultados de las corridas ameritan su propio espacio, separado de la interpretación
  (ver punto 7).
- Comentar código solo en los fragmentos importantes, no línea por línea.
- Bibliografía con libros/referencias reales (ej. Coss Bu), no inventadas.

## Tabla de Simulación vs Prueba de Escritorio

**Tabla de Simulación** — formato transpuesto (formato confirmado por el docente):
- **Primera columna** = nombre de variable o fórmula (ej. `R ← gcm()`, `x = 3+∛(54R-27)`)
- **Primera fila** = números de corrida (1, 2, 3, ..., n)
- **Celdas** = valor calculado de esa variable en esa corrida
- Muestra TODAS las corridas (n=10) en la misma tabla — "todo el recorrido"
- Para algoritmos con múltiples eventos (Maquinas, Equipo): cada evento puede ser una fila adicional

**Prueba de Escritorio**:
- Muestra **muchas corridas** (n=25 tabla completa, n≥50 resumen)
- Valida que el algoritmo produce resultados correctos
- Una fila = una corrida completa (resumen de sus salidas)

**Diferencia clave:** Tabla = todas las variables en n corridas (transpuesto). Prueba = resumen estadístico de muchas corridas.

## Reglas de notación (basadas en Apunte.tex)

**Variable matemática:** siempre `x`, nunca `t`. En el modelamiento (Pasos 1–4) toda función usa `x`: `f(x)`, `F(x)`, inversas. En algoritmo/Java se pueden usar nombres descriptivos (`td`, `tr`, `t_llegada`, etc.).

**Variable de integración:** usar `dx` y `[x]` cuando el integrando es constante (ej. `0.05`, `1/(b-a)`). El Apunte muestra: `\int_a^x \frac{1}{b-a}\,dx = \frac{1}{b-a}[x]_a^x`. Solo usar `t` como dummy variable si el integrando depende de la variable.

**Formato f(x) piecewise:**
```latex
f(x) = \begin{cases}
valor & ; \quad condición \\
0     & ; \quad \text{en otro caso}
\end{cases}
```

**Números aleatorios:** notación `$0 < R < 1$` y generación con `\text{gcm}()`. No usar `R \sim U(0,1)`.

**No inventar fórmulas:** si no está en `Apunte.tex`, no va en el informe.

**Prueba de escritorio:** solo n=25 (tabla completa) y n≥50 (resumen). No incluir n=10 — ese caso ya está cubierto en la Tabla de simulación.

**Conflicto de estilos:** si hay duda entre estilo personal y Apunte.tex, prevalece el Apunte.

## Entornos LaTeX disponibles en Informe.tex
- `\begin{mitabla}[H]{título}` — tablas estándar
- `\begin{formulabox}` — fórmulas destacadas
- `\begin{lstlisting}[caption={...}, language=Java]` — código Java (no existe un entorno `javabox` separado, pese a que estaba documentado acá)
- `\begin{lstlisting}[caption={...}, language=Pascal]` — pseudocódigo
- `\begin{landscape}...\end{landscape}` — tablas anchas (requiere `pdflscape`, ya incluido)

## Fallos / cosas a recordar
- **Nunca compilar con `pdflatex` suelto.** `pdflatex` no lee `.latexmkrc` y tira `.aux/.log/.fls/.pdf` en la raíz de `tex/`. Compilar siempre con `latexmk -pdf archivo.tex` (respeta `out_dir=pdf`, `aux_dir=build`).
- VS Code solo aplica `.vscode/settings.json` de la carpeta abierta como raíz de la ventana. Si se abre `taller_s/` (no `actividad1/`), la config de `actividad1/.vscode/settings.json` no aplica — por eso existe también `taller_s/.vscode/settings.json` con la misma config (recetas de latexmk, `autoBuild.run: onSave`). Si el auto-build en guardar deja de funcionar o vuelve a tirar basura en la raíz, revisar cuál `.vscode/settings.json` se está usando antes de tocar nada.
- Las carpetas se llaman `tex/` (antes `zLatex/`) y `tex/pdf/` (antes `tex/pdfs/`). Si alguna vez aparecen `zLatex/` o `pdfs/` de nuevo, es config vieja, no renombrar sin revisar `.gitignore`, `.latexmkrc` y `.vscode/settings.json` primero.
- No dejar líneas en blanco dobles/múltiples seguidas al editar `.tex` (ni otros archivos) — el usuario lo marcó como regla explícita.
- **Limpieza de repo (2026-10-07):** la carpeta de esta actividad se llamaba `act_1/`, se renombró a `actividad1/` para que el nombre sea consistente con `actividad2/` (la otra actividad del repo, carpeta hermana en la raíz). Si algo (un script, una nota vieja) todavía dice `act_1/`, está desactualizado. También ese día: se eliminaron `Actividad_1/` y `Actividad_1_grupal/` (carpetas de una entrega anterior, ya superadas, con `.class` compilados y otra basura commiteada), se agregó un `.gitignore` en la raíz del repo que ignora `*.pdf` y `.DS_Store` en todo el repositorio, y se reescribió el historial de git para sacar los PDFs viejos que estaban commiteados (con `force-push` al remoto — si clonaste este repo antes de esa fecha, hay que volver a clonarlo).
