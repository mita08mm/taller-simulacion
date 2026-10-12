# Glosario — Actividad 2

> Esto es solo para vos, para entender los términos que vamos usando. No es parte del informe.

## Estadística

**Empírico/a** — que viene de la observación directa de datos reales, no de una fórmula teórica o un supuesto. Ejemplo: si decimos que la distribución de "años hasta conseguir posgrado" es "empírica", significa que la sacamos de lo que la gente realmente respondió en la encuesta, no de una fórmula matemática que asumimos de antemano (como la distribución Normal).

**Estadística descriptiva** — resumir los datos que ya tenés, sin sacar conclusiones sobre nadie más que no haya respondido. Ejemplos: media, mediana, desviación estándar, frecuencias. Responde a "¿qué pasó en mi muestra?".

**Estadística exploratoria** — buscar patrones, relaciones o diferencias entre variables dentro de tus datos, para generar hipótesis (no para probarlas con certeza matemática todavía). Las tablas de contingencia y los cruces de variables entran acá.

**Estadística inferencial** — usar la muestra (lo que encuestaste) para sacar conclusiones sobre la población completa (todos los titulados, hayan respondido o no). El intervalo de confianza es inferencial: no solo describe tu muestra, intenta decir algo sobre todos los titulados.

**Media** — el promedio: sumar todos los valores y dividir entre cuántos hay.

**Mediana** — el valor del medio cuando ordenás todos los datos de menor a mayor. A diferencia de la media, no se deja "arrastrar" por un valor muy extremo (ej. un titulado con 30 años de egreso en una muestra de gente con 2-5 años).

**Desviación estándar** — qué tan dispersos (esparcidos) están los datos respecto a la media. Si es chica, los datos están todos parecidos entre sí; si es grande, hay mucha variedad.

**Variable numérica** — se mide con números que tienen orden y distancia real (ej. "años de egreso": 3, 5, 10). Se le puede calcular media/mediana/desviación.

**Variable categórica** — se mide con categorías, no con números que tengan una distancia matemática real (ej. "tipo de posgrado": Maestría, Doctorado, Ninguno). No tiene sentido calcularle una "media".

**Muestra** — el subconjunto de gente que efectivamente respondió la encuesta.

**Población** — el conjunto completo al que querés generalizar (todos los titulados de la Carrera, hayan respondido o no).

**Nivel de confianza / $\alpha$ (alfa)** — qué tan seguro querés estar de tu estimación. 95% de confianza = $\alpha=0.05$. Ya lo explicamos a fondo en `PLAN.md`, sección 2.

**Intervalo de confianza (IC)** — un rango (no un solo número) dentro del cual creemos que está el verdadero valor de la población, con el nivel de confianza elegido. Ejemplo nuestro: "la antigüedad de egreso promedio está entre 3.31 y 7.09 años, con 95% de confianza".

**Hipótesis nula ($H_0$)** — la afirmación que ponés a prueba, generalmente la versión "aburrida"/neutral (ej. "financiamiento y tipo de posgrado NO están relacionados"). La prueba estadística (como chi-cuadrada) te dice si hay evidencia para rechazarla o no.

**Chi-cuadrada ($\chi^2$ o $X_0^2$)** — una prueba que compara lo que observaste (`FO`, frecuencia observada) contra lo que esperarías si no hubiera relación entre dos variables (`FE`, frecuencia esperada). Si la diferencia es grande, hay evidencia de que las variables SÍ están relacionadas.

**Tabla de contingencia** — una tabla que cruza dos variables categóricas, mostrando cuántas respuestas caen en cada combinación (ej. filas = financiamiento, columnas = tipo de posgrado, celdas = cantidad de gente en esa combinación).

**Grados de libertad** — un número que depende de cuántas categorías tiene cada variable, usado para saber con qué valor de tabla comparar el resultado de chi-cuadrada. Para una tabla de contingencia: (filas − 1) × (columnas − 1).

**Frecuencia absoluta** — el conteo real (ej. "8 personas usaron recursos propios").

**Frecuencia relativa** — el conteo expresado como proporción o porcentaje del total (ej. "8 de 15 = 53%").

## Metodología / proyecto

**Pipeline** — una secuencia de pasos de procesamiento, uno después del otro (ej. recolectar datos → limpiarlos → calcular estadísticos → mostrarlos). Se usa mucho en análisis de datos.

**Data Wrangling** (a veces "limpieza de datos") — el trabajo de arreglar datos crudos/desordenados (valores faltantes, formatos inconsistentes, errores de tipeo) antes de poder analizarlos.

**Objetivo general vs. específico** — el general dice QUÉ se va a lograr en una sola oración amplia; los específicos son los pasos concretos (uno por acción) que, sumados, logran el general.

## Sistema / software

**Backend** — la parte del sistema que procesa datos y hace los cálculos, sin interfaz visual directa (en nuestro caso, el programa Java).

**Frontend** — la parte visual que ve el usuario en el navegador (en nuestro caso, el dashboard en Vue).

**API / API REST** — la forma en que el frontend le "pide" datos al backend por internet (una URL que devuelve información, en nuestro caso `http://localhost:7070/api/estadisticas`).

**JSON** — un formato de texto para representar datos estructurados (como un diccionario de pares clave-valor), es lo que nuestra API devuelve.

**Dashboard** — una pantalla que resume información (tablas, gráficos) de un vistazo.
