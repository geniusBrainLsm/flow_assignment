package com.assignment.fileextension.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileExtensionConstants 테스트")
class FileExtensionConstantsTest {

    @Test
    @DisplayName("고정 확장자 목록 확인")
    void fixedExtensions() {
        assertThat(FileExtensionConstants.FIXED_EXTENSIONS)
                .hasSize(7)
                .contains("bat", "cmd", "com", "cpl", "exe", "scr", "js");
    }

    @Test
    @DisplayName("파일 크기 제한 - 100MB")
    void fileSize_100MB() {
        long expectedSize = 100 * 1024 * 1024L; // 100MB
        
        assertThat(FileExtensionConstants.FileLimit.MAX_FILE_SIZE_BYTES)
                .isEqualTo(expectedSize);
    }

    @Test
    @DisplayName("파일 크기 초과 메시지 - 100MB")
    void fileSizeMessage_100MB() {
        assertThat(FileExtensionConstants.Messages.FILE_TOO_LARGE)
                .contains("100MB");
    }

    @Test
    @DisplayName("커스텀 확장자 최대 개수")
    void maxCustomExtensions() {
        assertThat(FileExtensionConstants.FileLimit.MAX_CUSTOM_EXTENSIONS)
                .isEqualTo(200);
    }

    @Test
    @DisplayName("확장자 이름 최대 길이")
    void maxExtensionNameLength() {
        assertThat(FileExtensionConstants.FileLimit.MAX_EXTENSION_NAME_LENGTH)
                .isEqualTo(20);
    }

    @Test
    @DisplayName("모든 메시지 상수 존재 확인")
    void allMessages() {
        assertThat(FileExtensionConstants.Messages.FILE_BLOCKED).isNotNull();
        assertThat(FileExtensionConstants.Messages.FILE_TOO_LARGE).isNotNull();
        assertThat(FileExtensionConstants.Messages.INVALID_FILENAME).isNotNull();
        assertThat(FileExtensionConstants.Messages.EXTENSION_NOT_FOUND).isNotNull();
        assertThat(FileExtensionConstants.Messages.EXTENSION_ALREADY_EXISTS).isNotNull();
        assertThat(FileExtensionConstants.Messages.MAX_EXTENSIONS_EXCEEDED).isNotNull();
    }

    @Test
    @DisplayName("모든 로그 메시지 템플릿 존재 확인")
    void allLogMessages() {
        assertThat(FileExtensionConstants.LogMessages.EXTENSION_SETTING_CHANGED).isNotNull();
        assertThat(FileExtensionConstants.LogMessages.FILE_UPLOAD_SUCCESS).isNotNull();
        assertThat(FileExtensionConstants.LogMessages.FILE_UPLOAD_BLOCKED).isNotNull();
        assertThat(FileExtensionConstants.LogMessages.FILE_DELETE_SUCCESS).isNotNull();
        assertThat(FileExtensionConstants.LogMessages.AUDIT_LOG_FAILED).isNotNull();
    }
}