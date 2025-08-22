package com.assignment.fileextension.integration;

import com.assignment.fileextension.dto.AuditLogDto;
import com.assignment.fileextension.enums.BlockReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
@DisplayName("WebSocket 통합 테스트")
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private BlockingQueue<AuditLogDto> blockingQueue;

    @BeforeEach
    void setUp() throws Exception {
        blockingQueue = new LinkedBlockingQueue<>();
        
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String url = "ws://localhost:" + port + "/ws";
        stompSession = stompClient.connect(url, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("WebSocket 연결 테스트")
    void testWebSocketConnection() {
        assertThat(stompSession.isConnected()).isTrue();
    }

    @Test
    @DisplayName("감사 로그 WebSocket 구독 테스트")
    void testAuditLogSubscription() throws Exception {
        // WebSocket 구독
        stompSession.subscribe("/topic/audit-logs", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AuditLogDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.add((AuditLogDto) payload);
            }
        });

        // 테스트용 감사 로그 생성
        AuditLogDto testLog = AuditLogDto.builder()
                .id(1L)
                .filename("test.exe")
                .fileSize(1024L)
                .ipAddress("192.168.1.100")
                .blocked(true)
                .message("차단된 확장자입니다")
                .blockedExtension("exe")
                .blockReason(BlockReason.BLOCKED_EXTENSION)
                .uploadTime(LocalDateTime.now())
                .build();

        // WebSocket으로 메시지 전송 (실제 서비스에서는 자동으로 전송됨)
        stompSession.send("/topic/audit-logs", testLog);

        // 메시지 수신 확인
        AuditLogDto receivedLog = blockingQueue.poll(5, TimeUnit.SECONDS);
        
        assertThat(receivedLog).isNotNull();
        assertThat(receivedLog.getFilename()).isEqualTo("test.exe");
        assertThat(receivedLog.getBlocked()).isTrue();
        assertThat(receivedLog.getBlockReason()).isEqualTo(BlockReason.BLOCKED_EXTENSION);
    }
}