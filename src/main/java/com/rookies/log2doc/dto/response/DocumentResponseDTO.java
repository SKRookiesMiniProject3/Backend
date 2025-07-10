package com.rookies.log2doc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 문서 상세 조회 및 목록 조회 시 클라이언트에 전달되는 문서 응답 DTO.
 */
@Data
@Builder
public class DocumentResponseDTO {

    /** 문서 ID (PK) */
    private Long id;

    /** 문서 파일명 */
    private String fileName;

    /** 문서 파일 저장 경로 */
    private String filePath;

    /** 문서 파일 크기 (바이트) */
    private Long fileSize;

    /** 문서 MIME 타입 (예: application/pdf) */
    private String mimeType;

    /** 작성자 이름 */
    private String author;

    /** 소유자 이름 */
    private String owner;

    /** 생성 시점 사용자 역할 */
    private String createdRole;

    /** 문서 생성 일시 */
    private LocalDateTime createdAt;

    /** 문서 상태 (예: PROCESSING, COMPLETED, FAILED 등) */
    private String status;

    /** 열람 가능한 권한 정보 */
    private RoleDTO readRole;

    /** 문서에 연결된 카테고리 목록 */
    private List<CategoryTypeDTO> categories;
}
