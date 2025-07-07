package com.rookies.log2doc.repository;

import com.rookies.log2doc.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryTypeRepository extends JpaRepository<CategoryType, Long> {

    boolean existsByName(String name);
}
