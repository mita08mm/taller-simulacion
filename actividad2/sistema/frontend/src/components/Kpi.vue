<script setup>
defineProps({ titulo: String, valor: String, detalle: String })
</script>

<template>
  <div class="kpi">
    <div class="kpi-titulo">{{ titulo }}</div>
    <div class="kpi-valor" :class="{ texto: valor && valor.length > 12 }">{{ valor }}</div>
    <div v-if="detalle" class="kpi-detalle">{{ detalle }}</div>
  </div>
</template>

<style scoped>
.kpi { background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 16px 18px; height: 100%; }
.kpi-titulo { color: var(--suave); font-size: 13px; font-weight: 500; }
.kpi-valor { font-size: 28px; font-weight: 650; letter-spacing: -0.02em; margin-top: 6px; line-height: 1.15; }
.kpi-valor.texto { font-size: 18px; letter-spacing: -0.01em; }
.kpi-detalle { color: var(--suave); font-size: 12.5px; margin-top: 4px; }
</style>
