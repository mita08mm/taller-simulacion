<script setup>
defineProps({ cargando: Boolean, error: String })
</script>

<template>
  <n-alert v-if="error" type="error" :bordered="false" style="margin-bottom: 16px">{{ error }}</n-alert>
  <div v-else-if="cargando" style="padding: 40px; display: grid; place-items: center"><n-spin size="medium" /></div>
</template>
