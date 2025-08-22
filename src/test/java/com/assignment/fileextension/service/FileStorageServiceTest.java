package com.assignment.fileextension.service;

import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.repository.UploadedFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService 테스트")
class FileStorageServiceTest {

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @InjectMocks
    private FileStorageService fileStorageService;

    private MultipartFile testFile;
    private UploadedFile mockUploadedFile;
    private UploadedFile protectedFile;

    @BeforeEach
    void setUp() {
        // 테스트 설정 값들
        ReflectionTestUtils.setField(fileStorageService, "uploadBaseDir", "test-uploads");
        ReflectionTestUtils.setField(fileStorageService, "maxFileSize", 104857600L); // 100MB

        testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        mockUploadedFile = UploadedFile.builder()
                .id(1L)
                .originalFilename("test.pdf")
                .extension("pdf")
                .fileSize(12L)
                .status(UploadedFile.FileStatus.ACTIVE)
                .deletionException(false)
                .build();

        protectedFile = UploadedFile.builder()
                .id(2L)
                .originalFilename("important.pdf")
                .extension("pdf")
                .fileSize(15L)
                .status(UploadedFile.FileStatus.ACTIVE)
                .deletionException(true)
                .build();
    }

    @Test
    @DisplayName("파일 저장 - 성공")
    void storeFile_Success() throws IOException {
        // given
        when(uploadedFileRepository.save(any(UploadedFile.class)))
                .thenReturn(mockUploadedFile);

        // when
        UploadedFile result = fileStorageService.storeFile(testFile);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilename()).isEqualTo("test.pdf");
        verify(uploadedFileRepository).save(any(UploadedFile.class));
    }

    @Test
    @DisplayName("확장자별 파일 삭제 - 보호된 파일 제외")
    void deleteFilesByExtension_SkipProtectedFiles() {
        // given
        List<UploadedFile> files = Arrays.asList(mockUploadedFile, protectedFile);
        when(uploadedFileRepository.findByExtensionAndStatus("pdf", UploadedFile.FileStatus.ACTIVE))
                .thenReturn(files);

        // when
        fileStorageService.deleteFilesByExtension("pdf");

        // then
        // 보호된 파일은 삭제되지 않아야 함
        verify(uploadedFileRepository, times(1)).delete(mockUploadedFile);
        verify(uploadedFileRepository, never()).delete(protectedFile);
    }

    @Test
    @DisplayName("삭제 예외 설정 - 성공")
    void setDeletionException_Success() {
        // given
        when(uploadedFileRepository.findById(1L))
                .thenReturn(Optional.of(mockUploadedFile));
        when(uploadedFileRepository.save(any(UploadedFile.class)))
                .thenReturn(mockUploadedFile);

        // when
        fileStorageService.setDeletionException(1L, true);

        // then
        verify(uploadedFileRepository).findById(1L);
        verify(uploadedFileRepository).save(any(UploadedFile.class));
    }

    @Test
    @DisplayName("삭제 예외 설정 - 파일 없음")
    void setDeletionException_FileNotFound() {
        // given
        when(uploadedFileRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileStorageService.setDeletionException(999L, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("파일 크기 검증 - 너무 큰 파일")
    void storeFile_FileTooLarge() {
        // given
        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                new byte[105 * 1024 * 1024] // 105MB
        );

        // when & then
        assertThatThrownBy(() -> fileStorageService.storeFile(largeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 크기가 최대 허용 크기를 초과합니다.");
    }

    @Test
    @DisplayName("빈 파일 검증 - 실패")
    void storeFile_EmptyFile() {
        // given
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        // when & then
        assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일이 선택되지 않았습니다.");
    }

    @Test
    @DisplayName("상태별 파일 조회")
    void getFilesByStatus() {
        // given
        List<UploadedFile> expectedFiles = Arrays.asList(mockUploadedFile);
        when(uploadedFileRepository.findByStatus(UploadedFile.FileStatus.ACTIVE))
                .thenReturn(expectedFiles);

        // when
        List<UploadedFile> result = fileStorageService.getFilesByStatus(UploadedFile.FileStatus.ACTIVE);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(mockUploadedFile);
        verify(uploadedFileRepository).findByStatus(UploadedFile.FileStatus.ACTIVE);
    }

    @Test
    @DisplayName("확장자별 파일 조회")
    void getFilesByExtension() {
        // given
        List<UploadedFile> expectedFiles = Arrays.asList(mockUploadedFile);
        when(uploadedFileRepository.findByExtension("pdf"))
                .thenReturn(expectedFiles);

        // when
        List<UploadedFile> result = fileStorageService.getFilesByExtension("pdf");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(mockUploadedFile);
        verify(uploadedFileRepository).findByExtension("pdf");
    }
}