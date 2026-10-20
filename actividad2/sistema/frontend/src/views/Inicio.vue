<script setup>
import { computed } from 'vue'
import Estado from '../components/Estado.vue'
import { useApi } from '../useApi'

// Vista general: cómo responde el estudio a cada criterio ARCU-SUR
const emit = defineEmits(['navegar'])
const { datos: criterios, error, cargando } = useApi('/api/criterios')
const ordenados = computed(() => [...(criterios.value ?? [])].sort((a, b) => a.id.localeCompare(b.id, undefined, { numeric: true })))
const total = (c) => c.favorables.length + c.desfavorables.length
</script>

<template>
  <div class="pagina">
    <h1 class="pagina-titulo">Respuesta a los criterios ARCU-SUR</h1>
    <p class="pagina-sub">Cada tarjeta resume lo que el estudio responde a un criterio con las encuestas a titulados y empleadores. Un aspecto es favorable cuando más de la mitad de quienes respondieron lo confirma. Abre un criterio para ver su evidencia.</p>
    <Estado :cargando="cargando && !criterios" :error="error" />

    <div v-if="criterios" class="rejilla rejilla-2">
      <button v-for="c in ordenados" :key="c.id" class="criterio" @click="emit('navegar', c.pantalla)">
        <div class="criterio-cabecera">
          <span class="etiqueta-criterio" style="margin: 0">{{ c.id }}</span>
          <span v-if="total(c)" class="conteo" :class="{ completo: c.desfavorables.length === 0 }">
            {{ c.favorables.length }} de {{ total(c) }} favorables
          </span>
          <span v-else class="conteo">Insumo, sin valoración</span>
        </div>
        <div class="criterio-nombre">{{ c.nombre }}</div>
        <div class="barra" :class="{ vacia: !total(c) }">
          <div class="barra-llena" :style="{ width: total(c) ? (100 * c.favorables.length) / total(c) + '%' : 0 }" />
        </div>
        <div class="indicador">
          <span class="indicador-valor">{{ c.indicadores[0]?.valor }}</span>
          <span class="nota" style="margin: 0">{{ c.indicadores[0]?.titulo }}</span>
        </div>
      </button>
    </div>
  </div>
</template>

<style scoped>
.criterio { text-align: left; font: inherit; color: inherit; background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 18px 20px; cursor: pointer; transition: border-color .15s, box-shadow .15s; }
.criterio:hover { border-color: #c7d2fe; box-shadow: 0 1px 8px rgba(79, 70, 229, 0.08); }
.criterio-cabecera { display: flex; justify-content: space-between; align-items: center; }
.criterio-nombre { font-weight: 600; font-size: 16px; margin-top: 4px; }
.conteo { font-size: 12.5px; color: var(--suave); }
.conteo.completo { color: var(--bien); }
.barra { height: 6px; background: #f1f1f3; border-radius: 3px; margin: 12px 0 14px; overflow: hidden; }
.barra-llena { height: 100%; background: var(--acento); border-radius: 3px; }
.barra.vacia { background: repeating-linear-gradient(90deg, #f1f1f3 0 6px, transparent 6px 10px); }
.indicador { display: flex; align-items: baseline; gap: 8px; }
.indicador-valor { font-size: 20px; font-weight: 650; letter-spacing: -0.01em; }
</style>
