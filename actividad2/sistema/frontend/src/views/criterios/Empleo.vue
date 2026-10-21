<script setup>
import { ref, computed } from 'vue'
import VChart from 'vue-echarts'
import CriterioPagina from '../../components/CriterioPagina.vue'
import Tarjeta from '../../components/Tarjeta.vue'
import { useApi } from '../../useApi'
import { barras, dona, likertApilado } from '../../graficos'

const { datos: d } = useApi('/api/empleabilidad')
const destinos = [
  { key: 'rubro', label: 'Rubro' },
  { key: 'sector_publico_privado', label: 'Sector' },
  { key: 'cargo', label: 'Cargo' },
  { key: 'area_organizacion', label: 'Área' },
  { key: 'remuneracion_promedio', label: 'Remuneración' },
  { key: 'departamento_bolivia', label: 'Departamento' },
]
const destino = ref('rubro')
const concordancia = computed(() => d.value && [
  { nombre: 'Sus labores son pertinentes a su formación', frecuencias: d.value.pertinenciaPuesto.frecuencias },
  { nombre: 'La formación concuerda con lo que pide el mercado', frecuencias: d.value.concordanciaRequerimientos.frecuencias },
])
</script>

<template>
  <CriterioPagina pantalla="empleo">
    <template v-if="d">
      <div class="rejilla rejilla-2">
        <Tarjeta titulo="Tiempo hasta el primer empleo" :nota="d.tiempoPrimerEmpleo.nota">
          <v-chart class="grafico" :option="barras(d.tiempoPrimerEmpleo.frecuencias)" autoresize />
        </Tarjeta>
        <Tarjeta titulo="Concordancia entre titulación y empleo">
          <v-chart class="grafico" :option="likertApilado(concordancia)" autoresize />
        </Tarjeta>
      </div>
      <Tarjeta titulo="Dónde trabajan" nota="Solo quienes trabajan en una organización. El área admite varias respuestas por persona." style="margin-top: 14px">
        <template #extra>
          <n-radio-group v-model:value="destino" size="small">
            <n-radio-button v-for="t in destinos" :key="t.key" :value="t.key" :label="t.label" />
          </n-radio-group>
        </template>
        <v-chart class="grafico-alto" :option="barras(d.destinoLaboral[destino], { horizontal: true, porcentaje: true })" autoresize />
      </Tarjeta>
    </template>
    <template #detalle>
      <Tarjeta v-if="d && d.medianaVidaProfesional != null" titulo="Experiencia profesional: junior y consolidados" style="margin-bottom: 14px"
        :nota="`La mitad de los titulados tiene hasta ${d.medianaVidaProfesional} años de vida profesional (mediana); la media es ${d.aniosVidaProfesional.media.toFixed(1)} porque unos pocos con muchos años la elevan. El corte entre grupos es la mediana.`">
        <n-table size="small" :bordered="false" :single-line="false">
          <thead><tr><th>Grupo</th><th>Años de vida profesional</th><th>Titulados</th><th>Ocupan un cargo ejecutivo</th></tr></thead>
          <tbody>
            <tr v-for="g in d.segmentosExperiencia" :key="g.segmento">
              <td><b>{{ g.segmento }}</b></td><td>{{ g.criterio }}</td><td>{{ g.casos }}</td>
              <td>{{ g.conCargo ? `${g.ejecutivos} de ${g.conCargo} (${(100 * g.ejecutivos / g.conCargo).toFixed(1)}%)` : '—' }}</td>
            </tr>
          </tbody>
        </n-table>
      </Tarjeta>
      <div v-if="d" class="rejilla rejilla-2">
        <Tarjeta titulo="Situación laboral actual"><v-chart class="grafico" :option="dona(d.situacionLaboral)" autoresize /></Tarjeta>
        <Tarjeta titulo="Razón por la que no trabaja"><v-chart class="grafico" :option="barras(d.razonNoTrabaja, { horizontal: true })" autoresize /></Tarjeta>
        <Tarjeta titulo="¿Es su primer empleo?"><v-chart class="grafico" :option="dona(d.primerEmpleo)" autoresize /></Tarjeta>
        <Tarjeta titulo="Empleos desde la titulación"><v-chart class="grafico" :option="barras(d.cantidadEmpleos)" autoresize /></Tarjeta>
        <Tarjeta titulo="Origen del emprendimiento"><v-chart class="grafico" :option="barras(d.emprendimiento.origen_emprendimiento, { horizontal: true })" autoresize /></Tarjeta>
        <Tarjeta titulo="Financiamiento del emprendimiento"><v-chart class="grafico" :option="dona(d.emprendimiento.financiamiento_externo_negocio)" autoresize /></Tarjeta>
      </div>
    </template>
  </CriterioPagina>
</template>
