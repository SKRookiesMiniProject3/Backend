package com.rookies.log2doc.service;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.dto.ErrorReportDTO;
import com.rookies.log2doc.entity.ErrorReport;
import com.rookies.log2doc.repository.ErrorReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ErrorReportService {

    private final ErrorReportRepository errorReportRepository;

    // ✅ 일별 에러 카운트
    public List<ErrorCountPerDayDTO> getDailyCounts() {
        return errorReportRepository.findDailyErrorCounts();
    }

    // ✅ 최신순 리스트
    public List<ErrorReportDTO> getLatestErrors() {
        return errorReportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
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

    // 에러 메세지 입력
    public ErrorReportDTO createError(ErrorReportDTO dto) {
        // 1️⃣ DB에 저장
        ErrorReport entity = ErrorReport.builder()
                .message(dto.getMessage())
                .errorCode(dto.getErrorCode())
                .resolved(dto.getResolved() != null ? dto.getResolved() : false)
                .build();

        ErrorReport saved = errorReportRepository.save(entity);

        // 2️⃣ Flask로 전송 (필요하다면!)
        sendToFlask(saved);

        return toDTO(saved);
    }

    private void sendToFlask(ErrorReport errorReport) {
        // TODO: 여기에 Flask 연동 로직 작성
        System.out.println("Flask로 보낼 데이터: " + errorReport.getMessage());
        // RestTemplate 등으로 Flask API 호출
    }

    private ErrorReportDTO toDTO(ErrorReport entity) {
        return ErrorReportDTO.builder()
                .id(entity.getId())
                .message(entity.getMessage())
                .resolved(entity.getResolved())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
