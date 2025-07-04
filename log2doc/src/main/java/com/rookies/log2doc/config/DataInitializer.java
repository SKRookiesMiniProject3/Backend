package com.rookies.log2doc.config;

import com.rookies.log2doc.entity.Role;
import com.rookies.log2doc.entity.User;
import com.rookies.log2doc.repository.RoleRepository;
import com.rookies.log2doc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 애플리케이션 시작 시 초기 데이터를 생성하는 클래스
 * 기본 권한과 관리자 계정을 자동 생성
 *
 * 알고리즘 설명:
 * 1. 애플리케이션 시작 시 run() 메서드가 자동 실행
 * 2. 기본 권한들을 순차적으로 생성 (존재하지 않는 경우에만)
 * 3. 관리자 계정을 생성 (존재하지 않는 경우에만)
 *
 * 시간 복잡도: O(n) - 권한 개수에 비례
 * 공간 복잡도: O(1) - 고정된 개수의 권한과 사용자만 생성
 */
@Component
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
@Slf4j  // 로거 자동 생성 (log 변수 사용 가능)
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 애플리케이션 시작 시 실행되는 메서드
     * 기본 권한과 관리자 계정을 생성
     */
    @Override
    @Transactional
    public void run(String... args) {
        log.info("데이터베이스 초기화 시작...");

        try {
            // 1. 기본 권한 생성
            initializeRoles();

            // 2. 관리자 계정 생성
            initializeAdminUser();

            log.info("데이터베이스 초기화 완료!");
        } catch (Exception e) {
            log.error("데이터베이스 초기화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("데이터베이스 초기화 실패", e);
        }
    }

    /**
     * 기본 권한들을 생성하는 메서드
     *
     * 알고리즘:
     * 1. 모든 권한 타입을 순회
     * 2. 각 권한이 존재하지 않으면 생성
     * 3. 데이터베이스에 저장
     *
     * 시간 복잡도: O(n) - 권한 개수에 비례
     * 공간 복잡도: O(1) - 고정된 개수의 권한만 생성
     */
    private void initializeRoles() {
        log.info("기본 권한 초기화 중...");

        // 각 권한 타입에 대해 존재하지 않으면 생성
        Arrays.stream(Role.RoleName.values())
                .forEach(roleName -> {
                    if (!roleRepository.existsByName(roleName)) {
                        Role role = Role.builder()
                                .name(roleName)
                                .description(roleName.getDescription())
                                .build();

                        roleRepository.save(role);
                        log.info("권한 생성: {}", roleName.name());
                    } else {
                        log.debug("권한 이미 존재: {}", roleName.name());
                    }
                });

        log.info("기본 권한 초기화 완료");
    }

    /**
     * 관리자 계정을 생성하는 메서드
     *
     * 알고리즘:
     * 1. 관리자 계정 존재 여부 확인
     * 2. 존재하지 않으면 필요한 권한 조회
     * 3. 관리자 계정 생성 및 권한 부여
     * 4. 데이터베이스에 저장
     *
     * 시간 복잡도: O(1) - 고정된 개수의 계정만 생성
     * 공간 복잡도: O(1) - 단일 사용자 객체만 생성
     *
     * 에지 케이스:
     * - 관리자 계정이 이미 존재하는 경우: 건너뛰기
     * - 필요한 권한이 존재하지 않는 경우: 예외 발생
     */
    private void initializeAdminUser() {
        log.info("관리자 계정 초기화 중...");

        // 관리자 계정이 존재하지 않는 경우에만 생성
        if (!userRepository.existsByUsername("admin")) {
            // 관리자 권한 조회
            Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("ADMIN 권한을 찾을 수 없습니다."));

            Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("USER 권한을 찾을 수 없습니다."));

            // 관리자 계정 생성
            User adminUser = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123!@#"))
                    .phone("010-0000-0000")
                    .isActive(true)
                    .isEmailVerified(true)
                    .build();

            // 권한 추가
            adminUser.addRole(adminRole);
            adminUser.addRole(userRole);

            userRepository.save(adminUser);
            log.info("관리자 계정 생성 완료: username=admin, email=admin@example.com");
        } else {
            log.info("관리자 계정이 이미 존재합니다.");
        }

        log.info("관리자 계정 초기화 완료");
    }
}