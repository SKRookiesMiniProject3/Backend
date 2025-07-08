package com.rookies.log2doc.controller;

import com.rookies.log2doc.service.FlaskReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FlaskTestController {

    private final FlaskReportService flaskReportService;

    @GetMapping("/test-flask")
    public String testFlask() {
        flaskReportService.sendReportToFlask();
        return "Flask 연동 테스트 완료!";
    }
}
