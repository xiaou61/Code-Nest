import { computed, reactive, ref, toRaw, type Ref } from 'vue'
import type { CnPagination } from '../types/components'

type CnTableQuery = Record<string, unknown>

export type CnTableResultData<T> =
  | T[]
  | {
      records?: T[]
      list?: T[]
      rows?: T[]
      total?: number
      [key: string]: unknown
    }

export type CnTableFetcherResult<T> =
  | CnTableResultData<T>
  | {
      data?: CnTableResultData<T>
      records?: T[]
      list?: T[]
      rows?: T[]
      total?: number
      [key: string]: unknown
    }

export interface UseCnTableOptions<T, Q extends CnTableQuery = CnTableQuery> {
  query?: Q
  page?: number
  pageSize?: number
  pageSizes?: number[]
  immediate?: boolean
  fetcher: (params: Q & { pageNum: number; pageSize: number }) => Promise<CnTableFetcherResult<T>>
}

export function useCnTable<T, Q extends CnTableQuery = CnTableQuery>(
  options: UseCnTableOptions<T, Q>
) {
  const loading = ref(false)
  const error = ref<unknown>(null)
  const rows = ref([]) as Ref<T[]>
  const query = reactive({ ...(options.query ?? {}) }) as Q
  const pagination = reactive({
    page: options.page ?? 1,
    pageSize: options.pageSize ?? 10,
    total: 0,
    pageSizes: options.pageSizes ?? [10, 20, 50, 100]
  })

  const tablePagination = computed<CnPagination>(() => ({
    page: pagination.page,
    pageSize: pagination.pageSize,
    total: pagination.total,
    pageSizes: pagination.pageSizes
  }))

  const isEmpty = computed(() => !loading.value && rows.value.length === 0)

  async function reload() {
    loading.value = true
    error.value = null

    try {
      const params = {
        ...(toRaw(query) as Q),
        pageNum: pagination.page,
        pageSize: pagination.pageSize
      } as Q & { pageNum: number; pageSize: number }

      const result = await options.fetcher(params)
      const nextRows = readRows<T>(result)
      rows.value = nextRows
      pagination.total = readTotal(result, nextRows.length)
      return result
    } catch (caughtError) {
      error.value = caughtError
      throw caughtError
    } finally {
      loading.value = false
    }
  }

  function setQuery(nextQuery: Partial<Q>) {
    Object.assign(query, nextQuery)
  }

  function search(nextQuery?: Partial<Q>) {
    if (nextQuery) {
      setQuery(nextQuery)
    }

    pagination.page = 1
    return reload()
  }

  function reset(nextQuery?: Partial<Q>) {
    for (const key of Object.keys(query as CnTableQuery)) {
      delete (query as CnTableQuery)[key]
    }

    Object.assign(query, options.query ?? {}, nextQuery ?? {})
    pagination.page = 1
    return reload()
  }

  function setPage(page: number) {
    pagination.page = page
    return reload()
  }

  function setPageSize(pageSize: number) {
    pagination.pageSize = pageSize
    pagination.page = 1
    return reload()
  }

  if (options.immediate !== false) {
    void reload().catch(() => undefined)
  }

  return {
    loading,
    error,
    rows,
    data: rows,
    query,
    pagination,
    tablePagination,
    isEmpty,
    reload,
    search,
    reset,
    setQuery,
    setPage,
    setPageSize
  }
}

function readRows<T>(source: unknown): T[] {
  if (Array.isArray(source)) {
    return source as T[]
  }

  if (!isRecord(source)) {
    return []
  }

  if (Array.isArray(source.records)) {
    return source.records as T[]
  }

  if (Array.isArray(source.list)) {
    return source.list as T[]
  }

  if (Array.isArray(source.rows)) {
    return source.rows as T[]
  }

  return readRows<T>(source.data)
}

function readTotal(source: unknown, fallback: number): number {
  if (Array.isArray(source)) {
    return source.length
  }

  if (!isRecord(source)) {
    return fallback
  }

  const total = Number(source.total)
  if (Number.isFinite(total)) {
    return total
  }

  return readTotal(source.data, fallback)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
