package com.assignment.fileextension.dto;

import com.assignment.fileextension.entity.FileAuditLog;
import com.assignment.fileextension.enums.BlockReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AuditLogDto 테스트")
class AuditLogDtoTest {

    private FileAuditLog testEntity;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.of(2024, 8, 22, 14, 30, 0);
        
        testEntity = FileAuditLog.builder()
                .id(1L)
                .filename("malware.exe")
                .fileSize(1024L)
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 Test Browser")
                .blocked(true)
                .blockReason("차단된 확장자입니다: exe")
                .blockedExtension("exe")
                .blockReasonType(BlockReason.BLOCKED_EXTENSION)
                .uploadTime(testTime)
                .build();
    }

    @Test
    @DisplayName("Entity에서 DTO 변환 - 차단된 파일")
    void fromEntity_BlockedFile() {
        // when
        AuditLogDto dto = AuditLogDto.fromEntity(testEntity);

        // then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFilename()).isEqualTo("malware.exe");
        assertThat(dto.getFileSize()).isEqualTo(1024L);
        assertThat(dto.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(dto.getUserAgent()).isEqualTo("Mozilla/5.0 Test Browser");
        assertThat(dto.getBlocked()).isTrue();
        assertThat(dto.getMessage()).isEqualTo("차단된 확장자입니다: exe");
        assertThat(dto.getBlockedExtension()).isEqualTo("exe");
        assertThat(dto.getBlockReason()).isEqualTo(BlockReason.BLOCKED_EXTENSION);
        assertThat(dto.getUploadTime()).isEqualTo(testTime);
    }

    @Test
    @DisplayName("Entity에서 DTO 변환 - 허용된 파일")
    void fromEntity_AllowedFile() {
        // given
        FileAuditLog allowedEntity = FileAuditLog.builder()
                .id(2L)
                .filename("document.pdf")
                .fileSize(2048L)
                .ipAddress("192.168.1.101")
                .userAgent("Chrome Browser")
                .blocked(false)
                .blockReason(null)
                .blockedExtension(null)
                .blockReasonType(null)
                .uploadTime(testTime)
                .build();

        // when
        AuditLogDto dto = AuditLogDto.fromEntity(allowedEntity);

        // then
        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getFilename()).isEqualTo("document.pdf");
        assertThat(dto.getFileSize()).isEqualTo(2048L);
        assertThat(dto.getIpAddress()).isEqualTo("192.168.1.101");
        assertThat(dto.getUserAgent()).isEqualTo("Chrome Browser");
        assertThat(dto.getBlocked()).isFalse();
        assertThat(dto.getMessage()).isNull();
        assertThat(dto.getBlockedExtension()).isNull();
        assertThat(dto.getBlockReason()).isNull();
        assertThat(dto.getUploadTime()).isEqualTo(testTime);
    }

    @Test
    @DisplayName("빌더 패턴으로 DTO 생성")
    void builderPattern() {
        // when
        AuditLogDto dto = AuditLogDto.builder()
                .id(3L)
                .filename("test.js")
                .fileSize(512L)
                .ipAddress("10.0.0.1")
                .blocked(true)
                .message("JavaScript 파일이 차단되었습니다")
                .blockedExtension("js")
                .blockReason(BlockReason.BLOCKED_EXTENSION)
                .uploadTime(testTime)
                .build();

        // then
        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getFilename()).isEqualTo("test.js");
        assertThat(dto.getFileSize()).isEqualTo(512L);
        assertThat(dto.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(dto.getBlocked()).isTrue();
        assertThat(dto.getMessage()).isEqualTo("JavaScript 파일이 차단되었습니다");
        assertThat(dto.getBlockedExtension()).isEqualTo("js");
        assertThat(dto.getBlockReason()).isEqualTo(BlockReason.BLOCKED_EXTENSION);
        assertThat(dto.getUploadTime()).isEqualTo(testTime);
    }
}