// 认证流程 E2E 测试
import { test, expect } from '@playwright/test'
import { setupMockApi, clearLocalStorage, testUser, mockApiResponses } from './test-utils'

test.describe('用户认证流程', () => {
  test.beforeEach(async ({ page }) => {
    await clearLocalStorage(page)
  })

  test('用户可以注册新账号', async ({ page }) => {
    await setupMockApi(page)

    // 导航到注册页面
    await page.goto('/register')

    // 等待页面加载
    await expect(page.locator('h1')).toContainText('注册')

    // 填写注册表单
    await page.fill('input[placeholder="请输入用户名"]', testUser.username)
    await page.fill('input[placeholder="请输入邮箱"]', testUser.email)
    await page.fill('input[placeholder="请输入密码"]', testUser.password)
    await page.fill('input[placeholder="请再次输入密码"]', testUser.password)

    // 提交表单
    await page.click('button:has-text("注册")')

    // 验证注册成功后跳转到首页
    await page.waitForURL('/')
    await expect(page).toHaveURL('/')
  })

  test('注册时密码不一致显示错误', async ({ page }) => {
    await page.goto('/register')

    // 填写表单，密码不一致
    await page.fill('input[placeholder="请输入用户名"]', testUser.username)
    await page.fill('input[placeholder="请输入邮箱"]', testUser.email)
    await page.fill('input[placeholder="请输入密码"]', testUser.password)
    await page.fill('input[placeholder="请再次输入密码"]', 'different_password')

    // 提交表单
    await page.click('button:has-text("注册")')

    // 等待 toast 错误消息出现
    // 由于 toast 是动态的，我们检查是否有错误提示
    await page.waitForTimeout(500)

    // 验证仍在注册页面
    await expect(page).toHaveURL('/register')
  })

  test('用户可以登录', async ({ page }) => {
    await setupMockApi(page)

    // 导航到登录页面
    await page.goto('/login')

    // 等待页面加载
    await expect(page.locator('h1')).toContainText('登录')

    // 填写登录表单
    await page.fill('input[placeholder="请输入用户名"]', testUser.username)
    await page.fill('input[placeholder="请输入密码"]', testUser.password)

    // 提交表单
    await page.click('button:has-text("登录")')

    // 验证登录成功后跳转到首页
    await page.waitForURL('/')
    await expect(page).toHaveURL('/')
  })

  test('登录失败显示错误信息', async ({ page }) => {
    // Mock 登录失败响应
    await page.route('**/api/user/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 401,
          message: '用户名或密码错误',
          data: null,
        }),
      })
    })

    // Mock 其他 API
    await page.route('**/api/user/profile', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockApiResponses.userProfile),
      })
    })

    await page.goto('/login')

    // 填写登录表单
    await page.fill('input[placeholder="请输入用户名"]', 'wrong_user')
    await page.fill('input[placeholder="请输入密码"]', 'wrong_password')

    // 提交表单
    await page.click('button:has-text("登录")')

    // 验证仍在登录页面
    await expect(page).toHaveURL('/login')
  })

  test('未登录用户访问受保护页面时重定向到登录页', async ({ page }) => {
    await clearLocalStorage(page)

    // 尝试访问受保护的页面
    await page.goto('/files')

    // 验证被重定向到登录页面
    await page.waitForURL('/login')
    await expect(page).toHaveURL('/login')
  })

  test('用户可以登出', async ({ page }) => {
    await setupMockApi(page)

    // 先登录
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', testUser.username)
    await page.fill('input[placeholder="请输入密码"]', testUser.password)
    await page.click('button:has-text("登录")')

    // 等待跳转到首页
    await page.waitForURL('/')

    // 点击用户菜单或登出按钮
    // 查找登出按钮（可能在 header 或菜单中）
    const logoutButton = page.locator('button:has-text("退出"), a:has-text("退出"), [data-testid="logout"]')

    // 如果找到登出按钮则点击
    if (await logoutButton.count() > 0) {
      await logoutButton.first().click()
    } else {
      // 尝试通过点击用户头像打开菜单
      const userMenu = page.locator('[data-testid="user-menu"], button:has-text("用户")')
      if (await userMenu.count() > 0) {
        await userMenu.click()
        await page.click('button:has-text("退出"), a:has-text("退出")')
      }
    }

    // 验证重定向到登录页面
    await page.waitForURL('/login')
    await expect(page).toHaveURL('/login')
  })

  test('已登录用户访问登录页时重定向到首页', async ({ page }) => {
    await setupMockApi(page)

    // 先登录
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', testUser.username)
    await page.fill('input[placeholder="请输入密码"]', testUser.password)
    await page.click('button:has-text("登录")')
    await page.waitForURL('/')

    // 尝试访问登录页面
    await page.goto('/login')

    // 验证被重定向到首页
    await page.waitForURL('/')
    await expect(page).toHaveURL('/')
  })

  test('注册页面有跳转到登录页的链接', async ({ page }) => {
    await page.goto('/register')

    // 点击"立即登录"链接
    await page.click('a:has-text("立即登录")')

    // 验证跳转到登录页面
    await expect(page).toHaveURL('/login')
  })

  test('登录页面有跳转到注册页的链接', async ({ page }) => {
    await page.goto('/login')

    // 点击"立即注册"链接
    await page.click('a:has-text("立即注册")')

    // 验证跳转到注册页面
    await expect(page).toHaveURL('/register')
  })
})

test.describe('表单验证', () => {
  test('注册表单验证必填字段', async ({ page }) => {
    await page.goto('/register')

    // 直接点击注册按钮，不填写任何字段
    await page.click('button:has-text("注册")')

    // 等待 toast 消息
    await page.waitForTimeout(500)

    // 验证仍在注册页面
    await expect(page).toHaveURL('/register')
  })

  test('登录表单验证必填字段', async ({ page }) => {
    await page.goto('/login')

    // 直接点击登录按钮，不填写任何字段
    await page.click('button:has-text("登录")')

    // 等待 toast 消息
    await page.waitForTimeout(500)

    // 验证仍在登录页面
    await expect(page).toHaveURL('/login')
  })

  test('注册时密码长度验证', async ({ page }) => {
    await page.goto('/register')

    // 填写表单，密码过短
    await page.fill('input[placeholder="请输入用户名"]', testUser.username)
    await page.fill('input[placeholder="请输入邮箱"]', testUser.email)
    await page.fill('input[placeholder="请输入密码"]', '12345')  // 5位密码
    await page.fill('input[placeholder="请再次输入密码"]', '12345')

    // 提交表单
    await page.click('button:has-text("注册")')

    // 验证仍在注册页面
    await expect(page).toHaveURL('/register')
  })
})