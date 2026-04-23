# Informe - Actividad 1 (Taller de Simulacion de Sistemas)

## Planteamiento del problema

Se trabajo con la Actividad 1 del Taller de Simulacion de Sistemas y se estructuro el problema en dos partes, con sus datos y decisiones asociadas.

### Parte 1: Por transformada inversa

Se utilizo la funcion de densidad indicada en el enunciado y se considero tambien la figura triangular que aparece al lado de la expresion:

f(x) = (x - 3)^2 / 18, para 0 <= x <= 6
f(x) = 0, para x < 0 o x > 6

El problema consistio en generar valores aleatorios con esa distribucion mediante transformada inversa y verificar que los valores simulados quedaran dentro del intervalo [0, 6]. La figura del triangulo represento una distribucion triangular general con parametros a, b y c, donde a fue el limite inferior, b la moda o punto mas alto, y c el limite superior.

Para esta parte se trabajo con las expresiones matematicas:

1. Densidad usada en el modelo:
   f(x) = (x - 3)^2 / 18,  para 0 <= x <= 6.
2. Funcion acumulada derivada:
   F(x) = ((x - 3)^3 + 27) / 54,  para 0 <= x <= 6.
3. Inversa de la acumulada:
   x = F^-1(u) = 3 + raiz_cubica(54u - 27), con u en (0,1).
4. Verificacion aplicada:
   las muestras obtenidas por simulacion se mantuvieron en el rango [0, 6].

### Parte 2: Aplicaciones de simulacion

#### Problema 1: Maquinas y mecanicos

Se planteo una empresa con muchas maquinas en operacion. Se utilizo la tabla de tiempo entre descomposturas y su probabilidad:

- 6-8 horas: 0.10
- 8-10 horas: 0.15
- 10-12 horas: 0.24
- 12-14 horas: 0.26
- 16-18 horas: 0.18
- 18-20 horas: 0.07

Tambien se utilizo la tabla de tiempo de reparacion por operador:

- 2-4 horas: 0.15
- 4-6 horas: 0.25
- 6-8 horas: 0.30
- 8-10 horas: 0.20
- 10-12 horas: 0.10

Con costo de maquina ociosa de $500 por hora y salario de mecanico de $50 por hora, el problema consistio en determinar cuantas maquinas asignar por mecanico minimizando el costo total.

#### Problema 2: Camiones y equipo de descarga

Se planteo un almacen central con llegada de camiones en turno nocturno. Se uso proceso Poisson con tasa media de 2 camiones por hora.

Se modelaron tiempos de descarga segun tamano del equipo:

- 3 trabajadores: uniforme entre 20 y 30 minutos
- 4 trabajadores: uniforme entre 15 y 25 minutos
- 5 trabajadores: uniforme entre 10 y 20 minutos
- 6 trabajadores: uniforme entre 5 y 15 minutos

Con salario de $25 por hora por trabajador, turno de 8 horas y costo de espera del camion de $50 por hora, el problema consistio en determinar el tamano optimo del equipo.

## Objetivos

Se definieron y se cumplieron los siguientes objetivos de trabajo:

1. Se implemento la generacion de numeros aleatorios de la Parte 1 con CDF inversa y se verifico su comportamiento estadistico con 100000 muestras.
2. Se construyo un simulador de eventos discretos para el Problema 1 y se evaluaron 20 alternativas de asignacion (m = 1..20).
3. Se construyo un simulador de cola para el Problema 2 y se evaluaron 4 alternativas de equipo (n = 3..6).
4. Se estandarizo el criterio economico de seleccion en ambos casos usando costo total minimo.
5. Se dejo una implementacion ejecutable en modo GUI y modo CLI para reproducir los resultados.

## Marco teorico

Se aplicaron conceptos de simulacion estocastica y teoria de colas que se usaron directamente en el codigo:

1. Generacion por transformada inversa: se uso U ~ U(0,1) y la transformacion X = F^-1(U).
2. Modelado por distribucion por tramos: se uso acumulada por intervalos para convertir una uniforme en muestras con las probabilidades de las tablas del enunciado.
3. Proceso Poisson para llegadas: se represento con tiempos entre llegadas exponenciales.
4. Servicio uniforme: se modelaron tiempos de atencion en intervalos [a,b] segun cantidad de trabajadores.
5. Simulacion de eventos discretos: se representaron eventos de falla, inicio/fin de reparacion, cola de espera y avance del reloj de simulacion.
6. Optimizacion por costo: se compararon alternativas discretas y se selecciono la de menor costo total esperado.

## Modelamiento del modelo

Se formularon y codificaron los siguientes modelos:

### Parte 1

Se definio la acumulada para la funcion dada y se aplico su inversa para generar muestras en [0, 6]. Se registro evidencia U -> X en la salida del simulador y se valido el rango observado de valores.

El procedimiento implementado en el simulador para la Parte 1 fue:

1. Se genero u con distribucion uniforme U(0,1).
2. Se calculo x con la formula inversa x = 3 + raiz_cubica(54u - 27).
3. Se repitio el proceso 100000 veces.
4. Se calcularon media, minimo y maximo como validacion del comportamiento de la variable simulada.

### Parte 2 - Problema 1 (maquinas)

1. Se modelaron fallas por maquina con la tabla de tiempo entre descomposturas:
   - 6-8: 0.10
   - 8-10: 0.15
   - 10-12: 0.24
   - 12-14: 0.26
   - 16-18: 0.18
   - 18-20: 0.07
2. Se modelaron tiempos de reparacion con la tabla:
   - 2-4: 0.15
   - 4-6: 0.25
   - 6-8: 0.30
   - 8-10: 0.20
   - 10-12: 0.10
3. Se construyo un sistema con un mecanico (servidor unico), con cola FIFO para maquinas averiadas.
4. Se simulo el horizonte de 8760 horas por corrida y se ejecutaron 500 replicas por alternativa.
5. Se acumulo el tiempo ocioso por maquina y se transformo a costo de ociosidad por maquina-hora.
6. Se incorporo el componente salarial por maquina-hora como salario/m.
7. Se compararon 20 alternativas y se selecciono la de menor costo total por maquina-hora.

### Parte 2 - Problema 2 (almacen)

1. Se modelaron llegadas de camiones con proceso Poisson de razon 2 camiones/hora, implementado como tiempos entre llegadas exponenciales.
2. Se modelaron tiempos de servicio por tamano de equipo:
   - 3 trabajadores: Uniforme(20,30) min
   - 4 trabajadores: Uniforme(15,25) min
   - 5 trabajadores: Uniforme(10,20) min
   - 6 trabajadores: Uniforme(5,15) min
3. Se simulo un servidor con capacidad variable por tamano de equipo y se acumulo espera total de camiones por turno.
4. Se ejecutaron 30000 replicas por alternativa para estabilizar la estimacion del costo esperado.
5. Se calculo costo de espera por turno y costo de mano de obra por turno.
6. Se compararon las alternativas n = 3, 4, 5, 6 y se selecciono la de menor costo total por turno.

## Formulas matematicas aplicadas

En esta actividad se aplicaron las siguientes formulas en el proceso de simulacion:

1. Transformada inversa (forma general):
   x = F^-1(u), con u en (0,1).

2. Parte 1 (funcion del enunciado):
   F(x) = ((x - 3)^3 + 27) / 54, para 0 <= x <= 6
   x = 3 + raiz_cubica(54u - 27)

3. Llegadas Poisson mediante tiempos entre llegadas exponenciales:
   t = -(ln(1-u)) / lambda
   con lambda = 2 camiones por hora.

4. Uniforme continua para tiempos de servicio:
   x = a + (b - a)u

5. Costo total del Problema 1:
   costo_total_maquina_hora = costo_ociosidad_maquina_hora + salario_mecanico_hora / m

6. Costo total del Problema 2:
   costo_total_turno = costo_espera_turno + costo_mano_obra_turno

## Fragmentos de codigo importantes

Se incluyeron los fragmentos centrales usados en la implementacion:

1. Transformada inversa de la Parte 1 (InverseTransformPartOneDistribution)

```java
@Override
public double inverseCdf(double u) {
   if (u < 0.0 || u > 1.0) {
      throw new IllegalArgumentException("u must be in [0,1]");
   }
   double x = 3.0 + Math.cbrt(54.0 * u - 27.0);
   if (x < 0.0) {
      return 0.0;
   }
   if (x > 6.0) {
      return 6.0;
   }
   return x;
}
```

2. Exponencial por transformada inversa (ExponentialDistribution)

```java
@Override
public double inverseCdf(double u) {
   if (u < 0.0 || u > 1.0) {
      throw new IllegalArgumentException("u must be in [0,1]");
   }
   double adjusted = (u <= 0.0) ? Double.MIN_VALUE : u;
   return -Math.log(1.0 - adjusted) / ratePerHour;
}
```

3. Uniforme por CDF inversa (UniformDistribution)

```java
@Override
public double inverseCdf(double u) {
   if (u < 0.0 || u > 1.0) {
      throw new IllegalArgumentException("u must be in [0,1]");
   }
   return min + (max - min) * u;
}
```

4. Distribucion por tramos con acumulada (PiecewiseUniformDistribution)

```java
@Override
public double inverseCdf(double u) {
   if (u < 0.0 || u > 1.0) {
      throw new IllegalArgumentException("u must be in [0,1]");
   }

   double scaled = u * totalProbability;
   double cumulative = 0.0;
   for (IntervalProbability interval : intervals) {
      double previous = cumulative;
      cumulative += interval.probability();
      if (scaled <= cumulative) {
         double localU = (scaled - previous) / interval.probability();
         return interval.minInclusive() + localU * interval.width();
      }
   }

   IntervalProbability last = intervals.get(intervals.size() - 1);
   return last.maxExclusive();
}
```

5. Evaluacion de alternativas del Problema 1 (ProblemOneSimulator)

```java
public List<ProblemOneResult> evaluateAssignments(
      int minMachines,
      int maxMachines,
      int replications,
      double horizonHours,
      double idleCostPerHour,
      double mechanicSalaryPerHour,
      RandomSource random) {

   List<ProblemOneResult> results = new ArrayList<>();
   for (int machines = minMachines; machines <= maxMachines; machines++) {
      double downtime = 0.0;
      for (int r = 0; r < replications; r++) {
         downtime += simulateDowntimePerMachine(machines, horizonHours, random);
      }

      double averageDowntimePerMachine = downtime / replications;
      double downtimeFraction = averageDowntimePerMachine / horizonHours;
      double idleCostPerMachineHour = idleCostPerHour * downtimeFraction;
      double salaryPerMachineHour = mechanicSalaryPerHour / machines;
      double totalCost = idleCostPerMachineHour + salaryPerMachineHour;

      results.add(new ProblemOneResult(
            machines,
            averageDowntimePerMachine,
            idleCostPerMachineHour,
            salaryPerMachineHour,
            totalCost));
   }
   return results;
}
```

6. Evaluacion de alternativas del Problema 2 (ProblemTwoSimulator)

```java
public List<ProblemTwoResult> evaluateTeamSizes(
      int minTeam,
      int maxTeam,
      int replications,
      double shiftHours,
      double workerSalaryPerHour,
      double waitingCostPerHour,
      RandomSource random) {

   List<ProblemTwoResult> results = new ArrayList<>();

   for (int teamSize = minTeam; teamSize <= maxTeam; teamSize++) {
      double totalWaiting = 0.0;
      for (int r = 0; r < replications; r++) {
         totalWaiting += simulateTotalWaitingHours(teamSize, shiftHours, random);
      }

      double avgTotalWaitingHours = totalWaiting / replications;
      double waitingCost = avgTotalWaitingHours * waitingCostPerHour;
      double laborCost = teamSize * workerSalaryPerHour * shiftHours;
      double totalCost = waitingCost + laborCost;

      results.add(new ProblemTwoResult(teamSize, avgTotalWaitingHours, waitingCost, laborCost, totalCost));
   }

   return results;
}
```

7. Integracion y salida de evidencias en SimulationService

```java
out.append("Ejemplo U -> X usando F^-1(u):\n");
for (int i = 1; i <= 5; i++) {
   double u = random.nextDouble();
   double x = distribution.inverseCdf(u);
   out.append(String.format(Locale.US, "  %d) u=%.5f -> x=%.5f%n", i, u, x));
}

out.append("Simulacion por alternativa (maquinas por mecanico):\n");
out.append("Simulacion por alternativa (tamano de equipo):\n");
```

## Recoleccion de datos

Se recolectaron directamente del PDF de la actividad los datos de entrada utilizados en la simulacion:

1. Tabla de tiempo entre descomposturas y probabilidades.
2. Tabla de tiempo de reparacion y probabilidades.
3. Costos unitarios del Problema 1:
   - Maquina ociosa: $500/h
   - Salario mecanico: $50/h
4. Parametros del Problema 2:
   - Llegadas: Poisson con media 2 camiones/h
   - Sueldo por trabajador: $25/h
   - Costo de espera del camion: $50/h
   - Turno: 8 horas

Adicionalmente, en el proceso de implementacion se fijaron parametros operativos de simulacion para garantizar reproducibilidad:

1. Semilla base: 1000.
2. Parte 1: 100000 muestras.
3. Problema 1: 500 replicas por alternativa y horizonte anual de 8760 horas.
4. Problema 2: 30000 replicas por alternativa y turno de 8 horas.

## Implementacion de la simulacion

Se implemento un programa Java estructurado por capas y responsabilidades:

1. Capa de aleatoriedad:
   - Se uso RandomSource y JavaRandomSource para centralizar la fuente aleatoria.
2. Capa de distribuciones:
   - Se implementaron UniformDistribution, ExponentialDistribution, PiecewiseUniformDistribution e InverseTransformPartOneDistribution.
   - Todas las distribuciones se conectaron con el enfoque de CDF inversa.
3. Capa de simulacion por problema:
   - ProblemOneSimulator: eventos de falla/reparacion, cola y costo por alternativa.
   - ProblemTwoSimulator: llegadas/servicio, espera y costo por alternativa.
4. Capa de aplicacion:
   - SimulationService: ejecucion integral de la actividad y armado de reporte textual.
   - SimulationFrame: GUI para ejecutar simulacion y visualizar resultados.
   - Main: entrada principal con dos modos (GUI por defecto y CLI con --cli).
5. Automatizacion de ejecucion:
   - Se uso compilar.sh para compilar y ejecutar en una sola instruccion.

### Implementacion en Excel (ubicacion en el informe)

En esta entrega no se adjunto el archivo de Excel. En esta seccion debe insertarse:

1. Captura de la hoja de simulacion manual (tablas y formulas).
2. Captura de resultados por alternativa en Excel.
3. Graficos comparativos de costos para Problema 1 y Problema 2.
4. Breve contraste entre resultados de Excel y resultados del programa Java.

## Resultados

Con la ejecucion validada del simulador (modo CLI y GUI) se obtuvieron los siguientes resultados:

### Parte 1

1. Se generaron 100000 muestras por transformada inversa.
2. Media estimada: 2.99808.
3. Minimo observado: 0.00000.
4. Maximo observado: 5.99998.
5. Se imprimieron ejemplos directos de transformacion U -> X para evidenciar la aplicacion de F^-1(u).

### Parte 2 - Problema 1

1. Se evaluaron 20 alternativas (m = 1 a 20).
2. La mejor alternativa fue m = 2 maquinas por mecanico.
3. Resultado de la alternativa optima:
   - Tiempo ocioso promedio por maquina: 3389.16004 h
   - Costo ociosidad por maquina-hora: 193.44521
   - Salario por maquina-hora: 25.00000
   - Costo total por maquina-hora: 218.44521
4. Tendencia observada: al incrementar m por encima de 2, el costo de ociosidad crecio mas rapido que la reduccion del salario por maquina.

### Parte 2 - Problema 2

1. Se evaluaron 4 alternativas (n = 3, 4, 5, 6).
2. La mejor alternativa fue n = 4 trabajadores.
3. Resultado de la alternativa optima:
   - Espera promedio total por turno: 4.63875 h
   - Costo de espera por turno: 231.93735
   - Costo de mano de obra por turno: 800.00000
   - Costo total por turno: 1031.93735
4. Tendencia observada: con n = 3 la espera fue alta; con n = 5 y n = 6 bajo la espera, pero el costo laboral supero el ahorro en espera.

## Conclusiones

1. Se implemento de forma consistente el metodo de transformada inversa, tanto en la Parte 1 como en las distribuciones base de la Parte 2.
2. Se completo un simulador que evaluo alternativas, no solo una corrida unica, y permitio sustentar la seleccion con tablas de costo.
3. En el Problema 1, la alternativa m = 2 fue la mas conveniente bajo los parametros del enunciado y la configuracion de simulacion ejecutada.
4. En el Problema 2, la alternativa n = 4 fue la mas conveniente al balancear costo de espera y costo de mano de obra.
5. Se obtuvo un entregable tecnico reproducible (GUI, CLI, script y formulas trazables en codigo) y se dejo definida la seccion exacta para incorporar la evidencia en Excel.
