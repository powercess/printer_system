// 社区 API 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useCommunityApi } from '../../../api/community'

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

describe('useCommunityApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('share', () => {
    it('应该发布分享', async () => {
      const mockPost = {
        id: 'post_123',
        title: 'My Share',
        content: 'Sharing my document',
        likes: 0,
        createdAt: '2024-01-01T00:00:00Z',
      }
      mockRequest.post.mockResolvedValueOnce(mockPost)

      const api = useCommunityApi()
      const result = await api.share({
        title: 'My Share',
        content: 'Sharing my document',
        fileId: 'file_123',
      })

      expect(result).toEqual(mockPost)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/community/share', {
        title: 'My Share',
        content: 'Sharing my document',
        fileId: 'file_123',
      })
    })
  })

  describe('getList', () => {
    it('应该获取分享列表', async () => {
      const mockPosts = {
        total: 5,
        page: 1,
        pageSize: 10,
        items: [
          { id: 'post_1', title: 'Post 1' },
          { id: 'post_2', title: 'Post 2' },
        ],
      }
      mockRequest.get.mockResolvedValueOnce(mockPosts)

      const api = useCommunityApi()
      const result = await api.getList({ page: 1, page_size: 10 })

      expect(result).toEqual(mockPosts)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/community/list', { page: 1, page_size: 10 })
    })
  })

  describe('getMyShares', () => {
    it('应该获取我的分享', async () => {
      const mockPosts = {
        total: 2,
        page: 1,
        pageSize: 10,
        items: [{ id: 'post_1', title: 'My Post' }],
      }
      mockRequest.get.mockResolvedValueOnce(mockPosts)

      const api = useCommunityApi()
      const result = await api.getMyShares({ page: 1 })

      expect(result).toEqual(mockPosts)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/community/my', { page: 1 })
    })
  })

  describe('getDetail', () => {
    it('应该获取分享详情', async () => {
      const mockPost = {
        id: 'post_123',
        title: 'My Share',
        content: 'Content here',
        likes: 10,
      }
      mockRequest.get.mockResolvedValueOnce(mockPost)

      const api = useCommunityApi()
      const result = await api.getDetail('post_123')

      expect(result).toEqual(mockPost)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/community/detail', { post_id: 'post_123' })
    })
  })

  describe('like', () => {
    it('应该点赞', async () => {
      const mockResponse = { success: true, likes: 11 }
      mockRequest.post.mockResolvedValueOnce(mockResponse)

      const api = useCommunityApi()
      const result = await api.like('post_123')

      expect(result).toEqual(mockResponse)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/community/like', { post_id: 'post_123' })
    })
  })

  describe('unlike', () => {
    it('应该取消点赞', async () => {
      const mockResponse = { success: true, likes: 10 }
      mockRequest.post.mockResolvedValueOnce(mockResponse)

      const api = useCommunityApi()
      const result = await api.unlike('post_123')

      expect(result).toEqual(mockResponse)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/community/unlike', { post_id: 'post_123' })
    })
  })

  describe('delete', () => {
    it('应该删除分享', async () => {
      mockRequest.delete.mockResolvedValueOnce({ success: true })

      const api = useCommunityApi()
      const result = await api.delete('post_123')

      expect(result).toEqual({ success: true })
      expect(mockRequest.delete).toHaveBeenCalledWith('/api/community/delete', { post_id: 'post_123' })
    })
  })
})