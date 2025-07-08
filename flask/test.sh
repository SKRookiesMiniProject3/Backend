#!/bin/bash

# Enhanced Flask Security Analysis API - Test Script
# 서버가 http://localhost:5001에서 실행중이라고 가정

echo "========================================"
echo "Flask Security Analysis API 테스트 시작"
echo "========================================"

# 1. 기본 분석 - 정상 요청
echo "1. 정상 요청 테스트..."
curl -s -X POST http://localhost:5001/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-01-15T10:30:00Z",
    "user_id": "user123",
    "request_method": "GET",
    "request_url": "/documents/report.pdf",
    "user_role": "EMPLOYEE",
    "action_type": "READ",
    "document_classification": "INTERNAL",
    "access_result": "SUCCESS",
    "request_headers": {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
  }' | jq '.'

sleep 1

# 2. 고급 분석 - SQL 인젝션 공격
echo "2. SQL 인젝션 공격 테스트..."
curl -s -X POST http://localhost:5001/analyze-advanced \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-01-15T10:35:00Z",
    "user_id": "attacker456",
    "request_method": "POST",
    "request_url": "/login?username=admin'\''union select * from users--",
    "user_role": "GUEST",
    "action_type": "READ",
    "document_classification": "PUBLIC",
    "access_result": "BLOCKED",
    "request_headers": {
      "User-Agent": "sqlmap/1.6.12"
    },
    "request_count_per_minute": 50
  }' | jq '.'

sleep 1

# 3. 고급 분석 - 권한 상승 시도
echo "3. 권한 상승 시도 테스트..."
curl -s -X POST http://localhost:5001/analyze-advanced \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-01-15T10:40:00Z",
    "user_id": "intern789",
    "request_method": "PUT",
    "request_url": "/documents/confidential-data.txt",
    "user_role": "INTERN",
    "action_type": "UPDATE",
    "document_classification": "CONFIDENTIAL",rh
    "document_id": "DOC_001",
    "access_result": "PERMISSION_DENIED",
    "request_headers": {
      "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"
    }
  }' | jq '.'

sleep 1

# 4. 고급 분석 - XSS 공격
echo "4. XSS 공격 테스트..."
curl -s -X POST http://localhost:5001/analyze-advanced \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-01-15T10:45:00Z",
    "user_id": "hacker999",
    "request_method": "GET",
    "request_url": "/search?q=<script>alert(\"XSS\")</script>",
    "user_role": "GUEST",
    "action_type": "READ",
    "document_classification": "PUBLIC",
    "access_result": "BLOCKED",
    "request_headers": {
      "User-Agent": "Mozilla/5.0 (compatible; Burp Suite)"
    },
    "security_events": ["XSS_DETECTED"]
  }' | jq '.'

sleep 1

echo "========================================"
echo "추가 INVALID 케이스 테스트 시작"
echo "========================================"

# 5. 일반 사용자가 기밀 문서 접근 시도 (권한 부족)
echo "5. 일반 사용자 기밀 문서 접근 거부 테스트..."
curl -s -X POST http://localhost:5001/analyze-advanced \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-01-15T11:00:00Z",
    "user_id": "employee001",
    "request_method": "GET",
    "request_url": "/documents/top-secret/financial-report-2024.pdf",
    "user_role": "EMPLOYEE",
    "action_type": "READ",
    "document_classification": "TOP_SECRET",
    "document_id": "TS_001",
    "access_result": "PERMISSION_DENIED",
    "request_headers": {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
  }' | jq '.'

sleep 1

# 6. 인턴이 삭제 권한 없는 문서 삭제 시도
echo "6. 인턴 문서 삭제 권한 거부 테스트..."
curl -s -X POST http://localhost:5001/analyze-advanced \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-01-15T11:05:00Z",
    "user_id": "intern_kim",
    "request_method": "DELETE",
    "request_url": "/documents/internal/project-proposal.docx",
    "user_role": "INTERN",
    "action_type": "DELETE",
    "document_classification": "INTERNAL",
    "document_id": "INT_005",
    "access_result": "PERMISSION_DENIED",
    "request_headers": {
      "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
    }
  }' | jq '.'

sleep 1

# 7. 게스트 사용자가 내부 문서 수정 시도
echo "7. 게스트 사용자 내부 문서 수정 거부 테스트..."
curl -s -X POST http://localhost:5001/analyze-advanced \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-01-15T11:10:00Z",
    "user_id": "guest_visitor",
    "request_method": "PUT",
    "request_url": "/documents/internal/company-policy.pdf",
    "user_role": "GUEST",
    "action_type": "UPDATE",
    "document_classification": "INTERNAL",
    "document_id": "POL_001",
    "access_result": "PERMISSION_DENIED",
    "request_headers": {
      "User-Agent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0"
    }
  }' | jq '.'

sleep 1

# 8. 외부 사용자가 기밀 문서 다운로드 시도
echo "8. 외부 사용자 기밀 문서 다운로드 거부 테스트..."
curl -s -X POST http://localhost:5001/analyze-advanced \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-01-15T11:15:00Z",
    "user_id": "external_contractor",
    "request_method": "GET",
    "request_url": "/documents/confidential/client-database.xlsx",
    "user_role": "EXTERNAL",
    "action_type": "DOWNLOAD",
    "document_classification": "CONFIDENTIAL",
    "document_id": "CONF_010",
    "access_result": "PERMISSION_DENIED",
    "request_headers": {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
  }' | jq '.'

sleep 1

echo "========================================"
echo "모든 테스트 완료!"
echo "========================================"
echo "보고서 생성 확인:"
echo "- report/valid/ (정상 요청)"
echo "- report/invalid/ (권한 거부)"
echo "- report/attack/ (공격 탐지)"
echo ""
echo "생성된 보고서 확인:"
find report/ -name "*.md" -type f -exec echo "📄 {}" \; 2>/dev/null || echo "report 디렉토리를 확인해주세요."