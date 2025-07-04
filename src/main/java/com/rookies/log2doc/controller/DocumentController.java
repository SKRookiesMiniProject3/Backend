package com.rookies.log2doc.controller;

import com.rookies.log2doc.entity.Document;
import com.rookies.log2doc.entity.Role;
import com.rookies.log2doc.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Document> createDocument(@RequestBody Document document) {
        return ResponseEntity.ok(documentService.createTextDocument(document));
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

    @GetMapping
    public ResponseEntity<List<Document>> getDocuments(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(documentService.getDocumentList(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(
            @PathVariable Long id,
            @RequestParam String userRoleName) {
        Document doc = documentService.getDocument(id, userRoleName);
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @RequestParam String userRoleName) throws MalformedURLException {
        Resource fileResource = documentService.loadFileAsResource(id, userRoleName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource);
    }

    // 문서 전체 업데이트 (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentUpdateRequest updateRequest,
            @RequestParam String userRoleName
    ) {
        Document updated = documentService.updateDocument(id, updateRequest, userRoleName);
        return ResponseEntity.ok(updated);
    }

    // 문서 일부 업데이트 (PATCH)
    @PatchMapping("/{id}")
    public ResponseEntity<Document> patchDocument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            @RequestParam String userRoleName
    ) {
        Document patched = documentService.patchDocument(id, updates, userRoleName);
        return ResponseEntity.ok(patched);
    }

    @DeleteMapping("/{id}/soft")
    public ResponseEntity<Void> softDeleteDocument(
            @PathVariable Long id,
            @RequestParam String userRole) {
        documentService.softDeleteDocument(id, userRole);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteDocument(
            @PathVariable Long id,
            @RequestParam String userRole) {
        documentService.hardDeleteDocument(id, userRole);
        return ResponseEntity.ok().build();
    }


}
