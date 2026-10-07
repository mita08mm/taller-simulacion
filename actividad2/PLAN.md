# Plan — Actividad 2: Análisis de Mercado y Herramienta de Recolección de Datos

## 1. Resumen del plan

**Captura de datos:** formulario externo (Google Forms), uno para titulados y
otro para empleadores (ya transcritos en `actvidad2.tex`). Las 5 empresas
confirmadas con el docente se encuestan por este medio.

**Sistema propio (confirmado con el docente — no basta con Forms + Excel):**

- **Backend:** Java + Javalin. Lee el CSV exportado de Google Forms, calcula
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
5. ¿Validan los cruces que proponemos (financiamiento × posgrado deseado, tipo de organización × nivel de formación demandado), o tienen cruces específicos que quieren ver sí o sí?

### Sobre las Historias de Usuario / el sistema
6. "Sistema propio" — ¿incluye construir nosotros el formulario de captura (en vez de Google Forms), o el sistema propio empieza desde que se importan los datos ya capturados?
7. Para el dashboard (HU5: "mapas de calor y diagramas de radar") — ¿son obligatorios esos dos tipos de gráfico, o sirve cualquier visualización que comunique bien el cruce de variables?

### Sobre plazos
8. El enunciado original decía 13 de septiembre — ¿sigue vigente esa fecha o hay una nueva?
