import { defineConfig } from '@playwright/test'

const reportSuffix = process.env.STREAMORA_E2E_REPORT_SUFFIX ?? 'deployed'

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['html', { outputFolder: `playwright-report/${reportSuffix}`, open: 'never' }],
    ['junit', { outputFile: `test-results/${reportSuffix}/e2e-junit.xml` }],
    ['list'],
  ],
  use: {
    baseURL: process.env.E2E_WEB_BASE_URL ?? 'http://127.0.0.1:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
})
