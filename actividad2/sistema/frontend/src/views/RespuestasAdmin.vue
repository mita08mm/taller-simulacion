<script setup>
import { ref, reactive, computed, watch, inject, h } from 'vue'
import VChart from 'vue-echarts'
import { useMessage, NTag, NButton, NSpace, NPopconfirm } from 'naive-ui'
import Kpi from '../components/Kpi.vue'
import { obtener, enviar } from '../api'
import { barras, COLORES } from '../graficos'

// CRUD de respuestas guardadas en la base: listado, detalle, eliminación y enlaces del formulario
const mensaje = useMessage()
const filtro = inject('filtro')
const version = inject('version')
const alImportar = inject('alImportar')

const encuesta = ref('titulados')
const origen = ref(null)
const pagina = ref(1)
const tamano = 15
const listado = ref({ total: 0, filas: [] })
const resumen = ref([])
const porDia = ref([])
const estructura = ref(null)
const cargando = ref(false)

async function cargar() {
  cargando.value = true
  try {
    const [l, r, d, e] = await Promise.all([
      obtener('/api/admin/respuestas', { encuesta: encuesta.value, origen: origen.value ?? '', pagina: pagina.value, tamano,
        desde: filtro.desde ?? '', hasta: filtro.hasta ?? '' }),
      obtener('/api/admin/encuestas'),
      obtener('/api/admin/respuestas-por-dia', { encuesta: encuesta.value, dias: 30 }),
      obtener(`/api/admin/encuestas/${encuesta.value}`),
    ])
    listado.value = l
    resumen.value = r
    porDia.value = d
    estructura.value = e
  } catch (e) {
    mensaje.error(e.message)
  } finally {
    cargando.value = false
  }
}
watch([encuesta, origen, () => filtro.desde, () => filtro.hasta, version], () => { pagina.value = 1; cargar() }, { immediate: true })
watch(pagina, cargar)

const actual = computed(() => resumen.value.find((e) => e.codigo === encuesta.value))
const enlace = (codigo) => `${location.origin}${location.pathname}#/encuesta/${codigo}`
async function copiar(codigo) {
  try {
    await navigator.clipboard.writeText(enlace(codigo))
    mensaje.success('Enlace copiado')
  } catch {
    mensaje.info(enlace(codigo))
  }
}

// Texto de la pregunta y etiqueta de la opción, para mostrar la respuesta legible
const preguntas = computed(() => {
  const m = {}
  for (const s of estructura.value?.secciones ?? []) for (const p of s.preguntas) m[p.codigo] = p
  return m
})
function legible(codigo, valor) {
  const p = preguntas.value[codigo]
  const etiqueta = (v) => p?.opciones?.find((o) => o.codigo === String(v))?.etiqueta ?? v
  return Array.isArray(valor) ? valor.map(etiqueta).join(' · ') : etiqueta(valor)
}

const detalle = ref(null)
async function verDetalle(id) {
  try {
    detalle.value = await obtener(`/api/admin/respuestas/${id}`)
  } catch (e) {
    mensaje.error(e.message)
  }
}
const filasDetalle = computed(() => {
  if (!detalle.value) return []
  const orden = Object.keys(preguntas.value)
  return Object.entries(detalle.value.datos)
    .sort(([a], [b]) => orden.indexOf(a) - orden.indexOf(b))
    .map(([codigo, valor]) => ({ codigo, pregunta: preguntas.value[codigo]?.texto ?? codigo, valor: legible(codigo, valor) }))
})

async function eliminar(id) {
  try {
    await enviar('DELETE', `/api/admin/respuestas/${id}`)
    mensaje.success('Respuesta eliminada')
    detalle.value = null
    alImportar()
  } catch (e) {
    mensaje.error(e.message)
  }
}
async function eliminarPrueba() {
  try {
    const r = await enviar('DELETE', '/api/admin/respuestas', undefined, { origen: 'prueba' })
    mensaje.success(`Se eliminaron ${r.eliminadas} respuestas de prueba`)
    alImportar()
  } catch (e) {
    mensaje.error(e.message)
  }
}

const tipoOrigen = { formulario: 'success', importado: 'info', prueba: 'default' }
const columnas = [
  { title: 'N.º', key: 'id', width: 70 },
  { title: 'Fecha', key: 'fecha', width: 170 },
  { title: 'Origen', key: 'origen', width: 110, render: (f) => h(NTag, { size: 'small', bordered: false, type: tipoOrigen[f.origen] }, () => f.origen) },
  { title: 'Versión', key: 'version', width: 80 },
  {
    title: 'Resumen', key: 'resumen', ellipsis: { tooltip: true },
    render: (f) => ['situacion_laboral', 'tipo_organizacion', 'edad', 'nombre_empresa']
      .filter((k) => f.datos[k] != null).map((k) => legible(k, f.datos[k])).join(' · '),
  },
  {
    title: '', key: 'acciones', width: 150,
    render: (f) => h(NSpace, { size: 4, wrap: false }, () => [
      h(NButton, { size: 'tiny', secondary: true, onClick: () => verDetalle(f.id) }, () => 'Ver'),
      h(NPopconfirm, { onPositiveClick: () => eliminar(f.id) }, {
        trigger: () => h(NButton, { size: 'tiny', quaternary: true, type: 'error' }, () => 'Eliminar'),
        default: () => '¿Eliminar esta respuesta?',
      }),
    ]),
  },
]
const graficoDias = computed(() => barras(porDia.value.map((d) => ({ etiqueta: d.dia.slice(5), frecuencia: d.n })), { color: COLORES[1] }))
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">Encuestas · CRUD de respuestas</div>
    <h1 class="pagina-titulo">Respuestas</h1>
    <p class="pagina-sub">Respuestas guardadas en la base de datos, del formulario propio, importadas de Forms o del dataset de prueba.</p>

    <n-card size="small" title="Enlaces del formulario para compartir">
      <n-grid :cols="2" :x-gap="14" :y-gap="10" responsive="screen" item-responsive>
        <n-gi v-for="c in ['titulados', 'empleadores']" :key="c" span="2 m:1">
          <div class="enlace">
            <div>
              <div class="enlace-titulo">Encuesta a {{ c }}</div>
              <div class="enlace-url">{{ enlace(c) }}</div>
            </div>
            <n-space size="small" :wrap="false">
              <n-button size="small" secondary @click="copiar(c)">Copiar</n-button>
              <n-button size="small" tag="a" :href="enlace(c)" target="_blank">Abrir</n-button>
            </n-space>
          </div>
        </n-gi>
      </n-grid>
      <div class="nota">Mientras el sistema corra en esta computadora, el enlace solo funciona en ella. Para que los titulados respondan desde su celular hay que publicar el sistema en internet.</div>
    </n-card>

    <div class="barra">
      <n-radio-group v-model:value="encuesta" size="small">
        <n-radio-button value="titulados">Titulados</n-radio-button>
        <n-radio-button value="empleadores">Empleadores</n-radio-button>
      </n-radio-group>
      <n-select v-model:value="origen" size="small" clearable placeholder="Todos los orígenes" style="width: 190px"
        :options="[{ label: 'Formulario', value: 'formulario' }, { label: 'Importado', value: 'importado' }, { label: 'Prueba', value: 'prueba' }]" />
      <span style="flex: 1" />
      <n-popconfirm @positive-click="eliminarPrueba">
        <template #trigger><n-button size="small" type="error" secondary>Eliminar datos de prueba</n-button></template>
        Se borran las respuestas de prueba de ambas encuestas. Las del formulario y las importadas no se tocan.
      </n-popconfirm>
    </div>

    <n-grid v-if="actual" :cols="4" :x-gap="14" :y-gap="14" responsive="screen" item-responsive>
      <n-gi span="4 m:2 l:1"><Kpi titulo="Del formulario propio" color="#14b8a6" :valor="String(actual.respuestas.formulario)" /></n-gi>
      <n-gi span="4 m:2 l:1"><Kpi titulo="Importadas de Forms" color="#0ea5e9" :valor="String(actual.respuestas.importado)" /></n-gi>
      <n-gi span="4 m:2 l:1"><Kpi titulo="De prueba" color="#71717a" :valor="String(actual.respuestas.prueba)" /></n-gi>
      <n-gi span="4 m:2 l:1"><Kpi titulo="Versión de la encuesta" :valor="String(actual.version)" detalle="Sube con cada cambio en el editor" /></n-gi>
    </n-grid>

    <n-card size="small" title="Respuestas del formulario en los últimos 30 días" style="margin-top: 14px">
      <v-chart v-if="porDia.length" style="height: 200px" :option="graficoDias" autoresize />
      <div v-else class="nota">Todavía no hay respuestas del formulario propio en los últimos 30 días.</div>
    </n-card>

    <n-card size="small" style="margin-top: 14px" :title="`Listado (${listado.total})`">
      <n-data-table size="small" :columns="columnas" :data="listado.filas" :loading="cargando" :row-key="(f) => f.id" />
      <div style="display: flex; justify-content: flex-end; margin-top: 10px">
        <n-pagination v-model:page="pagina" :item-count="listado.total" :page-size="tamano" />
      </div>
    </n-card>

    <n-drawer :show="!!detalle" :width="560" @update:show="(v) => { if (!v) detalle = null }">
      <n-drawer-content v-if="detalle" :title="`Respuesta N.º ${detalle.id}`" closable>
        <n-space size="small" style="margin-bottom: 12px">
          <n-tag :bordered="false">{{ detalle.fecha }}</n-tag>
          <n-tag :bordered="false" :type="tipoOrigen[detalle.origen]">{{ detalle.origen }}</n-tag>
          <n-tag :bordered="false">versión {{ detalle.version }}</n-tag>
        </n-space>
        <div v-for="f in filasDetalle" :key="f.codigo" class="dato">
          <div class="dato-pregunta">{{ f.pregunta }}</div>
          <div class="dato-valor">{{ f.valor }}</div>
        </div>
        <template v-if="detalle.contacto.email || detalle.contacto.telefono || detalle.contacto.linkedin">
          <n-divider title-placement="left">Datos de contacto (no entran al análisis)</n-divider>
          <div v-if="detalle.contacto.email" class="dato"><div class="dato-pregunta">Email</div><div class="dato-valor">{{ detalle.contacto.email }}</div></div>
          <div v-if="detalle.contacto.telefono" class="dato"><div class="dato-pregunta">Teléfono</div><div class="dato-valor">{{ detalle.contacto.telefono }}</div></div>
          <div v-if="detalle.contacto.linkedin" class="dato"><div class="dato-pregunta">LinkedIn</div><div class="dato-valor">{{ detalle.contacto.linkedin }}</div></div>
        </template>
        <template #footer>
          <n-popconfirm @positive-click="eliminar(detalle.id)">
            <template #trigger><n-button type="error" secondary>Eliminar respuesta</n-button></template>
            ¿Eliminar esta respuesta?
          </n-popconfirm>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<style scoped>
.barra { display: flex; align-items: center; gap: 10px; margin: 16px 0 14px; flex-wrap: wrap; }
.enlace { display: flex; justify-content: space-between; align-items: center; gap: 12px; border: 1px solid #e4e4e7; border-radius: 10px; padding: 10px 12px; }
.enlace-titulo { font-weight: 600; font-size: 13.5px; }
.enlace-url { color: #71717a; font-size: 12px; word-break: break-all; }
.dato { padding: 8px 0; border-bottom: 1px solid #f4f4f5; }
.dato-pregunta { color: #71717a; font-size: 12.5px; }
.dato-valor { font-size: 14px; margin-top: 2px; }
</style>
