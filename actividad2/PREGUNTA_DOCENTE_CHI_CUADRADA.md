# Pregunta para el docente — fórmula de frecuencia esperada ($FE_{ij}$)

> No es parte del informe. Es para explicar de dónde sale la fórmula
> $FE_{ij} = \text{total fila} \times \text{total columna} / N$ que se usa
> en `tex/Informe.tex` (sección 2.5, Prueba de Independencia Chi-Cuadrada),
> y para llevarle la pregunta al docente tal cual.

## La idea en palabras simples

Tabla de ejemplo (financiamiento × tipo de posgrado, $N=30$ encuestados):

|                   | Diplomado | Especialidad | Maestría | Doctorado | Pos Doctorado | Total fila |
|-------------------|-----------|--------------|----------|-----------|----------------|------------|
| Recursos propios  | 2         | 1            | 4        | 1         | 0              | 8          |
| Beca total        | 1         | 2            | 3        | 1         | 1              | 8          |
| Beca parcial      | 3         | 2            | 5        | 2         | 2              | 14         |
| **Total columna** | 6         | 5            | 12       | 4         | 3              | **N = 30** |

Mirá la celda "Recursos propios — Diplomado": se observaron $FO=2$
personas ahí. La pregunta es: ¿cuántas esperaríamos ahí **si
financiamiento y tipo de posgrado no tuvieran ninguna relación**?

1. De los 30 encuestados en total, 8 usaron "Recursos propios". Eso es
   el $8/30 = 26.7\%$ de toda la muestra.
2. Si financiamiento y tipo de posgrado **no estuvieran relacionados**,
   ese mismo 26.7% debería repetirse dentro de cada grupo de posgrado —
   porque "no relacionado" significa justamente que la proporción de
   financiamiento es igual sin importar qué posgrado eligió la persona.
3. De los que eligieron "Diplomado" hay 6 personas. Si el 26.7% de
   ellas "debería" usar recursos propios (por el punto 2):
   $6 \times 0.267 = 1.6$ — y ese es el $FE=1.60$ que se usó en el
   ejemplo del informe.

Eso es exactamente lo mismo que la fórmula, solo que con los pasos
separados:

$$FE = \underbrace{6}_{\text{total columna}} \times \underbrace{\frac{8}{30}}_{\text{proporción de la fila}} = \frac{6\times8}{30} = \frac{\text{fila}\times\text{columna}}{N}$$

## De dónde sale (y de dónde NO sale)

- Se derivó aplicando la regla básica de probabilidad para eventos
  independientes: $P(A \text{ y } B) = P(A)\times P(B)$. No es una
  fórmula copiada de ningún archivo de `referencias/` ni del libro de
  Coss Bu.
- En `referencias/apuntes_simulacion.tex` y en
  `referencias/cap3_pruebas_estadisticas.tex` solo aparecen versiones
  para **una variable** ($FE=n/k$) o para probar uniformidad/
  independencia de **números aleatorios** ($FE=(n-1)/k^2$) — ninguna
  de las dos es literalmente esto, aunque esta fórmula se reduce a
  esas dos cuando las filas/columnas son uniformes (ver sección 2.5
  de `Informe.tex` para esa relación).
- El valor de tabla usado en el ejemplo ($X^2_{0.05,8}=15.51$) tampoco
  sale de ningún archivo del curso — es un valor estándar de una tabla
  de chi-cuadrada publicada (no tenemos ninguna transcrita en
  `referencias/`).

## Pregunta lista para llevar al docente

> "Profesor, para cruzar dos preguntas de la encuesta (ej.
> financiamiento y tipo de posgrado) y calcular la frecuencia esperada
> de cada combinación, asumí que si las dos variables no están
> relacionadas, la proporción de cada categoría de financiamiento
> debería mantenerse igual en todos los grupos de posgrado. Por eso
> calculé $FE_{ij} = (\text{total de la fila} \times \text{total de la
> columna}) / N$. ¿Es ese el procedimiento que debo usar para cruzar
> dos variables con chi-cuadrada, o usted enseña/prefiere otro
> método?"

Relacionado: ver también `PLAN.md` sección 3 (chi-cuadrada explicada a
detalle) y sección 4, pregunta 1 (la misma duda, ya estaba anotada ahí
antes de este archivo).
