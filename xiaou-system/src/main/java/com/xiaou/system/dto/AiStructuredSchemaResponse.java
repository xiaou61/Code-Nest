package com.xiaou.system.dto;

import lombok.Data;

/**
 * 结构化输出 Schema 清单项。
 *
 * @author xiaou
 */
@Data
public class AiStructuredSchemaResponse {

    /**
     * 业务域。
     */
    private String domain;

    /**
     * 契约唯一标识。
     */
    private String specId;

    /**
     * 对应 Prompt Key。
     */
    private String promptKey;

    /**
     * 对应 Prompt 版本。
     */
    private String promptVersion;

    /**
     * Schema 根类型。
     */
    private String rootType;

    /**
     * Schema 逻辑标识。
     */
    private String schemaId;

    /**
     * Schema 导出文件名。
     */
    private String schemaFileName;

    /**
     * 格式化后的 JSON Schema。
     */
    private String schemaJson;
}
