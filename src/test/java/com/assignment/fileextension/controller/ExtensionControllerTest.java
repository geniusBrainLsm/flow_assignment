package com.assignment.fileextension.controller;

import com.assignment.fileextension.dto.CustomExtensionDto;
import com.assignment.fileextension.dto.ExtensionRequest;
import com.assignment.fileextension.dto.FixedExtensionSettingDto;
import com.assignment.fileextension.service.ExtensionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExtensionController.class)
@DisplayName("ExtensionController 테스트")
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
class ExtensionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtensionService extensionService;

    @Autowired
    private ObjectMapper objectMapper;

    private FixedExtensionSettingDto fixedExtensionSettingDto;
    private CustomExtensionDto customExtensionDto;

    @BeforeEach
    void setUp() {
        fixedExtensionSettingDto = FixedExtensionSettingDto.builder()
                .id(1L)
                .extension("exe")
                .isBlocked(false)
                .build();

        customExtensionDto = CustomExtensionDto.builder()
                .id(1L)
                .extension("zip")
                .build();
    }

    @Test
    @DisplayName("GET /api/extensions/fixed - 고정 확장자 목록 조회")
    void getFixedExtensions() throws Exception {
        // given
        List<FixedExtensionSettingDto> extensions = Arrays.asList(fixedExtensionSettingDto);
        when(extensionService.getAllFixedExtensionSettings()).thenReturn(extensions);

        // when & then
        mockMvc.perform(get("/api/extensions/fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].extension").value("exe"))
                .andExpect(jsonPath("$[0].isBlocked").value(false));
    }

    @Test
    @DisplayName("PUT /api/extensions/fixed/{extension} - 고정 확장자 상태 업데이트")
    void updateFixedExtension() throws Exception {
        // given
        FixedExtensionSettingDto updatedDto = FixedExtensionSettingDto.builder()
                .id(1L)
                .extension("exe")
                .isBlocked(true)
                .build();

        Map<String, Boolean> request = new HashMap<>();
        request.put("isBlocked", true);

        when(extensionService.updateFixedExtensionSetting("exe", true)).thenReturn(updatedDto);

        // when & then
        mockMvc.perform(put("/api/extensions/fixed/exe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.extension").value("exe"))
                .andExpect(jsonPath("$.isBlocked").value(true));
    }


    @Test
    @DisplayName("GET /api/extensions/custom - 커스텀 확장자 목록 조회")
    void getCustomExtensions() throws Exception {
        // given
        List<CustomExtensionDto> extensions = Arrays.asList(customExtensionDto);
        when(extensionService.getAllCustomExtensions()).thenReturn(extensions);

        // when & then
        mockMvc.perform(get("/api/extensions/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].extension").value("zip"));
    }

    @Test
    @DisplayName("POST /api/extensions/custom - 커스텀 확장자 추가")
    void addCustomExtension() throws Exception {
        // given
        ExtensionRequest request = new ExtensionRequest("pdf");
        when(extensionService.addCustomExtension(any(ExtensionRequest.class))).thenReturn(customExtensionDto);

        // when & then
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.extension").value("zip"));
    }

    @Test
    @DisplayName("POST /api/extensions/custom - 잘못된 요청 (중복 확장자)")
    void addCustomExtension_BadRequest() throws Exception {
        // given
        ExtensionRequest request = new ExtensionRequest("pdf");
        when(extensionService.addCustomExtension(any(ExtensionRequest.class)))
                .thenThrow(new IllegalArgumentException("이미 등록된 확장자입니다."));

        // when & then
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("이미 등록된 확장자입니다."));
    }

    @Test
    @DisplayName("DELETE /api/extensions/custom/{id} - 커스텀 확장자 삭제")
    void deleteCustomExtension() throws Exception {
        // given
        doNothing().when(extensionService).deleteCustomExtension(1L);

        // when & then
        mockMvc.perform(delete("/api/extensions/custom/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/extensions/custom/{id} - 존재하지 않는 확장자")
    void deleteCustomExtension_NotFound() throws Exception {
        // given
        doThrow(new IllegalArgumentException("해당 확장자를 찾을 수 없습니다."))
                .when(extensionService).deleteCustomExtension(1L);

        // when & then
        mockMvc.perform(delete("/api/extensions/custom/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/extensions/check - 확장자 차단 여부 확인")
    void checkExtension() throws Exception {
        // given
        when(extensionService.isExtensionBlocked(eq("malware.exe"), any())).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/extensions/check")
                        .param("fileName", "malware.exe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));
    }

    @Test
    @DisplayName("GET /api/extensions/check - 허용된 파일")
    void checkExtension_Allowed() throws Exception {
        // given
        when(extensionService.isExtensionBlocked(eq("document.txt"), any())).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/extensions/check")
                        .param("fileName", "document.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(false));
    }

    @Test
    @DisplayName("POST /api/extensions/custom - 유효성 검증 실패 (빈 확장자)")
    void addCustomExtension_ValidationError() throws Exception {
        // given
        ExtensionRequest request = new ExtensionRequest("");

        // when & then
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/extensions/custom - 유효성 검증 실패 (너무 긴 확장자)")
    void addCustomExtension_TooLongExtension() throws Exception {
        // given
        ExtensionRequest request = new ExtensionRequest("a".repeat(21)); // 21자

        // when & then
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}