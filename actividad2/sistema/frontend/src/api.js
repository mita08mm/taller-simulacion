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

export async function restablecer() {
  const r = await fetch(API + '/api/restablecer', { method: 'POST' })
  return r.json()
}

export const urlExportar = (que, filtro = {}) => {
  const url = new URL(API + '/api/exportar')
  url.searchParams.set('que', que)
  if (filtro.desde) url.searchParams.set('desde', filtro.desde)
  if (filtro.hasta) url.searchParams.set('hasta', filtro.hasta)
  return url.toString()
}

export const pct = (v, dec = 1) => (v == null ? '—' : `${(v * 100).toFixed(dec)}%`)
export const num = (v, dec = 2) => (v == null ? '—' : Number(v).toFixed(dec))
