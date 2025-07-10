package com.rookies.log2doc.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 문서 카테고리 타입 응답 DTO.
 * 문서에 연결된 카테고리 정보를 클라이언트에 전달할 때 사용됨.
 */
@Data
@Builder
public class CategoryTypeDTO {

    /** 카테고리 타입 ID */
    private Long id;

    /** 카테고리명 */
    private String name;

    /** 카테고리 설명 */
    private String description;
}
