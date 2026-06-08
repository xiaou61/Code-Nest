import type { Component } from 'vue'

export type CnSurface = 'transparent' | 'plain' | 'panel'

export type CnTone = 'brand' | 'success' | 'warning' | 'danger' | 'info' | 'neutral'

export type CnSize = 'sm' | 'md' | 'lg'

export interface CnPageProps {
  title?: string
  description?: string
  dense?: boolean
  fullHeight?: boolean
  surface?: CnSurface
  maxWidth?: string
}

export interface CnBreadcrumbItem {
  label: string
  to?: string
}

export interface CnPageHeaderProps {
  title: string
  description?: string
  eyebrow?: string
  breadcrumbs?: CnBreadcrumbItem[]
  compact?: boolean
}

export interface CnSectionProps {
  title?: string
  description?: string
  compact?: boolean
  surface?: CnSurface
  divided?: boolean
}

export type CnTrend = 'up' | 'down' | 'flat'

export interface CnStatCardProps {
  title: string
  value: string | number
  unit?: string
  description?: string
  trend?: CnTrend
  trendText?: string
  tone?: CnTone
  loading?: boolean
}

export interface CnStatusTagProps {
  label?: string
  type?: CnTone
  size?: CnSize
  dot?: boolean
  subtle?: boolean
}

export interface CnEmptyStateProps {
  title?: string
  description?: string
  icon?: string
  size?: CnSize
  surface?: CnSurface
}

export type CnFilterFieldType = 'input' | 'select' | 'date' | 'daterange' | 'switch' | 'custom'

export interface CnFilterOption {
  label: string
  value: string | number | boolean
  disabled?: boolean
}

export interface CnFilterField {
  prop: string
  label: string
  type: CnFilterFieldType
  placeholder?: string
  options?: CnFilterOption[]
  span?: number
  clearable?: boolean
  multiple?: boolean
  disabled?: boolean
  slot?: string
}

export interface CnFilterFormProps {
  modelValue: Record<string, unknown>
  fields: CnFilterField[]
  columns?: number
  loading?: boolean
  searchText?: string
  resetText?: string
}

export type CnTableColumnType = 'selection' | 'index' | 'expand'

export interface CnTableColumn<T = Record<string, unknown>> {
  prop?: keyof T | string
  label?: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: boolean | 'left' | 'right'
  type?: CnTableColumnType
  slot?: string
  formatter?: (row: T, column: CnTableColumn<T>, value: unknown, index: number) => string | number
  showOverflowTooltip?: boolean
  sortable?: boolean | 'custom'
}

export interface CnPagination {
  page: number
  pageSize: number
  total: number
  pageSizes?: number[]
  layout?: string
  background?: boolean
  disabled?: boolean
}

export interface CnDataTableProps<T = Record<string, unknown>> {
  columns: CnTableColumn<T>[]
  data: T[]
  loading?: boolean
  pagination?: CnPagination | null
  rowKey?: string
  stripe?: boolean
  border?: boolean
  emptyTitle?: string
  emptyDescription?: string
  emptyIcon?: string
}

export interface CnToolbarProps {
  title?: string
  description?: string
  dense?: boolean
  align?: 'start' | 'center' | 'end'
}

export interface CnThemeDrawerProps {
  modelValue: boolean
  title?: string
  description?: string
  size?: string
  showAdvancedThemes?: boolean
}

export type CnSidebarIcon = string | Component

export interface CnSidebarItem {
  label: string
  path?: string
  index?: string
  icon?: CnSidebarIcon
  children?: CnSidebarItem[]
}

export interface CnSidebarSearchResult {
  title: string
  path: string
  breadcrumb?: string
  icon?: CnSidebarIcon
}

export interface CnSidebarProps {
  items: CnSidebarItem[]
  activePath?: string
  collapsed?: boolean
  brand?: string
  collapsedBrand?: string
  searchable?: boolean
  searchKeyword?: string
  searchResults?: CnSidebarSearchResult[]
  searchPlaceholder?: string
  searchTooltip?: string
  ariaLabel?: string
  emptyText?: string
}

export interface CnCommandItem {
  path: string
  label: string
  desc?: string
  icon?: CnSidebarIcon
  matchPrefixes?: string[]
}

export interface CnCommandSection {
  key: string
  title: string
  items: CnCommandItem[]
}

export interface CnCommandPaletteProps {
  modelValue: boolean
  keyword?: string
  sections: CnCommandSection[]
  recentItems?: CnCommandItem[]
  activePath?: string
  title?: string
  description?: string
  placeholder?: string
  emptyTitle?: string
  emptyDescription?: string
  width?: string
}

export interface CnTopNavItem {
  path: string
  label: string
  desc?: string
  icon?: CnSidebarIcon
  matchPrefixes?: string[]
}

export interface CnTopNavGroup {
  title: string
  items: CnTopNavItem[]
}

export interface CnTopNavDropdown {
  key: string
  label: string
  icon?: CnSidebarIcon
  arrowIcon?: CnSidebarIcon
  groups?: CnTopNavGroup[]
  items?: CnTopNavItem[]
}

export interface CnTopNavMobileSection {
  key: string
  title: string
  items: CnTopNavItem[]
}

export interface CnTopNavUser {
  username?: string
  avatar?: string
}

export interface CnTopNavUserAction {
  command: string
  label: string
  icon?: CnSidebarIcon
  divided?: boolean
}

export interface CnTopNavProps {
  primaryItems: CnTopNavItem[]
  activePath?: string
  activeFullPath?: string
  dropdowns?: CnTopNavDropdown[]
  mobileSections?: CnTopNavMobileSection[]
  user?: CnTopNavUser | null
  userActions?: CnTopNavUserAction[]
  unreadCount?: number
  mobileOpen?: boolean
  scrolled?: boolean
  fixed?: boolean
  brand?: string
  subtitle?: string
  workspaceLabel?: string
  fallbackLabel?: string
  userCaption?: string
  searchLabel?: string
  searchShortcut?: string
  mobileSearchLabel?: string
  mobileMenuLabel?: string
  notificationLabel?: string
  mobileSearchTriggerLabel?: string
  mobileDescription?: string
  primaryMobileTitle?: string
  closeLabel?: string
  mobileDrawerSize?: string
  ariaLabel?: string
  showThemeSwitch?: boolean
}
