package com.rookies.log2doc.controller;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.dto.ErrorReportDTO;
import com.rookies.log2doc.dto.response.ApiResponse;
import com.rookies.log2doc.service.ErrorReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/error-reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "에러 리포트 조회", description = "AI가 생성한 에러 리포트 조회 API (읽기 전용)")
public class ErrorReportController {

    private final ErrorReportService errorReportService;

    // ========================================
    // 대시보드/통계 API
    // ========================================

    // ✅ 대시보드 요약 정보
    @GetMapping("/dashboard/summary")
    @Operation(summary = "대시보드 요약 정보", description = "전체 리포트 요약 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary(HttpServletRequest request) {
        try {
            Map<String, Object> summary = errorReportService.getDashboardSummary();

            request.setAttribute("error_report_action", "dashboard_summary");

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("대시보드 요약 정보 조회 성공")
                    .data(summary)
                    .build());

        } catch (Exception e) {
            log.error("대시보드 요약 정보 조회 실패", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("대시보드 요약 정보 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    // ✅ 보안 대시보드 요약 (공격 관련만)
    @GetMapping("/dashboard/security")
    @Operation(summary = "보안 대시보드 요약", description = "공격 탐지 관련 요약 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityDashboardSummary(HttpServletRequest request) {
        try {
            Map<String, Object> securitySummary = errorReportService.getSecurityDashboardSummary();

            request.setAttribute("error_report_action", "security_dashboard");

            log.info("🛡️ 보안 대시보드 조회: {}", securitySummary);

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("보안 대시보드 요약 정보 조회 성공")
                    .data(securitySummary)
                    .build());

        } catch (Exception e) {
            log.error("보안 대시보드 요약 정보 조회 실패", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("보안 대시보드 요약 정보 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    // ✅ 일별 에러 카운트
    @GetMapping("/analytics/daily-count")
    @Operation(summary = "일별 에러 카운트 조회", description = "날짜별 에러 발생 개수를 조회합니다.")
    public ResponseEntity<List<ErrorCountPerDayDTO>> getDailyCounts(HttpServletRequest request) {
        List<ErrorCountPerDayDTO> counts = errorReportService.getDailyCounts();

        request.setAttribute("error_report_action", "daily_count");
        request.setAttribute("result_count", counts.size());

        return ResponseEntity.ok(counts);
    }

    // ✅ 리포트 상태별 통계
    @GetMapping("/analytics/statistics")
    @Operation(summary = "리포트 통계 조회", description = "리포트 상태별 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getReportStatistics(HttpServletRequest request) {
        try {
            Map<String, Long> statistics = errorReportService.getReportStatistics();

            request.setAttribute("error_report_action", "statistics");

            return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                    .success(true)
                    .message("리포트 통계 조회 성공")
                    .data(statistics)
                    .build());

        } catch (Exception e) {
            log.error("리포트 통계 조회 실패", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Long>>builder()
                            .success(false)
                            .message("통계 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    // ✅ 카테고리별 통계 (AI 분류 결과)
    @GetMapping("/analytics/category-statistics")
    @Operation(summary = "카테고리별 리포트 통계", description = "AI가 분류한 카테고리별 에러 리포트 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCategoryStatistics(HttpServletRequest request) {
        try {
            Map<String, Long> statistics = errorReportService.getCategoryStatistics();

            request.setAttribute("error_report_action", "category_statistics");

            return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                    .success(true)
                    .message("카테고리별 리포트 통계 조회 성공")
                    .data(statistics)
                    .build());

        } catch (Exception e) {
            log.error("카테고리별 리포트 통계 조회 실패", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Long>>builder()
                            .success(false)
                            .message("카테고리별 통계 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    // ✅ 최근 공격 탐지 건수
    @GetMapping("/analytics/recent-attacks")
    @Operation(summary = "최근 공격 탐지 건수", description = "최근 N일간의 공격 탐지 건수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> getRecentAttackCount(
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest request) {
        try {
            long attackCount = errorReportService.getRecentAttackCount(days);

            request.setAttribute("error_report_action", "recent_attacks");
            request.setAttribute("days_range", days);

            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true)
                    .message(String.format("최근 %d일간 공격 탐지 건수 조회 성공", days))
                    .data(attackCount)
                    .build());

        } catch (Exception e) {
            log.error("최근 공격 탐지 건수 조회 실패", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Long>builder()
                            .success(false)
                            .message("공격 탐지 건수 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    // ========================================
    // 목록 조회 API (AI가 생성한 데이터 조회)
    // ========================================

    // ✅ 최신순 리스트
    @GetMapping("/list/latest")
    @Operation(summary = "최신 에러 리스트 조회", description = "AI가 생성한 최신순 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getLatestReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getLatestReports();

        request.setAttribute("error_report_action", "latest_list");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 진행중인 리포트 조회
    @GetMapping("/list/in-progress")
    @Operation(summary = "진행중인 리포트 조회", description = "현재 진행중인 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getInProgressReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getInProgressReports();

        request.setAttribute("error_report_action", "in_progress_list");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 완료된 리포트 조회
    @GetMapping("/list/completed")
    @Operation(summary = "완료된 리포트 조회", description = "완료된 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getCompletedReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getCompletedReports();

        request.setAttribute("error_report_action", "completed_list");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 시작되지 않은 리포트 조회
    @GetMapping("/list/not-started")
    @Operation(summary = "시작되지 않은 리포트 조회", description = "아직 시작되지 않은 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getNotStartedReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getNotStartedReports();

        request.setAttribute("error_report_action", "not_started_list");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ========================================
    // 카테고리별 조회 API (AI 분류 결과)
    // ========================================

    // ✅ 공격 탐지 리포트 조회 (AI가 분류한 중요 데이터!)
    @GetMapping("/list/attacks")
    @Operation(summary = "공격 탐지 리포트 조회", description = "AI가 공격으로 분류한 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getAttackReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getAttackReports();

        request.setAttribute("error_report_action", "attack_reports");
        request.setAttribute("result_count", reports.size());

        // 공격 탐지는 중요하므로 로그 레벨 높임
        log.warn("🚨 공격 탐지 리포트 조회 요청 - 총 {} 건", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 정상 리포트 조회
    @GetMapping("/list/valid")
    @Operation(summary = "정상 리포트 조회", description = "AI가 정상으로 분류한 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getValidReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getValidReports();

        request.setAttribute("error_report_action", "valid_reports");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 비정상 리포트 조회
    @GetMapping("/list/invalid")
    @Operation(summary = "비정상 리포트 조회", description = "AI가 비정상으로 분류한 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getInvalidReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getInvalidReports();

        request.setAttribute("error_report_action", "invalid_reports");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 심각도 높은 리포트 조회 (보안 대시보드용)
    @GetMapping("/list/critical")
    @Operation(summary = "심각도 높은 리포트 조회", description = "공격 카테고리의 진행중/완료 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getCriticalReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getCriticalReports();

        request.setAttribute("error_report_action", "critical_reports");
        request.setAttribute("result_count", reports.size());

        log.warn("🚨 심각도 높은 리포트 조회 요청 - 총 {} 건", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ========================================
    // 검색/필터 API
    // ========================================

    // ✅ 제목으로 검색
    @GetMapping("/search")
    @Operation(summary = "리포트 제목 검색", description = "제목으로 에러 리포트를 검색합니다.")
    public ResponseEntity<List<ErrorReportDTO>> searchReportsByTitle(
            @RequestParam String title,
            HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.searchReportsByTitle(title);

        request.setAttribute("error_report_action", "search_by_title");
        request.setAttribute("search_keyword", title);
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 기간별 조회
    @GetMapping("/list/by-date-range")
    @Operation(summary = "기간별 리포트 조회", description = "특정 기간의 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getReportsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getReportsByDateRange(startDate, endDate);

        request.setAttribute("error_report_action", "date_range_query");
        request.setAttribute("start_date", startDate);
        request.setAttribute("end_date", endDate);
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 상태와 카테고리 조합 조회
    @GetMapping("/list/by-status-category")
    @Operation(summary = "상태-카테고리 조합 조회", description = "특정 상태와 카테고리 조합으로 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getReportsByStatusAndCategory(
            @RequestParam String status,
            @RequestParam String category,
            HttpServletRequest request) {
        try {
            List<ErrorReportDTO> reports = errorReportService.getReportsByStatusAndCategory(status, category);

            request.setAttribute("error_report_action", "status_category_query");
            request.setAttribute("filter_status", status);
            request.setAttribute("filter_category", category);
            request.setAttribute("result_count", reports.size());

            return ResponseEntity.ok(reports);

        } catch (RuntimeException e) {
            request.setAttribute("error_report_action", "status_category_query_failed");
            request.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of());
        }
    }

    // ========================================
    // 단일 조회 API
    // ========================================

    // ✅ 파일 경로로 리포트 조회
    @GetMapping("/by-path")
    @Operation(summary = "파일 경로로 리포트 조회", description = "리포트 파일 경로로 에러 리포트를 조회합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> getReportByPath(
            @RequestParam String reportPath,
            HttpServletRequest request) {
        try {
            ErrorReportDTO report = errorReportService.getReportByPath(reportPath);

            request.setAttribute("error_report_action", "query_by_path");
            request.setAttribute("report_path", reportPath);

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("파일 경로로 리포트 조회 성공")
                    .data(report)
                    .build());

        } catch (RuntimeException e) {
            request.setAttribute("error_report_action", "query_by_path_failed");
            request.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message("해당 경로의 리포트를 찾을 수 없습니다.")
                            .build());
        }
    }

    // ✅ 에러 리포트 상세 조회 (ID 기준)
    @GetMapping("/{id}")
    @Operation(summary = "에러 리포트 상세 조회", description = "특정 에러 리포트의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> getReportById(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            ErrorReportDTO report = errorReportService.getReportById(id);

            request.setAttribute("error_report_id", report.getId());
            request.setAttribute("error_report_action", "detail_view");
            request.setAttribute("report_status", report.getReportStatus());
            request.setAttribute("report_category", report.getReportCategory());

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러 리포트 상세 정보 조회 성공")
                    .data(report)
                    .build());

        } catch (RuntimeException e) {
            request.setAttribute("error_report_id", id);
            request.setAttribute("error_report_action", "detail_view_failed");
            request.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message("에러 리포트를 찾을 수 없습니다.")
                            .build());
        }
    }
}