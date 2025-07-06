package com.rookies.log2doc.controller;

import com.rookies.log2doc.dto.request.DocumentCreateRequest;
import com.rookies.log2doc.dto.request.DocumentUpdateRequest;
import com.rookies.log2doc.entity.Document;
import com.rookies.log2doc.security.services.UserDetailsImpl;
import com.rookies.log2doc.service.DocumentService;
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

    /**
     * 텍스트 문서 생성
     */
    @PostMapping
    public ResponseEntity<Document> createDocument(@RequestBody DocumentCreateRequest req) {
        Document created = documentService.createTextDocument(req);
        return ResponseEntity.ok(created);
    }

    /**
     * 파일 업로드 후 문서 생성
     */
    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam MultipartFile file,
            @RequestParam String category,
            @RequestParam Long readRoleId,
            @RequestParam Long writeRoleId,
            @RequestParam Long deleteRoleId
    ) throws IOException {
        Document saved = documentService.uploadDocument(file, category, readRoleId, writeRoleId, deleteRoleId);
        return ResponseEntity.ok(saved);
    }

    /**
     * 문서 리스트 조회
     * - 카테고리 및 기간(startDate ~ endDate) 필터링 가능
     */
    @GetMapping
    public ResponseEntity<List<Document>> getDocuments(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<Document> result = documentService.getDocumentList(
                category,
                userDetails.getRoleName(),
                startDate,
                endDate
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 단일 문서 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Document doc = documentService.getDocument(id, userDetails.getRoleName());
        return ResponseEntity.ok(doc);
    }

    /**
     * 파일 다운로드 (문서 ID 기준)
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) throws MalformedURLException {
        Resource fileResource = documentService.loadFileAsResource(id, userDetails.getRoleName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource);
    }

    /**
     * 파일 다운로드 (해시 경로 기준)
     */
    @GetMapping("/files/{hash}")
    public ResponseEntity<Resource> downloadByHash(
            @PathVariable String hash,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) throws MalformedURLException {
        Resource fileResource = documentService.loadFileAsResourceByHash(hash, userDetails.getRoleName());
        Document doc = documentService.getDocumentByHash(hash);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(fileResource);
    }

    /**
     * 문서 전체 수정 (PUT)
     */
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentUpdateRequest updateRequest,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Document updated = documentService.updateDocument(id, updateRequest, userDetails.getRoleName());
        return ResponseEntity.ok(updated);
    }

    /**
     * 문서 일부 수정 (PATCH)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Document> patchDocument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Document patched = documentService.patchDocument(id, updates, userDetails.getRoleName());
        return ResponseEntity.ok(patched);
    }

    /**
     * 문서 소프트 삭제 (isDeleted = true)
     */
    @DeleteMapping("/{id}/soft")
    public ResponseEntity<Void> softDeleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        documentService.softDeleteDocument(id, userDetails.getRoleName());
        return ResponseEntity.ok().build();
    }

    /**
     * 문서 완전 삭제 (하드)
     */
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        documentService.hardDeleteDocument(id, userDetails.getRoleName());
        return ResponseEntity.ok().build();
    }

}