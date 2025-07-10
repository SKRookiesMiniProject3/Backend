from flask import Flask, request, jsonify
from flask_cors import CORS
import logging
from datetime import datetime
from typing import Dict, List, Any
import os
import json
from dotenv import load_dotenv
import openai
from pathlib import Path
from db_manager import db_manager, init_database

load_dotenv()

app = Flask(__name__)
CORS(app)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)
@app.before_request
def log_request_info():
    logger.info(f"요청 수신: {request.method} {request.url}")

    # JSON 데이터가 있는 경우에만 로깅
    if request.is_json and request.get_json():
        data = request.get_json()
        request_url = data.get('request_url', request.url)
        logger.info(f"요청 데이터 URL: {request_url}")
    else:
        logger.info(f"비JSON 요청 또는 빈 요청 본문")


REPORT_DIRS = {
    'NORMAL': 'report/valid',
    'EXCEPTION': 'report/invalid', 
    'ATTACK': 'report/attack'
}

for dir_path in REPORT_DIRS.values():
    Path(dir_path).mkdir(parents=True, exist_ok=True)

class SecurityAnalysisSystem:
    def __init__(self):
        self.openai_api_key = os.getenv('OPENAI_API_KEY')
        self.use_mock = not bool(self.openai_api_key)
        
        if self.openai_api_key:
            openai.api_key = self.openai_api_key
            logger.info("OpenAI API initialized")
        else:
            logger.info("Using mock analysis mode")
            
    def generate_simple_report(self, analysis: Dict[str, Any], log_data: Dict[str, Any]) -> str:
        classification = analysis.get("classification", "UNKNOWN")
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"simple_report_{timestamp}.md"
        
        if classification == "EXCEPTION":
            report_dir = REPORT_DIRS["EXCEPTION"]
        else:
            report_dir = REPORT_DIRS["NORMAL"]
        
        report_content = f"""
    # 간단 보고서

    **생성일시:** {datetime.now().strftime("%Y년 %m월 %d일 %H:%M:%S")}
    **분류:** {classification}
    **위험도:** {analysis.get('threat_level', 'UNKNOWN')}
    **사용자:** {log_data.get('user_id', 'N/A')}
    **URL:** {log_data.get('request_url', 'N/A')}
    **결과:** {log_data.get('access_result', 'N/A')}

    ## 권장사항
    {chr(10).join(f"- {rec}" for rec in analysis.get('recommendations', ['없음']))}
    """
        
        report_path = os.path.join(report_dir, filename)
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        logger.info(f"간단 보고서 저장 완료: {report_path}")
    
        try:
            report_id = db_manager.save_error_report(analysis, log_data, report_path)
            if report_id:
                logger.info(f"간단 보고서 DB 저장 완료: ID={report_id}")
            else:
                logger.warning("간단 보고서 DB 저장 실패")
        except Exception as e:
            logger.error(f"간단 보고서 DB 저장 중 오류: {e}")
        return report_path
    
    def analyze_log_with_llm(self, log_data: Dict[str, Any]) -> Dict[str, Any]:
        try:
            base_analysis = self.analyze_log(log_data)
            llm_analysis = self._get_llm_analysis(log_data, base_analysis)
            
            final_analysis = {
                **base_analysis,
                "llm_analysis": llm_analysis,
                "enhanced_classification": llm_analysis.get("classification", base_analysis["classification"]),
                "enhanced_threat_level": llm_analysis.get("threat_level", base_analysis["threat_level"])
            }
            
            report_path = self._generate_and_save_report(final_analysis, log_data)
            final_analysis["report_path"] = report_path
            
            return final_analysis
            
        except Exception as e:
            logger.error(f"LLM 분석 중 오류 발생: {str(e)}")
            return self.analyze_log(log_data)

    def _analyze_url_elements(self, url: str) -> List[str]:
        suspicious_elements = []

        # URL 길이 확인
        if len(url) > 500:
            suspicious_elements.append("비정상적으로 긴 URL")

        # 특수 문자 밀도 확인
        special_chars = sum(1 for c in url if c in "';\"<>&|%")
        if special_chars > 10:
            suspicious_elements.append("과도한 특수 문자 사용")

        # 인코딩 확인
        if url.count("%") > 5:
            suspicious_elements.append("과도한 URL 인코딩")

        # 스크립트 태그 확인
        if "script" in url.lower():
            suspicious_elements.append("스크립트 태그 포함")

        return suspicious_elements
    def _get_llm_analysis(self, log_data: Dict[str, Any], base_analysis: Dict[str, Any]) -> Dict[str, Any]:
        if self.use_mock:
            return self._mock_llm_analysis(log_data, base_analysis)
        
        try:
            prompt = self._create_analysis_prompt(log_data, base_analysis)
            
            response = openai.ChatCompletion.create(
                model="gpt-3.5-turbo",
                messages=[
                    {"role": "system", "content": "You are a cybersecurity expert analyzing security logs."},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=1500,
                temperature=0.3
            )
            
            analysis_text = response.choices[0].message.content
            return self._parse_llm_response(analysis_text)
            
        except Exception as e:
            logger.error(f"OpenAI API 호출 오류: {str(e)}")
            return self._mock_llm_analysis(log_data, base_analysis)
    
    def _mock_llm_analysis(self, log_data: Dict[str, Any], base_analysis: Dict[str, Any]) -> Dict[str, Any]:
        classification = base_analysis.get("classification", "UNKNOWN")

        url = log_data.get("request_url", "").lower()

        # URL 기반 위험도 재평가
        high_risk_patterns = ["union select", "drop table", "<script>", "alert(", "../etc/passwd", "cmd.exe", "or 1=1"]
        medium_risk_patterns = ["'", "javascript:", "..", "select", "insert", "update"]

        url_risk = "LOW"
        detected_patterns = []

        for pattern in high_risk_patterns:
            if pattern in url:
                url_risk = "HIGH"
                detected_patterns.append(pattern)

        if url_risk == "LOW":
            for pattern in medium_risk_patterns:
                if pattern in url:
                    url_risk = "MEDIUM"
                    detected_patterns.append(pattern)
        if classification == "ATTACK" or (url_risk in ["HIGH", "MEDIUM"] and detected_patterns):
            return {
                "classification": "ATTACK",
                "threat_level": "HIGH" if url_risk == "HIGH" else "MEDIUM",
                "url_analysis": {
                    "risk_level": url_risk,
                    "detected_patterns": detected_patterns,
                    "suspicious_elements": self._analyze_url_elements(url)
                },
                "when": log_data.get("timestamp", "Unknown"),
                "what": f"URL 기반 공격 탐지: {', '.join(detected_patterns) if detected_patterns else '의심스러운 요청 패턴'}",
                "who": f"사용자 ID: {log_data.get('user_id', 'Unknown')}",
                "how": f"악성 URL 요청: {url[:100]}{'...' if len(url) > 100 else ''}",
                "countermeasures": [
                    "즉시 해당 IP 주소 차단",
                    "웹 애플리케이션 방화벽(WAF) 규칙 강화",
                    "URL 패턴 기반 탐지 규칙 추가",
                    "입력 검증 및 필터링 강화"
                ]
            }
        elif classification == "EXCEPTION":
            return {
                "classification": "EXCEPTION",
                "threat_level": "LOW",
                "reason": "권한 부족으로 인한 접근 거부",
                "user_info": f"사용자: {log_data.get('user_id', 'Unknown')} (역할: {log_data.get('user_role', 'Unknown')})",
                "attempted_action": log_data.get('action_type', 'Unknown'),
                "recommendations": [
                    "사용자 권한 재검토",
                    "접근 제어 정책 확인",
                    "사용자 교육 필요성 검토"
                ]
            }
        else:
            return {
                "classification": "NORMAL",
                "threat_level": "LOW",
                "status": "정상적인 접근 요청",
                "user_activity": f"{log_data.get('user_id', 'Unknown')}의 {log_data.get('action_type', 'Unknown')} 요청",
                "recommendations": [
                    "지속적인 모니터링 유지",
                    "정상 활동 패턴 학습"
                ]
            }
    
    def _create_analysis_prompt(self, log_data: Dict[str, Any], base_analysis: Dict[str, Any]) -> str:
        return f"""
다음 보안 로그를 분석하고 한국어로 결과를 제공해주세요.

**특별 지시사항**: 
- 요청 URL을 매우 주의깊게 분석하세요
- SQL 인젝션, XSS, 경로 탐색, 명령어 인젝션 등의 공격 패턴을 찾아보세요
- URL 인코딩된 악성 페이로드도 확인하세요
- 비정상적인 매개변수나 특수문자 사용을 확인하세요

로그 데이터:
{json.dumps(log_data, indent=2, ensure_ascii=False)}

기본 분석 결과:
{json.dumps(base_analysis, indent=2, ensure_ascii=False)}

**URL 분석 포인트**:
1. 요청 URL: {log_data.get('request_url', 'N/A')}
2. 이 URL에 다음과 같은 공격 징후가 있는지 확인:
   - SQL 인젝션: ', union, select, drop, or 1=1 등
   - XSS: <script>, javascript:, alert() 등
   - 경로 탐색: ../, etc/passwd, windows/system32 등
   - 명령어 인젝션: ;, |, &, cat, ls 등
   - 기타 악성 패턴: %00, null byte, SSRF 등

다음 형식으로 분석 결과를 제공해주세요:
1. 분류: NORMAL/EXCEPTION/ATTACK 중 하나
2. 위협 수준: LOW/MEDIUM/HIGH 중 하나
3. URL 위험도 평가: URL에서 발견된 구체적인 공격 패턴
4. 공격인 경우: When(언제), What(무엇을), Who(누가), How(어떻게)
5. 대응 방안 또는 권장사항

JSON 형식으로 응답해주세요.
"""
    
    def _parse_llm_response(self, response_text: str) -> Dict[str, Any]:
        try:
            if "{" in response_text:
                json_start = response_text.find("{")
                json_end = response_text.rfind("}") + 1
                json_text = response_text[json_start:json_end]
                return json.loads(json_text)
            else:
                return {"analysis": response_text}
        except:
            return {"analysis": response_text}
    
    def _generate_and_save_report(self, analysis: Dict[str, Any], log_data: Dict[str, Any]) -> str:
        classification = analysis.get("enhanced_classification", analysis.get("classification", "UNKNOWN"))
        
        report_content = self._create_report_content(analysis, log_data)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"security_report_{timestamp}.md"
        
        if classification == "ATTACK":
            report_dir = REPORT_DIRS["ATTACK"]
        elif classification == "EXCEPTION":
            report_dir = REPORT_DIRS["EXCEPTION"]
        else:
            report_dir = REPORT_DIRS["NORMAL"]
        
        report_path = os.path.join(report_dir, filename)
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        logger.info(f"보고서 저장 완료: {report_path}")
        
        try:
            report_id = db_manager.save_error_report(analysis, log_data, report_path)
            if report_id:
                logger.info(f"보고서 DB 저장 완료: ID={report_id}")
            else:
                logger.warning("보고서 DB 저장 실패")
        except Exception as e:
            logger.error(f"보고서 DB 저장 중 오류: {e}")
        return report_path
    
    def _create_report_content(self, analysis: Dict[str, Any], log_data: Dict[str, Any]) -> str:
        classification = analysis.get("enhanced_classification", analysis.get("classification", "UNKNOWN"))
        timestamp = datetime.now().strftime("%Y년 %m월 %d일 %H:%M:%S")
        
        if classification == "ATTACK":
            return self._create_attack_report(analysis, log_data, timestamp)
        elif classification == "EXCEPTION":
            return self._create_exception_report(analysis, log_data, timestamp)
        else:
            return self._create_normal_report(analysis, log_data, timestamp)
    
    def _create_attack_report(self, analysis: Dict[str, Any], log_data: Dict[str, Any], timestamp: str) -> str:
        llm_analysis = analysis.get("llm_analysis", {})
        
        return f"""
# 보안 사고 보고서

**보고서 생성일시:** {timestamp}  
**분류:** 🚨 보안 위협 (공격)  
**위험도:** {analysis.get('enhanced_threat_level', 'HIGH')}  
**신뢰도:** {analysis.get('confidence', 0) * 100:.1f}%

---

## 1. 사고 개요

### 📋 기본 정보
- **로그 ID:** {log_data.get('timestamp', 'N/A')}
- **탐지 시각:** {log_data.get('timestamp', 'N/A')}
- **분석 완료 시각:** {analysis.get('timestamp', 'N/A')}

### 🎯 공격 정보 (5W1H)
- **When (언제):** {llm_analysis.get('when', log_data.get('timestamp', 'N/A'))}
- **Who (누가):** {llm_analysis.get('who', f"사용자 ID: {log_data.get('user_id', 'Unknown')}")}
- **What (무엇을):** {llm_analysis.get('what', '악의적인 요청 탐지')}
- **Where (어디서):** {log_data.get('request_url', 'N/A')}
- **Why (왜):** 시스템 침해 또는 정보 탈취 목적으로 추정
- **How (어떻게):** {llm_analysis.get('how', '공격 기법 분석 중')}

---

## 2. 상세 분석

### 🔍 탐지된 공격 유형
{chr(10).join(f"- {indicator}" for indicator in analysis.get('attack_indicators', ['정보 없음']))}

### 📊 요청 상세 정보
- **HTTP 메서드:** {log_data.get('request_method', 'N/A')}
- **요청 URL:** {log_data.get('request_url', 'N/A')}
- **사용자 역할:** {log_data.get('user_role', 'N/A')}
- **요청 결과:** {log_data.get('access_result', 'N/A')}
- **문서 분류:** {log_data.get('document_classification', 'N/A')}

### 🔒 보안 이벤트
{chr(10).join(f"- {event}" for event in log_data.get('security_events', ['없음']))}

---

## 3. 영향 평가

### 💥 잠재적 피해
- **영향 수준:** {analysis.get('detailed_analysis', {}).get('potential_impact', 'MEDIUM')}
- **영향 받은 자원:**
{chr(10).join(f"  - {resource}" for resource in analysis.get('detailed_analysis', {}).get('affected_resources', ['정보 없음']))}

---

## 4. 대응 방안

### 🚨 즉시 대응 조치
{chr(10).join(f"- {rec}" for rec in llm_analysis.get('countermeasures', analysis.get('recommendations', ['대응 방안 검토 중'])))}

### 📋 후속 조치
- [ ] 사고 대응 팀 소집
- [ ] 포렌식 분석 수행
- [ ] 관련 시스템 보안 점검
- [ ] 사용자 교육 및 정책 업데이트

---

## 5. 결론 및 권고사항

이 보안 사고는 **{analysis.get('enhanced_threat_level', 'HIGH')}** 위험도로 분류되며, 즉시 대응이 필요합니다.

**주요 권고사항:**
1. 즉시 해당 IP 주소 차단
2. 보안 팀 긴급 소집
3. 시스템 취약점 점검 및 패치
4. 모니터링 강화

---

**보고서 작성:** 보안 분석 시스템  
**검토 필요:** 보안 담당자  
**배포:** 경영진, IT 보안팀
"""

    def _create_exception_report(self, analysis: Dict[str, Any], log_data: Dict[str, Any], timestamp: str) -> str:
        llm_analysis = analysis.get("llm_analysis", {})
        
        return f"""
# 접근 제어 보고서

**보고서 생성일시:** {timestamp}  
**분류:** ⚠️ 접근 거부  
**위험도:** {analysis.get('enhanced_threat_level', 'LOW')}  
**신뢰도:** {analysis.get('confidence', 0) * 100:.1f}%

---

## 1. 사건 개요

### 📋 기본 정보
- **로그 ID:** {log_data.get('timestamp', 'N/A')}
- **발생 시각:** {log_data.get('timestamp', 'N/A')}
- **분석 완료 시각:** {analysis.get('timestamp', 'N/A')}

### 👤 사용자 정보
- **사용자 ID:** {log_data.get('user_id', 'N/A')}
- **사용자 역할:** {log_data.get('user_role', 'N/A')}
- **시도한 작업:** {log_data.get('action_type', 'N/A')}

---

## 2. 접근 거부 상세

### 🚫 거부 사유
{llm_analysis.get('reason', '권한 부족으로 인한 접근 거부')}

### 📊 요청 정보
- **HTTP 메서드:** {log_data.get('request_method', 'N/A')}
- **요청 URL:** {log_data.get('request_url', 'N/A')}
- **문서 분류:** {log_data.get('document_classification', 'N/A')}
- **접근 결과:** {log_data.get('access_result', 'N/A')}

---

## 3. 분석 결과

### 📈 위험 평가
- **보안 위험도:** 낮음
- **정책 위반 여부:** 없음
- **추가 조사 필요성:** 낮음

### 🔍 패턴 분석
정상적인 권한 제어 시스템 작동으로 판단됩니다.

---

## 4. 권장 조치

### 📋 즉시 조치
{chr(10).join(f"- {rec}" for rec in llm_analysis.get('recommendations', analysis.get('recommendations', ['권한 설정 확인'])))}

### 🔄 후속 조치
- [ ] 사용자 권한 재검토
- [ ] 필요시 권한 조정
- [ ] 사용자 교육 실시

---

## 5. 결론

이 사건은 정상적인 접근 제어 시스템의 작동으로, 추가적인 보안 위협은 없는 것으로 판단됩니다.

**권고사항:**
1. 현재 권한 정책 유지
2. 정기적인 권한 검토
3. 사용자 교육 지속

---

**보고서 작성:** 보안 분석 시스템  
**검토 필요:** 시스템 관리자  
**배포:** IT 관리팀
"""

    def _create_normal_report(self, analysis: Dict[str, Any], log_data: Dict[str, Any], timestamp: str) -> str:
        llm_analysis = analysis.get("llm_analysis", {})
        
        return f"""
# 정상 접근 보고서

**보고서 생성일시:** {timestamp}  
**분류:** ✅ 정상 활동  
**위험도:** {analysis.get('enhanced_threat_level', 'LOW')}  
**신뢰도:** {analysis.get('confidence', 0) * 100:.1f}%

---

## 1. 활동 개요

### 📋 기본 정보
- **로그 ID:** {log_data.get('timestamp', 'N/A')}
- **접근 시각:** {log_data.get('timestamp', 'N/A')}
- **분석 완료 시각:** {analysis.get('timestamp', 'N/A')}

### 👤 사용자 정보
- **사용자 ID:** {log_data.get('user_id', 'N/A')}
- **사용자 역할:** {log_data.get('user_role', 'N/A')}
- **수행 작업:** {log_data.get('action_type', 'N/A')}

---

## 2. 활동 상세

### 📊 요청 정보
- **HTTP 메서드:** {log_data.get('request_method', 'N/A')}
- **요청 URL:** {log_data.get('request_url', 'N/A')}
- **문서 분류:** {log_data.get('document_classification', 'N/A')}
- **접근 결과:** {log_data.get('access_result', 'N/A')}

### ✅ 정상 활동 확인
{llm_analysis.get('status', '정상적인 접근 요청으로 확인됨')}

---

## 3. 분석 결과

### 📈 보안 평가
- **위험 지표:** 없음
- **이상 행동:** 탐지되지 않음
- **정책 준수:** 적절

### 🔍 활동 패턴
사용자의 정상적인 업무 활동 패턴에 부합합니다.

---

## 4. 권장 사항

### 📋 모니터링 지속
{chr(10).join(f"- {rec}" for rec in llm_analysis.get('recommendations', analysis.get('recommendations', ['지속적인 모니터링 유지'])))}

### 🔄 시스템 운영
- [ ] 정상 활동 패턴 학습
- [ ] 베이스라인 업데이트
- [ ] 성능 모니터링

---

## 5. 결론

이 활동은 정상적인 사용자 접근으로 분류되며, 추가적인 조치가 필요하지 않습니다.

**현재 상태:** 양호  
**권고사항:** 현재 보안 정책 유지

---

**보고서 작성:** 보안 분석 시스템  
**검토 필요:** 해당 없음  
**배포:** 로그 보관소
"""

    def analyze_log(self, log_data: Dict[str, Any]) -> Dict[str, Any]:
        try:
            attack_indicators = self._detect_attack_patterns(log_data)
            classification, threat_level = self._classify_request(log_data, attack_indicators)
            
            confidence = self._calculate_confidence(log_data, attack_indicators)
            
            recommendations = self._generate_recommendations(classification, attack_indicators, threat_level)
            
            detailed_analysis = {}
            if classification == "ATTACK":
                detailed_analysis = self._detailed_threat_analysis(log_data)
            
            return {
                "timestamp": datetime.now().isoformat(),
                "log_id": log_data.get("timestamp", ""),
                "classification": classification,
                "threat_level": threat_level,
                "confidence": confidence,
                "attack_indicators": attack_indicators,
                "recommendations": recommendations,
                "detailed_analysis": detailed_analysis
            }
            
        except Exception as e:
            logger.error(f"분석 중 오류 발생: {str(e)}")
            return {
                "timestamp": datetime.now().isoformat(),
                "log_id": log_data.get("timestamp", ""),
                "error": str(e),
                "classification": "ERROR",
                "threat_level": "UNKNOWN"
            }

    def _detect_attack_patterns(self, log_data: Dict[str, Any]) -> List[str]:
        attack_indicators = []
        url = log_data.get("request_url", "").lower()
        # 요청 본문도 분석 대상에 포함
        request_body = log_data.get("request_body", "").lower()

        # JSON 본문인 경우 파싱해서 값들 추출
        json_values = ""
        if request_body:
            try:
                import json
                parsed_body = json.loads(request_body)
                if isinstance(parsed_body, dict):
                    json_values = " ".join(str(v) for v in parsed_body.values() if v)
            except:
                pass  # JSON 파싱 실패시 무시

        # URL과 본문을 모두 검사할 수 있도록 결합
        combined_content = f"{url} {request_body.lower()} {json_values.lower()}"

        # 기존 SQL Injection 패턴 확장
        sql_patterns = [
            "'", "\"", "union", "select", "drop", "delete", "insert", "update",
            "exec", "sp_", "xp_", "cmd", "master", "sysobjects", "syscolumns",
            "or 1=1", "and 1=1", "having 1=1", "waitfor delay", "benchmark(", "sleep(",
            "' or ", "\" or ", "' and ", "\" and ", "' union ", "\" union ",
            "or '1'='1", "or \"1\"=\"1", "or 1=1--", "or 1=1#", "or 1=1/*",
            "--", "#", "/*", "*/", "0x", "char(", "ascii(", "substring("
        ]
        if any(pattern in combined_content for pattern in sql_patterns):
            attack_indicators.append("SQL_INJECTION")

        # XSS 패턴 확장 및 강화
        xss_patterns = [
            "<script", "javascript:", "alert(", "eval(", "onload=", "onerror=",
            "onclick=", "onmouseover=", "prompt(", "confirm(", "document.cookie",
            "fromcharcode", "innerhtml", "outerhtml", "</script>", "src=",
            "href=javascript", "expression(", "vbscript:", "data:text/html",
            "onsubmit=", "onfocus=", "onblur=", "onchange=", "onkeyup=", "onkeydown="
        ]
        if any(pattern in combined_content for pattern in xss_patterns):
            attack_indicators.append("XSS")

        # Path Traversal 패턴 확장
        path_patterns = ["../", "..", "etc/passwd", "windows/system32", "boot.ini", "web.config", "wp-config", ".htaccess", "passwd", "shadow", "hosts"]
        if any(pattern in combined_content for pattern in path_patterns):
            attack_indicators.append("PATH_TRAVERSAL")

        # Command Injection 패턴 추가
        cmd_patterns = [";", "|", "&", "cat ", "ls ", "pwd", "whoami", "id", "uname", "netstat", "ps aux", "wget", "curl", "nc ", "telnet", "ssh", "rm -rf"]
        if any(pattern in combined_content for pattern in cmd_patterns):
            attack_indicators.append("COMMAND_INJECTION")

        # LDAP Injection 패턴 추가
        ldap_patterns = ["(|", "(&", "(cn=", "(userPassword=", ")(", "*(", "*)", "|(cn=*)", "(&(", ")(uid="]
        if any(pattern in combined_content for pattern in ldap_patterns):
            attack_indicators.append("LDAP_INJECTION")

        # 새로운 공격 패턴들 추가
        if any(pattern in combined_content for pattern in ["%00", "%0a", "%0d", "null", "chr(", "char("]):
            attack_indicators.append("NULL_BYTE_INJECTION")

        if any(pattern in combined_content for pattern in ["ldap://", "file://", "ftp://", "gopher://", "dict://", "sftp://", "tftp://"]):
            attack_indicators.append("SSRF")

        if combined_content.count("%") > 10 or combined_content.count("&") > 20:
            attack_indicators.append("PARAMETER_POLLUTION")

        # Header Injection 패턴 추가
        headers_str = ""
        if isinstance(log_data.get("request_headers"), dict):
            headers_str = " ".join(str(v) for v in log_data.get("request_headers", {}).values()).lower()
        elif "headers" in log_data and isinstance(log_data["headers"], dict):
            headers_str = " ".join(str(v) for v in log_data["headers"].values()).lower()

        if any(pattern in headers_str for pattern in ["\r\n", "\n", "\r", "evil.com", "injected"]):
            attack_indicators.append("HEADER_INJECTION")

        # User-Agent 체크
        user_agent = ""
        if isinstance(log_data.get("request_headers"), dict):
            user_agent = log_data.get("request_headers", {}).get("User-Agent", "").lower()
        elif "headers" in log_data and isinstance(log_data["headers"], dict):
            user_agent = log_data["headers"].get("User-Agent", "").lower()

        if any(tool in user_agent for tool in ["sqlmap", "nikto", "nmap", "burp", "attackbot"]):
            attack_indicators.append("AUTOMATED_SCAN")

        if log_data.get("request_count_per_minute", 0) > 100:
            attack_indicators.append("BRUTE_FORCE")

        return attack_indicators
    
    def _classify_request(self, log_data: Dict[str, Any], attack_indicators: List[str]) -> tuple:
        if attack_indicators:
            classification = "ATTACK"
            threat_level = self._calculate_threat_level(attack_indicators)
        elif log_data.get("access_result") == "PERMISSION_DENIED":
            classification = "EXCEPTION"
            threat_level = "LOW"
        elif log_data.get("access_result") == "SUCCESS":
            classification = "NORMAL"
            threat_level = "LOW"
        else:
            classification = "NORMAL"
            threat_level = "LOW"
        
        return classification, threat_level
    
    def _calculate_threat_level(self, attack_indicators: List[str]) -> str:
        if not attack_indicators:
            return "LOW"

        high_risk = ["SQL_INJECTION", "XSS", "PRIVILEGE_ESCALATION", "COMMAND_INJECTION", "LDAP_INJECTION"]
        medium_risk = ["PATH_TRAVERSAL", "AUTOMATED_SCAN", "SSRF", "NULL_BYTE_INJECTION", "HEADER_INJECTION"]

        if any(attack in high_risk for attack in attack_indicators):
            return "HIGH"
        elif any(attack in medium_risk for attack in attack_indicators):
            return "MEDIUM"
        else:
            return "LOW"
    
    def _calculate_confidence(self, log_data: Dict[str, Any], attack_indicators: List[str]) -> float:
        confidence = 0.5
        
        if attack_indicators:
            confidence += len(attack_indicators) * 0.1
        
        if log_data.get("access_result") == "BLOCKED":
            confidence += 0.2
        
        if log_data.get("security_events"):
            confidence += 0.2
        
        return min(confidence, 1.0)
    
    def _generate_recommendations(self, classification: str, attack_indicators: List[str], threat_level: str) -> List[str]:
        recommendations = []
        
        if classification == "ATTACK":
            recommendations.extend([
                "즉시 해당 IP 주소를 차단하세요.",
                "보안 팀에 즉시 알림을 보내세요."
            ])

            if "LDAP_INJECTION" in attack_indicators:
                recommendations.extend([
                    "LDAP 쿼리 입력 검증을 강화하세요.",
                    "LDAP 서버 접근 권한을 재검토하세요."
                ])

            if "COMMAND_INJECTION" in attack_indicators:
                recommendations.extend([
                    "시스템 명령어 실행을 차단하세요.",
                    "입력 데이터 필터링을 강화하세요."
                ])

            if "HEADER_INJECTION" in attack_indicators:
                recommendations.extend([
                    "HTTP 헤더 검증을 강화하세요.",
                    "요청 헤더 길이 제한을 설정하세요."
                ])
            
            if "SQL_INJECTION" in attack_indicators:
                recommendations.extend([
                    "데이터베이스 액세스 로그를 검토하세요.",
                    "SQL 인젝션 방지 패치를 적용하세요."
                ])
            
            if "XSS" in attack_indicators:
                recommendations.extend([
                    "입력 데이터 검증을 강화하세요.",
                    "CSP(Content Security Policy) 헤더를 설정하세요."
                ])
            
            if "PRIVILEGE_ESCALATION" in attack_indicators:
                recommendations.extend([
                    "권한 관리 시스템을 점검하세요.",
                    "해당 사용자 계정을 일시 정지하세요."
                ])
        
        elif classification == "EXCEPTION":
            recommendations.extend([
                "사용자 권한 설정을 확인하세요.",
                "접근 제어 정책을 검토하세요."
            ])
        
        else:
            recommendations.extend([
                "정상 요청으로 판단됩니다.",
                "지속적인 모니터링을 유지하세요."
            ])
        
        return recommendations
    
    def _detailed_threat_analysis(self, log_data: Dict[str, Any]) -> Dict[str, Any]:
        url = log_data.get("request_url", "").lower()
        
        if any(pattern in url for pattern in ["'", "union", "select"]):
            attack_vector = "SQL_INJECTION"
        elif "<script" in url:
            attack_vector = "XSS"
        elif log_data.get("user_role") != log_data.get("expected_role"):
            attack_vector = "PRIVILEGE_ESCALATION"
        else:
            attack_vector = "UNKNOWN"
        
        classification = log_data.get("document_classification", "")
        action = log_data.get("action_type", "")
        
        if classification in ["TOP_SECRET", "CONFIDENTIAL"] and action in ["READ", "DELETE"]:
            impact = "CRITICAL"
        elif classification == "INTERNAL" and action in ["UPDATE", "DELETE"]:
            impact = "HIGH"
        else:
            impact = "MEDIUM"
        
        return {
            "attack_vector": attack_vector,
            "potential_impact": impact,
            "affected_resources": [
                f"Document: {log_data.get('document_id', 'N/A')}",
                f"User: {log_data.get('user_id', 'N/A')}"
            ]
        }


security_system = SecurityAnalysisSystem()

def validate_request_data(required_fields: List[str] = None) -> Dict[str, Any]:
    if required_fields is None:
        required_fields = ['timestamp', 'user_id', 'request_method', 'request_url']

    # JSON 형식 강제 제거
    data = {}

    # JSON 형식인 경우 파싱
    if request.is_json and request.data:
        try:
            data = request.get_json() or {}
        except Exception as e:
            logger.warning(f"JSON 파싱 실패, 기본값 사용: {str(e)}")

    # Form 데이터가 있는 경우 추가
    if request.form:
        data.update(request.form.to_dict())

    # URL 파라미터가 있는 경우 추가
    if request.args:
        data.update(request.args.to_dict())

    # 기본값으로 요청 정보 추가
    data.setdefault('timestamp', datetime.now().isoformat())
    data.setdefault('user_id', 'anonymous')
    data.setdefault('request_method', request.method)
    data.setdefault('request_url', request.url)
    data.setdefault('access_result', 'UNKNOWN')
    data.setdefault('action_type', 'UNKNOWN')
    data.setdefault('user_role', 'UNKNOWN')
    # 입력 데이터의 필드명 매핑
    if 'url' in data and 'request_url' not in data:
        data['request_url'] = data['url']
    if 'method' in data and 'request_method' not in data:
        data['request_method'] = data['method']
    if 'headers' in data and 'request_headers' not in data:
        data['request_headers'] = data['headers']
    if 'body' in data and 'request_body' not in data:
        data['request_body'] = data['body']
    # 필수 필드 검증 제거 (기본값으로 대체)
    return data

@app.route('/analyze', methods=['POST'])
def analyze_security_log():
    try:
        log_data = validate_request_data()  # 더 이상 예외 발생하지 않음
        result = security_system.analyze_log(log_data)

        if result.get('classification') == 'ATTACK':
            logger.info(f"공격 탐지됨. 고급 분석으로 전환: {result.get('threat_level', 'UNKNOWN')}")
            return analyze_security_log_advanced_internal(log_data)
        else:
            simple_report = security_system.generate_simple_report(result, log_data)
            result["report_path"] = simple_report

        logger.info(f"분석 완료: {result.get('classification', 'UNKNOWN')} - {result.get('threat_level', 'UNKNOWN')}")
        return jsonify(result)

    except Exception as e:
        logger.error(f"분석 요청 처리 중 오류: {str(e)}")
        return jsonify({
            "error": "Internal server error",
            "timestamp": datetime.now().isoformat()
        }), 500

@app.route('/analyze-advanced', methods=['POST'])
def analyze_security_log_advanced():
    try:
        log_data = validate_request_data()  # 더 이상 예외 발생하지 않음
        return analyze_security_log_advanced_internal(log_data)

    except Exception as e:
        logger.error(f"고급 분석 요청 처리 중 오류: {str(e)}")
        return jsonify({
            "error": "Internal server error during advanced analysis",
            "timestamp": datetime.now().isoformat()
        }), 500

def analyze_security_log_advanced_internal(log_data):
    result = security_system.analyze_log_with_llm(log_data)
    
    classification = result.get('enhanced_classification', result.get('classification', 'UNKNOWN'))
    threat_level = result.get('enhanced_threat_level', result.get('threat_level', 'UNKNOWN'))
    
    logger.info(f"고급 분석 완료: {classification} - {threat_level}")
    logger.info(f"보고서 저장 위치: {result.get('report_path', 'N/A')}")
    
    return jsonify(result)
            
@app.errorhandler(404)
def not_found(error):
    return jsonify({"error": "Endpoint not found"}), 404

@app.errorhandler(500)
def internal_error(error):
    return jsonify({"error": "Internal server error"}), 500

if __name__ == '__main__':
    
    try:
        init_database()
    except Exception as e:
        logger.error(f"데이터베이스 초기화 실패: {e}")
        exit(1)
        
    port = int(os.environ.get('PORT', 5001))
    debug = os.environ.get('FLASK_DEBUG', 'False').lower() == 'true'
    
    logger.info(f"서버 시작: http://localhost:{port}")
    logger.info(f"보고서 저장 디렉토리: {list(REPORT_DIRS.values())}")
    
    app.run(host='0.0.0.0', port=port, debug=debug)