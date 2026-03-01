-- Code Nest 全量初始化数据脚本（由各版本 SQL 自动汇总）
-- 说明：
-- 1) 本脚本仅包含数据插入语句，不包含建表语句。
-- 2) 使用 INSERT IGNORE，支持重复执行。
-- 3) 需先执行 sql/MySql/code_nest.sql 建表脚本。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;

-- ==================== source: sql/v1.2.0/data.sql ====================
INSERT IGNORE INTO `sys_admin_role` VALUES (1, 1, 1, '2025-08-31 12:11:46', 1);
INSERT IGNORE INTO `sys_admin_role` VALUES (2, 2, 2, '2025-08-31 12:11:46', 1);

INSERT IGNORE INTO `sys_admin` VALUES (1, 'admin', '$2a$10$w9fmcYcV69fM7.01Ev6N8OwlUeShcb8Zc1B0uDNc6Q4s0z8CzvQVi', '超级管理员', 'admin@code-nest.com', '13800138000', NULL, 1, 0, '2025-09-03 11:17:28', '127.0.0.1', 11, '系统内置超级管理员账户', '2025-08-31 12:11:46', '2025-09-03 11:17:27', 1, NULL);
INSERT IGNORE INTO `sys_admin` VALUES (2, 'system', '$2a$10$w9fmcYcV69fM7.01Ev6N8OwlUeShcb8Zc1B0uDNc6Q4s0z8CzvQVi', '系统管理员', 'system@code-nest.com', '13800138001', NULL, 1, 0, NULL, NULL, 0, '系统内置系统管理员账户', '2025-08-31 12:11:46', '2025-08-31 12:28:30', 1, NULL);

INSERT IGNORE INTO `sys_role` VALUES (1, '超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有所有权限', 0, 1, '2025-08-31 12:11:46', '2025-08-31 12:11:46', NULL, NULL);
INSERT IGNORE INTO `sys_role` VALUES (2, '系统管理员', 'SYSTEM_ADMIN', '系统管理员，负责系统配置和用户管理', 0, 2, '2025-08-31 12:11:46', '2025-08-31 12:11:46', NULL, NULL);

INSERT IGNORE INTO `sys_role` VALUES (3, '普通管理员', 'ADMIN', '普通管理员，负责日常业务管理', 0, 3, '2025-08-31 12:11:46', '2025-08-31 12:11:46', NULL, NULL);


-- 插入默认本地存储配置
INSERT IGNORE INTO `storage_config` (`storage_type`, `config_name`, `config_params`, `is_default`, `is_enabled`, `test_status`) VALUES
    ('LOCAL', '本地存储', '{"basePath":"/uploads","urlPrefix":"http://localhost:9999/files"}', 1, 1, 1);

INSERT IGNORE INTO `file_system_setting` (`setting_key`, `setting_value`, `setting_desc`) VALUES
                                                                                       ('MAX_FILE_SIZE', '104857600', '最大文件大小限制(字节) - 100MB'),
                                                                                       ('ALLOWED_FILE_TYPES', '["jpg","jpeg","png","gif","bmp","webp","pdf","doc","docx","xls","xlsx","ppt","pptx","txt","zip","rar","7z","mp4","avi","mov","mp3","wav"]', '允许上传的文件类型白名单'),
                                                                                       ('AUTO_BACKUP_ENABLED', 'true', '是否自动创建本地备份'),
                                                                                       ('TEMP_LINK_EXPIRE_HOURS', '24', '临时链接有效期(小时)'),
                                                                                       ('STORAGE_QUOTA_PER_MODULE', '10737418240', '每个模块存储配额(字节) - 10GB');

INSERT IGNORE INTO notification_template (code, name, title_template, content_template) VALUES
                                                                                     ('WELCOME', '欢迎消息', '欢迎加入{platform}', '亲爱的{username}，欢迎加入我们的平台！'),
                                                                                     ('COMMUNITY_LIKE', '帖子点赞', '您的帖子收到点赞', '您的帖子《{postTitle}》收到了{likerName}的点赞'),
                                                                                     ('COMMUNITY_COMMENT', '帖子评论', '您的帖子收到评论', '您的帖子《{postTitle}》收到了{commenterName}的评论'),
                                                                                     ('INTERVIEW_FAVORITE', '面试题收藏', '收藏提醒', '您收藏的面试题《{questionTitle}》已更新'),
                                                                                     ('SYSTEM_MAINTENANCE', '系统维护', '系统维护通知', '系统将于{maintenanceTime}进行维护，预计耗时{duration}');

-- ==================== source: sql/v1.3.0/developer_calendar_v1.sql ====================
INSERT IGNORE INTO `developer_calendar_event` (`event_name`, `event_date`, `event_type`, `description`, `icon`, `color`, `is_major`, `blessing_text`) VALUES
('程序员节', '10-24', 1, '1024程序员节，因为1024是2的十次方，是一个特殊的数字，在信息技术中有特殊含义', 'code', '#722ED1', 1, '1024程序员节快乐！愿代码无Bug，愿人生多惊喜！'),
('世界编程日', '09-13', 1, '庆祝编程技术发展，激励更多人投身编程事业', 'terminal', '#13C2C2', 1, '世界编程日快乐！用代码改变世界，用技术创造未来！'),
('Java诞生日', '05-23', 2, '1995年5月23日，Java编程语言正式发布', 'coffee', '#F5A623', 1, '感谢Java为编程世界带来的贡献！Write Once, Run Anywhere!'),
('Python诞生日', '02-20', 2, '1991年2月20日，Python首个版本发布', 'python', '#4CAF50', 1, 'Python让编程变得简单而优雅！人生苦短，我用Python！'),
('GitHub上线日', '04-10', 3, '2008年4月10日，GitHub正式上线，改变了开源协作方式', 'github', '#24292E', 1, '感谢GitHub让全世界开发者能够协作共建！'),
('Linux诞生日', '08-25', 3, '1991年8月25日，Linus首次发布Linux内核', 'linux', '#FD7E14', 1, '致敬Linux！开源改变世界，自由万岁！'),
('JavaScript诞生日', '05-05', 2, '1995年5月，JavaScript在Netscape浏览器中首次亮相', 'javascript', '#F7DF1E', 0, 'JavaScript让Web变得生动有趣！'),
('C语言诞生日', '01-01', 2, '1972年，C语言在贝尔实验室诞生，影响深远的编程语言', 'c', '#00599C', 1, 'C语言是现代编程的基石！'),
('Git诞生日', '04-07', 3, '2005年4月7日，Linus创建了Git版本控制系统', 'git', '#F05032', 0, 'Git让代码版本管理变得简单高效！'),
('Stack Overflow上线日', '09-15', 3, '2008年9月15日，程序员问答社区Stack Overflow上线', 'stackoverflow', '#F47F24', 0, '感谢Stack Overflow帮助无数程序员解决问题！');

INSERT IGNORE INTO `daily_content` (`content_type`, `title`, `content`, `author`, `programming_language`, `tags`, `difficulty_level`) VALUES
(1, '简洁胜过复杂', '简单胜过复杂，复杂胜过混乱。', 'Tim Peters', 'Python', '["设计原则", "Python之禅"]', 1),
(1, '代码如诗', '代码应该写得像诗一样优美，让人读起来赏心悦目。', '佚名', NULL, '["代码质量", "编程哲学"]', 1),
(2, '使用有意义的变量名', '变量名应该清楚地表达其用途，避免使用a、b、c这样的无意义名称。', NULL, NULL, '["代码规范", "可读性"]', 1),
(2, '遵循单一职责原则', '一个函数或类应该只有一个引起变化的原因，专注于做好一件事。', NULL, NULL, '["设计模式", "SOLID原则"]', 2),
(3, 'Python列表推导式', '# 使用列表推导式生成平方数\nsquares = [x**2 for x in range(10)]\nprint(squares)  # [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]', NULL, 'Python', '["Python", "列表推导式"]', 1),
(3, 'JavaScript箭头函数', '// ES6箭头函数简化代码\nconst add = (a, b) => a + b;\nconst multiply = (a, b) => a * b;\nconsole.log(add(2, 3)); // 5', NULL, 'JavaScript', '["JavaScript", "ES6", "箭头函数"]', 1),
(4, '第一台电子计算机诞生', '1946年2月14日，世界上第一台通用电子计算机ENIAC在美国宾夕法尼亚大学诞生。', NULL, NULL, '["计算机历史", "ENIAC"]', 1),
(4, '万维网诞生', '1989年3月12日，蒂姆·伯纳斯-李提出了万维网(World Wide Web)的构想。', 'Tim Berners-Lee', NULL, '["互联网历史", "WWW"]', 1);

-- ==================== source: sql/v1.3.0/bug_store.sql ====================
INSERT IGNORE INTO `bug_item` (`title`, `phenomenon`, `cause_analysis`, `solution`, `tech_tags`, `difficulty_level`, `status`, `sort_order`, `created_by`) VALUES
('空指针异常 (NullPointerException)',
 '程序运行时抛出 java.lang.NullPointerException，通常发生在调用 null 对象的方法或访问其属性时。',
 '1. 对象未正确初始化\n2. 方法返回了 null 值但未做检查\n3. 集合或数组元素为 null\n4. 链式调用中某个环节返回 null',
 '1. 使用 null 检查：if (obj != null)\n2. 使用 Optional 类处理可能为 null 的值\n3. 初始化时给对象赋默认值\n4. 使用断言或参数验证\n5. IDE工具静态检查',
 'Java,异常处理,空指针', 1, 1, 100, 1),

('内存泄漏 (Memory Leak)',
 '程序运行时间越长，占用内存越来越多，最终可能导致内存溢出 OutOfMemoryError。',
 '1. 集合类持有对象引用未清理\n2. 监听器未注销\n3. 静态变量持有大对象\n4. 线程未正确关闭\n5. 缓存无限制增长',
 '1. 及时清理不再使用的对象引用\n2. 使用弱引用 WeakReference\n3. 监听器使用完后注销\n4. 限制缓存大小并设置过期策略\n5. 使用内存分析工具如 JProfiler',
 'Java,内存管理,性能优化', 3, 1, 95, 1),

('并发修改异常 (ConcurrentModificationException)',
 '在遍历集合时对集合进行修改操作，抛出 ConcurrentModificationException。',
 '多线程环境下或单线程中遍历时修改集合结构，导致迭代器的预期修改计数与实际不符。',
 '1. 使用迭代器的 remove() 方法删除元素\n2. 使用 ConcurrentHashMap 等线程安全集合\n3. 先收集要修改的元素，遍历结束后再修改\n4. 使用 Collections.synchronizedList() 包装',
 'Java,多线程,集合框架', 2, 1, 90, 1),

('死锁 (Deadlock)',
 '程序运行过程中多个线程相互等待对方释放资源，导致程序卡死无法继续执行。',
 '两个或多个线程按不同顺序获取多个锁资源，形成循环等待的情况。',
 '1. 统一锁的获取顺序\n2. 使用超时锁 tryLock(timeout)\n3. 减少锁的持有时间\n4. 避免嵌套锁\n5. 使用死锁检测工具',
 'Java,多线程,锁机制', 4, 1, 85, 1),

('栈溢出 (StackOverflowError)',
 '程序运行时抛出 java.lang.StackOverflowError，通常在递归调用时发生。',
 '1. 递归没有正确的终止条件\n2. 递归层次过深\n3. 方法调用链太长\n4. 局部变量占用空间过大',
 '1. 检查递归终止条件\n2. 优化递归算法，考虑使用迭代\n3. 增加JVM栈内存大小 -Xss\n4. 将递归改为尾递归优化',
 'Java,递归,内存管理', 2, 1, 80, 1);

-- ==================== source: sql/v1.4.0/chat_im.sql ====================
INSERT IGNORE INTO chat_rooms (room_name, room_type, description, status)
VALUES ('Code-Nest官方群组', 1, '欢迎来到Code-Nest官方聊天室，大家可以在这里自由交流！', 1);

-- ==================== source: sql/v1.5.0/community_upgrade.sql ====================
INSERT IGNORE INTO `community_tag` (`name`, `description`, `color`, `post_count`, `sort_order`, `status`) VALUES
('Java', 'Java编程语言相关', '#E53935', 0, 1, 1),
('Spring', 'Spring框架相关', '#43A047', 0, 2, 1),
('MySQL', 'MySQL数据库相关', '#1E88E5', 0, 3, 1),
('Redis', 'Redis缓存相关', '#D32F2F', 0, 4, 1),
('Vue', 'Vue.js前端框架', '#4CAF50', 0, 5, 1),
('算法', '算法与数据结构', '#FF9800', 0, 6, 1),
('面试', '面试经验分享', '#9C27B0', 0, 7, 1),
('架构', '系统架构设计', '#795548', 0, 8, 1),
('微服务', '微服务架构相关', '#00BCD4', 0, 9, 1),
('Docker', 'Docker容器技术', '#2196F3', 0, 10, 1),
('Kubernetes', 'K8s容器编排', '#326CE5', 0, 11, 1),
('前端', '前端开发相关', '#FF5722', 0, 12, 1),
('后端', '后端开发相关', '#3F51B5', 0, 13, 1),
('数据库', '数据库设计与优化', '#607D8B', 0, 14, 1),
('性能优化', '系统性能优化', '#FFC107', 0, 15, 1),
('分布式', '分布式系统', '#673AB7', 0, 16, 1),
('消息队列', 'MQ消息队列', '#009688', 0, 17, 1),
('安全', '系统安全相关', '#F44336', 0, 18, 1),
('运维', '系统运维相关', '#4CAF50', 0, 19, 1),
('职场', '职场经验分享', '#9E9E9E', 0, 20, 1);

-- ==================== source: sql/v1.5.0/lottery.sql ====================
INSERT IGNORE INTO lottery_prize_config (prize_name, prize_level, prize_points, base_probability, current_probability,
    target_return_rate, max_return_rate, min_return_rate, display_order, prize_desc, is_active) VALUES
('特等奖', 1, 1000, 0.00100000, 0.00100000, 0.0100, 0.0150, 0.0050, 1, '超级大奖！1000积分', 1),
('一等奖', 2, 500, 0.00500000, 0.00500000, 0.0250, 0.0300, 0.0200, 2, '幸运大奖！500积分', 1),
('二等奖', 3, 200, 0.02000000, 0.02000000, 0.0400, 0.0500, 0.0300, 3, '恭喜中奖！200积分', 1),
('三等奖', 4, 150, 0.05000000, 0.05000000, 0.0750, 0.0850, 0.0650, 4, '不错的奖品！150积分', 1),
('四等奖', 5, 100, 0.10000000, 0.10000000, 0.1000, 0.1100, 0.0900, 5, '保本奖！100积分', 1),
('五等奖', 6, 50, 0.30000000, 0.30000000, 0.1500, 0.1600, 0.1400, 6, '小奖励！50积分', 1),
('六等奖', 7, 20, 0.40000000, 0.40000000, 0.0800, 0.0900, 0.0700, 7, '安慰奖！20积分', 1),
('未中奖', 8, 0, 0.12400000, 0.12400000, 0.0000, 0.0000, 0.0000, 8, '很遗憾，再试一次吧！', 1);

-- ==================== source: sql/v1.5.0/blog.sql ====================
INSERT IGNORE INTO blog_category (category_name, category_icon, category_description, sort_order, status) VALUES
('Java', '☕', 'Java技术相关文章', 1, 1),
('Python', '🐍', 'Python技术相关文章', 2, 1),
('前端开发', '💻', '前端开发技术文章', 3, 1),
('数据库', '🗄️', '数据库相关文章', 4, 1),
('算法', '🧮', '算法与数据结构', 5, 1),
('系统设计', '🏗️', '系统架构设计', 6, 1),
('DevOps', '🚀', '运维和部署相关', 7, 1),
('其他', '📝', '其他技术文章', 99, 1)
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT IGNORE INTO blog_tag (tag_name, use_count) VALUES
('Spring Boot', 0),
('Vue', 0),
('React', 0),
('MySQL', 0),
('Redis', 0),
('Docker', 0),
('Kubernetes', 0),
('微服务', 0),
('分布式', 0),
('高并发', 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- ==================== source: sql/v1.5.0/sensitive_upgrade.sql ====================
INSERT IGNORE INTO `sensitive_strategy` (`strategy_name`, `module`, `level`, `action`, `notify_admin`, `limit_user`, `limit_duration`, `description`) VALUES
('社区低风险', 'community', 1, 'replace', 0, 0, NULL, '社区低风险敏感词-替换处理'),
('社区中风险', 'community', 2, 'replace', 1, 0, NULL, '社区中风险敏感词-替换处理并通知管理员'),
('社区高风险', 'community', 3, 'reject', 1, 1, 60, '社区高风险敏感词-拒绝发布并禁言1小时'),
('面试低风险', 'interview', 1, 'replace', 0, 0, NULL, '面试低风险敏感词-替换处理'),
('面试中风险', 'interview', 2, 'replace', 1, 0, NULL, '面试中风险敏感词-替换处理并通知管理员'),
('面试高风险', 'interview', 3, 'reject', 1, 1, 1440, '面试高风险敏感词-拒绝发布并禁言24小时'),
('朋友圈低风险', 'moment', 1, 'replace', 0, 0, NULL, '朋友圈低风险敏感词-替换处理'),
('朋友圈中风险', 'moment', 2, 'replace', 1, 0, NULL, '朋友圈中风险敏感词-替换处理并通知管理员'),
('朋友圈高风险', 'moment', 3, 'reject', 1, 1, 60, '朋友圈高风险敏感词-拒绝发布并禁言1小时');

INSERT IGNORE INTO `sensitive_homophone` (`original_char`, `homophone_chars`) VALUES
('傻', '沙,煞,啥'),
('逼', '比,币,鄙'),
('操', '草,曹,槽'),
('妈', '马,码'),
('你', '泥,拟');

INSERT IGNORE INTO `sensitive_similar_char` (`original_char`, `similar_chars`) VALUES
('草', '艹,屮'),
('日', '曰,囗'),
('毒', '毐'),
('爆', '暴');

-- ==================== source: sql/v1.6.3/mock_interview.sql ====================
INSERT IGNORE INTO mock_interview_direction (direction_code, direction_name, icon, description, sort_order, status) VALUES
('java', 'Java 后端', '☕', 'Java后端开发面试，涵盖Java基础、JVM、并发、Spring等', 1, 1),
('frontend', '前端开发', '🌐', '前端开发面试，涵盖HTML/CSS/JS、Vue/React、浏览器原理等', 2, 1),
('python', 'Python 开发', '🐍', 'Python开发面试，涵盖Python基础、Django/Flask、爬虫等', 3, 1),
('go', 'Go 开发', '🔷', 'Go语言开发面试，涵盖Go基础、并发编程、微服务等', 4, 1),
('fullstack', '全栈开发', '🔄', '全栈开发面试，涵盖前后端技术栈、系统设计等', 5, 1),
('database', '数据库', '🗄️', '数据库面试，涵盖MySQL、Redis、MongoDB等', 6, 1),
('devops', 'DevOps', '🔧', 'DevOps面试，涵盖Linux、Docker、K8s、CI/CD等', 7, 1),
('algorithm', '算法', '🧮', '算法面试，涵盖数据结构、算法思想、LeetCode等', 8, 1);

-- ==================== source: sql/v1.7.0/chat_upgrade.sql ====================
INSERT IGNORE INTO chat_sensitive_word (word, category, match_type, status) VALUES
('广告', 'spam', 1, 1),
('代理', 'spam', 1, 1),
('赌博', 'illegal', 1, 1);

-- ==================== source: sql/v1.8.0/oj_problems_data.sql ====================
INSERT IGNORE INTO `oj_problem` (`id`, `title`, `description`, `difficulty`, `time_limit`, `memory_limit`,
    `input_description`, `output_description`, `sample_input`, `sample_output`, `status`) VALUES

-- 1. 两数之和
(1, '两数之和',
'## 题目描述\n\n给定一个整数数组 `nums` 和一个整数目标值 `target`，请你在该数组中找出和为 `target` 的那两个整数，并返回它们的**下标**（从 0 开始）。\n\n你可以假设每种输入只会对应一个答案，并且不能使用同一个元素两次。\n\n## 提示\n\n- `2 <= nums.length <= 10^4`\n- `-10^9 <= nums[i] <= 10^9`\n- `-10^9 <= target <= 10^9`',
'easy', 2000, 256,
'第一行输入一个整数 `n`，表示数组长度。\n第二行输入 `n` 个整数，空格分隔。\n第三行输入目标值 `target`。',
'输出两个整数，表示下标，空格分隔，按升序输出。',
'4\n2 7 11 15\n9',
'0 1',
1),

-- 2. 反转字符串
(2, '反转字符串',
'## 题目描述\n\n给定一个字符串 `s`，将其反转后输出。\n\n## 提示\n\n- `1 <= s.length <= 10^5`\n- `s` 仅包含可打印的 ASCII 字符',
'easy', 1000, 128,
'输入一行字符串 `s`。',
'输出反转后的字符串。',
'hello',
'olleh',
1),

-- 3. 有效的括号
(3, '有效的括号',
'## 题目描述\n\n给定一个只包含 `(`、`)`、`{`、`}`、`[`、`]` 的字符串 `s`，判断字符串是否有效。\n\n有效字符串需满足：\n1. 左括号必须用相同类型的右括号闭合。\n2. 左括号必须以正确的顺序闭合。\n3. 每个右括号都有一个对应的相同类型的左括号。\n\n## 提示\n\n- `1 <= s.length <= 10^4`\n- `s` 仅由括号字符组成',
'easy', 1000, 128,
'输入一行括号字符串 `s`。',
'如果有效输出 `true`，否则输出 `false`。',
'()[]{}\n([)]',
'true\nfalse',
1),

-- 4. 最大子数组和
(4, '最大子数组和',
'## 题目描述\n\n给你一个整数数组 `nums`，请你找出一个具有最大和的连续子数组（至少包含一个元素），返回其最大和。\n\n## 提示\n\n- `1 <= nums.length <= 10^5`\n- `-10^4 <= nums[i] <= 10^4`',
'medium', 2000, 256,
'第一行输入一个整数 `n`，表示数组长度。\n第二行输入 `n` 个整数，空格分隔。',
'输出一个整数，表示最大子数组和。',
'9\n-2 1 -3 4 -1 2 1 -5 4',
'6',
1),

-- 5. 二分查找
(5, '二分查找',
'## 题目描述\n\n给定一个升序排列的整数数组 `nums` 和一个目标值 `target`，使用二分查找算法在数组中搜索 `target`。如果目标值存在，返回其下标（从 0 开始）；否则返回 `-1`。\n\n## 提示\n\n- `1 <= nums.length <= 10^4`\n- `-10^4 < nums[i], target < 10^4`\n- `nums` 中的所有元素互不相同\n- `nums` 已按升序排列',
'easy', 1000, 128,
'第一行输入一个整数 `n`，表示数组长度。\n第二行输入 `n` 个升序整数，空格分隔。\n第三行输入目标值 `target`。',
'输出目标值的下标，不存在则输出 `-1`。',
'6\n-1 0 3 5 9 12\n9',
'4',
1),

-- 6. 斐波那契数
(6, '斐波那契数',
'## 题目描述\n\n**斐波那契数**（通常用 `F(n)` 表示）形成的序列称为斐波那契数列。该数列由 `0` 和 `1` 开始，后面每一项都等于前两项之和。即：\n\n```\nF(0) = 0, F(1) = 1\nF(n) = F(n-1) + F(n-2)，其中 n > 1\n```\n\n给定 `n`，请计算 `F(n)`。\n\n## 提示\n\n- `0 <= n <= 45`',
'easy', 1000, 128,
'输入一个整数 `n`。',
'输出 `F(n)` 的值。',
'10',
'55',
1),

-- 7. 合并两个有序数组
(7, '合并两个有序数组',
'## 题目描述\n\n给你两个按非递减顺序排列的整数数组 `nums1` 和 `nums2`，请你将 `nums2` 合并到 `nums1` 中，使合并后的数组同样按非递减顺序排列，并输出合并后的数组。\n\n## 提示\n\n- `0 <= nums1.length, nums2.length <= 200`\n- `-10^9 <= nums1[i], nums2[j] <= 10^9`',
'easy', 1000, 128,
'第一行输入整数 `m`，表示第一个数组长度。\n第二行输入 `m` 个整数（若 m=0 则为空行）。\n第三行输入整数 `n`，表示第二个数组长度。\n第四行输入 `n` 个整数（若 n=0 则为空行）。',
'输出合并后的数组，空格分隔。',
'3\n1 2 3\n3\n2 5 6',
'1 2 2 3 5 6',
1),

-- 8. 爬楼梯
(8, '爬楼梯',
'## 题目描述\n\n假设你正在爬楼梯。需要 `n` 阶你才能到达楼顶。\n\n每次你可以爬 `1` 或 `2` 个台阶。你有多少种不同的方法可以爬到楼顶？\n\n## 提示\n\n- `1 <= n <= 45`',
'easy', 1000, 128,
'输入一个整数 `n`。',
'输出爬到楼顶的方法数。',
'3',
'3',
1),

-- 9. 最长递增子序列
(9, '最长递增子序列',
'## 题目描述\n\n给你一个整数数组 `nums`，找到其中最长严格递增子序列的长度。\n\n**子序列**是由数组派生而来的序列，删除（或不删除）数组中的元素而不改变其余元素的顺序。例如 `[3,6,2,7]` 是数组 `[0,3,1,6,2,2,7]` 的子序列。\n\n## 提示\n\n- `1 <= nums.length <= 2500`\n- `-10^4 <= nums[i] <= 10^4`',
'medium', 2000, 256,
'第一行输入一个整数 `n`，表示数组长度。\n第二行输入 `n` 个整数，空格分隔。',
'输出最长严格递增子序列的长度。',
'8\n10 9 2 5 3 7 101 18',
'4',
1),

-- 10. 零钱兑换
(10, '零钱兑换',
'## 题目描述\n\n给你一个整数数组 `coins`，表示不同面额的硬币，以及一个整数 `amount`，表示总金额。\n\n计算并返回可以凑成总金额所需的**最少的硬币个数**。如果没有任何一种硬币组合能组成总金额，返回 `-1`。\n\n你可以认为每种硬币的数量是无限的。\n\n## 提示\n\n- `1 <= coins.length <= 12`\n- `1 <= coins[i] <= 2^31 - 1`\n- `0 <= amount <= 10^4`',
'medium', 2000, 256,
'第一行输入一个整数 `n`，表示硬币种类数。\n第二行输入 `n` 个整数，表示各硬币面额，空格分隔。\n第三行输入总金额 `amount`。',
'输出最少硬币个数，无法凑成则输出 `-1`。',
'3\n1 2 5\n11',
'3',
1);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(1, '4\n2 7 11 15\n9', '0 1', 1, 1),
(1, '3\n3 2 4\n6', '1 2', 2, 0),
(1, '2\n3 3\n6', '0 1', 3, 0),
(1, '5\n1 5 3 7 2\n8', '1 4', 4, 0),
(1, '4\n-1 -2 -3 -4\n-6', '1 3', 5, 0);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(2, 'hello', 'olleh', 1, 1),
(2, 'world', 'dlrow', 2, 0),
(2, 'a', 'a', 3, 0),
(2, 'abcdef', 'fedcba', 4, 0),
(2, 'racecar', 'racecar', 5, 0);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(3, '()[]{}', 'true', 1, 1),
(3, '([)]', 'false', 2, 1),
(3, '()', 'true', 3, 0),
(3, '{[]}', 'true', 4, 0),
(3, '(', 'false', 5, 0),
(3, '((()))', 'true', 6, 0),
(3, ']{', 'false', 7, 0);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(4, '9\n-2 1 -3 4 -1 2 1 -5 4', '6', 1, 1),
(4, '1\n1', '1', 2, 0),
(4, '5\n5 4 -1 7 8', '23', 3, 0),
(4, '3\n-1 -2 -3', '-1', 4, 0),
(4, '6\n1 -1 1 -1 1 -1', '1', 5, 0);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(5, '6\n-1 0 3 5 9 12\n9', '4', 1, 1),
(5, '6\n-1 0 3 5 9 12\n2', '-1', 2, 0),
(5, '1\n5\n5', '0', 3, 0),
(5, '5\n1 3 5 7 9\n1', '0', 4, 0),
(5, '5\n1 3 5 7 9\n9', '4', 5, 0);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(6, '10', '55', 1, 1),
(6, '0', '0', 2, 0),
(6, '1', '1', 3, 0),
(6, '2', '1', 4, 0),
(6, '20', '6765', 5, 0),
(6, '30', '832040', 6, 0),
(6, '45', '1134903170', 7, 0);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(7, '3\n1 2 3\n3\n2 5 6', '1 2 2 3 5 6', 1, 1),
(7, '1\n1\n0\n', '1', 2, 0),
(7, '0\n\n1\n1', '1', 3, 0),
(7, '4\n1 3 5 7\n4\n2 4 6 8', '1 2 3 4 5 6 7 8', 4, 0),
(7, '3\n-3 -1 0\n3\n-2 1 2', '-3 -2 -1 0 1 2', 5, 0);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(8, '3', '3', 1, 1),
(8, '2', '2', 2, 0),
(8, '1', '1', 3, 0),
(8, '5', '8', 4, 0),
(8, '10', '89', 5, 0),
(8, '45', '1836311903', 6, 0);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(9, '8\n10 9 2 5 3 7 101 18', '4', 1, 1),
(9, '4\n0 1 0 3 2 3', '4', 2, 0),
(9, '1\n7', '1', 3, 0),
(9, '6\n1 3 6 7 9 4 10 5 6', '6', 4, 0),
(9, '5\n5 4 3 2 1', '1', 5, 0);

INSERT IGNORE INTO `oj_test_case` (`problem_id`, `input`, `expected_output`, `sort_order`, `is_sample`) VALUES
(10, '3\n1 2 5\n11', '3', 1, 1),
(10, '1\n2\n3', '-1', 2, 0),
(10, '1\n1\n0', '0', 3, 0),
(10, '3\n1 5 10\n27', '5', 4, 0),
(10, '2\n3 7\n11', '-1', 5, 0),
(10, '3\n1 2 5\n100', '20', 6, 0);

INSERT IGNORE INTO `oj_problem_tag_relation` (`problem_id`, `tag_id`) VALUES
-- 两数之和: 数组 + 哈希表
(1, 1), (1, 3),
-- 反转字符串: 字符串 + 双指针
(2, 2), (2, 4),
-- 有效的括号: 栈 + 字符串
(3, 6), (3, 2),
-- 最大子数组和: 数组 + 动态规划
(4, 1), (4, 12),
-- 二分查找: 数组 + 二分查找
(5, 1), (5, 11),
-- 斐波那契数: 数学 + 递归 + 动态规划
(6, 18), (6, 15), (6, 12),
-- 合并两个有序数组: 数组 + 排序 + 双指针
(7, 1), (7, 10), (7, 4),
-- 爬楼梯: 动态规划 + 数学
(8, 12), (8, 18),
-- 最长递增子序列: 数组 + 动态规划 + 二分查找
(9, 1), (9, 12), (9, 11),
-- 零钱兑换: 动态规划 + 数组
(10, 12), (10, 1);

-- ==================== source: sql/v1.8.0/oj_solution_table.sql ====================
INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(1, 'java', '哈希表解法',
'import java.util.*;

INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(2, 'java', '双指针 / StringBuilder',
'import java.util.Scanner;

INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(3, 'java', '栈匹配',
'import java.util.*;

INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(4, 'java', 'Kadane 算法',
'import java.util.Scanner;

INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(5, 'java', '标准二分查找',
'import java.util.Scanner;

INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(6, 'java', '迭代法',
'import java.util.Scanner;

INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(7, 'java', '双指针合并',
'import java.util.*;

INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(8, 'java', '动态规划',
'import java.util.Scanner;

INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(9, 'java', '动态规划 O(n²)',
'import java.util.Scanner;

INSERT IGNORE INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(10, 'java', '完全背包 DP',
'import java.util.*;

SET UNIQUE_CHECKS = 1;
SET FOREIGN_KEY_CHECKS = 1;

