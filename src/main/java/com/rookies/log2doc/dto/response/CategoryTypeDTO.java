package com.rookies.log2doc.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryTypeDTO {
    private Long id;
    private String name;
    private String description;
}
