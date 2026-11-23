import { test, expect } from '@playwright/test'

// Cada pantalla tiene su propia URL (#/empleo, #/perfil...), asi que se
// navega directo en vez de hacer clic en el menu (mas estable).
test.describe('Pantalla de un criterio (Condiciones de empleo)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/empleo')
    // Espera a que carguen los datos del criterio antes de revisar el contenido
    await expect(page.locator('.valoracion, .nota').first()).toBeVisible({ timeout: 15000 })
  })

  test('muestra el nombre del criterio como titulo, sin el codigo en el texto', async ({ page }) => {
    const titulo = page.locator('h1.pagina-titulo')
    await expect(titulo).toHaveText('Condiciones de empleo')
  })

  test('el codigo ARCU-SUR esta en una etiqueta aparte con tooltip, no en el titulo', async ({ page }) => {
    const etiqueta = page.locator('.etiqueta-criterio')
    await expect(etiqueta).toHaveText('3.2.3')
    await etiqueta.hover()
    await expect(page.getByText(/Criterio 3\.2\.3 de ARCU-SUR/)).toBeVisible()
  })

  test('hay una sola frase de conclusion arriba, no la explicacion completa de la regla', async ({ page }) => {
    const conclusion = page.locator('p.pagina-sub')
    await expect(conclusion).toBeVisible()
    // La explicacion larga de que es "favorable" ya no va como texto fijo
    await expect(page.getByText('Un aspecto es favorable cuando más de la mitad')).toHaveCount(0)
  })

  test('la lista de favorables/desfavorables tiene las dos columnas', async ({ page }) => {
    await expect(page.locator('.valoracion-columna-titulo.bien')).toContainText('Favorables')
    await expect(page.locator('.valoracion-columna-titulo.mal')).toContainText('Desfavorables')
  })

  test('se ven los graficos de empleabilidad', async ({ page }) => {
    await expect(page.getByText('Tiempo hasta el primer empleo')).toBeVisible()
    await expect(page.getByText('Concordancia entre titulación y empleo')).toBeVisible()
    await expect(page.locator('canvas').first()).toBeVisible()
  })

  test('la seccion de relaciones entre preguntas muestra al menos un cruce con su conclusion', async ({ page }) => {
    const seccion = page.getByText('Relaciones entre preguntas')
    await expect(seccion).toBeVisible()
    // Cada cruce muestra si hay o no evidencia de relacion
    await expect(page.getByText(/est[aá]n relacionadas|No hay evidencia de que/).first()).toBeVisible()
  })
})
