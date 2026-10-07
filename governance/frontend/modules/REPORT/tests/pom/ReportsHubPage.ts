/** POM helpers for the reports hub (SEC/NOTIF/AUDIT_REPORTS): open a module hub and select a report. */
import { expect, type Page } from '@playwright/test'

export async function openReport(page: Page, moduleCode: string, reportCode: string): Promise<void> {
  await page.goto(`/reports/${moduleCode}`)
  await expect(page.getByTestId(`report-card-${reportCode}`)).toBeVisible()
  await page.getByTestId(`report-select-${reportCode}`).click()
  await expect(page).toHaveURL(new RegExp(`report=${reportCode}`))
  await expect(page.getByTestId('report-params-drawer')).toBeVisible()
}
