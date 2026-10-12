<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import Kpi from '../components/Kpi.vue'
import Estado from '../components/Estado.vue'
import { useApi } from '../useApi'
import { pct, num } from '../api'
import { dona, barras, COLORES } from '../graficos'

const emp = useApi('/api/empleabilidad')
const form = useApi('/api/formacion')
const dem = useApi('/api/demografia')
const comp = useApi('/api/competencias')

const listo = computed(() => emp.datos.value && form.datos.value && dem.datos.value && comp.datos.value)
const error = computed(() => emp.error.value || form.error.value || dem.error.value || comp.error.value)
const acuerdo = (likert) => likert.frecuencias.filter((f) => f.codigo >= '3').reduce((s, f) => s + f.relativa, 0)

const criterios = [
  { id: '3.2.3', nombre: 'Condiciones de empleo', vista: 'empleabilidad', texto: 'Tasa de empleo, tiempo medio al primer empleo, concordancia titulación–empleo.' },
  { id: '3.2.1', nombre: 'Resultados del proceso formativo', vista: 'competencias', texto: 'Satisfacción con la formación y aspectos a mejorar.' },
  { id: '2.1.2', nombre: 'Perfil de egreso', vista: 'empleadores', texto: 'Coherencia entre el perfil y lo que demandan los empleadores.' },
  { id: '2.1.7', nombre: 'Actualización curricular', vista: 'competencias', texto: 'Asignaturas con ventaja competitiva y poco útiles.' },
  { id: '2.4.1', nombre: 'Actualización profesional y posgrado', vista: 'formacion', texto: 'Formación complementaria, interés y áreas de posgrado.' },
  { id: '3.2.2', nombre: 'Seguimiento a graduados', vista: 'resumen', texto: 'Interés en una red oficial de contactos de la Carrera.' },
]
const emit = defineEmits(['navegar'])
</script>

<template>
  <div class="pagina">
    <h1 class="pagina-titulo">Inicio</h1>
    <p class="pagina-sub">Indicadores principales de la encuesta a titulados, organizados según los criterios de calidad ARCU-SUR.</p>
    <Estado :cargando="!listo && !error" :error="error" />

    <template v-if="listo">
      <n-grid :cols="6" :x-gap="14" :y-gap="14" responsive="screen" item-responsive>
        <n-gi span="6 m:3 l:2"><Kpi titulo="Tasa de empleo" criterio="3.2.3" :valor="pct(emp.datos.value.tasaEmpleo.proporcion)" :detalle="`${emp.datos.value.tasaEmpleo.casos} de ${emp.datos.value.tasaEmpleo.total} titulados trabajan o emprenden`" /></n-gi>
        <n-gi span="6 m:3 l:2"><Kpi titulo="Tiempo medio al primer empleo" criterio="3.2.3" color="#14b8a6" :valor="`${num(emp.datos.value.tiempoPrimerEmpleo.mesesIC95.media, 1)} meses`" :detalle="`IC 95%: ${num(emp.datos.value.tiempoPrimerEmpleo.mesesIC95.limiteInferior, 1)} – ${num(emp.datos.value.tiempoPrimerEmpleo.mesesIC95.limiteSuperior, 1)} meses (n = ${emp.datos.value.tiempoPrimerEmpleo.mesesIC95.n})`" /></n-gi>
        <n-gi span="6 m:3 l:2"><Kpi titulo="De acuerdo con estar satisfecho" criterio="3.2.1" color="#8b5cf6" :valor="pct(acuerdo(comp.datos.value.satisfaccionGlobal))" :detalle="`Media ${num(comp.datos.value.satisfaccionGlobal.media.media)} en escala 1–4`" /></n-gi>
        <n-gi span="6 m:3 l:2"><Kpi titulo="Con formación complementaria" criterio="2.4.1" color="#f59e0b" :valor="pct(form.datos.value.conFormacionComplementaria.proporcion)" detalle="Posgrado, diplomado o especialidad" /></n-gi>
        <n-gi span="6 m:3 l:2"><Kpi titulo="Interés en posgrado" criterio="2.4.1" color="#0ea5e9" :valor="pct(form.datos.value.interesPosgrado.proporcion)" detalle="En áreas de la Ingeniería de Sistemas" /></n-gi>
        <n-gi span="6 m:3 l:2"><Kpi titulo="Interés en red de contactos" criterio="3.2.2" color="#64748b" :valor="pct(dem.datos.value.redContactos.proporcion)" detalle="Base para el seguimiento de graduados" /></n-gi>
      </n-grid>

      <n-grid :cols="3" :x-gap="14" :y-gap="14" style="margin-top: 14px" responsive="screen" item-responsive>
        <n-gi span="3 l:1"><n-card title="Situación laboral" size="small"><v-chart class="grafico" :option="dona(emp.datos.value.situacionLaboral)" autoresize /></n-card></n-gi>
        <n-gi span="3 l:1"><n-card title="Edad" size="small"><v-chart class="grafico" :option="barras(dem.datos.value.edad, { color: COLORES[1] })" autoresize /></n-card></n-gi>
        <n-gi span="3 l:1"><n-card title="Género" size="small"><v-chart class="grafico" :option="dona(dem.datos.value.genero)" autoresize /></n-card></n-gi>
      </n-grid>

      <n-card title="Qué responde este sistema a ARCU-SUR" size="small" style="margin-top: 14px">
        <n-grid :cols="3" :x-gap="12" :y-gap="12" responsive="screen" item-responsive>
          <n-gi v-for="c in criterios" :key="c.id" span="3 m:1">
            <div class="criterio" @click="emit('navegar', c.vista)">
              <div class="etiqueta-criterio">Criterio {{ c.id }}</div>
              <div class="criterio-nombre">{{ c.nombre }}</div>
              <div class="nota" style="margin-top: 2px">{{ c.texto }}</div>
            </div>
          </n-gi>
        </n-grid>
      </n-card>
    </template>
  </div>
</template>

<style scoped>
.criterio { border: 1px solid #e4e4e7; border-radius: 10px; padding: 12px 14px; cursor: pointer; transition: border-color .15s; height: 100%; }
.criterio:hover { border-color: #4f46e5; }
.criterio-nombre { font-weight: 600; font-size: 14px; margin-top: 2px; }
</style>
