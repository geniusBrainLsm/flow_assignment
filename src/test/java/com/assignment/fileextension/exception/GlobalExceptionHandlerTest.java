package com.assignment.fileextension.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("파일 크기 초과 예외 처리 - 100MB 메시지")
    void handleMaxSizeException_100MB_Message() {
        // given
        MaxUploadSizeExceededException exception = 
                new MaxUploadSizeExceededException(104857600L); // 100MB

        // when
        ResponseEntity<Map<String, Object>> response = 
                exceptionHandler.handleMaxSizeException(exception);

        // then
        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("error").toString()).contains("100MB");
        assertThat(body.get("type")).isEqualTo("FILE_SIZE_EXCEEDED");
    }

    @Test
    @DisplayName("파일 검증 예외 처리")
    void handleFileValidationException() {
        // given
        FileValidationException exception = 
                new FileValidationException("차단된 확장자입니다", "exe");

        // when
        ResponseEntity<Map<String, Object>> response = 
                exceptionHandler.handleFileValidationException(exception);

        // then
        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("error")).isEqualTo("차단된 확장자입니다");
        assertThat(body.get("type")).isEqualTo("FILE_VALIDATION_ERROR");
        assertThat(body.get("blockedExtension")).isEqualTo("exe");
    }

    @Test
    @DisplayName("확장자 찾을 수 없음 예외 처리")
    void handleExtensionNotFoundException() {
        // given
        ExtensionNotFoundException exception = 
                new ExtensionNotFoundException("pdf");

        // when
        ResponseEntity<Map<String, Object>> response = 
                exceptionHandler.handleExtensionNotFoundException(exception);

        // then
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    @DisplayName("일반 예외 처리")
    void handleGeneralException() {
        // given
        Exception exception = new RuntimeException("예상치 못한 오류");

        // when
        ResponseEntity<Map<String, Object>> response = 
                exceptionHandler.handleGeneralException(exception);

        // then
        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("error")).isEqualTo("서버 내부 오류가 발생했습니다");
        assertThat(body.get("type")).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("IllegalArgumentException 처리")
    void handleIllegalArgumentException() {
        // given
        IllegalArgumentException exception = 
                new IllegalArgumentException("잘못된 매개변수");

        // when
        ResponseEntity<Map<String, Object>> response = 
                exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("error")).isEqualTo("잘못된 매개변수");
        assertThat(body.get("type")).isEqualTo("INVALID_ARGUMENT");
    }
}