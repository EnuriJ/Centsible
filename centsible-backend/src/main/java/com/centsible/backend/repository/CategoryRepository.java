package com.centsible.backend.repository;

import com.centsible.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Used by account deletion - removes every category belonging to one user
    @Modifying
    @Query("DELETE FROM Category c WHERE c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    List<Category> findByUserIdOrderByNameAsc(Long userId);

    Optional<Category> findByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndName(Long userId, String name);

}
