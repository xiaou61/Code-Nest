import request from '@/utils/request'

export function getDashboardOverview() {
  return request.get('/admin/dashboard/overview')
}

export function getGrowthAnalyticsOverview(days = 7) {
  return request.get('/admin/growth-analytics/overview', { days })
}
