package com.rookies.log2doc.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentCreateRequest {
    private String title;
    private String content;
    private String category;
    private Long readRoleId;
    private Long writeRoleId;
    private Long deleteRoleId;
}
