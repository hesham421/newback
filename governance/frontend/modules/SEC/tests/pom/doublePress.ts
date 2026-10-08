/** Double presses on one button (SEC P2_5 SEC-U122, TEN-U144): Playwright's `dblclick()` and a zero-gap scripted pair. */
import type { Locator } from '@playwright/test'

export async function doublePress(target: Locator): Promise<void> {
  await target.dblclick()
}

/** Two clicks in one task: no render can come between them, so only the button's own latch stops the second. */
export async function zeroGapPress(target: Locator): Promise<void> {
  await target.evaluate((element: HTMLElement) => {
    element.click()
    element.click()
  })
}
