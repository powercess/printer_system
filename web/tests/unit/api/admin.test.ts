// 管理员 API 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAdminApi } from '../../../api/admin'

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

describe('useAdminApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('createUser', () => {
    it('应该创建用户', async () => {
      const mockUser = {
        id: 1,
        username: 'newuser',
        email: 'newuser@example.com',
        role: 'user',
      }
      mockRequest.post.mockResolvedValueOnce(mockUser)

      const api = useAdminApi()
      const result = await api.createUser({
        username: 'newuser',
        password: 'password123',
        email: 'newuser@example.com',
      })

      expect(result).toEqual(mockUser)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/admin/user/create', {
        username: 'newuser',
        password: 'password123',
        email: 'newuser@example.com',
      })
    })

    it('应该支持创建管理员用户', async () => {
      const mockUser = {
        id: 2,
        username: 'admin',
        email: 'admin@example.com',
        role: 'admin',
      }
      mockRequest.post.mockResolvedValueOnce(mockUser)

      const api = useAdminApi()
      const result = await api.createUser({
        username: 'admin',
        password: 'admin123',
        email: 'admin@example.com',
        role: 'admin',
      })

      expect(result.role).toBe('admin')
    })
  })

  describe('getUserList', () => {
    it('应该获取用户列表', async () => {
      const mockUsers = {
        total: 10,
        page: 1,
        pageSize: 10,
        items: [
          { id: 1, username: 'user1', role: 'user' },
          { id: 2, username: 'admin1', role: 'admin' },
        ],
      }
      mockRequest.get.mockResolvedValueOnce(mockUsers)

      const api = useAdminApi()
      const result = await api.getUserList({ page: 1, role: 'admin' })

      expect(result).toEqual(mockUsers)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/admin/user/list', { page: 1, role: 'admin' })
    })

    it('应该支持搜索参数', async () => {
      const mockUsers = {
        total: 1,
        page: 1,
        pageSize: 10,
        items: [{ id: 1, username: 'testuser' }],
      }
      mockRequest.get.mockResolvedValueOnce(mockUsers)

      const api = useAdminApi()
      await api.getUserList({ search: 'test' })

      expect(mockRequest.get).toHaveBeenCalledWith('/api/admin/user/list', { search: 'test' })
    })
  })

  describe('updateUser', () => {
    it('应该更新用户', async () => {
      const mockUser = {
        id: 1,
        username: 'user1',
        email: 'newemail@example.com',
      }
      mockRequest.put.mockResolvedValueOnce(mockUser)

      const api = useAdminApi()
      const result = await api.updateUser({
        user_id: 1,
        email: 'newemail@example.com',
      })

      expect(result).toEqual(mockUser)
      expect(mockRequest.put).toHaveBeenCalledWith('/api/admin/user/update', {
        user_id: 1,
        email: 'newemail@example.com',
      })
    })
  })

  describe('deleteUser', () => {
    it('应该删除用户', async () => {
      mockRequest.delete.mockResolvedValueOnce({ success: true })

      const api = useAdminApi()
      const result = await api.deleteUser(1)

      expect(result).toEqual({ success: true })
      expect(mockRequest.delete).toHaveBeenCalledWith('/api/admin/user/delete', { user_id: 1 })
    })
  })

  describe('getFileList', () => {
    it('应该获取文件列表', async () => {
      const mockFiles = {
        total: 5,
        page: 1,
        pageSize: 10,
        items: [{ id: 1, fileName: 'doc.pdf' }],
      }
      mockRequest.get.mockResolvedValueOnce(mockFiles)

      const api = useAdminApi()
      const result = await api.getFileList({ user_id: 1 })

      expect(result).toEqual(mockFiles)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/admin/file/list', { user_id: 1 })
    })
  })

  describe('getOrderList', () => {
    it('应该获取订单列表', async () => {
      const mockOrders = {
        total: 20,
        page: 1,
        pageSize: 10,
        items: [{ id: 'order_1', status: 'completed' }],
      }
      mockRequest.get.mockResolvedValueOnce(mockOrders)

      const api = useAdminApi()
      const result = await api.getOrderList({ status: 'completed' })

      expect(result).toEqual(mockOrders)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/admin/order/list', { status: 'completed' })
    })
  })

  describe('getStats', () => {
    it('应该获取统计数据', async () => {
      const mockStats = {
        total_users: 100,
        total_orders: 500,
        total_revenue: 5000.0,
        today_orders: 15,
        today_revenue: 150.0,
      }
      mockRequest.get.mockResolvedValueOnce(mockStats)

      const api = useAdminApi()
      const result = await api.getStats()

      expect(result).toEqual(mockStats)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/admin/stats')
    })
  })
})