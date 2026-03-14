import request from '@/utils/request'

export const learningAssetApi = {
  convert(data) {
    return request.post('/user/learning-assets/convert', data)
  },

  getRecords(params = {}) {
    return request.post('/user/learning-assets/records/list', params)
  },

  getRecordDetail(recordId) {
    return request.get(`/user/learning-assets/records/${recordId}`)
  },

  updateCandidate(candidateId, data) {
    return request.put(`/user/learning-assets/candidates/${candidateId}`, data)
  },

  confirm(recordId, candidateIds) {
    return request.post(`/user/learning-assets/records/${recordId}/confirm`, {
      candidateIds
    })
  },

  discardCandidate(candidateId) {
    return request.post(`/user/learning-assets/candidates/${candidateId}/discard`)
  },

  publish(recordId, candidateIds = []) {
    return request.post(`/user/learning-assets/records/${recordId}/publish`, {
      candidateIds
    })
  },

  retry(recordId) {
    return request.post(`/user/learning-assets/records/${recordId}/retry`)
  }
}

export default learningAssetApi
