package com.assignment.fileextension.controller;

import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.enums.BlockReason;
import com.assignment.fileextension.service.AuditService;
import com.assignment.fileextension.service.FileValidationService;
import com.assignment.fileextension.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)
@DisplayName("FileUploadController 테스트")
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileValidationService fileValidationService;

    @MockBean
    private StorageService storageService;

    @MockBean
    private AuditService auditService;

    @Test
    @DisplayName("파일 업로드 성공")
    void uploadFile_Success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        FileValidationService.FileValidationResult validationResult = 
                FileValidationService.FileValidationResult.allowed();

        UploadedFile uploadedFile = UploadedFile.builder()
                .id(1L)
                .originalFilename("test.pdf")
                .fileSize(12L)
                .filePath("/uploads/test.pdf")
                .build();

        when(fileValidationService.validateFile(any())).thenReturn(validationResult);
        when(storageService.storeFile(any())).thenReturn(uploadedFile);

        // when & then
        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("파일이 성공적으로 업로드되었습니다."))
                .andExpect(jsonPath("$.fileName").value("test.pdf"))
                .andExpect(jsonPath("$.fileSize").value(12))
                .andExpect(jsonPath("$.filePath").value("/uploads/test.pdf"));

        verify(auditService).logUploadAttempt(any(), any());
        verify(auditService).logSuccessfulUpload(any(), any());
    }

    @Test
    @DisplayName("차단된 파일 업로드 시도")
    void uploadFile_BlockedFile() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/exe",
                "malware content".getBytes()
        );

        FileValidationService.FileValidationResult validationResult = 
                FileValidationService.FileValidationResult.blocked(
                        "차단된 확장자: exe", 
                        BlockReason.BLOCKED_EXTENSION, 
                        "exe"
                );

        when(fileValidationService.validateFile(any())).thenReturn(validationResult);

        // when & then
        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("파일 업로드가 차단되었습니다."))
                .andExpect(jsonPath("$.reason").value("차단된 확장자: exe"))
                .andExpect(jsonPath("$.blockReason").value("BLOCKED_EXTENSION"))
                .andExpect(jsonPath("$.blockedExtension").value("exe"));

        verify(auditService).logUploadAttempt(any(), any());
        verify(auditService).logBlockedUpload(any(), any(), eq(validationResult));
        verify(storageService, never()).storeFile(any());
    }

    @Test
    @DisplayName("파일 검증만 - 허용되는 파일")
    void checkFileOnly_AllowedFile() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "document content".getBytes()
        );

        FileValidationService.FileValidationResult validationResult = 
                FileValidationService.FileValidationResult.allowed();

        when(fileValidationService.validateFile(any())).thenReturn(validationResult);

        // when & then
        mockMvc.perform(multipart("/api/files/check").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("document.pdf"))
                .andExpect(jsonPath("$.fileSize").value(16))
                .andExpect(jsonPath("$.result").value("allowed"))
                .andExpect(jsonPath("$.message").value("파일 업로드가 허용됩니다."));

        verify(storageService, never()).storeFile(any());
    }

    @Test
    @DisplayName("파일 검증만 - 차단되는 파일")
    void checkFileOnly_BlockedFile() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "virus.exe",
                "application/exe",
                "virus content".getBytes()
        );

        FileValidationService.FileValidationResult validationResult = 
                FileValidationService.FileValidationResult.blocked(
                        "차단된 확장자: exe",
                        BlockReason.BLOCKED_EXTENSION,
                        "exe"
                );

        when(fileValidationService.validateFile(any())).thenReturn(validationResult);

        // when & then
        mockMvc.perform(multipart("/api/files/check").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("virus.exe"))
                .andExpect(jsonPath("$.fileSize").value(13))
                .andExpect(jsonPath("$.result").value("blocked"))
                .andExpect(jsonPath("$.message").value("차단된 확장자: exe"))
                .andExpect(jsonPath("$.blockReason").value("BLOCKED_EXTENSION"))
                .andExpect(jsonPath("$.blockedExtension").value("exe"));

        verify(storageService, never()).storeFile(any());
    }

    @Test
    @DisplayName("빈 파일 검증 - 에러")
    void checkFileOnly_EmptyFile() throws Exception {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // when & then
        mockMvc.perform(multipart("/api/files/check").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("파일이 선택되지 않았습니다."));

        verify(fileValidationService, never()).validateFile(any());
        verify(storageService, never()).storeFile(any());
    }
}