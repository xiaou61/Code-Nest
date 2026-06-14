package com.xiaou.moyu.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.moyu.dto.BugItemDto;
import com.xiaou.moyu.service.BugStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Bug商店控制器（用户端）
 *
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/moyu/bug-store")
@RequiredArgsConstructor
public class BugStoreController {
    
    private final BugStoreService bugStoreService;
    
    /**
     * 随机获取一个Bug
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取随机Bug")
    @PostMapping("/random")
    public Result<BugItemDto> getRandomBug() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();

        try {
            BugItemDto bugItem = bugStoreService.getRandomBug(userId);
            
            if (bugItem == null) {
                return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "暂无可用的Bug内容");
            }
            
            return Result.success(bugItem);
        } catch (Exception e) {
            log.error("获取随机Bug失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取随机Bug失败");
        }
    }
}
