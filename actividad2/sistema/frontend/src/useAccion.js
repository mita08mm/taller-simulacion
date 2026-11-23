import { inject, ref } from 'vue'
import { useMessage } from 'naive-ui'

/** Mensaje legible de un error: distingue «el backend no está corriendo» de un error que respondió la API. */
export const textoError = (e) =>
  /fetch/i.test(e?.message ?? '')
    ? 'No se pudo conectar con el backend (¿está corriendo en el puerto 7070?).'
    : e?.message || 'Ocurrió un error inesperado.'

/**
 * Operaciones que cambian datos (crear, editar, borrar, ordenar, importar), con el mismo trato en
 * todo el panel: mensaje de éxito o de error, aviso cuando el backend desactivó en vez de borrar
 * (porque había respuestas) y aviso a las demás pantallas para que recarguen.
 *
 *   const { accion, ocupado } = useAccion()
 *   const r = await accion(() => enviar('DELETE', ...), { exito: 'Eliminada' })
 *
 * Devuelve la respuesta del backend, o null si falló (el error ya se mostró).
 */
export function useAccion() {
  const mensaje = useMessage()
  const alCambiarDatos = inject('alCambiarDatos', () => {})
  const ocupado = ref(false)

  async function accion(operacion, { exito, avisar = true } = {}) {
    ocupado.value = true
    try {
      const r = (await operacion()) ?? {}
      if (r.resultado === 'desactivada') mensaje.warning('Tenía respuestas: se desactivó en vez de eliminarse, para no perder datos.')
      else if (exito) mensaje.success(typeof exito === 'function' ? exito(r) : exito)
      if (avisar) alCambiarDatos()
      return r
    } catch (e) {
      mensaje.error(textoError(e))
      return null
    } finally {
      ocupado.value = false
    }
  }

  return { accion, ocupado, mensaje }
}
