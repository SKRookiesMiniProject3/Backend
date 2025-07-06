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

    /**
     * 문서 제목
     */
    @Column(nullable = true)
    private String title;

    /**
     * 텍스트 문서 내용
     */
    @Lob
    @Column(nullable = true)
    private String content;

    /**
     * 파일 업로드 관련 정보
     */
    private String fileName;   // 원본 파일명
    private String filePath;   // UUID 기반 저장 경로 (해시)
    private Long fileSize;     // 파일 크기 (Byte)
    private String mimeType;   // MIME 타입

    /**
     * 문서 카테고리 (필터링용)
     */
    @Column(nullable = true)
    private String category;

    /**
     * 작성자 ID 또는 이름
     */
    @Column(nullable = true)
    private String author;

    /**
     * 소프트 삭제 여부
     */
    @Column(nullable = false)
    private boolean isDeleted = false;

    /**
     * 생성/수정/삭제 일시
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    /**
     * 접근 권한 정보
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "read_role_id")
    private Role readRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "write_role_id")
    private Role writeRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delete_role_id")
    private Role deleteRole;

    /**
     * 생성일 기본값 처리
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 수정일 자동 갱신
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
