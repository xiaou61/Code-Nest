package com.xiaou.oj.contest;

import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.domain.OjContestParticipant;
import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.dto.contest.ContestRankingItem;
import com.xiaou.oj.enums.SubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContestRankingCalculatorTest {

    private final ContestRankingCalculator calculator = new ContestRankingCalculator();

    @Test
    void rankingShouldSortBySolvedThenPenaltyThenLastAcTime() {
        LocalDateTime start = LocalDateTime.of(2026, 2, 23, 14, 0, 0);
        OjContest contest = new OjContest()
                .setId(1L)
                .setStatus(2)
                .setStartTime(start)
                .setEndTime(start.plusHours(2));

        List<OjContestParticipant> participants = List.of(
                new OjContestParticipant().setContestId(1L).setUserId(1001L),
                new OjContestParticipant().setContestId(1L).setUserId(1002L),
                new OjContestParticipant().setContestId(1L).setUserId(1003L)
        );

        List<OjSubmission> submissions = List.of(
                // user 1001: p1 AC at +10, p2 AC at +70 => penalty 80, lastAc +70
                submission(1L, 1L, 1001L, 1L, SubmissionStatus.ACCEPTED.getValue(), start.plusMinutes(10)),
                submission(2L, 1L, 1001L, 2L, SubmissionStatus.ACCEPTED.getValue(), start.plusMinutes(70)),
                // user 1002: p1 AC at +20, p2 AC at +60 => penalty 80, lastAc +60 (应排在 1001 前)
                submission(3L, 1L, 1002L, 1L, SubmissionStatus.ACCEPTED.getValue(), start.plusMinutes(20)),
                submission(4L, 1L, 1002L, 2L, SubmissionStatus.ACCEPTED.getValue(), start.plusMinutes(60)),
                // user 1003: 仅 1 题 AC
                submission(5L, 1L, 1003L, 1L, SubmissionStatus.ACCEPTED.getValue(), start.plusMinutes(5))
        );

        List<ContestRankingItem> ranking = calculator.calculate(contest, submissions, participants);

        assertEquals(1002L, ranking.get(0).getUserId());
        assertEquals(2, ranking.get(0).getSolvedCount());
        assertEquals(80L, ranking.get(0).getPenalty());

        assertEquals(1001L, ranking.get(1).getUserId());
        assertEquals(2, ranking.get(1).getSolvedCount());
        assertEquals(80L, ranking.get(1).getPenalty());

        assertEquals(1003L, ranking.get(2).getUserId());
        assertEquals(1, ranking.get(2).getSolvedCount());
        assertEquals(3, ranking.size());
    }

    private OjSubmission submission(Long id, Long contestId, Long userId, Long problemId, String status, LocalDateTime createTime) {
        return new OjSubmission()
                .setId(id)
                .setContestId(contestId)
                .setUserId(userId)
                .setProblemId(problemId)
                .setStatus(status)
                .setCreateTime(createTime);
    }
}
