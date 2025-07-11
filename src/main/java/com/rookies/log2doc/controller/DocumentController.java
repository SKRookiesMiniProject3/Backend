package com.rookies.log2doc.controller;

import com.rookies.log2doc.dto.request.DocumentCreateRequest;
import com.rookies.log2doc.dto.response.DocumentResponseDTO;
import com.rookies.log2doc.entity.Document;
import com.rookies.log2doc.entity.Role;
import com.rookies.log2doc.exception.PermissionDeniedException;
import com.rookies.log2doc.repository.RoleRepository;
import com.rookies.log2doc.security.services.UserDetailsImpl;
import com.rookies.log2doc.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private RoleRepository roleRepository;

    /**
     * 파일 업로드
     */

    @PostMapping("/upload")

    public ResponseEntity<Document> uploadDocument(
            @Valid @ModelAttribute DocumentCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest servletRequest
    ) throws IOException {


        // DTO에서 데이터 추출
        MultipartFile file = request.getFile();
        String title = request.getTitle();
        String content = request.getContent();
        Long categoryTypeId = request.getCategoryTypeId();
        Long readRoleId = request.getReadRoleId();

        // 현재 사용자 직급 레벨 가져오기
        int userLevel = userDetails.getRoleId();

        // 업로드 대상 읽기 권한 Role 불러오기
        Role readRole = roleRepository.findById(readRoleId)
                .orElseThrow(() -> new RuntimeException("권한 정보가 없습니다."));

        int targetRoleLevel = readRole.getName().getLevel();

        // 직급 비교: 내 레벨보다 높은 직급이면 차단!
        if (targetRoleLevel > userLevel) {
            throw new PermissionDeniedException("내 직급보다 높은 접근 권한은 설정할 수 없습니다!");
        }

        // 통과 시 서비스 호출
        Document saved = documentService.uploadDocument(
                file,
                title,
                content,
                categoryTypeId,
                readRoleId,
                userDetails.getId(),
                userDetails.getRoleName()
        );

        // Request Attribute 설정 (Interceptor에서 사용)
        servletRequest.setAttribute("document_id", saved.getId());
        servletRequest.setAttribute("document_owner", saved.getAuthor());

        return ResponseEntity.ok(saved);
    }

    /**
     * 문서 리스트 조회
     * - 카테고리 및 기간(startDate ~ endDate) 필터링 가능
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getDocuments(
            @RequestParam(required = false) Long categoryTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request
    ) {
        // Service에서 DTO까지 변환해서 반환
        List<DocumentResponseDTO> result = documentService.getDocumentListAsDTO(
                categoryTypeId,
                userDetails.getRoleName(),
                startDate,
                endDate
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 단일 문서 조회 (해시 경로 기준)
     * - 권한 체크 포함 + DTO 변환
     */
    @GetMapping("/files/{hash}")
    public ResponseEntity<DocumentResponseDTO> getDocumentByHash(
            @PathVariable String hash,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request
    ) {
        DocumentResponseDTO doc = documentService.getDocumentByHash(hash, userDetails.getRoleId());

        // Request Attribute 설정
        request.setAttribute("document_id", doc.getId());
        request.setAttribute("document_owner", doc.getOwner());
        request.setAttribute("document_hash", hash);

        return ResponseEntity.ok(doc);
    }

    /**
     * 단일 문서 조회 (ID 기준)
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request
    ) {
        // Request Attribute 설정
        request.setAttribute("document_id", id);

        DocumentResponseDTO doc = documentService.getDocument(id, userDetails.getRoleId());

        // 추가 정보 설정
        request.setAttribute("document_owner", doc.getOwner());

        return ResponseEntity.ok(doc);
    }

    /**
     * 파일 다운로드 (문서 ID 기준)
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request
    ) throws MalformedURLException {

        Resource fileResource = documentService.loadFileAsResource(id, userDetails.getRoleId());
        DocumentResponseDTO doc = documentService.getDocument(id, userDetails.getRoleId());

        // Request Attribute 설정
        request.setAttribute("document_id", doc.getId());
        request.setAttribute("document_owner", doc.getOwner());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource);
    }

    /**
     * 문서 상태 조회
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getDocumentStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request
    ) {
        // Request Attribute 설정
        request.setAttribute("document_id", id);

        // 권한 체크 포함 단일 조회
        DocumentResponseDTO doc = documentService.getDocument(id, userDetails.getRoleId());

        // 추가 정보 설정
        request.setAttribute("document_owner", doc.getOwner());

        Map<String, Object> result = Map.of(
                "documentId", doc.getId(),
                "status", doc.getStatus()
        );

        return ResponseEntity.ok(result);
    }
}