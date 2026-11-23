import { test, expect } from '@playwright/test'

// Las tres herramientas dinamicas: una variable, dos variables (cruce
// libre) y el constructor de reportes. Se verifica contenido real
// calculado por el backend, no solo que la pantalla carga.

test.describe('Explorar una variable', () => {
  test('al elegir una variable aparece su grafico y su n', async ({ page }) => {
    await page.goto('/#/explorar')
    const selectorVariable = page.locator('.selector .n-select').nth(1)
    await selectorVariable.click()
    await page.getByRole('option', { name: 'Género:', exact: false }).first().click()
    await expect(page.getByText(/respuestas/)).toBeVisible({ timeout: 10000 })
    await expect(page.locator('canvas, svg').first()).toBeVisible()
  })

  test('una variable numerica muestra media e intervalo de confianza', async ({ page }) => {
    await page.goto('/#/explorar')
    const selectorVariable = page.locator('.selector .n-select').nth(1)
    await selectorVariable.click()
    await page.getByRole('option', { name: /años de vida profesional/i }).first().click()
    await expect(page.getByText('Intervalo de confianza 95%')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('Media')).toBeVisible()
  })
})

test.describe('Comparar dos preguntas', () => {
  test('cruza dos variables y muestra el resultado de independencia', async ({ page }) => {
    await page.goto('/#/comparar')
    // Ya vienen precargadas genero x interes_posgrado (valores por defecto del componente)
    await expect(page.getByText(/est[aá]n relacionadas|No hay evidencia de que/)).toBeVisible({ timeout: 10000 })
  })

  test('el boton de intercambiar invierte las dos variables elegidas', async ({ page }) => {
    await page.goto('/#/comparar')
    const [selectX, selectY] = await page.locator('.selector .n-select').all()
    const valorXAntes = await selectX.innerText()
    const valorYAntes = await selectY.innerText()
    await page.getByTitle('Intercambiar').click()
    await expect(selectX).toHaveText(valorYAntes)
    await expect(selectY).toHaveText(valorXAntes)
  })
})

test.describe('Generar reporte', () => {
  test('al elegir variables y generar, se abre el reporte con esas preguntas', async ({ page, context }) => {
    await page.goto('/#/reporte')
    await page.getByRole('checkbox').first().check()
    const [reporte] = await Promise.all([
      context.waitForEvent('page'),
      page.getByRole('button', { name: 'Ver reporte / PDF' }).click(),
    ])
    await reporte.waitForLoadState()
    await expect(reporte).toHaveURL(/#\/imprimir/)
  })
})
