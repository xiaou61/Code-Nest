package io.github.xiaou61.knowledge.internal.admin;

import io.github.xiaou61.knowledge.internal.entry.EntryMapper;
import io.github.xiaou61.knowledge.internal.relation.Relation;
import io.github.xiaou61.knowledge.internal.relation.RelationMapper;
import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 条目关系的管理端写入用例。 */
@Service
public class RelationAdminService {

    private final RelationMapper relationMapper;
    private final EntryMapper entryMapper;

    RelationAdminService(RelationMapper relationMapper, EntryMapper entryMapper) {
        this.relationMapper = relationMapper;
        this.entryMapper = entryMapper;
    }

    @Transactional
    public Long create(Long fromEntryId, Long toEntryId, String relationType) {
        if (fromEntryId == null || toEntryId == null) {
            throw invalid("关系两端都不能为空");
        }
        if (fromEntryId.equals(toEntryId)) {
            // 数据库有 CHECK 兜底，但那会以 500 的形式冒出来；这里给一个说得清的错误
            throw invalid("关系两端不能是同一条目");
        }
        if (!Relation.TYPE_PREREQUISITE.equals(relationType) && !Relation.TYPE_RELATED.equals(relationType)) {
            throw invalid("关系类型只能是 prerequisite 或 related");
        }
        requireEntry(fromEntryId);
        requireEntry(toEntryId);

        Relation relation = new Relation();
        relation.setFromEntryId(fromEntryId);
        relation.setToEntryId(toEntryId);
        relation.setRelationType(relationType);
        try {
            relationMapper.insert(relation);
        } catch (DuplicateKeyException exception) {
            // 唯一索引 (from, to, type) 兜住并发重复提交
            throw new BizException(ErrorCode.CONFLICT, "这条关系已经存在");
        }
        return relation.getId();
    }

    @Transactional
    public void delete(Long id) {
        if (relationMapper.deleteById(id) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "关系不存在");
        }
    }

    private void requireEntry(Long entryId) {
        // 这里允许引用草稿：关系是内容组织工作，管理员在发布前就要把关系连好
        if (entryMapper.findByIdIncludingDraft(entryId).isEmpty()) {
            throw invalid("条目不存在");
        }
    }

    private static BizException invalid(String message) {
        return new BizException(ErrorCode.INVALID_ARGUMENT, message);
    }
}
