<script setup>
import { ref, reactive, provide, computed, h, watch } from 'vue'
import { obtener } from './api'
import { parametrosFiltro } from './useApi'
import Resumen from './views/Resumen.vue'
import Empleabilidad from './views/Empleabilidad.vue'
import Formacion from './views/Formacion.vue'
import Competencias from './views/Competencias.vue'
import Cruces from './views/Cruces.vue'
import Empleadores from './views/Empleadores.vue'
import Simulacion from './views/Simulacion.vue'
import Datos from './views/Datos.vue'

const version = ref(0)
provide('version', version)
const estado = ref(null)
// Filtro global por fecha de respuesta: separa datos nuevos de antiguos o fija una fecha de corte
const filtro = reactive({ desde: null, hasta: null })
provide('filtro', filtro)
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

const punto = (color) => () => h('span', { style: `display:inline-block;width:8px;height:8px;border-radius:50%;background:${color}` })
const opciones = [
  { label: 'Inicio', key: 'resumen', icon: punto('#4f46e5') },
  { type: 'group', label: 'Titulados', key: 'g1', children: [
    { label: 'Empleabilidad', key: 'empleabilidad', icon: punto('#14b8a6') },
    { label: 'Formación y posgrado', key: 'formacion', icon: punto('#f59e0b') },
    { label: 'Valoración de la formación', key: 'competencias', icon: punto('#8b5cf6') },
  ] },
  { type: 'group', label: 'Empleadores', key: 'g2', children: [
    { label: 'Perfil y demanda', key: 'empleadores', icon: punto('#0ea5e9') },
  ] },
  { type: 'group', label: 'Análisis', key: 'g3', children: [
    { label: 'Cruces de variables', key: 'cruces', icon: punto('#ef4444') },
    { label: 'Simulación de escenarios', key: 'simulacion', icon: punto('#84cc16') },
  ] },
  { type: 'group', label: 'Datos', key: 'g4', children: [
    { label: 'Importar y exportar', key: 'datos', icon: punto('#64748b') },
  ] },
]
const vistas = { resumen: Resumen, empleabilidad: Empleabilidad, formacion: Formacion, competencias: Competencias, cruces: Cruces, empleadores: Empleadores, simulacion: Simulacion, datos: Datos }
const actual = ref('resumen')
const vista = computed(() => vistas[actual.value])

const tema = {
  common: { primaryColor: '#4f46e5', primaryColorHover: '#6366f1', primaryColorPressed: '#4338ca', fontFamily: 'Inter, system-ui, sans-serif', borderRadius: '8px' },
  Card: { borderRadius: '12px' },
}
</script>

<template>
  <n-config-provider :theme-overrides="tema">
    <n-message-provider>
      <n-layout has-sider style="height: 100vh">
        <n-layout-sider bordered :width="260" content-style="padding: 20px 8px;" :native-scrollbar="false">
          <div class="marca">
            <div class="marca-logo">IS</div>
            <div>
              <div class="marca-titulo">Observatorio de Titulados</div>
              <div class="marca-sub">Ing. de Sistemas · UMSS</div>
            </div>
          </div>
          <n-menu v-model:value="actual" :options="opciones" :indent="14" />
          <div class="fuente">
            <div class="fuente-titulo">Período de respuestas</div>
            <n-date-picker v-model:value="rango" type="daterange" clearable size="small" :shortcuts="atajos"
              start-placeholder="Desde" end-placeholder="Hasta" style="margin-top: 6px" />
            <template v-if="estado">
              <div style="margin-top: 10px">Titulados: <b>{{ estado.titulados.n }}</b><span class="de"> de {{ estado.titulados.nTotal }}</span></div>
              <div class="fuente-origen">{{ estado.titulados.origen }} · {{ estado.titulados.periodo.desde ?? '—' }} a {{ estado.titulados.periodo.hasta ?? '—' }}</div>
              <div style="margin-top: 6px">Empleadores: <b>{{ estado.empleadores.n }}</b><span class="de"> de {{ estado.empleadores.nTotal }}</span></div>
              <div class="fuente-origen">{{ estado.empleadores.origen }} · {{ estado.empleadores.periodo.desde ?? '—' }} a {{ estado.empleadores.periodo.hasta ?? '—' }}</div>
            </template>
          </div>
        </n-layout-sider>
        <n-layout-content :native-scrollbar="false">
          <component :is="vista" @navegar="(k) => (actual = k)" />
        </n-layout-content>
      </n-layout>
    </n-message-provider>
  </n-config-provider>
</template>

<style scoped>
.marca { display: flex; align-items: center; gap: 10px; padding: 0 12px 20px; }
.marca-logo { width: 34px; height: 34px; border-radius: 9px; background: #4f46e5; color: #fff; display: grid; place-items: center; font-weight: 700; font-size: 13px; }
.marca-titulo { font-weight: 600; font-size: 14px; }
.marca-sub { color: #71717a; font-size: 12px; }
.fuente { margin: 24px 12px 0; padding: 12px; border: 1px solid #e4e4e7; border-radius: 10px; font-size: 12.5px; background: #fafafa; }
.fuente-origen { color: #71717a; font-size: 11.5px; }
.fuente-titulo { font-weight: 600; font-size: 12.5px; }
.de { color: #71717a; }
</style>
