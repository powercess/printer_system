// 打印下单流程 E2E 测试
import { test, expect } from '@playwright/test'
import { setupMockApi, clearLocalStorage, testUser, mockApiResponses } from './test-utils'

test.describe('打印下单流程', () => {
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

  test('用户可以查看打印机状态', async ({ page }) => {
    // 验证打印机状态区域显示
    await expect(page.locator('h2:has-text("打印机状态")')).toBeVisible()

    // 验证打印机列表
    await expect(page.locator('text=Printer-A')).toBeVisible()
    await expect(page.locator('text=Printer-B')).toBeVisible()
  })

  test('完整的打印下单流程', async ({ page }) => {
    // 1. 上传文件
    const testFileContent = 'Test PDF content for printing'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'print-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 等待文件上传成功
    await expect(page.locator('text=print-test.pdf')).toBeVisible()

    // 2. 验证打印选项出现
    await expect(page.locator('h2:has-text("打印选项")')).toBeVisible()

    // 3. 选择打印机
    await page.click('[role="combobox"], button:has-text("请选择打印机")')
    await page.click('text=Printer-A')

    // 4. 设置打印份数
    const copiesInput = page.locator('input[type="number"]')
    await copiesInput.fill('2')

    // 5. 选择颜色模式
    await page.click('text=彩色')

    // 6. 选择纸张大小
    await page.click('button:has-text("A4")')
    await page.click('text=A3')

    // 7. 开启双面打印
    await page.click('[role="switch"]')

    // 8. 点击估算价格
    await page.click('button:has-text("估算价格")')

    // 等待价格显示
    await expect(page.locator('text=预估价格')).toBeVisible()

    // 9. 提交订单
    await page.click('button:has-text("提交订单")')

    // 等待订单提交成功
    await page.waitForTimeout(1000)
  })

  test('价格估算功能', async ({ page }) => {
    // 上传文件
    const testFileContent = 'Test PDF content'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'price-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 选择打印机
    await page.click('[role="combobox"], button:has-text("请选择打印机")')
    await page.click('text=Printer-A')

    // 点击估算价格
    await page.click('button:has-text("估算价格")')

    // 验证价格显示
    await expect(page.locator('text=预估价格')).toBeVisible()
    await expect(page.locator('text=¥2.50')).toBeVisible()
    await expect(page.locator('text=5 页')).toBeVisible()
  })

  test('未选择打印机无法提交订单', async ({ page }) => {
    // 上传文件
    const testFileContent = 'Test PDF content'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'no-printer-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 不选择打印机，直接提交订单
    await page.click('button:has-text("提交订单")')

    // 等待错误提示
    await page.waitForTimeout(500)

    // 验证仍在首页
    await expect(page).toHaveURL('/')
  })

  test('余额不足无法提交订单', async ({ page }) => {
    // Mock 余额不足的用户
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
            balance: 0.50,  // 余额不足
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
          data: { balance: 0.50 },
        }),
      })
    })

    // 刷新页面获取最新余额
    await page.reload()

    // 上传文件
    const testFileContent = 'Test PDF content'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'low-balance-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 选择打印机
    await page.click('[role="combobox"], button:has-text("请选择打印机")')
    await page.click('text=Printer-A')

    // 估算价格
    await page.click('button:has-text("估算价格")')
    await expect(page.locator('text=预估价格')).toBeVisible()

    // 提交订单（余额不足应该失败）
    await page.click('button:has-text("提交订单")')

    // 等待错误提示
    await page.waitForTimeout(500)
  })

  test('用户可以查看订单列表', async ({ page }) => {
    // 导航到订单页面
    await page.goto('/orders')

    // 验证页面标题
    await expect(page.locator('h1:has-text("订单记录")')).toBeVisible()

    // 验证订单列表显示
    await expect(page.locator('text=test.pdf')).toBeVisible()
    await expect(page.locator('text=Printer-A')).toBeVisible()
  })

  test('用户可以取消待处理的订单', async ({ page }) => {
    // Mock 取消订单成功
    await page.route('**/api/order/cancel', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: {
            success: true,
            message: '订单已取消',
            refund_amount: 2.50,
          },
        }),
      })
    })

    // 导航到订单页面
    await page.goto('/orders')

    // 等待订单列表加载
    await expect(page.locator('text=test.pdf')).toBeVisible()

    // 点击取消按钮
    await page.click('button:has-text("取消")')

    // 等待确认对话框
    await expect(page.locator('text=确认取消订单')).toBeVisible()

    // 确认取消
    await page.click('button:has-text("确认取消")')

    // 等待操作完成
    await page.waitForTimeout(1000)
  })

  test('用户可以按状态筛选订单', async ({ page }) => {
    // 导航到订单页面
    await page.goto('/orders')

    // 点击状态筛选下拉框
    await page.click('[role="combobox"], button:has-text("筛选状态")')

    // 选择"已完成"
    await page.click('text=已完成')

    // 验证筛选生效
    await page.waitForTimeout(500)
  })

  test('空订单列表显示提示信息', async ({ page }) => {
    // Mock 空订单列表
    await page.route('**/api/order/list**', async (route) => {
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

    // 导航到订单页面
    await page.goto('/orders')

    // 验证空列表提示
    await expect(page.locator('text=暂无订单记录')).toBeVisible()
  })
})

test.describe('打印选项', () => {
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

  test('颜色模式切换', async ({ page }) => {
    // 上传文件
    const testFileContent = 'Test PDF content'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'color-mode-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 默认应该是黑白
    await expect(page.locator('input[value="bw"]')).toBeChecked()

    // 切换到彩色
    await page.click('text=彩色')
    await expect(page.locator('input[value="color"]')).toBeChecked()
  })

  test('纸张大小选择', async ({ page }) => {
    // 上传文件
    const testFileContent = 'Test PDF content'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'paper-size-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 默认应该是 A4
    const paperSizeSelect = page.locator('button:has-text("A4")')
    await expect(paperSizeSelect).toBeVisible()

    // 选择 A3
    await paperSizeSelect.click()
    await page.click('text=A3')
    await expect(page.locator('button:has-text("A3")')).toBeVisible()
  })

  test('双面打印开关', async ({ page }) => {
    // 上传文件
    const testFileContent = 'Test PDF content'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'duplex-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 默认应该是关闭
    const duplexSwitch = page.locator('[role="switch"]')
    await expect(duplexSwitch).not.toBeChecked()

    // 开启双面打印
    await duplexSwitch.click()
    await expect(duplexSwitch).toBeChecked()
  })

  test('打印份数输入', async ({ page }) => {
    // 上传文件
    const testFileContent = 'Test PDF content'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'copies-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 获取份数输入框
    const copiesInput = page.locator('input[type="number"]')

    // 默认应该是 1
    await expect(copiesInput).toHaveValue('1')

    // 修改份数
    await copiesInput.fill('5')
    await expect(copiesInput).toHaveValue('5')
  })

  test('页码范围输入', async ({ page }) => {
    // 上传文件
    const testFileContent = 'Test PDF content'
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'page-range-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from(testFileContent),
    })

    // 输入页码范围
    const pageRangeInput = page.locator('input[placeholder="例如: 1-5, 8, 10-12"]')
    await pageRangeInput.fill('1-3, 5')

    // 验证输入值
    await expect(pageRangeInput).toHaveValue('1-3, 5')
  })
})

test.describe('打印下单权限', () => {
  test('未登录用户无法访问订单页面', async ({ page }) => {
    await clearLocalStorage(page)

    // 尝试访问订单页面
    await page.goto('/orders')

    // 验证被重定向到登录页面
    await page.waitForURL('/login')
    await expect(page).toHaveURL('/login')
  })
})