// API 基础请求封装测试

import { describe, it, expect, vi } from 'vitest'

describe('createApiError', () => {
  it('应该创建包含 statusCode 和 message 的错误对象', async () => {
    const { createApiError } = await import('../../../api/index')
    const error = createApiError(404, 'Not Found')

    expect(error.statusCode).toBe(404)
    expect(error.message).toBe('Not Found')
    expect(error.data).toBeUndefined()
  })

  it('应该支持可选的 data 参数', async () => {
    const { createApiError } = await import('../../../api/index')
    const data = { detail: 'Resource not found' }
    const error = createApiError(404, 'Not Found', data)

    expect(error.data).toEqual(data)
  })
})