package com.rookies.log2doc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DocumentResponseDTO {
    private Long id;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String author;
    private String owner;
    private String createdRole;
    private LocalDateTime createdAt;

    private String status;

    private RoleDTO readRole;

    private List<CategoryTypeDTO> categories;
}
