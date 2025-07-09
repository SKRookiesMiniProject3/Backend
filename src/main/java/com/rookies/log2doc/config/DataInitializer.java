package com.rookies.log2doc.config;

import com.rookies.log2doc.entity.CategoryType;
import com.rookies.log2doc.entity.Role;
import com.rookies.log2doc.entity.User;
import com.rookies.log2doc.entity.ErrorReport;
import com.rookies.log2doc.repository.CategoryTypeRepository;
import com.rookies.log2doc.repository.ErrorReportRepository;
import com.rookies.log2doc.repository.RoleRepository;
import com.rookies.log2doc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 애플리케이션 시작 시 초기 데이터를 생성하는 클래스
 * 기본 직급과 관리자 계정을 자동 생성
 *
 * 알고리즘 설명:
 * 1. 애플리케이션 시작 시 run() 메서드가 자동 실행
 * 2. 기본 직급들을 순차적으로 생성 (존재하지 않는 경우에만)
 * 3. 관리자 계정을 생성 (존재하지 않는 경우에만)
 *
 * 시간 복잡도: O(n) - 직급 개수에 비례
 * 공간 복잡도: O(1) - 고정된 개수의 직급과 사용자만 생성
 */
@Component
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
@Slf4j  // 로거 자동 생성 (log 변수 사용 가능)
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryTypeRepository categoryTypeRepository;
    private final ErrorReportRepository errorReportRepository;

    /**
     * 애플리케이션 시작 시 실행되는 메서드
     * 기본 직급과 관리자 계정을 생성
     */
    @Override
    @Transactional
    public void run(String... args) {
        log.info("데이터베이스 초기화 시작...");

        try {
            // 1. 기본 직급 생성
            initializeRoles();

            // 2. 관리자 계정 생성
            initializeAdminUser();

            // 3. 테스트 사용자 생성
            initializeTestUsers();

            // 4. 테스트 카테고리 생성
            initializeCategoryTypes();

            // 5. 에러 리포트 생성
            initializeErrorReports();

            log.info("데이터베이스 초기화 완료!");
        } catch (Exception e) {
            log.error("데이터베이스 초기화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("데이터베이스 초기화 실패", e);
        }
    }

    /**
     * 기본 직급들을 생성하는 메서드
     *
     * 알고리즘:
     * 1. 모든 직급 타입을 순회
     * 2. 각 직급이 존재하지 않으면 생성
     * 3. 데이터베이스에 저장
     *
     * 시간 복잡도: O(n) - 직급 개수에 비례
     * 공간 복잡도: O(1) - 고정된 개수의 직급만 생성
     */
    private void initializeRoles() {
        log.info("기본 직급 초기화 중...");

        // 각 직급 타입에 대해 존재하지 않으면 생성
        Arrays.stream(Role.RoleName.values())
                .forEach(roleName -> {
                    if (!roleRepository.existsByName(roleName)) {
                        Role role = Role.builder()
                                .name(roleName)
                                .description(roleName.getDescription())
                                .build();

                        roleRepository.save(role);
                        log.info("직급 생성: {} ({})", roleName.name(), roleName.getDescription());
                    } else {
                        log.debug("직급 이미 존재: {}", roleName.name());
                    }
                });

        log.info("기본 직급 초기화 완료");
    }

    /**
     * 관리자 계정을 생성하는 메서드
     *
     * 알고리즘:
     * 1. 관리자 계정 존재 여부 확인
     * 2. 존재하지 않으면 CEO 직급 조회
     * 3. 관리자 계정 생성 및 직급 부여
     * 4. 데이터베이스에 저장
     *
     * 시간 복잡도: O(1) - 고정된 개수의 계정만 생성
     * 공간 복잡도: O(1) - 단일 사용자 객체만 생성
     *
     * 에지 케이스:
     * - 관리자 계정이 이미 존재하는 경우: 건너뛰기
     * - 필요한 직급이 존재하지 않는 경우: 예외 발생
     */
    private void initializeAdminUser() {
        log.info("관리자 계정 초기화 중...");

        // 관리자 계정이 존재하지 않는 경우에만 생성
        if (!userRepository.existsByUsername("admin")) {
            // CEO 직급 조회
            Role ceoRole = roleRepository.findByName(Role.RoleName.CEO)
                    .orElseThrow(() -> new RuntimeException("CEO 직급을 찾을 수 없습니다."));

            // 관리자 계정 생성
            User adminUser = User.builder()
                    .username("admin")
                    .email("admin@company.com")
                    .password(passwordEncoder.encode("admin123!@#"))
                    .phone("010-0000-0000")
                    .isActive(true)
                    .isEmailVerified(true)
                    .role(ceoRole)  // 1:1 관계로 직급 설정
                    .build();

            userRepository.save(adminUser);
            log.info("관리자 계정 생성 완료: username=admin, email=admin@company.com, role=CEO");
        } else {
            log.info("관리자 계정이 이미 존재합니다.");
        }

        log.info("관리자 계정 초기화 완료");
    }

    /**
     * 테스트용 사용자들을 생성하는 메서드
     * 각 직급별로 샘플 사용자를 생성하여 테스트 환경 구성
     */
    private void initializeTestUsers() {
        log.info("테스트 사용자 초기화 중...");

        // 각 직급별 테스트 사용자 생성
        createTestUser("intern01", "intern@company.com", Role.RoleName.INTERN, "인턴 테스트 계정");
        createTestUser("staff01", "staff@company.com", Role.RoleName.STAFF, "사원 테스트 계정");
        createTestUser("manager01", "manager@company.com", Role.RoleName.MANAGER, "과장 테스트 계정");
        createTestUser("director01", "director@company.com", Role.RoleName.DIRECTOR, "부장 테스트 계정");
        createTestUser("vp01", "vp@company.com", Role.RoleName.VICE_PRESIDENT, "이사 테스트 계정");

        log.info("테스트 사용자 초기화 완료");
    }

    /**
     * 단일 테스트 사용자를 생성하는 헬퍼 메서드
     *
     * @param username 사용자명
     * @param email 이메일
     * @param roleName 직급
     * @param description 설명
     */
    private void createTestUser(String username, String email, Role.RoleName roleName, String description) {
        if (!userRepository.existsByUsername(username)) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException(roleName.name() + " 직급을 찾을 수 없습니다."));

            User testUser = User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode("test123!@#"))
                    .phone("010-1234-5678")
                    .isActive(true)
                    .isEmailVerified(true)
                    .role(role)
                    .build();

            userRepository.save(testUser);
            log.info("테스트 사용자 생성: {} - {} ({})", username, description, roleName.getDescription());
        } else {
            log.debug("테스트 사용자 이미 존재: {}", username);
        }
    }

    private void initializeCategoryTypes() {
        log.info("카테고리 타입 초기화 중...");

        String[][] defaultCategories = {
                {"A", "Category A"},
                {"B", "Category B"},
                {"C", "Category C"},
                {"D", "Category D"},
                {"E", "Category E"},
                {"F", "Category F"},
        };

        for (String[] cat : defaultCategories) {
            String name = cat[0];
            String desc = cat[1];

            if (!categoryTypeRepository.existsByName(name)) {
                CategoryType type = CategoryType.builder()
                        .name(name)
                        .description(desc)
                        .isDeleted(false)
                        .build();

                categoryTypeRepository.save(type);
                log.info("카테고리 타입 생성: {} ({})", name, desc);
            } else {
                log.debug("카테고리 타입 이미 존재: {}", name);
            }
        }

        log.info("카테고리 타입 초기화 완료");
    }

    private void initializeErrorReports() {
        log.info("프론트 테스트용 날짜별 분산 에러 리포트 샘플 데이터 초기화 중...");

        LocalDateTime now = LocalDateTime.now();

        // ========================================
        // 헬퍼 메서드로 날짜별 데이터 생성
        // ========================================

        // 30일 전 - 공격 1건
        createSampleReport(now.minusDays(30), "Credential Stuffing 공격 탐지",
                "유출된 계정 정보를 이용한 대량 로그인 시도가 감지되었습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.COMPLETED);

        // 28일 전 - 정상 1건, 오류 1건
        createSampleReport(now.minusDays(28), "일일 백업 정상 완료",
                "모든 데이터베이스와 파일 시스템 백업이 성공적으로 완료되었습니다.",
                ErrorReport.ReportCategory.VALID, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(28).plusHours(14), "메모리 사용량 임계치 경고",
                "서버 메모리 사용률이 85%를 초과했습니다. 메모리 누수 가능성을 조사 중입니다.",
                ErrorReport.ReportCategory.INVALID, ErrorReport.ReportStatus.COMPLETED);

        // 25일 전 - 공격 2건
        createSampleReport(now.minusDays(25), "SQL Injection 시도 차단",
                "로그인 폼에서 악성 SQL 쿼리 삽입 시도가 감지되어 자동 차단되었습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(25).plusHours(16), "XSS 공격 패턴 탐지",
                "게시판에서 악성 스크립트 삽입 시도가 감지되었습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.COMPLETED);

        // 22일 전 - 정상 2건
        createSampleReport(now.minusDays(22), "시스템 헬스체크 정상",
                "모든 마이크로서비스가 정상 동작 중입니다. CPU, 메모리, 디스크 모두 안정적입니다.",
                ErrorReport.ReportCategory.VALID, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(22).plusHours(12), "API 응답시간 정상",
                "주요 API 엔드포인트들의 평균 응답시간이 200ms 이하로 양호합니다.",
                ErrorReport.ReportCategory.VALID, ErrorReport.ReportStatus.COMPLETED);

        // 20일 전 - 오류 1건
        createSampleReport(now.minusDays(20), "데이터베이스 연결 풀 고갈",
                "최대 연결 수에 도달하여 새로운 DB 연결이 대기 상태입니다.",
                ErrorReport.ReportCategory.INVALID, ErrorReport.ReportStatus.COMPLETED);

        // 18일 전 - 공격 1건, 정상 1건
        createSampleReport(now.minusDays(18), "DDoS 공격 패턴 감지",
                "동일 IP 대역에서 초당 500건 이상의 HTTP 요청이 감지되었습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(18).plusHours(10), "SSL 인증서 갱신 완료",
                "만료 예정이던 SSL 인증서가 자동으로 갱신되었습니다.",
                ErrorReport.ReportCategory.VALID, ErrorReport.ReportStatus.COMPLETED);

        // 15일 전 - 오류 2건
        createSampleReport(now.minusDays(15), "Redis 캐시 서버 장애",
                "Redis 서버가 응답하지 않아 캐시 기능이 일시적으로 중단되었습니다.",
                ErrorReport.ReportCategory.INVALID, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(15).plusHours(8), "외부 API 연동 실패",
                "결제 시스템 API 호출에서 타임아웃이 발생하고 있습니다.",
                ErrorReport.ReportCategory.INVALID, ErrorReport.ReportStatus.COMPLETED);

        // 12일 전 - 공격 3건 (공격 집중일)
        createSampleReport(now.minusDays(12), "무차별 대입 공격 탐지",
                "admin 계정에 대한 무차별 패스워드 시도가 1시간 동안 지속되었습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(12).plusHours(6), "파일 업로드 공격 시도",
                "악성 파일(.php, .exe) 업로드 시도가 여러 차례 감지되었습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(12).plusHours(20), "디렉토리 트래버설 공격",
                "시스템 파일 접근을 위한 경로 조작 시도가 감지되었습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.COMPLETED);

        // 10일 전 - 정상 1건
        createSampleReport(now.minusDays(10), "주간 성능 리포트 정상",
                "지난 주 시스템 성능 지표가 모든 항목에서 정상 범위를 유지했습니다.",
                ErrorReport.ReportCategory.VALID, ErrorReport.ReportStatus.COMPLETED);

        // 8일 전 - 오류 1건, 정상 1건
        createSampleReport(now.minusDays(8), "로그 파일 용량 초과",
                "애플리케이션 로그 파일이 10GB를 초과하여 디스크 공간 부족 경고가 발생했습니다.",
                ErrorReport.ReportCategory.INVALID, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(8).plusHours(15), "사용자 인증 시스템 정상",
                "OAuth 2.0 인증 서버가 안정적으로 동작 중입니다. 토큰 발급율 99.9%입니다.",
                ErrorReport.ReportCategory.VALID, ErrorReport.ReportStatus.COMPLETED);

        // 5일 전 - 공격 1건, 오류 1건
        createSampleReport(now.minusDays(5), "세션 하이재킹 시도 탐지",
                "유효하지 않은 세션 토큰을 이용한 접근 시도가 감지되었습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(5).plusHours(11), "마이크로서비스 간 통신 오류",
                "주문 서비스와 재고 서비스 간 gRPC 통신에서 간헐적 오류가 발생하고 있습니다.",
                ErrorReport.ReportCategory.INVALID, ErrorReport.ReportStatus.COMPLETED);

        // 3일 전 - 정상 2건
        createSampleReport(now.minusDays(3), "데이터베이스 최적화 완료",
                "인덱스 재구성 및 통계 업데이트가 완료되어 쿼리 성능이 20% 향상되었습니다.",
                ErrorReport.ReportCategory.VALID, ErrorReport.ReportStatus.COMPLETED);
        createSampleReport(now.minusDays(3).plusHours(13), "CDN 캐시 히트율 양호",
                "CDN 캐시 히트율이 95%를 유지하여 서버 부하가 크게 감소했습니다.",
                ErrorReport.ReportCategory.VALID, ErrorReport.ReportStatus.COMPLETED);

        // 1일 전 - 공격 1건, 진행중
        createSampleReport(now.minusDays(1), "실시간 봇 트래픽 탐지",
                "자동화된 봇으로 추정되는 비정상적인 트래픽 패턴이 실시간으로 감지되고 있습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.IN_PROGRESS);

        // 오늘 - 오류 1건 진행중, 정상 1건 완료, 공격 1건 시작안함
        createSampleReport(now.minusHours(6), "API 응답 지연 발생",
                "사용자 조회 API에서 평소보다 3배 느린 응답시간을 보이고 있습니다.",
                ErrorReport.ReportCategory.INVALID, ErrorReport.ReportStatus.IN_PROGRESS);

        createSampleReport(now.minusHours(3), "시스템 모니터링 정상",
                "모든 시스템 메트릭이 정상 범위 내에서 안정적으로 유지되고 있습니다.",
                ErrorReport.ReportCategory.VALID, ErrorReport.ReportStatus.COMPLETED);

        createSampleReport(now.minusHours(1), "의심스러운 로그인 패턴 감지",
                "새벽 시간대 비정상적인 지역에서의 로그인 시도가 감지되었습니다.",
                ErrorReport.ReportCategory.ATTACK, ErrorReport.ReportStatus.NOT_STARTED);

        log.info("📊 프론트 테스트용 샘플 데이터 생성 완료!");
        log.info("📈 총 25건 - 30일간 분산 데이터");
        log.info("🚨 공격 탐지: 10건 (40%)");
        log.info("⚠️ 시스템 오류: 8건 (32%)");
        log.info("✅ 정상 동작: 7건 (28%)");
    }

    /**
     * 샘플 리포트 생성 헬퍼 메서드
     */
    private void createSampleReport(LocalDateTime createdDate, String title, String preview,
                                    ErrorReport.ReportCategory category, ErrorReport.ReportStatus status) {

        String dateStr = createdDate.toLocalDate().toString();
        String timeStr = createdDate.toLocalTime().toString().substring(0, 5); // HH:mm

        ErrorReport report = ErrorReport.builder()
                .reportTitle(title)
                .reportPreview(preview)
                .reportCategory(category)
                .reportPath(String.format("/reports/%s/%s_%s_%03d.json",
                        category.name().toLowerCase(),
                        category.name().toLowerCase(),
                        dateStr,
                        Math.abs(title.hashCode() % 1000)))
                .reportStatus(status)
                .reportComment(generateComment(category, status))
                .isDeleted(false)
                .build();

        // 날짜 수동 설정을 위해 별도 처리
        report.setCreatedDt(createdDate);

        errorReportRepository.save(report);
    }

    /**
     * 카테고리와 상태에 따른 코멘트 생성
     */
    private String generateComment(ErrorReport.ReportCategory category, ErrorReport.ReportStatus status) {
        switch (category) {
            case ATTACK:
                return status == ErrorReport.ReportStatus.COMPLETED ?
                        "보안팀에서 대응 완료" :
                        status == ErrorReport.ReportStatus.IN_PROGRESS ?
                                "보안팀 긴급 대응 중" : "보안팀 검토 대기";
            case INVALID:
                return status == ErrorReport.ReportStatus.COMPLETED ?
                        "시스템팀에서 수정 완료" :
                        status == ErrorReport.ReportStatus.IN_PROGRESS ?
                                "시스템팀 조치 중" : "시스템팀 배정 대기";
            case VALID:
                return "정상 동작 확인됨";
            default:
                return "검토 중";
        }
    }

//    private void initializeErrorReports() {
//        log.info("에러 리포트 샘플 데이터 초기화 중... (새 구조)");
//
//        // ========================================
//        // 샘플 에러 리포트 1 - 공격 탐지 (중요!)
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("SQL Injection 공격 시도 탐지")
//                .reportPreview("로그인 폼에서 악성 SQL 쿼리 삽입 시도가 감지되었습니다. 사용자 입력값에서 'UNION SELECT' 패턴을 발견했습니다.")
//                .reportCategory(ErrorReport.ReportCategory.ATTACK)
//                .reportPath("/reports/security/sql_injection_20240701_001.json")
//                .reportStatus(ErrorReport.ReportStatus.IN_PROGRESS)
//                .reportComment("보안팀에서 긴급 대응 중")
//                .build());
//
//        // ========================================
//        // 샘플 에러 리포트 2 - 공격 탐지 (완료됨)
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("무차별 대입 공격(Brute Force) 탐지")
//                .reportPreview("동일 IP에서 10분간 500회 이상의 로그인 실패 시도가 발생했습니다. 패스워드 크래킹 시도로 판단됩니다.")
//                .reportCategory(ErrorReport.ReportCategory.ATTACK)
//                .reportPath("/reports/security/brute_force_20240701_002.json")
//                .reportStatus(ErrorReport.ReportStatus.COMPLETED)
//                .reportComment("해당 IP 차단 완료 및 보안 정책 강화")
//                .build());
//
//        // ========================================
//        // 샘플 에러 리포트 3 - 비정상 동작
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("데이터베이스 연결 풀 고갈")
//                .reportPreview("최대 데이터베이스 연결 수(50개)에 도달하여 새로운 연결 요청이 거부되고 있습니다. 커넥션 누수 의심됩니다.")
//                .reportCategory(ErrorReport.ReportCategory.INVALID)
//                .reportPath("/reports/system/db_pool_exhaustion_20240701_003.json")
//                .reportStatus(ErrorReport.ReportStatus.IN_PROGRESS)
//                .reportComment("DBA팀에서 커넥션 풀 설정 검토 중")
//                .build());
//
//        // ========================================
//        // 샘플 에러 리포트 4 - 비정상 동작 (완료)
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("메모리 사용량 임계치 초과")
//                .reportPreview("JVM 힙 메모리 사용률이 95%를 초과했습니다. GC 빈도가 증가하여 애플리케이션 성능에 영향을 주고 있습니다.")
//                .reportCategory(ErrorReport.ReportCategory.INVALID)
//                .reportPath("/reports/system/memory_overflow_20240701_004.json")
//                .reportStatus(ErrorReport.ReportStatus.COMPLETED)
//                .reportComment("힙 메모리 크기 증설 및 메모리 누수 패치 완료")
//                .build());
//
//        // ========================================
//        // 샘플 에러 리포트 5 - 정상 동작
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("정기 시스템 백업 완료")
//                .reportPreview("매일 새벽 2시 정기 백업이 정상적으로 완료되었습니다. 모든 데이터베이스와 파일 시스템이 성공적으로 백업되었습니다.")
//                .reportCategory(ErrorReport.ReportCategory.VALID)
//                .reportPath("/reports/backup/daily_backup_20240701_005.json")
//                .reportStatus(ErrorReport.ReportStatus.COMPLETED)
//                .reportComment("백업 정상 완료 - 이상 없음")
//                .build());
//
//        // ========================================
//        // 샘플 에러 리포트 6 - 정상 동작
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("사용자 로그인 성공률 정상")
//                .reportPreview("지난 24시간 동안 사용자 로그인 성공률이 98.5%로 정상 범위 내에 있습니다. 평균 응답시간 0.3초로 양호합니다.")
//                .reportCategory(ErrorReport.ReportCategory.VALID)
//                .reportPath("/reports/auth/login_stats_20240701_006.json")
//                .reportStatus(ErrorReport.ReportStatus.COMPLETED)
//                .reportComment("인증 시스템 정상 동작 확인")
//                .build());
//
//        // ========================================
//        // 샘플 에러 리포트 7 - 공격 탐지 (아직 시작 안함)
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("XSS 공격 패턴 탐지")
//                .reportPreview("게시판 댓글 입력에서 악성 스크립트 삽입 시도가 감지되었습니다. '<script>' 태그를 포함한 입력값이 필터링되었습니다.")
//                .reportCategory(ErrorReport.ReportCategory.ATTACK)
//                .reportPath("/reports/security/xss_attempt_20240701_007.json")
//                .reportStatus(ErrorReport.ReportStatus.NOT_STARTED)
//                .reportComment("보안팀 검토 대기 중")
//                .build());
//
//        // ========================================
//        // 샘플 에러 리포트 8 - 비정상 동작 (시작 안함)
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("API 응답 시간 지연")
//                .reportPreview("사용자 정보 조회 API(/api/users)의 평균 응답시간이 3초를 초과했습니다. 정상 범위(1초) 대비 3배 지연되고 있습니다.")
//                .reportCategory(ErrorReport.ReportCategory.INVALID)
//                .reportPath("/reports/performance/api_delay_20240701_008.json")
//                .reportStatus(ErrorReport.ReportStatus.NOT_STARTED)
//                .reportComment("성능 최적화팀 배정 예정")
//                .build());
//
//        // ========================================
//        // 샘플 에러 리포트 9 - 공격 탐지 (진행중) - 매우 중요!
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("DDoS 공격 패턴 감지")
//                .reportPreview("동일한 C클래스 대역(192.168.1.*)에서 초당 1000건 이상의 HTTP 요청이 감지되었습니다. 서비스 가용성에 위협이 될 수 있습니다.")
//                .reportCategory(ErrorReport.ReportCategory.ATTACK)
//                .reportPath("/reports/security/ddos_attack_20240701_009.json")
//                .reportStatus(ErrorReport.ReportStatus.IN_PROGRESS)
//                .reportComment("🚨 긴급! 트래픽 차단 및 CDN 방화벽 활성화 중")
//                .build());
//
//        // ========================================
//        // 샘플 에러 리포트 10 - 정상 동작
//        // ========================================
//        errorReportRepository.save(ErrorReport.builder()
//                .reportTitle("일일 트랜잭션 처리량 정상")
//                .reportPreview("오늘 처리된 총 트랜잭션은 45,832건으로 평균 대비 정상 수준입니다. 오류율 0.02%로 매우 안정적입니다.")
//                .reportCategory(ErrorReport.ReportCategory.VALID)
//                .reportPath("/reports/transaction/daily_summary_20240701_010.json")
//                .reportStatus(ErrorReport.ReportStatus.COMPLETED)
//                .reportComment("시스템 안정성 양호")
//                .build());
//
//        log.info("📊 에러 리포트 샘플 데이터 초기화 완료!");
//        log.info("🚨 공격 탐지 리포트: 4건");
//        log.info("⚠️ 비정상 동작 리포트: 3건");
//        log.info("✅ 정상 동작 리포트: 3건");
//    }

}