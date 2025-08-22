package com.assignment.fileextension;

import com.assignment.fileextension.dto.ExtensionRequest;
import com.assignment.fileextension.entity.FixedExtension;
import com.assignment.fileextension.repository.CustomExtensionRepository;
import com.assignment.fileextension.repository.FixedExtensionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
@Transactional
@DisplayName("파일 확장자 차단 시스템 통합 테스트")
class FileExtensionIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FixedExtensionRepository fixedExtensionRepository;

    @Autowired
    private CustomExtensionRepository customExtensionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 고정 확장자 초기 데이터 확인 및 설정
        if (fixedExtensionRepository.count() == 0) {
            fixedExtensionRepository.save(FixedExtension.builder()
                    .extension("exe")
                    .isBlocked(false)
                    .build());
        }
    }

    @Test
    @DisplayName("전체 워크플로우 테스트 - 고정 확장자 차단 및 파일 업로드 검증")
    void fullWorkflow_FixedExtensionBlocking() throws Exception {
        // 1. 고정 확장자 목록 조회
        mockMvc.perform(get("/api/extensions/fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 2. exe 확장자 차단 설정
        Map<String, Boolean> blockRequest = new HashMap<>();
        blockRequest.put("isBlocked", true);

        mockMvc.perform(put("/api/extensions/fixed/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));

        // 3. exe 파일 차단 확인
        mockMvc.perform(get("/api/extensions/check")
                        .param("fileName", "malware.exe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));

        // 4. 허용된 파일 확인
        mockMvc.perform(get("/api/extensions/check")
                        .param("fileName", "document.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(false));
    }

    @Test
    @DisplayName("전체 워크플로우 테스트 - 커스텀 확장자 관리")
    void fullWorkflow_CustomExtensionManagement() throws Exception {
        // 1. 커스텀 확장자 추가
        ExtensionRequest addRequest = new ExtensionRequest("zip");

        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extension").value("zip"));

        // 2. 커스텀 확장자 목록 조회
        mockMvc.perform(get("/api/extensions/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].extension").value("zip"));

        // 3. 커스텀 확장자로 파일 차단 확인
        mockMvc.perform(get("/api/extensions/check")
                        .param("fileName", "archive.zip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));

        // 4. 커스텀 확장자 삭제
        mockMvc.perform(delete("/api/extensions/custom/1"))
                .andExpect(status().isOk());

        // 5. 삭제 후 차단 해제 확인
        mockMvc.perform(get("/api/extensions/check")
                        .param("fileName", "archive.zip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(false));
    }

    @Test
    @DisplayName("에러 시나리오 테스트 - 중복 확장자 추가")
    void errorScenario_DuplicateExtension() throws Exception {
        // 1. 첫 번째 확장자 추가
        ExtensionRequest request1 = new ExtensionRequest("pdf");
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // 2. 같은 확장자 중복 추가 시도
        ExtensionRequest request2 = new ExtensionRequest("pdf");
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("이미 등록된 확장자입니다."));
    }

    @Test
    @DisplayName("에러 시나리오 테스트 - 유효하지 않은 데이터")
    void errorScenario_InvalidData() throws Exception {
        // 1. 빈 확장자
        ExtensionRequest emptyRequest = new ExtensionRequest("");
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());

        // 2. 너무 긴 확장자 (21자)
        ExtensionRequest longRequest = new ExtensionRequest("a".repeat(21));
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longRequest)))
                .andExpect(status().isBadRequest());

        // 3. 존재하지 않는 고정 확장자 업데이트
        Map<String, Boolean> updateRequest = new HashMap<>();
        updateRequest.put("isBlocked", true);
        
        mockMvc.perform(put("/api/extensions/fixed/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("대소문자 구분 없이 확장자 처리")
    void caseInsensitiveExtensionHandling() throws Exception {
        // 1. 소문자 확장자 추가
        ExtensionRequest request = new ExtensionRequest("PDF");
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extension").value("pdf")); // 소문자로 저장됨

        // 2. 대소문자 다른 파일명으로 차단 확인
        mockMvc.perform(get("/api/extensions/check")
                        .param("fileName", "DOCUMENT.PDF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(true));
    }
}