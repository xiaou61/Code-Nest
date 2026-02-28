import request from '@/utils/request'

export function getDashboardOverview() {
  return request.get('/admin/dashboard/overview')
}
