package com.xiaou.oj.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * OJ 赛事参赛者
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class OjContestParticipant {

    private Long id;

    /**
     * 赛事ID
     */
    private Long contestId;

    /**
     * 用户ID
     */
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime joinTime;
}
