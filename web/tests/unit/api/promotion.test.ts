// 促销活动 API 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { usePromotionApi } from '../../../api/promotion'

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

describe('usePromotionApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getList', () => {
    it('应该获取活动列表', async () => {
      const mockPromotions = {
        total: 3,
        page: 1,
        pageSize: 10,
        items: [
          { id: 'promo_1', name: '新年特惠', discount: 0.9 },
          { id: 'promo_2', name: '学生优惠', discount: 0.8 },
        ],
      }
      mockRequest.get.mockResolvedValueOnce(mockPromotions)

      const api = usePromotionApi()
      const result = await api.getList({ page: 1, page_size: 10 })

      expect(result).toEqual(mockPromotions)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/promotion/list', { page: 1, page_size: 10 })
    })

    it('应该支持不带参数调用', async () => {
      const mockPromotions = { total: 0, page: 1, pageSize: 10, items: [] }
      mockRequest.get.mockResolvedValueOnce(mockPromotions)

      const api = usePromotionApi()
      await api.getList()

      expect(mockRequest.get).toHaveBeenCalledWith('/api/promotion/list', undefined)
    })
  })

  describe('validate', () => {
    it('应该验证活动码', async () => {
      const mockResponse = {
        valid: true,
        promotionId: 'promo_123',
        discount: 0.85,
        description: '新用户首单85折',
      }
      mockRequest.post.mockResolvedValueOnce(mockResponse)

      const api = usePromotionApi()
      const result = await api.validate({ code: 'NEWUSER85' })

      expect(result).toEqual(mockResponse)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/promotion/validate', { code: 'NEWUSER85' })
    })

    it('应该返回无效验证结果', async () => {
      const mockResponse = { valid: false, message: '活动码无效或已过期' }
      mockRequest.post.mockResolvedValueOnce(mockResponse)

      const api = usePromotionApi()
      const result = await api.validate({ code: 'INVALID' })

      expect(result).toEqual(mockResponse)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/promotion/validate', { code: 'INVALID' })
    })
  })
})