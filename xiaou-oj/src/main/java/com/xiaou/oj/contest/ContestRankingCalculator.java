package com.xiaou.oj.contest;

import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.domain.OjContestParticipant;
import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.dto.contest.ContestRankingItem;
import com.xiaou.oj.enums.SubmissionStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACM 赛事榜单计算器
 *
 * @author xiaou
 */
@Component
public class ContestRankingCalculator {

    private static final long WRONG_ANSWER_PENALTY_MINUTES = 20L;

    public List<ContestRankingItem> calculate(
            OjContest contest,
            List<OjSubmission> submissions,
            List<OjContestParticipant> participants) {
        Map<Long, UserStat> userStats = new HashMap<>();

        if (participants != null) {
            for (OjContestParticipant participant : participants) {
                userStats.computeIfAbsent(participant.getUserId(), ignore -> new UserStat());
            }
        }

        if (submissions != null) {
            List<OjSubmission> orderedSubmissions = new ArrayList<>(submissions);
            orderedSubmissions.sort(Comparator
                    .comparing(OjSubmission::getCreateTime, Comparator.nullsLast(LocalDateTime::compareTo))
                    .thenComparing(OjSubmission::getId, Comparator.nullsLast(Long::compareTo)));

            for (OjSubmission submission : orderedSubmissions) {
                if (submission.getUserId() == null || submission.getProblemId() == null) {
                    continue;
                }
                UserStat userStat = userStats.computeIfAbsent(submission.getUserId(), ignore -> new UserStat());
                ProblemStat problemStat = userStat.problemStats.computeIfAbsent(submission.getProblemId(), ignore -> new ProblemStat());

                if (problemStat.solved) {
                    continue;
                }

                if (SubmissionStatus.ACCEPTED.getValue().equals(submission.getStatus())) {
                    problemStat.solved = true;
                    userStat.solvedCount++;

                    long acMinutes = calculateMinutesFromStart(contest, submission.getCreateTime());
                    userStat.penalty += acMinutes + problemStat.wrongBeforeAc * WRONG_ANSWER_PENALTY_MINUTES;
                    userStat.lastAcTime = submission.getCreateTime();
                    continue;
                }

                if (SubmissionStatus.WRONG_ANSWER.getValue().equals(submission.getStatus())) {
                    problemStat.wrongBeforeAc++;
                }
            }
        }

        List<ContestRankingItem> ranking = new ArrayList<>();
        for (Map.Entry<Long, UserStat> entry : userStats.entrySet()) {
            ContestRankingItem item = new ContestRankingItem();
            item.setUserId(entry.getKey());
            item.setSolvedCount(entry.getValue().solvedCount);
            item.setPenalty(entry.getValue().penalty);
            item.setLastAcTime(entry.getValue().lastAcTime);
            ranking.add(item);
        }

        ranking.sort(
                Comparator.comparing(ContestRankingItem::getSolvedCount, Comparator.reverseOrder())
                        .thenComparing(ContestRankingItem::getPenalty)
                        .thenComparing(ContestRankingItem::getLastAcTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(ContestRankingItem::getUserId)
        );

        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setRank(i + 1);
        }
        return ranking;
    }

    private long calculateMinutesFromStart(OjContest contest, LocalDateTime submitTime) {
        if (contest == null || contest.getStartTime() == null || submitTime == null) {
            return 0L;
        }
        long minutes = Duration.between(contest.getStartTime(), submitTime).toMinutes();
        return Math.max(minutes, 0L);
    }

    private static class UserStat {
        private int solvedCount = 0;
        private long penalty = 0L;
        private LocalDateTime lastAcTime;
        private final Map<Long, ProblemStat> problemStats = new HashMap<>();
    }

    private static class ProblemStat {
        private boolean solved = false;
        private int wrongBeforeAc = 0;
    }
}
