<script setup>
// Contenedor de un gráfico: título, el gráfico y, si hace falta, una nota de cómo leerlo
defineProps({ titulo: String, nota: String })
</script>

<template>
  <div class="tarjeta">
    <div class="tarjeta-cabecera">
      <div class="tarjeta-titulo">{{ titulo }}</div>
      <slot name="extra" />
    </div>
    <slot />
    <div v-if="nota" class="nota">{{ nota }}</div>
  </div>
</template>

<style scoped>
.tarjeta { background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 16px 18px; min-width: 0; }
.tarjeta-cabecera { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 8px; }
.tarjeta-titulo { font-weight: 600; font-size: 14.5px; }
</style>
