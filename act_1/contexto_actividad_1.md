# Actividad de Taller de Simulación de Sistemas

Deberá estructurar la entrega de su trabajo de acuerdo a los siguientes puntos:

1. Modelo de simulación teórico de los problemas asignados.
2. La simulación implementada en Excel: corresponde a la simulación manual y los correspondientes gráficos.
3. El programa Java de simulación: El punto de partida es el modelo teórico de Simulación.
4. Llevar a cabo la validación correspondiente en el modelo de simulación manual y aquel implementado.
5. Presentar los requerimientos respecto de informe, diapositivas y código fuente ya explicados en clase.

A continuación los ejercicios que hacen a su actividad de taller de simulación.

## Parte 1: Por transformada inversa

f(x) {
	(x - 3)^2/18; 0 <= x <= 6
	0;            0 > x > 6
}

## Parte 2: Aplicaciones de Simulación

1. Una cierta compañía posee un gran número de máquinas en uso. El tiempo que dura en operación cada una de estas máquinas, sigue la siguiente distribución de probabilidad:

| Tiempo entre descomposturas (horas) | Probabilidad |
| --- | --- |
| 6 · 8 | 0.10 |
| 8 · 10 | 0.15 |
| 10 · 12 | 0.24 |
| 12 · 14 | 0.26 |
| 16 · 18 | 0.18 |
| 18 · 20 | 0.07 |

El tiempo que un operador se tarda en reparar una máquina, sigue la siguiente distribución de probabilidad:

| Tiempo de reparación (horas) | Probabilidad |
| --- | --- |
| 2 · 4 | 0.15 |
| 4 · 6 | 0.25 |
| 6 · 8 | 0.30 |
| 8 · 10 | 0.20 |
| 10 · 12 | 0.10 |

Si el costo de tener una máquina ociosa durante una hora es de $500, y el salario por hora para este tipo de operarios es de $50, ¿Cuantas maquinas se deben asignar a cada mecánico para que las atienda?

Sugerencia: Minimice el costo de tener la maquina ociosa, más el salario del mecánico dividido por el número de máquinas atendidas.

2. Una cadena de supermercados es abastecida por un almacén central. La mercancía que llega a este almacén es descargada en turnos nocturnos. Los camiones que se descargan en este almacén llegan en forma aleatoria de acuerdo a un proceso Poisson a una razón media de 2 camiones por hora. El tiempo que un equipo de tres trabajadores se tarda en descargar un camión, sigue una distribución uniforme entre 20 y 30 minutos. Si el número de trabajadores en el equipo se incrementa, entonces, la razón de servicio se incrementa. Por ejemplo, si el equipo está formado por 4 trabajadores, el tiempo de servicio esta uniformemente distribuido entre 15 y 25 minutos; si el equipo está formado por 5 trabajadores, el tiempo de servicio esta uniformemente distribuido entre 10 y 20 minutos y si el equipo está formado por 6 trabajadores, el tiempo de servicio esta uniformemente distribuido entre 5 y 15 minutos. Cada trabajador recibe $25 por hora durante el turno nocturno de ocho horas. El costo de tener un camión esperando se estima en $50 por hora. ¿El administrador del almacén desea saber cuál es el tamaño optimo del equipo?

