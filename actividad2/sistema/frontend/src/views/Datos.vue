<script setup>
import { inject, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { importarCsv, restablecer, urlExportar } from '../api'

const alImportar = inject('alImportar')
const filtro = inject('filtro')
const mensaje = useMessage()
const subiendo = ref('')

async function subir(tipo, { file }) {
  subiendo.value = tipo
  try {
    const r = await importarCsv(tipo, file.file)
    mensaje.success(`Importado: ${r[tipo].n} respuestas de ${tipo}`)
    alImportar()
  } catch (e) {
    mensaje.error(e.message)
  } finally {
    subiendo.value = ''
  }
  return false
}
async function volverAPrueba() {
  await restablecer()
  mensaje.info('Se volvió al dataset de prueba')
  alImportar()
}
const cruces = ['C1', 'C2', 'C3', 'C4', 'C5', 'C6', 'C7']
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">Datos</div>
    <h1 class="pagina-titulo">Importar y exportar</h1>
    <n-alert v-if="filtro.desde || filtro.hasta" type="info" :bordered="false" style="margin-bottom: 14px">
      Las exportaciones respetan el período elegido: {{ filtro.desde ?? 'inicio' }} a {{ filtro.hasta ?? 'hoy' }}.
    </n-alert>
    <p class="pagina-sub">Carga el CSV exportado de Microsoft Forms (titulados) o Google Forms (empleadores). Los datos personales (email, teléfono, LinkedIn, correo) se eliminan al importar. La fecha de cada respuesta se toma de la columna «Hora de finalización» (Microsoft Forms) o «Marca temporal» (Google Forms).</p>

    <n-grid :cols="2" :x-gap="14" :y-gap="14" responsive="screen" item-responsive>
      <n-gi v-for="tipo in ['titulados', 'empleadores']" :key="tipo" span="2 l:1">
        <n-card :title="`Encuesta a ${tipo}`" size="small">
          <n-upload accept=".csv" :max="1" :show-file-list="false" :custom-request="(o) => subir(tipo, o)">
            <n-upload-dragger>
              <div style="font-weight: 600">{{ subiendo === tipo ? 'Importando…' : 'Arrastra el CSV aquí' }}</div>
              <div class="nota">o haz clic para elegir el archivo</div>
            </n-upload-dragger>
          </n-upload>
          <n-button tag="a" :href="urlExportar(tipo, filtro)" secondary block style="margin-top: 12px">Exportar {{ tipo }} (CSV limpio)</n-button>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="Matrices de frecuencia cruzada" size="small" style="margin-top: 14px">
      <n-space>
        <n-button v-for="c in cruces" :key="c" tag="a" :href="urlExportar(c, filtro)" size="small" secondary>{{ c }} · CSV</n-button>
      </n-space>
      <div class="nota">Cada archivo trae la frecuencia observada, la esperada, X₀², los grados de libertad y el valor teórico.</div>
    </n-card>

    <n-card size="small" style="margin-top: 14px">
      <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px">
        <div>
          <div style="font-weight: 600">Volver al dataset de prueba</div>
          <div class="nota" style="margin: 0">Descarta lo importado y vuelve a cargar los datos sintéticos.</div>
        </div>
        <n-button @click="volverAPrueba">Restablecer</n-button>
      </div>
    </n-card>
  </div>
</template>
