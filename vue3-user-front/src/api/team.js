import request from '@/utils/request'

/**
 * 学习小组相关API
 */
export const teamApi = {
  // ==================== 小组管理 ====================

  // 创建小组
  createTeam(data) {
    return request.post('/user/team/create', data)
  },

  // 更新小组
  updateTeam(teamId, data) {
    return request.put(`/user/team/${teamId}`, data)
  },

  // 解散小组
  dissolveTeam(teamId) {
    return request.delete(`/user/team/${teamId}`)
  },

  // 获取小组详情
  getTeamDetail(teamId) {
    return request.get(`/user/team/${teamId}`)
  },

  // 获取小组列表（广场）
  getTeamList(data) {
    return request.post('/user/team/list', data)
  },

  // 获取我的小组
  getMyTeams() {
    return request.get('/user/team/my')
  },

  // 获取我创建的小组
  getCreatedTeams() {
    return request.get('/user/team/created')
  },

  // 获取推荐小组
  getRecommendTeams() {
    return request.get('/user/team/recommend')
  },

  // 获取邀请码
  getInviteCode(teamId) {
    return request.get(`/user/team/${teamId}/invite-code`)
  },

  // 刷新邀请码
  refreshInviteCode(teamId) {
    return request.post(`/user/team/${teamId}/invite-code/refresh`)
  },

  // 根据邀请码获取小组信息
  getTeamByInviteCode(inviteCode) {
    return request.get(`/user/team/by-code/${inviteCode}`)
  },

  // ==================== 成员管理 ====================

  // 申请加入小组
  applyJoin(teamId, data = {}) {
    return request.post(`/user/team/${teamId}/join`, data)
  },

  // 通过邀请码加入
  joinByInviteCode(inviteCode) {
    return request.post('/user/team/join-by-code', { inviteCode })
  },

  // 退出小组
  quitTeam(teamId) {
    return request.post(`/user/team/${teamId}/quit`)
  },

  // 获取成员列表
  getMemberList(teamId) {
    return request.get(`/user/team/${teamId}/members`)
  },

  // 获取申请列表（管理员）
  getApplicationList(teamId) {
    return request.get(`/user/team/${teamId}/applications`)
  },

  // 获取我的申请记录
  getMyApplications() {
    return request.get('/user/team/applications/my')
  },

  // 通过申请
  approveApplication(teamId, applicationId) {
    return request.post(`/user/team/${teamId}/application/${applicationId}/approve`)
  },

  // 拒绝申请
  rejectApplication(teamId, applicationId, rejectReason = '') {
    return request.post(`/user/team/${teamId}/application/${applicationId}/reject`, null, {
      params: { rejectReason }
    })
  },

  // 取消申请
  cancelApplication(applicationId) {
    return request.post(`/user/team/application/${applicationId}/cancel`)
  },

  // 移除成员
  removeMember(teamId, targetUserId) {
    return request.delete(`/user/team/${teamId}/member/${targetUserId}`)
  },

  // 设置成员角色
  setMemberRole(teamId, targetUserId, role) {
    return request.put(`/user/team/${teamId}/member/${targetUserId}/role`, null, {
      params: { role }
    })
  },

  // 转让组长
  transferLeader(teamId, newLeaderId) {
    return request.put(`/user/team/${teamId}/transfer`, null, {
      params: { newLeaderId }
    })
  },

  // 禁言成员
  muteMember(teamId, targetUserId, minutes = 60) {
    return request.post(`/user/team/${teamId}/member/${targetUserId}/mute`, null, {
      params: { minutes }
    })
  },

  // 解除禁言
  unmuteMember(teamId, targetUserId) {
    return request.delete(`/user/team/${teamId}/member/${targetUserId}/mute`)
  },

  // ==================== 任务管理 ====================

  // 创建任务
  createTask(teamId, data) {
    return request.post(`/user/team/${teamId}/task`, data)
  },

  // 更新任务
  updateTask(taskId, data) {
    return request.put(`/user/team/task/${taskId}`, data)
  },

  // 删除任务
  deleteTask(taskId) {
    return request.delete(`/user/team/task/${taskId}`)
  },

  // 设置任务状态
  setTaskStatus(taskId, status) {
    return request.put(`/user/team/task/${taskId}/status`, null, {
      params: { status }
    })
  },

  // 获取任务详情
  getTaskDetail(taskId) {
    return request.get(`/user/team/task/${taskId}`)
  },

  // 获取任务列表
  getTaskList(teamId, params = {}) {
    const { status = null } = params || {}
    return request.get(`/user/team/${teamId}/tasks`, {
      params: status !== null ? { status } : {}
    })
  },

  // 获取今日任务
  getTodayTasks(teamId) {
    return request.get(`/user/team/${teamId}/tasks/today`)
  },

  // ==================== 打卡 ====================

  // 打卡
  checkin(teamId, data) {
    return request.post(`/user/team/${teamId}/checkin`, data)
  },

  // 补卡
  supplementCheckin(teamId, data, date) {
    return request.post(`/user/team/${teamId}/checkin/supplement`, data, {
      params: { date }
    })
  },

  // 删除打卡
  deleteCheckin(checkinId) {
    return request.delete(`/user/team/checkin/${checkinId}`)
  },

  // 获取打卡详情
  getCheckinDetail(checkinId) {
    return request.get(`/user/team/checkin/${checkinId}`)
  },

  // 获取打卡列表
  getCheckinList(teamId, { taskId, page, pageNum, pageSize = 20 } = {}) {
    return request.get(`/user/team/${teamId}/checkins`, {
      params: { taskId, page: page ?? pageNum ?? 1, pageSize }
    })
  },

  // 获取我的打卡记录
  getMyCheckins(teamId, { startDate, endDate } = {}) {
    return request.get(`/user/team/${teamId}/checkins/my`, {
      params: { startDate, endDate }
    })
  },

  // 获取打卡日历
  getCheckinCalendar(teamId, year, month) {
    return request.get(`/user/team/${teamId}/checkin/calendar`, {
      params: { year, month }
    })
  },

  // 点赞打卡
  likeCheckin(checkinId) {
    return request.post(`/user/team/checkin/${checkinId}/like`)
  },

  // 取消点赞
  unlikeCheckin(checkinId) {
    return request.delete(`/user/team/checkin/${checkinId}/like`)
  },

  // 获取连续打卡天数
  getStreakDays(teamId, taskId = null) {
    return request.get(`/user/team/${teamId}/checkin/streak`, {
      params: taskId ? { taskId } : {}
    })
  },

  // 获取总打卡天数
  getTotalCheckinDays(teamId) {
    return request.get(`/user/team/${teamId}/checkin/total`)
  },

  // ==================== 排行榜 ====================

  // 获取打卡排行榜
  getCheckinRank(teamId, { type = 'total', limit = 20 } = {}) {
    return request.get(`/user/team/${teamId}/rank/checkin`, {
      params: { type, limit }
    })
  },

  // 获取连续打卡排行榜
  getStreakRank(teamId, limit = 20) {
    return request.get(`/user/team/${teamId}/rank/streak`, {
      params: { limit }
    })
  },

  // 获取学习时长排行榜
  getDurationRank(teamId, { type = 'total', limit = 20 } = {}) {
    return request.get(`/user/team/${teamId}/rank/duration`, {
      params: { type, limit }
    })
  },

  // 获取贡献值排行榜
  getContributionRank(teamId, limit = 20) {
    return request.get(`/user/team/${teamId}/rank/contribution`, {
      params: { limit }
    })
  },

  // 获取我的排名
  getMyRank(teamId, rankType = 'checkin') {
    return request.get(`/user/team/${teamId}/rank/my`, {
      params: { rankType }
    })
  },

  // ==================== 讨论 ====================

  // 创建讨论
  createDiscussion(teamId, data) {
    return request.post(`/user/team/${teamId}/discussion`, data)
  },

  // 更新讨论
  updateDiscussion(discussionId, data) {
    return request.put(`/user/team/discussion/${discussionId}`, data)
  },

  // 删除讨论
  deleteDiscussion(discussionId) {
    return request.delete(`/user/team/discussion/${discussionId}`)
  },

  // 获取讨论详情
  getDiscussionDetail(discussionId) {
    return request.get(`/user/team/discussion/${discussionId}`)
  },

  // 获取讨论列表
  getDiscussionList(teamId, { category, keyword, page, pageNum, pageSize = 20 } = {}) {
    return request.get(`/user/team/${teamId}/discussions`, {
      params: { category, keyword, page: page ?? pageNum ?? 1, pageSize }
    })
  },

  // 置顶/取消置顶
  setDiscussionTop(discussionId, isTop) {
    return request.put(`/user/team/discussion/${discussionId}/top`, null, {
      params: { isTop }
    })
  },

  // 设为精华/取消精华
  setDiscussionEssence(discussionId, isEssence) {
    return request.put(`/user/team/discussion/${discussionId}/essence`, null, {
      params: { isEssence }
    })
  },

  // 点赞讨论
  likeDiscussion(discussionId) {
    return request.post(`/user/team/discussion/${discussionId}/like`)
  },

  // 取消点赞讨论
  unlikeDiscussion(discussionId) {
    return request.delete(`/user/team/discussion/${discussionId}/like`)
  },

  // ==================== 统计 ====================

  // 获取小组统计概览
  getTeamStats(teamId) {
    return request.get(`/user/team/${teamId}/stats`)
  },

  // 获取每周统计
  getWeeklyStats(teamId) {
    return request.get(`/user/team/${teamId}/stats/weekly`)
  },

  // 获取每月统计
  getMonthlyStats(teamId, year, month) {
    return request.get(`/user/team/${teamId}/stats/monthly`, {
      params: { year, month }
    })
  },

  // 获取个人统计
  getMyStats(teamId) {
    return request.get(`/user/team/${teamId}/stats/my`)
  }
}

export default teamApi
