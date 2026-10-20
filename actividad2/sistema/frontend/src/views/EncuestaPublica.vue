<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { obtener, enviar } from '../api'

// Formulario que se arma a partir de la estructura guardada en la base (GET /api/encuestas/{codigo})
const props = defineProps({ codigo: String })

const encuesta = ref(null)
const errorCarga = ref(null)
const valores = reactive({})
const errores = reactive({})
const paso = ref(0)
const enviando = ref(false)
const enviado = ref(null)
const errorEnvio = ref(null)

onMounted(async () => {
  try {
    encuesta.value = await obtener(`/api/encuestas/${props.codigo}`)
    for (const s of encuesta.value.secciones) {
      for (const p of s.preguntas) valores[p.codigo] = p.tipo === 'multiple' ? [] : null
    }
  } catch (e) {
    errorCarga.value = e.message.includes('fetch') ? 'No se pudo conectar con el servidor.' : e.message
  }
})

// Misma regla que el backend: una pregunta se muestra si la pregunta de la que depende tiene uno de los valores que la habilitan
function visible(p) {
  if (!p.dependeDe) return true
  const v = valores[p.dependeDe]
  if (v == null || v === '' || (Array.isArray(v) && v.length === 0)) return false
  if (!p.dependeValores?.length) return true
  return Array.isArray(v) ? v.some((x) => p.dependeValores.includes(x)) : p.dependeValores.includes(String(v))
}
const secciones = computed(() =>
  (encuesta.value?.secciones ?? [])
    .map((s) => ({ ...s, visibles: s.preguntas.filter(visible) }))
    .filter((s) => s.visibles.length > 0))
const seccion = computed(() => secciones.value[paso.value])
const ultimo = computed(() => paso.value === secciones.value.length - 1)
// Si una respuesta oculta secciones posteriores, el paso actual no puede quedar fuera de rango
watch(() => secciones.value.length, (n) => { if (paso.value > n - 1) paso.value = Math.max(0, n - 1) })
const progreso = computed(() => (secciones.value.length ? Math.round(((paso.value + 1) / secciones.value.length) * 100) : 0))

const vacio = (v) => v == null || v === '' || (Array.isArray(v) && v.length === 0)

function validarPregunta(p) {
  const v = valores[p.codigo]
  delete errores[p.codigo]
  if (vacio(v)) {
    if (p.obligatoria) errores[p.codigo] = 'Esta pregunta es obligatoria'
    return
  }
  if (p.tipo === 'numero') {
    if (p.valorMinimo != null && v < p.valorMinimo) errores[p.codigo] = `El valor mínimo es ${p.valorMinimo}`
    if (p.valorMaximo != null && v > p.valorMaximo) errores[p.codigo] = `El valor máximo es ${p.valorMaximo}`
  }
  if (p.tipo === 'email' && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v)) errores[p.codigo] = 'Correo no válido'
}

function validarSeccion(s) {
  s.visibles.forEach(validarPregunta)
  return s.visibles.every((p) => !errores[p.codigo])
}

function siguiente() {
  if (!validarSeccion(seccion.value)) return
  paso.value++
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function anterior() {
  paso.value--
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function enviarRespuesta() {
  if (!validarSeccion(seccion.value)) return
  // Solo se envían las respuestas de preguntas visibles
  const cuerpo = {}
  for (const s of secciones.value) {
    for (const p of s.visibles) if (!vacio(valores[p.codigo])) cuerpo[p.codigo] = valores[p.codigo]
  }
  enviando.value = true
  errorEnvio.value = null
  try {
    enviado.value = await enviar('POST', `/api/respuestas/${props.codigo}`, cuerpo)
  } catch (e) {
    Object.assign(errores, e.errores)
    errorEnvio.value = e.message
    const conError = secciones.value.findIndex((s) => s.visibles.some((p) => errores[p.codigo]))
    if (conError >= 0) paso.value = conError
  } finally {
    enviando.value = false
  }
}

const maximoAlcanzado = (p, codigo) => p.maxSelecciones && valores[p.codigo].length >= p.maxSelecciones && !valores[p.codigo].includes(codigo)
</script>

<template>
  <div class="fondo">
    <div class="contenedor">
      <div class="cabecera">
        <div class="logo">IS</div>
        <div>
          <div class="institucion">Carrera de Ingeniería de Sistemas · UMSS</div>
          <div class="sub">Facultad de Ciencias y Tecnología</div>
        </div>
      </div>

      <n-alert v-if="errorCarga" type="error" :bordered="false">{{ errorCarga }}</n-alert>
      <div v-else-if="!encuesta" class="centro"><n-spin /></div>

      <n-card v-else-if="enviado" class="tarjeta">
        <n-result status="success" title="¡Gracias por participar!" :description="enviado.mensaje">
          <template #footer>
            <div class="nota">Su respuesta ayuda a la Carrera en su proceso de autoevaluación para la acreditación ARCU-SUR.</div>
          </template>
        </n-result>
      </n-card>

      <template v-else>
        <n-card class="tarjeta" v-if="paso === 0">
          <h1 class="titulo">{{ encuesta.nombre }}</h1>
          <p class="intro">{{ encuesta.introduccion }}</p>
        </n-card>

        <n-card class="tarjeta">
          <div class="progreso-fila">
            <span class="paso">Sección {{ paso + 1 }} de {{ secciones.length }}</span>
            <span class="paso">{{ progreso }}%</span>
          </div>
          <n-progress type="line" :percentage="progreso" :show-indicator="false" :height="6" style="margin: 6px 0 18px" />
          <h2 class="seccion">{{ seccion.titulo }}</h2>

          <div v-for="p in seccion.visibles" :key="p.codigo" class="pregunta" :class="{ conerror: errores[p.codigo] }">
            <div class="enunciado">{{ p.texto }}<span v-if="p.obligatoria" class="obligatoria"> *</span></div>
            <div v-if="p.ayuda" class="ayuda">{{ p.ayuda }}</div>
            <div v-if="p.tipo === 'multiple' && p.maxSelecciones" class="ayuda">Elija hasta {{ p.maxSelecciones }} opciones.</div>

            <n-radio-group v-if="p.tipo === 'unica'" v-model:value="valores[p.codigo]" @update:value="validarPregunta(p)">
              <n-space vertical :size="10">
                <n-radio v-for="o in p.opciones" :key="o.codigo" :value="o.codigo" :label="o.etiqueta" />
              </n-space>
            </n-radio-group>

            <n-radio-group v-else-if="p.tipo === 'escala'" v-model:value="valores[p.codigo]" @update:value="validarPregunta(p)" class="escala">
              <n-radio-button v-for="o in p.opciones" :key="o.codigo" :value="o.codigo" :label="o.etiqueta" />
            </n-radio-group>

            <n-checkbox-group v-else-if="p.tipo === 'multiple'" v-model:value="valores[p.codigo]" @update:value="validarPregunta(p)">
              <n-space vertical :size="10">
                <n-checkbox v-for="o in p.opciones" :key="o.codigo" :value="o.codigo" :label="o.etiqueta" :disabled="maximoAlcanzado(p, o.codigo)" />
              </n-space>
            </n-checkbox-group>

            <n-input-number v-else-if="p.tipo === 'numero'" v-model:value="valores[p.codigo]" :min="p.valorMinimo ?? undefined"
              :max="p.valorMaximo ?? undefined" placeholder="Escriba un número" style="max-width: 240px" @blur="validarPregunta(p)" />

            <n-input v-else-if="p.tipo === 'texto'" v-model:value="valores[p.codigo]" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }"
              maxlength="1000" placeholder="Su respuesta" @blur="validarPregunta(p)" />

            <n-input v-else v-model:value="valores[p.codigo]" :placeholder="p.tipo === 'email' ? 'nombre@correo.com' : 'Su respuesta'"
              @blur="validarPregunta(p)" />

            <div v-if="errores[p.codigo]" class="error">{{ errores[p.codigo] }}</div>
          </div>

          <n-alert v-if="errorEnvio" type="error" :bordered="false" style="margin-top: 12px">{{ errorEnvio }}</n-alert>

          <div class="botones">
            <n-button v-if="paso > 0" @click="anterior">Anterior</n-button>
            <span v-else />
            <n-button v-if="!ultimo" type="primary" @click="siguiente">Siguiente</n-button>
            <n-button v-else type="primary" :loading="enviando" @click="enviarRespuesta">Enviar respuesta</n-button>
          </div>
        </n-card>
      </template>
    </div>
  </div>
</template>

<style scoped>
.fondo { min-height: 100vh; background: #f4f4f5; padding: 24px 16px 60px; }
.contenedor { max-width: 720px; margin: 0 auto; }
.cabecera { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.logo { width: 40px; height: 40px; border-radius: 10px; background: #4f46e5; color: #fff; display: grid; place-items: center; font-weight: 700; }
.institucion { font-weight: 600; }
.sub { color: #71717a; font-size: 13px; }
.tarjeta { margin-bottom: 14px; border-radius: 14px; }
.titulo { font-size: 22px; margin: 0 0 8px; letter-spacing: -0.01em; }
.intro { color: #52525b; line-height: 1.6; margin: 0; font-size: 14.5px; }
.progreso-fila { display: flex; justify-content: space-between; }
.paso { color: #71717a; font-size: 12.5px; font-weight: 500; }
.seccion { font-size: 18px; margin: 0 0 10px; }
.pregunta { padding: 16px 0; border-top: 1px solid #f4f4f5; }
.pregunta.conerror .enunciado { color: #b91c1c; }
.enunciado { font-weight: 500; margin-bottom: 10px; line-height: 1.5; }
.obligatoria { color: #b91c1c; }
.ayuda { color: #71717a; font-size: 13px; margin: -4px 0 10px; }
.error { color: #b91c1c; font-size: 13px; margin-top: 8px; }
.escala { display: flex; flex-wrap: wrap; }
.botones { display: flex; justify-content: space-between; margin-top: 20px; }
.centro { padding: 60px; display: grid; place-items: center; }
.nota { color: #71717a; font-size: 13px; }
</style>
