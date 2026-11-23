// Opciones de ECharts reutilizables: un solo color de acento y grises, para que los gráficos no compitan entre sí.
import { ACENTO } from './tema'
export { ACENTO }
export const COLORES = [ACENTO, '#a5b4fc', '#334155', '#94a3b8', '#c7d2fe', '#64748b', '#e0e7ff', '#cbd5e1', '#1e1b4b']
// Barras horizontales de "otras" listas (lo que piden mejorar, dónde estudiarían...): gris oscuro, para no competir con el acento
export const SECUNDARIO = COLORES[2]
// Origen de las respuestas (formulario propio / importadas de Forms) en los indicadores de Respuestas
export const COLOR_ORIGEN = { formulario: '#14b8a6', importado: '#0ea5e9' }
const TEXTO = '#71717a'
// Botón para descargar cada gráfico como imagen PNG (fondo blanco, doble resolución)
const descarga = {
  right: 0, top: 0, itemSize: 13,
  iconStyle: { borderColor: '#a1a1aa' },
  feature: { saveAsImage: { title: 'Descargar PNG', name: 'grafico', pixelRatio: 2, backgroundColor: '#fff' } },
}
const LINEA = '#e4e4e7'
// Formato de los números en los gráficos: coma decimal, un decimal y % (ej. 79,2%)
export const fmtPct = (v) => `${Number(v).toFixed(1).replace('.', ',')}%`
const fmtEje = (v) => `${v}%`

const ejeCategorias = (datos, rotar = 0) => ({
  type: 'category', data: datos,
  axisLine: { lineStyle: { color: LINEA } }, axisTick: { show: false },
  axisLabel: { color: TEXTO, fontSize: 11, rotate: rotar, interval: 0, width: 110, overflow: 'break' },
})
const ejeValores = (formato) => ({
  type: 'value', splitLine: { lineStyle: { color: LINEA, type: 'dashed' } },
  axisLabel: { color: TEXTO, fontSize: 11, formatter: formato },
})

/** Barras a partir de la lista [{etiqueta, frecuencia, relativa}] que devuelve el backend. */
export function barras(items, { horizontal = false, color = COLORES[0], porcentaje = false, ordenar = false, otrosHasta = 0, eje = '' } = {}) {
  // ordenar: de mayor a menor; otrosHasta: junta en «Otros» las categorías con esa frecuencia o menos
  let lista = items.filter((i) => i.frecuencia > 0 || !otrosHasta)
  if (otrosHasta) {
    const chicas = lista.filter((i) => i.frecuencia <= otrosHasta)
    if (chicas.length > 1) {
      lista = lista.filter((i) => i.frecuencia > otrosHasta)
      lista.push({ etiqueta: 'Otros', frecuencia: chicas.reduce((a, i) => a + i.frecuencia, 0), relativa: chicas.reduce((a, i) => a + i.relativa, 0), otros: true })
    }
  }
  if (ordenar) lista = [...lista].sort((a, b) => (a.otros ? 1 : b.otros ? -1 : b.frecuencia - a.frecuencia))
  const etiquetas = lista.map((i) => i.etiqueta)
  const valores = lista.map((i) => (porcentaje ? +(i.relativa * 100).toFixed(1) : i.frecuencia))
  const ejeVal = { ...ejeValores(porcentaje ? fmtEje : '{value}'), name: eje, nameLocation: 'middle', nameGap: horizontal ? 26 : 34, nameTextStyle: { color: TEXTO, fontSize: 11 } }
  return {
    toolbox: descarga,
    grid: { left: 8, right: horizontal ? 56 : 16, top: 22, bottom: eje ? 30 : 8, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => (porcentaje ? fmtPct(v) : v) },
    xAxis: horizontal ? ejeVal : ejeCategorias(etiquetas, etiquetas.length > 5 ? 25 : 0),
    yAxis: horizontal ? { ...ejeCategorias(etiquetas), inverse: true, axisLabel: { color: TEXTO, fontSize: 11, width: 170, overflow: 'break' } } : ejeVal,
    series: [{
      type: 'bar', data: valores, barMaxWidth: 34,
      itemStyle: { color, borderRadius: horizontal ? [0, 6, 6, 0] : [6, 6, 0, 0] },
      label: { show: true, position: horizontal ? 'right' : 'top', color: TEXTO, fontSize: 11, formatter: (p) => (porcentaje ? fmtPct(p.value) : p.value) },
    }],
  }
}

export function dona(items) {
  return {
    toolbox: descarga,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { color: TEXTO, fontSize: 11 }, icon: 'circle' },
    color: COLORES,
    series: [{
      type: 'pie', radius: ['48%', '72%'], center: ['50%', '44%'], padAngle: 2,
      itemStyle: { borderRadius: 6 }, label: { show: false },
      data: items.filter((i) => i.frecuencia > 0).map((i) => ({ name: i.etiqueta, value: i.frecuencia })),
    }],
  }
}

/** Barras apiladas al 100% para una escala Likert. */
export function likertApilado(filas, { ordenarPor = null } = {}) {
  // filas: [{nombre, frecuencias:[{codigo, etiqueta, relativa}]}]; ordenarPor: código de la opción por la que ordenar de mayor a menor
  if (ordenarPor) filas = [...filas].sort((a, b) => (b.frecuencias.find((f) => f.codigo === ordenarPor)?.relativa ?? 0) - (a.frecuencias.find((f) => f.codigo === ordenarPor)?.relativa ?? 0))
  const niveles = filas[0]?.frecuencias.map((f) => f.etiqueta) ?? []
  const colores = niveles.length === 4 && niveles.includes('No sabe')
    ? ['#4f46e5', '#a5b4fc', '#fb7185', '#e4e4e7']
    : ['#fb7185', '#fecdd3', '#a5b4fc', '#4f46e5']
  const claros = ['#a5b4fc', '#fecdd3', '#e4e4e7']
  return {
    toolbox: descarga,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => fmtPct(v) },
    legend: { top: 0, left: 0, textStyle: { color: TEXTO, fontSize: 11 }, icon: 'roundRect', itemWidth: 12, itemHeight: 8 },
    grid: { left: 8, right: 16, top: 34, bottom: 30, containLabel: true },
    xAxis: { ...ejeValores(fmtEje), max: 100, name: '% de quienes respondieron', nameLocation: 'middle', nameGap: 24, nameTextStyle: { color: TEXTO, fontSize: 11 } },
    yAxis: { ...ejeCategorias(filas.map((f) => f.nombre)), inverse: true, axisLabel: { color: TEXTO, fontSize: 11.5, width: 240, overflow: 'break' } },
    series: niveles.map((nivel, k) => ({
      name: nivel, type: 'bar', stack: 'total', barMaxWidth: 26,
      itemStyle: { color: colores[k] },
      // el porcentaje escrito dentro de cada tramo, si hay espacio
      label: { show: true, formatter: (p) => (p.value >= 8 ? fmtPct(p.value) : ''), fontSize: 10.5, color: claros.includes(colores[k]) ? '#27272a' : '#fff' },
      data: filas.map((f) => +((f.frecuencias[k]?.relativa ?? 0) * 100).toFixed(1)),
    })),
  }
}

/** Mapa de calor de una tabla de contingencia (frecuencia observada). */
export function mapaCalor(cruce) {
  const datos = []
  cruce.observada.forEach((fila, i) => fila.forEach((v, j) => datos.push([j, i, v])))
  const max = Math.max(...datos.map((d) => d[2]), 1)
  return {
    toolbox: descarga,
    tooltip: {
      formatter: (p) => {
        const [j, i, v] = p.data
        return `${cruce.filas[i]} × ${cruce.columnas[j]}: <b>${v}</b> respuestas`
      },
    },
    grid: { left: 170, right: 24, top: 8, bottom: 70 },
    xAxis: { ...ejeCategorias(cruce.columnas), splitArea: { show: false } },
    yAxis: { ...ejeCategorias(cruce.filas), inverse: true },
    visualMap: { min: 0, max, show: false, inRange: { color: ['#eef2ff', '#4f46e5'] } },
    series: [{
      type: 'heatmap', data: datos,
      label: { show: true, color: '#18181b', fontSize: 12, fontWeight: 600 },
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
    }],
  }
}

// Parte un texto largo en líneas de hasta n caracteres (las etiquetas del radar no se cortan solas)
function partir(texto, n) {
  const lineas = ['']
  for (const palabra of texto.split(' ')) {
    if ((lineas.at(-1) + ' ' + palabra).trim().length > n) lineas.push(palabra)
    else lineas[lineas.length - 1] = (lineas.at(-1) + ' ' + palabra).trim()
  }
  return lineas.join('\n')
}

export function radar(indicadores, series) {
  // indicadores: [nombre], series: [{nombre, valores:[0..100]}]
  return {
    toolbox: descarga,
    tooltip: { trigger: 'item', valueFormatter: (v) => fmtPct(v) },
    legend: { bottom: 0, textStyle: { color: TEXTO, fontSize: 11 } },
    color: COLORES,
    radar: {
      indicator: indicadores.map((n) => ({ name: n, max: 100 })),
      radius: '58%', splitNumber: 4,
      axisName: { color: TEXTO, fontSize: 11.5, formatter: (n) => partir(n, 26) },
      splitLine: { lineStyle: { color: LINEA } }, splitArea: { show: false }, axisLine: { lineStyle: { color: LINEA } },
    },
    series: [{
      type: 'radar', symbolSize: 5,
      data: series.map((s) => ({ name: s.nombre, value: s.valores, areaStyle: { opacity: 0.15 } })),
    }],
  }
}

export function barrasAgrupadas(categorias, series, { horizontal = false, porcentaje = false, eje = '' } = {}) {
  // dos colores bien distintos (acento y gris oscuro) para comparar grupos
  const ejeVal = { ...ejeValores(porcentaje ? fmtEje : '{value}'), name: eje, nameLocation: 'middle', nameGap: 26, nameTextStyle: { color: TEXTO, fontSize: 11 } }
  return {
    toolbox: descarga,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => (porcentaje ? fmtPct(v) : v) },
    legend: { top: 0, left: 0, textStyle: { color: TEXTO, fontSize: 11 } },
    grid: { left: 8, right: horizontal ? 56 : 16, top: 30, bottom: eje ? 30 : 8, containLabel: true },
    color: ['#4f46e5', '#334155', '#a5b4fc', '#94a3b8'],
    xAxis: horizontal ? ejeVal : ejeCategorias(categorias),
    yAxis: horizontal ? { ...ejeCategorias(categorias), inverse: true, axisLabel: { color: TEXTO, fontSize: 11, width: 150, overflow: 'break' } } : ejeVal,
    series: series.map((s) => ({
      name: s.nombre, type: 'bar', data: s.valores, barMaxWidth: 22,
      itemStyle: { borderRadius: horizontal ? [0, 5, 5, 0] : [5, 5, 0, 0] },
      label: { show: true, position: horizontal ? 'right' : 'top', fontSize: 10.5, color: TEXTO, formatter: (p) => (porcentaje ? fmtPct(p.value) : p.value) },
    })),
  }
}

/** Radar de una escala de empleadores: % de acuerdo (total + parcial) y % totalmente de acuerdo por ítem. */
export function radarPerfil(items) {
  const parte = (item, codigo) => (item.frecuencias.find((f) => f.codigo === codigo)?.relativa ?? 0) * 100
  return radar(items.map((i) => i.criterio), [
    { nombre: 'De acuerdo (total o parcialmente)', valores: items.map((i) => +(i.porcentajeAcuerdo * 100).toFixed(1)) },
    { nombre: 'Totalmente de acuerdo', valores: items.map((i) => +parte(i, 'Totalmente_de_acuerdo').toFixed(1)) },
  ])
}

/** Misma opción sin animación ni botón de descarga, para el reporte impreso. */
export const paraImprimir = (opcion) => ({ ...opcion, animation: false, toolbox: { show: false } })

/** Prueba de las frecuencias: FO de cada opción en barras y la FE = n/k como línea, para ver qué opciones se alejan del reparto parejo. */
export function frecuenciaEsperada(u) {
  const etiquetas = u.categorias
  return {
    toolbox: descarga,
    grid: { left: 8, right: 70, top: 22, bottom: 30, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { ...ejeValores('{value}'), name: 'Respuestas (FO)', nameLocation: 'middle', nameGap: 26, nameTextStyle: { color: TEXTO, fontSize: 11 } },
    yAxis: { ...ejeCategorias(etiquetas), inverse: true, axisLabel: { color: TEXTO, fontSize: 11, width: 170, overflow: 'break' } },
    series: [{
      type: 'bar', name: 'FO', data: u.observada, barMaxWidth: 26,
      itemStyle: { color: ACENTO, borderRadius: [0, 6, 6, 0] },
      label: { show: true, position: 'right', color: TEXTO, fontSize: 11 },
      markLine: {
        symbol: 'none', silent: true, lineStyle: { color: '#f59e0b', type: 'dashed', width: 2 },
        label: { formatter: `FE = ${Number(u.esperada).toFixed(2).replace('.', ',')}`, color: '#b45309', fontSize: 11 },
        data: [{ xAxis: u.esperada }],
      },
    }],
  }
}
