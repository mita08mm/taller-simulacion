# Sistema — Observatorio de Titulados (Actividad 2)

Sistema propio para recolectar y analizar las encuestas a titulados y a
empleadores de Ingeniería de Sistemas (UMSS). Recibe respuestas por su propio
formulario o importa los CSV de Microsoft Forms (titulados) y Google Forms
(empleadores), las guarda en PostgreSQL y responde a los criterios ARCU-SUR
con indicadores, cruces de variables y reportes.

## Estructura

```
sistema/
  backend/    Java 21 puro (com.sun.net.httpserver, sin Maven) + PostgreSQL (driver en backend/lib/)
  frontend/   Vue 3 + Vite + Naive UI (componentes) + ECharts (gráficos)
```

## Cómo correrlo

**1. Base de datos (una sola vez).** Requiere PostgreSQL (probado con la versión 18):
```
psql -U postgres -h localhost -c "CREATE ROLE actividad2 LOGIN PASSWORD 'una_clave_segura';"
psql -U postgres -h localhost -c "CREATE DATABASE actividad2 OWNER actividad2;"
```
Crear `backend/db.properties` (no se sube a git):
```
url=jdbc:postgresql://localhost:5432/actividad2
user=actividad2
password=una_clave_segura
```
Al iniciar, el backend crea las tablas (`src/main/resources/schema.sql`) y carga
la estructura de las dos encuestas (`Semilla.java`) si todavía no existe.

**2. Backend:**
```
cd backend
javac -cp lib/postgresql-42.7.5.jar -d target/classes src/main/java/com/actividad2/*.java
java -cp target/classes:src/main/resources:lib/postgresql-42.7.5.jar com.actividad2.Main
```
Sirve en `http://localhost:7070` (otro puerto: `PUERTO=7071 java ...`). Si sale
`Address already in use`, ya hay un backend corriendo en ese puerto.

**3. Frontend** (en otra terminal):
```
cd frontend
pnpm install
pnpm run dev
```
Panel: `http://localhost:5173`. Formulario público:
`http://localhost:5173/#/encuesta/titulados` y `…/#/encuesta/empleadores`.
Para usar otro backend: `VITE_API_URL=http://localhost:7071 pnpm run dev`.

**4. Cargar las respuestas reales** (una vez, o cada vez que haya un CSV nuevo de Forms):
en el panel, *Administración → Importar y exportar*, arrastrar el CSV tal como lo
exporta Forms. O por consola:
```
cd backend
# simular (muestra qué texto se tradujo a qué código y qué no se reconoció; no guarda nada):
java -cp target/classes com.actividad2.FormsImport titulados "archivo.csv"
# guardar en la base (reemplaza la importación anterior de esa encuesta, no duplica):
java -cp target/classes:src/main/resources:lib/postgresql-42.7.5.jar com.actividad2.FormsImport titulados "archivo.csv" --guardar
```
Los CSV reales tienen datos personales: se guardan en `backend/src/main/resources/`
y están en `.gitignore`. **No se suben a git.**

## Panel

| Menú | Pantalla | Qué muestra |
|---|---|---|
| Inicio | Respuesta a los criterios ARCU-SUR | Una tarjeta por criterio con sus aspectos favorables |
| Criterios ARCU-SUR | 2.1.2, 2.1.7, 2.4.1, 2.4.2, 3.2.1, 3.2.2, 3.2.3 | Qué pide el criterio, valoración (favorable si > 50%), 3 indicadores, gráficos, relaciones entre preguntas y "Más detalle" |
| Herramientas | Comparar dos preguntas | Tabla cruzada, mapa de calor y conclusión de la prueba chi-cuadrada en una frase |
| Herramientas | Generar reporte | Reporte a medida: encuesta, período (gestión, semestre, mes, fechas) y preguntas; se imprime o guarda en PDF y se exporta a Excel |
| Administración | Preguntas de la encuesta | Editor de secciones, preguntas y opciones |
| Administración | Respuestas | Enlaces del formulario, listado y detalle, revisión de calidad (casi vacías, nombres sin sentido, duplicados) |
| Administración | Importar y exportar | Carga de CSV de Forms y descarga de datos y tablas cruzadas |

La barra superior tiene el filtro de período: todos los indicadores se recalculan
solo con las respuestas de ese período. Cada gráfico tiene un botón para bajarlo
como imagen PNG. Cada pantalla tiene su propia dirección (`#/empleo`, `#/perfil`, ...).

## Base de datos

PostgreSQL con un diseño mixto (justificación en el informe):

| Tabla | Contenido |
|---|---|
| `encuesta`, `seccion`, `pregunta`, `opcion` | Estructura de las encuestas, relacional con claves foráneas. La edita el panel. |
| `respuesta` | Una fila por persona; sus respuestas en la columna `datos` (JSONB): `{"codigo_pregunta": valor o [valores]}`. Guarda `origen` (formulario o importado), `fecha` y la `version` de la encuesta respondida. |
| `contacto` | Email, teléfono y LinkedIn, separados de las respuestas; nunca entran al análisis. |

Reglas: una pregunta u opción con respuestas no se borra (se desactiva) y no se
le cambia el código ni el tipo; cada cambio sube la versión de la encuesta.

## Servicios (API)

Todos los GET de análisis y las exportaciones aceptan `?desde=aaaa-mm-dd&hasta=aaaa-mm-dd`.

| Uso | Endpoints |
|---|---|
| Formulario público | `GET /api/encuestas/{titulados\|empleadores}`, `POST /api/respuestas/{titulados\|empleadores}` |
| Editor de la encuesta | `GET /api/admin/encuestas[/{encuesta}]`, `POST/PUT/DELETE /api/admin/secciones[/{id}]`, `…/preguntas[/{id}]`, `…/opciones[/{id}]` |
| Respuestas | `GET /api/admin/respuestas`, `GET/PUT/DELETE /api/admin/respuestas/{id}`, `GET /api/admin/respuestas/revisar?encuesta=…`, `GET /api/admin/respuestas-por-dia` |
| Criterios ARCU-SUR | `GET /api/criterios` |
| Indicadores | `GET /api/estado`, `/api/demografia`, `/api/empleabilidad`, `/api/formacion`, `/api/financiamiento`, `/api/competencias`, `/api/empleadores` |
| Cruces | `GET /api/cruces` (los 7 del estudio), `GET /api/variables`, `GET /api/cruce-libre?filas=…&columnas=…&agrupar=1` |
| Reporte a medida | `GET /api/reporte/variables?encuesta=…`, `GET /api/reporte?encuesta=…&variables=a,b,c` |
| Importar / exportar | `POST /api/importar?tipo=…`, `GET /api/exportar?que=titulados\|empleadores\|libre\|C1…C7\|reporte\|reporte-datos` |

`formato=excel` en `/api/exportar` usa separador `;`, decimales con coma y BOM UTF-8,
para que Excel en español lo abra en columnas.

## Backend (`backend/src/main/java/com/actividad2/`)

| Archivo | Qué hace |
|---|---|
| `Main.java` | Servidor HTTP (un hilo virtual por petición) y rutas |
| `Db.java` | Conexión a PostgreSQL con pool de conexiones y transacciones; crea el esquema |
| `Semilla.java` | Estructura inicial de las dos encuestas (réplica de los formularios de Forms + año de titulación) |
| `Encuestas.java` | Estructura de las encuestas y su edición (con caché del formulario) |
| `Respuestas.java` | Envío y validación del formulario, gestión de respuestas y conversión al `Dataset` del análisis |
| `FormsImport.java` | Importa los CSV de Microsoft/Google Forms: traduce el texto de preguntas y opciones a códigos e informa lo no reconocido |
| `Revision.java` | Revisión de calidad: respuestas casi vacías, nombres de organización sin sentido y posibles duplicados |
| `Dataset.java` | Respuestas de una encuesta en forma de tabla; filtro por fecha |
| `Catalogo.java` | Etiquetas, agrupaciones de los cruces, punto medio de los rangos de tiempo, homologación de cargos |
| `Estadistica.java` | Media, mediana, desviación, IC 95% (Coss Bu cap. 6), frecuencias, chi-cuadrada de tabla de contingencia (α = 0.05) |
| `Analisis.java` | Indicadores y los 7 cruces del estudio |
| `Criterios.java` | Valoración de cada criterio ARCU-SUR (la usan el panel y el informe) |
| `Abiertas.java` | Codificación de las preguntas abiertas de empleadores en categorías por palabras clave |
| `Reportes.java` | Reporte a medida: tablas de frecuencia de las preguntas elegidas y su exportación |
| `ReporteTex.java` | Escribe los desarrollos paso a paso del informe en `../tex/generado/` |
| `Csv.java`, `Json.java` | Lectura y escritura de CSV y JSON |

Las decisiones de procesamiento (punto medio de los rangos, ramas que no
responden una pregunta, escala de empleadores sin media, etc.) están en
`../PLAN.md` sección 5.6 y en comentarios del código.

## Generar los desarrollos del informe

`ReporteTex.java` escribe en `../tex/generado/` los desarrollos paso a paso
(Paso 1 … Paso 6, con cada suma y división a la vista) con los mismos
cálculos del sistema, leyendo las respuestas de la base. `Informe.tex` los
incluye con `\input`. No editar a mano esos archivos: se sobrescriben.

```
cd backend
java -cp target/classes:src/main/resources:lib/postgresql-42.7.5.jar com.actividad2.ReporteTex ../../tex/generado
# solo un período (aaaa-mm-dd; "-" = sin límite):
java -cp target/classes:src/main/resources:lib/postgresql-42.7.5.jar com.actividad2.ReporteTex ../../tex/generado - 2026-01-01 2026-12-31
```
Después, compilar el informe con `latexmk -pdf Informe.tex` desde `../tex/`.

Para revisar la codificación de las preguntas abiertas (qué respuestas quedaron
sin categoría):
```
java -cp target/classes:src/main/resources:lib/postgresql-42.7.5.jar com.actividad2.Abiertas
```
