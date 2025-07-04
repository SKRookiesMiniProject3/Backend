package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Lob
    private String content; // 텍스트 문서 내용

    private String fileName;   // 파일명
    private String filePath;   // 해시 경로
    private Long fileSize;     // 파일 크기
    private String mimeType;   // 파일 타입

    private String category;   // 카테고리별 필터링용

    private String author;     // 작성자 ID 또는 이름

    private boolean isDeleted = false; // Soft Delete

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "read_role_id")
    private Role readRole;

    @ManyToOne
    @JoinColumn(name = "write_role_id")
    private Role writeRole;

    @ManyToOne
    @JoinColumn(name = "delete_role_id")
    private Role deleteRole;
}
