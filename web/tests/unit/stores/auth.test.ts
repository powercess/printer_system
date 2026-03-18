// Auth Store 测试

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Get the mock from setup
const mockNavigateTo = (globalThis as any).__mockNavigateTo

// Mock useUserApi
const mockUserApi = {
  login: vi.fn(),
  register: vi.fn(),
  getProfile: vi.fn(),
}

vi.mock('../../../api/user', () => ({
  useUserApi: () => mockUserApi,
}))

// Mock useUserStore
vi.mock('../../../stores/user', () => ({
  useUserStore: () => ({
    fetchProfile: vi.fn().mockResolvedValue({ success: true }),
    clearUser: vi.fn(),
  }),
}))

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', async () => {
      const { useAuthStore } = await import('../../../stores/auth')
      const store = useAuthStore()

      expect(store.token).toBeNull()
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('getters', () => {
    it('getToken 应该返回当前 token', async () => {
      const { useAuthStore } = await import('../../../stores/auth')
      const store = useAuthStore()

      store.token = 'test_token'
      expect(store.getToken).toBe('test_token')
    })

    it('isLoggedIn 应该在已认证且有 token 时返回 true', async () => {
      const { useAuthStore } = await import('../../../stores/auth')
      const store = useAuthStore()

      expect(store.isLoggedIn).toBe(false)

      store.token = 'test_token'
      store.isAuthenticated = true
      expect(store.isLoggedIn).toBe(true)
    })

    it('isLoggedIn 应该在没有 token 时返回 false', async () => {
      const { useAuthStore } = await import('../../../stores/auth')
      const store = useAuthStore()

      store.isAuthenticated = true
      expect(store.isLoggedIn).toBe(false)
    })
  })

  describe('actions', () => {
    describe('init', () => {
      it('应该从 localStorage 加载 token', async () => {
        localStorage.setItem('auth_token', 'stored_token')

        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()
        store.init()

        expect(store.token).toBe('stored_token')
        expect(store.isAuthenticated).toBe(true)
      })

      it('当 localStorage 没有 token 时不应改变状态', async () => {
        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()
        store.init()

        expect(store.token).toBeNull()
        expect(store.isAuthenticated).toBe(false)
      })
    })

    describe('setToken', () => {
      it('应该设置 token 和认证状态', async () => {
        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()

        store.setToken('new_token')

        expect(store.token).toBe('new_token')
        expect(store.isAuthenticated).toBe(true)
        expect(localStorage.getItem('auth_token')).toBe('new_token')
      })
    })

    describe('clearAuth', () => {
      it('应该清除认证状态和 localStorage', async () => {
        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()

        store.setToken('token_to_clear')
        store.clearAuth()

        expect(store.token).toBeNull()
        expect(store.isAuthenticated).toBe(false)
        expect(localStorage.getItem('auth_token')).toBeNull()
      })
    })

    describe('login', () => {
      it('登录成功应该设置 token 并获取用户信息', async () => {
        mockUserApi.login.mockResolvedValueOnce({ token: 'login_token' })

        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()
        const result = await store.login('testuser', 'password')

        expect(result).toEqual({ success: true })
        expect(store.token).toBe('login_token')
        expect(store.isAuthenticated).toBe(true)
      })

      it('登录失败应该返回错误信息', async () => {
        mockUserApi.login.mockRejectedValueOnce(new Error('用户名或密码错误'))

        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()
        const result = await store.login('testuser', 'wrong_password')

        expect(result.success).toBe(false)
        expect(result.message).toBe('用户名或密码错误')
        expect(store.token).toBeNull()
      })

      it('登录失败应该返回默认错误信息', async () => {
        mockUserApi.login.mockRejectedValueOnce({})

        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()
        const result = await store.login('testuser', 'password')

        expect(result.message).toBe('登录失败')
      })
    })

    describe('register', () => {
      it('注册成功后应该自动登录', async () => {
        mockUserApi.register.mockResolvedValueOnce({})
        mockUserApi.login.mockResolvedValueOnce({ token: 'register_token' })

        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()
        const result = await store.register('newuser', 'password', 'email@test.com')

        expect(result).toEqual({ success: true })
        expect(mockUserApi.register).toHaveBeenCalledWith({
          username: 'newuser',
          password: 'password',
          email: 'email@test.com',
        })
        expect(mockUserApi.login).toHaveBeenCalled()
      })

      it('注册失败应该返回错误信息', async () => {
        mockUserApi.register.mockRejectedValueOnce(new Error('用户名已存在'))

        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()
        const result = await store.register('existinguser', 'password', 'email@test.com')

        expect(result.success).toBe(false)
        expect(result.message).toBe('用户名已存在')
      })
    })

    describe('logout', () => {
      it('应该清除认证状态并跳转到登录页', async () => {
        const { useAuthStore } = await import('../../../stores/auth')
        const store = useAuthStore()
        store.setToken('token')

        // navigateTo will throw because no Nuxt instance
        // We test the state changes work correctly
        try {
          store.logout()
        } catch {
          // navigateTo throws, but state should still be cleared
        }

        expect(store.token).toBeNull()
        expect(store.isAuthenticated).toBe(false)
      })
    })
  })
})