import { useEffect, useMemo, useRef } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

import { extractHeadings } from './heading-slug'
import { FigureWithLightbox } from './image-lightbox'
import { resolveMediaUrl } from './media'

/**
 * markdown 正文渲染。
 *
 * <p><b>用 `react-markdown` 而不是 `marked` + 消毒库</b>：它把 markdown 转成 React 元素，
 * 不产生 HTML 字符串，因此没有 `dangerouslySetInnerHTML`、注入面在构造上就不存在；
 * 换成"先转 HTML 再消毒"要多保证一件事正确。
 *
 * <p><b>标题 id 在 effect 里回填，而不是在组件里用一个计数闭包。</b>两种写法的差别在
 * 重复渲染上：React 在开发模式下会渲染两遍，若 id 由一个"渲染期间自增"的闭包给出，
 * 第二遍会把序号继续往后推，于是目录里的锚点全部对不上（而且只在开发模式暴露）。
 * 回填是幂等的：同样的 markdown 永远得到同一批 id。
 *
 * <p>与本包 {@link import('./entry-toc').EntryToc} 共用 `extractHeadings`，
 * 因此目录与正文按**同一顺序**编号，重复标题也不会串。
 */
export function MarkdownBody({
  markdown,
  className,
  mediaBaseUrl,
}: {
  markdown: string
  className?: string
  /**
   * 正文里相对附件地址（`/api/v1/knowledge/files/...`）要拼上的后端基址。
   * 由调用方从自己的 `API_BASE_URL` 传进来——本包不认识部署形态，也不该认识。
   */
  mediaBaseUrl?: string
}) {
  const container = useRef<HTMLDivElement>(null)
  // 只依赖 markdown：同一份正文的标题序列是确定的
  const headings = useMemo(() => extractHeadings(markdown), [markdown])

  useEffect(() => {
    const nodes = container.current?.querySelectorAll('h1, h2, h3, h4')
    if (nodes === undefined || nodes === null) {
      return
    }
    nodes.forEach((node, index) => {
      const heading = headings[index]
      if (heading !== undefined) {
        node.id = heading.id
      }
    })
  }, [headings])

  return (
    <div
      ref={container}
      data-testid="markdown-body"
      className={className ?? 'text-sm leading-7'}
    >
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          h1: (props) => <h1 className="mt-8 mb-3 text-xl font-semibold" {...props} />,
          h2: (props) => <h2 className="mt-8 mb-3 text-lg font-semibold" {...props} />,
          h3: (props) => <h3 className="mt-6 mb-2 text-base font-semibold" {...props} />,
          h4: (props) => <h4 className="mt-6 mb-2 text-sm font-semibold" {...props} />,
          p: (props) => <p className="my-3" {...props} />,
          ul: (props) => <ul className="my-3 list-disc space-y-1 pl-6" {...props} />,
          ol: (props) => <ol className="my-3 list-decimal space-y-1 pl-6" {...props} />,
          blockquote: (props) => (
            <blockquote className="border-border text-muted-foreground my-4 border-l-2 pl-4" {...props} />
          ),
          a: (props) => (
            <a
              {...props}
              target="_blank"
              rel="noreferrer noopener"
              className="underline underline-offset-2"
            />
          ),
          // 代码块保持原样换行，避免长行把页面撑破
          pre: (props) => (
            <pre
              className="bg-muted text-muted-foreground my-4 overflow-x-auto rounded-md p-3 font-mono text-xs"
              {...props}
            />
          ),
          code: (props) => <code className="bg-muted rounded px-1 py-0.5 font-mono text-xs" {...props} />,
          table: (props) => (
            <div className="my-4 overflow-x-auto">
              <table className="w-full border-collapse text-xs" {...props} />
            </div>
          ),
          th: (props) => <th className="border-border border-b px-2 py-1 text-left font-medium" {...props} />,
          td: (props) => <td className="border-border border-b px-2 py-1" {...props} />,
          img: (props) => (
            <FigureWithLightbox
              src={resolveMediaUrl(typeof props.src === 'string' ? props.src : '', mediaBaseUrl)}
              alt={typeof props.alt === 'string' ? props.alt : ''}
            />
          ),
        }}
      >
        {markdown}
      </ReactMarkdown>
    </div>
  )
}
