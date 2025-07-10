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

/**
 * DocumentService
 * - 문서 업로드, 조회, 다운로드 등 파일 및 메타데이터 관리
 * - 권한 체크와 카테고리 매핑 포함
 */
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
     * 파일 업로드 후 문서 엔티티 생성
     * - 파일을 UUID+확장자로 저장
     * - DB에는 원본 파일명, UUID(해시), 실제 경로 저장
     * - 문서 카테고리와 권한 매핑
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

        // 읽기 권한 Role 조회
        Role readRole = getRoleById(readRoleId, "읽기");

        // 파일명과 UUID 해시 생성
        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString();
        String storedFileName = uuid + extension;

        // 저장 경로 결정
        Path uploadDir = fileStorageConfig.getActiveStoragePath();
        Path savePath = uploadDir.resolve(storedFileName);

        log.info("저장 경로: {}, 저장 파일명: {}", uploadDir.toAbsolutePath(), storedFileName);

        // 실제 파일 복사
        try {
            Files.copy(file.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 저장 완료: {}", savePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", savePath.toAbsolutePath(), e);
            throw new RuntimeException("파일 저장 실패: " + e.getMessage());
        }

        // 문서 엔티티 저장
        Document doc = new Document();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setFileName(originalFileName);
        doc.setFilePath(uuid);                  // 해시
        doc.setFilePathNfs(savePath.toString()); // 실제 경로
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setReadRole(readRole);
        doc.setAuthor(String.valueOf(userId));
        doc.setCreatedRole(userRoleName);

        // 카테고리 FK
        CategoryType categoryType = categoryTypeRepository.findById(categoryTypeId)
                .orElseThrow(() -> new RuntimeException("카테고리 타입 없음"));

        documentRepository.save(doc);

        // 카테고리 매핑
        DocumentCategory mapping = new DocumentCategory();
        mapping.setDocument(doc);
        mapping.setCategoryType(categoryType);
        documentCategoryRepository.save(mapping);

        log.info("문서 저장 완료 - ID: {}, 파일명: {}", doc.getId(), originalFileName);
        return doc;
    }

    /**
     * 문서 목록 조회
     * - 카테고리, 날짜, 권한 필터링 포함
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

    /**
     * 문서 목록 DTO 반환
     */
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
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 단일 문서 조회 (ID)
     */
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocument(Long id, int userRoleId) {
        Document doc = documentRepository.findByIdWithRolesAndCategories(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));
        checkReadPermission(doc, userRoleId);
        return mapToDTO(doc);
    }

    /**
     * 단일 문서 조회 (해시)
     */
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentByHash(String hash, int userRoleId) {
        Document doc = documentRepository.findByFilePathWithRolesAndCategories(hash)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));
        checkReadPermission(doc, userRoleId);
        return mapToDTO(doc);
    }

    /**
     * 파일 다운로드 (ID)
     */
    @Transactional(readOnly = true)
    public Resource loadFileAsResource(Long id, int userRoleId) throws MalformedURLException {
        Document doc = documentRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));
        checkReadPermission(doc, userRoleId);
        return loadFileResourceByDocument(doc);
    }

    /**
     * 파일 다운로드 (해시)
     */
    @Transactional(readOnly = true)
    public Resource loadFileAsResourceByHash(String hash, int userRoleId) throws MalformedURLException {
        Document doc = documentRepository.findByFilePathWithRoles(hash)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        checkReadPermission(doc, userRoleId);
        return loadFileResourceByDocument(doc);
    }

    /**
     * 실제 파일 리소스 로드 공통 처리
     */
    private Resource loadFileResourceByDocument(Document doc) throws MalformedURLException {
        String extension = getFileExtension(doc.getFileName());
        Path uploadDir = fileStorageConfig.getActiveStoragePath();
        Path filePath = uploadDir.resolve(doc.getFilePath() + extension);

        if (!Files.exists(filePath)) {
            throw new RuntimeException("파일이 존재하지 않습니다: " + filePath);
        }
        if (!Files.isReadable(filePath)) {
            throw new RuntimeException("파일을 읽을 수 없습니다: " + filePath);
        }

        return new UrlResource(filePath.toUri());
    }

    /**
     * Document → DTO 변환
     */
    @Transactional(readOnly = true)
    public DocumentResponseDTO mapToDTO(Document doc) {
        List<CategoryTypeDTO> categories = doc.getDocumentCategories().stream()
                .map(dc -> {
                    CategoryType ct = dc.getCategoryType();
                    return CategoryTypeDTO.builder()
                            .id(ct.getId())
                            .name(ct.getName())
                            .description(ct.getDescription())
                            .build();
                }).collect(Collectors.toList());

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

    // ===================================
    // 내부 유틸리티 메서드
    // ===================================

    /** Role 조회 */
    private Role getRoleById(Long roleId, String roleType) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException(roleType + " 권한 Role 없음"));
    }

    /** 읽기 권한 체크 */
    private void checkReadPermission(Document doc, int userRoleId) {
        int requiredLevel = doc.getReadRole().getName().getLevel();
        if (userRoleId < requiredLevel) {
            throw new PermissionDeniedException("권한이 부족합니다.");
        }
    }

    /** 파일 확장자 추출 */
    private String getFileExtension(String filename) {
        return filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf("."))
                : "";
    }

    /** 생성일이 지정한 날짜 범위에 포함되는지 여부 */
    private boolean isInDateRange(LocalDateTime createdAt, LocalDate startDate, LocalDate endDate) {
        if (createdAt == null) return false;
        if (startDate != null && createdAt.toLocalDate().isBefore(startDate)) return false;
        if (endDate != null && createdAt.toLocalDate().isAfter(endDate)) return false;
        return true;
    }
}
