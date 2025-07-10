package com.rookies.log2doc.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 사용자 권한(Role) 정보를 외부에 전달할 때 사용하는 DTO.
 * 주로 사용자 상세 조회, 권한 목록 조회 등에 활용됨.
 */
@Data
@Builder
public class RoleDTO {

    /** 권한 ID (PK) */
    private Long id;

    /** 권한 이름 (예: ROLE_USER, ROLE_ADMIN 등) */
    private String name;

    /** 권한 설명 */
    private String description;

    /** 권한 수준 (숫자가 높을수록 높은 권한) */
    private int level;
}
