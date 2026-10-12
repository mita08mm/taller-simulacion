<script setup>
import { ref, computed } from 'vue'
import VChart from 'vue-echarts'
import Kpi from '../components/Kpi.vue'
import Estado from '../components/Estado.vue'
import { useApi } from '../useApi'
import { pct, num } from '../api'
import { barras, dona, likertApilado, COLORES } from '../graficos'

const { datos: d, error, cargando } = useApi('/api/empleabilidad')

const destinos = [
  { key: 'rubro', label: 'Rubro' },
  { key: 'sector_publico_privado', label: 'Sector' },
  { key: 'departamento_bolivia', label: 'Departamento' },
  { key: 'cargo', label: 'Cargo' },
  { key: 'area_organizacion', label: 'Área' },
  { key: 'remuneracion_promedio', label: 'Remuneración' },
  { key: 'antiguedad_trabajo', label: 'Antigüedad' },
  { key: 'medio_obtuvo_trabajo', label: 'Medio' },
]
const destino = ref('rubro')
const concordancia = computed(() => d.value && [
  { nombre: 'Labores pertinentes a la formación (puesto actual)', frecuencias: d.value.pertinenciaPuesto.frecuencias },
  { nombre: 'Concordancia formación – requerimientos del mercado', frecuencias: d.value.concordanciaRequerimientos.frecuencias },
])
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">Titulados · ARCU-SUR 3.2.3 Condiciones de empleo</div>
    <h1 class="pagina-titulo">Antigüedad y empleabilidad</h1>
    <p class="pagina-sub">Perfil temporal de los titulados, tasa de inserción y destino laboral.</p>
    <Estado :cargando="cargando && !d" :error="error" />

    <template v-if="d">
      <n-grid :cols="4" :x-gap="14" :y-gap="14" responsive="screen" item-responsive>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Tasa de empleo" :valor="pct(d.tasaEmpleo.proporcion)" :detalle="`${d.tasaEmpleo.casos} / ${d.tasaEmpleo.total}`" /></n-gi>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Tiempo medio al primer empleo" color="#14b8a6" :valor="`${num(d.tiempoPrimerEmpleo.mesesIC95.media, 1)} m`" :detalle="`IC 95%: ${num(d.tiempoPrimerEmpleo.mesesIC95.limiteInferior, 1)} – ${num(d.tiempoPrimerEmpleo.mesesIC95.limiteSuperior, 1)}`" /></n-gi>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Años de vida profesional" color="#f59e0b" :valor="num(d.aniosVidaProfesional.media, 1)" :detalle="`S = ${num(d.aniosVidaProfesional.desviacion)} · IC 95%: ${num(d.aniosVidaProfesional.limiteInferior, 1)} – ${num(d.aniosVidaProfesional.limiteSuperior, 1)}`" /></n-gi>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Años desempleado" color="#ef4444" :valor="num(d.aniosDesempleado.media, 2)" :detalle="`S = ${num(d.aniosDesempleado.desviacion)} · descartadas: ${d.filasInconsistentesDescartadas}`" /></n-gi>
      </n-grid>

      <n-grid :cols="2" :x-gap="14" :y-gap="14" style="margin-top: 14px" responsive="screen" item-responsive>
        <n-gi span="2 l:1">
          <n-card title="Tiempo hasta el primer empleo" size="small">
            <v-chart class="grafico" :option="barras(d.tiempoPrimerEmpleo.frecuencias, { color: COLORES[1] })" autoresize />
            <div class="nota">{{ d.tiempoPrimerEmpleo.nota }}</div>
          </n-card>
        </n-gi>
        <n-gi span="2 l:1">
          <n-card title="Situación laboral actual" size="small">
            <v-chart class="grafico" :option="dona(d.situacionLaboral)" autoresize />
            <div class="nota">Tasa de empleo = trabaja en una organización + emprendimiento propio.</div>
          </n-card>
        </n-gi>
      </n-grid>

      <n-card size="small" style="margin-top: 14px" title="Destino laboral">
        <template #header-extra><span class="nota" style="margin: 0">Solo quienes trabajan en una organización</span></template>
        <n-tabs v-model:value="destino" type="segment" size="small" animated>
          <n-tab v-for="t in destinos" :key="t.key" :name="t.key">{{ t.label }}</n-tab>
        </n-tabs>
        <v-chart class="grafico-alto" :option="barras(d.destinoLaboral[destino], { horizontal: true, porcentaje: true })" autoresize />
      </n-card>

      <n-card size="small" style="margin-top: 14px" title="Concordancia entre titulación y empleo">
        <v-chart style="height: 200px" :option="likertApilado(concordancia)" autoresize />
        <div class="nota">
          Media de pertinencia: {{ num(d.pertinenciaPuesto.media?.media) }} (n = {{ d.pertinenciaPuesto.n }}) ·
          Media de concordancia: {{ num(d.concordanciaRequerimientos.media?.media) }} (n = {{ d.concordanciaRequerimientos.n }}) — escala 1 a 4.
        </div>
      </n-card>

      <n-grid :cols="3" :x-gap="14" :y-gap="14" style="margin-top: 14px" responsive="screen" item-responsive>
        <n-gi span="3 l:1"><n-card title="¿Es su primer empleo?" size="small"><v-chart class="grafico" :option="dona(d.primerEmpleo)" autoresize /></n-card></n-gi>
        <n-gi span="3 l:1"><n-card title="Empleos desde la titulación" size="small"><v-chart class="grafico" :option="barras(d.cantidadEmpleos, { color: COLORES[4] })" autoresize /></n-card></n-gi>
        <n-gi span="3 l:1"><n-card title="Razón por la que no trabaja" size="small"><v-chart class="grafico" :option="barras(d.razonNoTrabaja, { horizontal: true, color: COLORES[3] })" autoresize /></n-card></n-gi>
      </n-grid>

      <n-card title="Emprendimiento propio" size="small" style="margin-top: 14px">
        <n-grid :cols="3" :x-gap="14" responsive="screen" item-responsive>
          <n-gi span="3 l:1"><div class="sub">Origen</div><v-chart style="height: 240px" :option="barras(d.emprendimiento.origen_emprendimiento, { horizontal: true, color: COLORES[6] })" autoresize /></n-gi>
          <n-gi span="3 l:1"><div class="sub">Financiamiento del negocio</div><v-chart style="height: 240px" :option="dona(d.emprendimiento.financiamiento_externo_negocio)" autoresize /></n-gi>
          <n-gi span="3 l:1"><div class="sub">Importancia de la formación para el negocio</div><v-chart style="height: 240px" :option="barras(d.emprendimiento.likert_importancia_formacion_negocio, { color: COLORES[4] })" autoresize /></n-gi>
        </n-grid>
      </n-card>
    </template>
  </div>
</template>

<style scoped>
.sub { font-size: 12.5px; color: #71717a; font-weight: 500; }
</style>
