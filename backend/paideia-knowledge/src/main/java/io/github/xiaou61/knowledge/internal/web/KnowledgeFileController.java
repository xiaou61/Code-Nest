package io.github.xiaou61.knowledge.internal.web;

import io.github.xiaou61.knowledge.internal.file.FileService;
import io.github.xiaou61.platform.ApiResponse;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件端点。
 *
 * <p>两个端点分属两条安全规则，路径上就看得出来：
 * <ul>
 *   <li>{@code POST /api/v1/knowledge/admin/files} —— 在 {@code /admin/} 之下，走管理员规则。</li>
 *   <li>{@code GET /api/v1/knowledge/files/{id}} —— **免鉴权**（用户裁决），规则里限定为 GET。</li>
 * </ul>
 * 读取之所以必须免鉴权：{@code <img src>} 不携带 {@code Authorization} 头，而本项目无 Cookie，
 * 要求登录就等于图片显示不出来。代价是未发布条目引用的图片一旦 URL 泄露即可被读，
 * 缓解是 id 不可枚举（UUID）且不提供目录列举——这条取舍已写进需求与测试计划的已知缺口。
 */
@RestController
@RequestMapping("/api/v1/knowledge")
class KnowledgeFileController {

    private final FileService fileService;

    KnowledgeFileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(path = "/admin/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<FileService.UploadedFile> upload(@RequestParam("file") MultipartFile file) {
        // 上传者只从令牌取；写路径要求主体确实是有效的账号标识
        return ApiResponse.ok(fileService.upload(file, CurrentUserId.require()));
    }

    /**
     * 读取附件字节。
     *
     * <p>不走统一响应包装：这是二进制内容，不是 JSON。返回的 {@code Content-Type} 用
     * **入库时嗅探到的类型**，并带 {@code nosniff} 禁止浏览器重新猜类型。
     *
     * <p>内容按 id 不可变，因此允许长缓存——这也是选择"端点公开"而非"前端转 blob"换来的好处之一。
     */
    @GetMapping("/files/{id}")
    ResponseEntity<byte[]> read(@PathVariable("id") String id) {
        FileService.FileContent content = fileService.read(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(content.content());
    }
}
