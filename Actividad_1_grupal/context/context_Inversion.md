## **Ejemplo 5.4. Proyecto de inversión**

La compañía $X$ desea incursionar en un nuevo negocio cuya inversión inicial requerida y los flujos de efectivo antes de depreciación e impuestos de los próximos cinco años siguen las siguientes distribuciones triangulares:

|  | **Estimación pesimista** | **Estimación más probable** | **Estimación optimista** |
| --- | --- | --- | --- |
| Activo fijo inicial | -100,000$ | -70,000$ | -60,000$ |
| Activo circulante inicial | -40,000$ | -30,000$ | -25,000$ |
| Flujo antes de impuestos | 30,000$ | 40,000$ | 45,000$ |

Además, esta compañía estima que las tasas de inflación en los próximos cinco años siguen las siguientes distribuciones triangulares:

**Tasas de inflación (%)**

| Año | **Estimación pesimista** | **Estimación más probable** | **Estimación optimista** |
| --- | --- | --- | --- |
| 1 | 18 | 15 | 12 |
| 2 | 18 | 15 | 12 |
| 3 | 22 | 18 | 15 |
| 4 | 25 | 20 | 18 |
| 5 | 28 | 22 | 19 |

Si la tasa de impuestos es de 50%, la TREMA de 15% y la alta administración ha establecido que si $Prob. \{ TIR > TREMA \}^* \geq 0.90$, entonces los nuevos proyectos se acepten; ¿debería la compañía $X$ aceptar este nuevo proyecto de inversión? (Asuma que la vida fiscal del activo fijo es de 5 años, y que el valor de rescate es un 20% del valor simulado para el activo fijo, y un 100% del valor simulado para el activo circulante.)

Este problema sería muy difícil de resolver en forma analítica, puesto que tanto los flujos de efectivo como las tasas de inflación son probabilísticas. Sin embargo, por medio de la simulación es muy sencillo establecer o desarrollar un modelo que incorpore toda la información probabilística de las diferentes variables aleatorias que intervienen en el proyecto de inversión. Específicamente, los pasos necesarios para determinar la distribución de probabilidad de la TIR y en base a ello tomar una decisión, serían:

1. **Determinar la TIR mínima** que puede resultar de la simulación. El valor de TIR mínimo resulta cuando el activo fijo inicial, el activo circulante inicial, el flujo de efectivo antes de impuestos y las tasas de inflación toman su valor pesimista. Para estos valores, la tabla 5.4 muestra los flujos de efectivo después de impuestos. Para estos flujos de efectivo, la tasa interna de rendimiento que se obtiene es de **3.1%**.
2. **Determinar la TIR máxima** que puede resultar de la simulación. El valor de TIR máximo resulta cuando el activo fijo inicial, el activo circulante inicial, el flujo de efectivo antes de impuestos y las tasas de inflación toman su valor optimista. Para estos valores, la tabla 5.5 muestra los flujos de efectivo después de impuestos. Para estos flujos de efectivo se obtiene una tasa de rendimiento de **19.91%**.
3. **Dividir el intervalo (-3.1%; 19.91%)** en 20 subintervalos iguales.
4. **Simular el valor del activo fijo inicial y del activo circulante inicial**, los cuales representan la inversión total del proyecto.
5. **Determinar el flujo de efectivo después de impuestos a pesos constantes**, de acuerdo a la siguiente expresión:

$S_t = \frac{x_t \prod_{j=1}^{t} (1+i_{i,j})(1-T) + (0.2AF)(T) - AC(i_{i,j}) \prod_{j=2}^{t} (1+i_{i,j})}{\prod_{j=1}^{t} (1+i_{i,j})}$

donde:

- $S_t =$ Flujo de efectivo después de impuestos a pesos constantes.
- $x_t =$ Valor simulado del flujo de efectivo antes de impuestos en el período $t$.
- $i_{i,j} =$ Valor simulado de la tasa de inflación en el período $j$.
- $T =$ Tasa de impuestos.
- $AF =$ Valor simulado del activo fijo inicial.
- $AC =$ Valor simulado del activo circulante inicial.
1. **Determinar el valor de rescate a pesos constantes**, utilizando la siguiente expresión:
    
    $VR = AC + (0.2AF) (1 - T)$
    
2. **Calcular la tasa interna de rendimiento** para estos valores simulados, de acuerdo a la siguiente expresión:

---

- TIR = Tasa interna de rendimiento.

TREMA = Tasa de recuperación mínima atractiva.

---

### **TABLA 5.4. Flujos de efectivo después de impuestos considerando que todas las variables aleatorias toman su valor pesimista.**

| **Año** | **Inversión adicional en activo circulante** | **Flujos de efectivo antes de impuestos** | **Depreciación** | **Ingreso gravable** | **Impuestos** | **Flujo de efectivo después de impuestos (pesos corrientes)** | **Flujo de efectivo después de impuestos (pesos constantes)** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0 |  | -140,000$ |  |  |  | -140,000$ | -140,000$ |
| 1 | 7,200 | 35,400 | 20,000 | 15,400 | 7,200 | 20,500 | 17,373 |
| 2 | 8,496 | 41,772 | 20,000 | 21,772 | 10,886 | 22,390 | 16,080 |
| 3 | 10,365 | 50,962 | 20,000 | 30,962 | 15,481 | 25,116 | 14,785 |
| 4 | 12,956 | 63,702 | 20,000 | 43,702 | 21,851 | 28,895 | 13,608 |
| 5 | 16,584 | 81,539 | 20,000 | 61,539 | 30,769 | 34,185 | 12,577 |
| 5 |  | 163,078 |  | 27,180 | 27,180 | 135,898 | 50,000 |

Tasa interna de rendimiento = $-3.1\%$

Por el contrario, si la respuesta es negativa, entonces:

$x = c - \sqrt{(c - a)(c - b)(1 - R)}$

Repetir los pasos anteriores tantas veces como se desee.

Si se aplica el procedimiento descrito anteriormente. y el método de la transformada inversa para simular distribuciones triangulares, el resultado es el histograma de la $TIR$ que se muestra en la tabla 5.6. A partir de este histograma, se obtiene la distribución acumulada de la $TIR$, la cual se muestra en la figura 5.1. En esta última figura se puede apreciar que la ${Prob. ITIR > TREMA}$ es prácticamente nula. Esto
significa, que deacuerdo a los estándares establecidos por la alta admi nistración, el proyecto deberá ser rechazado. Finalmente, conviene se nalar que esta decisión es bastante confiable, puesto que en la tabla 5.6 se muestran los resultados de simular 1.000 veces el valor de $TIR$.

| Limitte inferior del inventario | Limite superior del intervalo  | Fracción| Fracción acumulada |
| --- | --- | --- | --- |
| -3.10 | -1.95 | 0.000 | 0.000 |
| -1.95 | -0.8 | 0.000 | 0.000 |
| -0.80 | 0.35 | 0.000 | 0.000 |
| 0.35 | 1.5 | 0.000 | 0.000 |
| 1.5 | 2.65 | 0.003 | 0.003 |
| 2.65 | 3.8 | 0.019 | 0.022 |
| 3.8 | 4.96 | 0.082 | 0.104 |
| 4.96 | 6.11 | 0.141 | 0.245 |
| 6.11 | 7.26 | 0.189 | 0.434 |
| 7.26 | 8.41 | 0.193 | 0.627 |
| 8.41 | 9.56 | 0.184 | 0.811 |
| 9.56 | 10.71 | 0.111 | 0.922 |
| 10.71 | 11.86 | 0.057 | 0.979 |
| 11.86 | 13.01 | 0.015 | 0.994 |
| 13.01 | 14.16 | 0.006 | 1.000 |
| 14.16 | 15.31 | 0.000 | 1.000 |
| 15.31 | 16.46 | 0.000 | 1.000 |
| 16.46 | 17.61 | 0.000 | 1.000 |
| 17.61 | 18.76 | 0.000 | 1.000 |
| 18.76 | 19.91 | 0.000 | 1.000 |

Figura5'-1 Distribución acumulada edal tasa interna de rendimiento