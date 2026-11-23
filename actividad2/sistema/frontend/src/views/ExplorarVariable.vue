<script setup>
import { ref, reactive, computed, inject, watch } from 'vue'
import VChart from 'vue-echarts'
import Estado from '../components/Estado.vue'
import { useApi, parametrosFiltro } from '../useApi'
import { obtener } from '../api'
import { barras } from '../graficos'
import { textoError } from '../useAccion'

// Explorar una variable: se elige la encuesta y una pregunta, y se arma su
// gráfico y sus estadísticos al momento (sin armar un reporte imprimible).
const filtro = inject('filtro')
const version = inject('version')

const encuesta = ref('titulados')
const encuestas = [
  { label: 'Titulados', value: 'titulados' },
  { label: 'Empleadores', value: 'empleadores' },
]

const { datos: secciones, cargar: cargarSecciones } = useApi('/api/reporte/variables', () => ({ encuesta: encuesta.value }))
const opciones = computed(() => (secciones.value ?? []).flatMap((s) => s.preguntas.map((p) => ({ label: p.texto, value: p.codigo }))))
const variable = ref(null)
watch(secciones, () => { variable.value = opciones.value[0]?.value ?? null })
watch(encuesta, cargarSecciones)

const resultado = ref(null)
const error = ref(null)
const cargando = ref(false)
async function calcular() {
  if (!variable.value) { resultado.value = null; return }
  cargando.value = true
  error.value = null
  try {
    const r = await obtener('/api/reporte', { ...parametrosFiltro(filtro), encuesta: encuesta.value, variables: variable.value })
    resultado.value = r.variables[0] ?? null
  } catch (e) {
    error.value = textoError(e)
    resultado.value = null
  } finally {
    cargando.value = false
  }
}
watch([variable, () => filtro.desde, () => filtro.hasta, version], calcular)

const grafico = computed(() => resultado.value && barras(resultado.value.frecuencias, { horizontal: resultado.value.frecuencias.length > 6, porcentaje: true }))
</script>

<template>
  <div class="pagina">
    <h1 class="pagina-titulo">Explorar una variable</h1>
    <p class="pagina-sub">Elegí una pregunta de la encuesta y se arma su gráfico y sus estadísticos al momento.</p>

    <div class="selector">
      <n-form-item label="Encuesta" :show-feedback="false">
        <n-select v-model:value="encuesta" :options="encuestas" style="width: 180px" />
      </n-form-item>
      <n-form-item label="Variable" :show-feedback="false" style="flex: 1">
        <n-select v-model:value="variable" :options="opciones" filterable />
      </n-form-item>
    </div>

    <Estado :cargando="cargando && !resultado" :error="error" />

    <template v-if="resultado && !error">
      <div class="resultado">
        <div class="resultado-cabecera">
          <div class="resultado-titulo">{{ resultado.texto }}</div>
          <div class="nota">$n = {{ resultado.n }}$ respuestas</div>
        </div>
        <p v-if="resultado.nota" class="nota">{{ resultado.nota }}</p>

        <div v-if="resultado.estadistica" class="estadistica">
          <div><span class="nota">Media</span><b>{{ resultado.estadistica.media.toFixed(2) }}</b></div>
          <div><span class="nota">Desviación estándar</span><b>{{ resultado.estadistica.desviacion.toFixed(2) }}</b></div>
          <div><span class="nota">Intervalo de confianza 95%</span><b>[{{ resultado.estadistica.limiteInferior.toFixed(2) }}, {{ resultado.estadistica.limiteSuperior.toFixed(2) }}]</b></div>
        </div>

        <v-chart class="grafico-alto" :option="grafico" autoresize />
      </div>
    </template>
  </div>
</template>

<style scoped>
.selector { display: flex; gap: 12px; align-items: flex-start; background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 16px 18px; margin-bottom: 18px; }
@media (max-width: 900px) { .selector { flex-direction: column; align-items: stretch; } }
.resultado-cabecera { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; }
.resultado-titulo { font-weight: 600; font-size: 16px; }
.estadistica { display: flex; gap: 28px; flex-wrap: wrap; margin: 12px 0 18px; padding: 12px 16px; background: var(--superficie); border: 1px solid var(--borde); border-radius: 10px; }
.estadistica > div { display: flex; flex-direction: column; gap: 2px; }
.estadistica b { font-size: 16px; }
</style>
