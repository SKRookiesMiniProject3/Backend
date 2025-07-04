package com.rookies.log2doc.service;

import com.rookies.log2doc.dto.request.DocumentUpdateRequest;
import com.rookies.log2doc.entity.Document;
import com.rookies.log2doc.entity.Role;
import com.rookies.log2doc.exception.PermissionDeniedException;
import com.rookies.log2doc.repository.DocumentRepository;
import com.rookies.log2doc.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private RoleRepository roleRepository;

    public Document createTextDocument(Document document) {
        document.setCreatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    public List<Document> getDocumentList(String category) {
        if (category != null && !category.isEmpty()) {
            return documentRepository.findByCategoryAndIsDeletedFalse(category);
        } else {
            return documentRepository.findByIsDeletedFalse();
        }
    }

    public Document getDocument(Long id, String userRoleName) {
        Document doc = documentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        // ✅ 삭제 여부 확인
        if (doc.isDeleted()) {
            log.warn("삭제된 문서입니다. [id={}]", id);  // 콘솔에 남김
            throw new RuntimeException("삭제된 문서입니다.");
        }

        Role.RoleName userRole = Role.RoleName.valueOf(userRoleName);
        int userLevel = userRole.getLevel();
        int requiredLevel = doc.getReadRole().getName().getLevel();

        if (userLevel < requiredLevel) {
            throw new PermissionDeniedException("읽기 권한이 없습니다!");
        }

        return doc;
    }

    public Document uploadDocument(MultipartFile file, String category,
                                   Long readRoleId, Long writeRoleId, Long deleteRoleId) throws IOException {

        // ✅ FK Role 조회
        Role readRole = roleRepository.findById(readRoleId)
                .orElseThrow(() -> new RuntimeException("읽기 권한 Role 없음"));
        Role writeRole = roleRepository.findById(writeRoleId)
                .orElseThrow(() -> new RuntimeException("쓰기 권한 Role 없음"));
        Role deleteRole = roleRepository.findById(deleteRoleId)
                .orElseThrow(() -> new RuntimeException("삭제 권한 Role 없음"));

        // ✅ 파일 처리
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String hashedFileName = UUID.randomUUID() + extension;

        Path uploadDir = Paths.get("uploads");
        Files.createDirectories(uploadDir);

        Path filePath = uploadDir.resolve(hashedFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // ✅ Document 저장
        Document doc = new Document();
        doc.setFileName(originalFileName);
        doc.setFilePath(filePath.toString());
        doc.setMimeType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setCategory(category);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setReadRole(readRole);      // FK 연결!
        doc.setWriteRole(writeRole);
        doc.setDeleteRole(deleteRole);

        return documentRepository.save(doc);
    }

    public Resource loadFileAsResource(Long id, String userRoleName) throws MalformedURLException {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        Role.RoleName userRole = Role.RoleName.valueOf(userRoleName);

        int userLevel = userRole.getLevel();
        int requiredLevel = doc.getReadRole().getName().getLevel();

        if (userLevel < requiredLevel) {
            throw new PermissionDeniedException("읽기 권한이 없습니다!");
        }

        Path filePath = Paths.get(doc.getFilePath());
        return new UrlResource(filePath.toUri());
    }

    // 전체 교체
    public Document updateDocument(Long id, DocumentUpdateRequest req, String userRoleName) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문서 없음"));

        // 권한 체크
        checkWritePermission(doc, userRoleName);

        doc.setTitle(req.getTitle());
        doc.setCategory(req.getCategory());
        doc.setContent(req.getContent());
        // 필요한 필드 다 덮어씀

        return documentRepository.save(doc);
    }

    // 일부 변경
    public Document patchDocument(Long id, Map<String, Object> updates, String userRoleName) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문서 없음"));

        checkWritePermission(doc, userRoleName);

        if (updates.containsKey("title")) {
            doc.setTitle((String) updates.get("title"));
        }
        if (updates.containsKey("category")) {
            doc.setCategory((String) updates.get("category"));
        }
        // 필드별로 조건 처리

        return documentRepository.save(doc);
    }

    // 권한 체크
    private void checkWritePermission(Document doc, String userRoleName) {
        int userLevel = Role.RoleName.valueOf(userRoleName).getLevel();
        int requiredLevel = doc.getWriteRole().getName().getLevel();
        if (userLevel < requiredLevel) {
            throw new PermissionDeniedException("쓰기 권한 없음");
        }
    }

    public void softDeleteDocument(Long id, String userRole) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        Role.RoleName userRoleEnum = Role.RoleName.valueOf(userRole);
        int userLevel = userRoleEnum.getLevel();
        int requiredLevel = doc.getDeleteRole().getName().getLevel();

        if (userLevel < requiredLevel) {
            throw new PermissionDeniedException("삭제 권한이 없습니다!");
        }

        doc.setDeleted(true);
        doc.setDeletedAt(LocalDateTime.now());
        documentRepository.save(doc);
    }

    public void hardDeleteDocument(Long id, String userRole) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        Role.RoleName userRoleEnum = Role.RoleName.valueOf(userRole);
        int userLevel = userRoleEnum.getLevel();
        int requiredLevel = doc.getDeleteRole().getName().getLevel();

        if (userLevel < requiredLevel) {
            throw new PermissionDeniedException("삭제 권한이 없습니다!");
        }

        documentRepository.delete(doc);
    }

}
