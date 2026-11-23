<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { mapaCalor } from '../graficos'
import { num } from '../api'

// Tabla de contingencia resuelta con el procedimiento de clase:
// descripción (X independiente, Y dependiente) → hipótesis → datos → FO → FE → X0² → teórico → relación → conclusión → recomendación.
// compacto: sin tarjeta ni título, para mostrarlo dentro de la pantalla de un criterio
const props = defineProps({ c: Object, urlCsv: String, urlExcel: String, compacto: Boolean })

// Grupos con muy pocas personas (los que dejan poco confiable la prueba), dichos con nombre y cantidad
const grupoChico = computed(() => {
  const c = props.c
  if (!c || !c.celdasFeMenor5) return ''
  const chicos = [
    ...c.filas.map((f, i) => [f, c.totalFila[i]]),
    ...c.columnas.map((col, j) => [col, c.totalColumna[j]]),
  ].filter(([, n]) => n < 10).map(([nombre, n]) => `«${nombre}» (${n} ${n === 1 ? 'persona' : 'personas'})`)
  return chicos.length ? `hay pocas respuestas en ${chicos.join(', ')}` : 'algunas combinaciones tienen muy pocas personas'
})
// Suma de (FO - FE)² / FE celda por celda, para mostrar de dónde sale X0²
const terminos = computed(() => {
  const c = props.c
  if (!c?.esperada) return []
  return c.observada.flatMap((fila, i) => fila.map((fo, j) => ({
    celda: `${c.filas[i]} · ${c.columnas[j]}`, fo, fe: c.esperada[i][j], valor: (fo - c.esperada[i][j]) ** 2 / c.esperada[i][j],
  })))
})
const filasMenos1 = computed(() => props.c.filas.length - 1)
const columnasMenos1 = computed(() => props.c.columnas.length - 1)
</script>

<template>
  <component :is="compacto ? 'div' : 'n-card'" size="small">
    <div v-if="compacto" class="compacto-cabecera">
      <span />
      <n-space size="small" :wrap="false">
        <n-button size="tiny" tag="a" :href="urlCsv" secondary>CSV</n-button>
        <n-button size="tiny" tag="a" :href="urlExcel" secondary>Excel</n-button>
      </n-space>
    </div>
    <template v-if="!compacto" #header>{{ c.titulo }}</template>
    <template v-if="!compacto" #header-extra>
      <n-space size="small">
        <n-button size="small" tag="a" :href="urlCsv" secondary>CSV</n-button>
        <n-button size="small" tag="a" :href="urlExcel" secondary>Excel</n-button>
      </n-space>
    </template>

    <n-alert v-if="c.independientes == null" type="info" :bordered="false">
      No hay respuestas suficientes para aplicar la prueba (se necesitan al menos dos categorías en cada pregunta).
    </n-alert>
    <template v-else>
      <div class="conclusion" :class="{ relacion: !c.independientes }">
        <b>{{ c.independientes ? 'Independencia' : 'Existe dependencia' }}:</b>
        <template v-if="c.independientes"> «{{ c.variableFilas }}» no influye en «{{ c.variableColumnas }}»; las diferencias pueden deberse al azar.</template>
        <template v-else> «{{ c.variableColumnas }}» cambia según «{{ c.variableFilas }}»; la diferencia es demasiado grande para ser casualidad.</template>
        <span v-if="grupoChico" class="aviso">Tómalo con cuidado: {{ grupoChico }}; la conclusión podría cambiar con más respuestas.</span>
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 1. Describir el problema</div>
        <p>{{ c.proposito }}</p>
        <div class="variables">
          <span><b>X</b> (variable independiente): {{ c.variableFilas }}</span>
          <span><b>Y</b> (variable dependiente): {{ c.variableColumnas }}</span>
          <span>Población: {{ c.poblacion }} (N = {{ c.n }})</span>
        </div>
        <p v-if="c.nota" class="nota">{{ c.nota }}</p>
      </div>

      <div class="paso">
        <div class="paso-titulo">Hipótesis</div>
        <p><b>H₀:</b> {{ c.hipotesisNula }}</p>
        <p><b>H₁:</b> {{ c.hipotesisAlterna }}</p>
      </div>

      <div class="paso" v-if="c.muestra?.length">
        <div class="paso-titulo">Datos <span class="nota">(primeros {{ c.muestra.length }} de {{ c.n }} titulados)</span></div>
        <n-table size="small" :single-line="false" class="tabla-chica">
          <thead><tr><th>Titulado</th><th>X: {{ c.variableFilas }}</th><th>Y: {{ c.variableColumnas }}</th></tr></thead>
          <tbody><tr v-for="m in c.muestra" :key="m[0]"><td>{{ m[0] }}</td><td>{{ m[1] }}</td><td>{{ m[2] }}</td></tr></tbody>
        </n-table>
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 2. Tabla de contingencia (frecuencia observada FO<sub>ij</sub>)</div>
        <n-table size="small" :single-line="false">
          <thead>
            <tr><th>{{ c.variableFilas }} (X) \ {{ c.variableColumnas }} (Y)</th><th v-for="col in c.columnas" :key="col">{{ col }}</th><th>Total</th></tr>
          </thead>
          <tbody>
            <tr v-for="(fila, i) in c.filas" :key="fila">
              <td><b>{{ fila }}</b></td>
              <td v-for="(col, j) in c.columnas" :key="col">{{ c.observada[i][j] }}</td>
              <td>{{ c.totalFila[i] }}</td>
            </tr>
            <tr><td><b>Total</b></td><td v-for="(t, j) in c.totalColumna" :key="j">{{ t }}</td><td><b>{{ c.n }}</b></td></tr>
          </tbody>
        </n-table>
        <v-chart :style="{ height: Math.max(200, c.filas.length * 44 + 80) + 'px', marginTop: '10px' }" :option="mapaCalor(c)" autoresize />
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 3. Frecuencia esperada (FE<sub>ij</sub> = total fila × total columna / N)</div>
        <n-table size="small" :single-line="false">
          <thead><tr><th>{{ c.variableFilas }} (X) \ {{ c.variableColumnas }} (Y)</th><th v-for="col in c.columnas" :key="col">{{ col }}</th></tr></thead>
          <tbody>
            <tr v-for="(fila, i) in c.filas" :key="fila">
              <td><b>{{ fila }}</b></td>
              <td v-for="(col, j) in c.columnas" :key="col" :class="{ bajo: c.esperada[i][j] < 5 }">
                {{ c.totalFila[i] }} × {{ c.totalColumna[j] }} / {{ c.n }} = {{ num(c.esperada[i][j]) }}
              </td>
            </tr>
          </tbody>
        </n-table>
        <p v-if="c.celdasFeMenor5" class="nota">{{ c.celdasFeMenor5 }} de {{ c.totalCeldas }} celdas tienen FE &lt; 5 (marcadas): la prueba pierde confiabilidad.</p>
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 4. Estadístico chi-cuadrado: X₀² = Σ (FO − FE)² / FE</div>
        <n-table size="small" :single-line="false" class="tabla-chica">
          <thead><tr><th>Celda</th><th>FO</th><th>FE</th><th>(FO − FE)² / FE</th></tr></thead>
          <tbody>
            <tr v-for="t in terminos" :key="t.celda"><td>{{ t.celda }}</td><td>{{ t.fo }}</td><td>{{ num(t.fe) }}</td><td>{{ num(t.valor, 4) }}</td></tr>
            <tr><td colspan="3" style="text-align: right"><b>X₀²</b></td><td><b>{{ num(c.chiCuadrada, 4) }}</b></td></tr>
          </tbody>
        </n-table>
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 5. Estadístico teórico</div>
        <p>
          Grados de libertad = (f − 1)(c − 1) = ({{ c.filas.length }} − 1)({{ c.columnas.length }} − 1) = {{ filasMenos1 }} × {{ columnasMenos1 }} = <b>{{ c.gradosLibertad }}</b>,
          α = 0.05 → X²<sub>{{ c.gradosLibertad }}, 0.05</sub> = <b>{{ num(c.chiTeorico, 3) }}</b>
        </p>
      </div>

      <div class="paso">
        <div class="paso-titulo">Paso 6. Relación entre estadísticos y conclusión</div>
        <p>
          Si (X₀² &lt; X²<sub>(f−1)(c−1), α</sub>) → «Independencia»; sino → «NO independencia».
          <br />
          X₀² = {{ num(c.chiCuadrada, 3) }} {{ c.independientes ? '<' : '≥' }} {{ num(c.chiTeorico, 3) }}
          → <b>{{ c.independientes ? 'Independencia: no se rechaza H₀' : 'NO independencia: se rechaza H₀ y se acepta H₁' }}</b>
        </p>
      </div>

      <div class="paso" v-if="c.recomendacion">
        <div class="paso-titulo">Recomendación</div>
        <p>{{ c.recomendacion }}</p>
      </div>
    </template>
  </component>
</template>

<style scoped>
.conclusion { font-size: 14.5px; line-height: 1.5; padding: 10px 14px; border-radius: 10px; background: var(--fondo); }
.conclusion.relacion { background: var(--acento-suave); }
.aviso { display: block; font-size: 12.5px; color: var(--aviso); margin-top: 4px; }
.compacto-cabecera { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 8px; }
.paso { margin-top: 16px; }
.paso-titulo { font-weight: 600; font-size: 13.5px; margin-bottom: 6px; }
.paso p { margin: 4px 0; font-size: 14px; line-height: 1.55; }
.variables { display: flex; flex-wrap: wrap; gap: 6px 18px; font-size: 13.5px; margin-top: 4px; }
.tabla-chica { max-width: 640px; }
.bajo { background: var(--aviso-suave); }
</style>
