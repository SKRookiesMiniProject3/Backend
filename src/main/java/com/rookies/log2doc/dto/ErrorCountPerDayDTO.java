package com.rookies.log2doc.dto;

import java.time.LocalDate;

public class ErrorCountPerDayDTO {
    private LocalDate date;
    private Long count;

    // 기본 생성자
    public ErrorCountPerDayDTO() {
    }

    // HQL 쿼리와 일치하는 생성자
    public ErrorCountPerDayDTO(LocalDate date, Long count) {
        this.date = date;
        this.count = count;
    }

    // 쿼리가 java.sql.Date를 반환하는 경우 대안 생성자
    public ErrorCountPerDayDTO(java.sql.Date date, Long count) {
        this.date = date != null ? date.toLocalDate() : null;
        this.count = count;
    }

    // Getters and setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "ErrorCountPerDayDTO{" +
                "date=" + date +
                ", count=" + count +
                '}';
    }
}