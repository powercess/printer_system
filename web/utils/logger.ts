/**
 * 日志工具 - 基于 Nuxt 官方 consola 日志库
 *
 * Nuxt 4 官方推荐使用 consola 作为日志库
 * 文档: https://github.com/unjs/consola
 */

import { consola } from 'consola'

// 日志级别映射
const LOG_LEVELS = {
  trace: 5,
  debug: 4,
  info: 3,
  success: 3,
  warn: 2,
  error: 1,
  fatal: 0,
  silent: -Infinity,
} as const

type LogLevel = keyof typeof LOG_LEVELS

// 是否为开发环境
const isDev = import.meta.dev

// 默认日志级别
const defaultLevel: LogLevel = isDev ? 'debug' : 'warn'

// 计时器存储
const timers = new Map<string, number>()

/**
 * 创建基础 logger
 */
function createBaseLogger(tag: string, level: LogLevel = defaultLevel) {
  const logger = consola.withTag(tag)
  logger.level = LOG_LEVELS[level]

  return {
    trace: (message: unknown, ...args: unknown[]) => logger.trace(message, ...args),
    debug: (message: unknown, ...args: unknown[]) => logger.debug(message, ...args),
    info: (message: unknown, ...args: unknown[]) => logger.info(message, ...args),
    success: (message: unknown, ...args: unknown[]) => logger.success(message, ...args),
    warn: (message: unknown, ...args: unknown[]) => logger.warn(message, ...args),
    error: (message: unknown, ...args: unknown[]) => logger.error(message, ...args),
    fatal: (message: unknown, ...args: unknown[]) => logger.fatal(message, ...args),
    time: (label: string) => timers.set(label, Date.now()),
    timeEnd: (label: string) => {
      const start = timers.get(label)
      if (start) {
        const duration = Date.now() - start
        timers.delete(label)
        logger.debug(`${label}: ${duration}ms`)
        return duration
      }
      return 0
    },
  }
}

/**
 * 创建通用 logger
 */
export function useLogger(module: string, level?: LogLevel) {
  return createBaseLogger(module, level)
}

/**
 * 全局默认 logger
 */
export const logger = useLogger('app')

/**
 * 创建 API 请求日志记录器
 */
export function createApiLogger(apiName: string) {
  const log = createBaseLogger(`API:${apiName}`)

  return {
    ...log,
    requestStart: (method: string, endpoint: string, params?: unknown) => {
      log.debug(`→ ${method.toUpperCase()} ${endpoint}`, params ?? '')
      log.time(`${method}:${endpoint}`)
    },
    requestSuccess: (method: string, endpoint: string, data?: unknown) => {
      log.timeEnd(`${method}:${endpoint}`)
      log.debug(`← ${method.toUpperCase()} ${endpoint} 成功`, data ?? '')
    },
    requestError: (method: string, endpoint: string, error: unknown) => {
      log.timeEnd(`${method}:${endpoint}`)
      log.error(`← ${method.toUpperCase()} ${endpoint} 失败:`, error)
    },
  }
}

/**
 * 创建 Store 日志记录器
 */
export function createStoreLogger(storeName: string) {
  const log = createBaseLogger(`Store:${storeName}`)

  return {
    ...log,
    actionStart: (actionName: string, payload?: unknown) => {
      log.debug(`▶ action: ${actionName}`, payload ?? '')
      log.time(actionName)
    },
    actionSuccess: (actionName: string, result?: unknown) => {
      log.timeEnd(actionName)
      log.success(`✓ action: ${actionName}`, result ?? '')
    },
    actionError: (actionName: string, error: unknown) => {
      log.timeEnd(actionName)
      log.error(`✗ action: ${actionName}`, error)
    },
    stateChange: (stateName: string, oldValue: unknown, newValue: unknown) => {
      log.trace(`↻ state: ${stateName}`, { from: oldValue, to: newValue })
    },
    init: (data?: unknown) => {
      log.info('Store 初始化', data ?? '')
    },
  }
}

/**
 * 创建页面日志记录器
 */
export function createPageLogger(pageName: string) {
  const log = createBaseLogger(`Page:${pageName}`)

  return {
    ...log,
    mounted: () => log.debug('页面已挂载'),
    unmounted: () => log.debug('页面已卸载'),
    userAction: (action: string, details?: unknown) => {
      log.info(`👤 用户操作: ${action}`, details ?? '')
    },
    loadStart: (resourceName: string) => {
      log.debug(`⏳ 加载: ${resourceName}`)
      log.time(resourceName)
    },
    loadSuccess: (resourceName: string, data?: unknown) => {
      log.timeEnd(resourceName)
      log.success(`✓ 加载完成: ${resourceName}`, data ?? '')
    },
    loadError: (resourceName: string, error: unknown) => {
      log.timeEnd(resourceName)
      log.error(`✗ 加载失败: ${resourceName}`, error)
    },
    formSubmit: (formName: string, data?: unknown) => {
      log.info(`📝 表单提交: ${formName}`, data ?? '')
    },
  }
}

/**
 * 创建中间件日志记录器
 */
export function createMiddlewareLogger(middlewareName: string) {
  const log = createBaseLogger(`Middleware:${middlewareName}`)

  return {
    ...log,
    execute: (from: string, to: string) => {
      log.debug(`执行: ${from} → ${to}`)
    },
    redirect: (from: string, to: string, reason?: string) => {
      log.info(`重定向: ${from} → ${to}`, reason ?? '')
    },
    allow: (path: string) => {
      log.debug(`放行: ${path}`)
    },
  }
}

// 导出 consola 供高级用法
export { consola }