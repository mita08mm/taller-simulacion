Ejemplo 5.1. Juego de volados
Existe un método viejo para jugar volados, que consiste en doblar la apuesta cada vez que se pierde. Por ejemplo, si se apuesta  $X$  y se pierde, entonces, se apuesta $2X$; si en esta ocasión se vuelve a perder, entonces, se apuesta $4X$ y así sucesivamente. Sin embargo, si al seguir esta política sucede que la apuesta es mayor que la cantidad de que se dispone, entonces, se apuesta lo que se tiene disponible. Por el contrario, cada vez que se gane, la apuesta será de $X$. Si la cantidad inicial disponible es de $30, la apuesta es de $10, la ganancia es igual a la cantidad apostada, la probabilidad de ganar en un volado es 0.5 y se quiere tener $50, ¿cuál es la probabilidad de llegar a la meta?

Para la información anterior, la tabla 5.1 muestra los resultados de una simulación manual para este juego de volados. Como se puede observar en esta tabla, en la primera "corrida" se necesitaron dos lanzamientos para llegar a la meta de tener $50. El primer lanzamiento de esta corrida el cual está representado por el número aleatorio 0.03991, significa que se ganó en el volado (números aleatorios menores de 0.5 representan al evento ganar, y números aleatorios mayores que 0.5 representan al evento perder), y por consiguiente, la apuesta en el siguiente volado será de $10. Como el número aleatorio de este segundo volado es menor que 0.5, entonces, con este lanzamiento se llega a la meta de $50.

Por otra parte, la corrida número 3 representa una situación de quiebra, es decir, esta corrida representa una situación en la que se pierde completamente la cantidad inicial disponible. En esta corrida, se pierde el primer volado, y por consiguiente, la apuesta del segundo volado es $20. Como el número aleatorio de este lanzamiento es menor que 0.5, entonces, se gana en el segundo volado, y la apuesta del tercer volado será de $10. Como el número aleatorio de este volado es mayor que 0.5, entonces, se pierde en el tercer volado, y la apuesta del cuarto volado será de $20. Puesto que el número aleatorio de este volado es mayor que 0.5, entonces, se pierde en el cuarto volado, y la apuesta será de $40. Sin embargo, la cantidad que se tiene al final del cuarto volado es de $10. Por consiguiente, la cantidad apostada en el quinto volado será de $10. Como esta cantidad es perdida en el último lanzamiento, entonces, se llega finalmente a una situación de quiebra.

Finalmente, en esta tabla se puede observar que de 10 corridas, en 6 ocasiones se llegó a la meta. Esto significa que la probabilidad de llegar a la meta es de 0.6. Sin embargo, en necesario señalar que esta estimación no es muy confiable por el número tan reducido de corridas que se simularon. Para tomar una decisión que garantice buenos resultados, es necesario aumentar significativamente el número de corridas simuladas. También, vale la pena mencionar que este problema sería demasiado difícil de resolver analíticamente, sin embargo, a través del uso de simulación se facilita tremendamente la estimación de la probabilidad buscada.

## Tabla 5.1

**Resultados de la simulación manual de un juego de volados**

| Nº corrida | Cantidad antes | Apuesta | Nº aleatorio | ¿Se ganó? | Cantidad después | ¿Meta? |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 30 | 10 | 0.03991 | sí | 40 |  |
|  | 40 | 10 | 0.38555 | sí | 50 | sí |
| 2 | 30 | 10 | 0.17546 | sí | 40 |  |
|  | 40 | 10 | 0.32643 | sí | 50 | sí |
| 3 | 30 | 10 | 0.69572 | no | 20 |  |
|  | 20 | 20 | 0.24122 | sí | 40 |  |
|  | 40 | 10 | 0.61196 | no | 30 |  |
|  | 30 | 20 | 0.88722 | no | 10 |  |
|  | 10 | 10 | 0.65961 | no | 0 | quiebra |
| 4 | 30 | 10 | 0.03788 | sí | 40 |  |
|  | 40 | 10 | 0.48288 | sí | 50 | sí |
| 5 | 30 | 10 | 0.88618 | no | 20 |  |
|  | 20 | 20 | 0.71299 | no | 0 | quiebra |
| 6 | 30 | 10 | 0.27954 | sí | 40 |  |
|  | 40 | 10 | 0.80863 | no | 30 |  |
|  | 30 | 20 | 0.33564 | sí | 50 | sí |
| 7 | 30 | 10 | 0.90899 | no | 20 |  |
|  | 20 | 20 | 0.78038 | no | 0 | quiebra |
| 8 | 30 | 10 | 0.55986 | no | 20 |  |
|  | 20 | 20 | 0.87539 | no | 0 | quiebra |
| 9 | 30 | 10 | 0.16818 | sí | 40 |  |
|  | 40 | 10 | 0.34677 | sí | 50 | sí |
| 10 | 30 | 10 | 0.43305 | sí | 40 |  |
|  | 40 | 10 | 0.59747 | no | 30 |  |
|  | 30 | 20 | 0.16520 | sí | 50 | sí |

**Nota:**

*Estos números aleatorios se obtuvieron del apéndice.*