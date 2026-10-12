<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import Estado from '../components/Estado.vue'
import { useApi } from '../useApi'
import { num } from '../api'
import { barras, likertApilado, radar, COLORES } from '../graficos'

const { datos: d, error, cargando } = useApi('/api/competencias')

const likert = computed(() => d.value && [
  { nombre: 'De acuerdo con estar satisfecho con la formación', frecuencias: d.value.satisfaccionGlobal.frecuencias },
  { nombre: 'Concordancia formación – requerimientos del mercado', frecuencias: d.value.concordancia.frecuencias },
])
const radarPerfil = computed(() => {
  if (!d.value) return null
  const p = d.value.perfilEmpleadores
  const parte = (item, codigo) => (item.frecuencias.find((f) => f.codigo === codigo)?.relativa ?? 0) * 100
  return radar(p.map((i) => i.criterio), [
    { nombre: 'De acuerdo (total + parcial)', valores: p.map((i) => +(i.porcentajeAcuerdo * 100).toFixed(1)) },
    { nombre: 'Totalmente de acuerdo', valores: p.map((i) => +parte(i, 'Totalmente_de_acuerdo').toFixed(1)) },
  ])
})
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">Titulados y empleadores · ARCU-SUR 3.2.1, 2.1.2 y 2.1.7</div>
    <h1 class="pagina-titulo">Valoración de la formación</h1>
    <p class="pagina-sub">Cómo valoran los titulados su formación y cómo ven los empleadores el perfil profesional.</p>
    <Estado :cargando="cargando && !d" :error="error" />

    <template v-if="d">
      <n-card title="Valoración de la formación (titulados)" size="small">
        <v-chart style="height: 200px" :option="likertApilado(likert)" autoresize />
        <div class="nota">
          Medias (escala 1–4): satisfacción {{ num(d.satisfaccionGlobal.media?.media) }}, IC 95% {{ num(d.satisfaccionGlobal.media?.limiteInferior) }} – {{ num(d.satisfaccionGlobal.media?.limiteSuperior) }} ·
          concordancia {{ num(d.concordancia.media?.media) }}, IC 95% {{ num(d.concordancia.media?.limiteInferior) }} – {{ num(d.concordancia.media?.limiteSuperior) }}.
          La pregunta de satisfacción usa una escala de acuerdo, por eso se lee como «grado de acuerdo con estar satisfecho».
        </div>
      </n-card>

      <n-grid :cols="2" :x-gap="14" :y-gap="14" style="margin-top: 14px" responsive="screen" item-responsive>
        <n-gi span="2 l:1"><n-card title="Aspectos de mayor utilidad" size="small"><v-chart class="grafico" :option="barras(d.aspectosUtilidad, { horizontal: true, porcentaje: true, color: COLORES[1] })" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Aspectos a mejorar" size="small"><v-chart class="grafico" :option="barras(d.aspectosMejorar, { horizontal: true, porcentaje: true, color: COLORES[3] })" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Asignaturas con ventaja competitiva" size="small"><v-chart class="grafico-alto" :option="barras(d.asignaturasVentaja, { horizontal: true, porcentaje: true, color: COLORES[0] })" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Asignaturas poco útiles" size="small"><v-chart class="grafico-alto" :option="barras(d.asignaturasPocoUtiles, { horizontal: true, porcentaje: true, color: COLORES[8] })" autoresize /></n-card></n-gi>
      </n-grid>
      <div class="nota">Elección múltiple: el porcentaje es sobre el número de titulados, por eso puede sumar más de 100%. El ranking se limita a las asignaturas ofrecidas en el formulario.</div>

      <n-card title="Perfil profesional según los empleadores (diagrama de radar)" size="small" style="margin-top: 14px">
        <v-chart style="height: 460px" :option="radarPerfil" autoresize />
        <div class="nota">Porcentaje de empleadores de acuerdo con cada criterio. No se calcula media porque la escala del formulario no es simétrica (dos niveles de acuerdo y uno de desacuerdo, más «No sabe»).</div>
      </n-card>
    </template>
  </div>
</template>
