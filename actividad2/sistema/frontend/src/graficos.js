// Opciones de ECharts reutilizables: un solo color de acento y grises, para que los gráficos no compitan entre sí.
export const ACENTO = '#4f46e5'
export const COLORES = ['#4f46e5', '#a5b4fc', '#334155', '#94a3b8', '#c7d2fe', '#64748b', '#e0e7ff', '#cbd5e1', '#1e1b4b']
const TEXTO = '#71717a'
// Botón para descargar cada gráfico como imagen PNG (fondo blanco, doble resolución)
const descarga = {
  right: 0, top: 0, itemSize: 13,
  iconStyle: { borderColor: '#a1a1aa' },
  feature: { saveAsImage: { title: 'Descargar PNG', name: 'grafico', pixelRatio: 2, backgroundColor: '#fff' } },
}
const LINEA = '#e4e4e7'

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
export function barras(items, { horizontal = false, color = COLORES[0], porcentaje = false } = {}) {
  const etiquetas = items.map((i) => i.etiqueta)
  const valores = items.map((i) => (porcentaje ? +(i.relativa * 100).toFixed(1) : i.frecuencia))
  const fmt = porcentaje ? '{value}%' : '{value}'
  return {
    toolbox: descarga,
    grid: { left: horizontal ? 150 : 40, right: 24, top: 16, bottom: horizontal ? 24 : 60, containLabel: false },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => (porcentaje ? `${v}%` : v) },
    xAxis: horizontal ? ejeValores(fmt) : ejeCategorias(etiquetas, etiquetas.length > 5 ? 25 : 0),
    yAxis: horizontal ? { ...ejeCategorias(etiquetas), inverse: true } : ejeValores(fmt),
    series: [{
      type: 'bar', data: valores, barMaxWidth: 34,
      itemStyle: { color, borderRadius: horizontal ? [0, 6, 6, 0] : [6, 6, 0, 0] },
      label: { show: true, position: horizontal ? 'right' : 'top', color: TEXTO, fontSize: 11, formatter: porcentaje ? '{c}%' : '{c}' },
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
export function likertApilado(filas) {
  // filas: [{nombre, frecuencias:[{etiqueta, relativa}]}]
  const niveles = filas[0]?.frecuencias.map((f) => f.etiqueta) ?? []
  const colores = niveles.length === 4 && niveles.includes('No sabe')
    ? ['#4f46e5', '#a5b4fc', '#fb7185', '#e4e4e7']
    : ['#fb7185', '#fecdd3', '#a5b4fc', '#4f46e5']
  return {
    toolbox: descarga,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => `${v}%` },
    legend: { top: 0, left: 0, textStyle: { color: TEXTO, fontSize: 11 }, icon: 'roundRect', itemWidth: 12, itemHeight: 8 },
    grid: { left: 200, right: 16, top: 34, bottom: 24 },
    xAxis: { ...ejeValores('{value}%'), max: 100 },
    yAxis: { ...ejeCategorias(filas.map((f) => f.nombre)), inverse: true, axisLabel: { color: TEXTO, fontSize: 11, width: 190, overflow: 'break' } },
    series: niveles.map((nivel, k) => ({
      name: nivel, type: 'bar', stack: 'total', barMaxWidth: 26,
      itemStyle: { color: colores[k] },
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
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: TEXTO, fontSize: 11 } },
    color: COLORES,
    radar: {
      indicator: indicadores.map((n) => ({ name: n, max: 100 })),
      radius: '58%', splitNumber: 4,
      axisName: { color: TEXTO, fontSize: 10.5, formatter: (n) => partir(n, 24) },
      splitLine: { lineStyle: { color: LINEA } }, splitArea: { show: false }, axisLine: { lineStyle: { color: LINEA } },
    },
    series: [{
      type: 'radar', symbolSize: 5,
      data: series.map((s) => ({ name: s.nombre, value: s.valores, areaStyle: { opacity: 0.15 } })),
    }],
  }
}

export function barrasAgrupadas(categorias, series) {
  return {
    toolbox: descarga,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0, textStyle: { color: TEXTO, fontSize: 11 } },
    grid: { left: 40, right: 16, top: 16, bottom: 56 },
    color: COLORES,
    xAxis: ejeCategorias(categorias),
    yAxis: ejeValores('{value}'),
    series: series.map((s) => ({ name: s.nombre, type: 'bar', data: s.valores, barMaxWidth: 28, itemStyle: { borderRadius: [5, 5, 0, 0] } })),
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
