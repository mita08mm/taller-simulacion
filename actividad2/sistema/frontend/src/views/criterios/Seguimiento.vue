<script setup>
import VChart from 'vue-echarts'
import CriterioPagina from '../../components/CriterioPagina.vue'
import Tarjeta from '../../components/Tarjeta.vue'
import { useApi } from '../../useApi'
import { barras, dona, barrasAgrupadas } from '../../graficos'

const { datos: dem } = useApi('/api/demografia')
const porMes = (lista) => barrasAgrupadas(lista.map((m) => m.mes), [
  { nombre: 'Titulados', valores: lista.map((m) => m.titulados) },
  { nombre: 'Empleadores', valores: lista.map((m) => m.empleadores) },
])
const red = (r) => [
  { etiqueta: 'Sí', frecuencia: r.casos },
  { etiqueta: 'No', frecuencia: r.total - r.casos },
]
</script>

<template>
  <CriterioPagina pantalla="seguimiento">
    <template #default="{ criterio }">
      <div class="rejilla rejilla-2">
        <Tarjeta titulo="Respuestas recibidas por mes" nota="El sistema recibe respuestas de forma continua (formulario propio e importaciones), y cada nueva respuesta actualiza los indicadores.">
          <v-chart class="grafico" :option="porMes(criterio.respuestasPorMes)" autoresize />
        </Tarjeta>
        <Tarjeta v-if="dem" titulo="¿Le interesa formar parte de una red de contactos de la Carrera?">
          <v-chart class="grafico" :option="dona(red(dem.redContactos))" autoresize />
        </Tarjeta>
      </div>
    </template>
    <template #detalle>
      <div v-if="dem" class="rejilla rejilla-2">
        <Tarjeta titulo="Edad de los titulados que respondieron"><v-chart class="grafico" :option="barras(dem.edad)" autoresize /></Tarjeta>
        <Tarjeta titulo="Género"><v-chart class="grafico" :option="dona(dem.genero)" autoresize /></Tarjeta>
      </div>
    </template>
  </CriterioPagina>
</template>
