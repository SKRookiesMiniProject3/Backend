package com.rookies.log2doc.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleDTO {
    private Long id;
    private String name;
    private String description;
    private int level;
}
