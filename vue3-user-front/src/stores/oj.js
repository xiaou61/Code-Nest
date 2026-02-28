import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ojApi } from '@/api/oj'
import { cachedRequest, invalidateCache } from '@/utils/cache'

export const useOjStore = defineStore('oj', () => {
  // ================== 状态管理 ==================

  // 题目列表
  const problems = ref([])
  const problemsLoading = ref(false)
  const problemsTotal = ref(0)

  // 当前题目详情
  const currentProblem = ref({})
  const currentProblemLoading = ref(false)

  // 标签列表
  const tags = ref([])
  const tagsLoading = ref(false)

  // 提交记录
  const submissions = ref([])
  const submissionsLoading = ref(false)
  const submissionsTotal = ref(0)

  // 统计数据
  const statistics = ref({})
  const statisticsLoading = ref(false)

  // ================== 题目相关方法 ==================

  const fetchProblems = async (params = {}, options = {}) => {
    const cacheKey = `oj/problems?${JSON.stringify(params)}`
    problemsLoading.value = true
    try {
      const data = await cachedRequest(
        () => ojApi.getProblems(params),
        cacheKey,
        { ttl: 2 * 60 * 1000, force: options.forceRefresh, ...options }
      )
      problems.value = data?.records || []
      problemsTotal.value = data?.total || 0
      return data
    } catch (error) {
      console.error('获取题目列表失败:', error)
      throw error
    } finally {
      problemsLoading.value = false
    }
  }

  const fetchProblemDetail = async (id, options = {}) => {
    const cacheKey = `oj/problem/${id}`
    currentProblemLoading.value = true
    try {
      const data = await cachedRequest(
        () => ojApi.getProblemDetail(id),
        cacheKey,
        { ttl: 5 * 60 * 1000, ...options }
      )
      currentProblem.value = data || {}
      return data
    } catch (error) {
      console.error('获取题目详情失败:', error)
      throw error
    } finally {
      currentProblemLoading.value = false
    }
  }

  const fetchTags = async (options = {}) => {
    const cacheKey = 'oj/tags'
    tagsLoading.value = true
    try {
      const data = await cachedRequest(
        () => ojApi.getTags(),
        cacheKey,
        { ttl: 10 * 60 * 1000, ...options }
      )
      tags.value = data || []
      return data
    } catch (error) {
      console.error('获取标签列表失败:', error)
      throw error
    } finally {
      tagsLoading.value = false
    }
  }

  // ================== 提交相关方法 ==================

  const submitCode = async (data) => {
    try {
      const submissionId = await ojApi.submitCode(data)
      // 清理相关缓存
      invalidateCache(['oj/submissions', 'oj/statistics'])
      return submissionId
    } catch (error) {
      console.error('提交代码失败:', error)
      throw error
    }
  }

  const fetchSubmissionDetail = async (id) => {
    try {
      return await ojApi.getSubmissionDetail(id)
    } catch (error) {
      console.error('获取提交详情失败:', error)
      throw error
    }
  }

  const fetchMySubmissions = async (params = {}, options = {}) => {
    const cacheKey = `oj/submissions/my?${JSON.stringify(params)}`
    submissionsLoading.value = true
    try {
      const data = await cachedRequest(
        () => ojApi.getMySubmissions(params),
        cacheKey,
        { ttl: 1 * 60 * 1000, force: options.forceRefresh, ...options }
      )
      submissions.value = data?.records || []
      submissionsTotal.value = data?.total || 0
      return data
    } catch (error) {
      console.error('获取提交记录失败:', error)
      throw error
    } finally {
      submissionsLoading.value = false
    }
  }

  // ================== 统计相关方法 ==================

  const fetchStatistics = async (options = {}) => {
    const cacheKey = 'oj/statistics/me'
    statisticsLoading.value = true
    try {
      const data = await cachedRequest(
        () => ojApi.getMyStatistics(),
        cacheKey,
        { ttl: 2 * 60 * 1000, ...options }
      )
      statistics.value = data || {}
      return data
    } catch (error) {
      console.error('获取统计数据失败:', error)
      throw error
    } finally {
      statisticsLoading.value = false
    }
  }

  // ================== 工具方法 ==================

  const resetState = () => {
    problems.value = []
    currentProblem.value = {}
    tags.value = []
    submissions.value = []
    statistics.value = {}
    problemsTotal.value = 0
    submissionsTotal.value = 0
  }

  return {
    // 状态
    problems, problemsLoading, problemsTotal,
    currentProblem, currentProblemLoading,
    tags, tagsLoading,
    submissions, submissionsLoading, submissionsTotal,
    statistics, statisticsLoading,
    // 方法
    fetchProblems, fetchProblemDetail, fetchTags,
    submitCode, fetchSubmissionDetail, fetchMySubmissions,
    fetchStatistics, resetState
  }
})
