package io.github.xiaou61.knowledge.internal.file;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件的上传与读取用例。
 *
 * <p><b>校验顺序是"先校验后落盘"</b>：类型、大小、魔数都在写文件之前完成，这样磁盘上
 * 不会留下半截或被拒的文件。落盘成功后才插表，插表失败则删掉刚写的文件。
 *
 * <p><b>三重校验缺一不可</b>：大小（防塞满磁盘）、魔数（防改名的伪文件）、扩展名与魔数
 * 是否自洽（防"内容是 png 却叫 .svg"这类边界混淆）。只信扩展名或只信客户端声明的
 * Content-Type 都等于没校验。
 */
@Service
public class FileService {

    private final FileStorage storage;
    private final StoredFileMapper storedFileMapper;
    private final KnowledgeStorageProperties properties;

    FileService(FileStorage storage, StoredFileMapper storedFileMapper, KnowledgeStorageProperties properties) {
        this.storage = storage;
        this.storedFileMapper = storedFileMapper;
        this.properties = properties;
    }

    /** 上传结果。{@code url} 是**稳定标识**而不是绝对地址：前端渲染时再补 baseUrl。 */
    public record UploadedFile(String id, String url, String contentType, long size, String originalName) {
    }

    public record FileContent(byte[] content, String contentType) {
    }

    public UploadedFile upload(MultipartFile upload, Long uploaderId) {
        if (upload == null || upload.isEmpty()) {
            throw invalid("附件不能为空");
        }
        if (upload.getSize() > properties.getMaxFileSize()) {
            throw invalid("附件超过上限 " + (properties.getMaxFileSize() / 1024 / 1024) + " MB");
        }

        byte[] content;
        try {
            content = upload.getBytes();
        } catch (IOException exception) {
            throw new BizException(ErrorCode.INTERNAL, "读取上传内容失败");
        }

        String sniffed = MediaTypeSniffer.sniff(content);
        if (sniffed == null) {
            throw invalid("不支持的附件类型；只允许 png、jpeg、gif、webp、pdf");
        }
        String originalName = sanitizeName(upload.getOriginalFilename());
        if (!MediaTypeSniffer.allowedExtensions(sniffed).contains(extensionOf(originalName))) {
            throw invalid("文件内容与扩展名不符（内容是 " + sniffed + "）");
        }

        String id = storage.put(content, sniffed);
        StoredFile file = new StoredFile();
        file.setId(id);
        file.setOriginalName(originalName);
        file.setContentType(sniffed);
        file.setSizeBytes(content.length);
        file.setUploaderId(uploaderId);
        try {
            storedFileMapper.insert(file);
        } catch (DataAccessException exception) {
            // 尽力回滚磁盘上的文件：否则会留下一条谁也引不到、也没人知道该删的孤儿
            storage.delete(id);
            throw new BizException(ErrorCode.INTERNAL, "保存附件记录失败");
        }

        return new UploadedFile(id, "/api/v1/knowledge/files/" + id, sniffed, content.length, originalName);
    }

    public FileContent read(String id) {
        StoredFile file = storedFileMapper.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "附件不存在"));
        Optional<byte[]> content = storage.read(id);
        if (content.isEmpty()) {
            // 表里有记录但磁盘上没了（被运维清理、或换了实例）——按不存在处理并留痕，
            // 不要抛 500：这对调用方就是"这个附件没有了"
            throw new BizException(ErrorCode.NOT_FOUND, "附件文件缺失");
        }
        return new FileContent(content.get(), file.getContentType());
    }

    /** 只保留文件名本身：客户端可以传任意路径串进来，取末段避免出现目录痕迹。 */
    private static String sanitizeName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "attachment";
        }
        String name = raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? "attachment" : trimmed.substring(0, Math.min(trimmed.length(), 255));
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static BizException invalid(String message) {
        return new BizException(ErrorCode.INVALID_ARGUMENT, message);
    }
}
