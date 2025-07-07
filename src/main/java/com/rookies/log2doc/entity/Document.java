package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    private DocumentClassification classification;  // 기밀 등급 필드 추가

    private String owner;  // 작성자 user_id

    /**
     * 파일 업로드 관련 정보
     */
    private String fileName;   // 원본 파일명
    private String filePath;   // UUID 기반 저장 경로 (해시)
    private Long fileSize;     // 파일 크기 (Byte)
    private String mimeType;   // MIME 타입

    /**
     * 카테고리 FK 필드 제거됨
     * @OneToMany 연관관계로 대체
     */
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentCategory> documentCategories = new ArrayList<>();

    /**
     * 문서 status 필드 추가
     * */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    /**
     * 작성자 ID 또는 이름
     */
    @Column(nullable = true)
    private String author;

    /**
     * 작성자 권한 저장 필드
     */
    @Column(name = "created_role")
    private String createdRole;

    /**
     * 소프트 삭제 여부
     */
    @Column(nullable = false)
    private Boolean isDeleted;

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
     * 수정일 자동 갱신
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.status == null) {
            this.status = DocumentStatus.PROCESSING;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

}
