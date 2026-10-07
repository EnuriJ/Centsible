package com.centsible.backend.controller;

import com.centsible.backend.model.Category;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<String> listCategoryNames(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return categoryRepository.findByUserIdOrderByNameAsc(userDetails.getUser().getId()).stream()
                .map(Category::getName)
                .collect(Collectors.toList());
    }
}
