import { test, expect } from '@playwright/test'

// Verifica la correccion pedida: los codigos ARCU-SUR (2.1.2, 3.2.1...) no
// van en el menu, y el grupo se llama "Resultados por tema", no "Criterios
// ARCU-SUR".
test.describe('Menu principal', () => {
  test('el grupo se llama "Resultados por tema", no "Criterios ARCU-SUR"', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByText('Resultados por tema')).toBeVisible()
    await expect(page.getByText('Criterios ARCU-SUR', { exact: true })).toHaveCount(0)
  })

  test('ningun item del menu muestra un codigo tipo "2.1.2"', async ({ page }) => {
    await page.goto('/')
    const menu = page.locator('.n-menu')
    await expect(menu).not.toContainText(/\d\.\d\.\d/)
  })

  test('los 7 temas estan en el menu por su nombre', async ({ page }) => {
    await page.goto('/')
    const nombres = [
      'Perfil de egreso', 'Actualización curricular', 'Actualización y posgrado',
      'Relación con organizaciones', 'Resultados de la formación',
      'Seguimiento a graduados', 'Condiciones de empleo',
    ]
    for (const nombre of nombres) await expect(page.getByText(nombre, { exact: true })).toBeVisible()
  })

  test('las herramientas dinamicas estan en el menu', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByText('Explorar una variable')).toBeVisible()
    await expect(page.getByText('Comparar dos preguntas')).toBeVisible()
    await expect(page.getByText('Generar reporte')).toBeVisible()
  })
})
