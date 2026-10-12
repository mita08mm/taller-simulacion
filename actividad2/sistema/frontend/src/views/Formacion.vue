<script setup>
import VChart from 'vue-echarts'
import Kpi from '../components/Kpi.vue'
import Estado from '../components/Estado.vue'
import { useApi } from '../useApi'
import { pct, num } from '../api'
import { barras, dona, likertApilado, COLORES } from '../graficos'

const { datos: d, error, cargando } = useApi('/api/formacion')
const fin = useApi('/api/financiamiento')
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">Titulados · ARCU-SUR 2.4.1 y 1.1.5</div>
    <h1 class="pagina-titulo">Formación y posgrado</h1>
    <p class="pagina-sub">Qué formación complementaria tienen los titulados, qué les interesa estudiar, dónde y cómo la financiaron.</p>
    <Estado :cargando="cargando && !d" :error="error" />

    <template v-if="d">
      <n-grid :cols="3" :x-gap="14" :y-gap="14" responsive="screen" item-responsive>
        <n-gi span="3 m:1"><Kpi titulo="Con formación complementaria" :valor="pct(d.conFormacionComplementaria.proporcion)" :detalle="`${d.conFormacionComplementaria.casos} de ${d.conFormacionComplementaria.total}`" /></n-gi>
        <n-gi span="3 m:1"><Kpi titulo="Interesados en un posgrado" color="#14b8a6" :valor="pct(d.interesPosgrado.proporcion)" :detalle="`${d.interesPosgrado.casos} de ${d.interesPosgrado.total}`" /></n-gi>
        <n-gi span="3 m:1"><Kpi titulo="Oferta FCyT afín (media 1–4)" color="#f59e0b" :valor="num(d.ofertaPosgradoFcyt.media?.media)" :detalle="`IC 95%: ${num(d.ofertaPosgradoFcyt.media?.limiteInferior)} – ${num(d.ofertaPosgradoFcyt.media?.limiteSuperior)}`" /></n-gi>
      </n-grid>

      <n-grid :cols="2" :x-gap="14" :y-gap="14" style="margin-top: 14px" responsive="screen" item-responsive>
        <n-gi span="2 l:1"><n-card title="Área de interés para un posgrado" size="small"><v-chart class="grafico" :option="barras(d.areaInteres, { horizontal: true, porcentaje: true })" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Dónde le gustaría estudiarlo" size="small"><v-chart class="grafico" :option="barras(d.organizacionInteres, { horizontal: true, porcentaje: true, color: COLORES[1] })" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Nivel del programa que cursó o cursa" size="small"><v-chart class="grafico" :option="barras(d.programaNivel, { color: COLORES[2] })" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Dónde lo cursó" size="small"><v-chart class="grafico" :option="barras(d.programaDonde, { horizontal: true, color: COLORES[5] })" autoresize /></n-card></n-gi>
      </n-grid>

      <n-card title="¿Hay programas de posgrado de la FCyT afines a Ingeniería de Sistemas?" size="small" style="margin-top: 14px">
        <v-chart style="height: 150px" :option="likertApilado([{ nombre: 'Oferta de posgrado FCyT afín', frecuencias: d.ofertaPosgradoFcyt.frecuencias }])" autoresize />
      </n-card>

      <n-card v-if="fin.datos.value" title="Fuente de financiamiento del programa cursado" size="small" style="margin-top: 14px">
        <n-grid :cols="2" :x-gap="14" responsive="screen" item-responsive>
          <n-gi span="2 l:1"><v-chart class="grafico" :option="dona(fin.datos.value.programaFinanciamiento)" autoresize /></n-gi>
          <n-gi span="2 l:1">
            <n-table size="small" :bordered="false" :single-line="false">
              <thead><tr><th>Fuente</th><th>Frecuencia</th><th>%</th></tr></thead>
              <tbody>
                <tr v-for="f in fin.datos.value.programaFinanciamiento" :key="f.codigo"><td>{{ f.etiqueta }}</td><td>{{ f.frecuencia }}</td><td>{{ pct(f.relativa) }}</td></tr>
              </tbody>
            </n-table>
            <div class="nota">{{ fin.datos.value.nota }} El cruce con el nivel cursado está en «Matrices cruzadas» (C7).</div>
          </n-gi>
        </n-grid>
      </n-card>
    </template>
  </div>
</template>
