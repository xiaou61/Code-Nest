package com.xiaou.oj.service;

import com.xiaou.oj.dto.RankingItem;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OJ排行榜服务
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class OjRankingService {

    private final OjSubmissionMapper submissionMapper;
    private final UserInfoApiService userInfoApiService;

    private static final int RANKING_LIMIT = 50;

    public List<RankingItem> getRanking(String type) {
        List<RankingItem> items;
        if ("weekly".equals(type)) {
            String weekStart = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
            items = submissionMapper.selectRankingWeekly(weekStart, RANKING_LIMIT);
        } else {
            items = submissionMapper.selectRankingAll(RANKING_LIMIT);
        }

        // 填充排名序号
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setRank(i + 1);
        }

        // 批量填充用户信息
        if (!items.isEmpty()) {
            List<Long> userIds = items.stream().map(RankingItem::getUserId).collect(Collectors.toList());
            Map<Long, SimpleUserInfo> userInfoMap = userInfoApiService.getSimpleUserInfoBatch(userIds);
            for (RankingItem item : items) {
                SimpleUserInfo info = userInfoMap.get(item.getUserId());
                if (info != null) {
                    item.setNickname(info.getDisplayName());
                    item.setAvatar(info.getAvatar());
                } else {
                    item.setNickname("用户" + item.getUserId());
                }
            }
        }

        return items;
    }
}
