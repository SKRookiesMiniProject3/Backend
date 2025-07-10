package com.rookies.log2doc.dto;

import java.time.LocalDate;

/**
 * 일자별 에러 카운트 결과를 담는 DTO.
 * 통계성 조회(HQL/JPQL) 쿼리 결과 매핑에 사용됨.
 */
public class ErrorCountPerDayDTO {

    /** 에러 발생 일자 */
    private LocalDate date;

    /** 해당 일자의 에러 개수 */
    private Long count;

    /** 기본 생성자 (필수) */
    public ErrorCountPerDayDTO() {
    }

    /**
     * JPQL/HQL 쿼리 결과와 매핑되는 생성자.
     *
     * @param date  에러 발생 일자 (java.time.LocalDate)
     * @param count 에러 개수
     */
    public ErrorCountPerDayDTO(LocalDate date, Long count) {
        this.date = date;
        this.count = count;
    }

    /**
     * 쿼리가 java.sql.Date 타입을 반환할 때 사용할 생성자.
     *
     * @param date  java.sql.Date 타입 일자
     * @param count 에러 개수
     */
    public ErrorCountPerDayDTO(java.sql.Date date, Long count) {
        this.date = date != null ? date.toLocalDate() : null;
        this.count = count;
    }

    // Getter & Setter
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
