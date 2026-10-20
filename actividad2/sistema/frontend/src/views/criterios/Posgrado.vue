<script setup>
import VChart from 'vue-echarts'
import CriterioPagina from '../../components/CriterioPagina.vue'
import Tarjeta from '../../components/Tarjeta.vue'
import { useApi } from '../../useApi'
import { barras, dona, likertApilado } from '../../graficos'

const { datos: d } = useApi('/api/formacion')
const { datos: fin } = useApi('/api/financiamiento')
</script>

<template>
  <CriterioPagina pantalla="posgrado">
    <template v-if="d">
      <div class="rejilla rejilla-2">
        <Tarjeta titulo="Área que les interesa estudiar"><v-chart class="grafico" :option="barras(d.areaInteres, { horizontal: true, porcentaje: true })" autoresize /></Tarjeta>
        <Tarjeta titulo="Dónde lo estudiarían"><v-chart class="grafico" :option="barras(d.organizacionInteres, { horizontal: true, porcentaje: true, color: '#334155' })" autoresize /></Tarjeta>
      </div>
      <Tarjeta titulo="¿La FCyT tiene posgrados afines a Ingeniería de Sistemas?" style="margin-top: 14px">
        <v-chart style="height: 110px" :option="likertApilado([{ nombre: 'La oferta de posgrado de la FCyT es afín', frecuencias: d.ofertaPosgradoFcyt.frecuencias }])" autoresize />
      </Tarjeta>
    </template>
    <template #detalle>
      <div v-if="d" class="rejilla rejilla-3">
        <Tarjeta titulo="Nivel del programa que cursó"><v-chart class="grafico" :option="barras(d.programaNivel)" autoresize /></Tarjeta>
        <Tarjeta titulo="Dónde lo cursó"><v-chart class="grafico" :option="barras(d.programaDonde, { horizontal: true })" autoresize /></Tarjeta>
        <Tarjeta v-if="fin" titulo="Cómo lo financió" :nota="fin.nota"><v-chart class="grafico" :option="dona(fin.programaFinanciamiento)" autoresize /></Tarjeta>
      </div>
    </template>
  </CriterioPagina>
</template>
