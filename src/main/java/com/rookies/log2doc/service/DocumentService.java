package com.rookies.log2doc.service;

import com.rookies.log2doc.config.FileStorageConfig;
import com.rookies.log2doc.dto.response.CategoryTypeDTO;
import com.rookies.log2doc.dto.response.DocumentResponseDTO;
import com.rookies.log2doc.dto.response.RoleDTO;
import com.rookies.log2doc.entity.*;
import com.rookies.log2doc.exception.PermissionDeniedException;
import com.rookies.log2doc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final RoleRepository roleRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final DocumentCategoryRepository documentCategoryRepository;
    private final FileStorageConfig fileStorageConfig;

    /**
     * 파일 업로드 후 문서 생성 (저장 경로 개선)
     */
    @Transactional
    public Document uploadDocument(
            MultipartFile file,
            String title,
            String content,
            Long categoryTypeId,
            Long readRoleId,
            Long userId,
            String userRoleName
    ) throws IOException {

        // ✅ 읽기 권한 가져오기
        Role readRole = getRoleById(readRoleId, "읽기");

        // ✅ 파일 정보 처리
        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString();
        String storedFileName = uuid + extension;

        // ✅ 설정된 저장 경로 사용
        Path uploadDir = fileStorageConfig.getActiveStoragePath();

        log.info("📁 파일 저장 경로: {}", uploadDir.toAbsolutePath());
        log.info("📄 저장할 파일명: {}", storedFileName);
        log.info("💾 저장 방식: {}", fileStorageConfig.getStorageInfo());

        // ✅ 실제 파일 저장
        Path savePath = uploadDir.resolve(storedFileName);

        try {
            Files.copy(file.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ 파일 저장 성공: {}", savePath.toAbsolutePath());

            // 저장된 파일 크기 확인
            long savedFileSize = Files.size(savePath);
            log.info("📊 저장된 파일 크기: {} bytes", savedFileSize);

        } catch (IOException e) {
            log.error("❌ 파일 저장 실패: {}", savePath.toAbsolutePath(), e);
            throw new RuntimeException("파일 저장에 실패했습니다: " + e.getMessage(), e);
        }

        // ✅ 문서 엔티티 생성
        Document doc = new Document();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setFileName(originalFileName);
        doc.setFilePath(uuid);           // 해시 (UUID)
        doc.setFilePathNfs(savePath.toString()); // 실제 저장된 물리 경로
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setReadRole(readRole);
        doc.setAuthor(String.valueOf(userId));
        doc.setCreatedRole(userRoleName);

        // ✅ 카테고리 FK
        CategoryType categoryType = categoryTypeRepository.findById(categoryTypeId)
                .orElseThrow(() -> new RuntimeException("카테고리 타입 없음"));

        // ✅ 문서 저장
        documentRepository.save(doc);

        // ✅ 카테고리 매핑 저장
        DocumentCategory mapping = new DocumentCategory();
        mapping.setDocument(doc);
        mapping.setCategoryType(categoryType);
        documentCategoryRepository.save(mapping);

        log.info("✅ 문서 저장 완료 - ID: {}, 파일: {}", doc.getId(), originalFileName);

        return doc;
    }

    /**
     * 문서 리스트 조회 (카테고리, 기간, 권한)
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentList(Long categoryTypeId, String userRoleName, LocalDate startDate, LocalDate endDate) {
        List<Document> docs = (categoryTypeId != null)
                ? documentRepository.findByCategoryTypeIdAndIsDeletedFalseWithRoles(categoryTypeId)
                : documentRepository.findAllWithRoles();

        Role.RoleName userRole = Role.RoleName.valueOf(userRoleName);
        int userLevel = userRole.getLevel();

        return docs.stream()
                .filter(doc -> userLevel >= doc.getReadRole().getName().getLevel())
                .filter(doc -> isInDateRange(doc.getCreatedAt(), startDate, endDate))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getDocumentListAsDTO(Long categoryTypeId, String userRoleName, LocalDate startDate, LocalDate endDate) {
        List<Document> docs = (categoryTypeId != null)
                ? documentRepository.findByCategoryTypeIdAndIsDeletedFalseWithRoles(categoryTypeId)
                : documentRepository.findAllWithRoles();

        Role.RoleName userRole = Role.RoleName.valueOf(userRoleName);
        int userLevel = userRole.getLevel();

        return docs.stream()
                .filter(doc -> userLevel >= doc.getReadRole().getName().getLevel())
                .filter(doc -> isInDateRange(doc.getCreatedAt(), startDate, endDate))
                .map(this::mapToDTO)  // 트랜잭션 범위 안에서 DTO 변환
                .collect(Collectors.toList());
    }

    /**
     * 단일 문서 조회 (ID 기준)
     */
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocument(Long id, int userRoleId) {
        Document doc = documentRepository.findByIdWithRolesAndCategories(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        checkReadPermission(doc, userRoleId);

        return mapToDTO(doc);
    }

    /**
     * 단일 문서 조회 (해시 기준) - 수정됨
     */
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentByHash(String hash, int userRoleId) {
        Document doc = documentRepository.findByFilePathWithRolesAndCategories(hash)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        checkReadPermission(doc, userRoleId);

        return mapToDTO(doc);
    }

    /**
     * 파일 다운로드 (문서 ID 기준) - 저장 경로 개선
     */
    @Transactional(readOnly = true)
    public Resource loadFileAsResource(Long id, int userRoleId) throws MalformedURLException {
        Document doc = documentRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));
        checkReadPermission(doc, userRoleId);

        return loadFileResourceByDocument(doc);
    }

    /**
     * 파일 다운로드 (해시 경로 기준) - 수정됨
     */
    @Transactional(readOnly = true)
    public Resource loadFileAsResourceByHash(String hash, int userRoleId) throws MalformedURLException {
        Document doc = documentRepository.findByFilePathWithRoles(hash)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        checkReadPermission(doc, userRoleId);

        return loadFileResourceByDocument(doc);
    }

    /**
     * 공통 파일 리소스 로드 메서드 (중복 제거)
     */
    private Resource loadFileResourceByDocument(Document doc) throws MalformedURLException {
        String extension = getFileExtension(doc.getFileName());

        // ✅ 설정된 저장 경로에서 파일 로드
        Path uploadDir = fileStorageConfig.getActiveStoragePath();
        Path filePath = uploadDir.resolve(doc.getFilePath() + extension);

        log.info("📂 파일 로드 시도: {}", filePath.toAbsolutePath());

        if (!Files.exists(filePath)) {
            log.error("❌ 파일이 존재하지 않음: {}", filePath.toAbsolutePath());
            throw new RuntimeException("파일이 존재하지 않습니다: " + filePath.toAbsolutePath());
        }

        if (!Files.isReadable(filePath)) {
            log.error("❌ 파일 읽기 권한 없음: {}", filePath.toAbsolutePath());
            throw new RuntimeException("파일을 읽을 수 없습니다: " + filePath.toAbsolutePath());
        }

        log.info("✅ 파일 로드 성공: {}", filePath.toAbsolutePath());
        return new UrlResource(filePath.toUri());
    }

    /**
     * DTO 변환 메서드
     */
    @Transactional(readOnly = true)
    public DocumentResponseDTO mapToDTO(Document doc) {
        // 카테고리 DTO 변환
        List<CategoryTypeDTO> categories = doc.getDocumentCategories().stream()
                .map(dc -> {
                    CategoryType ct = dc.getCategoryType();
                    return CategoryTypeDTO.builder()
                            .id(ct.getId())
                            .name(ct.getName())
                            .description(ct.getDescription())
                            .build();
                })
                .collect(Collectors.toList());

        // Role DTO 변환
        RoleDTO readRole = RoleDTO.builder()
                .id(doc.getReadRole().getId())
                .name(doc.getReadRole().getName().name())
                .description(doc.getReadRole().getDescription())
                .level(doc.getReadRole().getName().getLevel())
                .build();

        return DocumentResponseDTO.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .filePath(doc.getFilePath())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .owner(doc.getAuthor())
                .createdRole(doc.getCreatedRole())
                .createdAt(doc.getCreatedAt())
                .status(doc.getStatus().name())
                .readRole(readRole)
                .categories(categories)
                .build();
    }

    // ========================================
    // ✅ 헬퍼 메서드들 (중복 제거 및 정리)
    // ========================================

    /**
     * Role ID로 Role 엔티티 조회
     */
    private Role getRoleById(Long roleId, String roleType) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException(roleType + " 권한 Role 없음"));
    }

    /**
     * 읽기 권한 체크
     */
    private void checkReadPermission(Document doc, int userRoleId) {
        int requiredLevel = doc.getReadRole().getName().getLevel();
        if (userRoleId < requiredLevel) {
            throw new PermissionDeniedException("권한이 부족합니다.");
        }
    }

    /**
     * 권한 체크 (사용되지 않는 메서드 - 필요시 제거)
     */
    private void checkPermission(Role requiredRole, String userRoleName, String action) {
        Role.RoleName userRoleEnum = Role.RoleName.valueOf(userRoleName);
        int userLevel = userRoleEnum.getLevel();
        int requiredLevel = requiredRole.getName().getLevel();

        if (!userRoleEnum.equals(Role.RoleName.CEO) && userLevel < requiredLevel) {
            throw new PermissionDeniedException(action + " 권한이 없습니다!");
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        return filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf("."))
                : "";
    }

    /**
     * 날짜 범위 체크
     */
    private boolean isInDateRange(LocalDateTime createdAt, LocalDate startDate, LocalDate endDate) {
        if (createdAt == null) return false;
        if (startDate != null && createdAt.toLocalDate().isBefore(startDate)) return false;
        if (endDate != null && createdAt.toLocalDate().isAfter(endDate)) return false;
        return true;
    }
}