package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 문서와 카테고리 타입의 매핑 엔티티.
 * 하나의 문서가 여러 카테고리와 연결될 수 있음.
 */
@Entity
@Table(name = "document_category")
@Getter
@Setter
public class DocumentCategory {

    /** PK: 문서-카테고리 매핑 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연관된 문서 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Document document;

    /** 연관된 카테고리 타입 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_type_id")
    private CategoryType categoryType;

    /** 생성일시 (매핑 생성 시 자동 설정) */
    @Column(nullable = false)
    private LocalDateTime createdDt;

    /** persist 시 생성일시 자동 설정 */
    @PrePersist
    public void prePersist() {
        this.createdDt = LocalDateTime.now();
    }
}
