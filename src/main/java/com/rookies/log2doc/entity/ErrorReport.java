package com.rookies.log2doc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(length = 100)
    private String errorCode;

    @Column(length = 500)
    private String description; // 상세 설명

    @Column(length = 50)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(length = 100)
    private String location; // 에러 발생 위치

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime resolvedAt; // 해결된 시간

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.resolved == null) {
            this.resolved = false;
        }
    }

    // 비즈니스 메서드
    public void resolve() {
        this.resolved = true;
        this.resolvedAt = LocalDateTime.now();
    }

    public boolean isResolved() {
        return Boolean.TRUE.equals(this.resolved);
    }
}