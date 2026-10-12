<script setup>
import { reactive, computed } from 'vue'
import VChart from 'vue-echarts'
import Kpi from '../components/Kpi.vue'
import Estado from '../components/Estado.vue'
import { useApi } from '../useApi'
import { pct, num } from '../api'
import { histogramaComparado } from '../graficos'

const p = reactive({ corridas: 1000, cohorte: 100, semilla: 12345, ajusteEmpleo: 50, ajusteTiempo: 30 })
const { datos: d, error, cargando, cargar } = useApi('/api/simulacion', () => ({ ...p }))

const delta = (a, b, f) => (b - a >= 0 ? '+' : '') + f(b - a)
const colsTI = [
  { title: 'x', key: 'etiqueta' },
  { title: 'p(x)', key: 'probabilidad', render: (r) => num(r.probabilidad, 3) },
  { title: 'F(x)', key: 'acumulada', render: (r) => num(r.acumulada, 3) },
  { title: 'Rango de R', key: 'rango', render: (r) => `[${num(r.rangoDesde, 3)} , ${num(r.rangoHasta, 3)})` },
]
const colsCohorte = [
  { title: 'Titulado', key: 'titulado' },
  { title: 'R₁ ← gcm()', key: 'r1', render: (r) => num(r.r1, 4) },
  { title: 'Situación = F⁻¹(R₁)', key: 'situacion' },
  { title: 'R₂ ← gcm()', key: 'r2', render: (r) => (r.r2 == null ? '—' : num(r.r2, 4)) },
  { title: 'Tiempo = F⁻¹(R₂)', key: 'tiempo' },
  { title: 'Meses', key: 'meses', render: (r) => (r.meses == null ? '—' : r.meses) },
]
const colsCorridas = [
  { title: 'Corrida', key: 'corrida' },
  { title: 'Empleados', key: 'empleados' },
  { title: 'Tasa de empleo', key: 'tasaEmpleo', render: (r) => pct(r.tasaEmpleo) },
  { title: 'En organización', key: 'enOrganizacion' },
  { title: 'Tiempo medio (meses)', key: 'tiempoMedio', render: (r) => num(r.tiempoMedio) },
]
const histTasa = computed(() => d.value && histogramaComparado(d.value.base.histogramaTasa, d.value.escenario.histogramaTasa, { formato: (v) => `${Math.round(v * 100)}%` }))
const histTiempo = computed(() => d.value && histogramaComparado(d.value.base.histogramaTiempo, d.value.escenario.histogramaTiempo, { decimales: 1 }))
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">Experimentación · ARCU-SUR 3.2.3</div>
    <h1 class="pagina-titulo">Simulación de escenarios</h1>
    <p class="pagina-sub">
      Se generan cohortes de titulados con Transformada Inversa discreta a partir de las frecuencias observadas en la encuesta,
      y se compara el escenario actual (base) contra uno modificado.
    </p>

    <n-card size="small" title="Parámetros">
      <n-grid :cols="5" :x-gap="18" :y-gap="12" responsive="screen" item-responsive>
        <n-gi span="5 m:1"><n-form-item label="Corridas (n)" :show-feedback="false"><n-input-number v-model:value="p.corridas" :min="1" :max="20000" :step="100" /></n-form-item></n-gi>
        <n-gi span="5 m:1"><n-form-item label="Titulados por cohorte" :show-feedback="false"><n-input-number v-model:value="p.cohorte" :min="1" :max="5000" /></n-form-item></n-gi>
        <n-gi span="5 m:1"><n-form-item label="Semilla x₀" :show-feedback="false"><n-input-number v-model:value="p.semilla" :min="0" /></n-form-item></n-gi>
        <n-gi span="5 m:1">
          <n-form-item :label="`Reducir «No trabaja» en ${p.ajusteEmpleo}%`" :show-feedback="false">
            <n-slider v-model:value="p.ajusteEmpleo" :min="0" :max="100" :step="5" />
          </n-form-item>
        </n-gi>
        <n-gi span="5 m:1">
          <n-form-item :label="`Mover ${p.ajusteTiempo}% de los tiempos > 4 meses a 1–4 meses`" :show-feedback="false">
            <n-slider v-model:value="p.ajusteTiempo" :min="0" :max="100" :step="5" />
          </n-form-item>
        </n-gi>
      </n-grid>
      <div style="display: flex; gap: 12px; align-items: center; margin-top: 14px">
        <n-button type="primary" :loading="cargando" @click="cargar">Ejecutar simulación</n-button>
        <span v-if="d" class="nota" style="margin: 0">Generador congruencial mixto: <code>{{ d.generador }}</code></span>
      </div>
    </n-card>

    <Estado :cargando="cargando && !d" :error="error" />

    <template v-if="d">
      <n-grid :cols="4" :x-gap="14" :y-gap="14" style="margin-top: 14px" responsive="screen" item-responsive>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Tasa de empleo · base" :valor="pct(d.base.tasaEmpleo.media)" :detalle="`IC 95%: ${pct(d.base.tasaEmpleo.limiteInferior, 2)} – ${pct(d.base.tasaEmpleo.limiteSuperior, 2)}`" /></n-gi>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Tasa de empleo · escenario" color="#14b8a6" :valor="pct(d.escenario.tasaEmpleo.media)" :detalle="`${delta(d.base.tasaEmpleo.media, d.escenario.tasaEmpleo.media, (x) => pct(x, 1))} respecto a la base`" /></n-gi>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Tiempo medio al 1er empleo · base" color="#f59e0b" :valor="`${num(d.base.tiempoMedio.media)} m`" :detalle="`IC 95%: ${num(d.base.tiempoMedio.limiteInferior)} – ${num(d.base.tiempoMedio.limiteSuperior)}`" /></n-gi>
        <n-gi span="4 m:2 l:1"><Kpi titulo="Tiempo medio · escenario" color="#8b5cf6" :valor="`${num(d.escenario.tiempoMedio.media)} m`" :detalle="`${delta(d.base.tiempoMedio.media, d.escenario.tiempoMedio.media, (x) => num(x) + ' meses')} respecto a la base`" /></n-gi>
      </n-grid>

      <n-grid :cols="2" :x-gap="14" :y-gap="14" style="margin-top: 14px" responsive="screen" item-responsive>
        <n-gi span="2 l:1"><n-card title="Distribución de la tasa de empleo por corrida" size="small"><v-chart class="grafico" :option="histTasa" autoresize /></n-card></n-gi>
        <n-gi span="2 l:1"><n-card title="Distribución del tiempo medio por corrida (meses)" size="small"><v-chart class="grafico" :option="histTiempo" autoresize /></n-card></n-gi>
      </n-grid>

      <n-card title="Modelo: tablas de Transformada Inversa discreta" size="small" style="margin-top: 14px">
        <n-grid :cols="2" :x-gap="18" :y-gap="14" responsive="screen" item-responsive>
          <n-gi span="2 l:1"><div class="sub">Situación laboral · base</div><n-data-table size="small" :columns="colsTI" :data="d.tablasTI.situacionBase" /></n-gi>
          <n-gi span="2 l:1"><div class="sub">Situación laboral · escenario</div><n-data-table size="small" :columns="colsTI" :data="d.tablasTI.situacionEscenario" /></n-gi>
          <n-gi span="2 l:1"><div class="sub">Tiempo al primer empleo · base</div><n-data-table size="small" :columns="colsTI" :data="d.tablasTI.tiempoBase" /></n-gi>
          <n-gi span="2 l:1"><div class="sub">Tiempo al primer empleo · escenario</div><n-data-table size="small" :columns="colsTI" :data="d.tablasTI.tiempoEscenario" /></n-gi>
        </n-grid>
        <div class="nota">Se elige la primera categoría cuya F(x) es mayor que R. El tiempo solo se genera para quien trabaja en una organización, igual que en el formulario.</div>
      </n-card>

      <n-card title="Tabla de simulación (primeros 10 titulados de la corrida 1, escenario base)" size="small" style="margin-top: 14px">
        <n-data-table size="small" :columns="colsCohorte" :data="d.base.primeraCohorte" />
      </n-card>

      <n-card title="Prueba de escritorio (primeras 25 corridas, escenario base)" size="small" style="margin-top: 14px">
        <n-data-table size="small" :columns="colsCorridas" :data="d.base.corridas" :pagination="{ pageSize: 10 }" />
      </n-card>
    </template>
  </div>
</template>

<style scoped>
.sub { font-size: 12.5px; color: #71717a; font-weight: 500; margin-bottom: 6px; }
code { font-size: 12px; background: #f4f4f5; padding: 2px 6px; border-radius: 4px; }
</style>
