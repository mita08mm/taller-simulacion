<script setup>
import VChart from 'vue-echarts'
import { frecuenciaEsperada } from '../graficos'
import { num, pct } from '../api'

// Prueba de las frecuencias (principio de uniformidad) con los 6 pasos de los apuntes de clase:
// muestra → FE = n/k → X0² → teórico → relación → conclusión → recomendación.
defineProps({ u: Object })
</script>

<template>
  <div>
    <n-alert v-if="u.uniforme == null" type="info" :bordered="false">No hay respuestas suficientes para aplicar la prueba.</n-alert>
    <template v-else>
      <div class="conclusion" :class="{ preferida: !u.uniforme }">
        <b>{{ u.uniforme ? 'Uniformidad' : 'No uniformidad' }}:</b>
        <template v-if="u.uniforme"> las respuestas se reparten de forma pareja entre las {{ u.k }} opciones; ninguna se destaca más allá del azar.</template>
        <template v-else> hay opciones preferidas de verdad, no por azar. La más elegida es «{{ u.masElegida }}» ({{ u.frecuenciaMasElegida }} de {{ u.n }}, {{ pct(u.proporcionMasElegida) }}).</template>
        <span v-if="u.feMenor5" class="aviso">Tómalo con cuidado: FE &lt; 5, hay pocas respuestas para tantas opciones.</span>
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 1. Contar con la muestra y determinar su tamaño</div>
        <p>{{ u.proposito }}</p>
        <div class="variables">
          <span>Población: {{ u.poblacion }}</span>
          <span>n = <b>{{ u.n }}</b></span>
          <span>k = <b>{{ u.k }}</b> opciones</span>
          <span>α = 0.05</span>
        </div>
        <p><b>H₀:</b> {{ u.hipotesisNula }}</p>
        <p><b>H₁:</b> {{ u.hipotesisAlterna }}</p>
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 2. Frecuencia esperada</div>
        <p>FE = n / k = {{ u.n }} / {{ u.k }} = <b>{{ num(u.esperada) }}</b> respuestas por opción si nadie tuviera preferencia.</p>
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 3. Estadístico chi-cuadrado: x₀² = Σ (FO − FE)² / FE</div>
        <n-table size="small" :single-line="false" class="tabla-chica">
          <thead><tr><th>Opción</th><th>FO</th><th>FE</th><th>(FO − FE)² / FE</th></tr></thead>
          <tbody>
            <tr v-for="(cat, i) in u.categorias" :key="cat">
              <td>{{ cat }}</td><td>{{ u.observada[i] }}</td><td>{{ num(u.esperada) }}</td><td>{{ num(u.aportes[i], 4) }}</td>
            </tr>
            <tr><td><b>Total</b></td><td><b>{{ u.n }}</b></td><td>{{ u.n }}</td><td><b>x₀² = {{ num(u.chiCuadrada, 4) }}</b></td></tr>
          </tbody>
        </n-table>
        <v-chart :style="{ height: Math.max(200, u.k * 40 + 70) + 'px', marginTop: '10px', maxWidth: '640px' }" :option="frecuenciaEsperada(u)" autoresize />
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 4. Estadístico teórico</div>
        <p>x²<sub>α, k−1</sub> = x²<sub>0.05, {{ u.k }}−1</sub> = x²<sub>0.05, {{ u.gradosLibertad }}</sub> = <b>{{ num(u.chiTeorico, 3) }}</b> (tabla chi-cuadrada)</p>
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 5 y 6. Relación entre estadísticos y conclusión</div>
        <p>
          Si (x₀² &lt; x²<sub>α, k−1</sub>) → «Uniformidad»; sino → «No uniformidad».
          <br />
          x₀² = {{ num(u.chiCuadrada, 3) }} {{ u.uniforme ? '<' : '≥' }} {{ num(u.chiTeorico, 3) }}
          → <b>{{ u.uniforme ? 'Uniformidad: no se rechaza H₀' : 'No uniformidad: se rechaza H₀' }}</b>
        </p>
      </div>

      <div class="paso" v-if="u.recomendacion">
        <div class="paso-titulo">Recomendación</div>
        <p>{{ u.recomendacion }}</p>
      </div>
    </template>
  </div>
</template>

<style scoped>
.conclusion { font-size: 14.5px; line-height: 1.5; padding: 10px 14px; border-radius: 10px; background: var(--fondo); }
.conclusion.preferida { background: var(--acento-suave); }
.aviso { display: block; font-size: 12.5px; color: var(--aviso); margin-top: 4px; }
.paso { margin-top: 16px; }
.paso-titulo { font-weight: 600; font-size: 13.5px; margin-bottom: 6px; }
.paso p { margin: 4px 0; font-size: 14px; line-height: 1.55; }
.variables { display: flex; flex-wrap: wrap; gap: 6px 18px; font-size: 13.5px; margin: 4px 0 6px; }
.tabla-chica { max-width: 640px; }
</style>
