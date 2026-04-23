## 1) Datos del contexto (sin cambiar base)

- AF (activo fijo inicial): -100,000
- AC (activo circulante inicial): -40,000
- Flujo antes de impuestos: 30,000
- Inflacion por ano: 18%, 18%, 22%, 25%, 28%
- Impuestos: T = 50% = 0.50
- Vida fiscal: 5 anos
- Depreciacion anual: 20% de AF = 20,000

## 2) Conceptos clave en palabras simples

- Flujo antes de impuestos:
	- Dinero que genera el proyecto antes de calcular impuestos.
	- En este contexto, la base es 30,000 por ano.

- Base gravable (o ingreso gravable):
	- Es la parte sobre la que se cobran impuestos.
	- Aqui: base gravable = flujo corriente - depreciacion.

- Depreciacion:
	- No es una salida real de caja ese ano.
	- Se usa contablemente para reducir impuestos.

- Escudo fiscal por depreciacion:
	- Ahorro de impuestos por poder restar depreciacion.
	- Formula: depreciacion x T.
	- Aqui: 20,000 x 0.50 = 10,000.

- (1 - T):
	- Fraccion que queda despues de impuestos.
	- Si T=0.50, entonces (1-T)=0.50.
	- Si T=0.30, entonces (1-T)=0.70.

- Producto acumulado de inflacion:
	- Se escribe como $prod_{j=1..t}(1+i_j)$.
	- Significa multiplicar todos los factores de inflacion hasta el ano t.
	- Ejemplo t=3: (1+0.18)(1+0.18)(1+0.22)=1.6987.

- Inversion adicional en activo circulante:
	- Dinero extra que se inmoviliza por inflacion.
	- En la Tabla 5.4 es la columna "Inversion adicional en activo circulante".

## 3) Formula del contexto (literal)

$S_t = \dfrac{x_t \prod_{j=1}^{t}(1+i_{i,j})(1-T) + (0.2AF)(T) - AC(i_{i,j})\prod_{j=2}^{t}(1+i_{i,j})}{\prod_{j=1}^{t}(1+i_{i,j})}$

Lectura corta:
- Primer termino: flujo neto de impuestos.
- Segundo termino: escudo fiscal por depreciacion.
- Tercer termino: inversion adicional de AC (se resta).
- Denominador: convertir de pesos corrientes a pesos constantes.

## 4) Tabla 5.4 detallada (todas las columnas)

La siguiente tabla esta reconstruida con los parametros del contexto (escenario pesimista) y con calculo consistente columna por columna.

| Ano | Inversion adicional en AC | Flujo antes de impuestos (corriente) | Depreciacion | Ingreso gravable | Impuestos | Flujo despues de impuestos (corriente) | Flujo despues de impuestos (constante) |
|-----|----------------------------|---------------------------------------|--------------|------------------|-----------|----------------------------------------|----------------------------------------|
| 0 | - | -140,000 | - | - | - | -140,000 | -140,000 |
| 1 | 7,200 | 35,400 | 20,000 | 15,400 | 7,700 | 20,500 | 17,373 |
| 2 | 8,496 | 41,772 | 20,000 | 21,772 | 10,886 | 22,390 | 16,080 |
| 3 | 10,365 | 50,962 | 20,000 | 30,962 | 15,481 | 25,116 | 14,785 |
| 4 | 12,956 | 63,702 | 20,000 | 43,702 | 21,851 | 28,895 | 13,608 |
| 5 | 16,584 | 81,539 | 20,000 | 61,539 | 30,769 | 34,185 | 12,577 |
| 5 (rescate) | - | - | - | - | - | +135,898 | +50,000 |

### Como se calcula cada columna (exacto)

1. Flujo antes de impuestos (corriente):
	- $x_t^{corriente} = 30,000 x \prod_{j=1}^{t}(1+i_j)$

2. Depreciacion:
	- $Dep = 0.2 x |AF| = 0.2 x 100,000 = 20,000$

3. Ingreso gravable:
	- $IG_t = x_t^{corriente} - Dep$

4. Impuestos:
	- $Imp_t = T x IG_t = 0.50 x IG_t$

5. Inversion adicional en activo circulante:
	- Ano 1: $\Delta AC_1 = |AC| x i_1 = 40,000 x 0.18 = 7,200$
	- Anos siguientes: $\Delta AC_t = \Delta AC_{t-1} x (1+i_t)$

6. Flujo despues de impuestos (corriente):
	- $FE_t^{corriente} = x_t^{corriente} - Imp_t - \Delta AC_t$

7. Flujo despues de impuestos (constante):
	- $S_t = FE_t^{corriente} / \prod_{j=1}^{t}(1+i_j)$

8. Fila de rescate (ano 5):
	- Corriente: $VR_{corriente} = 50,000 x \prod_{j=1}^{5}(1+i_j) = 50,000 x 2.7179 = 135,898$
	- Constante: $VR = 50,000$

Nota:
- En algunas versiones escaneadas de la tabla 5.4 aparece un pequeno desfase en "Impuestos" del ano 1.
- En este documento se usan valores consistentes con las ecuaciones y con el flujo corriente reportado (20,500).

## 5) Donde aparece cada simbolo con numeros reales

Ejemplo para t = 3 (Tabla 5.4):

- $x_t (base)$ = 30,000
- $prod_{j=1..3}(1+i_j)$ = 1.18 x 1.18 x 1.22 = 1.6987
- (1-T) = 0.50
- $(0.2AF)(T)$ = (0.2 x 100,000) x 0.50 = 10,000
- $AC(i_{i,j}) prod_{j=2..3}(1+i_j)$: en la tabla equivale a la inversion adicional de AC del ano 3 = 10,365

Sustitucion:

$S_3 = \dfrac{(30,000)(1.6987)(0.50) + 10,000 - 10,365}{1.6987} \approx 14,785$

Coincide con la Tabla 5.4: S_3 = 14,785.

## 6) Por que aparece Delta AC_1 = |AC| x i_1

Porque en el primer ano el incremento de capital de trabajo se calcula sobre el AC inicial:

$\Delta AC_1 = |AC| x i_1 = 40,000 x 0.18 = 7,200$

Luego se actualiza ano a ano:
- Delta AC_2 = 7,200 x 1.18 = 8,496
- Delta AC_3 = 8,496 x 1.22 = 10,365
- Delta AC_4 = 10,365 x 1.25 = 12,956
- Delta AC_5 = 12,956 x 1.28 = 16,584

Estos son exactamente los valores de la tabla del contexto.

## 7) Resultado final del escenario pesimista (contexto)

- Inversion inicial: -140,000
- Flujos constantes: 17,373; 16,080; 14,785; 13,608; 12,577
- Valor de rescate: 50,000
- TIR aproximada: -3.1%
- TREMA: 15%

Conclusion:
- Como -3.1% < 15%, el escenario pesimista se rechaza.

## 8) Desglose de la formula S_t para los 5 anos (literal y numerico)

Para evitar confusion, separamos la formula en partes:

$S_t = \dfrac{A_t + B - C_t}{D_t}$

donde:
- $A_t = x_t \cdot \prod_{j=1}^{t}(1+i_j) \cdot (1-T)$
- $B = (0.2|AF|)\cdot T = 10,000$
- $C_t = AC(i_{i,j})\cdot \prod_{j=2}^{t}(1+i_j)$ (en la tabla es la columna "Inversion adicional en AC")
- $D_t = \prod_{j=1}^{t}(1+i_j)$

Con parametros del contexto (pesimista):
- $x_t = 30,000$
- $T=0.50$
- Inflaciones: 0.18, 0.18, 0.22, 0.25, 0.28

| Ano | $D_t=\prod_{j=1}^{t}(1+i_j)$ | $A_t=(30,000)D_t(1-0.50)$ | $B$ | $C_t$ (tabla AC adicional) | $S_t=(A_t+B-C_t)/D_t$ |
|-----|-------------------------------|-----------------------------|-----|-----------------------------|----------------------|
| 1 | 1.1800 | 17,700 | 10,000 | 7,200 | 17,373 |
| 2 | 1.3924 | 20,886 | 10,000 | 8,496 | 16,080 |
| 3 | 1.6987 | 25,481 | 10,000 | 10,365 | 14,785 |
| 4 | 2.1234 | 31,851 | 10,000 | 12,956 | 13,608 |
| 5 | 2.7179 | 40,769 | 10,000 | 16,584 | 12,577 |

Nota importante sobre tu duda de $x_t$:
- En esta formula del contexto, se usa $x_t$ como base del flujo antes de impuestos (30,000 en este ejemplo).
- El termino $\prod_{j=1}^{t}(1+i_j)$ dentro del numerador ya "lleva" ese flujo a corrientes.
- Por eso no se debe volver a inflar dos veces.

## 9) TIR: se da o se calcula, y por que aparece el intervalo

La TIR no se "da"; se calcula resolviendo esta ecuacion:

$0 = I_0 + \sum_{t=1}^{5}\dfrac{S_t}{(1+r)^t} + \dfrac{VR}{(1+r)^5}$

con:
- $I_0=-140,000$
- $S_t$ de la tabla
- $VR=50,000$ (en pesos constantes)

Como no se despeja facil a mano, se obtiene numericamente (biseccion).

Sobre el intervalo del contexto:
- TIR minima: escenario pesimista -> alrededor de -3.1%
- TIR maxima: escenario optimista -> alrededor de 19.91%

Por eso luego se trabaja en el intervalo aproximado $[-3.1\%, 19.91\%]$ y se divide en subintervalos para histograma.

## 10) VR: aclaracion de pesos corrientes vs constantes

En el contexto aparece:

$VR = AC + (0.2AF)(1-T)$

Esa expresion se usa en pesos constantes del ano 0 (sin inflar):
- $VR = 40,000 + (20,000)(0.50) = 50,000$

Si lo quieres en pesos corrientes del ano 5, se multiplica por inflacion acumulada:
- $VR_{corriente} = 50,000 \cdot \prod_{j=1}^{5}(1+i_j) = 50,000\cdot 2.7179 = 135,898$

Resumen corto:
- Para ecuacion de TIR en esta formulacion: usa VR constante (50,000).
- En la fila de tabla en corrientes: aparece VR corriente (135,898).
