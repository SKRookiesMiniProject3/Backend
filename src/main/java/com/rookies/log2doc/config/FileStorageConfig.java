package com.rookies.log2doc.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 파일 저장 경로 설정 클래스
 * 개발/운영 환경에 따라 다른 저장 경로 사용
 */
@Component
@ConfigurationProperties(prefix = "file.upload")
@Data
@Slf4j
public class FileStorageConfig {
    
    // 로컬 저장 설정
    private Path pathLocal = Paths.get("./uploads");
    private Path pathLocalAbsolute;
    private boolean enabledLocal = true;
    
    // NFS 저장 설정
    private Path pathNfs;
    private boolean enabledNfs = false;
    
    @PostConstruct
    public void init() {
        try {
            // 현재 활성화된 저장 방식에 따라 디렉토리 생성
            if (enabledLocal) {
                setupLocalStorage();
            }
            
            if (enabledNfs) {
                setupNfsStorage();
            }
            
        } catch (Exception e) {
            log.error("파일 저장 디렉토리 초기화 실패", e);
            throw new RuntimeException("파일 저장 설정 초기화 실패", e);
        }
    }
    
    /**
     * 로컬 저장소 설정
     */
    private void setupLocalStorage() throws Exception {
        // 상대 경로로 먼저 시도
        if (createDirectoryIfNotExists(pathLocal)) {
            log.info("로컬 저장 경로 설정 완료: {}", pathLocal.toAbsolutePath());
            return;
        }
        
        // 상대 경로 실패 시 절대 경로 시도
        if (pathLocalAbsolute != null && createDirectoryIfNotExists(pathLocalAbsolute)) {
            pathLocal = pathLocalAbsolute;
            log.info("로컬 저장 경로 설정 완료 (절대경로): {}", pathLocal.toAbsolutePath());
            return;
        }
        
        // 마지막 수단: 시스템 임시 디렉토리 사용
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "uploads");
        if (createDirectoryIfNotExists(tempDir)) {
            pathLocal = tempDir;
            log.warn("⚠임시 디렉토리 사용: {}", pathLocal.toAbsolutePath());
            return;
        }
        
        throw new RuntimeException("로컬 저장 디렉토리를 생성할 수 없습니다.");
    }
    
    /**
     * NFS 저장소 설정
     */
    private void setupNfsStorage() throws Exception {
        if (pathNfs == null) {
            throw new RuntimeException("NFS 경로가 설정되지 않았습니다.");
        }
        
        if (!createDirectoryIfNotExists(pathNfs)) {
            throw new RuntimeException("NFS 디렉토리를 생성할 수 없습니다: " + pathNfs);
        }
        
        log.info("NFS 저장 경로 설정 완료: {}", pathNfs.toAbsolutePath());
    }
    
    /**
     * 디렉토리 생성 (없으면 생성)
     */
    private boolean createDirectoryIfNotExists(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("디렉토리 생성: {}", path.toAbsolutePath());
            }
            
            // 쓰기 권한 확인
            if (!Files.isWritable(path)) {
                log.error("쓰기 권한 없음: {}", path.toAbsolutePath());
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("디렉토리 생성 실패: {}", path.toAbsolutePath(), e);
            return false;
        }
    }
    
    /**
     * 현재 활성화된 저장 경로 반환
     */
    public Path getActiveStoragePath() {
        if (enabledNfs && pathNfs != null) {
            return pathNfs;
        } else if (enabledLocal && pathLocal != null) {
            return pathLocal;
        } else {
            throw new RuntimeException("활성화된 저장 경로가 없습니다.");
        }
    }
    
    /**
     * 저장 방식 정보 반환
     */
    public String getStorageInfo() {
        if (enabledNfs) {
            return "NFS: " + pathNfs.toAbsolutePath();
        } else {
            return "LOCAL: " + pathLocal.toAbsolutePath();
        }
    }
}