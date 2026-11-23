<script setup>
// Asa para arrastrar (6 puntos, como en Google Forms). La lista la configura ARRASTRE (orden.js) con
// handle: '.asa-arrastre'. Enfocada con Tab, el padre puede escuchar ↑/↓ para mover sin mouse.
defineProps({ vertical: { type: Boolean, default: false } })
</script>

<template>
  <div
    class="asa-arrastre" :class="{ 'asa-lateral': vertical }" role="button" tabindex="0"
    title="Arrastrar para mover" aria-label="Mover: arrastrar, o usar las flechas arriba y abajo"
  >
    <svg :width="vertical ? 10 : 18" :height="vertical ? 16 : 10" :viewBox="vertical ? '0 0 10 16' : '0 0 18 10'" aria-hidden="true" fill="currentColor">
      <template v-if="vertical">
        <circle v-for="(y, k) in [3, 8, 13]" :key="'a' + k" cx="2.5" :cy="y" r="1.6" />
        <circle v-for="(y, k) in [3, 8, 13]" :key="'b' + k" cx="7.5" :cy="y" r="1.6" />
      </template>
      <template v-else>
        <circle v-for="(x, k) in [3, 9, 15]" :key="'a' + k" :cx="x" cy="2.5" r="1.6" />
        <circle v-for="(x, k) in [3, 9, 15]" :key="'b' + k" :cx="x" cy="7.5" r="1.6" />
      </template>
    </svg>
  </div>
</template>
