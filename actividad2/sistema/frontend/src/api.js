// Cliente de la API del backend Java (sistema/backend).
export const API = import.meta.env.VITE_API_URL ?? 'http://localhost:7070'

export async function obtener(ruta, parametros = {}) {
  const url = new URL(API + ruta)
  Object.entries(parametros).forEach(([k, v]) => url.searchParams.set(k, v))
  const r = await fetch(url)
  if (!r.ok) throw new Error((await r.json().catch(() => ({}))).error ?? `Error ${r.status}`)
  return r.json()
}

export async function importarCsv(tipo, archivo) {
  const url = new URL(API + '/api/importar')
  url.searchParams.set('tipo', tipo)
  url.searchParams.set('archivo', archivo.name)
  const r = await fetch(url, { method: 'POST', body: await archivo.text(), headers: { 'Content-Type': 'text/csv' } })
  const cuerpo = await r.json()
  if (!r.ok) throw new Error(cuerpo.error ?? `Error ${r.status}`)
  return cuerpo
}

// extra: { formato: 'excel' } para separador ';' y decimales con coma; o filas/columnas/agrupar del cruce libre
export const urlExportar = (que, filtro = {}, extra = {}) => {
  const url = new URL(API + '/api/exportar')
  url.searchParams.set('que', que)
  if (filtro.desde) url.searchParams.set('desde', filtro.desde)
  if (filtro.hasta) url.searchParams.set('hasta', filtro.hasta)
  Object.entries(extra).forEach(([k, v]) => url.searchParams.set(k, v))
  return url.toString()
}

export const pct = (v, dec = 1) => (v == null ? '—' : `${(v * 100).toFixed(dec)}%`)
export const num = (v, dec = 2) => (v == null ? '—' : Number(v).toFixed(dec))

/** POST / PUT / DELETE con cuerpo JSON. Si el backend responde con error, lanza un Error con .errores (por pregunta). */
export async function enviar(metodo, ruta, cuerpo, parametros = {}) {
  const url = new URL(API + ruta)
  Object.entries(parametros).forEach(([k, v]) => { if (v != null && v !== '') url.searchParams.set(k, v) })
  const r = await fetch(url, {
    method: metodo,
    headers: cuerpo === undefined ? {} : { 'Content-Type': 'application/json' },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  })
  const datos = await r.json().catch(() => ({}))
  if (!r.ok) {
    const e = new Error(datos.error ?? `Error ${r.status}`)
    e.errores = datos.errores ?? {}
    throw e
  }
  return datos
}

export const TIPOS_PREGUNTA = [
  { label: 'Opción única', value: 'unica' },
  { label: 'Elección múltiple', value: 'multiple' },
  { label: 'Escala (acuerdo, satisfacción…)', value: 'escala' },
  { label: 'Número', value: 'numero' },
  { label: 'Texto libre', value: 'texto' },
  { label: 'Correo electrónico', value: 'email' },
  { label: 'Teléfono', value: 'telefono' },
]
export const conOpciones = (tipo) => ['unica', 'multiple', 'escala'].includes(tipo)
