package com.rookies.log2doc.controller;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.dto.ErrorReportDTO;
import com.rookies.log2doc.dto.request.CreateErrorReportRequest;
import com.rookies.log2doc.dto.response.ApiResponse;
import com.rookies.log2doc.service.ErrorReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/error-reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "에러 리포트 관리", description = "에러 리포트 생성 및 조회 API")
public class ErrorReportController {

    private final ErrorReportService errorReportService;

    // ========================================
    // 분석/통계 API (가장 먼저 배치)
    // ========================================

    // ✅ 일별 에러 카운트
    @GetMapping("/analytics/daily-count")
    @Operation(summary = "일별 에러 카운트 조회", description = "날짜별 에러 발생 개수를 조회합니다.")
    public ResponseEntity<List<ErrorCountPerDayDTO>> getDailyCounts(HttpServletRequest request) {
        List<ErrorCountPerDayDTO> counts = errorReportService.getDailyCounts();

        // ✅ Request Attribute 설정 (Interceptor에서 사용)
        request.setAttribute("error_report_action", "daily_count");
        request.setAttribute("result_count", counts.size());

        return ResponseEntity.ok(counts);
    }

    // ✅ 리포트 상태별 통계
    @GetMapping("/analytics/statistics")
    @Operation(summary = "리포트 통계 조회", description = "리포트 상태별 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getReportStatistics(HttpServletRequest request) {
        log.info("📊 리포트 통계 조회 요청");

        try {
            Map<String, Long> statistics = errorReportService.getReportStatistics();

            // ✅ Request Attribute 설정
            request.setAttribute("error_report_action", "statistics");
            request.setAttribute("statistics_count", statistics.size());

            log.info("📊 리포트 통계 조회 성공: {}", statistics);

            return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                    .success(true)
                    .message("리포트 통계 조회 성공")
                    .data(statistics)
                    .build());

        } catch (Exception e) {
            log.error("📊 리포트 통계 조회 실패", e);

            request.setAttribute("error_report_action", "statistics_failed");
            request.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Long>>builder()
                            .success(false)
                            .message("통계 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    // ========================================
    // 목록 조회 API
    // ========================================

    // ✅ 최신순 리스트
    @GetMapping("/list/latest")
    @Operation(summary = "최신 에러 리스트 조회", description = "최신순으로 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getLatestReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getLatestReports();

        // ✅ Request Attribute 설정
        request.setAttribute("error_report_action", "latest_list");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 진행중인 리포트 조회
    @GetMapping("/list/in-progress")
    @Operation(summary = "진행중인 리포트 조회", description = "현재 진행중인 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getInProgressReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getInProgressReports();

        // ✅ Request Attribute 설정
        request.setAttribute("error_report_action", "in_progress_list");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 완료된 리포트 조회
    @GetMapping("/list/completed")
    @Operation(summary = "완료된 리포트 조회", description = "완료된 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getCompletedReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getCompletedReports();

        // ✅ Request Attribute 설정
        request.setAttribute("error_report_action", "completed_list");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 시작되지 않은 리포트 조회
    @GetMapping("/list/not-started")
    @Operation(summary = "시작되지 않은 리포트 조회", description = "아직 시작되지 않은 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getNotStartedReports(HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getNotStartedReports();

        // ✅ Request Attribute 설정
        request.setAttribute("error_report_action", "not_started_list");
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ✅ 특정 사용자의 에러 리포트 조회
    @GetMapping("/member/{memberId}")
    @Operation(summary = "사용자별 에러 리포트 조회", description = "특정 사용자가 원인인 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getReportsByMember(
            @PathVariable Long memberId,
            HttpServletRequest request) {
        List<ErrorReportDTO> reports = errorReportService.getReportsByMember(memberId);

        // ✅ Request Attribute 설정
        request.setAttribute("error_report_action", "member_reports");
        request.setAttribute("target_member_id", memberId);
        request.setAttribute("result_count", reports.size());

        return ResponseEntity.ok(reports);
    }

    // ========================================
    // CRUD 작업들 (ID 기반)
    // ========================================

    // ✅ 에러 리포트 생성 (Request Attribute 추가)
    @PostMapping
    @Operation(summary = "에러 리포트 생성", description = "새로운 에러 리포트를 생성합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> createErrorReport(
            @Valid @RequestBody CreateErrorReportRequest request,
            HttpServletRequest servletRequest) {

        try {
            ErrorReportDTO created = errorReportService.createErrorReport(request);

            // ✅ Request Attribute 설정 (Interceptor에서 사용)
            servletRequest.setAttribute("error_report_id", created.getId());
            servletRequest.setAttribute("error_report_status", created.getReportStatus());
            servletRequest.setAttribute("error_source_member", created.getErrorSourceMember());
            servletRequest.setAttribute("error_report_action", "create");

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러 리포트가 성공적으로 생성되었습니다.")
                    .data(created)
                    .build());

        } catch (Exception e) {
            log.error("에러 리포트 생성 중 오류 발생", e);

            // ✅ 예외 발생 시 Attribute 설정
            servletRequest.setAttribute("error_report_action", "create_failed");
            servletRequest.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message("서버 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    // ✅ 에러 리포트 상세 조회 (Request Attribute 추가) - 맨 마지막에 배치
    @GetMapping("/{id}")
    @Operation(summary = "에러 리포트 상세 조회", description = "특정 에러 리포트의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> getReportById(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            ErrorReportDTO report = errorReportService.getReportById(id);

            // ✅ Request Attribute 설정
            request.setAttribute("error_report_id", report.getId());
            request.setAttribute("error_report_action", "detail_view");
            request.setAttribute("report_status", report.getReportStatus());
            request.setAttribute("error_source_member", report.getErrorSourceMember());

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러 리포트 상세 정보 조회 성공")
                    .data(report)
                    .build());

        } catch (RuntimeException e) {
            // ✅ 예외 발생 시 Attribute 설정
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

    // ✅ 에러 리포트 상태 변경
    @PatchMapping("/{id}/status")
    @Operation(summary = "에러 리포트 상태 변경", description = "에러 리포트의 상태를 변경합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> updateReportStatus(
            @PathVariable Long id,
            @RequestParam String status,
            HttpServletRequest request) {
        try {
            ErrorReportDTO updated = errorReportService.updateReportStatus(id, status);

            // ✅ Request Attribute 설정
            request.setAttribute("error_report_id", updated.getId());
            request.setAttribute("error_report_action", "status_update");
            request.setAttribute("new_status", updated.getReportStatus());
            request.setAttribute("old_status", status);

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러 리포트 상태가 변경되었습니다.")
                    .data(updated)
                    .build());

        } catch (RuntimeException e) {
            // ✅ 예외 발생 시 Attribute 설정
            request.setAttribute("error_report_id", id);
            request.setAttribute("error_report_action", "status_update_failed");
            request.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    // ✅ 에러 리포트 완료 처리
    @PatchMapping("/{id}/complete")
    @Operation(summary = "에러 리포트 완료", description = "에러 리포트를 완료 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> completeReport(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            ErrorReportDTO completed = errorReportService.completeReport(id);

            // ✅ Request Attribute 설정
            request.setAttribute("error_report_id", completed.getId());
            request.setAttribute("error_report_action", "complete");
            request.setAttribute("report_status", completed.getReportStatus());

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러 리포트가 완료되었습니다.")
                    .data(completed)
                    .build());

        } catch (RuntimeException e) {
            // ✅ 예외 발생 시 Attribute 설정
            request.setAttribute("error_report_id", id);
            request.setAttribute("error_report_action", "complete_failed");
            request.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message("에러 리포트를 찾을 수 없습니다.")
                            .build());
        }
    }

    // ✅ 에러 리포트 시작
    @PatchMapping("/{id}/start")
    @Operation(summary = "에러 리포트 시작", description = "에러 리포트를 시작합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> startReport(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            ErrorReportDTO started = errorReportService.startReport(id);

            // ✅ Request Attribute 설정
            request.setAttribute("error_report_id", started.getId());
            request.setAttribute("error_report_action", "start");
            request.setAttribute("report_status", started.getReportStatus());

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러 리포트가 시작되었습니다.")
                    .data(started)
                    .build());

        } catch (RuntimeException e) {
            // ✅ 예외 발생 시 Attribute 설정
            request.setAttribute("error_report_id", id);
            request.setAttribute("error_report_action", "start_failed");
            request.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message("에러 리포트를 찾을 수 없습니다.")
                            .build());
        }
    }

    // ✅ 에러 리포트 코멘트 수정
    @PatchMapping("/{id}/comment")
    @Operation(summary = "에러 리포트 코멘트 수정", description = "에러 리포트의 코멘트를 수정합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> updateComment(
            @PathVariable Long id,
            @RequestParam String comment,
            HttpServletRequest request) {
        try {
            ErrorReportDTO updated = errorReportService.updateComment(id, comment);

            // ✅ Request Attribute 설정
            request.setAttribute("error_report_id", updated.getId());
            request.setAttribute("error_report_action", "comment_update");
            request.setAttribute("comment_length", comment.length());

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러 리포트 코멘트가 수정되었습니다.")
                    .data(updated)
                    .build());

        } catch (RuntimeException e) {
            // ✅ 예외 발생 시 Attribute 설정
            request.setAttribute("error_report_id", id);
            request.setAttribute("error_report_action", "comment_update_failed");
            request.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message("에러 리포트를 찾을 수 없습니다.")
                            .build());
        }
    }

    // ✅ 에러 리포트 삭제
    @DeleteMapping("/{id}")
    @Operation(summary = "에러 리포트 삭제", description = "에러 리포트를 삭제합니다 (소프트 삭제).")
    public ResponseEntity<ApiResponse<Void>> deleteReport(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            errorReportService.deleteReport(id);

            // ✅ Request Attribute 설정
            request.setAttribute("error_report_id", id);
            request.setAttribute("error_report_action", "delete");

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("에러 리포트가 삭제되었습니다.")
                    .build());

        } catch (RuntimeException e) {
            // ✅ 예외 발생 시 Attribute 설정
            request.setAttribute("error_report_id", id);
            request.setAttribute("error_report_action", "delete_failed");
            request.setAttribute("error_message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("에러 리포트를 찾을 수 없습니다.")
                            .build());
        }
    }
}