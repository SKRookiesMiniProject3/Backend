package com.rookies.log2doc.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 텍스트 문서 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class DocumentCreateRequest {

    // 문서 제목
    private String title;

    // 문서 본문 내용
    private String content;

    // 문서 카테고리
    private String category;

    // 읽기 권한 Role ID
    private Long readRoleId;

    // 쓰기 권한 Role ID
    private Long writeRoleId;

    // 삭제 권한 Role ID
    private Long deleteRoleId;
}