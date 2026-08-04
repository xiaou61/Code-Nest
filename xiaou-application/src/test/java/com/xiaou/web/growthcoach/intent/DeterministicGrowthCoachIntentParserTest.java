package com.xiaou.web.growthcoach.intent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicGrowthCoachIntentParserTest {

    private final DeterministicGrowthCoachIntentParser parser = new DeterministicGrowthCoachIntentParser();

    @Test
    void extractsRemainingTimeTargetRoleAndInterviewPriorityFromNaturalLanguage() {
        GrowthCoachIntent intent = parser.parse("这周临时加班，只剩 3 小时，下周要面试 Java 后端岗位。");

        assertThat(intent.getAvailableMinutes()).isEqualTo(180);
        assertThat(intent.getTargetRole()).isEqualTo("Java 后端");
        assertThat(intent.isPrioritizeInterview()).isTrue();
    }

    @Test
    void leavesRoleEmptyWhenTheMessageDoesNotContainASupportedRolePhrase() {
        GrowthCoachIntent intent = parser.parse("这周我只能抽出 90 分钟，请先压缩任务。");

        assertThat(intent.getAvailableMinutes()).isEqualTo(90);
        assertThat(intent.getTargetRole()).isBlank();
        assertThat(intent.isPrioritizeInterview()).isFalse();
    }
}
