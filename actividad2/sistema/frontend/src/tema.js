// Colores del panel para Naive UI (no lee variables CSS: necesita los valores para calcular
// los tonos de hover y pressed). Son los mismos de :root en style.css; si cambia uno, cambiar ambos.
export const ACENTO = '#4f46e5'

export const temaNaive = {
  common: {
    primaryColor: ACENTO, primaryColorHover: '#6366f1', primaryColorPressed: '#4338ca',
    fontFamily: 'Inter, system-ui, sans-serif', borderRadius: '8px',
  },
  Card: { borderRadius: '12px' },
  Menu: { itemHeight: '38px', groupTextColor: '#9a9aa5', itemTextColorActive: ACENTO, itemColorActive: '#eef2ff', itemColorActiveHover: '#eef2ff' },
}
