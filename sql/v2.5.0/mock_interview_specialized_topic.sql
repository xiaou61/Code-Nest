-- v2.5.0: 保存用户确认的专项模拟面试技术关注点。
-- 只保存短主题，不保存 JD、简历、完整提示词或模型上下文。
ALTER TABLE mock_interview_session
    ADD COLUMN specialized_topic VARCHAR(120) NULL COMMENT '专项面试关注点（用户确认的短技术主题）'
    AFTER interview_type;
