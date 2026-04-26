package com.xiaou.filestorage.controller.admin;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.filestorage.domain.FileMigration;
import com.xiaou.filestorage.service.FileMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员迁移管理控制器
 *
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/admin/file")
public class AdminMigrationController {

    @Autowired
    private FileMigrationService fileMigrationService;

    /**
     * 创建迁移任务
     *
     * @param request 迁移任务请求
     * @return 创建结果
     */
    @PostMapping("/migrate")
    @RequireAdmin
    public Result<Long> createMigrationTask(@RequestBody Map<String, Object> request) {
        try {
            Long sourceStorageId = Long.valueOf(request.get("sourceStorageId").toString());
            Long targetStorageId = Long.valueOf(request.get("targetStorageId").toString());
            String migrationType = (String) request.get("migrationType");
            String taskName = (String) request.get("taskName");
            Map<String, Object> filterParams = toObjectMap(request.get("filterParams"));

            Long migrationId = fileMigrationService.createMigrationTask(
                sourceStorageId, targetStorageId, migrationType, filterParams, taskName);

            if (migrationId != null) {
                return Result.success(migrationId);
            } else {
                return Result.error("创建迁移任务失败");
            }

        } catch (Exception e) {
            log.error("创建迁移任务失败: {}", e.getMessage(), e);
            return Result.error("创建迁移任务失败: " + e.getMessage());
        }
    }

    private Map<String, Object> toObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        rawMap.forEach((key, mapValue) -> {
            if (key != null) {
                result.put(key.toString(), mapValue);
            }
        });
        return result;
    }

    /**
     * 查询迁移任务状态
     *
     * @param id 任务ID
     * @return 任务信息
     */
    @GetMapping("/migration/{id}")
    @RequireAdmin
    public Result<FileMigration> getMigrationTask(@PathVariable("id") Long id) {
        try {
            FileMigration migration = fileMigrationService.getMigrationTask(id);
            if (migration != null) {
                return Result.success(migration);
            } else {
                return Result.error("迁移任务不存在");
            }
        } catch (Exception e) {
            log.error("查询迁移任务失败: {}", e.getMessage(), e);
            return Result.error("查询迁移任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询迁移任务列表
     *
     * @param status 任务状态
     * @param limit  限制数量
     * @return 任务列表
     */
    @GetMapping("/migrations")
    @RequireAdmin
    public Result<List<FileMigration>> listMigrationTasks(@RequestParam(value = "status", required = false) String status,
                                                         @RequestParam(value = "limit", defaultValue = "50") Integer limit) {
        try {
            List<FileMigration> migrations = fileMigrationService.listMigrationTasks(status, limit);
            return Result.success(migrations);
        } catch (Exception e) {
            log.error("查询迁移任务列表失败: {}", e.getMessage(), e);
            return Result.error("查询迁移任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 执行迁移任务
     *
     * @param id 任务ID
     * @return 执行结果
     */
    @PostMapping("/migration/{id}/execute")
    @RequireAdmin
    public Result<Boolean> executeMigration(@PathVariable("id") Long id) {
        try {
            boolean success = fileMigrationService.executeMigration(id);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("执行迁移任务失败");
            }
        } catch (Exception e) {
            log.error("执行迁移任务失败: {}", e.getMessage(), e);
            return Result.error("执行迁移任务失败: " + e.getMessage());
        }
    }

    /**
     * 停止迁移任务
     *
     * @param id 任务ID
     * @return 停止结果
     */
    @PutMapping("/migration/{id}/stop")
    @RequireAdmin
    public Result<Boolean> stopMigration(@PathVariable("id") Long id) {
        try {
            boolean success = fileMigrationService.stopMigration(id);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("停止迁移任务失败");
            }
        } catch (Exception e) {
            log.error("停止迁移任务失败: {}", e.getMessage(), e);
            return Result.error("停止迁移任务失败: " + e.getMessage());
        }
    }

    /**
     * 删除迁移任务
     *
     * @param id 任务ID
     * @return 删除结果
     */
    @DeleteMapping("/migration/{id}")
    @RequireAdmin
    public Result<Boolean> deleteMigrationTask(@PathVariable("id") Long id) {
        try {
            boolean success = fileMigrationService.deleteMigrationTask(id);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("删除迁移任务失败");
            }
        } catch (Exception e) {
            log.error("删除迁移任务失败: {}", e.getMessage(), e);
            return Result.error("删除迁移任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取迁移进度
     *
     * @param id 任务ID
     * @return 进度信息
     */
    @GetMapping("/migration/{id}/progress")
    @RequireAdmin
    public Result<Map<String, Object>> getMigrationProgress(@PathVariable("id") Long id) {
        try {
            Map<String, Object> progress = fileMigrationService.getMigrationProgress(id);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取迁移进度失败: {}", e.getMessage(), e);
            return Result.error("获取迁移进度失败: " + e.getMessage());
        }
    }
}
