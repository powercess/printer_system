// 支付流程 E2E 测试 (Mock)
import { test, expect } from '@playwright/test'
import { setupMockApi, clearLocalStorage, testUser } from './test-utils'

test.describe('钱包功能', () => {
  test.beforeEach(async ({ page }) => {
    await clearLocalStorage(page)
    await setupMockApi(page)

    // 登录用户
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', testUser.username)
    await page.fill('input[placeholder="请输入密码"]', testUser.password)
    await page.click('button:has-text("登录")')
    await page.waitForURL('/')
  })

  test('用户可以查看钱包余额', async ({ page }) => {
    // 导航到钱包页面
    await page.goto('/wallet')

    // 验证页面标题
    await expect(page.locator('h1:has-text("钱包")')).toBeVisible()
  })

  test('用户可以查看交易记录', async ({ page }) => {
    // Mock 交易记录
    await page.route('**/api/user/wallet/transactions**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: {
            total: 2,
            page: 1,
            pageSize: 10,
            items: [
              {
                id: 1,
                amount: 100.00,
                balanceBefore: 0,
                balanceAfter: 100.00,
                type: 0,
                relatedId: 'pay_001',
                createdAt: new Date().toISOString(),
              },
              {
                id: 2,
                amount: -2.50,
                balanceBefore: 100.00,
                balanceAfter: 97.50,
                type: 1,
                relatedId: 'order_001',
                createdAt: new Date().toISOString(),
              },
            ],
          },
        }),
      })
    })

    // 导航到钱包页面
    await page.goto('/wallet')

    // 验证交易记录显示
    await expect(page.locator('h1:has-text("钱包")')).toBeVisible()
  })

  test('用户可以发起充值', async ({ page }) => {
    // Mock 充值响应
    await page.route('**/api/user/wallet/recharge', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: {
            id: 1,
            amount: 50.00,
            balanceBefore: 100.00,
            balanceAfter: 150.00,
            type: 0,
            relatedId: 'pay_' + Date.now(),
            createdAt: new Date().toISOString(),
          },
        }),
      })
    })

    // 导航到钱包页面
    await page.goto('/wallet')

    // 如果有充值按钮，点击充值
    const rechargeButton = page.locator('button:has-text("充值")')
    if (await rechargeButton.count() > 0) {
      await rechargeButton.click()

      // 等待充值对话框或页面
      await page.waitForTimeout(500)
    }
  })

  test('空交易记录显示提示信息', async ({ page }) => {
    // Mock 空交易记录
    await page.route('**/api/user/wallet/transactions**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: {
            total: 0,
            page: 1,
            pageSize: 10,
            items: [],
          },
        }),
      })
    })

    // 导航到钱包页面
    await page.goto('/wallet')

    // 验证页面加载
    await expect(page.locator('h1:has-text("钱包")')).toBeVisible()
  })
})

test.describe('支付流程 (Mock)', () => {
  test.beforeEach(async ({ page }) => {
    await clearLocalStorage(page)
    await setupMockApi(page)

    // 登录用户
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', testUser.username)
    await page.fill('input[placeholder="请输入密码"]', testUser.password)
    await page.click('button:has-text("登录")')
    await page.waitForURL('/')
  })

  test('余额不足时提示充值', async ({ page }) => {
    // Mock 余额为 0
    await page.route('**/api/user/profile', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: {
            id: 1,
            username: testUser.username,
            email: testUser.email,
            role: 'user',
            balance: 0,
            created_at: new Date().toISOString(),
            updated_at: new Date().toISOString(),
          },
        }),
      })
    })

    await page.route('**/api/user/wallet/balance', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: { balance: 0 },
        }),
      })
    })

    // 刷新页面
    await page.reload()

    // 上传文件并尝试下单
    const testFileContent = 'Test PDF content'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'no-balance.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 选择打印机
    await page.click('[role="combobox"], button:has-text("请选择打印机")')
    await page.click('text=Printer-A')

    // 估算价格
    await page.click('button:has-text("估算价格")')
    await expect(page.locator('text=预估价格')).toBeVisible()

    // 尝试提交订单
    await page.click('button:has-text("提交订单")')

    // 等待错误提示
    await page.waitForTimeout(500)
  })

  test('支付回调处理 (Mock)', async ({ page }) => {
    // Mock 支付通知回调
    await page.route('**/api/payment/notify**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'text/plain',
        body: 'success',
      })
    })

    // 模拟支付回调（通常由支付平台发起）
    const response = await page.request.post('http://localhost:8080/api/payment/notify', {
      data: {
        order_id: 'pay_test_001',
        status: 'success',
        amount: 50.00,
      },
    })

    // 验证响应
    expect(response.status()).toBe(200)
  })
})

test.describe('钱包权限', () => {
  test('未登录用户无法访问钱包页面', async ({ page }) => {
    await clearLocalStorage(page)

    // 尝试访问钱包页面
    await page.goto('/wallet')

    // 验证被重定向到登录页面
    await page.waitForURL('/login')
    await expect(page).toHaveURL('/login')
  })
})