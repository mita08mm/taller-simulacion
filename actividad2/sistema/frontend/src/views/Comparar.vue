<script setup>
import { ref, reactive, computed, inject, watch } from 'vue'
import CruceDetalle from '../components/CruceDetalle.vue'
import { useApi, parametrosFiltro } from '../useApi'
import { obtener, urlExportar } from '../api'

// Comparación libre: se eligen dos preguntas y el sistema aplica la prueba chi-cuadrada al momento
const filtro = inject('filtro')
const version = inject('version')
const { datos: variables } = useApi('/api/variables')
const opciones = computed(() => (variables.value ?? []).map((v) => ({ label: v.pregunta, value: v.columna })))
const libre = reactive({ filas: 'genero', columnas: 'interes_posgrado', agrupar: true })
const resultado = ref(null)
const error = ref(null)
const cargando = ref(false)
const params = () => ({ filas: libre.filas, columnas: libre.columnas, agrupar: libre.agrupar ? 1 : 0 })
async function calcular() {
  if (!libre.filas || !libre.columnas) return
  cargando.value = true
  error.value = null
  try {
    resultado.value = await obtener('/api/cruce-libre', { ...parametrosFiltro(filtro), ...params() })
  } catch (e) {
    error.value = e.message
    resultado.value = null
  } finally {
    cargando.value = false
  }
}
watch([() => libre.filas, () => libre.columnas, () => libre.agrupar, () => filtro.desde, () => filtro.hasta, version], calcular, { immediate: true })
function intercambiar() {
  ;[libre.filas, libre.columnas] = [libre.columnas, libre.filas]
}
</script>

<template>
  <div class="pagina">
    <h1 class="pagina-titulo">Comparar dos preguntas</h1>
    <p class="pagina-sub">Elige dos preguntas de la encuesta a titulados para ver cómo se reparten las respuestas entre sí y si están relacionadas.</p>

    <div class="selector">
      <n-form-item label="Pregunta en filas" :show-feedback="false" style="flex: 1">
        <n-select v-model:value="libre.filas" :options="opciones" filterable />
      </n-form-item>
      <n-button quaternary @click="intercambiar" title="Intercambiar" style="margin-top: 26px">⇄</n-button>
      <n-form-item label="Pregunta en columnas" :show-feedback="false" style="flex: 1">
        <n-select v-model:value="libre.columnas" :options="opciones" filterable />
      </n-form-item>
    </div>
    <n-checkbox v-model:checked="libre.agrupar" style="margin-top: 10px">
      Agrupar categorías parecidas (acuerdo/desacuerdo, rangos de remuneración, tiempo y edad), para que la comparación sea más confiable
    </n-checkbox>

    <n-alert v-if="error" type="error" :bordered="false" style="margin-top: 14px">{{ error }}</n-alert>
    <div v-else-if="cargando && !resultado" style="padding: 40px; display: grid; place-items: center"><n-spin /></div>
    <CruceDetalle v-if="resultado && !error" :c="resultado" style="margin-top: 18px"
      :url-csv="urlExportar('libre', filtro, params())" :url-excel="urlExportar('libre', filtro, { ...params(), formato: 'excel' })" />
  </div>
</template>

<style scoped>
.selector { display: flex; gap: 12px; align-items: flex-start; background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 16px 18px; }
@media (max-width: 900px) { .selector { flex-direction: column; align-items: stretch; } }
</style>
