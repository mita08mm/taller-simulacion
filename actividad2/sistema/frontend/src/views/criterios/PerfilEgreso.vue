<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import CriterioPagina from '../../components/CriterioPagina.vue'
import Tarjeta from '../../components/Tarjeta.vue'
import { useApi } from '../../useApi'
import { barras, dona, likertApilado, barrasAgrupadas, radarPerfil } from '../../graficos'

const { datos: d } = useApi('/api/empleadores')
const ITEMS = ['perfil_conoce_perfil', 'perfil_coherente_desempeno', 'perfil_titulo_consistente', 'perfil_competencias_laborales', 'perfil_valores', 'perfil_desempeno_destacado', 'perfil_confianza']
const items = computed(() => d.value && d.value.perfil.filter((p) => ITEMS.includes(p.codigo)))
const perfil = computed(() => items.value && items.value.map((p) => ({ nombre: p.criterio, frecuencias: p.frecuencias })))
const cargos = computed(() => {
  if (!d.value) return null
  const h = d.value.cargosHomologados
  const aPct = (v) => { const t = v.reduce((s, x) => s + x, 0) || 1; return v.map((x) => +((x / t) * 100).toFixed(1)) }
  return barrasAgrupadas(h.grupos, [
    { nombre: 'Titulados: cargo actual (%)', valores: aPct(h.titulados) },
    { nombre: 'Empleadores: cargos que ocupan (%)', valores: aPct(h.empleadores) },
  ])
})
</script>

<template>
  <CriterioPagina pantalla="perfil">
    <template v-if="d">
      <Tarjeta titulo="Opinión de los empleadores sobre el perfil profesional" nota="Porcentaje de organizaciones de acuerdo con cada afirmación. Mientras más se acerca al borde, mayor acuerdo.">
        <v-chart style="height: 420px" :option="radarPerfil(items)" autoresize />
      </Tarjeta>
      <div class="rejilla rejilla-2" style="margin-top: 14px">
        <Tarjeta titulo="Cargos: lo que dicen titulados y empleadores"><v-chart class="grafico" :option="cargos" autoresize /></Tarjeta>
        <Tarjeta titulo="Nivel de formación que piden"><v-chart class="grafico" :option="dona(d.nivel_formacion_demandado)" autoresize /></Tarjeta>
      </div>
    </template>
    <template #detalle>
      <Tarjeta v-if="d" titulo="Detalle por opción de respuesta" nota="Porcentaje de organizaciones por opción." style="margin-bottom: 14px">
        <v-chart style="height: 340px" :option="likertApilado(perfil)" autoresize />
      </Tarjeta>
      <div v-if="d" class="rejilla rejilla-2">
        <Tarjeta titulo="Rubro de las organizaciones"><v-chart class="grafico-alto" :option="barras(d.rubro.filter((r) => r.frecuencia > 0), { horizontal: true })" autoresize /></Tarjeta>
        <Tarjeta titulo="Tamaño de las organizaciones"><v-chart class="grafico-alto" :option="barras(d.tamano)" autoresize /></Tarjeta>
      </div>
    </template>
  </CriterioPagina>
</template>
