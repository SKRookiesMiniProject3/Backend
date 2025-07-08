package com.rookies.log2doc.repository;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.entity.ErrorReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ErrorReportRepository extends JpaRepository<ErrorReport, Long> {

    // ✅ 일별 에러 카운트
    @Query("SELECT new com.rookies.log2doc.dto.ErrorCountPerDayDTO(" +
            "CAST(e.createdAt AS date), COUNT(e)) " +
            "FROM ErrorReport e " +
            "GROUP BY CAST(e.createdAt AS date) " +
            "ORDER BY CAST(e.createdAt AS date) DESC")
    List<ErrorCountPerDayDTO> findDailyErrorCounts();

    // ✅ 최신순 리스트
    List<ErrorReport> findAllByOrderByCreatedAtDesc();

    // ✅ 미해결 리스트
    List<ErrorReport> findByResolvedFalseOrderByCreatedAtDesc();
}
