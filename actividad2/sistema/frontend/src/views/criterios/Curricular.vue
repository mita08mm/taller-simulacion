<script setup>
import VChart from 'vue-echarts'
import CriterioPagina from '../../components/CriterioPagina.vue'
import Tarjeta from '../../components/Tarjeta.vue'
import { useApi } from '../../useApi'
import { barras } from '../../graficos'

const { datos: d } = useApi('/api/competencias')
const { datos: e } = useApi('/api/empleadores')
const abiertas = [
  { key: 'areas_emergentes', titulo: 'Áreas emergentes' },
  { key: 'herramientas', titulo: 'Herramientas tecnológicas' },
  { key: 'habilidades', titulo: 'Habilidades' },
]
</script>

<template>
  <CriterioPagina pantalla="curricular">
    <div v-if="d" class="rejilla rejilla-2">
      <Tarjeta titulo="Asignaturas que dieron ventaja en el trabajo"><v-chart class="grafico-alto" :option="barras(d.asignaturasVentaja, { horizontal: true, porcentaje: true })" autoresize /></Tarjeta>
      <Tarjeta titulo="Asignaturas poco útiles"><v-chart class="grafico-alto" :option="barras(d.asignaturasPocoUtiles, { horizontal: true, porcentaje: true, color: '#334155' })" autoresize /></Tarjeta>
    </div>
    <Tarjeta v-if="e" titulo="Lo que piden los empleadores (menciones en preguntas abiertas)" style="margin-top: 14px">
      <div class="rejilla rejilla-3">
        <div v-for="a in abiertas" :key="a.key">
          <div class="nota" style="margin: 0 0 4px; font-weight: 500">{{ a.titulo }}</div>
          <div v-for="m in e.preguntasAbiertas[a.key].slice(0, 6)" :key="m.codigo" class="mencion">
            <span>{{ m.etiqueta }}</span><span class="cuenta">{{ m.frecuencia }}</span>
          </div>
        </div>
      </div>
    </Tarjeta>
  </CriterioPagina>
</template>

<style scoped>
.mencion { display: flex; justify-content: space-between; padding: 7px 0; border-bottom: 1px solid #f1f1f3; font-size: 13.5px; }
.cuenta { color: var(--suave); font-variant-numeric: tabular-nums; }
</style>
