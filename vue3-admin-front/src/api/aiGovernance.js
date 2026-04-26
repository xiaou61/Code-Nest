import request from '@/utils/request'

export const aiGovernanceApi = {
  // AI治理总览
  getOverview() {
    return request.get('/admin/ai/governance/overview')
  }
}

export default aiGovernanceApi
