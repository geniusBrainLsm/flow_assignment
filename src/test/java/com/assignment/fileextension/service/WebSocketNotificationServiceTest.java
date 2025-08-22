package com.assignment.fileextension.service;

import com.assignment.fileextension.dto.AuditLogDto;
import com.assignment.fileextension.enums.BlockReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketNotificationService 테스트")
class WebSocketNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketNotificationService webSocketNotificationService;

    private AuditLogDto testAuditLog;

    @BeforeEach
    void setUp() {
        testAuditLog = AuditLogDto.builder()
                .id(1L)
                .filename("malware.exe")
                .fileSize(1024L)
                .ipAddress("192.168.1.100")
                .blocked(true)
                .message("차단된 확장자입니다")
                .blockedExtension("exe")
                .blockReason(BlockReason.BLOCKED_EXTENSION)
                .uploadTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("감사 로그 WebSocket 전송 - 성공")
    void sendAuditLogUpdate_Success() {
        // when
        webSocketNotificationService.sendAuditLogUpdate(testAuditLog);

        // then
        verify(messagingTemplate).convertAndSend("/topic/audit-logs", testAuditLog);
    }

    @Test
    @DisplayName("감사 로그 WebSocket 전송 - 예외 발생시 로그만 기록")
    void sendAuditLogUpdate_ExceptionOccurred() {
        // given
        doThrow(new RuntimeException("Connection failed"))
                .when(messagingTemplate).convertAndSend("/topic/audit-logs", testAuditLog);

        // when & then (예외가 전파되지 않아야 함)
        webSocketNotificationService.sendAuditLogUpdate(testAuditLog);

        verify(messagingTemplate).convertAndSend("/topic/audit-logs", testAuditLog);
    }
}