// Listas que se reordenan arrastrando (vue-draggable-plus, sobre SortableJS), con la misma
// configuración en todo el panel: se toman por el asa de 6 puntos y las demás tarjetas se
// corren con animación para dejar el hueco, como en Google Forms.
//
//   <VueDraggable v-model="lista" v-bind="ARRASTRE" @end="(e) => cambioOrden(e) && guardar()">
//     <div v-for="x in lista" :key="x.id" class="arrastrable"><AsaArrastre /> ...</div>
//   </VueDraggable>

export const ARRASTRE = {
  handle: '.asa-arrastre',
  animation: 180,
  ghostClass: 'fantasma', // el hueco donde va a caer
  chosenClass: 'elegida',
  dragClass: 'arrastrada',
}

/** Si el arrastre cambió algo (soltar en el mismo lugar no se guarda). */
export const cambioOrden = (e) => e.from !== e.to || e.oldIndex !== e.newIndex

/** Copia de la lista con el elemento `i` movido `delta` lugares (↑/↓ del teclado en el asa); null si no se puede. */
export function moverUno(lista, i, delta) {
  const j = i + delta
  if (j < 0 || j >= lista.length) return null
  const copia = [...lista]
  copia.splice(j, 0, copia.splice(i, 1)[0])
  return copia
}
