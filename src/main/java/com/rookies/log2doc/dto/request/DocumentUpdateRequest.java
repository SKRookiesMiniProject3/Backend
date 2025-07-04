package com.rookies.log2doc.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentUpdateRequest {
    private String title;
    private String category;
    private String content;
}
