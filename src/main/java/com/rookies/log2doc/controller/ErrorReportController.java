package com.rookies.log2doc.controller;

import com.rookies.log2doc.dto.ErrorCountPerDayDTO;
import com.rookies.log2doc.dto.ErrorReportDTO;
import com.rookies.log2doc.service.ErrorReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/errors")
@RequiredArgsConstructor
public class ErrorReportController {

    private final ErrorReportService errorReportService;

    // ✅ 일별 에러 카운트
    @GetMapping("/daily-count")
    public List<ErrorCountPerDayDTO> getDailyCounts() {
        return errorReportService.getDailyCounts();
    }

    // ✅ 최신순 리스트
    @GetMapping("/latest")
    public List<ErrorReportDTO> getLatestErrors() {
        return errorReportService.getLatestErrors();
    }

    // ✅ 미해결 리스트
    @GetMapping("/unresolved")
    public List<ErrorReportDTO> getUnresolvedErrors() {
        return errorReportService.getUnresolvedErrors();
    }

    @PostMapping
    public ErrorReportDTO createError(@RequestBody ErrorReportDTO dto) {
        return errorReportService.createError(dto);
    }
}
