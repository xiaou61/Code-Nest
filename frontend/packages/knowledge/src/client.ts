import type { ApiClient } from '@paideia/core'

import type { CategoryNode, EntryDetail, EntrySummary, PageResult } from './types'

export interface ListEntriesOptions {
  /** 传父分类时后端会**包含其后代分类**的条目。 */
  categoryId?: number | null
  /** 检索串；长度不足 ngram 分词长度时后端自动退回 LIKE。 */
  q?: string
  page?: number
  size?: number
}

export interface KnowledgeApi {
  categories(): Promise<CategoryNode[]>
  entries(options?: ListEntriesOptions): Promise<PageResult<EntrySummary>>
  entry(id: number): Promise<EntryDetail>
}

/**
 * 知识库端点客户端。
 *
 * <p>只包一层路径与查询串拼装，鉴权、401 刷新重放都由传入的 `ApiClient`（装配自
 * `packages/auth`）负责——本包不认识令牌。
 */
export function createKnowledgeApi(client: ApiClient): KnowledgeApi {
  return {
    categories: () => client.requestEnvelope<CategoryNode[]>('/api/v1/knowledge/categories'),

    entries: (options = {}) => {
      const params = new URLSearchParams()
      if (options.categoryId !== undefined && options.categoryId !== null) {
        params.set('categoryId', String(options.categoryId))
      }
      const keyword = options.q?.trim() ?? ''
      if (keyword !== '') {
        params.set('q', keyword)
      }
      if (options.page !== undefined) {
        params.set('page', String(options.page))
      }
      if (options.size !== undefined) {
        params.set('size', String(options.size))
      }
      const query = params.toString()
      return client.requestEnvelope<PageResult<EntrySummary>>(
        `/api/v1/knowledge/entries${query === '' ? '' : `?${query}`}`,
      )
    },

    entry: (id) => client.requestEnvelope<EntryDetail>(`/api/v1/knowledge/entries/${id}`),
  }
}
