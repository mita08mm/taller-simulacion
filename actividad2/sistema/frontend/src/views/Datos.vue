<script setup>
import { inject, ref } from 'vue'
import { importarCsv, urlExportar, enviar, API } from '../api'
import { useApi } from '../useApi'
import { useAccion, textoError } from '../useAccion'

const filtro = inject('filtro')
const { accion, mensaje } = useAccion()
const subiendo = ref('')

async function subir(tipo, { file }) {
  subiendo.value = tipo
  await accion(() => importarCsv(tipo, file.file), { exito: (r) => `Se guardaron ${r.importadas} respuestas de ${tipo} en la base de datos` })
  subiendo.value = ''
  return false
}
const { datos: cruces } = useApi('/api/cruces')

// Informe: el sistema regenera todos sus fragmentos con las respuestas actuales y lo compila con latexmk
const actualizando = ref(false)
const resultadoInforme = ref(null)
async function actualizarInforme() {
  actualizando.value = true
  resultadoInforme.value = null
  try {
    resultadoInforme.value = await enviar('POST', '/api/admin/informe')
    if (resultadoInforme.value.ok) mensaje.success(resultadoInforme.value.mensaje)
    else mensaje.error('La compilación del informe falló')
  } catch (e) {
    mensaje.error(textoError(e))
  } finally {
    actualizando.value = false
  }
}
</script>

<template>
  <div class="pagina">
    <h1 class="pagina-titulo">Importar y exportar</h1>
    <n-alert v-if="filtro.desde || filtro.hasta" type="info" :bordered="false" style="margin-bottom: 14px">
      Las exportaciones respetan el período elegido: {{ filtro.desde ?? 'inicio' }} a {{ filtro.hasta ?? 'hoy' }}.
    </n-alert>
    <p class="pagina-sub">Carga el CSV exportado de Microsoft Forms (titulados) o Google Forms (empleadores). El sistema traduce los textos de las preguntas y opciones a los códigos de la encuesta y guarda las respuestas con origen «importado»; volver a subir el archivo reemplaza la importación anterior (no duplica). Los datos personales (email, teléfono, LinkedIn, correo) se guardan aparte y no entran al análisis. La fecha de cada respuesta se toma de la columna «Hora de finalización» (Microsoft Forms) o «Marca temporal» (Google Forms).</p>

    <n-grid :cols="2" :x-gap="14" :y-gap="14" responsive="screen" item-responsive>
      <n-gi v-for="tipo in ['titulados', 'empleadores']" :key="tipo" span="2 l:1">
        <n-card :title="`Encuesta a ${tipo}`" size="small">
          <n-upload accept=".csv" :max="1" :show-file-list="false" :custom-request="(o) => subir(tipo, o)">
            <n-upload-dragger>
              <div style="font-weight: 600">{{ subiendo === tipo ? 'Importando…' : 'Arrastra el CSV aquí' }}</div>
              <div class="nota">o haz clic para elegir el archivo</div>
            </n-upload-dragger>
          </n-upload>
          <n-space style="margin-top: 12px" :wrap="false">
            <n-button tag="a" :href="urlExportar(tipo, filtro)" secondary>Exportar CSV</n-button>
            <n-button tag="a" :href="urlExportar(tipo, filtro, { formato: 'excel' })" secondary>Exportar para Excel</n-button>
          </n-space>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="Informe" size="small" style="margin-top: 14px">
      <p class="nota" style="margin: 0 0 10px">
        Recalcula todo el informe (tablas, desarrollos paso a paso, cruces, resumen, conclusiones y propuestas) con las respuestas que hay ahora en la base, y lo compila en PDF.
        Úsalo después de importar respuestas nuevas o de limpiar datos.
      </p>
      <n-space>
        <n-button type="primary" :loading="actualizando" @click="actualizarInforme">Actualizar informe</n-button>
        <n-button tag="a" :href="API + '/api/admin/informe/pdf'" secondary>Descargar PDF</n-button>
      </n-space>
      <n-alert v-if="resultadoInforme && !resultadoInforme.ok" type="error" :bordered="false" style="margin-top: 10px">{{ resultadoInforme.mensaje }}</n-alert>
    </n-card>

    <n-card title="Relaciones entre preguntas (tablas cruzadas)" size="small" style="margin-top: 14px">
      <div v-for="c in cruces ?? []" :key="c.id" class="fila-cruce">
        <span>{{ c.titulo.replace(' (opcional)', '') }}</span>
        <n-space size="small" :wrap="false">
          <n-button tag="a" :href="urlExportar(c.id, filtro)" size="tiny" secondary>CSV</n-button>
          <n-button tag="a" :href="urlExportar(c.id, filtro, { formato: 'excel' })" size="tiny" secondary>Excel</n-button>
        </n-space>
      </div>
      <div class="nota">Cada archivo trae la frecuencia observada, la esperada, X₀², los grados de libertad y el valor teórico. El formato «Excel» usa punto y coma y coma decimal, para que Excel en español lo abra en columnas.</div>
    </n-card>

  </div>
</template>

<style scoped>
.fila-cruce { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--borde-suave); font-size: 14px; }
</style>
