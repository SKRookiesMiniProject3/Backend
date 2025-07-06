package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Document 엔티티 JPA Repository
 * - Soft Delete 제외 기본 조회
 * - 권한(Role) Fetch Join 포함 조회 지원
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 카테고리별 문서 리스트 조회 (Soft Delete 제외)
     */
    List<Document> findByCategoryAndIsDeletedFalse(String category);

    /**
     * 삭제되지 않은 모든 문서 리스트 조회
     */
    List<Document> findByIsDeletedFalse();

    /**
     * 파일 해시(filePath)로 단일 문서 조회
     */
    Optional<Document> findByFilePath(String filePath);

    /**
     * 카테고리별 문서 리스트 조회 + 권한 Role Fetch Join (Soft Delete 제외)
     */
    @Query("""
        SELECT d FROM Document d
        JOIN FETCH d.readRole
        JOIN FETCH d.writeRole
        JOIN FETCH d.deleteRole
        WHERE d.isDeleted = false AND d.category = :category
    """)
    List<Document> findByCategoryAndIsDeletedFalseWithRoles(@Param("category") String category);

    /**
     * 전체 문서 리스트 조회 + 권한 Role Fetch Join (Soft Delete 제외)
     */
    @Query("""
        SELECT d FROM Document d
        JOIN FETCH d.readRole
        JOIN FETCH d.writeRole
        JOIN FETCH d.deleteRole
        WHERE d.isDeleted = false
    """)
    List<Document> findAllWithRoles();

    /**
     * ID로 단일 문서 조회 + 권한 Role Fetch Join (Soft Delete 제외)
     */
    @Query("""
        SELECT d FROM Document d
        JOIN FETCH d.readRole
        JOIN FETCH d.writeRole
        JOIN FETCH d.deleteRole
        WHERE d.id = :id AND d.isDeleted = false
    """)
    Optional<Document> findByIdWithRoles(@Param("id") Long id);

    /**
     * ID로 단일 문서 조회 + 권한 Role Fetch Join (Soft Delete 무시)
     * - 하드 삭제 시 사용
     */
    @Query("""
        SELECT d FROM Document d
        JOIN FETCH d.readRole
        JOIN FETCH d.writeRole
        JOIN FETCH d.deleteRole
        WHERE d.id = :id
    """)
    Optional<Document> findByIdWithRolesIgnoreIsDeleted(@Param("id") Long id);

    /**
     * 파일 해시(filePath)로 단일 문서 조회 + 권한 Role Fetch Join
     * - 파일 다운로드(해시) 시 사용
     */
    @Query("""
        SELECT d FROM Document d
        JOIN FETCH d.readRole
        JOIN FETCH d.writeRole
        JOIN FETCH d.deleteRole
        WHERE d.filePath = :filePath
    """)
    Optional<Document> findByFilePathWithRoles(@Param("filePath") String filePath);

}
