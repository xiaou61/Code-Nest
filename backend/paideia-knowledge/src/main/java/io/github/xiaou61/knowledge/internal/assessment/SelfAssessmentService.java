package io.github.xiaou61.knowledge.internal.assessment;

import io.github.xiaou61.knowledge.internal.entry.Entry;
import io.github.xiaou61.knowledge.internal.entry.EntryMapper;
import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 自评的读写用例。
 *
 * <p>三条纪律：
 * <ul>
 *   <li><b>只能标记已发布的条目</b>：草稿对学习者按"不存在"处理（404），与只读浏览同一口径——
 *       否则"对草稿标记成功"就等于告诉他这里有个未发布的条目。</li>
 *   <li><b>user_id 只从调用方（令牌）来</b>，这里不接受任何"代表别人"的参数。</li>
 *   <li><b>取消标记是幂等的</b>：本来就没有标记也返回成功——用户点两下不该看到报错。</li>
 * </ul>
 */
@Service
public class SelfAssessmentService {

    private final SelfAssessmentMapper selfAssessmentMapper;
    private final EntryMapper entryMapper;

    SelfAssessmentService(SelfAssessmentMapper selfAssessmentMapper, EntryMapper entryMapper) {
        this.selfAssessmentMapper = selfAssessmentMapper;
        this.entryMapper = entryMapper;
    }

    public record AssessedItem(Long entryId, String title, String level, java.time.Instant updatedAt) {
    }

    @Transactional
    public void mark(Long userId, Long entryId, String level) {
        String normalized = normalizeLevel(level);
        requirePublishedEntry(entryId);
        selfAssessmentMapper.upsert(userId, entryId, normalized);
    }

    @Transactional
    public void clear(Long userId, Long entryId) {
        requirePublishedEntry(entryId);
        selfAssessmentMapper.delete(userId, entryId);
    }

    public List<AssessedItem> listMine(Long userId, String level) {
        String normalized = level == null || level.isBlank() ? null : normalizeLevel(level);
        return selfAssessmentMapper.findByUser(userId, normalized).stream()
                .map(row -> new AssessedItem(
                        row.getEntryId(), row.getTitle(), row.getLevel(), row.getUpdatedAt()))
                .toList();
    }

    /**
     * 当前用户对某条目的标记；没有则为空。供条目详情复用。
     *
     * <p>{@code userId} 为空（例如令牌主体不是数字）时直接返回空，而不是抛错——详情本身
     * 是可读的，不该因为读不出"我的标记"就整页失败。
     */
    public Optional<String> levelOf(Long userId, Long entryId) {
        if (userId == null) {
            return Optional.empty();
        }
        return selfAssessmentMapper.findLevel(userId, entryId);
    }

    private void requirePublishedEntry(Long entryId) {
        Optional<Entry> entry = entryMapper.findPublishedById(entryId);
        if (entry.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "条目不存在");
        }
    }

    private static String normalizeLevel(String level) {
        if (SelfAssessment.LEVEL_UNDERSTOOD.equals(level) || SelfAssessment.LEVEL_UNSURE.equals(level)) {
            return level;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "标记只能是 understood 或 unsure");
    }
}
