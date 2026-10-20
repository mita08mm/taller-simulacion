<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import CriterioPagina from '../../components/CriterioPagina.vue'
import Tarjeta from '../../components/Tarjeta.vue'
import { useApi } from '../../useApi'
import { barras, likertApilado } from '../../graficos'

const { datos: d } = useApi('/api/competencias')
const valoracion = computed(() => d.value && [
  { nombre: 'Está satisfecho con la formación recibida', frecuencias: d.value.satisfaccionGlobal.frecuencias },
])
</script>

<template>
  <CriterioPagina pantalla="resultados">
    <template v-if="d">
      <Tarjeta titulo="Satisfacción con la formación" nota="La pregunta usa una escala de acuerdo, por eso se lee como grado de acuerdo con estar satisfecho.">
        <v-chart style="height: 110px" :option="likertApilado(valoracion)" autoresize />
      </Tarjeta>
      <div class="rejilla rejilla-2" style="margin-top: 14px">
        <Tarjeta titulo="Lo que más les sirvió de la carrera"><v-chart class="grafico" :option="barras(d.aspectosUtilidad, { horizontal: true, porcentaje: true })" autoresize /></Tarjeta>
        <Tarjeta titulo="Lo que piden mejorar"><v-chart class="grafico" :option="barras(d.aspectosMejorar, { horizontal: true, porcentaje: true, color: '#334155' })" autoresize /></Tarjeta>
      </div>
      <div class="nota">Elección múltiple: el porcentaje es sobre el número de titulados, por eso puede sumar más de 100%.</div>
    </template>
  </CriterioPagina>
</template>
