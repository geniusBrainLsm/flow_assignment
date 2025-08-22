package com.assignment.fileextension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "에러 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    @Schema(description = "에러 메시지", example = "이미 등록된 확장자입니다.")
    private String error;
}