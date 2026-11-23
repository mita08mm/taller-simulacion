<script setup>
import { ref, reactive, provide, computed, watch } from 'vue'
import { esAR, dateEsAR } from 'naive-ui'
import { obtener } from './api'
import { parametrosFiltro } from './useApi'
import { temaNaive as tema } from './tema'
import Inicio from './views/Inicio.vue'
import Empleo from './views/criterios/Empleo.vue'
import Resultados from './views/criterios/Resultados.vue'
import Seguimiento from './views/criterios/Seguimiento.vue'
import PerfilEgreso from './views/criterios/PerfilEgreso.vue'
import Curricular from './views/criterios/Curricular.vue'
import Posgrado from './views/criterios/Posgrado.vue'
import Relaciones from './views/criterios/Relaciones.vue'
import Comparar from './views/Comparar.vue'
import ExplorarVariable from './views/ExplorarVariable.vue'
import Datos from './views/Datos.vue'
import Editor from './views/Editor.vue'
import RespuestasAdmin from './views/RespuestasAdmin.vue'
import EncuestaPublica from './views/EncuestaPublica.vue'
import Reporte from './views/Reporte.vue'
import ReporteConfig from './views/ReporteConfig.vue'

// Ruta pública del formulario: #/encuesta/titulados o #/encuesta/empleadores (sin el menú del panel)
const hash = ref(location.hash)
window.addEventListener('hashchange', () => { hash.value = location.hash })
const encuestaPublica = computed(() => hash.value.match(/^#\/encuesta\/(titulados|empleadores)/)?.[1] ?? null)
// Reporte imprimible armado en «Generar reporte»: #/imprimir?encuesta=...&variables=...&desde=...&hasta=..., sin el menú
const esReporte = computed(() => hash.value.startsWith('#/imprimir'))

const version = ref(0)
provide('version', version)
const estado = ref(null)
// Filtro global por fecha de respuesta: separa datos nuevos de antiguos o fija una fecha de corte
const filtro = reactive({ desde: null, hasta: null })
provide('filtro', filtro)
if (esReporte.value) {
  const q = new URLSearchParams(location.hash.split('?')[1] ?? '')
  filtro.desde = q.get('desde')
  filtro.hasta = q.get('hasta')
}
const aIso = (ms) => (ms == null ? null : new Date(ms).toISOString().slice(0, 10))
const rango = ref(null)
watch(rango, (r) => { filtro.desde = aIso(r?.[0]); filtro.hasta = aIso(r?.[1]) })
// Fechas con respuestas (de las dos encuestas): fuera de ese rango no se puede elegir nada en los calendarios
const rangoDatos = ref(null)
provide('rangoDatos', rangoDatos)
const aFecha = (iso) => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, m - 1, d) }
async function cargarRangoDatos() {
  try {
    const e = await obtener('/api/estado')
    const fechas = [e.titulados.periodo, e.empleadores.periodo].flatMap((p) => [p.desde, p.hasta]).filter(Boolean).sort()
    rangoDatos.value = fechas.length ? { desde: fechas[0], hasta: fechas.at(-1) } : null
  } catch { rangoDatos.value = null }
}
const fueraDeRango = (ts) => {
  if (!rangoDatos.value) return false
  const ini = aFecha(rangoDatos.value.desde).getTime()
  const fin = aFecha(rangoDatos.value.hasta).getTime() + 864e5 - 1
  return ts < ini || ts > fin
}
provide('fueraDeRango', fueraDeRango)
const atajos = computed(() => {
  if (!rangoDatos.value) return {}
  const ini = aFecha(rangoDatos.value.desde).getTime(), fin = aFecha(rangoDatos.value.hasta).getTime()
  const anio = aFecha(rangoDatos.value.hasta).getFullYear()
  return {
    'Todo el período': () => [ini, fin],
    [`Gestión ${anio}`]: () => [Math.max(ini, new Date(anio, 0, 1).getTime()), fin],
    'Último mes con respuestas': () => [Math.max(ini, new Date(aFecha(rangoDatos.value.hasta).getFullYear(), aFecha(rangoDatos.value.hasta).getMonth(), 1).getTime()), fin],
  }
})
async function actualizarEstado() {
  try { estado.value = await obtener('/api/estado', parametrosFiltro(filtro)) } catch { estado.value = null }
}
watch(() => [filtro.desde, filtro.hasta], actualizarEstado)
// Cualquier pantalla que cambie datos (importar, editar preguntas o respuestas) llama a esto vía useAccion:
// sube la versión y todas las vistas que la observan vuelven a pedir sus datos
provide('alCambiarDatos', () => { version.value++; actualizarEstado(); cargarRangoDatos() })
actualizarEstado()
cargarRangoDatos()

// Barra superior: de qué encuesta son los datos de la pantalla y de qué meses (ej. «Encuesta a 53 empleadores · sep–oct 2026»)
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
function meses(periodo) {
  if (!periodo?.desde || !periodo?.hasta) return ''
  const a = aFecha(periodo.desde), b = aFecha(periodo.hasta)
  if (a.getFullYear() === b.getFullYear()) {
    return a.getMonth() === b.getMonth() ? `${MESES[a.getMonth()]} ${a.getFullYear()}` : `${MESES[a.getMonth()]}–${MESES[b.getMonth()]} ${b.getFullYear()}`
  }
  return `${MESES[a.getMonth()]} ${a.getFullYear()} – ${MESES[b.getMonth()]} ${b.getFullYear()}`
}
const FUENTE = { perfil: 'empleadores', relaciones: 'empleadores', empleo: 'titulados', resultados: 'titulados', posgrado: 'titulados', comparar: 'titulados' }
const textoFuente = computed(() => {
  if (!estado.value) return null
  const t = estado.value.titulados, e = estado.value.empleadores
  const tit = `${t.n} titulados · ${meses(t.periodo)}`, emp = `${e.n} empleadores · ${meses(e.periodo)}`
  const f = FUENTE[actual.value]
  return f === 'empleadores' ? `Encuesta a ${emp}` : f === 'titulados' ? `Encuesta a ${tit}` : `Encuestas: ${tit} | ${emp}`
})

// Los códigos ARCU-SUR (2.1.2, 3.2.1...) le sirven al comité de acreditación,
// no a quien navega el panel -- no van en el menú, solo como tooltip en cada página.
const opciones = [
  { label: 'Inicio', key: 'inicio' },
  { type: 'group', label: 'Resultados por tema', key: 'g-criterios', children: [
    { label: 'Perfil de egreso', key: 'perfil' },
    { label: 'Actualización curricular', key: 'curricular' },
    { label: 'Actualización y posgrado', key: 'posgrado' },
    { label: 'Relación con organizaciones', key: 'relaciones' },
    { label: 'Resultados de la formación', key: 'resultados' },
    { label: 'Seguimiento a graduados', key: 'seguimiento' },
    { label: 'Condiciones de empleo', key: 'empleo' },
  ] },
  { type: 'group', label: 'Herramientas', key: 'g-herramientas', children: [
    { label: 'Explorar una variable', key: 'explorar' },
    { label: 'Comparar dos preguntas', key: 'comparar' },
    { label: 'Generar reporte', key: 'reporte' },
  ] },
  { type: 'group', label: 'Administración', key: 'g-admin', children: [
    { label: 'Preguntas de la encuesta', key: 'editor' },
    { label: 'Respuestas', key: 'respuestas' },
    { label: 'Importar y exportar', key: 'datos' },
  ] },
]
const vistas = {
  inicio: Inicio, empleo: Empleo, resultados: Resultados, seguimiento: Seguimiento, perfil: PerfilEgreso,
  curricular: Curricular, posgrado: Posgrado, relaciones: Relaciones, explorar: ExplorarVariable, comparar: Comparar,
  reporte: ReporteConfig, datos: Datos, editor: Editor, respuestas: RespuestasAdmin,
}
// Cada pantalla tiene su dirección (#/empleo, #/perfil, ...): funciona el botón atrás y se puede compartir el enlace
const desdeHash = (hs) => { const k = hs.match(/^#\/([a-z]+)$/)?.[1]; return k && vistas[k] ? k : 'inicio' }
const actual = ref(desdeHash(location.hash))
watch(hash, (hs) => { if (!encuestaPublica.value && !esReporte.value) actual.value = desdeHash(hs) })
watch(actual, (k) => { if (location.hash !== '#/' + k) location.hash = '#/' + k })
const vista = computed(() => vistas[actual.value])
const contenido = ref(null)
watch(actual, () => contenido.value?.scrollTo({ top: 0 }))

</script>

<template>
  <!-- Textos internos de los componentes (placeholders, calendario, «Confirmar»...) en español -->
  <n-config-provider :theme-overrides="tema" :locale="esAR" :date-locale="dateEsAR">
    <n-message-provider>
    <n-dialog-provider>
      <EncuestaPublica v-if="encuestaPublica" :codigo="encuestaPublica" :key="encuestaPublica" />
      <Reporte v-else-if="esReporte" />
      <n-layout v-else has-sider style="height: 100vh">
        <n-layout-sider bordered :width="292" content-style="padding: 18px 8px;" :native-scrollbar="false">
          <div class="marca">
            <div class="marca-logo">IS</div>
            <div>
              <div class="marca-titulo">Observatorio de Titulados</div>
              <div class="marca-sub">Ingeniería de Sistemas · UMSS</div>
            </div>
          </div>
          <n-menu v-model:value="actual" :options="opciones" :indent="16" />
        </n-layout-sider>
        <n-layout>
          <div class="barra">
            <div class="barra-datos" v-if="estado">
              <span>{{ textoFuente }}</span>
              <span v-if="filtro.desde || filtro.hasta" class="filtrado">filtrado por fecha</span>
            </div>
            <span v-else class="barra-datos">Sin conexión con el backend</span>
            <n-date-picker v-model:value="rango" type="daterange" clearable size="small" :shortcuts="atajos" :is-date-disabled="fueraDeRango"
              start-placeholder="Desde" end-placeholder="Hasta" style="width: 260px" />
          </div>
          <div ref="contenido" class="contenido">
            <component :is="vista" @navegar="(k) => (actual = k)" />
          </div>
        </n-layout>
      </n-layout>
    </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<style scoped>
.marca { display: flex; align-items: center; gap: 10px; padding: 0 12px 22px; }
.marca-logo { width: 34px; height: 34px; border-radius: 9px; background: var(--acento); color: var(--sobre-acento); display: grid; place-items: center; font-weight: 700; font-size: 13px; }
.marca-titulo { font-weight: 600; font-size: 14px; }
.marca-sub { color: var(--suave); font-size: 12px; }
.barra { height: 56px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 0 32px; border-bottom: 1px solid var(--borde); background: var(--superficie); }
.barra-datos { font-size: 13.5px; color: var(--suave); display: flex; align-items: center; gap: 6px; }
.barra-datos b { color: var(--texto); }
.filtrado { margin-left: 8px; font-size: 12px; color: var(--acento); background: var(--acento-suave); padding: 2px 8px; border-radius: 10px; }
.contenido { height: calc(100vh - 56px); overflow-y: auto; background: var(--fondo); }
</style>
