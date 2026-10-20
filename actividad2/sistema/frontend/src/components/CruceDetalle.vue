<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { mapaCalor } from '../graficos'

// Detalle de una tabla de contingencia con su prueba chi-cuadrada (cruces del estudio y comparación libre).
// compacto: sin tarjeta ni título, para mostrarlo dentro de la pantalla de un criterio
const props = defineProps({ c: Object, urlCsv: String, urlExcel: String, compacto: Boolean })

// Grupos con muy pocas personas (los que dejan poco confiable la comparación), dichos con nombre y cantidad
const grupoChico = computed(() => {
  const c = props.c
  if (!c || !c.celdasFeMenor5) return ''
  const chicos = [
    ...c.filas.map((f, i) => [f, c.totalFila[i]]),
    ...c.columnas.map((col, j) => [col, c.totalColumna[j]]),
  ].filter(([, n]) => n < 10).map(([nombre, n]) => `«${nombre}» (${n} ${n === 1 ? 'persona' : 'personas'})`)
  return chicos.length
    ? `hay pocas respuestas en ${chicos.join(', ')}`
    : 'algunas combinaciones tienen muy pocas personas'
})
</script>

<template>
  <component :is="compacto ? 'div' : 'n-card'" size="small">
    <div v-if="compacto" class="compacto-cabecera">
      <div class="nota" style="margin: 0">{{ c.proposito }} Población: {{ c.poblacion }} (N = {{ c.n }}).</div>
      <n-space size="small" :wrap="false">
        <n-button size="tiny" tag="a" :href="urlCsv" secondary>CSV</n-button>
        <n-button size="tiny" tag="a" :href="urlExcel" secondary>Excel</n-button>
      </n-space>
    </div>
    <template v-if="!compacto" #header>
      <div>{{ c.titulo }}</div>
      <div class="nota" style="margin: 2px 0 0; font-weight: 400">
        {{ c.proposito }} Población: {{ c.poblacion }} (N = {{ c.n }}).
      </div>
    </template>
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
        <template v-if="c.independientes">
          En todos los grupos de «{{ c.variableFilas }}» las respuestas de «{{ c.variableColumnas }}» son parecidas:
          <b>{{ c.variableFilas.toLowerCase() }} no cambia {{ c.variableColumnas.toLowerCase() }}</b>. Las pequeñas diferencias que se ven pueden ser casualidad.
        </template>
        <template v-else>
          Las respuestas de «{{ c.variableColumnas }}» cambian según «{{ c.variableFilas }}»:
          <b>las dos preguntas están relacionadas</b>, y la diferencia es demasiado grande para ser casualidad.
        </template>
        <span v-if="grupoChico" class="aviso">
          Tómalo con cuidado: {{ grupoChico }}. Con tan pocas personas no se puede asegurar nada sobre ese grupo, y la conclusión podría cambiar con más respuestas.
        </span>
      </div>
      <v-chart :style="{ height: Math.max(220, c.filas.length * 46 + 90) + 'px', marginTop: '10px' }" :option="mapaCalor(c)" autoresize />

      <div class="sub" style="margin-top: 12px">Cantidad de respuestas</div>
      <n-table size="small" :single-line="false" style="margin-top: 6px">
        <thead>
          <tr><th>{{ c.variableFilas }} \ {{ c.variableColumnas }}</th><th v-for="col in c.columnas" :key="col">{{ col }}</th><th>Total</th></tr>
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
    </template>
  </component>
</template>

<style scoped>
.sub { font-size: 12.5px; color: #71717a; font-weight: 500; }
.conclusion { font-size: 14.5px; line-height: 1.5; padding: 10px 14px; border-radius: 10px; background: #f4f4f5; }
.conclusion.relacion { background: #eef2ff; }
.aviso { display: block; font-size: 12.5px; color: #92400e; margin-top: 4px; }
.compacto-cabecera { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 12px; }
</style>
