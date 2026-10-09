import type { ReactNode } from 'react'

/**
 * 最简展示容器。目前只用来验证工作区与包引用链路，
 * 真正的设计系统在后续工作项按需求建立，不在这里提前铺开。
 */
export function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section
      style={{
        border: '1px solid #d0d7de',
        borderRadius: 8,
        padding: '12px 16px',
        margin: '12px 0',
      }}
    >
      <h2 style={{ fontSize: 15, margin: '0 0 8px' }}>{title}</h2>
      {children}
    </section>
  )
}
