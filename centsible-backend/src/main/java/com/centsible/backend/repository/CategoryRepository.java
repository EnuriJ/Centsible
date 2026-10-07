package com.centsible.backend.repository;

import com.centsible.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrderByNameAsc(Long userId);

    Optional<Category> findByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndName(Long userId, String name);

}
