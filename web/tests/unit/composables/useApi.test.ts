// useApi composable 测试

import { describe, it, expect } from 'vitest'

describe('useApi', () => {
  // Note: useApi depends on useApiRequest which requires Nuxt instance
  // The handleApiCall logic can be tested with integration tests
  it.skip('handleApiCall 应该在成功时返回数据', () => {
    // This test requires Nuxt instance
    // Can be tested with integration tests using @nuxt/test-utils
  })
})