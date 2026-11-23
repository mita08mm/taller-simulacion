<script setup>
import { computed, inject, useSlots } from 'vue'
import Kpi from './Kpi.vue'
import Estado from './Estado.vue'
import CruceDetalle from './CruceDetalle.vue'
import UniformidadDetalle from './UniformidadDetalle.vue'
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
// Pruebas de uniformidad (prueba de las frecuencias) de las preguntas de este criterio
const { datos: pruebasUniformidad } = useApi('/api/uniformidad')
const uniformidad = computed(() => (pruebasUniformidad.value ?? []).filter((u) => u.pantalla === props.pantalla))
const seValora = computed(() => c.value && c.value.favorables.length + c.value.desfavorables.length > 0)

// Conclusión del criterio en una frase: quién respondió, qué se concluye y con cuántos aspectos
const conclusion = computed(() => {
  if (!c.value || !seValora.value) return c.value?.nota ?? null
  const fav = c.value.favorables.length
  const total = fav + c.value.desfavorables.length
  const veredicto = fav === total ? 'el criterio se cumple'
    : fav > total - fav ? 'el criterio se cumple en la mayoría de los aspectos'
      : fav === total - fav ? 'el criterio se cumple solo a medias'
        : 'el criterio presenta debilidades'
  return `Según ${c.value.nFuente} ${c.value.fuente}, ${veredicto}: ${fav} de ${total} aspectos superan el 50% de respuestas a favor.`
})

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
      <div class="titulo-grupo">
        <h1 class="pagina-titulo">{{ c.nombre }}</h1>
        <n-tooltip trigger="hover">
          <template #trigger><span class="etiqueta-criterio">{{ c.id }}</span></template>
          ARCU-SUR es el sistema de acreditación de carreras universitarias del MERCOSUR; sus criterios son los requisitos de calidad que evalúa.
          El criterio {{ c.id }} pide: {{ c.quePide }}
        </n-tooltip>
      </div>
      <p class="pagina-sub">{{ conclusion }}</p>

      <div class="valoracion">
        <template v-if="seValora">
          <div class="valoracion-resumen">
            <div class="valoracion-cifra-grupo">
              <div class="valoracion-cifra">{{ c.favorables.length }}<span> de {{ c.favorables.length + c.desfavorables.length }}</span></div>
              <n-tooltip trigger="hover">
                <template #trigger><span class="info">i</span></template>
                Favorable = más del 50% de los {{ c.fuente }} responde a favor.
              </n-tooltip>
            </div>
            <div class="nota" style="margin: 2px 0 0">aspectos favorables</div>
          </div>
          <div>
          <div class="nota" style="margin: 0 0 8px">El porcentaje de cada aspecto es el {{ c.medida }}.</div>
          <div class="valoracion-columnas">
            <div class="valoracion-columna">
              <div class="valoracion-columna-titulo bien">Favorables ({{ c.favorables.length }})</div>
              <div v-if="!c.favorables.length" class="nota" style="margin: 4px 0 0">Ningún aspecto superó el 50%.</div>
              <div v-for="f in c.favorables" :key="f.texto" class="aspecto">
                <div class="aspecto-fila">
                  <span class="aspecto-texto">{{ f.texto }}</span><b>{{ pct(f.proporcion) }}</b>
                </div>
                <div class="aspecto-barra"><div class="aspecto-barra-llena bien" :style="{ width: f.proporcion * 100 + '%' }" /></div>
              </div>
            </div>
            <div class="valoracion-columna">
              <div class="valoracion-columna-titulo mal">Desfavorables ({{ c.desfavorables.length }})</div>
              <div v-if="!c.desfavorables.length" class="nota" style="margin: 4px 0 0">Ningún aspecto quedó por debajo del 50%.</div>
              <div v-for="f in c.desfavorables" :key="f.texto" class="aspecto">
                <div class="aspecto-fila">
                  <span class="aspecto-texto">{{ f.texto }}</span><b class="mal">{{ pct(f.proporcion) }}</b>
                </div>
                <div class="aspecto-barra"><div class="aspecto-barra-llena mal" :style="{ width: f.proporcion * 100 + '%' }" /></div>
              </div>
            </div>
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

      <template v-if="uniformidad.length">
        <div class="titulo-grupo">
          <div class="seccion-titulo">¿Hay respuestas preferidas?</div>
          <n-tooltip trigger="hover">
            <template #trigger><span class="info">i</span></template>
            Prueba de las frecuencias (principio de uniformidad): indica si las respuestas se reparten por igual entre las opciones o si hay opciones preferidas que no se explican por azar.
          </n-tooltip>
        </div>
        <n-collapse class="relaciones" :default-expanded-names="[]">
          <n-collapse-item v-for="u in uniformidad" :key="u.id" :name="u.id">
            <template #header>
              <div>
                <div class="relacion-titulo">{{ u.titulo }}</div>
                <div class="nota" style="margin: 2px 0 0">
                  {{ u.uniforme == null ? 'No hay respuestas suficientes.' : u.uniforme ? `Las respuestas se reparten parejo entre las ${u.k} opciones.` : `Preferencia clara por «${u.masElegida}» (${pct(u.proporcionMasElegida)}).` }}
                </div>
              </div>
            </template>
            <template #header-extra>
              <n-tag size="small" round :bordered="false" :type="u.uniforme === false ? 'warning' : 'default'">
                {{ u.uniforme == null ? 'Sin datos' : u.uniforme ? 'Uniformidad' : 'No uniformidad' }}
              </n-tag>
            </template>
            <UniformidadDetalle :u="u" />
          </n-collapse-item>
        </n-collapse>
      </template>

      <template v-if="relaciones.length">
        <div class="titulo-grupo">
          <div class="seccion-titulo">Relaciones entre preguntas</div>
          <n-tooltip trigger="hover">
            <template #trigger><span class="info">i</span></template>
            Indica si dos respuestas están relacionadas o si sus diferencias se deben al azar.
          </n-tooltip>
        </div>
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
.titulo-grupo { display: flex; align-items: center; gap: 8px; }
.pagina-titulo { margin-bottom: 0; }
.titulo-grupo:has(.seccion-titulo) { margin: 32px 0 12px; }
.titulo-grupo:has(.seccion-titulo) .seccion-titulo { margin: 0; }
.etiqueta-criterio {
  font-size: 11px; color: var(--acento); font-weight: 600; letter-spacing: 0.02em;
  background: var(--acento-suave); border-radius: 5px; padding: 2px 6px; margin: 0;
  cursor: help;
}
.info {
  display: inline-flex; align-items: center; justify-content: center;
  width: 15px; height: 15px; border-radius: 50%; border: 1px solid var(--suave);
  color: var(--suave); font-size: 10px; font-style: italic; font-family: Georgia, serif;
  cursor: help; flex-shrink: 0;
}
.valoracion { display: grid; grid-template-columns: 220px 1fr; gap: 24px; background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 18px 20px; }
.valoracion:has(> .nota) { display: block; }
.valoracion-cifra-grupo { display: flex; align-items: baseline; gap: 8px; }
.valoracion-cifra { font-size: 34px; font-weight: 650; letter-spacing: -0.02em; line-height: 1; }
.valoracion-cifra span { font-size: 16px; color: var(--suave); font-weight: 500; }
.valoracion-columnas { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.valoracion-columna-titulo { font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.02em; margin-bottom: 6px; }
.aspecto { padding: 7px 0; border-bottom: 1px solid var(--borde-suave); font-size: 13px; }
.aspecto:last-child { border-bottom: none; }
.aspecto-fila { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; }
.aspecto-texto { color: var(--texto); }
.aspecto-barra { height: 4px; border-radius: 2px; background: var(--borde-suave); margin-top: 5px; overflow: hidden; }
.aspecto-barra-llena { height: 100%; border-radius: 2px; }
.aspecto-barra-llena.bien { background: var(--bien); }
.aspecto-barra-llena.mal { background: var(--mal); }
.bien { color: var(--bien); }
.mal { color: var(--mal); }
@media (max-width: 700px) { .valoracion-columnas { grid-template-columns: 1fr; } }
.relaciones { background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 4px 18px 12px; }
.relacion-titulo { font-weight: 600; font-size: 14px; }
@media (max-width: 900px) { .valoracion { grid-template-columns: 1fr; } }
</style>
