# Contexto de la Actividad 1

## Nombre de la actividad

Actividad de Taller de Simulación de Sistemas

## Contexto general

La entrega del trabajo debe estructurarse según los siguientes puntos:

1. Modelo de simulación teórico de los problemas asignados.
2. Simulación implementada en Excel, incluyendo la simulación manual y los gráficos correspondientes.
3. Programa Java de simulación, tomando como punto de partida el modelo teórico.
4. Validación del modelo de simulación manual y del modelo implementado.
5. Presentación de los requerimientos relacionados con informe, diapositivas y código fuente, ya explicados en clase.

Después de esos lineamientos aparecen los ejercicios del taller de simulación.

## Estructura de la actividad

### Parte 1: Por transformada inversa

Se presenta una función de densidad triangular con soporte entre 0 y 6:

f(x) = (x - 3)^2 / 18, para 0 <= x <= 6

La figura muestra una distribución triangular con vértices en a, b y c.

### Parte 2: Aplicaciones de simulación

#### Problema 1

Una compañía tiene un gran número de máquinas en uso. El tiempo de operación de cada máquina sigue esta distribución de probabilidad:

- 6 - 8 horas: 0.10
- 8 - 10 horas: 0.15
- 10 - 12 horas: 0.24
- 12 - 14 horas: 0.26
- 16 - 18 horas: 0.18
- 18 - 20 horas: 0.07

Se indica además que:

- El costo de tener una máquina ociosa durante una hora es de $500.
- El salario por hora del operario es de $50.
- Se debe determinar cuántas máquinas se asignan a cada mecánico para que las atienda.

Sugerencia dada por el enunciado:

- Minimizar el costo de tener la máquina ociosa más el salario del mecánico dividido por el número de máquinas atendidas.

#### Problema 2

Una cadena de supermercados es abastecida por un almacén central. La mercancía llega en turnos nocturnos y los camiones llegan según un proceso Poisson con una razón media de 2 camiones por hora.

El tiempo que un equipo de tres trabajadores tarda en descargar un camión sigue una distribución uniforme entre 20 y 30 minutos.

Si el equipo aumenta de tamaño, la razón de servicio cambia así:

- 4 trabajadores: uniforme entre 15 y 25 minutos
- 5 trabajadores: uniforme entre 10 y 20 minutos
- 6 trabajadores: uniforme entre 5 y 15 minutos

Además:

- Cada trabajador recibe $25 por hora durante un turno nocturno de 8 horas.
- El costo de tener un camión esperando se estima en $50 por hora.
- Se debe determinar cuál es el tamaño óptimo del equipo.

## Requisitos identificados antes del código

- Modelar las distribuciones de probabilidad indicadas.
- Resolver al menos una parte por transformada inversa.
- Implementar la simulación en Excel y en Java.
- Validar los resultados del modelo manual y del implementado.
- Analizar costos para decidir el mejor número de máquinas por mecánico y el tamaño óptimo del equipo de descarga.

## Observaciones previas al análisis

- El PDF contiene 2 páginas.
- El documento mezcla teoría de simulación con dos casos prácticos.
- Antes de programar conviene separar cada problema y definir sus variables aleatorias, probabilidades, rangos y métricas de costo.
- La primera tabla de tiempos de falla parece tener una lectura ambigua en uno de los intervalos; conviene revisar el PDF original si luego se va a implementar con precisión total.

