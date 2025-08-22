package com.assignment.fileextension.service;

import com.assignment.fileextension.dto.CustomExtensionDto;
import com.assignment.fileextension.dto.ExtensionRequest;
import com.assignment.fileextension.dto.FixedExtensionSettingDto;
import com.assignment.fileextension.entity.CustomExtension;
import com.assignment.fileextension.entity.FixedExtensionSetting;
import com.assignment.fileextension.exception.ExtensionNotFoundException;
import com.assignment.fileextension.repository.CustomExtensionRepository;
import com.assignment.fileextension.repository.FixedExtensionSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExtensionService 테스트")
class ExtensionServiceTest {

    @Mock
    private FixedExtensionSettingRepository fixedExtensionSettingRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private CustomExtensionRepository customExtensionRepository;

    @InjectMocks
    private ExtensionService extensionService;

    private FixedExtensionSetting fixedExtensionSetting;
    private CustomExtension customExtension;

    @BeforeEach
    void setUp() {
        fixedExtensionSetting = FixedExtensionSetting.builder()
                .id(1L)
                .extension("exe")
                .isBlocked(false)
                .build();

        customExtension = CustomExtension.builder()
                .id(1L)
                .extension("zip")
                .build();
    }

    @Test
    @DisplayName("모든 고정 확장자 조회")
    void getAllFixedExtensionSettings() {
        // given
        List<FixedExtensionSetting> extensions = Arrays.asList(fixedExtensionSetting);
        when(fixedExtensionSettingRepository.findAllOrderByExtension()).thenReturn(extensions);

        // when
        List<FixedExtensionSettingDto> result = extensionService.getAllFixedExtensionSettings();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExtension()).isEqualTo("exe");
        assertThat(result.get(0).getIsBlocked()).isFalse();
    }

    @Test
    @DisplayName("고정 확장자 차단 상태 업데이트 - 성공")
    void updateFixedExtensionSetting_Success() {
        // given
        when(fixedExtensionSettingRepository.findByExtension("exe")).thenReturn(Optional.of(fixedExtensionSetting));
        when(fixedExtensionSettingRepository.save(any(FixedExtensionSetting.class))).thenReturn(fixedExtensionSetting);

        // when
        FixedExtensionSettingDto result = extensionService.updateFixedExtensionSetting("exe", true);

        // then
        assertThat(result.getIsBlocked()).isTrue();
        verify(fixedExtensionSettingRepository).save(any(FixedExtensionSetting.class));
    }

    @Test
    @DisplayName("고정 확장자 차단 상태 업데이트 - 존재하지 않는 확장자")
    void updateFixedExtensionSetting_NotFound() {
        // given
        when(fixedExtensionSettingRepository.findByExtension("nonexistent")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> extensionService.updateFixedExtensionSetting("nonexistent", true))
                .isInstanceOf(ExtensionNotFoundException.class);
    }

    @Test
    @DisplayName("모든 커스텀 확장자 조회")
    void getAllCustomExtensions() {
        // given
        List<CustomExtension> extensions = Arrays.asList(customExtension);
        when(customExtensionRepository.findAll()).thenReturn(extensions);

        // when
        List<CustomExtensionDto> result = extensionService.getAllCustomExtensions();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExtension()).isEqualTo("zip");
    }

    @Test
    @DisplayName("커스텀 확장자 추가 - 성공")
    void addCustomExtension_Success() {
        // given
        ExtensionRequest request = new ExtensionRequest("pdf");
        when(customExtensionRepository.existsByExtension("pdf")).thenReturn(false);
        when(customExtensionRepository.countCustomExtensions()).thenReturn(5L);
        when(customExtensionRepository.save(any(CustomExtension.class))).thenReturn(customExtension);

        // when
        CustomExtensionDto result = extensionService.addCustomExtension(request);

        // then
        assertThat(result.getExtension()).isEqualTo("zip");
        verify(customExtensionRepository).save(any(CustomExtension.class));
    }

    @Test
    @DisplayName("커스텀 확장자 추가 - 중복된 확장자")
    void addCustomExtension_Duplicate() {
        // given
        ExtensionRequest request = new ExtensionRequest("pdf");
        when(customExtensionRepository.existsByExtension("pdf")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> extensionService.addCustomExtension(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 확장자입니다.");
    }

    @Test
    @DisplayName("커스텀 확장자 추가 - 최대 개수 초과")
    void addCustomExtension_MaxCount() {
        // given
        ExtensionRequest request = new ExtensionRequest("pdf");
        when(customExtensionRepository.existsByExtension("pdf")).thenReturn(false);
        when(customExtensionRepository.countCustomExtensions()).thenReturn(200L);

        // when & then
        assertThatThrownBy(() -> extensionService.addCustomExtension(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("커스텀 확장자는 최대 200개까지 추가 가능합니다.");
    }

    @Test
    @DisplayName("커스텀 확장자 삭제 - 성공")
    void deleteCustomExtension_Success() {
        // given
        when(customExtensionRepository.findById(1L)).thenReturn(Optional.of(customExtension));

        // when
        extensionService.deleteCustomExtension(1L);

        // then
        verify(customExtensionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("커스텀 확장자 삭제 - 존재하지 않는 ID")
    void deleteCustomExtension_NotFound() {
        // given
        when(customExtensionRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> extensionService.deleteCustomExtension(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 확장자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 고정 확장자 차단됨")
    void isExtensionBlocked_FixedBlocked() {
        // given
        when(fixedExtensionSettingRepository.findByExtension("exe")).thenReturn(Optional.of(
                FixedExtensionSetting.builder().extension("exe").isBlocked(true).build()));
        when(customExtensionRepository.findAllExtensions()).thenReturn(Arrays.asList("zip"));

        // when
        boolean result = extensionService.isExtensionBlocked("malware.exe");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 커스텀 확장자 차단됨")
    void isExtensionBlocked_CustomBlocked() {
        // given
        when(fixedExtensionSettingRepository.findByExtension("zip")).thenReturn(Optional.empty());
        when(customExtensionRepository.findAllExtensions()).thenReturn(Arrays.asList("zip", "rar"));

        // when
        boolean result = extensionService.isExtensionBlocked("archive.zip");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 허용된 확장자")
    void isExtensionBlocked_Allowed() {
        // given
        when(fixedExtensionSettingRepository.findByExtension("txt")).thenReturn(Optional.empty());
        when(customExtensionRepository.findAllExtensions()).thenReturn(Arrays.asList("zip"));

        // when
        boolean result = extensionService.isExtensionBlocked("document.txt");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("확장자 차단 여부 확인 - 확장자가 없는 파일")
    void isExtensionBlocked_NoExtension() {
        // when
        boolean result = extensionService.isExtensionBlocked("README");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("확장자 정규화 테스트 - 점으로 시작하는 확장자")
    void addCustomExtension_NormalizeDotPrefix() {
        // given
        ExtensionRequest request = new ExtensionRequest(".pdf");
        when(customExtensionRepository.existsByExtension("pdf")).thenReturn(false);
        when(customExtensionRepository.countCustomExtensions()).thenReturn(5L);
        when(customExtensionRepository.save(any(CustomExtension.class))).thenReturn(customExtension);

        // when
        extensionService.addCustomExtension(request);

        // then
        verify(customExtensionRepository).existsByExtension("pdf");
    }
}