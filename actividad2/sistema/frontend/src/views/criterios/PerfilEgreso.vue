<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import CriterioPagina from '../../components/CriterioPagina.vue'
import Tarjeta from '../../components/Tarjeta.vue'
import { useApi } from '../../useApi'
import { pct } from '../../api'
import { barras, likertApilado, barrasAgrupadas, radarPerfil } from '../../graficos'

const { datos: d } = useApi('/api/empleadores')
// Las mismas 7 afirmaciones que se valoran arriba (Criterios.java, ITEMS_PERFIL_EGRESO)
const ITEMS = ['perfil_conoce_perfil', 'perfil_coherente_desempeno', 'perfil_titulo_consistente', 'perfil_competencias_laborales', 'perfil_valores', 'perfil_desempeno_destacado', 'perfil_confianza']
const items = computed(() => d.value && d.value.perfil.filter((p) => ITEMS.includes(p.codigo)))
const perfil = computed(() => items.value && items.value.map((p) => ({ nombre: p.criterio, frecuencias: p.frecuencias })))
const deAcuerdo = computed(() => items.value?.filter((p) => p.porcentajeAcuerdo > 0.5).length ?? 0)

// Cargos homologados: % de cada grupo según titulados y según empleadores
const cargosDatos = computed(() => {
  if (!d.value) return null
  const h = d.value.cargosHomologados
  const aPct = (v) => { const t = v.reduce((s, x) => s + x, 0) || 1; return v.map((x) => +((x / t) * 100).toFixed(1)) }
  return { grupos: h.grupos, titulados: aPct(h.titulados), empleadores: aPct(h.empleadores) }
})
const cargos = computed(() => cargosDatos.value && barrasAgrupadas(cargosDatos.value.grupos, [
  { nombre: 'Titulados: su cargo actual', valores: cargosDatos.value.titulados },
  { nombre: 'Empleadores: cargos que ocupan los titulados', valores: cargosDatos.value.empleadores },
], { horizontal: true, porcentaje: true, eje: '% de las respuestas de cada grupo' }))
const tituloCargos = computed(() => {
  const c = cargosDatos.value
  if (!c) return ''
  const mayor = (v) => c.grupos[v.indexOf(Math.max(...v))]
  const t = mayor(c.titulados), e = mayor(c.empleadores)
  return t === e
    ? `Titulados y empleadores coinciden: el cargo más frecuente es «${t}»`
    : `Los titulados se ven sobre todo en «${t}»; los empleadores los ubican en «${e}»`
})
const nivelTop = computed(() => d.value && [...d.value.nivel_formacion_demandado].sort((a, b) => b.frecuencia - a.frecuencia)[0])
const rubroTop = computed(() => d.value && [...d.value.rubro].sort((a, b) => b.frecuencia - a.frecuencia)[0])
</script>

<template>
  <CriterioPagina pantalla="perfil">
    <template v-if="d">
      <Tarjeta :titulo="`Los empleadores están de acuerdo con ${deAcuerdo} de las ${items.length} afirmaciones sobre el perfil`"
        nota="Porcentaje de organizaciones que eligió cada opción, ordenado de mayor a menor «Totalmente de acuerdo».">
        <v-chart style="height: 380px" :option="likertApilado(perfil, { ordenarPor: 'Totalmente_de_acuerdo' })" autoresize />
      </Tarjeta>
      <div class="rejilla rejilla-2" style="margin-top: 14px">
        <Tarjeta :titulo="tituloCargos">
          <v-chart class="grafico" :option="cargos" autoresize />
          <div class="nota">El formulario de titulados no ofrece la opción «Analista / Especialista»: por eso ese grupo aparece en 0% para los titulados. No es un error de cálculo.</div>
        </Tarjeta>
        <Tarjeta :titulo="`El ${pct(nivelTop.relativa)} de los empleadores pide ${nivelTop.etiqueta.toLowerCase()}`">
          <v-chart class="grafico" :option="barras(d.nivel_formacion_demandado, { horizontal: true, porcentaje: true, ordenar: true, eje: '% de organizaciones' })" autoresize />
        </Tarjeta>
      </div>
    </template>
    <template #detalle>
      <div v-if="d" class="rejilla rejilla-2">
        <Tarjeta :titulo="`El rubro más frecuente es «${rubroTop.etiqueta}»`" nota="Los rubros con 1 o 2 organizaciones se juntan en «Otros».">
          <v-chart class="grafico-alto" :option="barras(d.rubro, { horizontal: true, porcentaje: true, ordenar: true, otrosHasta: 2, eje: '% de organizaciones' })" autoresize />
        </Tarjeta>
        <Tarjeta titulo="Tamaño de las organizaciones">
          <v-chart class="grafico-alto" :option="barras(d.tamano, { horizontal: true, porcentaje: true, eje: '% de organizaciones' })" autoresize />
        </Tarjeta>
      </div>
      <Tarjeta v-if="d" titulo="Las mismas afirmaciones en un diagrama de radar" style="margin-top: 14px"
        nota="Cada eje es una afirmación; mientras más cerca del borde, mayor acuerdo. Escala de 0% a 100%.">
        <v-chart style="height: 440px" :option="radarPerfil(items)" autoresize />
      </Tarjeta>
    </template>
  </CriterioPagina>
</template>
