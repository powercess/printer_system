// User Store 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock useUserApi
const mockUserApi = {
  getProfile: vi.fn(),
  getWalletBalance: vi.fn(),
}

vi.mock('../../../api/user', () => ({
  useUserApi: () => mockUserApi,
}))

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', async () => {
      const { useUserStore } = await import('../../../stores/user')
      const store = useUserStore()

      expect(store.user).toBeNull()
      expect(store.balance).toBe(0)
    })
  })

  describe('getters', () => {
    it('getUser 应该返回当前用户', async () => {
      const { useUserStore } = await import('../../../stores/user')
      const store = useUserStore()

      const mockUser = { id: 1, username: 'test', groupId: 1, groupName: '普通用户' }
      store.user = mockUser

      expect(store.getUser).toEqual(mockUser)
    })

    it('getBalance 应该返回当前余额', async () => {
      const { useUserStore } = await import('../../../stores/user')
      const store = useUserStore()

      store.balance = 100.5
      expect(store.getBalance).toBe(100.5)
    })

    it('isAdmin 应该在用户 groupId 为 0 或 groupName 为管理员 时返回 true', async () => {
      const { useUserStore } = await import('../../../stores/user')
      const store = useUserStore()

      expect(store.isAdmin).toBe(false)

      // groupId 为 0 是管理员
      store.user = { id: 1, username: 'admin', groupId: 0, groupName: '管理员' } as any
      expect(store.isAdmin).toBe(true)

      // groupName 为 管理员 也是管理员
      store.user = { id: 2, username: 'admin2', groupId: 1, groupName: '管理员' } as any
      expect(store.isAdmin).toBe(true)

      // 普通用户
      store.user = { id: 3, username: 'user', groupId: 1, groupName: '普通用户' } as any
      expect(store.isAdmin).toBe(false)
    })

    it('username 应该返回用户名或空字符串', async () => {
      const { useUserStore } = await import('../../../stores/user')
      const store = useUserStore()

      expect(store.username).toBe('')

      store.user = { id: 1, username: 'testuser', nickname: 'Test', groupId: 1, groupName: '普通用户' } as any
      expect(store.username).toBe('Test') // 优先返回 nickname
    })
  })

  describe('actions', () => {
    describe('init', () => {
      it('应该从 localStorage 加载用户数据', async () => {
        const storedData = {
          user: { id: 1, username: 'stored_user', groupId: 1, groupName: '普通用户' },
          balance: 50,
        }
        localStorage.setItem('user_data', JSON.stringify(storedData))

        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()
        store.init()

        expect(store.user).toEqual(storedData.user)
        expect(store.balance).toBe(50)
      })

      it('当 localStorage 没有数据时不应改变状态', async () => {
        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()
        store.init()

        expect(store.user).toBeNull()
        expect(store.balance).toBe(0)
      })
    })

    describe('setUser', () => {
      it('应该设置用户信息并持久化到 localStorage', async () => {
        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()

        const mockUser = {
          id: 1,
          username: 'testuser',
          nickname: 'Test User',
          email: 'test@example.com',
          groupId: 1,
          groupName: '普通用户',
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
        }
        // setUser 需要显式传入 balance 参数
        store.setUser(mockUser, 100)

        expect(store.user).toEqual(mockUser)
        expect(store.balance).toBe(100)

        const stored = JSON.parse(localStorage.getItem('user_data') || '{}')
        expect(stored.user).toEqual(mockUser)
        expect(stored.balance).toBe(100)
      })

      it('不传入 balance 时应保持原有余额', async () => {
        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()
        store.balance = 50 // 先设置一个余额

        const mockUser = {
          id: 1,
          username: 'testuser',
          nickname: 'Test User',
          email: 'test@example.com',
          groupId: 1,
          groupName: '普通用户',
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
        }
        store.setUser(mockUser)

        expect(store.user).toEqual(mockUser)
        expect(store.balance).toBe(50) // 余额保持不变
      })
    })

    describe('updateBalance', () => {
      it('应该更新余额并同步到 user 对象', async () => {
        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()

        store.user = { id: 1, username: 'test', balance: 50 } as any
        store.updateBalance(75)

        expect(store.balance).toBe(75)
        expect(store.user?.balance).toBe(75)
      })

      it('当 user 为 null 时应该只更新 balance', async () => {
        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()

        store.updateBalance(100)

        expect(store.balance).toBe(100)
        expect(store.user).toBeNull()
      })
    })

    describe('clearUser', () => {
      it('应该清除用户数据和 localStorage', async () => {
        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()

        store.user = { id: 1, username: 'test' } as any
        store.balance = 100
        localStorage.setItem('user_data', JSON.stringify({ user: store.user, balance: 100 }))

        store.clearUser()

        expect(store.user).toBeNull()
        expect(store.balance).toBe(0)
        expect(localStorage.getItem('user_data')).toBeNull()
      })
    })

    describe('persist', () => {
      it('应该将当前状态持久化到 localStorage', async () => {
        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()

        store.user = { id: 1, username: 'test' } as any
        store.balance = 200
        store.persist()

        const stored = JSON.parse(localStorage.getItem('user_data') || '{}')
        expect(stored.user).toEqual({ id: 1, username: 'test' })
        expect(stored.balance).toBe(200)
      })
    })

    describe('fetchProfile', () => {
      it('成功获取用户信息应该更新 store', async () => {
        const mockUser = {
          id: 1,
          username: 'testuser',
          nickname: 'Test User',
          email: 'test@example.com',
          groupId: 1,
          groupName: '普通用户',
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
        }
        mockUserApi.getProfile.mockResolvedValueOnce(mockUser)
        // fetchProfile 会调用 fetchBalance，需要 mock
        mockUserApi.getWalletBalance.mockResolvedValueOnce({ balance: 150 })

        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()
        const result = await store.fetchProfile()

        expect(result).toEqual({ success: true, user: mockUser })
        expect(store.user).toEqual({ ...mockUser, balance: 150 })
        expect(store.balance).toBe(150)
      })

      it('获取失败应该返回错误信息', async () => {
        mockUserApi.getProfile.mockRejectedValueOnce(new Error('未授权'))

        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()
        const result = await store.fetchProfile()

        expect(result.success).toBe(false)
        expect(result.message).toBe('未授权')
      })

      it('获取失败应该返回默认错误信息', async () => {
        mockUserApi.getProfile.mockRejectedValueOnce({})

        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()
        const result = await store.fetchProfile()

        expect(result.message).toBe('获取用户信息失败')
      })
    })

    describe('fetchBalance', () => {
      it('成功获取余额应该更新 store', async () => {
        mockUserApi.getWalletBalance.mockResolvedValueOnce({ balance: 250 })

        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()
        const result = await store.fetchBalance()

        expect(result).toEqual({ success: true, balance: 250 })
        expect(store.balance).toBe(250)
      })

      it('获取失败应该返回错误信息', async () => {
        mockUserApi.getWalletBalance.mockRejectedValueOnce(new Error('网络错误'))

        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()
        const result = await store.fetchBalance()

        expect(result.success).toBe(false)
        expect(result.message).toBe('网络错误')
      })

      it('获取失败应该返回默认错误信息', async () => {
        mockUserApi.getWalletBalance.mockRejectedValueOnce({})

        const { useUserStore } = await import('../../../stores/user')
        const store = useUserStore()
        const result = await store.fetchBalance()

        expect(result.message).toBe('获取余额失败')
      })
    })
  })
})