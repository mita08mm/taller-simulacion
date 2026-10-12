import { createApp } from 'vue'
import naive from 'naive-ui'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart, HeatmapChart, RadarChart, LineChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent, VisualMapComponent, TitleComponent, MarkLineComponent,
} from 'echarts/components'
import './style.css'
import App from './App.vue'

use([
  CanvasRenderer, BarChart, PieChart, HeatmapChart, RadarChart, LineChart,
  GridComponent, TooltipComponent, LegendComponent, VisualMapComponent, TitleComponent, MarkLineComponent,
])

createApp(App).use(naive).mount('#app')
