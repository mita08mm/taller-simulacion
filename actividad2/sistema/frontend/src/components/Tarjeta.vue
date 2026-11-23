<script setup>
// Contenedor de un gráfico: título (ya pensado como conclusión) y el gráfico.
// Si hace falta aclarar cómo leerlo, va en un ícono (i) con tooltip, no como
// texto siempre visible.
defineProps({ titulo: String, nota: String })
</script>

<template>
  <div class="tarjeta">
    <div class="tarjeta-cabecera">
      <div class="tarjeta-titulo-grupo">
        <div class="tarjeta-titulo">{{ titulo }}</div>
        <n-tooltip v-if="nota" trigger="hover">
          <template #trigger><span class="info">i</span></template>
          {{ nota }}
        </n-tooltip>
      </div>
      <slot name="extra" />
    </div>
    <slot />
  </div>
</template>

<style scoped>
.tarjeta { background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 16px 18px; min-width: 0; }
.tarjeta-cabecera { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 8px; }
.tarjeta-titulo-grupo { display: flex; align-items: center; gap: 6px; }
.tarjeta-titulo { font-weight: 600; font-size: 14.5px; }
.info {
  display: inline-flex; align-items: center; justify-content: center;
  width: 15px; height: 15px; border-radius: 50%; border: 1px solid var(--suave);
  color: var(--suave); font-size: 10px; font-style: italic; font-family: Georgia, serif;
  cursor: help; flex-shrink: 0;
}
</style>
