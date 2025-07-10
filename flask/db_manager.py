import mysql.connector
from mysql.connector import Error
import os
from datetime import datetime
from typing import Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)

class DatabaseManager:
    def __init__(self):
        self.connection = None
        self.db_config = {
            'host': os.getenv('DB_HOST', 'localhost'),
            'port': int(os.getenv('DB_PORT', '3306')),
            'user': os.getenv('DB_USER', 'admin'),
            'password': os.getenv('DB_PASSWORD', 'admin'),
            'database': os.getenv('DB_NAME', 'log2doc'),
            'charset': 'utf8mb4',
            'collation': 'utf8mb4_unicode_ci'
        }
    
    def connect(self):
        """데이터베이스 연결"""
        try:
            if self.connection is None or not self.connection.is_connected():
                self.connection = mysql.connector.connect(**self.db_config)
                logger.info("MariaDB 연결 성공")
        except Error as e:
            logger.error(f"MariaDB 연결 실패: {e}")
            raise
    
    def disconnect(self):
        """데이터베이스 연결 해제"""
        if self.connection and self.connection.is_connected():
            self.connection.close()
            logger.info("MariaDB 연결 해제")
    
    def create_error_report_table(self):
        """error_report 테이블 생성"""
        create_table_query = """
        CREATE TABLE IF NOT EXISTS error_report (
            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '리포트 ID',
            report_title VARCHAR(500) NOT NULL DEFAULT 'title' COMMENT '리포트 제목',
            report_preview TEXT DEFAULT 'preview' COMMENT '리포트 프리뷰',
            report_category VARCHAR(255) NOT NULL COMMENT '보고서 카테고리 (valid, invalid, attack)',
            report_status VARCHAR(255) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '리포트 진행상황',
            report_comment TEXT COMMENT '리포트 작업에 맞는 코멘트',
            report_path VARCHAR(255) NOT NULL COMMENT '리포트 파일 경로',
            is_deleted BOOLEAN DEFAULT FALSE COMMENT '삭제 여부',
            created_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일 (레코드 생성 시 자동 설정)',
            deleted_dt DATETIME NULL COMMENT '삭제일 (삭제 시 설정, null: 미삭제)'
        ) CHARACTER SET = 'utf8mb4' COLLATE = 'utf8mb4_unicode_ci' COMMENT='에러 리포트 테이블'
        """
        
        try:
            self.connect()
            cursor = self.connection.cursor()
            cursor.execute(create_table_query)
            self.connection.commit()
            logger.info("error_report 테이블 생성 완료")
        except Error as e:
            logger.error(f"테이블 생성 실패: {e}")
            raise
        finally:
            if cursor:
                cursor.close()
    
    def save_error_report(self, analysis: Dict[str, Any], log_data: Dict[str, Any], report_path: str) -> Optional[int]:
        """에러 리포트를 데이터베이스에 저장"""
        try:
            self.connect()
            cursor = self.connection.cursor()
            
            # 분류 결정
            classification = analysis.get("enhanced_classification", analysis.get("classification", "UNKNOWN"))
            
            # 카테고리 매핑
            category_mapping = {
                "NORMAL": "VALID",
                "EXCEPTION": "INVALID",
                "ATTACK": "ATTACK"
            }
            report_category = category_mapping.get(classification, "invalid")
            
            # 제목 추출 (LLM 분석에서 'what' 내용 또는 기본값)
            llm_analysis = analysis.get("llm_analysis", {})
            report_title = llm_analysis.get("what", "title")
            if not report_title or report_title.strip() == "title":
                if classification == "EXCEPTION":
                    report_title = "접근 거부"
                elif classification == "NORMAL":
                    report_title = "접근 허용"
            
            # 프리뷰 추출 (LLM 분석에서 'why' 내용 또는 기본값)
            report_preview = "시스템 침해 또는 정보 탈취 목적으로 추정" if classification == "ATTACK" else "preview"
            
            # 추가적인 'why' 정보가 있다면 사용
            if "why" in llm_analysis:
                report_preview = llm_analysis["why"]
            elif classification == "EXCEPTION":
                report_preview = llm_analysis.get("reason", "권한 부족으로 인한 접근 거부")
            elif classification == "NORMAL":
                report_preview = llm_analysis.get("status", "정상적인 접근 요청")
            
            # INSERT 쿼리 실행
            insert_query = """
            INSERT INTO error_report (
                report_title, report_preview, report_category, 
                report_status, report_comment, report_path, 
                is_deleted, created_dt
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            values = (
                report_title[:500],  # 제목 길이 제한
                report_preview,
                report_category,
                'NOT_STARTED',
                '',  # 빈 코멘트
                report_path,
                False,  # is_deleted
                datetime.now()
            )
            
            cursor.execute(insert_query, values)
            self.connection.commit()
            
            # 생성된 ID 반환
            report_id = cursor.lastrowid
            logger.info(f"에러 리포트 DB 저장 완료: ID={report_id}, 카테고리={report_category}")
            
            return report_id
            
        except Error as e:
            logger.error(f"에러 리포트 저장 실패: {e}")
            if self.connection:
                self.connection.rollback()
            return None
        except Exception as e:
            logger.error(f"예상치 못한 오류: {e}")
            if self.connection:
                self.connection.rollback()
            return None
        finally:
            if cursor:
                cursor.close()
    
    def get_error_report(self, report_id: int) -> Optional[Dict[str, Any]]:
        """특정 에러 리포트 조회"""
        try:
            self.connect()
            cursor = self.connection.cursor(dictionary=True)
            
            select_query = """
            SELECT * FROM error_report 
            WHERE id = %s AND is_deleted = FALSE
            """
            
            cursor.execute(select_query, (report_id,))
            result = cursor.fetchone()
            
            return result
            
        except Error as e:
            logger.error(f"에러 리포트 조회 실패: {e}")
            return None
        finally:
            if cursor:
                cursor.close()
    
    def get_error_reports_by_category(self, category: str, limit: int = 100) -> list:
        """카테고리별 에러 리포트 목록 조회"""
        try:
            self.connect()
            cursor = self.connection.cursor(dictionary=True)
            
            select_query = """
            SELECT * FROM error_report 
            WHERE report_category = %s AND is_deleted = FALSE
            ORDER BY created_dt DESC
            LIMIT %s
            """
            
            cursor.execute(select_query, (category, limit))
            results = cursor.fetchall()
            
            return results
            
        except Error as e:
            logger.error(f"에러 리포트 목록 조회 실패: {e}")
            return []
        finally:
            if cursor:
                cursor.close()
    
    def update_report_status(self, report_id: int, status: str, comment: str = None) -> bool:
        """리포트 상태 업데이트"""
        try:
            self.connect()
            cursor = self.connection.cursor()
            
            if comment:
                update_query = """
                UPDATE error_report 
                SET report_status = %s, report_comment = %s 
                WHERE id = %s
                """
                cursor.execute(update_query, (status, comment, report_id))
            else:
                update_query = """
                UPDATE error_report 
                SET report_status = %s 
                WHERE id = %s
                """
                cursor.execute(update_query, (status, report_id))
            
            self.connection.commit()
            logger.info(f"리포트 상태 업데이트 완료: ID={report_id}, 상태={status}")
            return True
            
        except Error as e:
            logger.error(f"리포트 상태 업데이트 실패: {e}")
            if self.connection:
                self.connection.rollback()
            return False
        finally:
            if cursor:
                cursor.close()
    
    def delete_report(self, report_id: int) -> bool:
        """리포트 소프트 삭제"""
        try:
            self.connect()
            cursor = self.connection.cursor()
            
            update_query = """
            UPDATE error_report 
            SET is_deleted = TRUE, deleted_dt = %s 
            WHERE id = %s
            """
            
            cursor.execute(update_query, (datetime.now(), report_id))
            self.connection.commit()
            logger.info(f"리포트 삭제 완료: ID={report_id}")
            return True
            
        except Error as e:
            logger.error(f"리포트 삭제 실패: {e}")
            if self.connection:
                self.connection.rollback()
            return False
        finally:
            if cursor:
                cursor.close()

# 전역 데이터베이스 매니저 인스턴스
db_manager = DatabaseManager()

def init_database():
    """데이터베이스 초기화"""
    try:
        db_manager.create_error_report_table()
        logger.info("데이터베이스 초기화 완료")
    except Exception as e:
        logger.error(f"데이터베이스 초기화 실패: {e}")
        raise