// 打印机 API 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { usePrinterApi } from '../../../api/printer'

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

describe('usePrinterApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getStatus', () => {
    it('应该获取打印机状态列表', async () => {
      const mockStatus = [
        { id: 1, name: 'Printer1', status: 'idle' },
        { id: 2, name: 'Printer2', status: 'printing' },
      ]
      mockRequest.get.mockResolvedValueOnce(mockStatus)

      const api = usePrinterApi()
      const result = await api.getStatus()

      expect(result).toEqual(mockStatus)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/printer/status')
    })
  })

  describe('getCupsList', () => {
    it('应该获取 CUPS 打印机列表', async () => {
      const mockPrinters = [
        { name: 'HP-LaserJet', isAcceptingJobs: true, state: 'idle' },
        { name: 'Canon-MG3600', isAcceptingJobs: false, state: 'stopped' },
      ]
      mockRequest.get.mockResolvedValueOnce(mockPrinters)

      const api = usePrinterApi()
      const result = await api.getCupsList()

      expect(result).toEqual(mockPrinters)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/printer/cups/list')
    })
  })

  describe('print', () => {
    it('应该发送打印任务', async () => {
      const mockResponse = { job_id: 123 }
      mockRequest.post.mockResolvedValueOnce(mockResponse)

      const api = usePrinterApi()
      const result = await api.print({
        file_path: '/uploads/test.pdf',
        printer_name: 'HP-LaserJet',
        copies: 2,
        color_mode: 'bw',
        duplex: true,
        paper_size: 'A4',
      })

      expect(result).toEqual(mockResponse)
      expect(mockRequest.post).toHaveBeenCalledWith('/api/printer/cups/print', {
        file_path: '/uploads/test.pdf',
        printer_name: 'HP-LaserJet',
        copies: 2,
        color_mode: 'bw',
        duplex: true,
        paper_size: 'A4',
      })
    })

    it('应该支持可选的 page_range 参数', async () => {
      const mockResponse = { job_id: 124 }
      mockRequest.post.mockResolvedValueOnce(mockResponse)

      const api = usePrinterApi()
      await api.print({
        file_path: '/uploads/test.pdf',
        printer_name: 'HP-LaserJet',
        copies: 1,
        color_mode: 'color',
        duplex: false,
        paper_size: 'A4',
        page_range: '1-5',
      })

      expect(mockRequest.post).toHaveBeenCalledWith('/api/printer/cups/print', {
        file_path: '/uploads/test.pdf',
        printer_name: 'HP-LaserJet',
        copies: 1,
        color_mode: 'color',
        duplex: false,
        paper_size: 'A4',
        page_range: '1-5',
      })
    })
  })

  describe('getJobs', () => {
    it('应该获取打印任务列表', async () => {
      const mockJobs = {
        total: 5,
        page: 1,
        pageSize: 10,
        items: [
          { id: 1, status: 'completed', printer: 'HP-LaserJet' },
          { id: 2, status: 'pending', printer: 'Canon-MG3600' },
        ],
      }
      mockRequest.get.mockResolvedValueOnce(mockJobs)

      const api = usePrinterApi()
      const result = await api.getJobs({ page: 1, page_size: 10 })

      expect(result).toEqual(mockJobs)
      expect(mockRequest.get).toHaveBeenCalledWith('/api/printer/cups/jobs', { page: 1, page_size: 10 })
    })

    it('应该支持不带参数调用', async () => {
      const mockJobs = { total: 0, page: 1, pageSize: 10, items: [] }
      mockRequest.get.mockResolvedValueOnce(mockJobs)

      const api = usePrinterApi()
      await api.getJobs()

      expect(mockRequest.get).toHaveBeenCalledWith('/api/printer/cups/jobs', undefined)
    })
  })
})