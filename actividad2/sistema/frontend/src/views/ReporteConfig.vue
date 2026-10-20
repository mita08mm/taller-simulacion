<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { obtener, urlExportar } from '../api'

// Constructor de reportes: el usuario elige encuesta, período y variables; el reporte se calcula al abrirlo
const encuesta = ref('titulados')
const modo = ref('todo')
const anio = ref(new Date().getFullYear())
const semestre = ref('I')
const mes = ref(null)
const rango = ref(null)
const secciones = ref([])
const elegidas = ref([])
const buscar = ref('')
const conGraficos = ref(true)
const conArcusur = ref(false)
const titulo = ref('')
const anios = ref([])
const error = ref(null)

const modos = [
  { value: 'todo', label: 'Todo' },
  { value: 'gestion', label: 'Gestión (año)' },
  { value: 'semestre', label: 'Semestre' },
  { value: 'mes', label: 'Mes' },
  { value: 'fechas', label: 'Fechas exactas' },
]
const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre']
const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

// Período elegido traducido a fechas desde/hasta y a un texto para la portada del reporte
const periodo = computed(() => {
  switch (modo.value) {
    case 'gestion': return { desde: `${anio.value}-01-01`, hasta: `${anio.value}-12-31`, texto: `Gestión ${anio.value}` }
    case 'semestre': return semestre.value === 'I'
      ? { desde: `${anio.value}-01-01`, hasta: `${anio.value}-06-30`, texto: `Semestre I-${anio.value} (enero a junio)` }
      : { desde: `${anio.value}-07-01`, hasta: `${anio.value}-12-31`, texto: `Semestre II-${anio.value} (julio a diciembre)` }
    case 'mes': {
      if (!mes.value) return null
      const d = new Date(mes.value)
      return { desde: iso(new Date(d.getFullYear(), d.getMonth(), 1)), hasta: iso(new Date(d.getFullYear(), d.getMonth() + 1, 0)), texto: `${MESES[d.getMonth()]} de ${d.getFullYear()}` }
    }
    case 'fechas': {
      if (!rango.value) return null
      const [a, b] = rango.value.map((x) => iso(new Date(x)))
      return { desde: a, hasta: b, texto: `Del ${a} al ${b}` }
    }
    default: return { desde: null, hasta: null, texto: 'Todas las respuestas recibidas' }
  }
})

async function cargarVariables() {
  error.value = null
  try {
    secciones.value = await obtener('/api/reporte/variables', { encuesta: encuesta.value })
    elegidas.value = []
  } catch (e) {
    error.value = e.message.includes('fetch') ? 'No se pudo conectar con el backend.' : e.message
  }
}
watch(encuesta, cargarVariables)
onMounted(async () => {
  cargarVariables()
  try {
    // años con respuestas, para elegir la gestión
    const e = await obtener('/api/estado')
    const fechas = [e.titulados.periodo, e.empleadores.periodo].flatMap((p) => [p.desde, p.hasta]).filter(Boolean).map((f) => +f.slice(0, 4))
    const min = Math.min(...fechas, new Date().getFullYear())
    for (let y = new Date().getFullYear(); y >= min; y--) anios.value.push(y)
  } catch { anios.value = [new Date().getFullYear()] }
})

const filtradas = computed(() => {
  const t = buscar.value.trim().toLowerCase()
  return secciones.value
    .map((s) => ({ ...s, preguntas: s.preguntas.filter((p) => !t || p.texto.toLowerCase().includes(t)) }))
    .filter((s) => s.preguntas.length)
})
const todasDeSeccion = (s) => s.preguntas.every((p) => elegidas.value.includes(p.codigo))
function alternarSeccion(s) {
  const codigos = s.preguntas.map((p) => p.codigo)
  elegidas.value = todasDeSeccion(s) ? elegidas.value.filter((c) => !codigos.includes(c)) : [...new Set([...elegidas.value, ...codigos])]
}
const todas = () => { elegidas.value = secciones.value.flatMap((s) => s.preguntas.map((p) => p.codigo)) }
const ninguna = () => { elegidas.value = [] }

// Se respeta el orden de la encuesta, no el orden en que se marcaron
const ordenadas = computed(() => secciones.value.flatMap((s) => s.preguntas.map((p) => p.codigo)).filter((c) => elegidas.value.includes(c)))
const listo = computed(() => ordenadas.value.length > 0 && periodo.value)
const filtro = computed(() => ({ desde: periodo.value?.desde, hasta: periodo.value?.hasta }))
const extra = (formato) => ({ encuesta: encuesta.value, variables: ordenadas.value.join(','), ...(formato ? { formato } : {}) })

function verReporte() {
  const q = new URLSearchParams({
    encuesta: encuesta.value,
    variables: ordenadas.value.join(','),
    periodo: periodo.value.texto,
    titulo: titulo.value.trim() || `Reporte de la encuesta a ${encuesta.value}`,
    graficos: conGraficos.value ? '1' : '0',
    arcusur: conArcusur.value ? '1' : '0',
  })
  if (periodo.value.desde) q.set('desde', periodo.value.desde)
  if (periodo.value.hasta) q.set('hasta', periodo.value.hasta)
  window.open(`${location.pathname}#/imprimir?${q}`, '_blank')
}
</script>

<template>
  <div class="pagina">
    <h1 class="pagina-titulo">Generar reporte</h1>
    <p class="pagina-sub">Elige la encuesta, el período y las preguntas que quieres incluir. El reporte se calcula con los datos actuales de la base y se puede imprimir, guardar en PDF o descargar en Excel.</p>
    <n-alert v-if="error" type="error" :bordered="false" style="margin-bottom: 14px">{{ error }}</n-alert>

    <div class="pasos">
      <div class="paso">
        <div class="paso-titulo"><span class="numero">1</span> Encuesta</div>
        <n-radio-group v-model:value="encuesta">
          <n-radio-button value="titulados" label="Titulados" />
          <n-radio-button value="empleadores" label="Empleadores" />
        </n-radio-group>
      </div>

      <div class="paso">
        <div class="paso-titulo"><span class="numero">2</span> Período</div>
        <n-radio-group v-model:value="modo" size="small">
          <n-radio-button v-for="m in modos" :key="m.value" :value="m.value" :label="m.label" />
        </n-radio-group>
        <div class="periodo-opciones">
          <n-select v-if="modo === 'gestion' || modo === 'semestre'" v-model:value="anio" :options="anios.map((y) => ({ label: String(y), value: y }))" style="width: 120px" />
          <n-radio-group v-if="modo === 'semestre'" v-model:value="semestre">
            <n-radio-button value="I" label="I (ene–jun)" />
            <n-radio-button value="II" label="II (jul–dic)" />
          </n-radio-group>
          <n-date-picker v-if="modo === 'mes'" v-model:value="mes" type="month" placeholder="Elige el mes" clearable />
          <n-date-picker v-if="modo === 'fechas'" v-model:value="rango" type="daterange" start-placeholder="Desde" end-placeholder="Hasta" clearable />
        </div>
        <div class="nota" style="margin-top: 8px">{{ periodo ? periodo.texto + (periodo.desde ? ` · ${periodo.desde} a ${periodo.hasta}` : '') : 'Completa el período' }}</div>
      </div>

      <div class="paso">
        <div class="paso-titulo">
          <span class="numero">3</span> Preguntas
          <span class="contador">{{ ordenadas.length }} elegidas</span>
        </div>
        <div class="herramientas-variables">
          <n-input v-model:value="buscar" placeholder="Buscar pregunta (ej. edad, área, cargo)" clearable style="max-width: 360px" />
          <n-button size="small" quaternary @click="todas">Todas</n-button>
          <n-button size="small" quaternary @click="ninguna">Ninguna</n-button>
        </div>
        <n-checkbox-group v-model:value="elegidas">
          <div v-for="s in filtradas" :key="s.titulo" class="seccion">
            <div class="seccion-cabecera">
              <span>{{ s.titulo }}</span>
              <n-button size="tiny" text type="primary" @click="alternarSeccion(s)">{{ todasDeSeccion(s) ? 'Quitar sección' : 'Toda la sección' }}</n-button>
            </div>
            <div class="preguntas">
              <n-checkbox v-for="p in s.preguntas" :key="p.codigo" :value="p.codigo" :label="p.texto" />
            </div>
          </div>
        </n-checkbox-group>
      </div>

      <div class="paso">
        <div class="paso-titulo"><span class="numero">4</span> Opciones</div>
        <n-input v-model:value="titulo" :placeholder="`Título (por defecto: Reporte de la encuesta a ${encuesta})`" style="max-width: 520px" />
        <n-space vertical style="margin-top: 10px">
          <n-checkbox v-model:checked="conGraficos">Incluir un gráfico por pregunta</n-checkbox>
          <n-checkbox v-model:checked="conArcusur">Incluir la valoración de los criterios ARCU-SUR del período</n-checkbox>
        </n-space>
      </div>
    </div>

    <div class="acciones">
      <span class="nota" style="margin: 0">{{ listo ? `${ordenadas.length} preguntas · ${periodo.texto}` : 'Elige al menos una pregunta' }}</span>
      <n-space>
        <n-button tag="a" secondary :disabled="!listo" :href="listo ? urlExportar('reporte-datos', filtro, extra('excel')) : undefined">Respuestas (Excel)</n-button>
        <n-button tag="a" secondary :disabled="!listo" :href="listo ? urlExportar('reporte', filtro, extra('excel')) : undefined">Tablas (Excel)</n-button>
        <n-button type="primary" :disabled="!listo" @click="verReporte">Ver reporte / PDF</n-button>
      </n-space>
    </div>
  </div>
</template>

<style scoped>
.pasos { display: grid; gap: 14px; }
.paso { background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; padding: 16px 18px; }
.paso-titulo { display: flex; align-items: center; gap: 10px; font-weight: 600; margin-bottom: 12px; }
.numero { width: 22px; height: 22px; border-radius: 50%; background: var(--acento-suave); color: var(--acento); display: grid; place-items: center; font-size: 12px; font-weight: 700; }
.contador { margin-left: auto; font-weight: 500; font-size: 13px; color: var(--acento); }
.periodo-opciones { display: flex; gap: 10px; align-items: center; margin-top: 10px; flex-wrap: wrap; }
.periodo-opciones:empty { display: none; }
.herramientas-variables { display: flex; gap: 8px; align-items: center; margin-bottom: 12px; }
.seccion { border-top: 1px solid #f1f1f3; padding: 10px 0; }
.seccion-cabecera { display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: 600; color: var(--suave); margin-bottom: 8px; }
.preguntas { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 18px; }
.acciones { position: sticky; bottom: 0; display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 14px; padding: 12px 18px; background: var(--superficie); border: 1px solid var(--borde); border-radius: 12px; box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.04); }
@media (max-width: 900px) { .preguntas { grid-template-columns: 1fr; } .acciones { flex-direction: column; align-items: stretch; } }
</style>
