-- v2.4.3: 记录首次引导选择的学习阶段，保证首周任务能在后续重排时保持同一重点。
ALTER TABLE `growth_autopilot_goal`
    ADD COLUMN `current_stage` VARCHAR(32) NOT NULL DEFAULT 'practice'
        COMMENT '当前学习阶段：foundation/practice/interview' AFTER `weekly_hours`;
