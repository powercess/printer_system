// 支付 API 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { usePaymentApi } from '../../../api/payment'

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

describe('usePaymentApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('create', () => {
    it('应该创建支付', async () => {
      const mockPayment = {
        id: 'pay_123',
        orderId: 'order_123',
        amount: 10.5,
        status: 'pending',
        payUrl: 'https://pay.example.com/pay_123',
      }
      mockRequest.post.mockResolvedValueOnce(mockPayment)

      const api = usePaymentApi()
      const result = await api.create({
        orderId: 'order_123',
        amount: 10.5,
        paymentMethod: 'alipay',
      })

      expect(result).toEqual(mockPayment)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/payment/create', {
        orderId: 'order_123',
        amount: 10.5,
        paymentMethod: 'alipay',
      })
    })
  })

  describe('getStatus', () => {
    it('应该获取支付状态', async () => {
      const mockStatus = {
        paymentId: 'pay_123',
        status: 'paid',
        paidAt: '2024-01-01T12:00:00Z',
      }
      mockRequest.get.mockResolvedValueOnce(mockStatus)

      const api = usePaymentApi()
      const result = await api.getStatus('pay_123')

      expect(result).toEqual(mockStatus)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/payment/status', { payment_id: 'pay_123' })
    })
  })
})