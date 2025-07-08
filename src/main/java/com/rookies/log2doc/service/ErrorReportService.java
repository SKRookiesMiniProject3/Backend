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

    private ErrorReportDTO toDTO(ErrorReport entity) {
        return ErrorReportDTO.builder()
                .id(entity.getId())
                .message(entity.getMessage())
                .resolved(entity.getResolved())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
