export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export type ApiId = number | string

export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  totalPages: number
  records: T[]
  hasNext: boolean
  hasPrevious: boolean
}

export type NotificationType = 'PERSONAL' | 'ANNOUNCEMENT' | 'COMMUNITY_INTERACTION' | 'SYSTEM'
export type NotificationStatus = 'UNREAD' | 'READ' | 'DELETED'
export type NotificationPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export interface NotificationRecord {
  id: ApiId
  title: string
  content: string
  type: NotificationType
  priority: NotificationPriority
  senderId: ApiId | null
  receiverId: ApiId | null
  sourceModule: string | null
  sourceId: string | null
  status: NotificationStatus
  readTime: string | null
  createdTime: string
  updatedTime: string | null
}

export interface NotificationStatistics {
  todayTotal: number
  monthTotal: number
  unreadTotal: number
  announcementCount: number
  personalCount: number
  communityCount: number
  systemCount: number
}

export interface NotificationTemplateRecord {
  id: ApiId
  code: string
  name: string
  titleTemplate: string
  contentTemplate: string
  isEnabled: boolean
  createdTime: string
  updatedTime: string | null
}

export interface NotificationTemplateCommand {
  code: string
  name: string
  titleTemplate: string
  contentTemplate: string
  isEnabled: boolean
}

export type ApiErrorKind =
  | 'authentication'
  | 'authorization'
  | 'validation'
  | 'not-found'
  | 'conflict'
  | 'rate-limit'
  | 'timeout'
  | 'unavailable'
  | 'server'
  | 'network'
  | 'business'
  | 'unknown'

export interface ApiErrorOptions {
  kind?: ApiErrorKind
  code?: number | null
  httpStatus?: number | null
  cause?: unknown
}

export class ApiError extends Error {
  readonly kind: ApiErrorKind
  readonly code: number | null
  readonly httpStatus: number | null
  readonly cause: unknown

  constructor(message: string, options?: ApiErrorOptions)
}

export function isApiResponse<T = unknown>(value: unknown): value is ApiResponse<T>
export function isSuccessfulHttpStatus(status: unknown): status is number
export function unwrapApiResponse<T>(
  body: ApiResponse<T> | T,
  options?: { httpStatus?: number | null }
): T
export function classifyApiFailure(error: unknown): ApiError
export function getApiErrorMessage(error: unknown, fallback?: string): string
export function normalizeQueryRequestConfig(
  input?: Record<string, unknown>,
  config?: Record<string, unknown>
): Record<string, unknown>
export function normalizeBodyRequestConfig(
  config?: Record<string, unknown>
): Record<string, unknown>
