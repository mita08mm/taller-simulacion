<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import Kpi from '../components/Kpi.vue'
import Estado from '../components/Estado.vue'
import { useApi } from '../useApi'
import { pct } from '../api'
import { barras, dona, likertApilado, barrasAgrupadas, COLORES } from '../graficos'

const { datos: d, error, cargando } = useApi('/api/empleadores')
const parte = (lista, codigo) => lista.find((f) => f.codigo === codigo)?.relativa ?? 0

const perfil = computed(() => d.value && d.value.perfil.map((p) => ({ nombre: p.criterio, frecuencias: p.frecuencias })))
const homologados = computed(() => {
  if (!d.value) return null
  const h = d.value.cargosHomologados
  const aPct = (v) => { const t = v.reduce((s, x) => s + x, 0) || 1; return v.map((x) => +((x / t) * 100).toFixed(1)) }
  return barrasAgrupadas(h.grupos, [
    { nombre: 'Titulados (cargo actual, %)', valores: aPct(h.titulados) },
    { nombre: 'Empleadores (cargos que ocupan, %)', valores: aPct(h.empleadores) },
  ])
})
const abiertas = [
  { key: 'areas_emergentes', titulo: 'Áreas y tendencias emergentes' },
  { key: 'herramientas', titulo: 'Herramientas tecnológicas' },
  { key: 'habilidades', titulo: 'Habilidades y competencias' },
]
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">ARCU-SUR 2.1.2 Perfil de egreso · 2.4.2 Relaciones con el sector público y privado</div>
    <h1 class="pagina-titulo">Perfil y demanda de los empleadores</h1>
    <p class="pagina-sub">Con pocas organizaciones encuestadas el análisis es solo descriptivo (no se aplica chi-cuadrada ni intervalos de confianza).</p>
    <Estado :cargando="cargando && !d" :error="error" />

    <template v-if="d">
      <n-grid :cols="4" :x-gap="14" :y-gap="14" responsive="screen" item-responsive>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Organizaciones encuestadas" :valor="String(d.n)" :detalle="`${d.empresas.filter((e) => e.tipo === 'Público').length} públicas · ${d.empresas.filter((e) => e.tipo === 'Privado').length} privadas`" /></n-gi>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Contrataron en los últimos 5 años" color="#14b8a6" :valor="pct(parte(d.contrato_ultimos_5_anios, 'SI'))" /></n-gi>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Alta probabilidad de contratar" color="#f59e0b" :valor="pct(parte(d.probabilidad_contratacion, 'alta'))" /></n-gi>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Demandan solo licenciatura" color="#8b5cf6" :valor="pct(parte(d.nivel_formacion_demandado, 'Licenciatura'))" /></n-gi>
      </n-grid>

      <n-card title="Formación y perfil profesional" size="small" style="margin-top: 14px">
        <v-chart style="height: 440px" :option="likertApilado(perfil)" autoresize />
        <div class="nota">Porcentaje de organizaciones por opción. Los ítems de competencias y valores agrupan varias afirmaciones, así que se leen como una valoración global.</div>
      </n-card>

      <n-grid :cols="2" :x-gap="14" :y-gap="14" style="margin-top: 14px" responsive="screen" item-responsive>
        <n-gi span="2 l:1"><n-card title="Cargos: titulados vs empleadores (tabla de homologación)" size="small"><v-chart class="grafico" :option="homologados" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Cargos que ocupan los titulados" size="small"><v-chart class="grafico" :option="barras(d.cargos, { horizontal: true, porcentaje: true })" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Rubro" size="small"><v-chart class="grafico" :option="barras(d.rubro.filter((r) => r.frecuencia > 0), { horizontal: true, color: COLORES[5] })" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Nivel de formación demandado" size="small"><v-chart class="grafico" :option="dona(d.nivel_formacion_demandado)" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Tamaño de la organización" size="small"><v-chart class="grafico" :option="barras(d.tamano, { color: COLORES[1] })" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Medio de convocatoria" size="small"><v-chart class="grafico" :option="barras(d.medio_convocatoria, { horizontal: true, color: COLORES[2] })" autoresize /></n-card></n-gi>
      </n-grid>

      <n-card title="Preguntas abiertas (menciones agrupadas)" size="small" style="margin-top: 14px">
        <n-grid :cols="3" :x-gap="14" responsive="screen" item-responsive>
          <n-gi v-for="a in abiertas" :key="a.key" span="3 l:1">
            <div class="sub">{{ a.titulo }}</div>
            <div v-for="m in d.preguntasAbiertas[a.key].slice(0, 6)" :key="m.codigo" class="mencion">
              <span>{{ m.etiqueta }}</span><n-tag size="small" round :bordered="false">{{ m.frecuencia }}</n-tag>
            </div>
          </n-gi>
        </n-grid>
      </n-card>

      <n-card title="Organizaciones encuestadas" size="small" style="margin-top: 14px">
        <n-data-table size="small" :columns="[{ title: 'Organización', key: 'nombre' }, { title: 'Tipo', key: 'tipo' }, { title: 'Tamaño', key: 'tamano' }]" :data="d.empresas" :pagination="{ pageSize: 8 }" />
        <div class="nota">Se listan como exige el enunciado, pero ningún resultado se atribuye a una organización con nombre.</div>
      </n-card>
    </template>
  </div>
</template>

<style scoped>
.sub { font-size: 12.5px; color: #71717a; font-weight: 500; margin-bottom: 6px; }
.mencion { display: flex; justify-content: space-between; align-items: center; padding: 7px 0; border-bottom: 1px solid #f4f4f5; font-size: 13.5px; }
</style>
