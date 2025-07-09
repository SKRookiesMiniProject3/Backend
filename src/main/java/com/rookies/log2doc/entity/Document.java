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
    @Column(nullable = false)
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
    @Column(nullable = false)
    private String fileName;   // 원본 파일명

    @Column(nullable = false)
    private String filePath;   // UUID 기반 저장 경로 (해시)

    @Column(name = "file_path_nfs", nullable = false)
    private String filePathNfs;  // nfs에 저장한 파일 물리 경로

    @Column(nullable = false)
    private Long fileSize;     // 파일 크기 (Byte)

    @Column(nullable = false)
    private String mimeType;   // MIME 타입

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; // 기본값 false


    /**
     * 카테고리 FK: @OneToMany 연관관계로 DocumentCategory 테이블에서 관리됨
     */
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<DocumentCategory> documentCategories = new ArrayList<>();

    /**
     * 문서 status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    /**
     * 작성자 ID 또는 이름
     */
    @Column(nullable = false)
    private String author;

    /**
     * 작성자 권한 저장 필드
     */
    @Column(name = "created_role")
    private String createdRole;

    /**
     * 생성 일시
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 접근 권한 정보
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "read_role_id", nullable = false)
    private Role readRole;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = DocumentStatus.PROCESSING;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
