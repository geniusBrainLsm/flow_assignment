package com.assignment.fileextension.service;

import com.assignment.fileextension.dto.AuditLogDto;
import com.assignment.fileextension.entity.FileAuditLog;
import com.assignment.fileextension.enums.BlockReason;
import com.assignment.fileextension.repository.FileAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService 테스트")
class AuditServiceTest {

    @Mock
    private FileAuditLogRepository auditLogRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private AuditService auditService;

    private MultipartFile testFile;
    private HttpServletRequest mockRequest;
    private FileValidationService.FileValidationResult blockedResult;
    private FileAuditLog savedAuditLog;

    @BeforeEach
    void setUp() {
        testFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/exe",
                "malware content".getBytes()
        );

        mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("192.168.1.100");
        ((MockHttpServletRequest) mockRequest).addHeader("User-Agent", "Test Browser");

        blockedResult = new FileValidationService.FileValidationResult(
                false,
                true,
                "차단된 확장자입니다: exe",
                "exe",
                BlockReason.BLOCKED_EXTENSION
        );

        savedAuditLog = FileAuditLog.builder()
                .id(1L)
                .filename("malware.exe")
                .fileSize(15L)
                .ipAddress("192.168.1.100")
                .userAgent("Test Browser")
                .blocked(true)
                .blockReason("차단된 확장자입니다: exe")
                .blockedExtension("exe")
                .blockReasonType(BlockReason.BLOCKED_EXTENSION)
                .uploadTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("업로드 시도 로그 기록")
    void logUploadAttempt_Success() {
        // given
        when(auditLogRepository.save(any(FileAuditLog.class)))
                .thenReturn(savedAuditLog);

        // when
        auditService.logUploadAttempt(testFile, mockRequest);

        // then
        ArgumentCaptor<FileAuditLog> captor = ArgumentCaptor.forClass(FileAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        FileAuditLog savedLog = captor.getValue();
        assertThat(savedLog.getFilename()).isEqualTo("malware.exe");
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(savedLog.getBlocked()).isFalse();
    }

    @Test
    @DisplayName("차단된 업로드 로그 기록 및 WebSocket 알림")
    void logBlockedUpload_Success() {
        // given
        when(auditLogRepository.save(any(FileAuditLog.class)))
                .thenReturn(savedAuditLog);

        // when
        auditService.logBlockedUpload(testFile, mockRequest, blockedResult);

        // then
        // 데이터베이스에 저장 검증
        ArgumentCaptor<FileAuditLog> auditCaptor = ArgumentCaptor.forClass(FileAuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());

        FileAuditLog savedLog = auditCaptor.getValue();
        assertThat(savedLog.getFilename()).isEqualTo("malware.exe");
        assertThat(savedLog.getBlocked()).isTrue();
        assertThat(savedLog.getBlockReason()).contains("exe");
        assertThat(savedLog.getBlockReasonType()).isEqualTo(BlockReason.BLOCKED_EXTENSION);

        // WebSocket 알림 검증
        ArgumentCaptor<AuditLogDto> wsCaptor = ArgumentCaptor.forClass(AuditLogDto.class);
        verify(webSocketNotificationService).sendAuditLogUpdate(wsCaptor.capture());

        AuditLogDto sentDto = wsCaptor.getValue();
        assertThat(sentDto.getFilename()).isEqualTo("malware.exe");
        assertThat(sentDto.getBlocked()).isTrue();
        assertThat(sentDto.getBlockReason()).isEqualTo(BlockReason.BLOCKED_EXTENSION);
    }

    @Test
    @DisplayName("성공한 업로드 로그 기록")
    void logSuccessfulUpload_Success() {
        // given
        FileAuditLog successLog = FileAuditLog.builder()
                .filename("document.pdf")
                .blocked(false)
                .build();
        when(auditLogRepository.save(any(FileAuditLog.class)))
                .thenReturn(successLog);

        // when
        auditService.logSuccessfulUpload(testFile, mockRequest);

        // then
        verify(auditLogRepository).save(any(FileAuditLog.class));
    }

    @Test
    @DisplayName("차단된 업로드 조회")
    void getBlockedUploads_Success() {
        // given
        List<FileAuditLog> blockedLogs = Arrays.asList(savedAuditLog);
        Page<FileAuditLog> page = new PageImpl<>(blockedLogs);
        Pageable pageable = PageRequest.of(0, 10);

        when(auditLogRepository.findByBlockedTrueOrderByUploadTimeDesc(pageable))
                .thenReturn(page);

        // when
        Page<FileAuditLog> result = auditService.getBlockedUploads(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBlocked()).isTrue();
        verify(auditLogRepository).findByBlockedTrueOrderByUploadTimeDesc(pageable);
    }

    @Test
    @DisplayName("차단된 업로드 로그 기록 시 예외 발생 - 서비스 중단되지 않음")
    void logBlockedUpload_ExceptionOccurred_ServiceContinues() {
        // given
        when(auditLogRepository.save(any(FileAuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // when & then (예외가 전파되지 않아야 함)
        auditService.logBlockedUpload(testFile, mockRequest, blockedResult);

        verify(auditLogRepository).save(any(FileAuditLog.class));
        // WebSocket 알림은 호출되지 않아야 함
        verify(webSocketNotificationService, never()).sendAuditLogUpdate(any());
    }

    @Test
    @DisplayName("클라이언트 IP 주소 추출 - X-Forwarded-For 헤더")
    void getClientIpAddress_XForwardedFor() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        request.setRemoteAddr("127.0.0.1");

        when(auditLogRepository.save(any(FileAuditLog.class)))
                .thenReturn(savedAuditLog);

        // when
        auditService.logUploadAttempt(testFile, request);

        // then
        ArgumentCaptor<FileAuditLog> captor = ArgumentCaptor.forClass(FileAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        // X-Forwarded-For의 첫 번째 IP가 사용되어야 함
        FileAuditLog savedLog = captor.getValue();
        assertThat(savedLog.getIpAddress()).isEqualTo("10.0.0.1");
    }
}