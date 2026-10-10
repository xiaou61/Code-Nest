import { Dialog, DialogContent, DialogTitle } from '@paideia/ui'
import { useState } from 'react'

/**
 * 图注 + 点击放大的图片。
 *
 * <p>放大复用 `@paideia/ui` 的 `Dialog` 而不是引灯箱库：本版只需要"点开看大图、能关掉"，
 * 而 Dialog 原语已经带来了焦点管理与 Esc 关闭——自己写这两件事比引库更容易做错。
 *
 * <p>图注取 markdown 图片的替代文本（`![图注](url)`）。替代文本对可访问性是必需的，
 * 这里顺手把它变成可见的图注，避免"写了 alt 但没人看得到"。
 */
export function FigureWithLightbox({ src, alt }: { src: string; alt: string }) {
  const [open, setOpen] = useState(false)

  if (src === '') {
    // 没有 src 的图片无可展示：宁可不渲染，也不留一个破图图标
    return null
  }

  return (
    <figure className="my-6" data-testid="knowledge-figure">
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="mx-auto block cursor-zoom-in"
        aria-label={alt === '' ? '放大图片' : `放大图片：${alt}`}
      >
        <img
          src={src}
          alt={alt}
          loading="lazy"
          className="border-border mx-auto max-h-96 w-auto max-w-full rounded-md border"
        />
      </button>
      {alt === '' ? null : (
        <figcaption className="text-muted-foreground mt-2 text-center text-xs">{alt}</figcaption>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="w-auto max-w-[92vw] sm:max-w-[92vw]">
          <DialogTitle className="sr-only">{alt === '' ? '图片' : alt}</DialogTitle>
          <img src={src} alt={alt} className="mx-auto max-h-[80vh] w-auto max-w-full" />
        </DialogContent>
      </Dialog>
    </figure>
  )
}
