package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 문서 카테고리 타입 엔티티.
 * 예: 보안, 개인정보, 운영 등 카테고리 분류 단위로 사용됨.
 */
@Entity
@Table(name = "category_type")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CategoryType {

    /** PK: 카테고리 타입 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 카테고리명 (유일) */
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    /** 카테고리 설명 (선택) */
    @Column(length = 255)
    private String description;

    /** 삭제 여부 (소프트 삭제용) */
    @Column(nullable = false)
    private Boolean isDeleted = false;

    /** 생성일시 (엔티티 생성 시 자동 설정) */
    @Column(nullable = false)
    private LocalDateTime createdDt = LocalDateTime.now();

    /** 삭제일시 (삭제 처리 시 설정) */
    private LocalDateTime deletedDt;

    /**
     * 엔티티 persist 전 기본값 설정
     */
    @PrePersist
    public void prePersist() {
        this.createdDt = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    /** 연관된 문서-카테고리 매핑 리스트 (양방향 연관관계) */
    @OneToMany(mappedBy = "categoryType")
    private List<DocumentCategory> documentCategories = new ArrayList<>();
}
