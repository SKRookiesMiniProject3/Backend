package com.rookies.log2doc.controller;

import com.rookies.log2doc.dto.request.DocumentCreateRequest;
import com.rookies.log2doc.dto.request.DocumentUpdateRequest;
import com.rookies.log2doc.entity.Document;
import com.rookies.log2doc.entity.Role;
import com.rookies.log2doc.security.services.UserDetailsImpl;
import com.rookies.log2doc.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public ResponseEntity<Document> createDocument(@RequestBody DocumentCreateRequest req) {
        return ResponseEntity.ok(documentService.createTextDocument(req));
    }

    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam MultipartFile file,
            @RequestParam String category,
            @RequestParam Long readRoleId,
            @RequestParam Long writeRoleId,
            @RequestParam Long deleteRoleId
    ) throws IOException {
        Document savedDoc = documentService.uploadDocument(
                file,
                category,
                readRoleId,
                writeRoleId,
                deleteRoleId
        );
        return ResponseEntity.ok(savedDoc);
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<Document>> getDocuments(
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(
                documentService.getDocumentList(category, userDetails.getRoleName())
        );
    }

    // 단일 조회
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Document doc = documentService.getDocument(id, userDetails.getRoleName());
        return ResponseEntity.ok(doc);
    }

    // 문서 파일 다운로드
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) throws MalformedURLException {
        Resource fileResource = documentService.loadFileAsResource(id, userDetails.getRoleName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource);
    }

    // 문서 파일 해시 경로
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

    // 문서 전체 업데이트 (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentUpdateRequest updateRequest,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Document updated = documentService.updateDocument(id, updateRequest, userDetails.getRoleName());
        return ResponseEntity.ok(updated);
    }

    // 문서 일부 업데이트 (PATCH)
    @PatchMapping("/{id}")
    public ResponseEntity<Document> patchDocument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Document patched = documentService.patchDocument(id, updates, userDetails.getRoleName());
        return ResponseEntity.ok(patched);
    }

    // 문서 소프트 삭제
    @DeleteMapping("/{id}/soft")
    public ResponseEntity<Void> softDelete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        documentService.softDeleteDocument(id, userDetails.getRoleName());
        return ResponseEntity.ok().build();
    }

    // 문서 하드 삭제
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        documentService.hardDeleteDocument(id, userDetails.getRoleName());
        return ResponseEntity.ok().build();
    }

}
