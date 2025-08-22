package com.assignment.fileextension.service;

import com.assignment.fileextension.enums.BlockReason;
import com.assignment.fileextension.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileValidationService 테스트")
class FileValidationServiceTest {

    @Mock
    private ExtensionService extensionService;

    @InjectMocks
    private FileValidationService fileValidationService;

    private MultipartFile validFile;
    private MultipartFile emptyFile;
    private MultipartFile largeFile;
    private MultipartFile blockedFile;

    @BeforeEach
    void setUp() {
        // 정상 파일
        validFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // 빈 파일
        emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        // 큰 파일 (100MB 초과)
        largeFile = new MockMultipartFile(
                "file",
                "large.pdf", 
                "application/pdf",
                new byte[101 * 1024 * 1024]
        );

        // 차단된 확장자 파일
        blockedFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/exe",
                "malware content".getBytes()
        );
    }

    @Test
    @DisplayName("정상 파일 검증 - 성공")
    void validateFile_ValidFile_Success() {
        // given
        when(extensionService.isExtensionBlocked(eq("test.pdf"), any(String.class)))
                .thenReturn(false);

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(validFile);

        // then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getReason()).isNull();
        assertThat(result.getBlockReason()).isNull();
    }

    @Test
    @DisplayName("빈 파일 검증 - 실패")
    void validateFile_EmptyFile_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFile(emptyFile))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("파일이 선택되지 않았습니다.");
    }

    @Test
    @DisplayName("큰 파일 검증 - 실패")
    void validateFile_LargeFile_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFile(largeFile))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("파일 크기가 너무 큽니다");
    }

    @Test
    @DisplayName("null 파일명 검증 - 실패")
    void validateFile_NullFileName_ThrowsException() {
        // given
        MultipartFile nullNameFile = new MockMultipartFile(
                "file",
                null,
                "application/pdf",
                "content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileValidationService.validateFile(nullNameFile))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("올바르지 않은 파일명입니다");
    }

    @Test
    @DisplayName("차단된 확장자 파일 검증 - 차단")
    void validateFile_BlockedExtension_Blocked() {
        // given
        when(extensionService.isExtensionBlocked(eq("malware.exe"), eq("exe")))
                .thenReturn(true);

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(blockedFile);

        // then
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getBlockReason()).isEqualTo(BlockReason.BLOCKED_EXTENSION);
        assertThat(result.getBlockedExtension()).isEqualTo("exe");
        assertThat(result.getReason()).contains("exe");
    }

    @Test
    @DisplayName("우회 공격 파일 검증 - 차단")
    void validateFile_BypassAttempt_Blocked() {
        // given
        MultipartFile bypassFile = new MockMultipartFile(
                "file",
                "document.pdf.exe",
                "application/exe",
                "bypass content".getBytes()
        );

        when(extensionService.isExtensionBlocked(eq("document.pdf.exe"), eq("exe")))
                .thenReturn(true);

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(bypassFile);

        // then
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getBlockReason()).isEqualTo(BlockReason.BLOCKED_EXTENSION);
        assertThat(result.getBlockedExtension()).isEqualTo("exe");
    }

    @Test
    @DisplayName("복합 확장자 파일 검증 - 허용")
    void validateFile_MultipleExtensions_Allowed() {
        // given
        MultipartFile multiExtFile = new MockMultipartFile(
                "file",
                "backup.2024.txt",
                "text/plain",
                "backup content".getBytes()
        );

        when(extensionService.isExtensionBlocked(eq("backup.2024.txt"), eq("txt")))
                .thenReturn(false);
        when(extensionService.isExtensionBlocked(eq("backup.2024.txt"), eq("2024")))
                .thenReturn(false);

        // when
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(multiExtFile);

        // then
        assertThat(result.isAllowed()).isTrue();
    }
}