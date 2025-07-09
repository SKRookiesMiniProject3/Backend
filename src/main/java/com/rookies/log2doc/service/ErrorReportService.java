package com.rookies.log2doc.service;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.dto.ErrorReportDTO;
import com.rookies.log2doc.dto.request.CreateErrorReportRequest;
import com.rookies.log2doc.entity.CategoryType;
import com.rookies.log2doc.entity.ErrorReport;
import com.rookies.log2doc.repository.CategoryTypeRepository;
import com.rookies.log2doc.repository.ErrorReportRepository;
import com.rookies.log2doc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorReportService {

    private final ErrorReportRepository errorReportRepository;
    private final UserRepository userRepository;
    private final CategoryTypeRepository categoryTypeRepository; // 추가
    private final FlaskReportService flaskReportService;

    // ✅ 일별 에러 카운트
    public List<ErrorCountPerDayDTO> getDailyCounts() {
        return errorReportRepository.findDailyErrorCounts();
    }

    // ✅ 최신순 리스트 (삭제되지 않은 것만) - 카테고리 정보 포함
    public List<ErrorReportDTO> getLatestReports() {
        return errorReportRepository.findLatestWithCategory()
                .stream()
                .limit(50) // 최대 50개만 반환
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 진행중인 리포트 조회
    public List<ErrorReportDTO> getInProgressReports() {
        return errorReportRepository.findInProgressReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 완료된 리포트 조회
    public List<ErrorReportDTO> getCompletedReports() {
        return errorReportRepository.findCompletedReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 시작되지 않은 리포트 조회
    public List<ErrorReportDTO> getNotStartedReports() {
        return errorReportRepository.findNotStartedReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 에러 리포트 생성
    @Transactional
    public ErrorReportDTO createErrorReport(CreateErrorReportRequest request) {
        log.info("새로운 에러 리포트 생성 요청");

        try {
            // 1️⃣ 리포트 상태 검증
            ErrorReport.ReportStatus status;
            try {
                status = ErrorReport.ReportStatus.valueOf(request.getReportStatus());
            } catch (IllegalArgumentException e) {
                status = ErrorReport.ReportStatus.NOT_STARTED;
                log.warn("잘못된 리포트 상태: {}, 기본값으로 설정", request.getReportStatus());
            }

            // 2️⃣ 에러 원인 사용자 검증 (선택사항)
            if (request.getErrorSourceMember() != null) {
                boolean userExists = userRepository.existsById(request.getErrorSourceMember());
                if (!userExists) {
                    log.warn("존재하지 않는 사용자 ID: {}", request.getErrorSourceMember());
                    // 존재하지 않는 사용자면 null로 설정
                    request.setErrorSourceMember(null);
                }
            }

            // 3️⃣ 카테고리 타입 검증 (선택사항)
            CategoryType categoryType = null;
            if (request.getCategoryTypeId() != null) {
                categoryType = categoryTypeRepository.findById(request.getCategoryTypeId())
                        .orElse(null);
                if (categoryType == null) {
                    log.warn("존재하지 않는 카테고리 타입 ID: {}", request.getCategoryTypeId());
                }
            }

            // 4️⃣ DB에 저장
            ErrorReport entity = ErrorReport.builder()
                    .reportFileId(request.getReportFileId())
                    .errorSourceMember(request.getErrorSourceMember())
                    .reportStatus(status)
                    .reportComment(request.getReportComment())
                    .categoryType(categoryType) // 카테고리 설정
                    .build();

            ErrorReport saved = errorReportRepository.save(entity);
            log.info("에러 리포트 DB 저장 완료 - ID: {}, 카테고리: {}",
                    saved.getId(),
                    categoryType != null ? categoryType.getName() : "없음");

            // 5️⃣ Flask로 비동기 전송
            sendToFlaskAsync(saved);

            return toDTO(saved);

        } catch (Exception e) {
            log.error("에러 리포트 생성 실패", e);
            throw new RuntimeException("에러 리포트 생성에 실패했습니다: " + e.getMessage());
        }
    }

    // ✅ 에러 리포트 상태 변경
    @Transactional
    public ErrorReportDTO updateReportStatus(Long id, String newStatus) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        try {
            ErrorReport.ReportStatus status = ErrorReport.ReportStatus.valueOf(newStatus);
            errorReport.setReportStatus(status);

            ErrorReport saved = errorReportRepository.save(errorReport);
            log.info("에러 리포트 상태 변경 완료 - ID: {}, 상태: {}", id, newStatus);

            return toDTO(saved);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("잘못된 리포트 상태입니다: " + newStatus);
        }
    }

    // ✅ 에러 리포트 완료 처리
    @Transactional
    public ErrorReportDTO completeReport(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.completeReport();
        ErrorReport saved = errorReportRepository.save(errorReport);
        log.info("에러 리포트 완료 처리 - ID: {}", id);

        return toDTO(saved);
    }

    // ✅ 에러 리포트 시작
    @Transactional
    public ErrorReportDTO startReport(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.startReport();
        ErrorReport saved = errorReportRepository.save(errorReport);
        log.info("에러 리포트 시작 - ID: {}", id);

        return toDTO(saved);
    }

    // ✅ 에러 리포트 코멘트 수정
    @Transactional
    public ErrorReportDTO updateComment(Long id, String comment) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.updateComment(comment);
        ErrorReport saved = errorReportRepository.save(errorReport);
        log.info("에러 리포트 코멘트 수정 완료 - ID: {}", id);

        return toDTO(saved);
    }

    // ✅ 에러 리포트 소프트 삭제
    @Transactional
    public void deleteReport(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        errorReport.softDelete();
        errorReportRepository.save(errorReport);
        log.info("에러 리포트 삭제 완료 - ID: {}", id);
    }

    // ✅ 에러 리포트 상세 조회 - 카테고리 정보 포함
    public ErrorReportDTO getReportById(Long id) {
        ErrorReport errorReport = errorReportRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        return toDTO(errorReport);
    }

    // ✅ 특정 사용자의 에러 리포트 조회
    public List<ErrorReportDTO> getReportsByMember(Long memberId) {
        return errorReportRepository.findByErrorSourceMember(memberId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========================================
    // 카테고리 관련 메서드들 추가
    // ========================================

    // ✅ 특정 카테고리의 에러 리포트 조회
    public List<ErrorReportDTO> getReportsByCategory(Long categoryId) {
        return errorReportRepository.findByCategoryTypeIdWithCategory(categoryId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 카테고리별 에러 리포트 개수 통계
    public Map<String, Long> getCategoryStatistics() {
        List<Object[]> results = errorReportRepository.countByCategory();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> (Long) result[1]
                ));
    }

    // ✅ 특정 카테고리의 특정 상태 리포트 조회
    public List<ErrorReportDTO> getReportsByCategoryAndStatus(Long categoryId, String statusName) {
        try {
            ErrorReport.ReportStatus status = ErrorReport.ReportStatus.valueOf(statusName);
            return errorReportRepository.findByCategoryAndStatus(categoryId, status)
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("잘못된 리포트 상태입니다: " + statusName);
        }
    }

    // ✅ 카테고리가 없는 에러 리포트 조회
    public List<ErrorReportDTO> getUncategorizedReports() {
        return errorReportRepository.findUncategorizedReports()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ 에러 리포트 카테고리 변경
    @Transactional
    public ErrorReportDTO updateReportCategory(Long id, Long categoryId) {
        ErrorReport errorReport = errorReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("에러 리포트를 찾을 수 없습니다."));

        CategoryType categoryType = null;
        if (categoryId != null) {
            categoryType = categoryTypeRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
        }

        errorReport.setCategoryType(categoryType);
        ErrorReport saved = errorReportRepository.save(errorReport);

        log.info("에러 리포트 카테고리 변경 완료 - ID: {}, 새 카테고리: {}",
                id, categoryType != null ? categoryType.getName() : "없음");

        return toDTO(saved);
    }

    // ✅ 리포트 상태별 통계
    public Map<String, Long> getReportStatistics() {
        List<Object[]> results = errorReportRepository.countByReportStatus();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((ErrorReport.ReportStatus) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    // ✅ Flask 비동기 전송
    @Async
    public void sendToFlaskAsync(ErrorReport errorReport) {
        try {
            Map<String, Object> flaskData = Map.of(
                    "id", errorReport.getId(),
                    "reportFileId", errorReport.getReportFileId() != null ? errorReport.getReportFileId() : "",
                    "errorSourceMember", errorReport.getErrorSourceMember() != null ? errorReport.getErrorSourceMember() : "",
                    "reportStatus", errorReport.getReportStatus().name(),
                    "reportComment", errorReport.getReportComment() != null ? errorReport.getReportComment() : "",
                    "createdDt", errorReport.getCreatedDt().toString(),
                    "isDeleted", errorReport.getIsDeleted()
            );

            flaskReportService.sendErrorReportToFlask(flaskData);
            log.info("Flask 전송 성공 - 에러 리포트 ID: {}", errorReport.getId());

        } catch (Exception e) {
            log.error("Flask 전송 실패 - 에러 리포트 ID: {}", errorReport.getId(), e);
            // Flask 전송 실패해도 애플리케이션은 계속 동작
        }
    }

    // ✅ DTO 변환 - 카테고리 정보 포함
    private ErrorReportDTO toDTO(ErrorReport entity) {
        String errorSourceMemberName = null;
        if (entity.getErrorSourceMember() != null) {
            // 사용자 이름 조회 (추후 구현)
            errorSourceMemberName = "사용자_" + entity.getErrorSourceMember();
        }

        // 카테고리 정보 추출
        Long categoryTypeId = null;
        String categoryName = null;
        String categoryDescription = null;

        if (entity.getCategoryType() != null) {
            categoryTypeId = entity.getCategoryType().getId();
            categoryName = entity.getCategoryType().getName();
            categoryDescription = entity.getCategoryType().getDescription();
        }

        return ErrorReportDTO.builder()
                .id(entity.getId())
                .reportFileId(entity.getReportFileId())
                .errorSourceMember(entity.getErrorSourceMember())
                .reportStatus(entity.getReportStatus().name())
                .reportStatusDescription(entity.getReportStatus().getDescription())
                .reportComment(entity.getReportComment())
                .isDeleted(entity.getIsDeleted())
                .createdDt(entity.getCreatedDt())
                .deletedDt(entity.getDeletedDt())
                .categoryTypeId(categoryTypeId)
                .categoryName(categoryName)
                .categoryDescription(categoryDescription)
                .errorSourceMemberName(errorSourceMemberName)
                .build();
    }
}