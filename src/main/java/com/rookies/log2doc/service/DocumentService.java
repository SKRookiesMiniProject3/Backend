package com.rookies.log2doc.service;

import com.rookies.log2doc.dto.request.DocumentCreateRequest;
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
import java.nio.file.Paths;
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
    private final CategoryTypeRepository categoryTypeRepository;        // 추가
    private final DocumentCategoryRepository documentCategoryRepository; // 추가

    /**
     * 텍스트 문서 생성
     */
    @Transactional
    public Document createTextDocument(DocumentCreateRequest req, Long userId, String userRoleName) {
        Role readRole = getRoleById(req.getReadRoleId(), "읽기");

        Document doc = new Document();
        doc.setTitle(req.getTitle());
        doc.setContent(req.getContent());
        doc.setReadRole(readRole);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setAuthor(String.valueOf(userId));
        doc.setCreatedRole(userRoleName);         // 작성자 저장

        // ✅ FK로 CategoryType 가져오기
        CategoryType categoryType = categoryTypeRepository.findById(req.getCategoryTypeId())
                .orElseThrow(() -> new RuntimeException("카테고리 타입 없음"));

        // ✅ Document 저장
        documentRepository.save(doc);

        // ✅ DocumentCategory 매핑
        DocumentCategory mapping = new DocumentCategory();
        mapping.setDocument(doc);
        mapping.setCategoryType(categoryType);
        documentCategoryRepository.save(mapping);

        return doc;
    }

    /**
     * 파일 업로드 후 문서 생성
     */
    @Transactional
    public Document uploadDocument(
            MultipartFile file,
            Long categoryTypeId,
            Long readRoleId,
            Long userId, String userRoleName
    ) throws IOException {

        Role readRole = getRoleById(readRoleId, "읽기");

        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString();
        String storedFileName = uuid + extension;

        Path uploadDir = Paths.get("uploads");
        Files.createDirectories(uploadDir);
        Path savePath = uploadDir.resolve(storedFileName);
        Files.copy(file.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

        Document doc = new Document();
        doc.setFileName(originalFileName);
        doc.setFilePath(uuid);
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setReadRole(readRole);
        doc.setAuthor(String.valueOf(userId));
        doc.setCreatedRole(userRoleName);

        // ✅ FK 가져오기
        CategoryType categoryType = categoryTypeRepository.findById(categoryTypeId)
                .orElseThrow(() -> new RuntimeException("카테고리 타입 없음"));

        documentRepository.save(doc);

        DocumentCategory mapping = new DocumentCategory();
        mapping.setDocument(doc);
        mapping.setCategoryType(categoryType);
        documentCategoryRepository.save(mapping);

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
     * 단일 문서 조회 (읽기 권한)
     */
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocument(Long id, int userRoleId) {
        Document doc = documentRepository.findByIdWithRolesAndCategories(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        checkReadPermission(doc, userRoleId);

        return mapToDTO(doc); // ⭐️ 이게 핵심!
    }

    /**
     * 파일 다운로드 (문서 ID 기준)
     */
    @Transactional(readOnly = true)
    public Resource loadFileAsResource(Long id, int userRoleId) throws MalformedURLException {
        Document doc = documentRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));
        checkReadPermission(doc, userRoleId);

        String extension = getFileExtension(doc.getFileName());

        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
        Path filePath = uploadDir.resolve(doc.getFilePath() + extension);

        if (!Files.exists(filePath)) {
            throw new RuntimeException("파일이 존재하지 않습니다: " + filePath.toAbsolutePath());
        }

        return new UrlResource(filePath.toUri());
    }

    /**
     * 파일 다운로드 (해시 경로 기준)
     */
    @Transactional(readOnly = true)
    public Resource loadFileAsResourceByHash(String hash, int userRoleId) throws MalformedURLException {
        Document doc = documentRepository.findByFilePathWithRoles(hash)
                .orElseThrow(() -> new RuntimeException("파일 없음"));

        checkReadPermission(doc, userRoleId);  // int로 전달

        String extension = getFileExtension(doc.getFileName());
        Path path = Paths.get("uploads").resolve(hash + extension);
        return new UrlResource(path.toUri());
    }

    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentByHash(String hash, int userRoleId) {
        Document doc = documentRepository.findByFilePathWithRolesAndCategories(hash)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        checkReadPermission(doc, userRoleId);

        return mapToDTO(doc);
    }

    /**
     * 공통 권한 로직
     */
    private Role getRoleById(Long roleId, String roleType) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException(roleType + " 권한 Role 없음"));
    }

    private void checkReadPermission(Document doc, int userRoleId) {
        int requiredLevel = doc.getReadRole().getName().getLevel();
        if (userRoleId < requiredLevel) {
            throw new PermissionDeniedException("권한이 부족합니다.");
        }
    }

    private void checkPermission(Role requiredRole, String userRoleName, String action) {
        Role.RoleName userRoleEnum = Role.RoleName.valueOf(userRoleName);
        int userLevel = userRoleEnum.getLevel();
        int requiredLevel = requiredRole.getName().getLevel();

        if (!userRoleEnum.equals(Role.RoleName.CEO) && userLevel < requiredLevel) {
            throw new PermissionDeniedException(action + " 권한이 없습니다!");
        }
    }

    private String getFileExtension(String filename) {
        return filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf("."))
                : "";
    }

    private boolean isInDateRange(LocalDateTime createdAt, LocalDate startDate, LocalDate endDate) {
        if (createdAt == null) return false;
        if (startDate != null && createdAt.toLocalDate().isBefore(startDate)) return false;
        if (endDate != null && createdAt.toLocalDate().isAfter(endDate)) return false;
        return true;
    }

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

}
