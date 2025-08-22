package com.assignment.fileextension.controller;

import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileManagementController.class)
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
@DisplayName("FileManagementController 테스트")
class FileManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StorageService storageService;

    private UploadedFile mockFile;
    private UploadedFile protectedFile;

    @BeforeEach
    void setUp() {
        mockFile = UploadedFile.builder()
                .id(1L)
                .originalFilename("test.pdf")
                .storedFilename("uuid.pdf")
                .filePath("/uploads/uuid.pdf")
                .extension("pdf")
                .fileSize(1024L)
                .status(UploadedFile.FileStatus.ACTIVE)
                .deletionException(false)
                .build();

        protectedFile = UploadedFile.builder()
                .id(2L)
                .originalFilename("important.pdf")
                .storedFilename("uuid2.pdf")
                .filePath("/uploads/uuid2.pdf")
                .extension("pdf")
                .fileSize(2048L)
                .status(UploadedFile.FileStatus.ACTIVE)
                .deletionException(true)
                .build();
    }

    @Test
    @DisplayName("전체 파일 목록 조회")
    void getAllFiles() throws Exception {
        List<UploadedFile> files = Arrays.asList(mockFile, protectedFile);
        when(storageService.getFilesByStatus(UploadedFile.FileStatus.ACTIVE))
                .thenReturn(files);

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].originalFilename").value("test.pdf"))
                .andExpect(jsonPath("$[1].originalFilename").value("important.pdf"));

        verify(storageService).getFilesByStatus(UploadedFile.FileStatus.ACTIVE);
    }

    @Test
    @DisplayName("특정 파일 정보 조회 - 성공")
    void getFileById_Success() throws Exception {
        when(storageService.findById(1L))
                .thenReturn(mockFile);

        mockMvc.perform(get("/api/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originalFilename").value("test.pdf"))
                .andExpect(jsonPath("$.deletionException").value(false));
    }

    @Test
    @DisplayName("특정 파일 정보 조회 - 파일 없음")
    void getFileById_NotFound() throws Exception {
        when(storageService.findById(999L))
                .thenReturn(null);

        mockMvc.perform(get("/api/files/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("파일 삭제 - 성공")
    void deleteFile_Success() throws Exception {
        doNothing().when(storageService).deletePhysicalFile(1L);

        mockMvc.perform(delete("/api/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("파일이 성공적으로 삭제되었습니다."));

        verify(storageService).deletePhysicalFile(1L);
    }

    @Test
    @DisplayName("파일 삭제 - 파일 없음")
    void deleteFile_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("파일을 찾을 수 없습니다."))
                .when(storageService).deletePhysicalFile(999L);

        mockMvc.perform(delete("/api/files/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("삭제 예외 설정 - 보호 설정")
    void setDeletionException_SetProtection() throws Exception {
        Map<String, Boolean> request = new HashMap<>();
        request.put("deletionException", true);

        doNothing().when(storageService).setDeletionException(1L, true);

        mockMvc.perform(put("/api/files/1/protection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("파일이 삭제 예외로 설정되었습니다."))
                .andExpect(jsonPath("$.deletionException").value(true));

        verify(storageService).setDeletionException(1L, true);
    }

    @Test
    @DisplayName("삭제 예외 설정 - 보호 해제")
    void setDeletionException_RemoveProtection() throws Exception {
        Map<String, Boolean> request = new HashMap<>();
        request.put("deletionException", false);

        doNothing().when(storageService).setDeletionException(1L, false);

        mockMvc.perform(put("/api/files/1/protection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("파일의 삭제 예외가 해제되었습니다."))
                .andExpect(jsonPath("$.deletionException").value(false));

        verify(storageService).setDeletionException(1L, false);
    }

    @Test
    @DisplayName("삭제 예외 설정 - 잘못된 요청")
    void setDeletionException_BadRequest() throws Exception {
        Map<String, String> request = new HashMap<>(); // Boolean 대신 String
        request.put("wrongField", "true");

        mockMvc.perform(put("/api/files/1/protection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("deletionException 필드가 필요합니다."));

        verify(storageService, never()).setDeletionException(any(), any());
    }

    @Test
    @DisplayName("상태별 파일 조회")
    void getFilesByStatus() throws Exception {
        List<UploadedFile> activeFiles = Arrays.asList(mockFile);
        when(storageService.getFilesByStatus(UploadedFile.FileStatus.ACTIVE))
                .thenReturn(activeFiles);

        mockMvc.perform(get("/api/files/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(storageService).getFilesByStatus(UploadedFile.FileStatus.ACTIVE);
    }

    @Test
    @DisplayName("확장자별 파일 조회")
    void getFilesByExtension() throws Exception {
        List<UploadedFile> pdfFiles = Arrays.asList(mockFile, protectedFile);
        when(storageService.getFilesByExtension("pdf"))
                .thenReturn(pdfFiles);

        mockMvc.perform(get("/api/files/extension/pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].extension").value("pdf"));

        verify(storageService).getFilesByExtension("pdf");
    }
}