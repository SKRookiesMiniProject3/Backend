package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category_type")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false)
    private LocalDateTime createdDt = LocalDateTime.now();

    private LocalDateTime deletedDt;

    @PrePersist
    public void prePersist() {
        this.createdDt = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    // 연관관계 필요하면 (양방향) 추가
    @OneToMany(mappedBy = "categoryType")
    private List<DocumentCategory> documentCategories = new ArrayList<>();
}
