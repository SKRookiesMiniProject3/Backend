package com.rookies.log2doc.service;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.dto.ErrorReportDTO;
import com.rookies.log2doc.dto.request.CreateErrorReportRequest;
import com.rookies.log2doc.entity.ErrorReport;
import com.rookies.log2doc.repository.ErrorReportRepository;
import com.rookies.log2doc.service.FlaskReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 3. ErrorReportService.java 개선
@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorReportService {

    private final ErrorReportRepository errorReportRepository;
    private final FlaskReportService flaskReportService; // Flask 연동

    // ✅ 일별 에러 카운트
    public List<ErrorCountPerDayDTO> getDailyCounts() {
        return errorReportRepository.findDailyErrorCounts();
    }

    // ✅ 최신순 리스트 (페이징 추가)
    public List<ErrorReportDTO> getLatestErrors() {
        return errorReportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .limit(50) // 최대 50개만 반환
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 미해결 리스트
    public List<ErrorReportDTO> getUnresolvedErrors() {
        return errorReportRepository.findByResolvedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 에러 메시지 입력 (개선됨)
    @Transactional
    public ErrorReportDTO createError(CreateErrorReportRequest request) {
        log.info("새로운 에러 리포트 생성 요청: {}", request.getMessage());

        try {
            // 1️⃣ DB에 저장
            ErrorReport entity = ErrorReport.builder()
                    .message(request.getMessage())
                    .errorCode(request.getErrorCode())
                    .resolved(request.getResolved() != null ? request.getResolved() : false)
                    .description(request.getDescription())
                    .severity(request.getSeverity())
                    .location(request.getLocation())
                    .build();

            ErrorReport saved = errorReportRepository.save(entity);
            log.info("에러 리포트 DB 저장 완료 - ID: {}", saved.getId());

            // 2️⃣ Flask로 비동기 전송
            sendToFlaskAsync(saved);

            return toDTO(saved);

        } catch (Exception e) {
            log.error("에러 리포트 생성 실패", e);
            throw new RuntimeException("에러 리포트 생성에 실패했습니다: " + e.getMessage());
        }
    }

    // ✅ 에러 해결 처리
    @Transactional
    public ErrorReportDTO resolveError(Long id) {
        ErrorReport errorReport = errorReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.setResolved(true);
        errorReport.setResolvedAt(LocalDateTime.now());

        ErrorReport saved = errorReportRepository.save(errorReport);
        log.info("에러 리포트 해결 완료 - ID: {}", id);

        return toDTO(saved);
    }

    // ✅ 에러 상세 조회
    public ErrorReportDTO getErrorById(Long id) {
        ErrorReport errorReport = errorReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        return toDTO(errorReport);
    }

    // ✅ Flask 비동기 전송
    @Async
    public void sendToFlaskAsync(ErrorReport errorReport) {
        try {
            Map<String, Object> flaskData = Map.of(
                    "id", errorReport.getId(),
                    "message", errorReport.getMessage(),
                    "errorCode", errorReport.getErrorCode() != null ? errorReport.getErrorCode() : "",
                    "severity", errorReport.getSeverity() != null ? errorReport.getSeverity() : "MEDIUM",
                    "location", errorReport.getLocation() != null ? errorReport.getLocation() : "",
                    "createdAt", errorReport.getCreatedAt().toString(),
                    "resolved", errorReport.getResolved()
            );

            flaskReportService.sendErrorReportToFlask(flaskData);
            log.info("Flask 전송 성공 - 에러 ID: {}", errorReport.getId());

        } catch (Exception e) {
            log.error("Flask 전송 실패 - 에러 ID: {}", errorReport.getId(), e);
            // Flask 전송 실패해도 애플리케이션은 계속 동작
        }
    }

    // ✅ DTO 변환 (개선됨)
    private ErrorReportDTO toDTO(ErrorReport entity) {
        return ErrorReportDTO.builder()
                .id(entity.getId())
                .message(entity.getMessage())
                .errorCode(entity.getErrorCode())
                .resolved(entity.getResolved())
                .createdAt(entity.getCreatedAt())
                .description(entity.getDescription())
                .severity(entity.getSeverity())
                .location(entity.getLocation())
                .resolvedAt(entity.getResolvedAt())
                .build();
    }
}