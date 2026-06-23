package com.xiaou.sensitive.service.impl;

import com.xiaou.sensitive.api.SensitiveCheckService;
import com.xiaou.sensitive.domain.SensitiveWord;
import com.xiaou.sensitive.mapper.SensitiveCategoryMapper;
import com.xiaou.sensitive.mapper.SensitiveWordMapper;
import com.xiaou.sensitive.service.SensitiveVersionService;
import com.xiaou.sensitive.service.SensitiveWordService.ImportResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SensitiveWordServiceImplTest {

    @Test
    void addWordShouldRejectMissingDefaultCategoryBeforeInsert() {
        SensitiveWordMapper wordMapper = mock(SensitiveWordMapper.class);
        SensitiveCategoryMapper categoryMapper = mock(SensitiveCategoryMapper.class);
        SensitiveCheckService checkService = mock(SensitiveCheckService.class);
        SensitiveVersionService versionService = mock(SensitiveVersionService.class);
        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl(
                wordMapper,
                categoryMapper,
                checkService,
                versionService
        );

        SensitiveWord word = new SensitiveWord();
        word.setWord("  违规词  ");

        when(wordMapper.selectWordByWord("违规词")).thenReturn(null);
        when(categoryMapper.selectCategoryById(3)).thenReturn(null);
        when(wordMapper.insertWord(any(SensitiveWord.class))).thenReturn(1);

        assertFalse(service.addWord(word));

        verify(categoryMapper).selectCategoryById(3);
        verify(wordMapper, never()).insertWord(any(SensitiveWord.class));
        verifyNoInteractions(checkService, versionService);
    }

    @Test
    void addWordShouldRejectExplicitMissingCategoryBeforeInsert() {
        SensitiveWordMapper wordMapper = mock(SensitiveWordMapper.class);
        SensitiveCategoryMapper categoryMapper = mock(SensitiveCategoryMapper.class);
        SensitiveCheckService checkService = mock(SensitiveCheckService.class);
        SensitiveVersionService versionService = mock(SensitiveVersionService.class);
        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl(
                wordMapper,
                categoryMapper,
                checkService,
                versionService
        );

        SensitiveWord word = new SensitiveWord();
        word.setWord("违规词");
        word.setCategoryId(99);

        when(wordMapper.selectWordByWord("违规词")).thenReturn(null);
        when(categoryMapper.selectCategoryById(99)).thenReturn(null);
        when(wordMapper.insertWord(any(SensitiveWord.class))).thenReturn(1);

        assertFalse(service.addWord(word));

        verify(categoryMapper).selectCategoryById(99);
        verify(wordMapper, never()).insertWord(any(SensitiveWord.class));
        verifyNoInteractions(checkService, versionService);
    }

    @Test
    void importWordsShouldReportErrorWhenDefaultCategoryMissing() {
        SensitiveWordMapper wordMapper = mock(SensitiveWordMapper.class);
        SensitiveCategoryMapper categoryMapper = mock(SensitiveCategoryMapper.class);
        SensitiveCheckService checkService = mock(SensitiveCheckService.class);
        SensitiveVersionService versionService = mock(SensitiveVersionService.class);
        SensitiveWordServiceImpl service = new SensitiveWordServiceImpl(
                wordMapper,
                categoryMapper,
                checkService,
                versionService
        );

        when(categoryMapper.selectCategoryById(3)).thenReturn(null);
        when(wordMapper.selectWordsByWords(anyList())).thenReturn(List.of());
        when(wordMapper.batchInsertWords(anyList())).thenReturn(1);

        ImportResult result = service.importWords(List.of("违规词"), 7L);

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getSuccess());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("默认敏感词分类不存在")));
        verify(categoryMapper).selectCategoryById(3);
        verify(wordMapper, never()).batchInsertWords(anyList());
        verifyNoInteractions(checkService, versionService);
    }
}
