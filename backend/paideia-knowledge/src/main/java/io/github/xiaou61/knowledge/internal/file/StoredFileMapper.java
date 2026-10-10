package io.github.xiaou61.knowledge.internal.file;

import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 附件记录访问。id 由存储实现生成后写入，因此这里没有 {@code useGeneratedKeys}。 */
@Mapper
public interface StoredFileMapper {

    @Insert("""
            INSERT INTO knowledge_files (id, original_name, content_type, size_bytes, uploader_id)
            VALUES (#{id}, #{originalName}, #{contentType}, #{sizeBytes}, #{uploaderId})
            """)
    int insert(StoredFile file);

    @Select("""
            SELECT id, original_name, content_type, size_bytes, uploader_id, created_at
            FROM knowledge_files WHERE id = #{id}
            """)
    Optional<StoredFile> findById(@Param("id") String id);
}
