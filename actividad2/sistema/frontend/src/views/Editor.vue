<script setup>
import { ref, reactive, computed, watch, inject, h } from 'vue'
import { useMessage, useDialog, NTag, NButton, NSpace, NInput, NPopconfirm } from 'naive-ui'
import { obtener, enviar, TIPOS_PREGUNTA, conOpciones } from '../api'

// CRUD de la encuesta: secciones, preguntas y opciones guardadas en PostgreSQL
const mensaje = useMessage()
const dialogo = useDialog()
const alImportar = inject('alImportar')

const codigo = ref('titulados')
const encuesta = ref(null)
const cargando = ref(false)

async function cargar() {
  cargando.value = true
  try {
    encuesta.value = await obtener(`/api/admin/encuestas/${codigo.value}`)
  } catch (e) {
    mensaje.error(e.message)
  } finally {
    cargando.value = false
  }
}
watch(codigo, cargar, { immediate: true })

const preguntasTodas = computed(() => (encuesta.value?.secciones ?? []).flatMap((s) => s.preguntas))
const etiquetaTipo = (t) => TIPOS_PREGUNTA.find((x) => x.value === t)?.label ?? t
const urlFormulario = computed(() => `${location.origin}${location.pathname}#/encuesta/${codigo.value}`)

async function ejecutar(promesa, exito) {
  try {
    const r = await promesa
    if (r?.resultado === 'desactivada') mensaje.warning('Tenía respuestas: se desactivó en vez de eliminarse, para no perder datos.')
    else if (exito) mensaje.success(exito)
    await cargar()
    alImportar?.()
    return r
  } catch (e) {
    mensaje.error(e.message)
    throw e
  }
}

// ---------- Secciones ----------
function editarSeccion(s) {
  const titulo = ref(s?.titulo ?? '')
  dialogo.create({
    title: s ? 'Renombrar sección' : 'Nueva sección',
    content: () => h(NInput, { value: titulo.value, 'onUpdate:value': (v) => (titulo.value = v), placeholder: 'Título de la sección' }),
    positiveText: 'Guardar',
    negativeText: 'Cancelar',
    onPositiveClick: () => (s
      ? ejecutar(enviar('PUT', `/api/admin/secciones/${s.id}`, { titulo: titulo.value }), 'Sección actualizada')
      : ejecutar(enviar('POST', '/api/admin/secciones', { encuesta: codigo.value, titulo: titulo.value }), 'Sección creada')),
  })
}
const borrarSeccion = (s) => ejecutar(enviar('DELETE', `/api/admin/secciones/${s.id}`), 'Sección eliminada')
async function moverSeccion(i, delta) {
  const lista = encuesta.value.secciones
  const a = lista[i], b = lista[i + delta]
  await enviar('PUT', `/api/admin/secciones/${a.id}`, { orden: b.orden })
  await ejecutar(enviar('PUT', `/api/admin/secciones/${b.id}`, { orden: a.orden }))
}

// ---------- Preguntas ----------
const drawer = ref(false)
const form = reactive({})
const nuevaOpcion = reactive({ codigo: '', etiqueta: '' })
const opcionesForm = ref([])

function abrirPregunta(p, seccion) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, p
    ? { ...p, dependeValores: [...(p.dependeValores ?? [])] }
    : { id: null, seccionId: seccion.id, codigo: '', texto: '', ayuda: '', tipo: 'unica', obligatoria: true, dependeDe: null,
        dependeValores: [], maxSelecciones: null, valorMinimo: null, valorMaximo: null, esContacto: false, activa: true, respuestas: 0 })
  opcionesForm.value = (p?.opciones ?? []).map((o) => ({ ...o }))
  nuevaOpcion.codigo = ''
  nuevaOpcion.etiqueta = ''
  drawer.value = true
}

const opcionesDependencia = computed(() =>
  preguntasTodas.value.filter((p) => p.codigo !== form.codigo && conOpciones(p.tipo)).map((p) => ({ label: `${p.texto.slice(0, 70)} (${p.codigo})`, value: p.codigo })))
const valoresDependencia = computed(() =>
  (preguntasTodas.value.find((p) => p.codigo === form.dependeDe)?.opciones ?? []).map((o) => ({ label: o.etiqueta, value: o.codigo })))
const seccionesOpciones = computed(() => (encuesta.value?.secciones ?? []).map((s) => ({ label: s.titulo, value: s.id })))

async function guardarPregunta() {
  const cuerpo = {
    encuesta: codigo.value, seccionId: form.seccionId, codigo: form.codigo, texto: form.texto, ayuda: form.ayuda, tipo: form.tipo,
    obligatoria: form.obligatoria, dependeDe: form.dependeDe || null, dependeValores: form.dependeDe ? form.dependeValores : [],
    maxSelecciones: form.tipo === 'multiple' ? form.maxSelecciones : null,
    valorMinimo: form.tipo === 'numero' ? form.valorMinimo : null, valorMaximo: form.tipo === 'numero' ? form.valorMaximo : null,
    esContacto: form.esContacto, activa: form.activa,
  }
  if (form.id) {
    await ejecutar(enviar('PUT', `/api/admin/preguntas/${form.id}`, cuerpo), 'Pregunta actualizada')
  } else {
    const r = await ejecutar(enviar('POST', '/api/admin/preguntas', cuerpo), 'Pregunta creada')
    form.id = r.id
    if (!conOpciones(form.tipo)) drawer.value = false
    return
  }
  drawer.value = false
}
const borrarPregunta = (p) => ejecutar(enviar('DELETE', `/api/admin/preguntas/${p.id}`), 'Pregunta eliminada')
const alternarActiva = (p) => ejecutar(enviar('PUT', `/api/admin/preguntas/${p.id}`, { activa: !p.activa }), p.activa ? 'Pregunta desactivada' : 'Pregunta activada')
async function moverPregunta(s, i, delta) {
  const a = s.preguntas[i], b = s.preguntas[i + delta]
  await enviar('PUT', `/api/admin/preguntas/${a.id}`, { orden: b.orden })
  await ejecutar(enviar('PUT', `/api/admin/preguntas/${b.id}`, { orden: a.orden }))
}

// ---------- Opciones (dentro del panel de la pregunta) ----------
async function recargarOpciones() {
  await cargar()
  const p = preguntasTodas.value.find((x) => x.id === form.id)
  opcionesForm.value = (p?.opciones ?? []).map((o) => ({ ...o }))
}
async function agregarOpcion() {
  if (!form.id) return mensaje.warning('Guarde primero la pregunta')
  try {
    await enviar('POST', '/api/admin/opciones', { preguntaId: form.id, codigo: nuevaOpcion.codigo, etiqueta: nuevaOpcion.etiqueta })
    nuevaOpcion.codigo = ''
    nuevaOpcion.etiqueta = ''
    await recargarOpciones()
  } catch (e) { mensaje.error(e.message) }
}
async function guardarOpcion(o) {
  try {
    await enviar('PUT', `/api/admin/opciones/${o.id}`, { codigo: o.codigo, etiqueta: o.etiqueta })
    mensaje.success('Opción guardada')
    await recargarOpciones()
  } catch (e) { mensaje.error(e.message) }
}
async function borrarOpcion(o) {
  try {
    const r = await enviar('DELETE', `/api/admin/opciones/${o.id}`)
    if (r.resultado === 'desactivada') mensaje.warning('La opción ya fue elegida en respuestas: se desactivó en vez de eliminarse.')
    await recargarOpciones()
  } catch (e) { mensaje.error(e.message) }
}
async function moverOpcion(i, delta) {
  const a = opcionesForm.value[i], b = opcionesForm.value[i + delta]
  try {
    await enviar('PUT', `/api/admin/opciones/${a.id}`, { orden: b.orden })
    await enviar('PUT', `/api/admin/opciones/${b.id}`, { orden: a.orden })
    await recargarOpciones()
  } catch (e) { mensaje.error(e.message) }
}
const columnasOpciones = [
  { title: 'Código', key: 'codigo', width: 150, render: (o) => h(NInput, { size: 'small', value: o.codigo, disabled: o.respuestas > 0, 'onUpdate:value': (v) => (o.codigo = v) }) },
  { title: 'Texto que ve quien responde', key: 'etiqueta', render: (o) => h(NInput, { size: 'small', value: o.etiqueta, 'onUpdate:value': (v) => (o.etiqueta = v) }) },
  { title: 'Resp.', key: 'respuestas', width: 60 },
  {
    title: '', key: 'acciones', width: 190,
    render: (o, i) => h(NSpace, { size: 4, wrap: false }, () => [
      h(NButton, { size: 'tiny', quaternary: true, disabled: i === 0, onClick: () => moverOpcion(i, -1) }, () => '↑'),
      h(NButton, { size: 'tiny', quaternary: true, disabled: i === opcionesForm.value.length - 1, onClick: () => moverOpcion(i, 1) }, () => '↓'),
      h(NButton, { size: 'tiny', secondary: true, onClick: () => guardarOpcion(o) }, () => 'Guardar'),
      h(NPopconfirm, { onPositiveClick: () => borrarOpcion(o) }, {
        trigger: () => h(NButton, { size: 'tiny', quaternary: true, type: 'error' }, () => 'Quitar'),
        default: () => (o.respuestas > 0 ? 'Tiene respuestas: se desactivará.' : '¿Eliminar la opción?'),
      }),
      o.activa ? null : h(NTag, { size: 'tiny', bordered: false }, () => 'inactiva'),
    ]),
  },
]
</script>

<template>
  <div class="pagina">
    <div class="etiqueta-criterio">Encuestas · CRUD de la encuesta</div>
    <h1 class="pagina-titulo">Editor de preguntas</h1>
    <p class="pagina-sub">
      Las preguntas se guardan en la base de datos y el formulario se arma a partir de ellas. Una pregunta u opción que ya tiene respuestas
      no se borra: se desactiva, y no se puede cambiar su código ni su tipo, para no perder datos.
    </p>

    <n-card size="small">
      <div class="barra">
        <n-radio-group v-model:value="codigo" size="small">
          <n-radio-button value="titulados">Titulados</n-radio-button>
          <n-radio-button value="empleadores">Empleadores</n-radio-button>
        </n-radio-group>
        <n-tag v-if="encuesta" :bordered="false" round>Versión {{ encuesta.version }}</n-tag>
        <span class="flex" />
        <n-button size="small" tag="a" :href="urlFormulario" target="_blank" secondary>Ver formulario</n-button>
        <n-button size="small" type="primary" @click="editarSeccion(null)">Nueva sección</n-button>
      </div>
    </n-card>

    <div v-if="cargando && !encuesta" style="padding: 40px; display: grid; place-items: center"><n-spin /></div>

    <n-card v-for="(s, i) in encuesta?.secciones ?? []" :key="s.id" size="small" style="margin-top: 14px">
      <template #header>
        <span class="num">{{ i + 1 }}</span> {{ s.titulo }}
        <span class="nota" style="margin-left: 8px">{{ s.preguntas.length }} preguntas</span>
      </template>
      <template #header-extra>
        <n-space size="small">
          <n-button size="tiny" quaternary :disabled="i === 0" @click="moverSeccion(i, -1)">↑</n-button>
          <n-button size="tiny" quaternary :disabled="i === encuesta.secciones.length - 1" @click="moverSeccion(i, 1)">↓</n-button>
          <n-button size="tiny" secondary @click="editarSeccion(s)">Renombrar</n-button>
          <n-popconfirm @positive-click="borrarSeccion(s)">
            <template #trigger><n-button size="tiny" quaternary type="error">Eliminar</n-button></template>
            Solo se puede eliminar una sección vacía.
          </n-popconfirm>
          <n-button size="tiny" type="primary" secondary @click="abrirPregunta(null, s)">+ Pregunta</n-button>
        </n-space>
      </template>

      <div v-for="(p, j) in s.preguntas" :key="p.id" class="fila" :class="{ inactiva: !p.activa }">
        <div class="orden">
          <n-button size="tiny" quaternary :disabled="j === 0" @click="moverPregunta(s, j, -1)">↑</n-button>
          <n-button size="tiny" quaternary :disabled="j === s.preguntas.length - 1" @click="moverPregunta(s, j, 1)">↓</n-button>
        </div>
        <div class="contenido" @click="abrirPregunta(p)">
          <div class="texto">{{ p.texto }}</div>
          <n-space size="small" style="margin-top: 4px">
            <n-tag size="tiny" :bordered="false">{{ p.codigo }}</n-tag>
            <n-tag size="tiny" :bordered="false" type="info">{{ etiquetaTipo(p.tipo) }}</n-tag>
            <n-tag v-if="p.obligatoria" size="tiny" :bordered="false" type="warning">obligatoria</n-tag>
            <n-tag v-if="p.dependeDe" size="tiny" :bordered="false">si {{ p.dependeDe }} = {{ p.dependeValores.join(' o ') }}</n-tag>
            <n-tag v-if="p.esContacto" size="tiny" :bordered="false" type="error">dato personal</n-tag>
            <n-tag v-if="conOpciones(p.tipo)" size="tiny" :bordered="false">{{ p.opciones.length }} opciones</n-tag>
            <n-tag size="tiny" :bordered="false" type="success">{{ p.respuestas }} respuestas</n-tag>
            <n-tag v-if="!p.activa" size="tiny" :bordered="false">inactiva</n-tag>
          </n-space>
        </div>
        <n-space size="small" :wrap="false">
          <n-button size="tiny" secondary @click="abrirPregunta(p)">Editar</n-button>
          <n-button size="tiny" quaternary @click="alternarActiva(p)">{{ p.activa ? 'Desactivar' : 'Activar' }}</n-button>
          <n-popconfirm @positive-click="borrarPregunta(p)">
            <template #trigger><n-button size="tiny" quaternary type="error">Eliminar</n-button></template>
            {{ p.respuestas > 0 ? 'Tiene respuestas: se desactivará en vez de eliminarse.' : '¿Eliminar la pregunta y sus opciones?' }}
          </n-popconfirm>
        </n-space>
      </div>
      <div v-if="!s.preguntas.length" class="nota">Sección vacía.</div>
    </n-card>

    <n-drawer v-model:show="drawer" :width="620" placement="right">
      <n-drawer-content :title="form.id ? 'Editar pregunta' : 'Nueva pregunta'" closable>
        <n-alert v-if="form.respuestas > 0" type="info" :bordered="false" style="margin-bottom: 14px">
          Esta pregunta tiene {{ form.respuestas }} respuestas: se puede cambiar el texto y las condiciones, pero no el código ni el tipo.
        </n-alert>
        <n-form label-placement="top" :show-feedback="false">
          <n-grid :cols="2" :x-gap="12" :y-gap="12">
            <n-gi :span="2"><n-form-item label="Texto de la pregunta"><n-input v-model:value="form.texto" type="textarea" :autosize="{ minRows: 2 }" /></n-form-item></n-gi>
            <n-gi><n-form-item label="Código (nombre para el análisis)"><n-input v-model:value="form.codigo" :disabled="form.respuestas > 0" placeholder="ej. anio_titulacion" /></n-form-item></n-gi>
            <n-gi><n-form-item label="Tipo"><n-select v-model:value="form.tipo" :options="TIPOS_PREGUNTA" :disabled="form.respuestas > 0" /></n-form-item></n-gi>
            <n-gi :span="2"><n-form-item label="Ayuda (opcional)"><n-input v-model:value="form.ayuda" /></n-form-item></n-gi>
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
              <n-gi><n-form-item label="Valor máximo"><n-input-number v-model:value="form.valorMaximo" clearable /></n-form-item></n-gi>
            </template>
            <n-gi :span="2">
              <n-form-item label="Mostrar solo si… (lógica condicional)">
                <n-select v-model:value="form.dependeDe" :options="opcionesDependencia" clearable filterable placeholder="Siempre visible" />
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
            <n-data-table size="small" :columns="columnasOpciones" :data="opcionesForm" :row-key="(o) => o.id" />
            <n-space style="margin-top: 10px" :wrap="false">
              <n-input v-model:value="nuevaOpcion.codigo" size="small" placeholder="Código (ej. 1_4_meses)" style="width: 170px" />
              <n-input v-model:value="nuevaOpcion.etiqueta" size="small" placeholder="Texto de la opción" />
              <n-button size="small" type="primary" secondary @click="agregarOpcion">Agregar</n-button>
            </n-space>
          </template>
        </template>

        <template #footer>
          <n-space>
            <n-button @click="drawer = false">Cerrar</n-button>
            <n-button type="primary" @click="guardarPregunta">{{ form.id ? 'Guardar cambios' : 'Crear pregunta' }}</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<style scoped>
.barra { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.flex { flex: 1; }
.num { display: inline-grid; place-items: center; width: 22px; height: 22px; border-radius: 6px; background: #eef2ff; color: #4f46e5; font-size: 12px; font-weight: 700; margin-right: 6px; }
.fila { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px solid #f4f4f5; }
.fila.inactiva { opacity: 0.55; }
.orden { display: flex; flex-direction: column; }
.contenido { flex: 1; cursor: pointer; min-width: 0; }
.texto { font-size: 14px; }
</style>
