package com.centsible.backend.repository;

import com.centsible.backend.model.CategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {

    List<CategoryRule> findAllByOrderByPriorityDesc();

    Optional<CategoryRule> findByKeyword(String keyword);

    boolean existsByKeyword(String keyword);

}
