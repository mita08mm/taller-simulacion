<script setup>
import { computed, inject, useSlots } from 'vue'
import Kpi from './Kpi.vue'
import Estado from './Estado.vue'
import CruceDetalle from './CruceDetalle.vue'
import { useApi } from '../useApi'
import { pct, urlExportar } from '../api'

// Estructura común de cada criterio: qué pide → valoración → indicadores → evidencia → relaciones → más detalle
const props = defineProps({ pantalla: String })
const slots = useSlots()
const filtro = inject('filtro')

const { datos: criterios, error, cargando } = useApi('/api/criterios')
const { datos: cruces } = useApi('/api/cruces')
const c = computed(() => criterios.value?.find((x) => x.pantalla === props.pantalla))
const relaciones = computed(() => (cruces.value ?? []).filter((x) => c.value?.cruces.includes(x.id)))
const seValora = computed(() => c.value && c.value.favorables.length + c.value.desfavorables.length > 0)

function resultado(x) {
  if (x.independientes == null) return 'No hay respuestas suficientes para comparar.'
  return x.independientes
    ? `No hay evidencia de que «${x.variableFilas}» y «${x.variableColumnas}» estén relacionadas.`
    : `«${x.variableFilas}» y «${x.variableColumnas}» están relacionadas.`
}
</script>

<template>
  <div class="pagina">
    <Estado :cargando="cargando && !c" :error="error" />
    <template v-if="c">
      <div class="etiqueta-criterio">Criterio ARCU-SUR {{ c.id }}</div>
      <h1 class="pagina-titulo">{{ c.nombre }}</h1>
      <p class="pagina-sub">{{ c.quePide }}</p>

      <div class="valoracion">
        <template v-if="seValora">
          <div class="valoracion-resumen">
            <div class="valoracion-cifra">{{ c.favorables.length }}<span> de {{ c.favorables.length + c.desfavorables.length }}</span></div>
            <div class="nota" style="margin: 2px 0 0">aspectos favorables</div>
            <div class="nota" style="margin-top: 10px">Un aspecto es favorable cuando más de la mitad de quienes respondieron lo confirma.</div>
          </div>
          <div class="valoracion-lista">
            <div v-for="f in c.favorables" :key="f.texto" class="aspecto">
              <span class="icono bien">✓</span><span class="aspecto-texto">{{ f.texto }}</span><b>{{ pct(f.proporcion) }}</b>
            </div>
            <div v-for="f in c.desfavorables" :key="f.texto" class="aspecto">
              <span class="icono mal">✕</span><span class="aspecto-texto">{{ f.texto }}</span><b class="mal">{{ pct(f.proporcion) }}</b>
            </div>
          </div>
        </template>
        <div v-else class="nota" style="margin: 0">{{ c.nota }}</div>
      </div>

      <div class="rejilla rejilla-3" style="margin-top: 14px">
        <Kpi v-for="i in c.indicadores" :key="i.titulo" :titulo="i.titulo" :valor="i.valor" :detalle="i.detalle" />
      </div>

      <div class="seccion-titulo">Evidencia</div>
      <slot :criterio="c" />

      <template v-if="relaciones.length">
        <div class="seccion-titulo">Relaciones entre preguntas</div>
        <p class="nota" style="margin: -4px 0 12px">Indica si dos respuestas están relacionadas o si sus diferencias se deben al azar.</p>
        <n-collapse class="relaciones" :default-expanded-names="[]">
          <n-collapse-item v-for="x in relaciones" :key="x.id" :name="x.id">
            <template #header>
              <div>
                <div class="relacion-titulo">{{ x.titulo.replace(' (opcional)', '') }}</div>
                <div class="nota" style="margin: 2px 0 0">{{ resultado(x) }}</div>
              </div>
            </template>
            <template #header-extra>
              <n-tag size="small" round :bordered="false" :type="x.independientes === false ? 'warning' : 'default'">
                {{ x.independientes == null ? 'Sin datos' : x.independientes ? 'Sin relación' : 'Relacionadas' }}
              </n-tag>
            </template>
            <CruceDetalle :c="x" compacto :url-csv="urlExportar(x.id, filtro)" :url-excel="urlExportar(x.id, filtro, { formato: 'excel' })" />
          </n-collapse-item>
        </n-collapse>
      </template>

      <template v-if="slots.detalle">
        <n-collapse style="margin-top: 28px">
          <n-collapse-item title="Más detalle" name="detalle">
            <slot name="detalle" :criterio="c" />
          </n-collapse-item>
        </n-collapse>
      </template>
    </template>
  </div>
</template>

<style scoped>
.valoracion { display: grid; grid-template-columns: 220px 1fr; gap: 24px; background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 18px 20px; }
.valoracion:has(> .nota) { display: block; }
.valoracion-cifra { font-size: 34px; font-weight: 650; letter-spacing: -0.02em; line-height: 1; }
.valoracion-cifra span { font-size: 16px; color: var(--suave); font-weight: 500; }
.aspecto { display: grid; grid-template-columns: 22px 1fr auto; align-items: center; gap: 8px; padding: 7px 0; border-bottom: 1px solid #f1f1f3; font-size: 14px; }
.aspecto:last-child { border-bottom: none; }
.icono { font-size: 12px; font-weight: 700; }
.bien { color: var(--bien); }
.mal { color: var(--mal); }
.relaciones { background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 4px 18px 12px; }
.relacion-titulo { font-weight: 600; font-size: 14px; }
@media (max-width: 900px) { .valoracion { grid-template-columns: 1fr; } }
</style>
