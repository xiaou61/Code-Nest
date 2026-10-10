import type { EntryDetail, EntryRef } from './types'

/**
 * 邻域视图里单个标签的最大长度：节点是示意图，不是阅读区。
 */
const MAX_LABEL = 10

type NeighbourKind = 'prerequisite' | 'dependent' | 'related'

interface Neighbour {
  ref: EntryRef
  kind: NeighbourKind
  label: string
}

/**
 * 三类关系**用线型区分，不用色相**。
 *
 * <p>理由有两条，第二条是硬约束：
 * <ol>
 *   <li>这套设计系统的层次是靠字号、留白与 1px 细线建立的，堆色阶反而是它要避免的做法。</li>
 *   <li>颜色令牌只有 Geist 那一档的二十来个，**没有 chart 色**。写一个不存在的类名不会报错，
 *       只会静默不生效——比报错更难发现。</li>
 * </ol>
 */
const LEGEND: readonly { kind: NeighbourKind; text: string; dash: string | undefined }[] = [
  { kind: 'prerequisite', text: '前置（本条依赖它）', dash: undefined },
  { kind: 'dependent', text: '依赖本条目', dash: '4 3' },
  { kind: 'related', text: '相关', dash: '1 3' },
]

function truncate(text: string): string {
  return text.length <= MAX_LABEL ? text : `${text.slice(0, MAX_LABEL)}…`
}

function dashOf(kind: NeighbourKind): string | undefined {
  return LEGEND.find((item) => item.kind === kind)?.dash
}

/**
 * 条目关系的邻域视图：当前条目居中，直接关联的条目环绕在四周。
 *
 * <p><b>为什么手写 SVG 而不引图布局库</b>：本版只需要回答"这条知识的前置与相关是什么"，
 * 圆环布局就够了；力导向布局回答的是"整个知识库长什么样"，那是另一个问题（全局图不在本版）。
 * 为此引一个图库等于用一个大依赖换十几行三角函数。
 *
 * <p>节点只显示截断后的标题且不可点击——真正的导航在正文下方的三组列表里，
 * 这张图只负责"一眼看清邻域结构"。
 */
export function KnowledgeNeighborhood({ entry }: { entry: EntryDetail }) {
  const neighbours: Neighbour[] = [
    ...entry.prerequisites.map((ref) => ({
      ref,
      kind: 'prerequisite' as const,
      label: truncate(ref.title),
    })),
    ...entry.dependents.map((ref) => ({
      ref,
      kind: 'dependent' as const,
      label: truncate(ref.title),
    })),
    ...entry.related.map((ref) => ({ ref, kind: 'related' as const, label: truncate(ref.title) })),
  ]

  if (neighbours.length === 0) {
    return null
  }

  const width = 640
  const height = 340
  const centreX = width / 2
  const centreY = height / 2
  const radiusX = width / 2 - 70
  const radiusY = height / 2 - 46
  const nodeWidth = 112
  const nodeHeight = 28

  return (
    <figure className="border-border my-8 rounded-md border p-4" data-testid="entry-neighbourhood">
      <figcaption className="text-muted-foreground mb-2 text-xs">
        邻域视图：中心是当前条目，四周是直接关联
      </figcaption>

      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="h-auto w-full"
        role="img"
        aria-label="条目关系邻域"
      >
        {neighbours.map((neighbour, index) => {
          const angle = (2 * Math.PI * index) / neighbours.length - Math.PI / 2
          const x = centreX + radiusX * Math.cos(angle)
          const y = centreY + radiusY * Math.sin(angle)
          return (
            <g key={`${neighbour.kind}-${neighbour.ref.id}`}>
              <line
                x1={centreX}
                y1={centreY}
                x2={x}
                y2={y}
                className="stroke-border"
                strokeWidth={neighbour.kind === 'prerequisite' ? 1.5 : 1}
                strokeDasharray={dashOf(neighbour.kind)}
              />
              <rect
                x={x - nodeWidth / 2}
                y={y - nodeHeight / 2}
                width={nodeWidth}
                height={nodeHeight}
                rx={6}
                className="fill-muted stroke-border"
                strokeWidth={1}
                strokeDasharray={dashOf(neighbour.kind)}
              />
              <text x={x} y={y + 4} textAnchor="middle" className="fill-foreground text-[9px]">
                {neighbour.label}
              </text>
            </g>
          )
        })}

        <rect
          x={centreX - 82}
          y={centreY - 20}
          width={164}
          height={40}
          rx={8}
          className="fill-card stroke-foreground"
          strokeWidth={1.5}
        />
        <text
          x={centreX}
          y={centreY + 4}
          textAnchor="middle"
          className="fill-card-foreground text-[10px] font-medium"
        >
          {truncate(entry.title)}
        </text>
      </svg>

      <div className="text-muted-foreground mt-2 flex flex-wrap gap-4 text-xs">
        {LEGEND.map((item) => (
          <span key={item.kind} className="flex items-center gap-1">
            <svg viewBox="0 0 24 8" className="h-2 w-6" aria-hidden="true">
              <line
                x1="0"
                y1="4"
                x2="24"
                y2="4"
                className="stroke-border"
                strokeWidth={item.kind === 'prerequisite' ? 1.5 : 1}
                strokeDasharray={item.dash}
              />
            </svg>
            {item.text}
          </span>
        ))}
      </div>
    </figure>
  )
}
