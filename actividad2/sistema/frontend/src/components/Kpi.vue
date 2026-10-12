<script setup>
defineProps({ titulo: String, valor: String, detalle: String, criterio: String, color: { type: String, default: '#4f46e5' } })
</script>

<template>
  <n-card size="small" :bordered="true" class="kpi">
    <div class="kpi-fila">
      <span class="kpi-titulo">{{ titulo }}</span>
      <n-tag v-if="criterio" size="tiny" :bordered="false" round>{{ criterio }}</n-tag>
    </div>
    <div class="kpi-valor" :style="{ color }">{{ valor }}</div>
    <div v-if="detalle" class="kpi-detalle">{{ detalle }}</div>
  </n-card>
</template>

<style scoped>
.kpi { height: 100%; }
.kpi-fila { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.kpi-titulo { color: #71717a; font-size: 12.5px; font-weight: 500; }
.kpi-valor { font-size: 28px; font-weight: 650; letter-spacing: -0.02em; margin-top: 6px; }
.kpi-detalle { color: #71717a; font-size: 12px; margin-top: 2px; }
</style>
