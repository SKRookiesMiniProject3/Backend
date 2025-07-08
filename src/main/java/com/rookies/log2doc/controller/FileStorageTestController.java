package com.rookies.log2doc.controller;

import com.rookies.log2doc.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class FileStorageTestController {

    private final FileStorageConfig fileStorageConfig;

    @GetMapping("/storage-info")
    public ResponseEntity<Map<String, Object>> getStorageInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("activeStoragePath", fileStorageConfig.getActiveStoragePath().toAbsolutePath().toString());
        info.put("storageInfo", fileStorageConfig.getStorageInfo());
        info.put("localEnabled", fileStorageConfig.isEnabledLocal());
        info.put("nfsEnabled", fileStorageConfig.isEnabledNfs());
        
        // 디렉토리 존재 및 쓰기 권한 확인
        Path activePath = fileStorageConfig.getActiveStoragePath();
        info.put("directoryExists", Files.exists(activePath));
        info.put("directoryWritable", Files.isWritable(activePath));
        
        return ResponseEntity.ok(info);
    }
}