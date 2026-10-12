import { ref, watch, inject, reactive } from 'vue'
import { obtener } from './api'

/** Fechas del filtro como parámetros desde/hasta (aaaa-mm-dd) de la API. */
export function parametrosFiltro(filtro) {
  const p = {}
  if (filtro.desde) p.desde = filtro.desde
  if (filtro.hasta) p.hasta = filtro.hasta
  return p
}

/** Carga un endpoint y lo vuelve a pedir cuando cambian los datos importados. */
export function useApi(ruta, parametros = () => ({})) {
  const datos = ref(null)
  const error = ref(null)
  const cargando = ref(false)
  const version = inject('version', ref(0))
  const filtro = inject('filtro', reactive({ desde: null, hasta: null }))

  async function cargar() {
    cargando.value = true
    error.value = null
    try {
      datos.value = await obtener(ruta, { ...parametrosFiltro(filtro), ...parametros() })
    } catch (e) {
      error.value = e.message.includes('fetch')
        ? 'No se pudo conectar con el backend (¿está corriendo en el puerto 7070?).'
        : e.message
    } finally {
      cargando.value = false
    }
  }

  watch([version, () => filtro.desde, () => filtro.hasta], cargar, { immediate: true })
  return { datos, error, cargando, cargar }
}
