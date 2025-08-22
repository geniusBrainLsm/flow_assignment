package com.assignment.fileextension.performance;

import com.assignment.fileextension.entity.UploadedFile;
import com.assignment.fileextension.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
@Transactional
@DisplayName("쿼리 최적화 성능 테스트")
class QueryOptimizationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @SpyBean
    private StorageService storageService;

    @Test
    @DisplayName("파일 조회 최적화 - findById 단일 쿼리 사용 검증")
    void fileQuery_OptimizedSingleQuery() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // given: 존재하지 않는 파일 ID
        Long nonExistentFileId = 999L;

        // when: 파일 조회 API 호출
        mockMvc.perform(get("/api/files/" + nonExistentFileId))
                .andExpect(status().isNotFound());

        // then: 최적화 검증
        // 1. findById 메서드가 정확히 1번만 호출되어야 함
        verify(storageService, times(1)).findById(nonExistentFileId);
        
        // 2. 비효율적인 getFilesByStatus 메서드는 호출되지 않아야 함
        verify(storageService, never()).getFilesByStatus(any());
        
        // 3. 추가적인 불필요한 DB 호출이 없어야 함
        verifyNoMoreInteractions(storageService);
    }

    @Test
    @DisplayName("파일 다운로드 최적화 - 중복 쿼리 제거 검증")
    void fileDownload_OptimizedQuery() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // given: 존재하지 않는 파일 ID
        Long nonExistentFileId = 888L;

        // when: 파일 다운로드 API 호출
        mockMvc.perform(get("/api/files/" + nonExistentFileId + "/download"))
                .andExpect(status().isNotFound());

        // then: 최적화 검증
        // 다운로드도 findById 한번만 호출되어야 함 (이전에는 여러 번 호출됨)
        verify(storageService, times(1)).findById(nonExistentFileId);
        verify(storageService, never()).getFilesByStatus(any());
    }

    @Test
    @DisplayName("성능 개선 측정 - 쿼리 횟수 최소화")
    void performanceImprovement_QueryCount() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Reset spy to get clean count
        Mockito.clearInvocations(storageService);

        Long testFileId = 777L;

        // when: 파일 정보 조회와 다운로드를 순차적으로 호출
        mockMvc.perform(get("/api/files/" + testFileId));
        mockMvc.perform(get("/api/files/" + testFileId + "/download"));

        // then: 총 2번의 findById 호출만 있어야 함 (각 API당 1번씩)
        verify(storageService, times(2)).findById(testFileId);
        
        // 비효율적인 메서드들은 호출되지 않아야 함
        verify(storageService, never()).getFilesByStatus(UploadedFile.FileStatus.ACTIVE);
        verify(storageService, never()).getFilesByStatus(UploadedFile.FileStatus.DELETED);
    }

    @Test
    @DisplayName("메모리 사용량 최적화 - 불필요한 전체 로딩 방지")
    void memoryOptimization_NoFullLoading() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Reset spy
        Mockito.clearInvocations(storageService);

        // when: 여러 파일 개별 조회
        mockMvc.perform(get("/api/files/1"));
        mockMvc.perform(get("/api/files/2"));
        mockMvc.perform(get("/api/files/3"));

        // then: 각 파일마다 개별적으로 조회되어야 함 (전체 로딩 X)
        verify(storageService, times(3)).findById(anyLong());
        
        // 전체 상태별 조회는 발생하지 않아야 함 (메모리 절약)
        verify(storageService, never()).getFilesByStatus(any());
    }
}