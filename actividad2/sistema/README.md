# Sistema — Observatorio de Titulados (Actividad 2)

Sistema propio que importa los datos exportados de las dos encuestas
(titulados por Microsoft Forms, empleadores por Google Forms, ambos como
CSV), calcula los indicadores y cruces definidos en `../PLAN.md` sección 5
(según los criterios ARCU-SUR) y los muestra en un dashboard. Incluye la
simulación de escenarios (Transformada Inversa discreta + generador
congruencial mixto).

## Estructura

```
sistema/
  backend/    Java puro (com.sun.net.httpserver, sin Maven ni dependencias)
  frontend/   Vue 3 + Vite + Naive UI (componentes) + ECharts (gráficos)
```

## Cómo correrlo

**Backend** (JDK 17 o superior):
```
cd backend
javac -d target/classes src/main/java/com/actividad2/*.java
java -cp target/classes:src/main/resources com.actividad2.Main
```
Sirve en `http://localhost:7070`. Al iniciar carga el dataset de prueba.

**Frontend** (en otra terminal):
```
cd frontend
pnpm install
pnpm run dev
```
Abrir `http://localhost:5173`. Si el backend corre en otra dirección,
definir `VITE_API_URL`.

## Historias de usuario → pantallas y endpoints

| HU | Pantalla | Endpoint | Criterio ARCU-SUR |
|---|---|---|---|
| 1 Antigüedad y empleabilidad | HU1 · Empleabilidad | `GET /api/empleabilidad` | 3.2.3 |
| 2 Formación continua | HU2–3 · Formación y financiamiento | `GET /api/formacion` | 2.4.1 / 1.1.5 |
| 3 Financiamiento | HU2–3 · Formación y financiamiento | `GET /api/financiamiento` | (cruce C7, opcional) |
| 4 Brechas en competencias | HU4 · Competencias (incluye radar) | `GET /api/competencias` | 3.2.1 / 2.1.2 / 2.1.7 |
| 5 Matrices cruzadas y exportación | HU5 · Matrices cruzadas (mapas de calor) e Importar/exportar | `GET /api/cruces`, `GET /api/exportar?que=…`, `POST /api/importar?tipo=…` | — |
| — | Empleadores | `GET /api/empleadores` | 2.1.2 / 2.4.2 |
| — | Simulación de escenarios | `GET /api/simulacion?corridas=&cohorte=&semilla=&ajusteEmpleo=&ajusteTiempo=` | 3.2.3 |

Otros: `GET /api/estado`, `GET /api/demografia`, `POST /api/restablecer`.

## Backend (`backend/src/main/java/com/actividad2/`)

| Archivo | Qué hace |
|---|---|
| `Main.java` | Servidor HTTP y endpoints |
| `Csv.java` | Lectura/escritura CSV (campos entre comillas) |
| `Dataset.java` | Respuestas de una encuesta; elimina email/teléfono/LinkedIn/correo al cargar |
| `Catalogo.java` | Códigos → etiquetas en el orden del formulario, agrupaciones de los cruces, punto medio de los rangos de tiempo, homologación de cargos |
| `Estadistica.java` | Media, desviación, IC 95% (cap6 6.1), frecuencias, chi-cuadrada de tabla de contingencia con valor teórico α = 0.05 |
| `Analisis.java` | Indicadores U1–U11 y cruces C1–C7 (PLAN.md 5.2–5.3) |
| `Simulacion.java` | Generador congruencial mixto, tablas de TI discreta, corridas base vs escenario |

Las decisiones de procesamiento sobre el formulario (punto medio de los
rangos, ramas que no responden una pregunta, escala de empleadores sin
media, etc.) están en `../PLAN.md` sección 5.6 y en comentarios del código.

## Generar los desarrollos del informe

`ReporteTex.java` escribe en `../tex/generado/` los desarrollos paso a paso
del informe (Paso 1 … Paso 6, con cada suma y división a la vista), usando
los mismos cálculos del sistema. `Informe.tex` los incluye con `\input`.

```
cd backend
javac -d target/classes src/main/java/com/actividad2/*.java
# con el dataset de prueba:
java -cp target/classes:src/main/resources com.actividad2.ReporteTex ../../tex/generado
# con los datos reales (CSV ya con los nombres cortos de columna):
java -cp target/classes:src/main/resources com.actividad2.ReporteTex ../../tex/generado titulados.csv empleadores.csv
# con fecha de corte (desde hasta, aaaa-mm-dd; "-" = sin límite):
java -cp target/classes:src/main/resources com.actividad2.ReporteTex ../../tex/generado titulados.csv empleadores.csv - 2026-10-31
```

Con datos reales, los fragmentos salen sin la marca **[Datos de prueba]**.
No editar a mano los archivos de `tex/generado/`: se sobrescriben.

## Filtro por fecha de respuesta

Cada respuesta guarda su fecha en la columna `fecha_respuesta`. Al
importar, el sistema la toma de «Hora de finalización» (Microsoft Forms)
o «Marca temporal» (Google Forms) y la convierte a `aaaa-mm-dd`; con
fechas tipo `03/04/2026` asume día/mes. Todos los endpoints GET y las
exportaciones aceptan `?desde=aaaa-mm-dd&hasta=aaaa-mm-dd`, y el
dashboard tiene el selector «Período de respuestas» en el menú lateral.
Las filas sin fecha quedan fuera cuando hay un período elegido.

## Importar datos reales

En la pantalla *Importar / exportar* se sube el CSV. Las columnas deben
llamarse como en los CSV de prueba (`backend/src/main/resources/`). Las
exportaciones de Microsoft Forms y Google Forms ponen como encabezado el
texto completo de la pregunta, así que antes de importar hay que
renombrar las columnas (una sola vez, en Excel) a esos nombres cortos.
Los CSV con respuestas reales **no se suben a git**.

## Dataset de prueba

Datos sintéticos (no son respuestas reales) con los campos exactos de los
instrumentos de `../actvidad2.tex`:

- `dataset_titulados.csv` — 100 filas.
- `dataset_empleadores.csv` — 20 filas, con nombres de empresa ficticios.
  Incluye cargos, las 3 preguntas abiertas y los 10 criterios de
  "Formación y perfil profesional".
