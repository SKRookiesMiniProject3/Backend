package com.rookies.log2doc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 텍스트 문서 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class DocumentCreateRequest {

    // 파일 추가
    @NotNull(message = "파일은 필수입니다!")
    private MultipartFile file;

    // 문서 제목
    @NotBlank(message = "제목은 필수입니다!")
    private String title;

    // 문서 본문 내용
    private String content;

    // FK: 카테고리 타입 ID
    @NotNull(message = "카테고리는 필수입니다!")
    private Long categoryTypeId;

    // 읽기 권한 Role ID
    @NotNull(message = "읽기 권한은 필수입니다!")
    private Long readRoleId;
}
