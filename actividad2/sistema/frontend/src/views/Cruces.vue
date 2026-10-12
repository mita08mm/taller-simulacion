<script setup>
import { ref, computed, inject } from 'vue'
import VChart from 'vue-echarts'
import Estado from '../components/Estado.vue'
import { useApi } from '../useApi'
import { num, urlExportar } from '../api'
import { mapaCalor } from '../graficos'

const { datos: cruces, error, cargando } = useApi('/api/cruces')
const elegido = ref('C1')
const filtro = inject('filtro')
const c = computed(() => cruces.value?.find((x) => x.id === elegido.value))
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">Análisis · Matrices de frecuencia cruzada</div>
    <h1 class="pagina-titulo">Cruces de variables y prueba chi-cuadrada</h1>
    <p class="pagina-sub">Cada cruce responde una pregunta de un criterio ARCU-SUR. Nivel de significancia α = 0.05.</p>
    <Estado :cargando="cargando && !cruces" :error="error" />

    <template v-if="cruces">
      <n-grid :cols="7" :x-gap="10" :y-gap="10" responsive="screen" item-responsive>
        <n-gi v-for="x in cruces" :key="x.id" span="7 m:3 l:1">
          <div class="chip" :class="{ activo: x.id === elegido }" @click="elegido = x.id">
            <div class="chip-id">{{ x.id }}</div>
            <n-tag size="tiny" round :bordered="false" :type="x.independientes === false ? 'warning' : 'default'">
              {{ x.independientes == null ? 'Sin datos' : x.independientes ? 'Independientes' : 'Relacionadas' }}
            </n-tag>
          </div>
        </n-gi>
      </n-grid>

      <n-card v-if="c" size="small" style="margin-top: 14px">
        <template #header>
          <div>{{ c.titulo }}</div>
          <div class="nota" style="margin: 2px 0 0; font-weight: 400">{{ c.proposito }} Criterio {{ c.criterio }} · Población: {{ c.poblacion }} (N = {{ c.n }})</div>
        </template>
        <template #header-extra>
          <n-button size="small" tag="a" :href="urlExportar(c.id, filtro)" secondary>Exportar CSV</n-button>
        </template>

        <n-alert v-if="c.independientes == null" type="info" :bordered="false">No hay respuestas suficientes en el período elegido para aplicar la prueba.</n-alert>
        <n-grid v-else :cols="5" :x-gap="18" :y-gap="14" responsive="screen" item-responsive>
          <n-gi span="5 l:3">
            <div class="sub">Mapa de calor (frecuencia observada)</div>
            <v-chart style="height: 320px" :option="mapaCalor(c)" autoresize />
          </n-gi>
          <n-gi span="5 l:2">
            <div class="sub">Pasos de la prueba</div>
            <ol class="pasos">
              <li>Tabla de contingencia con N = {{ c.n }}.</li>
              <li>FE<sub>ij</sub> = (total fila × total columna) / N.</li>
              <li>X₀² = Σ (FO − FE)² / FE = <b>{{ num(c.chiCuadrada, 3) }}</b></li>
              <li>Teórico X²<sub>0.05, {{ c.gradosLibertad }}</sub> = <b>{{ num(c.chiTeorico, 3) }}</b></li>
              <li>{{ num(c.chiCuadrada, 3) }} {{ c.independientes ? '<' : '≥' }} {{ num(c.chiTeorico, 3) }}</li>
              <li>
                <n-tag :type="c.independientes ? 'success' : 'warning'" :bordered="false">
                  {{ c.independientes ? 'No se rechaza la independencia' : 'Hay evidencia de relación entre las variables' }}
                </n-tag>
              </li>
            </ol>
            <n-alert v-if="c.celdasFeMenor5 > 0" type="warning" :bordered="false" style="margin-top: 10px">
              {{ c.celdasFeMenor5 }} de {{ c.totalCeldas }} celdas tienen FE &lt; 5: la prueba pierde confiabilidad. Conviene agrupar más categorías o juntar más respuestas.
            </n-alert>
          </n-gi>
        </n-grid>

        <template v-if="c.independientes != null">
        <div class="sub" style="margin-top: 12px">Frecuencia observada (frecuencia esperada)</div>
        <n-table size="small" :single-line="false" style="margin-top: 6px">
          <thead>
            <tr><th>{{ c.variableFilas }} \ {{ c.variableColumnas }}</th><th v-for="col in c.columnas" :key="col">{{ col }}</th><th>Total</th></tr>
          </thead>
          <tbody>
            <tr v-for="(fila, i) in c.filas" :key="fila">
              <td><b>{{ fila }}</b></td>
              <td v-for="(col, j) in c.columnas" :key="col">{{ c.observada[i][j] }} <span class="fe">({{ num(c.esperada[i][j]) }})</span></td>
              <td>{{ c.totalFila[i] }}</td>
            </tr>
            <tr><td><b>Total</b></td><td v-for="(t, j) in c.totalColumna" :key="j">{{ t }}</td><td><b>{{ c.n }}</b></td></tr>
          </tbody>
        </n-table>
        </template>
      </n-card>
    </template>
  </div>
</template>

<style scoped>
.chip { border: 1px solid #e4e4e7; background: #fff; border-radius: 10px; padding: 10px 12px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; transition: all .15s; }
.chip:hover { border-color: #a5b4fc; }
.chip.activo { border-color: #4f46e5; box-shadow: 0 0 0 3px #eef2ff; }
.chip-id { font-weight: 650; }
.sub { font-size: 12.5px; color: #71717a; font-weight: 500; }
.pasos { padding-left: 18px; margin: 8px 0 0; font-size: 13.5px; line-height: 2; }
.fe { color: #a1a1aa; font-size: 12px; }
</style>
