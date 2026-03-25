// E2E 测试工具函数
import type { Page} from '@playwright/test';
import { expect } from '@playwright/test'

// 测试用户配置
export const testUser = {
  username: `testuser_${Date.now()}`,
  email: `test_${Date.now()}@example.com`,
  password: 'Test123456',
}

// Mock API 响应
export const mockApiResponses = {
  // 用户注册成功
  registerSuccess: {
    code: 201,
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
  },

  // 用户登录成功
  loginSuccess: {
    code: 200,
    message: 'success',
    data: {
      token: 'mock_token_' + Date.now(),
    },
  },

  // 用户信息
  userProfile: {
    code: 200,
    message: 'success',
    data: {
      id: 1,
      username: testUser.username,
      email: testUser.email,
      role: 'user',
      balance: 100.00,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    },
  },

  // 钱包余额
  walletBalance: {
    code: 200,
    message: 'success',
    data: {
      balance: 100.00,
    },
  },

  // 打印机列表
  printersList: {
    code: 200,
    message: 'success',
    data: [
      { name: 'Printer-A', description: '主楼打印机', status: 'idle' },
      { name: 'Printer-B', description: '图书馆打印机', status: 'idle' },
    ],
  },

  // 文件上传成功
  fileUploadSuccess: {
    code: 201,
    message: 'success',
    data: {
      file_id: 'file_' + Date.now(),
      file_name: 'test.pdf',
      file_path: '/uploads/test.pdf',
      file_size: 1024,
    },
  },

  // 文件列表
  fileList: {
    code: 200,
    message: 'success',
    data: {
      total: 1,
      page: 1,
      pageSize: 10,
      items: [
        {
          id: 1,
          userId: 1,
          name: 'test.pdf',
          filePath: '/uploads/test.pdf',
          fileSize: 1024,
          fileType: 'application/pdf',
          pageCount: 5,
          uploadTime: new Date().toISOString(),
          deletedAt: null,
        },
      ],
    },
  },

  // 价格估算
  priceEstimate: {
    code: 200,
    message: 'success',
    data: {
      price: 2.50,
      pages: 5,
    },
  },

  // 创建订单成功
  createOrderSuccess: {
    code: 201,
    message: 'success',
    data: {
      id: 1,
      userId: 1,
      fileId: 1,
      fileName: 'test.pdf',
      printerId: 1,
      printerName: 'Printer-A',
      copies: 1,
      colorMode: 0,
      duplex: 0,
      paperSize: 'A4',
      originalAmount: 2.50,
      discountAmount: 0,
      finalAmount: 2.50,
      status: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      username: testUser.username,
    },
  },

  // 订单列表
  orderList: {
    code: 200,
    message: 'success',
    data: {
      total: 1,
      page: 1,
      pageSize: 10,
      items: [
        {
          id: 1,
          userId: 1,
          fileId: 1,
          fileName: 'test.pdf',
          printerId: 1,
          printerName: 'Printer-A',
          copies: 1,
          colorMode: 0,
          duplex: 0,
          paperSize: 'A4',
          originalAmount: 2.50,
          discountAmount: 0,
          finalAmount: 2.50,
          status: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          username: testUser.username,
        },
      ],
    },
  },
}

// Mock API 请求
export async function setupMockApi(page: Page) {
  // Mock 用户注册
  await page.route('**/api/user/register', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.registerSuccess),
    })
  })

  // Mock 用户登录
  await page.route('**/api/user/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.loginSuccess),
    })
  })

  // Mock 用户信息
  await page.route('**/api/user/profile', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.userProfile),
    })
  })

  // Mock 钱包余额
  await page.route('**/api/user/wallet/balance', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.walletBalance),
    })
  })

  // Mock 打印机列表
  await page.route('**/api/printer/cups/list', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.printersList),
    })
  })

  // Mock 文件上传
  await page.route('**/api/file/upload', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.fileUploadSuccess),
    })
  })

  // Mock 文件列表
  await page.route('**/api/file/list**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.fileList),
    })
  })

  // Mock 价格估算
  await page.route('**/api/order/estimate', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.priceEstimate),
    })
  })

  // Mock 创建订单
  await page.route('**/api/order/create', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.createOrderSuccess),
    })
  })

  // Mock 订单列表
  await page.route('**/api/order/list**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockApiResponses.orderList),
    })
  })
}

// 辅助函数：等待页面加载
export async function waitForPageLoad(page: Page) {
  await page.waitForLoadState('networkidle')
}

// 辅助函数：检查元素可见性
export async function expectElementVisible(page: Page, selector: string) {
  await expect(page.locator(selector)).toBeVisible()
}

// 辅助函数：检查元素文本
export async function expectElementText(page: Page, selector: string, text: string) {
  await expect(page.locator(selector)).toContainText(text)
}

// 辅助函数：填充表单
export async function fillForm(page: Page, fields: Record<string, string>) {
  for (const [selector, value] of Object.entries(fields)) {
    await page.fill(selector, value)
  }
}

// 辅助函数：清除 localStorage
export async function clearLocalStorage(page: Page) {
  await page.evaluate(() => {
    localStorage.clear()
  })
}