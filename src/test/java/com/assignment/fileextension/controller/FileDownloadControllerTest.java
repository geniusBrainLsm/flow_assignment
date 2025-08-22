package com.assignment.fileextension.controller;

import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileManagementController.class)
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
@DisplayName("파일 다운로드 최적화 테스트")
class FileDownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    private UploadedFile testFile;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트용 실제 파일 생성
        Path testFilePath = Paths.get("test-file.txt");
        Files.write(testFilePath, "Test content".getBytes());

        testFile = UploadedFile.builder()
                .id(1L)
                .originalFilename("test.txt")
                .storedFilename("test-file.txt")
                .filePath(testFilePath.toString())
                .extension("txt")
                .fileSize(12L)
                .status(UploadedFile.FileStatus.ACTIVE)
                .deletionException(false)
                .build();
    }

    @Test
    @DisplayName("파일 다운로드 - 최적화된 단일 쿼리 사용")
    void downloadFile_OptimizedQuery() throws Exception {
        // given - StorageService.findById() 한번만 호출됨
        when(storageService.findById(1L))
                .thenReturn(testFile);

        // when & then
        mockMvc.perform(get("/api/files/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));

        // 최적화 검증: findById 한번만 호출되어야 함
        org.mockito.Mockito.verify(storageService, org.mockito.Mockito.times(1)).findById(1L);
        // 이전 비효율적 메서드들은 호출되지 않아야 함
        org.mockito.Mockito.verify(storageService, org.mockito.Mockito.never())
                .getFilesByStatus(org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("파일 다운로드 - 파일 없음 (최적화)")
    void downloadFile_FileNotFound_Optimized() throws Exception {
        // given
        when(storageService.findById(999L))
                .thenReturn(null);

        // when & then
        mockMvc.perform(get("/api/files/999/download"))
                .andExpect(status().isNotFound());

        // 최적화 검증: 불필요한 추가 쿼리가 발생하지 않음
        org.mockito.Mockito.verify(storageService, org.mockito.Mockito.times(1)).findById(999L);
    }
}