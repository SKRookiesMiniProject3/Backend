package com.rookies.log2doc.service;

import com.rookies.log2doc.dto.request.DocumentCreateRequest;
import com.rookies.log2doc.dto.request.DocumentUpdateRequest;
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
import java.util.Map;
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
        Role writeRole = getRoleById(req.getWriteRoleId(), "쓰기");
        Role deleteRole = getRoleById(req.getDeleteRoleId(), "삭제");

        Document doc = new Document();
        doc.setTitle(req.getTitle());
        doc.setContent(req.getContent());
        doc.setReadRole(readRole);
        doc.setWriteRole(writeRole);
        doc.setDeleteRole(deleteRole);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setAuthor(String.valueOf(userId)); // 작성자 ID
        doc.setCreatedRole(userRoleName);      // 작성자 권한

        documentRepository.save(doc);

        // CategoryType FK 연결
        CategoryType categoryType = categoryTypeRepository.findById(req.getCategoryTypeId())
                .orElseThrow(() -> new RuntimeException("카테고리 타입 없음"));

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
            Long categoryTypeId,  // 문자열 대신 ID
            Long readRoleId, Long writeRoleId, Long deleteRoleId,
            Long userId, String userRoleName
    ) throws IOException {

        Role readRole = getRoleById(readRoleId, "읽기");
        Role writeRole = getRoleById(writeRoleId, "쓰기");
        Role deleteRole = getRoleById(deleteRoleId, "삭제");

        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString();
        String storedFileName = uuid + extension;

        // uploads 디렉토리 생성 후 저장
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
        doc.setWriteRole(writeRole);
        doc.setDeleteRole(deleteRole);
        doc.setAuthor(String.valueOf(userId));
        doc.setCreatedRole(userRoleName);

        documentRepository.save(doc);

        // CategoryType FK 연결
        CategoryType categoryType = categoryTypeRepository.findById(categoryTypeId)
                .orElseThrow(() -> new RuntimeException("카테고리 타입 없음"));

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
    public List<Document> getDocumentList(
            Long categoryTypeId,  // ✅ 문자열 대신 ID
            String userRoleName,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Document> docs;

        if (categoryTypeId != null) {
            docs = documentRepository.findByCategoryTypeIdAndIsDeletedFalseWithRoles(categoryTypeId);
        } else {
            docs = documentRepository.findAllWithRoles();
        }

        Role.RoleName userRole = Role.RoleName.valueOf(userRoleName);
        int userLevel = userRole.getLevel();

        return docs.stream()
                .filter(doc -> userLevel >= doc.getReadRole().getName().getLevel())
                .filter(doc -> isInDateRange(doc.getCreatedAt(), startDate, endDate))
                .collect(Collectors.toList());
    }

    /**
     * 단일 문서 조회 (읽기 권한)
     */
    @Transactional(readOnly = true)
    public Document getDocument(Long id, String userRoleName) {
        Document doc = documentRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));
        checkReadPermission(doc, userRoleName);
        return doc;
    }

    /**
     * 파일 다운로드 (문서 ID 기준)
     */
    @Transactional(readOnly = true)
    public Resource loadFileAsResource(Long id, String userRoleName) throws MalformedURLException {
        Document doc = documentRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));
        checkReadPermission(doc, userRoleName);

        String extension = getFileExtension(doc.getFileName());
        Path filePath = Paths.get("uploads").resolve(doc.getFilePath() + extension);
        return new UrlResource(filePath.toUri());
    }

    /**
     * 파일 다운로드 (해시 경로 기준)
     */
    @Transactional(readOnly = true)
    public Resource loadFileAsResourceByHash(String hash, String userRoleName) throws MalformedURLException {
        Document doc = documentRepository.findByFilePathWithRoles(hash)
                .orElseThrow(() -> new RuntimeException("파일 없음"));

        checkReadPermission(doc, userRoleName);

        String extension = getFileExtension(doc.getFileName());
        Path path = Paths.get("uploads").resolve(hash + extension);
        return new UrlResource(path.toUri());
    }

    public Document getDocumentByHash(String hash) {
        return documentRepository.findByFilePath(hash)
                .orElseThrow(() -> new RuntimeException("문서 없음"));
    }

    /**
     * 문서 전체 수정 (쓰기 권한)
     */
    @Transactional
    public Document updateDocument(Long id, DocumentUpdateRequest req, String userRoleName) {
        Document doc = documentRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new RuntimeException("문서 없음"));
        checkWritePermission(doc, userRoleName);

        doc.setTitle(req.getTitle());
        doc.setContent(req.getContent());
        // ✅ 카테고리는 DocumentCategory로 따로 관리하므로 여기선 변경 X

        return documentRepository.save(doc);
    }

    /**
     * 문서 일부 수정 (쓰기 권한)
     */
    @Transactional
    public Document patchDocument(Long id, Map<String, Object> updates, String userRoleName) {
        Document doc = documentRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new RuntimeException("문서 없음"));
        checkWritePermission(doc, userRoleName);

        if (updates.containsKey("title")) {
            doc.setTitle((String) updates.get("title"));
        }
        if (updates.containsKey("content")) {
            doc.setContent((String) updates.get("content"));
        }
        // ✅ categoryType 변경이 필요하다면 별도 로직에서 처리
        return documentRepository.save(doc);
    }

    /**
     * 문서 소프트 삭제
     */
    @Transactional
    public void softDeleteDocument(Long id, String userRoleName) {
        Document doc = documentRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));
        checkDeletePermission(doc, userRoleName);

        doc.setDeleted(true);
        doc.setDeletedAt(LocalDateTime.now());
        documentRepository.save(doc);
    }

    /**
     * 문서 하드 삭제
     */
    @Transactional
    public void hardDeleteDocument(Long id, String userRoleName) {
        Document doc = documentRepository.findByIdWithRolesIgnoreIsDeleted(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));
        checkDeletePermission(doc, userRoleName);

        documentRepository.delete(doc);
    }

    /**
     * 공통 권한 로직
     */
    private Role getRoleById(Long roleId, String roleType) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException(roleType + " 권한 Role 없음"));
    }

    private void checkReadPermission(Document doc, String userRoleName) {
        checkPermission(doc.getReadRole(), userRoleName, "읽기");
    }

    private void checkWritePermission(Document doc, String userRoleName) {
        checkPermission(doc.getWriteRole(), userRoleName, "쓰기");
    }

    private void checkDeletePermission(Document doc, String userRoleName) {
        checkPermission(doc.getDeleteRole(), userRoleName, "삭제");
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
}
