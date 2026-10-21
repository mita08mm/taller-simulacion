<script setup>
import { computed, inject } from 'vue'
import VChart from 'vue-echarts'
import { useApi } from '../useApi'
import { pct, urlExportar } from '../api'
import { barras, barrasAgrupadas, radarPerfil, paraImprimir } from '../graficos'

// Reporte a medida: encuesta, período y preguntas llegan en la dirección (#/imprimir?...) desde «Generar reporte».
// Se calcula al abrirlo con los datos de la base, listo para imprimir o guardar en PDF.
const filtro = inject('filtro')
const q = new URLSearchParams(location.hash.split('?')[1] ?? '')
const encuesta = q.get('encuesta') === 'empleadores' ? 'empleadores' : 'titulados'
const variables = q.get('variables') ?? ''
const conGraficos = q.get('graficos') !== '0'
const conArcusur = q.get('arcusur') === '1'
const titulo = q.get('titulo') || `Reporte de la encuesta a ${encuesta}`
const { datos: reporte, error } = useApi('/api/reporte', () => ({ encuesta, variables }))
const { datos: criterios } = useApi('/api/criterios')
const { datos: cruces } = useApi('/api/cruces')
const { datos: emp } = useApi('/api/empleabilidad')
const { datos: comp } = useApi('/api/competencias')
const { datos: form } = useApi('/api/formacion')
const { datos: empl } = useApi('/api/empleadores')
const { datos: estado } = useApi('/api/estado')

const ordenados = computed(() => [...(criterios.value ?? [])].sort((a, b) => a.id.localeCompare(b.id, undefined, { numeric: true })))
const listo = computed(() => reporte.value && estado.value
  && (!conArcusur || (criterios.value && cruces.value && emp.value && comp.value && form.value && empl.value)))
const imprimir = () => window.print()
const generado = new Date().toLocaleString('es-BO', { dateStyle: 'long', timeStyle: 'short' })
const periodo = q.get('periodo') || (filtro.desde || filtro.hasta
  ? `Respuestas del ${filtro.desde ?? 'inicio'} al ${filtro.hasta ?? 'día de hoy'}`
  : 'Todas las respuestas recibidas')

// Gráfico de cada pregunta: barras horizontales con el porcentaje de cada respuesta
const graficoVariable = (v) => barras(v.frecuencias, { horizontal: true, porcentaje: true })
const altoVariable = (v) => Math.max(140, v.frecuencias.length * 34 + 50)
const extra = { encuesta, variables, formato: 'excel' }

const PERFIL = ['perfil_conoce_perfil', 'perfil_coherente_desempeno', 'perfil_titulo_consistente', 'perfil_competencias_laborales', 'perfil_valores', 'perfil_desempeno_destacado', 'perfil_confianza']
const RELACION = ['perfil_vinculos_oportunidades', 'perfil_recurre_umss', 'perfil_opinion_practicas', 'perfil_consulta_autoridades', 'perfil_consultado_desempeno', 'perfil_consultada_pertinencia', 'perfil_consultado_autoevaluacion', 'perfil_participo_retroalimentacion', 'perfil_participo_vinculacion_entorno', 'perfil_participo_difusion', 'perfil_participo_vinculacion_profesionales']
const items = (codigos) => codigos.map((c) => empl.value.perfil.find((p) => p.codigo === c)).filter(Boolean)

// Un gráfico clave por criterio: el que mejor resume su evidencia
function grafico(c) {
  switch (c.pantalla) {
    case 'empleo': return { titulo: 'Tiempo hasta el primer empleo (titulados)', opcion: barras(emp.value.tiempoPrimerEmpleo.frecuencias), alto: 240 }
    case 'resultados': return { titulo: 'Lo que piden mejorar (% de titulados)', opcion: barras(comp.value.aspectosMejorar, { horizontal: true, porcentaje: true }), alto: 260 }
    case 'perfil': return { titulo: 'Acuerdo de los empleadores con el perfil profesional (%)', opcion: radarPerfil(items(PERFIL)), alto: 360 }
    case 'curricular': return { titulo: 'Asignaturas que dieron ventaja en el trabajo (% de titulados)', opcion: barras(comp.value.asignaturasVentaja, { horizontal: true, porcentaje: true }), alto: 320 }
    case 'posgrado': return { titulo: 'Área en la que harían un posgrado (% de interesados)', opcion: barras(form.value.areaInteres, { horizontal: true, porcentaje: true }), alto: 220 }
    case 'seguimiento': return {
      titulo: 'Respuestas recibidas por mes',
      opcion: barrasAgrupadas(c.respuestasPorMes.map((m) => m.mes), [
        { nombre: 'Titulados', valores: c.respuestasPorMes.map((m) => m.titulados) },
        { nombre: 'Empleadores', valores: c.respuestasPorMes.map((m) => m.empleadores) },
      ]),
      alto: 240,
    }
    case 'relaciones': return { titulo: 'Vinculación de la Carrera con las organizaciones (% de acuerdo)', opcion: radarPerfil(items(RELACION)), alto: 420 }
    default: return null
  }
}

const conclusion = (x) => (x.independientes == null
  ? 'No hay respuestas suficientes para comparar.'
  : x.independientes
    ? `No hay evidencia de que «${x.variableFilas}» y «${x.variableColumnas}» estén relacionadas.`
    : `«${x.variableFilas}» y «${x.variableColumnas}» están relacionadas.`)
</script>

<template>
  <div class="hoja">
    <div class="acciones no-imprimir">
      <span class="nota" style="margin: 0">El reporte se calcula con los datos actuales de la base. Para guardarlo en PDF, elige «Guardar como PDF» al imprimir.</span>
      <n-space>
        <n-button tag="a" :href="urlExportar('reporte-datos', filtro, extra)" secondary>Respuestas (Excel)</n-button>
        <n-button tag="a" :href="urlExportar('reporte', filtro, extra)" secondary>Tablas (Excel)</n-button>
        <n-button type="primary" :disabled="!listo" @click="imprimir">Imprimir / Guardar PDF</n-button>
      </n-space>
    </div>

    <n-alert v-if="error" type="error" :bordered="false">{{ error }}</n-alert>
    <div v-else-if="!listo" class="centro"><n-spin /></div>

    <template v-else>
      <header class="portada">
        <div class="institucion">Carrera de Ingeniería de Sistemas · Universidad Mayor de San Simón</div>
        <h1>{{ titulo }}</h1>
        <div class="meta">
          <span>Encuesta a {{ encuesta }}</span>
          <span>{{ periodo }}</span>
          <span>{{ reporte.n }} respuestas</span>
          <span>Generado el {{ generado }}</span>
        </div>
      </header>

      <n-alert v-if="reporte.n === 0" type="warning" :bordered="false">No hay respuestas en el período elegido.</n-alert>

      <section v-for="(v, k) in reporte.variables" :key="v.codigo" class="variable">
        <h2><span class="num">{{ k + 1 }}.</span> {{ v.texto }}</h2>
        <div class="nota" style="margin: 0 0 6px">Respondieron {{ v.n }} de {{ reporte.n }}<template v-if="v.nota"> · {{ v.nota }}</template></div>
        <div v-if="v.estadistica?.media != null" class="estadistica">
          Media <b>{{ v.estadistica.media.toFixed(2) }}</b>
          <template v-if="v.estadistica.limiteInferior != null"> · desviación estándar {{ v.estadistica.desviacion.toFixed(2) }} · con 95% de confianza, entre {{ v.estadistica.limiteInferior.toFixed(2) }} y {{ v.estadistica.limiteSuperior.toFixed(2) }}</template>
        </div>
        <div class="variable-cuerpo" :class="{ solo: !conGraficos || !v.frecuencias.length }">
          <table class="tabla">
            <thead><tr><th>Respuesta</th><th style="width: 70px">Cantidad</th><th style="width: 70px">%</th></tr></thead>
            <tbody>
              <tr v-for="f in v.frecuencias" :key="f.codigo"><td>{{ f.etiqueta }}</td><td>{{ f.frecuencia }}</td><td>{{ pct(f.relativa) }}</td></tr>
            </tbody>
          </table>
          <v-chart v-if="conGraficos && v.frecuencias.length" :style="{ height: altoVariable(v) + 'px' }" :option="paraImprimir(graficoVariable(v))" autoresize />
        </div>
      </section>

      <template v-if="conArcusur">
      <section class="criterio">
        <h2>Resumen por criterio ARCU-SUR</h2>
        <p class="nota">Un aspecto es favorable cuando más de la mitad de quienes respondieron lo confirma.</p>
        <table class="tabla">
          <thead><tr><th>Criterio</th><th>Indicador principal</th><th>Aspectos favorables</th></tr></thead>
          <tbody>
            <tr v-for="c in ordenados" :key="c.id">
              <td><b>{{ c.id }}</b> {{ c.nombre }}</td>
              <td>{{ c.indicadores[0]?.titulo }}: <b>{{ c.indicadores[0]?.valor }}</b></td>
              <td>{{ c.favorables.length + c.desfavorables.length ? `${c.favorables.length} de ${c.favorables.length + c.desfavorables.length}` : 'Sin valoración (insumo)' }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-for="c in ordenados" :key="c.id" class="criterio">
        <div class="etiqueta-criterio">Criterio ARCU-SUR {{ c.id }}</div>
        <h2>{{ c.nombre }}</h2>
        <p class="que-pide">{{ c.quePide }}</p>

        <div class="indicadores">
          <div v-for="i in c.indicadores" :key="i.titulo" class="indicador">
            <div class="nota" style="margin: 0">{{ i.titulo }}</div>
            <div class="valor">{{ i.valor }}</div>
            <div class="nota" style="margin: 0">{{ i.detalle }}</div>
          </div>
        </div>

        <table v-if="c.favorables.length + c.desfavorables.length" class="tabla">
          <thead><tr><th style="width: 40px"></th><th>Aspecto</th><th style="width: 90px">%</th></tr></thead>
          <tbody>
            <tr v-for="f in c.favorables" :key="f.texto"><td class="bien">✓</td><td>{{ f.texto }}</td><td>{{ pct(f.proporcion) }}</td></tr>
            <tr v-for="f in c.desfavorables" :key="f.texto"><td class="mal">✕</td><td>{{ f.texto }}</td><td>{{ pct(f.proporcion) }}</td></tr>
          </tbody>
        </table>
        <p v-else class="nota">{{ c.nota }}</p>

        <div v-if="grafico(c)" class="bloque-grafico">
          <div class="grafico-titulo">{{ grafico(c).titulo }}</div>
          <v-chart :style="{ height: grafico(c).alto + 'px' }" :option="paraImprimir(grafico(c).opcion)" autoresize />
        </div>

        <template v-if="c.cruces.length">
          <div class="grafico-titulo">Relaciones entre preguntas</div>
          <ul class="relaciones">
            <li v-for="x in cruces.filter((y) => c.cruces.includes(y.id))" :key="x.id">
              <b>{{ x.titulo.replace(' (opcional)', '') }}.</b> {{ conclusion(x) }}
            </li>
          </ul>
        </template>
      </section>
      </template>

      <footer class="pie nota">
        Fuente: encuestas a titulados (Microsoft Forms y formulario propio) y a empleadores (Google Forms), procesadas con el sistema de la Actividad 2.
        Los datos personales no forman parte del análisis.
      </footer>
    </template>
  </div>
</template>

<style scoped>
.hoja { max-width: 900px; margin: 0 auto; padding: 24px 32px 60px; background: #fff; min-height: 100vh; color: #18181b; }
.acciones { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 12px 16px; margin-bottom: 24px; background: #f4f4f5; border-radius: 10px; }
.centro { padding: 80px; display: grid; place-items: center; }
.portada { border-bottom: 2px solid #18181b; padding-bottom: 14px; margin-bottom: 20px; }
.institucion { font-size: 12.5px; color: #52525b; letter-spacing: 0.02em; }
h1 { font-size: 24px; margin: 6px 0 10px; letter-spacing: -0.01em; }
h2 { font-size: 18px; margin: 2px 0 6px; }
.meta { display: flex; flex-wrap: wrap; gap: 6px 18px; font-size: 13px; color: #52525b; }
section { margin-bottom: 28px; }
.criterio { border-top: 1px solid #e4e4e7; padding-top: 18px; break-inside: avoid-page; }
.variable { border-top: 1px solid #e4e4e7; padding-top: 14px; break-inside: avoid; margin-bottom: 18px; }
.variable h2 { font-size: 15.5px; line-height: 1.4; }
.num { color: #a1a1aa; }
.estadistica { font-size: 13.5px; margin-bottom: 6px; }
.variable-cuerpo { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; align-items: start; }
.variable-cuerpo.solo { grid-template-columns: 1fr; }
.que-pide { color: #52525b; font-size: 14px; line-height: 1.5; margin: 0 0 12px; }
.indicadores { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 12px; }
.indicador { border: 1px solid #e4e4e7; border-radius: 8px; padding: 10px 12px; }
.valor { font-size: 20px; font-weight: 650; margin: 2px 0; }
.tabla { width: 100%; border-collapse: collapse; font-size: 13.5px; margin: 6px 0 12px; }
.tabla th, .tabla td { text-align: left; padding: 6px 8px; border-bottom: 1px solid #e4e4e7; vertical-align: top; }
.tabla th { font-weight: 600; color: #52525b; font-size: 12.5px; }
.bien { color: #15803d; font-weight: 700; }
.mal { color: #be123c; font-weight: 700; }
.bloque-grafico { break-inside: avoid; }
.grafico-titulo { font-weight: 600; font-size: 13.5px; margin: 10px 0 2px; }
.relaciones { margin: 4px 0 0; padding-left: 18px; font-size: 13.5px; line-height: 1.6; }
.pie { border-top: 1px solid #e4e4e7; padding-top: 10px; }
@media print {
  .no-imprimir { display: none !important; }
  .hoja { padding: 0; max-width: none; min-height: 0; }
  :global(body) { background: #fff; }
  @page { margin: 16mm 14mm; }
}
</style>
