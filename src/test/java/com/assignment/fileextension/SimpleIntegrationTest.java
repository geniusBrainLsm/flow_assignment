package com.assignment.fileextension;

import com.assignment.fileextension.service.AuditService;
import com.assignment.fileextension.service.ExtensionService;
import com.assignment.fileextension.service.StorageService;
import com.assignment.fileextension.service.WebSocketNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
@Transactional
@DisplayName("간단한 통합 테스트")
class SimpleIntegrationTest {

    @Autowired
    private ExtensionService extensionService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    @Test
    @DisplayName("스프링 컨텍스트 로딩 테스트")
    void contextLoads() {
        assertThat(extensionService).isNotNull();
        assertThat(auditService).isNotNull();
        assertThat(storageService).isNotNull();
        assertThat(webSocketNotificationService).isNotNull();
    }

    @Test
    @DisplayName("고정 확장자 조회 테스트")
    void getAllFixedExtensions() {
        // when
        var extensions = extensionService.getAllFixedExtensionSettings();

        // then
        assertThat(extensions).isNotEmpty();
        assertThat(extensions.size()).isGreaterThanOrEqualTo(7); // bat, cmd, com, cpl, exe, scr, js
    }

    @Test
    @DisplayName("파일 확장자 차단 여부 확인 테스트")
    void isExtensionBlocked() {
        // 확장자가 없는 파일은 허용
        assertThat(extensionService.isExtensionBlocked("README")).isFalse();
        
        // null 파일명 처리
        assertThat(extensionService.isExtensionBlocked(null)).isFalse();
    }
}