package com.rookies.log2doc.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * NFS 전용 파일 저장 경로 설정 클래스
 * 무조건 NFS에만 저장하도록 구성
 */
@Component
@Slf4j
public class FileStorageConfig {

    // NFS 경로 설정 - @Value로 직접 읽기
    @Value("${file.upload.path.nfs:/app/document}")
    private String nfsPath;

    // 실제 사용할 NFS Path 객체
    private Path nfsStoragePath;

    @PostConstruct
    public void init() {
        log.info("=== NFS 전용 파일 저장 설정 ===");
        log.info("NFS 경로: '{}'", nfsPath);

        try {
            // NFS 경로가 설정되지 않은 경우 예외 발생
            if (nfsPath == null || nfsPath.trim().isEmpty()) {
                throw new RuntimeException("NFS 경로가 필수입니다. application.properties에서 file.upload.path.nfs를 설정하세요.");
            }

            // NFS Path 객체 생성
            nfsStoragePath = Paths.get(nfsPath.trim());
            log.info("NFS Path 객체 생성: {}", nfsStoragePath.toAbsolutePath());

            // NFS 디렉토리 생성 및 권한 확인
            setupNfsStorage();

            log.info("=== NFS 저장 설정 완료: {} ===", nfsStoragePath.toAbsolutePath());

            // 쓰기 권한 테스트
            testWritePermission(nfsStoragePath);

        } catch (Exception e) {
            log.error("NFS 저장 설정 실패", e);
            throw new RuntimeException("NFS 저장 설정이 필수입니다: " + e.getMessage(), e);
        }
    }

    /**
     * NFS 저장소 설정
     */
    private void setupNfsStorage() throws Exception {
        if (!createDirectoryIfNotExists(nfsStoragePath)) {
            throw new RuntimeException("NFS 디렉토리를 생성할 수 없습니다: " + nfsStoragePath);
        }

        log.info("NFS 디렉토리 준비 완료: {}", nfsStoragePath.toAbsolutePath());
    }

    /**
     * 디렉토리 생성 (없으면 생성)
     */
    private boolean createDirectoryIfNotExists(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("NFS 디렉토리 생성: {}", path.toAbsolutePath());
            }

            // 쓰기 권한 확인
            if (!Files.isWritable(path)) {
                log.error("NFS 디렉토리 쓰기 권한 없음: {}", path.toAbsolutePath());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("NFS 디렉토리 생성 실패: {}", path.toAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 쓰기 권한 테스트
     */
    private void testWritePermission(Path path) {
        try {
            Path testFile = path.resolve("nfs_write_test_" + System.currentTimeMillis() + ".tmp");
            Files.write(testFile, "NFS 쓰기 테스트".getBytes());
            Files.delete(testFile);
            log.info("NFS 쓰기 권한 테스트 성공: {}", path);
        } catch (Exception e) {
            log.error("NFS 쓰기 권한 테스트 실패: {}", path, e);
            throw new RuntimeException("NFS 저장 경로에 쓰기 권한이 없습니다: " + path, e);
        }
    }

    /**
     * 활성화된 저장 경로 반환 (무조건 NFS)
     */
    public Path getActiveStoragePath() {
        if (nfsStoragePath == null) {
            throw new RuntimeException("NFS 저장 경로가 초기화되지 않았습니다.");
        }
        return nfsStoragePath;
    }

    /**
     * 저장 방식 정보 반환
     */
    public String getStorageInfo() {
        if (nfsStoragePath != null) {
            return "NFS 전용: " + nfsStoragePath.toAbsolutePath();
        } else {
            return "NFS 설정 오류";
        }
    }
}