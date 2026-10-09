import type { Platform } from '@paideia/core'
import { createContext, useContext, type ReactNode } from 'react'

const PlatformContext = createContext<Platform | null>(null)

/**
 * 平台能力只从上下文获取，业务组件不直接引用任何宿主 API。
 * 这样同一份组件代码在 Web 与桌面壳里都成立，替换宿主只改注入点。
 */
export function PlatformProvider({ platform, children }: { platform: Platform; children: ReactNode }) {
  return <PlatformContext.Provider value={platform}>{children}</PlatformContext.Provider>
}

export function usePlatform(): Platform {
  const platform = useContext(PlatformContext)
  if (platform === null) {
    throw new Error('缺少 PlatformProvider：平台能力必须通过 usePlatform() 获取')
  }
  return platform
}
