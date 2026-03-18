// 文件 API 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useFileApi } from '../../../api/file'

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

describe('useFileApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('upload', () => {
    it('应该上传文件并返回响应', async () => {
      const mockResponse = {
        file_id: 'file_123',
        file_name: 'test.pdf',
        file_path: '/uploads/test.pdf',
        file_size: 1024,
      }
      mockRequest.upload.mockResolvedValueOnce(mockResponse)

      const api = useFileApi()
      const file = new File(['test content'], 'test.pdf', { type: 'application/pdf' })
      const result = await api.upload(file)

      expect(result).toEqual(mockResponse)
      expect(mockRequest.upload).toHaveBeenCalledWith('/api/file/upload', file)
    })
  })

  describe('getList', () => {
    it('应该获取文件列表', async () => {
      const mockResponse = {
        total: 2,
        page: 1,
        pageSize: 10,
        items: [
          { id: 1, fileName: 'file1.pdf' },
          { id: 2, fileName: 'file2.pdf' },
        ],
      }
      mockRequest.get.mockResolvedValueOnce(mockResponse)

      const api = useFileApi()
      const result = await api.getList({ page: 1, page_size: 10 })

      expect(result).toEqual(mockResponse)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/file/list', { page: 1, page_size: 10 })
    })

    it('应该支持不带参数调用', async () => {
      const mockResponse = { total: 0, page: 1, pageSize: 10, items: [] }
      mockRequest.get.mockResolvedValueOnce(mockResponse)

      const api = useFileApi()
      await api.getList()

      expect(mockRequest.get).toHaveBeenCalledWith('/api/file/list', undefined)
    })
  })

  describe('getDetail', () => {
    it('应该获取文件详情', async () => {
      const mockDetail = {
        id: 1,
        fileName: 'test.pdf',
        filePath: '/uploads/test.pdf',
        fileSize: 1024,
        createdAt: '2024-01-01T00:00:00Z',
      }
      mockRequest.get.mockResolvedValueOnce(mockDetail)

      const api = useFileApi()
      const result = await api.getDetail('file_123')

      expect(result).toEqual(mockDetail)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/file/detail', { file_id: 'file_123' })
    })
  })

  describe('delete', () => {
    it('应该删除文件', async () => {
      mockRequest.delete.mockResolvedValueOnce({ success: true })

      const api = useFileApi()
      const result = await api.delete('file_123')

      expect(result).toEqual({ success: true })
      expect(mockRequest.delete).toHaveBeenCalledWith('/api/file/delete', { file_id: 'file_123' })
    })
  })
})