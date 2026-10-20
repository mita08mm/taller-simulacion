<script setup>
import { ref, reactive, provide, computed, h, watch } from 'vue'
import { obtener } from './api'
import { parametrosFiltro } from './useApi'
import Inicio from './views/Inicio.vue'
import Empleo from './views/criterios/Empleo.vue'
import Resultados from './views/criterios/Resultados.vue'
import Seguimiento from './views/criterios/Seguimiento.vue'
import PerfilEgreso from './views/criterios/PerfilEgreso.vue'
import Curricular from './views/criterios/Curricular.vue'
import Posgrado from './views/criterios/Posgrado.vue'
import Relaciones from './views/criterios/Relaciones.vue'
import Comparar from './views/Comparar.vue'
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
const atajos = {
  'Últimos 6 meses': () => [Date.now() - 182 * 864e5, Date.now()],
  'Este año': () => [new Date(new Date().getFullYear(), 0, 1).getTime(), Date.now()],
}
async function actualizarEstado() {
  try { estado.value = await obtener('/api/estado', parametrosFiltro(filtro)) } catch { estado.value = null }
}
watch(() => [filtro.desde, filtro.hasta], actualizarEstado)
provide('alImportar', () => { version.value++; actualizarEstado() })
actualizarEstado()

// Criterios ordenados por su número ARCU-SUR; el número va en gris delante del nombre
const criterio = (codigo, nombre, key) => ({
  key,
  label: () => h('span', { class: 'menu-criterio' }, [h('span', { class: 'menu-codigo' }, codigo), nombre]),
})
const opciones = [
  { label: 'Inicio', key: 'inicio' },
  { type: 'group', label: 'Criterios ARCU-SUR', key: 'g-criterios', children: [
    criterio('2.1.2', 'Perfil de egreso', 'perfil'),
    criterio('2.1.7', 'Actualización curricular', 'curricular'),
    criterio('2.4.1', 'Actualización y posgrado', 'posgrado'),
    criterio('2.4.2', 'Relación con organizaciones', 'relaciones'),
    criterio('3.2.1', 'Resultados de la formación', 'resultados'),
    criterio('3.2.2', 'Seguimiento a graduados', 'seguimiento'),
    criterio('3.2.3', 'Condiciones de empleo', 'empleo'),
  ] },
  { type: 'group', label: 'Herramientas', key: 'g-herramientas', children: [
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
  curricular: Curricular, posgrado: Posgrado, relaciones: Relaciones, comparar: Comparar,
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

const tema = {
  common: { primaryColor: '#4f46e5', primaryColorHover: '#6366f1', primaryColorPressed: '#4338ca', fontFamily: 'Inter, system-ui, sans-serif', borderRadius: '8px' },
  Card: { borderRadius: '12px' },
  Menu: { itemHeight: '38px', groupTextColor: '#9a9aa5', itemTextColorActive: '#4f46e5', itemColorActive: '#eef2ff', itemColorActiveHover: '#eef2ff' },
}
</script>

<template>
  <n-config-provider :theme-overrides="tema">
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
              <span><b>{{ estado.titulados.n }}</b> titulados</span>
              <span class="separador">·</span>
              <span><b>{{ estado.empleadores.n }}</b> empleadores</span>
              <span v-if="filtro.desde || filtro.hasta" class="filtrado">filtrado por fecha</span>
            </div>
            <span v-else class="barra-datos">Sin conexión con el backend</span>
            <n-date-picker v-model:value="rango" type="daterange" clearable size="small" :shortcuts="atajos"
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
.marca-logo { width: 34px; height: 34px; border-radius: 9px; background: #4f46e5; color: #fff; display: grid; place-items: center; font-weight: 700; font-size: 13px; }
.marca-titulo { font-weight: 600; font-size: 14px; }
.marca-sub { color: #71717a; font-size: 12px; }
.barra { height: 56px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 0 32px; border-bottom: 1px solid var(--borde); background: var(--superficie); }
.barra-datos { font-size: 13.5px; color: var(--suave); display: flex; align-items: center; gap: 6px; }
.barra-datos b { color: var(--texto); }
.separador { color: #d4d4d8; }
.filtrado { margin-left: 8px; font-size: 12px; color: var(--acento); background: var(--acento-suave); padding: 2px 8px; border-radius: 10px; }
.contenido { height: calc(100vh - 56px); overflow-y: auto; background: var(--fondo); }
:deep(.menu-criterio) { display: inline-flex; gap: 10px; align-items: baseline; }
:deep(.menu-codigo) { font-size: 11.5px; color: #a1a1aa; font-variant-numeric: tabular-nums; width: 30px; }
</style>
