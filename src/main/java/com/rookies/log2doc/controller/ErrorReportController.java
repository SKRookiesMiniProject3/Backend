package com.rookies.log2doc.controller;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.dto.ErrorReportDTO;
import com.rookies.log2doc.dto.request.CreateErrorReportRequest;
import com.rookies.log2doc.dto.response.ApiResponse;
import com.rookies.log2doc.service.ErrorReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/errors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "에러 리포트 관리", description = "에러 리포트 생성 및 조회 API")
public class ErrorReportController {

    private final ErrorReportService errorReportService;

    // ✅ 일별 에러 카운트
    @GetMapping("/daily-count")
    @Operation(summary = "일별 에러 카운트 조회", description = "날짜별 에러 발생 개수를 조회합니다.")
    public ResponseEntity<List<ErrorCountPerDayDTO>> getDailyCounts() {
        List<ErrorCountPerDayDTO> counts = errorReportService.getDailyCounts();
        return ResponseEntity.ok(counts);
    }

    // ✅ 최신순 리스트
    @GetMapping("/latest")
    @Operation(summary = "최신 에러 리스트 조회", description = "최신순으로 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getLatestErrors() {
        List<ErrorReportDTO> errors = errorReportService.getLatestErrors();
        return ResponseEntity.ok(errors);
    }

    // ✅ 미해결 리스트
    @GetMapping("/unresolved")
    @Operation(summary = "미해결 에러 리스트 조회", description = "해결되지 않은 에러 리포트를 조회합니다.")
    public ResponseEntity<List<ErrorReportDTO>> getUnresolvedErrors() {
        List<ErrorReportDTO> errors = errorReportService.getUnresolvedErrors();
        return ResponseEntity.ok(errors);
    }

    // ✅ 에러 메시지 입력 (중복 제거 및 개선)
    @PostMapping
    @Operation(summary = "에러 리포트 생성", description = "새로운 에러 리포트를 생성합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> createError(
            @Valid @RequestBody CreateErrorReportRequest request) {

        try {
            ErrorReportDTO created = errorReportService.createError(request);

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러 리포트가 성공적으로 생성되었습니다.")
                    .data(created)
                    .build());

        } catch (Exception e) {
            log.error("에러 리포트 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message("서버 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    // ✅ 에러 해결 처리
    @PatchMapping("/{id}/resolve")
    @Operation(summary = "에러 해결 처리", description = "특정 에러 리포트를 해결 완료로 표시합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> resolveError(@PathVariable Long id) {
        try {
            ErrorReportDTO resolved = errorReportService.resolveError(id);

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러가 해결 완료로 처리되었습니다.")
                    .data(resolved)
                    .build());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message("에러 리포트를 찾을 수 없습니다.")
                            .build());
        }
    }

    // ✅ 에러 상세 조회
    @GetMapping("/{id}")
    @Operation(summary = "에러 상세 조회", description = "특정 에러 리포트의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ErrorReportDTO>> getErrorById(@PathVariable Long id) {
        try {
            ErrorReportDTO error = errorReportService.getErrorById(id);

            return ResponseEntity.ok(ApiResponse.<ErrorReportDTO>builder()
                    .success(true)
                    .message("에러 상세 정보 조회 성공")
                    .data(error)
                    .build());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ErrorReportDTO>builder()
                            .success(false)
                            .message("에러 리포트를 찾을 수 없습니다.")
                            .build());
        }
    }
}