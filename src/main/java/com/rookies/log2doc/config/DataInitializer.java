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

}