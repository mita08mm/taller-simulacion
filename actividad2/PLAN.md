# Plan — Actividad 2: Análisis de Mercado y Herramienta de Recolección de Datos

## 1. Resumen del plan

**Captura de datos:** dos formularios externos distintos (ya transcritos en
`actvidad2.tex`) — titulados responden por Microsoft Forms
(forms.cloud.microsoft), empleadores por Google Forms. Las 5 empresas
confirmadas con el docente se encuestan por este segundo medio.

**Sistema propio (confirmado con el docente — no basta con Forms + Excel):**

- **Backend:** Java puro (`com.sun.net.httpserver`, sin Maven ni
  dependencias externas). Lee el CSV exportado de cada formulario, calcula
  las estadísticas (media, desviación, intervalo de confianza, chi-cuadrada)
  y expone los resultados como API REST (JSON).
- **Frontend:** Vue. Consume esa API y muestra tablas + gráficos (dashboard).

```
actividad2/
  sistema/
    backend/   (Maven, Java + Javalin)
    frontend/  (Vite + Vue)
```

**Para esta semana (avance a mostrar al docente):** el pipeline completo
funcionando de punta a punta sobre un dataset de prueba (10-15 filas
inventadas con la forma real de la encuesta), no con datos reales todavía —
para validar que el cálculo y el dashboard funcionan antes de tener las 5
encuestas completas.

**Base teórica (de `referencias/`, el libro de Coss Bu — *Simulación: un
enfoque práctico*):**
- Intervalo de confianza para una media — `referencias/cap6_analisis_resultados.tex`, sección 6.1 "Métodos de estimación".
- Estadístico chi-cuadrada ($X_0^2$) — `referencias/cap3_pruebas_estadisticas.tex`, sección 3.2 "Prueba de frecuencias".

---

## 2. Intervalo de confianza para una media — explicado a detalle

### 2.1. ¿Qué es el "nivel de confianza"?

Cuando calculamos un promedio (ej. "la antigüedad promedio de egreso es
6.3 años") a partir de una **muestra** (no de *todos* los titulados, solo de
los que respondieron la encuesta), ese promedio no es exacto — si
encuestáramos a otro grupo distinto de titulados, el promedio saldría
ligeramente distinto. El intervalo de confianza es la forma de decir
*"no sé el valor exacto, pero estoy bastante seguro de que el verdadero
promedio está entre estos dos números"*.

El **nivel de confianza** es qué tan seguro queremos estar de esa
afirmación. Si decimos "intervalo de confianza del 95%", significa: si
repitiéramos la encuesta 100 veces con muestras distintas, en 95 de esas
100 veces el intervalo que calculemos sí va a contener al verdadero
promedio de la población completa. El 5% restante se llama **nivel de
significado**, y se escribe con la letra griega $\alpha$ (alfa):

$$\alpha = 1 - \text{nivel de confianza}$$

Por ejemplo: confianza 95% $\Rightarrow \alpha = 0.05$. Confianza 90%
$\Rightarrow \alpha = 0.10$.

Mientras más alto el nivel de confianza que pidas (99% en vez de 95%), más
*ancho* (menos preciso) sale el intervalo — es el precio de estar más
seguro. Por eso en la pregunta para el docente preguntamos qué nivel
quieren (95% es el estándar más común, y es el que usa el libro en su
ejemplo — ver 2.3 abajo).

### 2.2. La fórmula completa (cap6, sección 6.1)

El libro (cap6, ecuación justo después de "lo cual sigue una distribución
$t$ con $n-1$ grados de libertad...") da esta fórmula, para cuando **no
conocemos la varianza real de toda la población** (que es siempre nuestro
caso — solo tenemos la muestra de la encuesta):

$$\bar{x} - \frac{S}{\sqrt{n}}\,t_{n-1,\alpha/2} \;\le\; \mu \;\le\; \bar{x} + \frac{S}{\sqrt{n}}\,t_{n-1,\alpha/2}$$

Variable por variable, qué significa cada una:

| Símbolo | Significado | En nuestro caso (ej. antigüedad de egreso) |
|---|---|---|
| $\mu$ | El promedio **real** de toda la población de titulados (lo que queremos estimar, nunca lo sabremos exacto) | "La antigüedad de egreso promedio de TODOS los titulados de la carrera" |
| $\bar{x}$ | El promedio de **la muestra** (lo que sí podemos calcular, con los datos que recolectamos) | El promedio de antigüedad de egreso de los titulados que SÍ respondieron la encuesta |
| $S$ | La desviación estándar de la muestra (qué tan dispersos están los datos) — se calcula con la ecuación (6.5) del libro: $S^2 = \frac{1}{n-1}\sum_{i=1}^n (X_i-\bar{x})^2$ | Qué tan parejos o dispersos son los años de egreso entre los encuestados |
| $n$ | Tamaño de la muestra (cuántas encuestas respondidas tenemos) | Número de titulados que llenaron el formulario |
| $t_{n-1,\alpha/2}$ | Un valor de tabla (distribución $t$ de Student), que depende de $n-1$ "grados de libertad" y de $\alpha$. Se busca en una tabla de la distribución $t$, no se calcula a mano. | Para $n=25$ y $\alpha=0.05$ (confianza 95%), $t_{24,0.025}\approx 2.064$ |
| $\frac{S}{\sqrt n}\,t_{n-1,\alpha/2}$ | El "margen de error" — cuánto se le suma y resta a $\bar{x}$ para formar el intervalo | |

### 2.3. Ejemplo resuelto (tal como lo trae el libro, Ejemplo 6.1)

El libro, para ilustrar, usa un caso de una Tasa Interna de Rendimiento
(TIR) simulada 100 veces, con $\bar{x}=13.37\%$, $S=6.71\%$, $n=100$,
confianza 95% ($\alpha=5\%$, $t\approx1.99$):

$$13.37\% \pm \frac{6.71}{\sqrt{100}}(1.99)\% = 13.37\% \pm 1.34\%$$

Intervalo resultante: $(12.03\%,\ 14.71\%)$, de ancho 2.68%.

**Aplicado a nuestro caso** (hipotético, con datos inventados para que
veas la mecánica — esto lo recalculamos con datos reales cuando tengamos
las encuestas):

Si encuestamos $n=20$ titulados y la antigüedad de egreso promedio sale
$\bar{x}=6.3$ años con $S=3.1$ años, y pedimos 95% de confianza
($t_{19,0.025}\approx 2.093$):

$$6.3 \pm \frac{3.1}{\sqrt{20}}(2.093) = 6.3 \pm 1.45$$

Intervalo: $(4.85,\ 7.75)$ años. Interpretación: "con 95% de confianza, la
antigüedad de egreso promedio de todos los titulados (no solo los
encuestados) está entre 4.85 y 7.75 años".

### 2.4. Por qué importa para el tamaño de muestra

El libro también señala algo útil para justificarle al docente cuántas
encuestas van a levantar: el ancho del intervalo es

$$\frac{2S\,t_{n-1,\alpha/2}}{\sqrt{n}}$$

Como $n$ está dividiendo dentro de una raíz, para reducir el intervalo a
la mitad hace falta **cuadruplicar** $n$ (el libro lo dice explícito:
"para reducir el intervalo de confianza a la mitad, es necesario... aumentar
cuatro veces el número de observaciones"). Esto es un buen argumento para
explicarle al docente por qué con pocas encuestas (ej. 10-15) el intervalo
va a salir bastante ancho/impreciso, y por qué conviene juntar la mayor
cantidad posible de respuestas.

---

## 3. Chi-cuadrada — explicado a detalle (y la parte que NO está en el libro)

### 3.1. La versión que sí trae el libro (cap3, sección 3.2)

$$X_0^2 = \sum_{i=1}^{n} \frac{(FO_i - FE_i)^2}{FE_i}$$

Variable por variable (tal como las define el libro):

| Símbolo | Significado según el libro | Nota |
|---|---|---|
| $FO_i$ | "Frecuencia observada del $i$-ésimo subintervalo" | cuántos datos reales cayeron en ese grupo $i$ |
| $FE_i$ | "Frecuencia esperada del $i$-ésimo subintervalo $(N/n)$" | cuántos datos *deberían* caer ahí si la hipótesis (uniformidad) fuera cierta |
| $N$ | Tamaño de la muestra | total de datos |
| $n$ | Número de subintervalos | en cuántos grupos se dividió el rango de la variable |

**Importante:** en el libro esto se usa para probar que **una sola
variable** (los números pseudoaleatorios) se reparte uniformemente en $n$
tramos — por eso $FE_i = N/n$ (si son $N$ datos repartidos parejo entre
$n$ grupos, cada grupo "debería" tener $N/n$). El ejemplo del libro: $n=5$
subintervalos, $N=100$ datos, entonces $FE_i=100/5=20$ en cada uno, y
compara contra lo que realmente cayó ($FO_i$ = 21, 22, 19, 23, 15).

Esto se compara contra un valor de tabla $X^2_{\alpha,(n-1)}$ (chi-cuadrada
con $n-1$ grados de libertad) — si $X_0^2$ sale **menor**, no se rechaza la
hipótesis (en nuestro caso: no se rechaza que las dos variables son
independientes — ver abajo).

### 3.2. Lo que nosotros necesitamos — y por qué NO es exactamente lo mismo

Nuestro caso no es "¿una variable se reparte uniforme en $n$ grupos?". Es:
**¿dos variables categóricas están relacionadas entre sí, o son
independientes?** (ej.: ¿la fuente de financiamiento influye en qué tipo
de posgrado eligen, o no tiene nada que ver?). Esto se llama **prueba de
independencia (tabla de contingencia)**, y usa la misma fórmula general
pero con $FO$ y $FE$ redefinidos para **dos dimensiones** en vez de una:

$$X_0^2 = \sum_{i=1}^{k}\sum_{j=1}^{m} \frac{(FO_{ij} - FE_{ij})^2}{FE_{ij}}$$

| Símbolo | Significado en nuestro caso |
|---|---|
| $k$ | Número de categorías de la variable A (ej. "fuente de financiamiento": recursos propios / beca total / beca parcial → $k=3$) |
| $m$ | Número de categorías de la variable B (ej. "tipo de posgrado deseado": Maestría / Especialidad / Doctorado → $m=3$) |
| $FO_{ij}$ | Cuántos encuestados cayeron **a la vez** en la categoría $i$ de A y categoría $j$ de B (la celda $(i,j)$ de la tabla de contingencia) |
| $FE_{ij}$ | Cuántos *se esperarían* en esa celda **si A y B fueran independientes** |
| $FE_{ij} = \dfrac{(\text{total de la fila } i)\times(\text{total de la columna } j)}{N}$ | Esta es la parte que reemplaza al $N/n$ del libro — ya no es "parejo entre $n$ grupos", es "proporcional a cuánta gente hay en cada fila y cada columna" |
| $N$ | Total de encuestados (suma de toda la tabla) |

Grados de libertad para buscar en la tabla de $X^2$: $(k-1)(m-1)$ — en vez
de $(n-1)$ como en el caso de una sola variable.

**Ejemplo con números chicos para que se entienda la mecánica** (inventado,
no son datos reales): supongamos 40 encuestados, cruzando "financiamiento"
(3 categorías) × "tipo de posgrado" (2 categorías: Maestría / Otro):

| | Maestría | Otro | **Total fila** |
|---|---|---|---|
| Recursos propios | 10 | 5 | 15 |
| Beca total | 8 | 2 | 10 |
| Beca parcial | 12 | 3 | 15 |
| **Total columna** | 30 | 10 | **N = 40** |

La celda "Recursos propios × Maestría" tiene $FO_{11}=10$. Su esperada,
asumiendo independencia:

$$FE_{11} = \frac{(\text{fila } 1)(\text{columna } 1)}{N} = \frac{15 \times 30}{40} = 11.25$$

Y así para cada una de las $3\times2=6$ celdas, sumando
$(FO_{ij}-FE_{ij})^2/FE_{ij}$ en cada una para obtener $X_0^2$, que luego se
compara contra $X^2_{\alpha,(3-1)(2-1)} = X^2_{\alpha,2}$ de tabla.

### 3.3. Por qué esto hay que preguntárselo al docente

La fórmula de la sección 3.1 (una variable, $FE_i=N/n$) **sí** está
textual en el capítulo que tenemos. La de la sección 3.2 (dos variables,
tabla de contingencia, $FE_{ij}=\text{fila}\times\text{columna}/N$) **no
está escrita en ese capítulo** — es la extensión estándar de la misma
idea que se enseña en cualquier curso de estadística, pero no es un
copy-paste del libro de Coss Bu. Como en Actividad 1 la regla fue "no
inventar fórmulas, si no está en la referencia no va", hay que
preguntarle directamente al docente si para Actividad 2 pueden usar esta
extensión (es información real y estándar, no inventada, solo que no
viene en este capítulo específico) o si prefiere que se restrinjan
estrictamente a la versión de una variable.

### 3.4. Regla práctica a confirmar también

El libro menciona (en la prueba de la distancia, sección 3.3) que cuando
una categoría tiene muy pocos datos esperados, hay que agruparla con las
vecinas para que ninguna celda quede con frecuencia esperada menor a 5 —
esto aplica igual acá: si "rubro de la organización" tiene 14 categorías
posibles y solo encuestamos 5 empresas, casi todas las celdas van a tener
$FE_{ij}<5$. Hay que preguntarle al docente si agrupamos categorías poco
frecuentes como "Otros", o qué criterio prefiere.

---

## 4. Preguntas para el docente (resumen, para llevar a la reunión)

### Sobre las fórmulas / variables
1. La fórmula de tabla de contingencia ($FE_{ij}=\text{fila}\times\text{columna}/N$, sección 3.2 de este documento) no está literal en el capítulo 3 del libro — ¿la podemos usar igual?
2. ¿Qué nivel de confianza esperan para los intervalos de confianza — 95%, 90%? (ver sección 2.1)
3. Con pocas respuestas por categoría, ¿agrupamos como "Otros" para que $FE_{ij}\ge5$, o prefieren otro criterio? (ver sección 3.4)
4. "Tiempo hasta primer empleo" es una pregunta de rangos (ej. "entre 1-4 meses"), no un número exacto — para calcular una media hay que convertir cada rango a su punto medio. ¿Está bien esa aproximación, o la tratamos solo como variable categórica (sin media, solo frecuencias)?

### Sobre las variables a cruzar
5. ¿Validan los cruces que proponemos, o tienen cruces específicos que quieren ver sí o sí? **(Reemplazado por la sección 5, cruces C1–C6 basados en ARCU-SUR; la lista de abajo es la propuesta anterior.)**
   - Titulados: financiamiento × tipo de posgrado (objetivo 3); situación
     laboral × satisfacción con la formación (objetivo 2); sector
     público/privado × pertinencia del puesto (objetivo 2); género × área
     de interés en posgrado (objetivo 2).
   - Empleadores: tipo de organización × nivel de formación demandado
     (objetivo 4); rubro × probabilidad de contratación futura (objetivo
     4); tamaño de organización × contrató en últimos 5 años (objetivo 4).

### Sobre las Historias de Usuario / el sistema
6. "Sistema propio" — ¿incluye construir nosotros el formulario de captura (en vez de Google Forms), o el sistema propio empieza desde que se importan los datos ya capturados?
7. Para el dashboard (HU5: "mapas de calor y diagramas de radar") — ¿son obligatorios esos dos tipos de gráfico, o sirve cualquier visualización que comunique bien el cruce de variables?

### Sobre la base de datos y las fechas (agregadas 2026-10-12)
9. ¿El sistema debe guardar las respuestas en una base de datos propia que acumule el histórico, o basta con importar el CSV cada vez? (El filtro por fecha ya está implementado sobre el CSV importado; falta decidir la persistencia.)
10. ¿El formulario queda abierto permanentemente o se aplica por períodos (gestión/semestre)? ¿Qué define un dato "nuevo": fecha de respuesta o período de aplicación?
11. ¿Se puede agregar "año de titulación" al formulario? Sin ella no hay análisis por cohorte (criterio 3.2.1). Va contra la decisión de no modificar el formulario: preguntarlo explícitamente.
12. Si un titulado responde dos veces, ¿se reemplaza o se guardan ambas? Para reconocerlo hace falta un identificador (el email, que hoy se borra).
13. ¿La encuesta es solo para titulados o también para egresados no titulados?
14. ¿El informe se cierra con una fecha de corte?

### Sobre plazos
8. El enunciado original decía 13 de septiembre — ¿sigue vigente esa fecha o hay una nueva?

---

## 5. Variables y cruces según los criterios ARCU-SUR

> Fuente: *Criterios de Calidad para la acreditación ARCU-SUR — Ingeniería*
> (mayo 2019). Los números (3.2.3, 2.1.2, etc.) son los criterios del
> documento; lo que va entre comillas son sus **indicadores**, copiados
> literalmente. La *Guía de Autoevaluación* (2016) y la *Guía de Pares*
> (2016) no agregan indicadores nuevos: repiten los mismos componentes y
> piden "redactar un texto que refleje la aplicación de cada uno de los
> criterios" con "aspectos favorables y desfavorables". Además, el propio
> documento de Criterios, en "Documentación y fuentes de información", pide
> presentar "Encuestas aplicadas para la autoevaluación" e "Información
> sobre mecanismos de seguimiento de graduados": este estudio es
> exactamente eso.
>
> Criterio de selección: solo entra una variable o un cruce si responde
> a un indicador concreto de ARCU-SUR **y** sale de una pregunta real del
> formulario (decisión del 2026-10-12, ver `CLAUDE.md`). Los nombres de
> columna son los de los CSV de `sistema/backend/src/main/resources/`.

### 5.1. Mapa criterio ARCU-SUR → preguntas del formulario

| Criterio ARCU-SUR | Indicador (literal) | Columnas del formulario |
|---|---|---|
| **3.2.3 Condiciones de empleo** | "Tiempo medio para obtener el primer empleo." | `tiempo_primer_trabajo` |
| 3.2.3 | "Destino laboral y profesional de los graduados; tasa de empleo." | `situacion_laboral`, `rubro`, `sector_publico_privado`, `departamento_bolivia`, `area_organizacion`, `cargo`, `remuneracion_promedio`, `anios_vida_profesional`, `anios_desempleado` |
| 3.2.3 | "Concordancia entre las características de la titulación y las del empleo." | `likert_pertinencia_puesto`, `likert_concordancia_requerimientos` |
| **3.2.1 Resultados** | "Debe evaluarse el resultado del proceso formativo y utilizar dicha evaluación para realizar los ajustes correspondientes." (criterio) | `likert_satisfaccion_global_formacion`, `aspectos_utilidad`, `aspectos_mejorar` |
| **3.2.2 Vinculación y seguimiento a los graduados** | "Mecanismos de seguimiento a los graduados." / "Participación de los graduados en instancias de asesoramiento o decisión de la carrera." | `red_contactos_interes` (la encuesta misma es el mecanismo de seguimiento) |
| **2.1.2 Perfil de Egreso** | "Coherencia entre el perfil de egreso y la demanda explícita de competencias profesionales y otras capacidades expresadas por agentes sociales relevantes…" | Empleadores: escala de "Formación y perfil profesional" (perfil coherente con desempeño, competencias laborales, valores), cargos que ocupan, preguntas abiertas de competencias y herramientas |
| 2.1.2 b) (competencias generales) | "…actuar con espíritu emprendedor, creativo e innovador." | Rama emprendimiento: `origen_emprendimiento`, `likert_importancia_formacion_negocio` |
| **2.1.7 Actualización curricular** | "…mecanismos permanentes y sistemáticos de actualización del currículo, en consonancia con los desarrollos disciplinares y profesionales." (criterio) | `asignaturas_ventaja`, `asignaturas_poco_utiles`, `aspectos_mejorar`; empleadores: tecnologías y tendencias emergentes (texto) |
| 2.1.3 Actividades integradoras | "La vinculación con entidades o empresas, por medio de pasantías u otros mecanismos…" (criterio) | `aspectos_utilidad` (opción pasantías), `aspectos_mejorar` (opción relacionamiento con empresas); empleadores: Likert "considera nuestra opinión… pasantías y modalidades de titulación" |
| **2.4.1 Cursos de actualización profesional permanente** | "Programas de formación de posgrado en disciplinas afines a la carrera." | `formacion_complementaria`, `programa_nivel`, `programa_donde`, `interes_posgrado`, `area_interes_posgrado`, `organizacion_interes_posgrado`, `likert_oferta_posgrado_fcyt` |
| **1.1.5** (posgrado) | "Programas para promoción de estudios de posgrado (Doctorados, Maestrías y Especialidades) dentro y fuera de la institución." | `programa_donde`, `organizacion_interes_posgrado`, `programa_financiamiento` |
| **2.4.2 Relaciones con el sector público y privado** | "Resultados obtenidos bajo convenios en los últimos 5 años." / "Convenios vigentes." | Empleadores: `tipo_organizacion`, `contrato_ultimos_5_anios`, `probabilidad_contratacion`, Likert "las autoridades consultan regularmente nuestra opinión" |

Lo que **no** cubre ningún indicador de ARCU-SUR y queda solo como dato de
caracterización (frecuencias, sin cruce propio): `edad`, `genero`,
`medio_obtuvo_trabajo`, `antiguedad_trabajo`, `cantidad_empleos`,
`razon_no_trabaja`. `programa_financiamiento` solo tiene una relación
indirecta: el criterio 1.5.1 (becas) habla de **alumnos y docentes**, no
de graduados, así que no se puede presentar como evidencia de 1.5.1.

### 5.2. Indicadores univariados (qué se calcula de cada uno)

| # | Indicador | Cálculo | Criterio |
|---|---|---|---|
| U1 | **Tasa de empleo** | proporción de `situacion_laboral` = trabajando + emprendimiento sobre el total | 3.2.3 |
| U2 | **Tiempo medio hasta el primer empleo** | media e IC 95% de `tiempo_primer_trabajo`, cada rango reemplazado por su punto medio | 3.2.3 (pide literalmente "tiempo medio") |
| U3 | Años de vida profesional y años desempleado | media, $S$ e IC 95% (variables numéricas reales del formulario) | 3.2.3 |
| U4 | Destino laboral | frecuencias de `rubro`, `sector_publico_privado`, `departamento_bolivia`, `cargo`, `remuneracion_promedio` | 3.2.3 |
| U5 | Concordancia titulación–empleo | frecuencias + media de las escalas `likert_pertinencia_puesto` y `likert_concordancia_requerimientos` (1–4) | 3.2.3 |
| U6 | Satisfacción con la formación | frecuencias + media de `likert_satisfaccion_global_formacion`; frecuencias de `aspectos_utilidad` / `aspectos_mejorar` | 3.2.1 |
| U7 | Ranking de asignaturas | frecuencia de cada asignatura en `asignaturas_ventaja` y en `asignaturas_poco_utiles` | 2.1.7 |
| U8 | Demanda de posgrado | % con `formacion_complementaria` = SI; % `interes_posgrado` = SI; frecuencias de `area_interes_posgrado` y `organizacion_interes_posgrado` | 2.4.1 / 1.1.5 |
| U9 | Percepción de la oferta de posgrado FCyT | frecuencias + media de `likert_oferta_posgrado_fcyt` | 2.4.1 |
| U10 | Seguimiento de graduados | % `red_contactos_interes` = SI | 3.2.2 |
| U11 | Opinión de empleadores sobre el perfil | frecuencia de cada respuesta en los 10 ítems de "Formación y perfil profesional" | 2.1.2 / 2.4.2 |

U2 resuelve la pregunta 4 de la sección 4 a favor del punto medio: si el
indicador oficial es un "tiempo **medio**", hace falta una media. Igual
conviene confirmarlo con el docente.

### 5.3. Cruces (tablas de contingencia + chi-cuadrada)

Solo titulados. Cada cruce responde una pregunta que un par evaluador
haría al leer el criterio.

| # | Cruce | Pregunta que responde | Criterio |
|---|---|---|---|
| C1 | `situacion_laboral` × `likert_satisfaccion_global_formacion` | ¿Quienes no trabajan están menos satisfechos con la formación? | 3.2.1 / 3.2.3 |
| C2 | `sector_publico_privado` × `likert_pertinencia_puesto` | ¿La concordancia titulación–empleo cambia entre sector público y privado? | 3.2.3 |
| C3 | `formacion_complementaria` × `remuneracion_promedio` | ¿Haber hecho posgrado se asocia con mejor remuneración? (justifica 2.4.1) | 2.4.1 / 3.2.3 |
| C4 | `interes_posgrado` × `likert_oferta_posgrado_fcyt` | ¿Los interesados en posgrado ven afín la oferta de la FCyT? | 2.4.1 / 1.1.5 |
| C5 | `area_interes_posgrado` × `organizacion_interes_posgrado` | ¿En qué áreas la FCyT pierde demanda frente a otras universidades? | 2.4.1 / 1.1.5 |
| C6 | `likert_concordancia_requerimientos` × `tiempo_primer_trabajo` | ¿Quienes ven la formación alineada al mercado consiguieron trabajo antes? | 3.2.3 |

Notas:
- **Agrupar categorías:** con escalas de 4 niveles y pocas respuestas,
  casi seguro hay que juntar "Totalmente en desacuerdo + En desacuerdo" y
  "De acuerdo + Totalmente de acuerdo" para que $FE_{ij}\ge5$ (pregunta 3
  de la sección 4). Lo mismo con remuneración (ej. ≤ Bs. 5.000 / > Bs. 5.000).
- **El cruce anterior "financiamiento × tipo de posgrado" sale de la lista
  principal:** con el formulario real solo existiría "financiamiento ×
  nivel del programa ya cursado", sobre el subgrupo con
  `formacion_complementaria` = SI (chico), y no corresponde a ningún
  indicador ARCU-SUR (ver la nota sobre 1.5.1 en 5.1). Queda como opcional.
- **Empleadores = solo descriptivo.** Con 5 empresas no se puede hacer
  chi-cuadrada ni intervalo de confianza: todas las celdas tendrían
  $FE<5$. Se presentan las frecuencias tal cual (U11), empresa por empresa
  si hace falta, y se dice explícitamente en el informe por qué.

### 5.4. Qué entra en la simulación

Los dos indicadores literales de 3.2.3 son los candidatos naturales para
simular con Transformada Inversa discreta (tabla de probabilidades
acumuladas a partir de las frecuencias observadas, como en p3/p4 de
Actividad 1):
- **Tiempo hasta el primer empleo** (U2): generar cohortes simuladas de
  titulados y estimar el tiempo medio con su IC.
- **Tasa de empleo** (U1): escenario base frente a escenarios modificados
  (ej. más titulados con pasantías o con posgrado), para ver cuánto
  cambiaría el indicador.

### 5.5. Huecos detectados al hacer este mapa

- `dataset_empleadores.csv` (prueba) solo tiene 10 columnas: le faltan
  cargos, las 3 preguntas abiertas y los 10 ítems de "Formación y perfil
  profesional", que son justamente lo que responde a 2.1.2. Hay que
  agregarlos al dataset de prueba y al lector del backend.
- El formulario de titulados no tiene año de egreso ni cohorte, así que
  no se puede calcular nada de 3.2.1 "relación entre ingresantes y
  graduados por cohorte": eso sale de registros de la carrera, no de la
  encuesta. Mencionarlo como límite del estudio.

### 5.6. Decisiones de análisis sobre el formulario tal como está

> El formulario **no se modifica** y solo se envía a titulados de
> Ingeniería de Sistemas de la UMSS (decisión del usuario, 2026-10-12).
> Por eso cada problema detectado en la revisión se resuelve como una
> decisión de procesamiento (que se describe en el Marco Metodológico) o
> como una limitación declarada en el informe, no cambiando preguntas.

**Titulados**

| # | Situación en el formulario | Decisión |
|---|---|---|
| T1 | No hay pregunta filtro de "¿es titulado de la UMSS?" | No hace falta: la distribución es solo a titulados UMSS. Decirlo en "Población objetivo" y "Técnicas de recolección". |
| T2 | No hay año de titulación ni cohorte | Limitación declarada: no se segmenta por cohorte ni se calcula nada de 3.2.1 por cohorte. |
| T3 | `tiempo_primer_trabajo` solo lo responde la rama "trabajando en una organización" | U2 se calcula solo con quienes la respondieron, informando ese $n$. Se declara que el tiempo medio excluye a emprendedores y a quienes hoy no trabajan. |
| T4 | Punto medio de `tiempo_primer_trabajo` | Ya trabajaba antes de titularse = 0; Menos de 1 mes = 0.5; 1–4 = 2.5; 4–8 = 6; 8–12 = 10 (meses). "Más de 12 meses" no tiene límite superior: se toma 12 y se aclara que la media resultante es una cota inferior. Además de la media se reporta la categoría más frecuente. **Confirmar con el docente.** Los bordes que se pisan (4 en "1–4" y en "4–8") no afectan el cálculo: se usa la opción que la persona eligió. |
| T5 | `primer_empleo` también se le pregunta a quien "Actualmente NO trabajo" | Para esa rama la respuesta se descarta (se trata como vacía). |
| T6 | "Años de vida profesional" no dice desde cuándo; desempleo en años | Se usan los valores tal como se declararon (se aceptan decimales). Se descartan filas inconsistentes (`anios_desempleado` > `anios_vida_profesional`) y se informa cuántas fueron. |
| T7 | La pregunta de satisfacción usa una escala de acuerdo | En el informe se nombra como "grado de acuerdo con estar satisfecho con la formación". Codificación 1 = Totalmente en desacuerdo … 4 = Totalmente de acuerdo. |
| T8 | `situacion_laboral` tiene solo 3 opciones | Tasa de empleo (U1) = trabajando en organización + emprendimiento propio. Limitación: no distingue freelance ni quien trabaja y emprende a la vez. |
| T9 | Rubros y cargos distintos entre titulados y empleadores | Se arma una tabla de homologación propia (ej. soporte / técnico-desarrollo / analista-especialista / gestión-jefatura / otro) y se compara solo a ese nivel agrupado. Va como anexo del informe. |
| T10 | Financiamiento sin "empleador", "crédito" ni "Otro"; nivel sin "doble titulación" | Se reportan las opciones tal cual. Es el cruce opcional de 5.3. |
| T11 | Áreas de posgrado sin "Otro" | Frecuencias tal cual; limitación: no capta interés fuera de las 5 áreas. |
| T12 | Asignaturas: lista cerrada y corta | El ranking (U7) se presenta como "entre las asignaturas ofrecidas en el formulario". |
| T13 | Edad con rangos 15–18 y 19–22 | Frecuencias tal cual. Para cruces se agrupa: ≤ 26 / 27–34 / ≥ 35. |
| T14 | Likert sin "No aplica" | Respuesta vacía = dato faltante; cada indicador informa su $n$ efectivo. |
| T15 | Remuneración solo para quien trabaja en una organización | C3 y U4 se calculan solo sobre esa rama, informando el $n$. |

**Empleadores**

| # | Situación en el formulario | Decisión |
|---|---|---|
| E1 | Escala desbalanceada (dos niveles de acuerdo y uno de desacuerdo, más "No sabe") | No se calcula media. Solo frecuencia y porcentaje de cada opción, y "No sabe" se reporta aparte. Se declara el sesgo de la escala como limitación. |
| E2 | Ítems compuestos (competencias, valores, "profesional y formación integral") | Se interpretan como valoración global del grupo; no se concluye sobre una competencia aislada. |
| E3 | Promete anonimato pero pide correo y nombre de empresa | El enunciado exige individualizar las empresas: se listan en la metodología (con su tipo público/privado), pero los resultados no se atribuyen a una empresa con nombre. |
| E4 | Tres preguntas abiertas | Se agrupan a mano en categorías (codificación) y después se cuentan frecuencias; el procedimiento se describe en el Marco Metodológico. |
| E5 | "Contrató en los últimos 5 años" es solo SI/NO | Se reporta como proporción de empresas; no hay cantidad de contratados. |

**Datos personales:** antes de importar al sistema se eliminan `email`,
`telefono`, `linkedin` (titulados) y `correo` (empleadores). Los CSV con
respuestas reales no se suben a git.
