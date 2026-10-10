/**
 * 知识库共享包。
 *
 * <p>学习者端与管理端都用它：两个应用展示同一份内容、按同一条规则生成正文锚点，
 * 因此类型、客户端与 markdown 渲染只能有一份实现——另写一份就会在"管理端预览与实际
 * 展示不一致"上翻车，而预览的全部价值恰恰是"看到的就是学习者看到的"。
 *
 * <p>本包**不认识令牌**：鉴权、401 刷新重放都由传入的 `ApiClient` 负责。
 */

export { createKnowledgeAdminApi } from './admin-client'
export type {
  AdminEntryDetail,
  AdminEntrySummary,
  AdminRelation,
  CategoryInput,
  EntryInput,
  KnowledgeAdminApi,
} from './admin-client'
export { createKnowledgeApi } from './client'
export type { KnowledgeApi, ListEntriesOptions } from './client'
export { EntryToc } from './entry-toc'
export { MAX_HEADING_LEVEL, createSlugger, extractHeadings, slugify } from './heading-slug'
export type { Heading } from './heading-slug'
export { FigureWithLightbox } from './image-lightbox'
export { MarkdownBody } from './markdown-body'
export { KnowledgeNeighborhood } from './neighborhood'
export type {
  CategoryNode,
  EntryDetail,
  EntryRef,
  EntrySummary,
  PageResult,
} from './types'
