# Explicación de las Simulaciones - Método Monte Carlo

## Introducción

Este proyecto implementa dos ejemplos clásicos de simulación Monte Carlo del libro de Raúl Coss Bu. El método Monte Carlo utiliza números aleatorios para resolver problemas que pueden ser difíciles o imposibles de resolver analíticamente.

---

## 1. SIMULACIÓN DEL JUEGO DE VOLADOS (Ejemplo 5.1)

### 1.1 Descripción del Problema

Se tiene un juego de volados con las siguientes características:
- **Capital inicial**: $30
- **Apuesta inicial**: $10
- **Meta**: Llegar a $50
- **Probabilidad de ganar**: 0.5 (50%)
- **Estrategia**: Doblar la apuesta cada vez que se pierde

**Pregunta**: ¿Cuál es la probabilidad de alcanzar la meta antes de quebrar?

### 1.2 Lógica de la Simulación

El juego funciona así:

1. **Si ganas un volado**: 
   - Aumentas tu capital por el monto apostado
   - La siguiente apuesta vuelve a ser $10

2. **Si pierdes un volado**:
   - Disminuyes tu capital por el monto apostado
   - La siguiente apuesta se duplica ($10 → $20 → $40...)

3. **Restricción importante**:
   - Si la apuesta calculada es mayor que tu capital disponible, solo apuestas lo que tienes

4. **Condiciones de término**:
   - **META**: Alcanzas o superas $50
   - **QUIEBRA**: Tu capital llega a $0

### 1.3 Ejemplo de una Corrida

```
Capital inicial: $30

Volado 1: Ganas (r = 0.25)
  Apuesta: $10
  Capital: $30 + $10 = $40
  Siguiente apuesta: $10

Volado 2: Ganas (r = 0.38)
  Apuesta: $10
  Capital: $40 + $10 = $50
  RESULTADO: ¡META ALCANZADA!
```

### 1.4 Resultados Típicos

Al ejecutar **10 corridas**:
- Metas alcanzadas: ~6 (60%)
- Quiebras: ~4 (40%)

Al ejecutar **1000 corridas**:
- La probabilidad se estabiliza alrededor de 0.6 (60%)
- Este resultado es más confiable por la Ley de Grandes Números

### 1.5 Interpretación de Resultados

**Con los parámetros del libro ($30, $10, $50)**:
- Tienes **60% de probabilidad** de alcanzar la meta
- Tienes **40% de probabilidad** de quebrar

**¿Por qué no es 50-50 si la moneda es justa?**
Porque:
1. La meta está más cerca ($20 más) que el inicio ($30 menos hasta quebrar)
2. La estrategia de doblar apuesta permite recuperarse de rachas perdedoras

### 1.6 Análisis de Sensibilidad

Puedes experimentar cambiando parámetros:

| Cambio | Efecto en Probabilidad de Éxito |
|--------|----------------------------------|
| ↑ Capital Inicial | ↑ Probabilidad (más margen de error) |
| ↑ Meta | ↓ Probabilidad (objetivo más lejano) |
| ↑ Apuesta Inicial | ↓ Probabilidad (menos volados posibles) |
| ↑ Prob. de Ganar | ↑ Probabilidad (juego favorable) |

**Ejemplo**: Si cambias probabilidad de ganar a 0.3 (desventaja):
- Probabilidad de éxito cae a ~30-35%
- Más corridas terminan en quiebra

---

## 2. ESTIMACIÓN DE π (Ejemplo 5.3)

### 2.1 Descripción del Problema

Estimar el valor de π usando el método Monte Carlo.

**Fundamento matemático**:
- Área del círculo de radio 1: π
- Área del cuadrado que lo contiene: 4
- Relación: π/4 = (Área círculo) / (Área cuadrado)

### 2.2 Lógica de la Simulación

El método funciona así:

1. **Generar punto aleatorio** (x, y) en el cuadrado [0,1] × [0,1]

2. **Calcular distancia** desde el origen: d = √(x² + y²)

3. **Verificar si está dentro del círculo**:
   - Si d ≤ 1 → Dentro del círculo
   - Si d > 1 → Fuera del círculo

4. **Repetir** n veces

5. **Estimar π**:
   ```
   π̂ = 4 × (puntos_dentro / total_puntos)
   ```

### 2.3 Ejemplo Visual

Imaginemos el cuadrado unitario con un cuarto de círculo inscrito:

```
     1 ┌─────────┐
       │    ╱    │
       │   ╱  ●  │  ● = Puntos dentro
       │  ╱   ○  │  ○ = Puntos fuera
       │ ╱    ●  │
       │╱  ●     │
     0 └─────────┘
       0         1
```

### 2.4 Resultados según Número de Puntos

| Puntos | π Estimado | Error | Error % |
|--------|------------|-------|---------|
| 100 | 3.08 - 3.20 | ±0.06 | ±2% |
| 1,000 | 3.12 - 3.16 | ±0.02 | ±0.6% |
| 10,000 | 3.138 - 3.145 | ±0.003 | ±0.1% |
| 100,000 | 3.1413 - 3.1418 | ±0.0002 | ±0.006% |

**Valor real de π**: 3.14159265359...

### 2.5 Convergencia

La estimación **mejora** con más puntos simulados:

- **100 puntos**: Muy impreciso, puede dar 3.08 o 3.24
- **1,000 puntos**: Razonablemente cerca, ~3.14
- **10,000 puntos**: Buena precisión, 3.141x
- **100,000 puntos**: Excelente precisión

**Ley de Grandes Números**: A medida que n → ∞, π̂ → π

### 2.6 ¿Cuántos Puntos Necesito?

El sistema incluye una calculadora que determina cuántos puntos se necesitan para:

**Ejemplo 1**: Error < 0.1 con 95% de confianza
- Puntos necesarios: ~1,036

**Ejemplo 2**: Error < 0.01 con 95% de confianza
- Puntos necesarios: ~103,600

**Ejemplo 3**: Error < 0.001 con 99% de confianza
- Puntos necesarios: ~10,658,000

**Conclusión**: Para mayor precisión, se necesita exponencialmente más puntos.

### 2.7 Interpretación de la Convergencia

El gráfico de convergencia muestra:
- **Inicio**: π̂ fluctúa mucho (pocas muestras)
- **Progreso**: π̂ se va acercando a π
- **Final**: π̂ se estabiliza cerca de π = 3.14159...

---

## 3. CONCEPTOS CLAVE DE SIMULACIÓN MONTE CARLO

### 3.1 Números Aleatorios Uniformes

- Cada número entre 0 y 1 tiene **igual probabilidad** de aparecer
- Son la base de todas las simulaciones Monte Carlo
- Se usan para representar eventos aleatorios

### 3.2 Corridas y Repeticiones

- **Corrida**: Una ejecución completa de la simulación
- **Múltiples corridas**: Necesarias para estimar probabilidades
- Más corridas = estimación más confiable

### 3.3 Ley de Grandes Números

**Teorema**: El promedio de muchas observaciones aleatorias converge al valor esperado.

**Aplicación**:
- 10 corridas → Resultado poco confiable
- 100 corridas → Resultado razonable
- 1,000+ corridas → Resultado confiable

### 3.4 Variabilidad Estocástica

Cada vez que ejecutas la simulación obtienes **resultados ligeramente diferentes**.

**Ejemplo**:
- Ejecución 1: Probabilidad = 0.62
- Ejecución 2: Probabilidad = 0.58
- Ejecución 3: Probabilidad = 0.61
- Promedio: ~0.60

Esto es **normal** en simulación.

---

## 4. VENTAJAS Y LIMITACIONES

### 4.1 Ventajas de la Simulación

1. **Problemas complejos**: Resuelve problemas difíciles analíticamente
2. **Flexibilidad**: Fácil cambiar parámetros
3. **Visualización**: Se puede ver el proceso
4. **Comprensión**: Ayuda a entender el comportamiento del sistema

### 4.2 Limitaciones

1. **No es exacta**: Solo una aproximación
2. **Requiere muchas corridas**: Para precisión necesitas tiempo de cómputo
3. **Validación necesaria**: Debes verificar que el modelo sea correcto
4. **Números aleatorios**: Depende de la calidad del generador

---

## 5. CONCLUSIONES DE LAS SIMULACIONES

### Juego de Volados

**Pregunta original**: ¿Cuál es la probabilidad de llegar a $50 partiendo con $30?

**Respuesta**: ~60% (basado en 1000 simulaciones)

**Aprendizaje**:
- La estrategia de doblar apuesta tiene ventajas y riesgos
- El capital inicial y la distancia a la meta son determinantes
- Aunque la moneda sea justa, el resultado no es 50-50 por la estructura del juego

### Estimación de π

**Pregunta original**: ¿Se puede estimar π usando simulación?

**Respuesta**: Sí, con suficientes puntos

**Aprendizaje**:
- 10,000 puntos dan precisión de 2 decimales (~3.14)
- 100,000 puntos dan precisión de 4 decimales (~3.1416)
- El método es correcto pero ineficiente comparado con fórmulas matemáticas
- Excelente ejemplo didáctico del método Monte Carlo

---

## 6. RECOMENDACIONES PARA PRESENTACIÓN

### Al explicar a tu docente enfatiza:

1. **Fundamento teórico**: Por qué funciona el método Monte Carlo

2. **Proceso de simulación**: 
   - Generación de números aleatorios
   - Reglas del sistema
   - Criterios de término

3. **Análisis de resultados**:
   - Tablas con datos
   - Gráficos de visualización
   - Interpretación estadística

4. **Validación**:
   - Comparar con resultados del libro
   - Verificar convergencia
   - Análisis de sensibilidad

5. **Conclusiones prácticas**:
   - Qué aprendiste del comportamiento del sistema
   - Aplicaciones en la vida real
   - Limitaciones del método

### Datos importantes para reportar:

**Para Juego de Volados**:
- Número de corridas simuladas
- Probabilidad estimada de éxito
- Promedio de volados por corrida
- Comparación con resultado del libro (0.6)

**Para Estimación de π**:
- Número de puntos simulados
- Valor estimado de π
- Error absoluto y porcentual
- Gráfico de convergencia

---

## 7. APLICACIONES REALES DEL MÉTODO MONTE CARLO

- **Finanzas**: Valoración de opciones, riesgo de portafolios
- **Física**: Simulación de partículas, mecánica cuántica
- **Ingeniería**: Análisis de confiabilidad, tolerancias
- **Medicina**: Dosificación de radiación, epidemiología
- **Logística**: Optimización de inventarios, cadenas de suministro

El método Monte Carlo es una herramienta fundamental en modelación y simulación de sistemas complejos.
