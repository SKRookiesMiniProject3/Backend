package com.rookies.log2doc.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 문서 전체 업데이트 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class DocumentUpdateRequest {

    // 문서 제목
    private String title;

    // 문서 카테고리
    private String category;

    // 문서 본문 내용
    private String content;
}
