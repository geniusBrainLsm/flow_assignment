package com.assignment.fileextension.controller;

import com.assignment.fileextension.entity.FileAuditLog;
import com.assignment.fileextension.enums.BlockReason;
import com.assignment.fileextension.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@DisplayName("AuditController 테스트")
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    private FileAuditLog blockedAuditLog;
    private Page<FileAuditLog> blockedLogsPage;

    @BeforeEach
    void setUp() {
        blockedAuditLog = FileAuditLog.builder()
                .id(1L)
                .filename("malware.exe")
                .fileSize(1024L)
                .uploadTime(LocalDateTime.now())
                .blocked(true)
                .blockReason("차단된 확장자: exe")
                .blockedExtension("exe")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .actionType(FileAuditLog.ActionType.UPLOAD_BLOCKED)
                .blockReasonType(BlockReason.BLOCKED_EXTENSION)
                .build();

        List<FileAuditLog> blockedLogs = Arrays.asList(blockedAuditLog);
        blockedLogsPage = new PageImpl<>(blockedLogs, PageRequest.of(0, 10), 1);
    }

    @Test
    @DisplayName("GET /api/audit/blocked - 차단된 업로드 조회 성공")
    void getBlockedUploads_Success() throws Exception {
        // given
        when(auditService.getBlockedUploads(any(Pageable.class))).thenReturn(blockedLogsPage);

        // when & then
        mockMvc.perform(get("/api/audit/blocked")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].filename").value("malware.exe"))
                .andExpect(jsonPath("$.content[0].fileSize").value(1024))
                .andExpect(jsonPath("$.content[0].blocked").value(true))
                .andExpect(jsonPath("$.content[0].blockReason").value("차단된 확장자: exe"))
                .andExpect(jsonPath("$.content[0].blockedExtension").value("exe"))
                .andExpect(jsonPath("$.content[0].ipAddress").value("192.168.1.100"))
                .andExpect(jsonPath("$.content[0].actionType").value("UPLOAD_BLOCKED"))
                .andExpect(jsonPath("$.content[0].blockReasonType").value("BLOCKED_EXTENSION"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("GET /api/audit/blocked - 기본 페이징 파라미터")
    void getBlockedUploads_DefaultPaging() throws Exception {
        // given
        when(auditService.getBlockedUploads(any(Pageable.class))).thenReturn(blockedLogsPage);

        // when & then
        mockMvc.perform(get("/api/audit/blocked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("GET /api/audit/blocked - 커스텀 페이징 파라미터")
    void getBlockedUploads_CustomPaging() throws Exception {
        // given
        Page<FileAuditLog> customPage = new PageImpl<>(
                Arrays.asList(blockedAuditLog), 
                PageRequest.of(1, 5), 
                10
        );
        when(auditService.getBlockedUploads(any(Pageable.class))).thenReturn(customPage);

        // when & then
        mockMvc.perform(get("/api/audit/blocked")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    @DisplayName("GET /api/audit/blocked - 빈 결과")
    void getBlockedUploads_EmptyResult() throws Exception {
        // given
        Page<FileAuditLog> emptyPage = new PageImpl<>(
                Arrays.asList(), 
                PageRequest.of(0, 10), 
                0
        );
        when(auditService.getBlockedUploads(any(Pageable.class))).thenReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/audit/blocked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @DisplayName("GET /api/audit/blocked - 다양한 차단 사유 테스트")
    void getBlockedUploads_VariousBlockReasons() throws Exception {
        // given
        FileAuditLog fileSizeLog = FileAuditLog.builder()
                .id(2L)
                .filename("largefile.pdf")
                .fileSize(10485760L) // 10MB
                .uploadTime(LocalDateTime.now())
                .blocked(true)
                .blockReason("파일 크기가 너무 큽니다")
                .ipAddress("192.168.1.101")
                .actionType(FileAuditLog.ActionType.UPLOAD_BLOCKED)
                .blockReasonType(BlockReason.FILE_SIZE_EXCEEDED)
                .build();

        FileAuditLog bypassLog = FileAuditLog.builder()
                .id(3L)
                .filename("document.pdf.exe")
                .fileSize(2048L)
                .uploadTime(LocalDateTime.now())
                .blocked(true)
                .blockReason("우회 공격 시도")
                .blockedExtension("exe")
                .ipAddress("192.168.1.102")
                .actionType(FileAuditLog.ActionType.UPLOAD_BLOCKED)
                .blockReasonType(BlockReason.BYPASS_ATTEMPT)
                .build();

        List<FileAuditLog> variousLogs = Arrays.asList(blockedAuditLog, fileSizeLog, bypassLog);
        Page<FileAuditLog> variousLogsPage = new PageImpl<>(variousLogs, PageRequest.of(0, 10), 3);

        when(auditService.getBlockedUploads(any(Pageable.class))).thenReturn(variousLogsPage);

        // when & then
        mockMvc.perform(get("/api/audit/blocked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").hasJsonPath())
                .andExpect(jsonPath("$.content[0].blockReasonType").value("BLOCKED_EXTENSION"))
                .andExpect(jsonPath("$.content[1].blockReasonType").value("FILE_SIZE_EXCEEDED"))
                .andExpect(jsonPath("$.content[2].blockReasonType").value("BYPASS_ATTEMPT"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}