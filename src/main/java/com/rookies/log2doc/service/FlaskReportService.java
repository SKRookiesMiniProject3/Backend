package com.rookies.log2doc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FlaskReportService {

    public void sendReportToFlask() {
        RestClient restClient = RestClient.create();

        String flaskUrl = "http://localhost:5000/receive-report";

        Map<String, Object> requestBody = Map.of(
            "message", "스프링에서 RestClient로 보낸 테스트 에러!",
            "resolved", false
        );

        String response = restClient.post()
            .uri(flaskUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(String.class);

        System.out.println("📌 Flask 응답: " + response);
    }
}
