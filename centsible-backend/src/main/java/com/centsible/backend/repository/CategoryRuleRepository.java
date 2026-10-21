package com.centsible.backend.repository;

import com.centsible.backend.model.CategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {

    // Used by account deletion - removes every keyword rule belonging to one user
    @Modifying
    @Query("DELETE FROM CategoryRule r WHERE r.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    List<CategoryRule> findByUserIdOrderByPriorityDesc(Long userId);

    Optional<CategoryRule> findByUserIdAndKeyword(Long userId, String keyword);

    boolean existsByUserIdAndKeyword(Long userId, String keyword);

}
