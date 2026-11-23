<script setup>
import { ref, reactive, computed, watch, inject } from 'vue'
import { useDialog } from 'naive-ui'
import { enviar, TIPOS_PREGUNTA, conOpciones } from '../api'
import { useAccion } from '../useAccion'
import { VueDraggable } from 'vue-draggable-plus'
import { ARRASTRE, moverUno } from '../orden'
import AsaArrastre from './AsaArrastre.vue'

// Panel lateral para crear o editar una pregunta y sus opciones de respuesta
const props = defineProps({
  encuesta: { type: Object, default: null },
  codigo: { type: String, required: true },
  preguntaId: { type: Number, default: null },
  seccionId: { type: Number, default: null },
})
const visible = defineModel('show', { type: Boolean, default: false })
const dialogo = useDialog()
const { accion } = useAccion()
const recargar = inject('alCambiarDatos', () => {})

const PREGUNTA_NUEVA = {
  id: null, codigo: '', texto: '', ayuda: '', tipo: 'unica', obligatoria: true, dependeDe: null, dependeValores: [],
  maxSelecciones: null, valorMinimo: null, valorMaximo: null, esContacto: false, activa: true,
}
const CAMPOS = Object.keys(PREGUNTA_NUEVA)
const form = reactive({ ...PREGUNTA_NUEVA, seccionId: null })

const preguntas = computed(() => (props.encuesta?.secciones ?? []).flatMap((s) => s.preguntas))
// Versión guardada de la pregunta (se actualiza sola con cada recarga del editor)
const guardada = computed(() => preguntas.value.find((p) => p.id === form.id) ?? null)
const respuestas = computed(() => guardada.value?.respuestas ?? 0)
const opciones = computed(() => guardada.value?.opciones ?? [])

watch(visible, (abierto) => { if (abierto) reiniciar() })
function reiniciar() {
  const p = preguntas.value.find((x) => x.id === props.preguntaId)
  const base = p ? Object.fromEntries(CAMPOS.map((k) => [k, p[k]])) : PREGUNTA_NUEVA
  Object.assign(form, base, { dependeValores: [...(base.dependeValores ?? [])], seccionId: p?.seccionId ?? props.seccionId })
  Object.keys(borradores).forEach((k) => delete borradores[k])
  Object.assign(nueva, { codigo: '', etiqueta: '' })
}

// ---------- Lógica condicional ----------
// No se ofrecen: la misma pregunta, las desactivadas y las que dependen (directa o indirectamente)
// de esta, porque con un ciclo ninguna de las dos se mostraría nunca.
const dependientes = computed(() => {
  const r = new Set(guardada.value ? [guardada.value.codigo] : [])
  for (let crecio = r.size > 0; crecio;) {
    crecio = false
    for (const p of preguntas.value) {
      if (p.dependeDe && r.has(p.dependeDe) && !r.has(p.codigo)) { r.add(p.codigo); crecio = true }
    }
  }
  return r
})
const opcionesDependencia = computed(() => preguntas.value
  .filter((p) => p.id !== form.id && conOpciones(p.tipo) && !dependientes.value.has(p.codigo) && (p.activa || p.codigo === form.dependeDe))
  .map((p) => ({ label: `${p.texto.slice(0, 70)} (${p.codigo})`, value: p.codigo })))
const valoresDependencia = computed(() =>
  (preguntas.value.find((p) => p.codigo === form.dependeDe)?.opciones ?? []).map((o) => ({ label: o.etiqueta, value: o.codigo })))
// Los valores elegidos son opciones de la pregunta anterior: al cambiar de pregunta ya no valen
function cambiarDependencia(v) {
  form.dependeDe = v ?? null
  form.dependeValores = []
}
const seccionesOpciones = computed(() => (props.encuesta?.secciones ?? []).map((s) => ({ label: s.titulo, value: s.id })))
const rangoInvalido = computed(() => form.tipo === 'numero' && form.valorMinimo != null && form.valorMaximo != null && form.valorMinimo > form.valorMaximo)

async function guardar() {
  const esNumero = form.tipo === 'numero'
  const cuerpo = {
    encuesta: props.codigo, seccionId: form.seccionId, codigo: form.codigo, texto: form.texto, ayuda: form.ayuda, tipo: form.tipo,
    obligatoria: form.obligatoria, dependeDe: form.dependeDe, dependeValores: form.dependeDe ? form.dependeValores : [],
    maxSelecciones: form.tipo === 'multiple' ? form.maxSelecciones : null,
    valorMinimo: esNumero ? form.valorMinimo : null, valorMaximo: esNumero ? form.valorMaximo : null,
    esContacto: form.esContacto, activa: form.activa,
  }
  const nuevaPregunta = !form.id
  const r = await accion(
    () => (nuevaPregunta ? enviar('POST', '/api/admin/preguntas', cuerpo) : enviar('PUT', `/api/admin/preguntas/${form.id}`, cuerpo)),
    { exito: nuevaPregunta ? 'Pregunta creada: ahora puede agregar sus opciones' : 'Pregunta actualizada' },
  )
  if (!r) return
  if (nuevaPregunta) {
    form.id = r.id
    if (conOpciones(form.tipo)) return // queda abierta para cargar las opciones
  }
  cerrar()
}

// ---------- Opciones ----------
// Lo que se escribe en una opción queda como borrador hasta presionar «Guardar» en esa fila:
// así las recargas (al guardar otra opción, ordenar, etc.) no borran lo que no se guardó.
const borradores = reactive({})
const valor = (o, campo) => borradores[o.id]?.[campo] ?? o[campo]
function escribir(o, campo, v) {
  borradores[o.id] = { codigo: o.codigo, etiqueta: o.etiqueta, ...borradores[o.id], [campo]: v }
}
const sinGuardar = (o) => borradores[o.id] && (borradores[o.id].codigo !== o.codigo || borradores[o.id].etiqueta !== o.etiqueta)
const hayBorradores = computed(() => opciones.value.some(sinGuardar))

const nueva = reactive({ codigo: '', etiqueta: '' })
async function agregarOpcion() {
  const r = await accion(() => enviar('POST', '/api/admin/opciones', { preguntaId: form.id, ...nueva }), { exito: 'Opción agregada' })
  if (r) Object.assign(nueva, { codigo: '', etiqueta: '' })
}
async function guardarOpcion(o) {
  if (await accion(() => enviar('PUT', `/api/admin/opciones/${o.id}`, borradores[o.id]), { exito: 'Opción guardada' })) delete borradores[o.id]
}
async function borrarOpcion(o) {
  if (await accion(() => enviar('DELETE', `/api/admin/opciones/${o.id}`), { exito: 'Opción eliminada' })) delete borradores[o.id]
}
const activarOpcion = (o) => accion(() => enviar('PUT', `/api/admin/opciones/${o.id}`, { activa: true }), { exito: 'Opción activada' })

// Orden de las opciones: se muestra al instante (la lista es la que tiene cargada el editor) y se
// guarda completo, sin recargar toda la encuesta (solo se suma la versión que subió el backend);
// si el backend lo rechaza, el editor recarga el orden guardado
async function ordenarOpciones(lista) {
  if (lista.every((o, i) => o.id === opciones.value[i]?.id)) return
  guardada.value.opciones = lista
  const ids = lista.map((o) => o.id)
  const r = await accion(() => enviar('PUT', `/api/admin/preguntas/${form.id}/opciones/orden`, { ids }), { avisar: false })
  if (r) props.encuesta.version++
  else recargar()
}
const moverConTeclado = (i, delta) => { const l = moverUno(opciones.value, i, delta); if (l) ordenarOpciones(l) }

function cerrar() {
  if (!hayBorradores.value) {
    visible.value = false
    return
  }
  dialogo.warning({
    title: 'Hay opciones sin guardar',
    content: '¿Cerrar el panel y descartar esos cambios?',
    positiveText: 'Descartar',
    negativeText: 'Seguir editando',
    onPositiveClick: () => { visible.value = false },
  })
}
</script>

<template>
  <n-drawer :show="visible" :width="640" placement="right" @update:show="(v) => (v ? (visible = true) : cerrar())">
    <n-drawer-content :title="form.id ? 'Editar pregunta' : 'Nueva pregunta'" closable>
      <n-alert v-if="respuestas > 0" type="info" :bordered="false" class="aviso">
        Esta pregunta tiene {{ respuestas }} respuestas: se puede cambiar el texto y las condiciones, pero no el código ni el tipo.
      </n-alert>
      <n-form label-placement="top" :show-feedback="false">
        <n-grid :cols="2" :x-gap="12" :y-gap="12">
          <n-gi :span="2"><n-form-item label="Texto de la pregunta"><n-input v-model:value="form.texto" type="textarea" :autosize="{ minRows: 2 }" placeholder="Lo que se le pregunta a quien responde" /></n-form-item></n-gi>
          <n-gi><n-form-item label="Código (nombre para el análisis)"><n-input v-model:value="form.codigo" :disabled="respuestas > 0" placeholder="ej. anio_titulacion" /></n-form-item></n-gi>
          <n-gi><n-form-item label="Tipo"><n-select v-model:value="form.tipo" :options="TIPOS_PREGUNTA" :disabled="respuestas > 0" /></n-form-item></n-gi>
          <n-gi :span="2"><n-form-item label="Ayuda (opcional)"><n-input v-model:value="form.ayuda" placeholder="Texto breve que aparece debajo de la pregunta" /></n-form-item></n-gi>
          <n-gi><n-form-item label="Sección"><n-select v-model:value="form.seccionId" :options="seccionesOpciones" /></n-form-item></n-gi>
          <n-gi>
            <n-form-item label="Opciones">
              <n-space vertical :size="6">
                <n-checkbox v-model:checked="form.obligatoria">Obligatoria</n-checkbox>
                <n-checkbox v-model:checked="form.esContacto">Dato personal (no entra al análisis)</n-checkbox>
                <n-checkbox v-model:checked="form.activa">Activa</n-checkbox>
              </n-space>
            </n-form-item>
          </n-gi>
          <n-gi v-if="form.tipo === 'multiple'"><n-form-item label="Máximo de opciones a elegir"><n-input-number v-model:value="form.maxSelecciones" :min="1" clearable /></n-form-item></n-gi>
          <template v-if="form.tipo === 'numero'">
            <n-gi><n-form-item label="Valor mínimo"><n-input-number v-model:value="form.valorMinimo" clearable /></n-form-item></n-gi>
            <n-gi>
              <n-form-item label="Valor máximo" :validation-status="rangoInvalido ? 'error' : undefined">
                <n-input-number v-model:value="form.valorMaximo" clearable />
              </n-form-item>
            </n-gi>
            <n-gi v-if="rangoInvalido" :span="2" class="error">El valor mínimo no puede ser mayor que el máximo.</n-gi>
          </template>
          <n-gi :span="2">
            <n-form-item label="Mostrar solo si… (lógica condicional)">
              <n-select :value="form.dependeDe" :options="opcionesDependencia" clearable filterable placeholder="Siempre visible" @update:value="cambiarDependencia" />
            </n-form-item>
          </n-gi>
          <n-gi v-if="form.dependeDe" :span="2">
            <n-form-item label="…la respuesta es">
              <n-select v-model:value="form.dependeValores" :options="valoresDependencia" multiple placeholder="Cualquier respuesta" />
            </n-form-item>
          </n-gi>
        </n-grid>
      </n-form>

      <template v-if="conOpciones(form.tipo)">
        <n-divider title-placement="left">Opciones de respuesta</n-divider>
        <div v-if="!form.id" class="nota">Guarde la pregunta para agregar sus opciones.</div>
        <template v-else>
          <div class="opciones-cabecera nota">
            <span class="col-asa" /><span class="col-codigo">Código</span><span class="col-etiqueta">Texto que ve quien responde</span><span class="col-resp">Resp.</span>
          </div>
          <VueDraggable :model-value="opciones" v-bind="ARRASTRE" @update:model-value="ordenarOpciones">
            <div v-for="(o, i) in opciones" :key="o.id" class="opcion arrastrable" :class="{ inactiva: !o.activa }">
              <AsaArrastre vertical @keydown.up.prevent="moverConTeclado(i, -1)" @keydown.down.prevent="moverConTeclado(i, 1)" />
              <n-input class="col-codigo" size="small" :value="valor(o, 'codigo')" :disabled="o.respuestas > 0" placeholder="Código" @update:value="(v) => escribir(o, 'codigo', v)" />
              <n-input class="col-etiqueta" size="small" :value="valor(o, 'etiqueta')" placeholder="Texto de la opción" @update:value="(v) => escribir(o, 'etiqueta', v)" />
              <span class="col-resp">{{ o.respuestas }}</span>
              <n-space :size="4" :wrap="false">
                <n-button v-if="sinGuardar(o)" size="tiny" type="primary" secondary @click="guardarOpcion(o)">Guardar</n-button>
                <n-button v-if="!o.activa" size="tiny" secondary @click="activarOpcion(o)">Activar</n-button>
                <n-popconfirm v-if="o.activa" @positive-click="borrarOpcion(o)">
                  <template #trigger><n-button size="tiny" quaternary type="error">Quitar</n-button></template>
                  {{ o.respuestas > 0 ? 'Tiene respuestas: se desactivará.' : '¿Eliminar la opción?' }}
                </n-popconfirm>
              </n-space>
            </div>
          </VueDraggable>
          <div v-if="!opciones.length" class="nota">Todavía no tiene opciones.</div>
          <div class="opcion nueva">
            <span class="col-asa" />
            <n-input v-model:value="nueva.codigo" class="col-codigo" size="small" placeholder="Código (ej. 1_4_meses)" />
            <n-input v-model:value="nueva.etiqueta" class="col-etiqueta" size="small" placeholder="Texto de la opción" @keyup.enter="agregarOpcion" />
            <n-button size="small" type="primary" secondary @click="agregarOpcion">Agregar</n-button>
          </div>
        </template>
      </template>

      <template #footer>
        <n-space>
          <n-button @click="cerrar">Cerrar</n-button>
          <n-button type="primary" :disabled="rangoInvalido" @click="guardar">{{ form.id ? 'Guardar cambios' : 'Crear pregunta' }}</n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
.aviso { margin-bottom: 14px; }
.error { color: var(--mal); font-size: 12.5px; }
.opciones-cabecera, .opcion { display: flex; align-items: center; gap: 8px; }
.opciones-cabecera { margin: 0 0 4px; }
.opcion { padding: 6px 0; border-bottom: 1px solid var(--borde-suave); border-radius: 4px; }
.opcion.inactiva .col-etiqueta, .opcion.inactiva .col-codigo { opacity: 0.55; }
.opcion.nueva { border-bottom: 0; margin-top: 8px; }
.col-asa { width: 22px; flex: none; }
.col-codigo { width: 160px; flex: none; }
.col-etiqueta { flex: 1; min-width: 0; }
.col-resp { width: 40px; flex: none; text-align: right; font-size: 12.5px; color: var(--suave); }
</style>
