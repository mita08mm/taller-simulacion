EJEMPLO 5.3. ESTIMACIÓN DE π

Usando la figura que se muestra a continuación, ¿describir un procedimiento que estime el valor de π?

En la figura anterior, es obvio que la probabilidad de que un punto pertenezca al cuarto de círculo es la siguiente:

$$
\frac{\text{Área del cuarto de círculo}}{\text{Área del cuadrado}}
= \frac{\pi/4}{1}
= \frac{\pi}{4}
$$

también, es obvio que esta probabilidad puede ser estimada mediante la relación ( x/n ), donde ( x ) representa el número de veces que un punto simulado cayó dentro del cuarto de círculo y ( n ) el número de corridas.

Sin embargo, ( $\hat{\pi} )$ puede ser estimada usando la relación ( 4x/n ). Por consiguiente, sustituyendo   ( $\hat{\pi} = 4x/n$ ) en la última expresión, se obtiene:

$$
\text{Prob} \left\{ \frac{\pi - 0.1}{4} \le \frac{x}{n} \le \frac{\pi + 0.1}{4} \right\} = 0.95
$$

Es obvio que este valor estimado de π difiere demasiado de su valor verdadero. Consecuentemente, si la estimación del valor de π se quiere aproximar más a su valor verdadero, entonces, es necesario aumentar significativamente el número de corridas. Por ejemplo, en este momento es posible preguntar, ¿cuántas corridas es necesario simular para que el estimador de π difiera de su valor verdadero en una cantidad menor que 0.1, con una seguridad de 95%? Esta pregunta puede ser planteada de acuerdo a la siguiente expresión:

$$
\text{Prob.} { |\hat{\pi} - \pi| \le 0.1 } = 0.95
$$

la cual puede ser rearrreglada y expresada como:

$$
\text{Prob.} { \pi - 0.1 \le \hat{\pi} \le \pi + 0.1 } = 0.95
$$

lado cayó dentro del cuarto de círculo y ( n ) el número de corridas. Por consiguiente, en el límite cuando el número de corridas tienda a infinito, las dos relaciones anteriores deben ser idénticas, esto es:

$$
\lim_{n \to \infty} \frac{x}{n} = \frac{\pi}{4}
\quad y \quad
\hat{\pi} = 4\frac{x}{n}
$$

De acuerdo a la identidad anterior, el procedimiento para estimar el valor de π, sería:

1. Generar dos números uniformes ( R_1 ) y ( R_2 ).
2. Evaluar ( $\sqrt{R_1^2 + R_2^2} )$. Si este valor es menor que 1, entonces, el número simulado cae dentro del cuarto de círculo, y por consiguiente se incrementa el valor de ( x ). De lo contrario, es necesario regresar al paso 1.
3. Repetir los pasos anteriores hasta que ( n ) corridas hayan sido simuladas. En este momento, el valor de π, sería: ( 4x/n ).

Aplicando el procedimiento anterior, la tabla 5.3 muestra los resultados de una simulación manual para este problema. Como se puede observar en esta tabla, de 25 puntos simulados, 21 cayeron adentro del cuarto de círculo. Por consiguiente, el valor estimado de π, de acuerdo a estas 25 corridas, sería:

$$
\hat{\pi} = 4(21/25) = 3.36
$$

### TABLA 5.3

**Resultados de la simulación manual para estimar el valor de π**

| Número de corrida | Primer número aleatorio | Segundo número aleatorio | ¿√R₁² + R₂² ≤ 1? | Valor acumulado de X |
| --- | --- | --- | --- | --- |
| 1 | 0.03991 | 0.38555 | sí | 1 |
| 2 | 0.17546 | 0.32643 | sí | 2 |
| 3 | 0.69572 | 0.24122 | sí | 3 |
| 4 | 0.61196 | 0.30532 | sí | 4 |
| 5 | 0.03788 | 0.48228 | sí | 5 |
| 6 | 0.88618 | 0.71299 | no | 5 |
| 7 | 0.27954 | 0.80863 | sí | 6 |
| 8 | 0.33564 | 0.90899 | sí | 7 |
| 9 | 0.78038 | 0.55986 | sí | 8 |
| 10 | 0.87539 | 0.16818 | sí | 9 |
| 11 | 0.34677 | 0.45305 | sí | 10 |
| 12 | 0.59747 | 0.16520 | sí | 11 |
| 13 | 0.68652 | 0.79375 | no | 11 |
| 14 | 0.33521 | 0.59589 | sí | 12 |
| 15 | 0.20554 | 0.59404 | sí | 13 |
| 16 | 0.42614 | 0.34994 | sí | 14 |
| 17 | 0.99385 | 0.66497 | no | 14 |
| 18 | 0.48509 | 0.15470 | sí | 15 |
| 19 | 0.20094 | 0.73788 | sí | 16 |
| 20 | 0.60530 | 0.44372 | sí | 17 |
| 21 | 0.18611 | 0.58319 | sí | 18 |
| 22 | 0.61199 | 0.18627 | sí | 19 |
| 23 | 0.00441 | 0.32624 | sí | 20 |
| 24 | 0.65961 | 0.20288 | sí | 21 |
| 25 | 0.59362 | 0.99782 | no | 21 |

---

Si quieres, puedo pasarte esto en formato Word, PDF o LaTeX listo para imprimir.

y puesto que $x$ (cantidad de puntos simulados que cayeron adentro del cuarto de círculo) es una variable aleatoria que sigue una distribución binomial (cada punto simulado sigue una distribución Bernoulli), con media $n \pi/4$ y variancia $n \pi/4(1 - \pi/4)$, entonces, $x/n$ la cual representa la fracción de éxitos, tiene la siguiente media y variancia:

$E \left( \frac{x}{n} \right) = \frac{1}{n} E(x) = \pi/4$

$VAR \left( \frac{x}{n} \right) = E \left( \frac{x}{n} - \frac{\pi}{4} \right)^2 = \frac{1}{n^2} E(x - n \pi/4)^2 = \pi/4(1 - \pi/4)/n$

Por otra parte, es obvio que cuando $n$ es grande, tanto la distribución binomial como la fracción de éxitos ($x/n$), pueden ser reemplazadas por una distribución normal con los parámetros respectivos. Por ejemplo, se puede decir que cuando $n$ es grande, $x/n$ sigue aproximadamente una distribución normal, con media y variancia respectivamente, de $\pi/4$ y $\pi/4(1 - \pi/4)/n$. Por consiguiente, el número de corridas que es necesario realizar para que el valor estimado de $\pi$, difiera de su valor verdadero en menos de 0.1, con una seguridad de 95%, puede ser encontrado al resolver la siguiente ecuación:

$Z_{\alpha/2} = \frac{\frac{\pi + 0.1}{4} - \frac{\pi}{4}}{\sqrt{\frac{\pi}{4} \left( 1 - \frac{\pi}{4} \right) \frac{1}{n}}}$

y despejando el valor de $n$ se obtiene:

$n = \frac{4 \pi (1 - \pi/4) Z_{\alpha/2}^2}{(0.1)^2}$

y puesto que $Z_{\alpha/2} = 1.96$, entonces el valor de $n$ resulta ser de 1036. En esta última expresión, se puede observar que el valor de $n$ aumenta entre mayor sea el nivel de confianza o la exactitud requerida. En conclusión, se puede decir que para que el valor estimado de $\pi$, difiera de su valor verdadero en una cantidad menor que 0.1, es necesario simular 1036 corridas.