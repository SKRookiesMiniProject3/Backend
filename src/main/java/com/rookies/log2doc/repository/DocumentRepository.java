package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Document 엔티티 JPA Repository
 * - 권한(Role) Fetch Join 포함 조회 지원
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 파일 해시(filePath)로 단일 문서 조회
     */
    Optional<Document> findByFilePath(String filePath);

    @Query("""
            SELECT DISTINCT d FROM Document d
            JOIN d.documentCategories dc
            JOIN dc.categoryType ct
            JOIN FETCH d.readRole
            WHERE ct.id = :categoryTypeId
            """)
    List<Document> findByCategoryTypeIdAndIsDeletedFalseWithRoles(@Param("categoryTypeId") Long categoryTypeId);


    /**
     * 전체 문서 리스트 조회 + 권한 Role Fetch Join
     */
    @Query("""
                SELECT d FROM Document d
                JOIN FETCH d.readRole
            """)
    List<Document> findAllWithRoles();

    /**
     * ID로 단일 문서 조회 + 권한 Role + 카테고리 Fetch Join (Soft Delete 제외)
     */
    @Query("""
                SELECT DISTINCT d FROM Document d
                JOIN FETCH d.readRole
                JOIN FETCH d.documentCategories dc
                JOIN FETCH dc.categoryType
                WHERE d.id = :id
            """)
    Optional<Document> findByIdWithRolesAndCategories(@Param("id") Long id);

    /**
     * ID로 단일 문서 조회 + 권한 Role Fetch Join (Soft Delete 제외)
     */
    @Query("""
                SELECT d FROM Document d
                JOIN FETCH d.readRole
                WHERE d.id = :id
            """)
    Optional<Document> findByIdWithRoles(@Param("id") Long id);

    /**
     * 파일 해시(filePath)로 단일 문서 조회 + 권한 Role Fetch Join
     * - 파일 다운로드(해시) 시 사용
     */
    @Query("""
                SELECT d FROM Document d
                JOIN FETCH d.readRole
                WHERE d.filePath = :filePath
            """)
    Optional<Document> findByFilePathWithRoles(@Param("filePath") String filePath);

}
