import { defineConfig } from '@playwright/test'

// Pruebas E2E del dashboard. No arranca el backend ni el frontend por su
// cuenta (webServer): el equipo que prueba corre los dos de antemano
// (ver sistema/README.md), igual que para usar el dashboard a mano.
export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://localhost:5173',
  },
  reporter: 'list',
})
