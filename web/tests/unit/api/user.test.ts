// 用户 API 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useUserApi } from '../../../api/user'

// Mock useApiRequest
const mockRequest = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
  upload: vi.fn(),
}

vi.mock('../../../api/index', () => ({
  useApiRequest: () => mockRequest,
}))

describe('useUserApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('register', () => {
    it('应该调用 POST /api/user/register', async () => {
      const mockUser = { id: 1, username: 'test', email: 'test@example.com' }
      mockRequest.post.mockResolvedValueOnce(mockUser)

      const api = useUserApi()
      const result = await api.register({
        username: 'test',
        password: 'password123',
        email: 'test@example.com',
      })

      expect(result).toEqual(mockUser)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/user/register', {
        username: 'test',
        password: 'password123',
        email: 'test@example.com',
      })
    })
  })

  describe('login', () => {
    it('应该调用 POST /api/user/login', async () => {
      const mockResponse = { token: 'abc123' }
      mockRequest.post.mockResolvedValueOnce(mockResponse)

      const api = useUserApi()
      const result = await api.login({
        username: 'test',
        password: 'password123',
      })

      expect(result).toEqual(mockResponse)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/user/login', {
        username: 'test',
        password: 'password123',
      })
    })
  })

  describe('getProfile', () => {
    it('应该调用 GET /api/user/profile', async () => {
      const mockUser = { id: 1, username: 'test', email: 'test@example.com' }
      mockRequest.get.mockResolvedValueOnce(mockUser)

      const api = useUserApi()
      const result = await api.getProfile()

      expect(result).toEqual(mockUser)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/user/profile')
    })
  })

  describe('updateProfile', () => {
    it('应该调用 PUT /api/user/profile', async () => {
      const mockUser = { id: 1, username: 'test', email: 'new@example.com' }
      mockRequest.put.mockResolvedValueOnce(mockUser)

      const api = useUserApi()
      const result = await api.updateProfile({ email: 'new@example.com' })

      expect(result).toEqual(mockUser)
      expect(mockRequest.put).toHaveBeenCalledWith('/api/user/profile', {
        email: 'new@example.com',
      })
    })
  })

  describe('getWalletBalance', () => {
    it('应该调用 GET /api/user/wallet/balance', async () => {
      mockRequest.get.mockResolvedValueOnce({ balance: 100 })

      const api = useUserApi()
      const result = await api.getWalletBalance()

      expect(result).toEqual({ balance: 100 })
      expect(mockRequest.get).toHaveBeenCalledWith('/api/user/wallet/balance')
    })
  })

  describe('recharge', () => {
    it('应该调用 POST /api/user/wallet/recharge', async () => {
      const mockTransaction = {
        id: 1,
        amount: 50,
        balanceBefore: 100,
        balanceAfter: 150,
        type: 0,
        relatedId: 'pay_123',
        createdAt: '2024-01-01T00:00:00Z',
      }
      mockRequest.post.mockResolvedValueOnce(mockTransaction)

      const api = useUserApi()
      const result = await api.recharge({
        amount: 50,
        payment_method: 'alipay',
      })

      expect(result).toEqual(mockTransaction)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/user/wallet/recharge', {
        amount: 50,
        payment_method: 'alipay',
      })
    })
  })

  describe('getTransactions', () => {
    it('应该调用 GET /api/user/wallet/transactions', async () => {
      const mockResponse = {
        total: 10,
        page: 1,
        pageSize: 10,
        items: [],
      }
      mockRequest.get.mockResolvedValueOnce(mockResponse)

      const api = useUserApi()
      const result = await api.getTransactions({ page: 1, page_size: 10 })

      expect(result).toEqual(mockResponse)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/user/wallet/transactions', {
        page: 1,
        page_size: 10,
      })
    })

    it('应该支持不带参数调用', async () => {
      const mockResponse = { total: 0, page: 1, pageSize: 10, items: [] }
      mockRequest.get.mockResolvedValueOnce(mockResponse)

      const api = useUserApi()
      await api.getTransactions()

      expect(mockRequest.get).toHaveBeenCalledWith('/api/user/wallet/transactions', undefined)
    })
  })
})