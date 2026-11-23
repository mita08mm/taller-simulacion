<script setup>
import { ref, computed, watch, inject, h } from 'vue'
import { useDialog, NInput } from 'naive-ui'
import { obtener, enviar, TIPOS_PREGUNTA, conOpciones } from '../api'
import { useAccion, textoError } from '../useAccion'
import { VueDraggable } from 'vue-draggable-plus'
import { ARRASTRE, cambioOrden, moverUno } from '../orden'
import PreguntaPanel from '../components/PreguntaPanel.vue'
import AsaArrastre from '../components/AsaArrastre.vue'

// CRUD de la encuesta: secciones, preguntas y opciones guardadas en PostgreSQL.
// El orden de las preguntas se cambia arrastrándolas por su asa, como en Google Forms.
const dialogo = useDialog()
const { accion, mensaje } = useAccion()
const version = inject('version', ref(0))

const encuestas = ref([])
obtener('/api/admin/encuestas').then((l) => (encuestas.value = l)).catch((e) => mensaje.error(textoError(e)))
const codigo = ref('titulados')
const encuesta = ref(null)
const cargando = ref(false)

// Cada carga lleva un número: si se cambia de encuesta rápido, una respuesta atrasada no pisa a la actual
let ultimaCarga = 0
async function cargar() {
  const n = ++ultimaCarga
  cargando.value = true
  try {
    const e = await obtener(`/api/admin/encuestas/${codigo.value}`)
    if (n === ultimaCarga) encuesta.value = e
  } catch (e) {
    if (n === ultimaCarga) mensaje.error(textoError(e))
  } finally {
    if (n === ultimaCarga) cargando.value = false
  }
}
// Toda acción exitosa sube la versión (useAccion), así que el editor se recarga solo después de cada cambio
watch([codigo, version], cargar, { immediate: true })
watch(codigo, () => { encuesta.value = null; panel.value.visible = false })

const secciones = computed(() => encuesta.value?.secciones ?? [])
const etiquetaTipo = (t) => TIPOS_PREGUNTA.find((x) => x.value === t)?.label ?? t
const urlFormulario = computed(() => `${location.origin}${location.pathname}#/encuesta/${codigo.value}`)
const mayuscula = (s) => s.charAt(0).toUpperCase() + s.slice(1)

// ---------- Secciones ----------
function editarSeccion(s) {
  const titulo = ref(s?.titulo ?? '')
  dialogo.create({
    title: s ? 'Renombrar sección' : 'Nueva sección',
    content: () => h(NInput, { value: titulo.value, 'onUpdate:value': (v) => (titulo.value = v), placeholder: 'Título de la sección', autofocus: true }),
    positiveText: 'Guardar',
    negativeText: 'Cancelar',
    // Devolver false deja el diálogo abierto si el backend rechazó el cambio
    onPositiveClick: async () => (await accion(
      () => (s
        ? enviar('PUT', `/api/admin/secciones/${s.id}`, { titulo: titulo.value })
        : enviar('POST', '/api/admin/secciones', { encuesta: codigo.value, titulo: titulo.value })),
      { exito: s ? 'Sección actualizada' : 'Sección creada' },
    )) !== null,
  })
}
const borrarSeccion = (s) => accion(() => enviar('DELETE', `/api/admin/secciones/${s.id}`), { exito: 'Sección eliminada' })

// ---------- Orden de las preguntas (arrastrar) ----------
// La librería mueve la pregunta en pantalla (también a otra sección: todas comparten el grupo
// «preguntas») y al soltar se guarda el orden completo en una sola transacción. La pantalla ya
// muestra el orden nuevo, así que no se recarga la encuesta (eso hacía lento cada arrastre):
// solo se suma la versión que subió el backend. Si lo rechaza, se recarga el orden guardado.
async function guardarOrden() {
  const cuerpo = { secciones: secciones.value.map((s) => ({ id: s.id, preguntas: s.preguntas.map((p) => p.id) })) }
  const r = await accion(() => enviar('PUT', `/api/admin/encuestas/${codigo.value}/orden`, cuerpo), { avisar: false })
  if (r) encuesta.value.version++
  else cargar()
}
const alSoltar = (e) => cambioOrden(e) && guardarOrden()
// ↑/↓ con el asa enfocada: mueve un lugar dentro de la sección
function moverConTeclado(s, i, delta) {
  const nueva = moverUno(s.preguntas, i, delta)
  if (!nueva) return
  s.preguntas = nueva
  guardarOrden()
}

// ---------- Preguntas ----------
const panel = ref({ visible: false, preguntaId: null, seccionId: null })
const abrirPregunta = (p, s) => (panel.value = { visible: true, preguntaId: p?.id ?? null, seccionId: s?.id ?? p.seccionId })
const borrarPregunta = (p) => accion(() => enviar('DELETE', `/api/admin/preguntas/${p.id}`), { exito: 'Pregunta eliminada' })
const alternarActiva = (p) => accion(() => enviar('PUT', `/api/admin/preguntas/${p.id}`, { activa: !p.activa }),
  { exito: p.activa ? 'Pregunta desactivada' : 'Pregunta activada' })
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">Encuestas · CRUD de la encuesta</div>
    <h1 class="pagina-titulo">Editor de preguntas</h1>
    <p class="pagina-sub">
      Las preguntas se guardan en la base de datos y el formulario se arma a partir de ellas. Para cambiar el orden de una
      pregunta, arrástrela desde su asa (los seis puntos arriba de la tarjeta); también se puede soltar en otra sección. Una pregunta u opción
      que ya tiene respuestas no se borra: se desactiva, y no se puede cambiar su código ni su tipo, para no perder datos.
    </p>

    <n-card size="small">
      <div class="barra-acciones">
        <n-radio-group v-model:value="codigo" size="small">
          <n-radio-button v-for="e in encuestas" :key="e.codigo" :value="e.codigo" :title="e.nombre">{{ mayuscula(e.codigo) }}</n-radio-button>
        </n-radio-group>
        <n-tag v-if="encuesta" :bordered="false" round>Versión {{ encuesta.version }}</n-tag>
        <span class="espaciador" />
        <n-button size="small" tag="a" :href="urlFormulario" target="_blank" secondary>Ver formulario</n-button>
        <n-button size="small" type="primary" @click="editarSeccion(null)">Nueva sección</n-button>
      </div>
    </n-card>

    <div v-if="cargando && !encuesta" class="cargando"><n-spin /></div>

    <n-card v-for="(s, i) in secciones" :key="s.id" size="small" class="seccion">
      <template #header>
        <span class="num">{{ i + 1 }}</span> {{ s.titulo }}
        <span class="nota cantidad">{{ s.preguntas.length }} preguntas</span>
      </template>
      <template #header-extra>
        <n-space size="small">
          <n-button size="tiny" secondary @click="editarSeccion(s)">Renombrar</n-button>
          <n-popconfirm @positive-click="borrarSeccion(s)">
            <template #trigger><n-button size="tiny" quaternary type="error">Eliminar</n-button></template>
            Solo se puede eliminar una sección vacía.
          </n-popconfirm>
          <n-button size="tiny" type="primary" secondary @click="abrirPregunta(null, s)">+ Pregunta</n-button>
        </n-space>
      </template>

      <VueDraggable v-model="s.preguntas" v-bind="ARRASTRE" group="preguntas" class="lista-preguntas" @end="alSoltar">
        <div v-for="(p, j) in s.preguntas" :key="p.id" class="pregunta arrastrable" :class="{ inactiva: !p.activa }">
          <AsaArrastre @keydown.up.prevent="moverConTeclado(s, j, -1)" @keydown.down.prevent="moverConTeclado(s, j, 1)" />
          <div class="pregunta-cuerpo">
            <button type="button" class="contenido" @click="abrirPregunta(p)">
              <span class="texto">{{ p.texto }}</span>
              <n-space size="small" class="etiquetas">
                <n-tag size="tiny" :bordered="false">{{ p.codigo }}</n-tag>
                <n-tag size="tiny" :bordered="false" type="info">{{ etiquetaTipo(p.tipo) }}</n-tag>
                <n-tag v-if="p.obligatoria" size="tiny" :bordered="false" type="warning">obligatoria</n-tag>
                <n-tag v-if="p.dependeDe" size="tiny" :bordered="false">si {{ p.dependeDe }} = {{ p.dependeValores.join(' o ') || 'cualquiera' }}</n-tag>
                <n-tag v-if="p.esContacto" size="tiny" :bordered="false" type="error">dato personal</n-tag>
                <n-tag v-if="conOpciones(p.tipo)" size="tiny" :bordered="false">{{ p.opciones.length }} opciones</n-tag>
                <n-tag size="tiny" :bordered="false" type="success">{{ p.respuestas }} respuestas</n-tag>
                <n-tag v-if="!p.activa" size="tiny" :bordered="false">inactiva</n-tag>
              </n-space>
            </button>
            <n-space size="small" :wrap="false">
              <n-button size="tiny" secondary @click="abrirPregunta(p)">Editar</n-button>
              <n-button size="tiny" quaternary @click="alternarActiva(p)">{{ p.activa ? 'Desactivar' : 'Activar' }}</n-button>
              <n-popconfirm @positive-click="borrarPregunta(p)">
                <template #trigger><n-button size="tiny" quaternary type="error">Eliminar</n-button></template>
                {{ p.respuestas > 0 ? 'Tiene respuestas: se desactivará en vez de eliminarse.' : '¿Eliminar la pregunta y sus opciones?' }}
              </n-popconfirm>
            </n-space>
          </div>
        </div>
      </VueDraggable>
    </n-card>

    <PreguntaPanel
      v-model:show="panel.visible" :encuesta="encuesta" :codigo="codigo"
      :pregunta-id="panel.preguntaId" :seccion-id="panel.seccionId"
    />
  </div>
</template>

<style scoped>
.seccion { margin-top: 18px; border-top: 6px solid var(--acento); }
.num { display: inline-grid; place-items: center; width: 22px; height: 22px; border-radius: 6px; background: var(--acento-suave); color: var(--acento); font-size: 12px; font-weight: 700; margin-right: 6px; }
.cantidad { margin-left: 8px; }
/* Cada pregunta es una tarjeta, como en Google Forms: al pasar el mouse se marca con una franja a la izquierda */
.pregunta {
  background: var(--superficie); border: 1px solid var(--borde); border-radius: 10px; padding: 2px 16px 14px; margin-top: 10px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.pregunta:hover { border-color: var(--acento-borde); box-shadow: inset 4px 0 0 var(--acento), 0 1px 6px rgba(24, 24, 27, 0.06); }
.pregunta.inactiva .pregunta-cuerpo { opacity: 0.55; }
.pregunta-cuerpo { display: flex; align-items: center; gap: 10px; }
/* Botón sin aspecto de botón: toda la pregunta se abre con clic o con Enter */
.contenido { flex: 1; min-width: 0; text-align: left; background: none; border: 0; padding: 0; font: inherit; color: inherit; cursor: pointer; border-radius: 4px; }
.contenido:focus-visible { outline: 2px solid var(--acento); outline-offset: 2px; }
.texto { display: block; font-size: 14px; }
.etiquetas { margin-top: 4px; }
/* Una sección vacía sigue siendo un destino: la lista conserva alto y muestra el aviso (sin elementos extra
   dentro, porque la librería trata cada hijo como un elemento arrastrable) */
.lista-preguntas:empty { min-height: 56px; margin-top: 10px; border: 1px dashed var(--borde); border-radius: 10px; display: grid; place-items: center; }
.lista-preguntas:empty::before { content: 'Sección vacía: agregue una pregunta o arrastre aquí una de otra sección.'; color: var(--suave); font-size: 12.5px; }
</style>
