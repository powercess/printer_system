// 订单 API 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useOrderApi } from '../../../api/order'

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

describe('useOrderApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('create', () => {
    it('应该创建订单', async () => {
      const mockOrder = {
        id: 'order_123',
        status: 'pending',
        totalAmount: 10.5,
      }
      mockRequest.post.mockResolvedValueOnce(mockOrder)

      const api = useOrderApi()
      const result = await api.create({
        fileId: 'file_123',
        printerName: 'HP-LaserJet',
        copies: 2,
        colorMode: 'bw',
        duplex: true,
        paperSize: 'A4',
      })

      expect(result).toEqual(mockOrder)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/order/create', {
        fileId: 'file_123',
        printerName: 'HP-LaserJet',
        copies: 2,
        colorMode: 'bw',
        duplex: true,
        paperSize: 'A4',
      })
    })
  })

  describe('estimate', () => {
    it('应该估算价格', async () => {
      const mockEstimate = { price: 5.0, pages: 10 }
      mockRequest.post.mockResolvedValueOnce(mockEstimate)

      const api = useOrderApi()
      const result = await api.estimate({
        fileId: 'file_123',
        copies: 1,
        colorMode: 'bw',
        duplex: true,
        paperSize: 'A4',
      })

      expect(result).toEqual(mockEstimate)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/order/estimate', {
        fileId: 'file_123',
        copies: 1,
        colorMode: 'bw',
        duplex: true,
        paperSize: 'A4',
      })
    })
  })

  describe('getList', () => {
    it('应该获取订单列表', async () => {
      const mockOrders = {
        total: 3,
        page: 1,
        pageSize: 10,
        items: [
          { id: 'order_1', status: 'completed' },
          { id: 'order_2', status: 'pending' },
        ],
      }
      mockRequest.get.mockResolvedValueOnce(mockOrders)

      const api = useOrderApi()
      const result = await api.getList({ page: 1, status: 'completed' })

      expect(result).toEqual(mockOrders)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/order/list', { page: 1, status: 'completed' })
    })

    it('应该支持不带参数调用', async () => {
      const mockOrders = { total: 0, page: 1, pageSize: 10, items: [] }
      mockRequest.get.mockResolvedValueOnce(mockOrders)

      const api = useOrderApi()
      await api.getList()

      expect(mockRequest.get).toHaveBeenCalledWith('/api/order/list', undefined)
    })
  })

  describe('getDetail', () => {
    it('应该获取订单详情', async () => {
      const mockOrder = {
        id: 'order_123',
        status: 'completed',
        totalAmount: 10.5,
        createdAt: '2024-01-01T00:00:00Z',
      }
      mockRequest.get.mockResolvedValueOnce(mockOrder)

      const api = useOrderApi()
      const result = await api.getDetail('order_123')

      expect(result).toEqual(mockOrder)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/order/detail', { order_id: 'order_123' })
    })
  })

  describe('cancel', () => {
    it('应该取消订单', async () => {
      const mockResponse = { success: true, refundAmount: 5.0 }
      mockRequest.post.mockResolvedValueOnce(mockResponse)

      const api = useOrderApi()
      const result = await api.cancel('order_123')

      expect(result).toEqual(mockResponse)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/order/cancel', { order_id: 'order_123' })
    })
  })
})