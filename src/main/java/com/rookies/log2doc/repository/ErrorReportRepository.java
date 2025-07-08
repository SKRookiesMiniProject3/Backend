package com.rookies.log2doc.repository;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.entity.ErrorReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ErrorReportRepository extends JpaRepository<ErrorReport, Long> {

    // ✅ 일별 에러 카운트 (삭제되지 않은 것만)
    @Query("SELECT new com.rookies.log2doc.dto.ErrorCountPerDayDTO(" +
            "CAST(e.createdDt AS date), COUNT(e)) " +
            "FROM ErrorReport e " +
            "WHERE e.isDeleted = false " +
            "GROUP BY CAST(e.createdDt AS date) " +
            "ORDER BY CAST(e.createdDt AS date) DESC")
    List<ErrorCountPerDayDTO> findDailyErrorCounts();

    // ✅ 최신순 리스트 (삭제되지 않은 것만)
    List<ErrorReport> findByIsDeletedFalseOrderByCreatedDtDesc();

    // ✅ 진행중인 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = 'IN_PROGRESS' ORDER BY e.createdDt DESC")
    List<ErrorReport> findInProgressReports();

    // ✅ 완료된 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = 'COMPLETED' ORDER BY e.createdDt DESC")
    List<ErrorReport> findCompletedReports();

    // ✅ 시작되지 않은 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.reportStatus = 'NOT_STARTED' ORDER BY e.createdDt DESC")
    List<ErrorReport> findNotStartedReports();

    // ✅ 특정 사용자가 원인인 에러 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.errorSourceMember = :memberId ORDER BY e.createdDt DESC")
    List<ErrorReport> findByErrorSourceMember(@Param("memberId") Long memberId);

    // ✅ 특정 기간 내 에러 리포트 조회
    @Query("SELECT e FROM ErrorReport e WHERE e.isDeleted = false AND e.createdDt BETWEEN :startDate AND :endDate ORDER BY e.createdDt DESC")
    List<ErrorReport> findByCreatedDtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ✅ 삭제되지 않은 리포트 중 ID로 조회
    Optional<ErrorReport> findByIdAndIsDeletedFalse(Long id);

    // ✅ 리포트 상태별 개수 조회
    @Query("SELECT e.reportStatus, COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false GROUP BY e.reportStatus")
    List<Object[]> countByReportStatus();

    // ✅ 전체 리포트 개수 (삭제되지 않은 것만)
    long countByIsDeletedFalse();

    // ✅ 오늘 생성된 리포트 개수
    @Query("SELECT COUNT(e) FROM ErrorReport e WHERE e.isDeleted = false AND DATE(e.createdDt) = CURRENT_DATE")
    long countTodayReports();
}