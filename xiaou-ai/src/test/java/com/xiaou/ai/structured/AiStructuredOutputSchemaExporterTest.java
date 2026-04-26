package com.xiaou.ai.structured;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiStructuredOutputSchemaExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExportAllStructuredOutputSchemasToDirectory() throws Exception {
        Path outputDir = tempDir.resolve("schemas");
        AiStructuredOutputSchemaExporter.exportToDirectory(outputDir);

        assertTrue(Files.exists(outputDir));
        try (var stream = Files.list(outputDir)) {
            assertEquals(AiStructuredOutputCatalog.all().size(), stream.count());
        }
        for (AiStructuredOutputSpec spec : AiStructuredOutputCatalog.all()) {
            Path schemaFile = outputDir.resolve(spec.schemaFileName());
            assertTrue(Files.exists(schemaFile), () -> "缺少 schema 文件: " + schemaFile);
            String content = Files.readString(schemaFile);
            assertTrue(content.contains(spec.promptSpec().promptId()));
        }
    }
}
