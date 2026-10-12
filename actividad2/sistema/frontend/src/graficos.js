// Opciones de ECharts reutilizables, con una paleta minimalista.
export const COLORES = ['#4f46e5', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6', '#0ea5e9', '#84cc16', '#ec4899', '#64748b']
const TEXTO = '#71717a'
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
    ? ['#14b8a6', '#99f6e4', '#fca5a5', '#d4d4d8']
    : ['#ef4444', '#fca5a5', '#99f6e4', '#14b8a6']
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => `${v}%` },
    legend: { bottom: 0, textStyle: { color: TEXTO, fontSize: 11 }, icon: 'roundRect' },
    grid: { left: 230, right: 24, top: 8, bottom: 40 },
    xAxis: { ...ejeValores('{value}%'), max: 100 },
    yAxis: { ...ejeCategorias(filas.map((f) => f.nombre)), inverse: true, axisLabel: { color: TEXTO, fontSize: 11, width: 220, overflow: 'break' } },
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
    tooltip: {
      formatter: (p) => {
        const [j, i, v] = p.data
        return `${cruce.filas[i]} × ${cruce.columnas[j]}<br/>FO = <b>${v}</b> · FE = ${cruce.esperada[i][j].toFixed(2)}`
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

export function radar(indicadores, series) {
  // indicadores: [nombre], series: [{nombre, valores:[0..100]}]
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: TEXTO, fontSize: 11 } },
    color: COLORES,
    radar: {
      indicator: indicadores.map((n) => ({ name: n, max: 100 })),
      radius: '62%', splitNumber: 4,
      axisName: { color: TEXTO, fontSize: 10.5, width: 120, overflow: 'break' },
      splitLine: { lineStyle: { color: LINEA } }, splitArea: { show: false }, axisLine: { lineStyle: { color: LINEA } },
    },
    series: [{
      type: 'radar', symbolSize: 5,
      data: series.map((s) => ({ name: s.nombre, value: s.valores, areaStyle: { opacity: 0.15 } })),
    }],
  }
}

export function histogramaComparado(base, escenario, { decimales = 2, formato = (v) => v } = {}) {
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, textStyle: { color: TEXTO, fontSize: 11 } },
    grid: { left: 40, right: 16, top: 16, bottom: 56 },
    xAxis: ejeCategorias(base.marcas.map((m) => formato(+m.toFixed(decimales)))),
    yAxis: ejeValores('{value}'),
    series: [
      { name: 'Base', type: 'bar', data: base.frecuencias, itemStyle: { color: '#a5b4fc', borderRadius: [4, 4, 0, 0] }, barGap: '-100%' },
      { name: 'Escenario', type: 'bar', data: escenario.frecuencias, itemStyle: { color: 'rgba(20,184,166,0.75)', borderRadius: [4, 4, 0, 0] } },
    ],
  }
}

export function barrasAgrupadas(categorias, series) {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0, textStyle: { color: TEXTO, fontSize: 11 } },
    grid: { left: 40, right: 16, top: 16, bottom: 56 },
    color: COLORES,
    xAxis: ejeCategorias(categorias),
    yAxis: ejeValores('{value}'),
    series: series.map((s) => ({ name: s.nombre, type: 'bar', data: s.valores, barMaxWidth: 28, itemStyle: { borderRadius: [5, 5, 0, 0] } })),
  }
}
