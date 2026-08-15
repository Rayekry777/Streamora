import { expect, test } from '@playwright/test'

const webBaseUrl = process.env.E2E_WEB_BASE_URL ?? 'http://127.0.0.1:3000'
const adminBaseUrl = process.env.E2E_ADMIN_BASE_URL ?? 'http://127.0.0.1:3001'
const adminLogin = process.env.E2E_ADMIN_LOGIN ?? 'admin'
const adminPassword = process.env.E2E_ADMIN_PASSWORD ?? '123456'

test('用户端注册后切换个人宠物，且不能使用管理员会话', async ({ page }) => {
  const login = `ci-e2e-${Date.now()}-${test.info().workerIndex}`

  await page.goto('/')
  await expect(page.getByTestId('global-pet-host')).toHaveAttribute('data-pet-source', 'PUBLIC')

  await page.getByRole('link', { name: '登录' }).click()
  await page.getByRole('button', { name: '还没有账号？立即注册' }).click()
  await page.getByLabel('登录名').fill(login)
  await page.getByLabel('昵称').fill('部署联调用户')
  await page.getByLabel('密码').fill('streamora-e2e-user-password')
  await page.getByRole('button', { name: '注册并登录' }).click()

  await expect(page.getByRole('button', { name: '退出' })).toBeVisible()
  await expect(page.getByTestId('global-pet-host')).toHaveAttribute('data-pet-source', 'PERSONAL')

  const response = await page.context().request.get(`${adminBaseUrl}/admin-api/v1/auth/session`)
  expect(response.status()).toBe(401)
})

test('管理端登录后可读取运营概览，且不能使用用户会话', async ({ page }) => {
  await page.goto(`${adminBaseUrl}/login`)
  await page.getByLabel('管理员登录名').fill(adminLogin)
  await page.getByLabel('密码').fill(adminPassword)
  await page.getByRole('button', { name: '进入运营工作台' }).click()

  await expect(page.getByTestId('admin-shell')).toBeVisible()
  await expect(page.getByRole('heading', { name: '运营概览' })).toBeVisible()

  const overviewResponse = await page.context().request.get(`${adminBaseUrl}/admin-api/v1/operations/overview`)
  expect(overviewResponse.status()).toBe(200)

  const userSessionResponse = await page.context().request.get(`${webBaseUrl}/api/v1/auth/session`)
  expect(userSessionResponse.status()).toBe(401)
})
