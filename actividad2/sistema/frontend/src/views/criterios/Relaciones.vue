<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import CriterioPagina from '../../components/CriterioPagina.vue'
import Tarjeta from '../../components/Tarjeta.vue'
import { useApi } from '../../useApi'
import { barras, likertApilado, radarPerfil, SECUNDARIO } from '../../graficos'

const { datos: d } = useApi('/api/empleadores')
const ITEMS = ['perfil_vinculos_oportunidades', 'perfil_recurre_umss', 'perfil_opinion_practicas', 'perfil_consulta_autoridades', 'perfil_consultado_desempeno', 'perfil_consultada_pertinencia', 'perfil_consultado_autoevaluacion', 'perfil_participo_retroalimentacion', 'perfil_participo_vinculacion_entorno', 'perfil_participo_difusion', 'perfil_participo_vinculacion_profesionales']
const items = computed(() => d.value && ITEMS.map((c) => d.value.perfil.find((p) => p.codigo === c)).filter(Boolean))
const vinculacion = computed(() => items.value && items.value.map((p) => ({ nombre: p.criterio, frecuencias: p.frecuencias })))
const deAcuerdo = computed(() => items.value?.filter((p) => p.porcentajeAcuerdo > 0.5).length ?? 0)
const columnas = [{ title: 'Organización', key: 'nombre' }, { title: 'Tipo', key: 'tipo' }, { title: 'Tamaño', key: 'tamano' }, { title: 'Rubro', key: 'rubro' }]
</script>

<template>
  <CriterioPagina pantalla="relaciones">
    <template v-if="d">
      <Tarjeta :titulo="`Los empleadores están de acuerdo con ${deAcuerdo} de las ${items.length} afirmaciones sobre su relación con la Carrera`"
        nota="Porcentaje de organizaciones que eligió cada opción, ordenado de mayor a menor «Totalmente de acuerdo».">
        <v-chart style="height: 520px" :option="likertApilado(vinculacion, { ordenarPor: 'Totalmente_de_acuerdo' })" autoresize />
      </Tarjeta>
      <div class="rejilla rejilla-2" style="margin-top: 14px">
        <Tarjeta titulo="Cómo convocan profesionales"><v-chart class="grafico" :option="barras(d.medio_convocatoria, { horizontal: true, porcentaje: true, ordenar: true, eje: '% de organizaciones' })" autoresize /></Tarjeta>
        <Tarjeta titulo="Probabilidad de contratar en los próximos años"><v-chart class="grafico" :option="barras(d.probabilidad_contratacion, { horizontal: true, porcentaje: true, color: SECUNDARIO, eje: '% de organizaciones' })" autoresize /></Tarjeta>
      </div>
    </template>
    <template #detalle>
      <Tarjeta v-if="d" titulo="Las mismas afirmaciones en un diagrama de radar" style="margin-bottom: 14px"
        nota="Cada eje es una afirmación; mientras más cerca del borde, mayor acuerdo. Escala de 0% a 100%.">
        <v-chart style="height: 500px" :option="radarPerfil(items)" autoresize />
      </Tarjeta>
      <Tarjeta v-if="d" titulo="Organizaciones encuestadas" nota="Ningún resultado se atribuye a una organización con nombre.">
        <n-data-table size="small" :columns="columnas" :data="d.empresas" :pagination="{ pageSize: 10 }" />
      </Tarjeta>
    </template>
  </CriterioPagina>
</template>
