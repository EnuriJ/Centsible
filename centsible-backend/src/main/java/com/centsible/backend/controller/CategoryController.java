package com.centsible.backend.controller;

import com.centsible.backend.model.Category;
import com.centsible.backend.repository.CategoryRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // GET http://localhost:8080/api/categories
    @GetMapping
    public List<String> listCategoryNames() {
        return categoryRepository.findAll().stream()
                .map(Category::getName)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }
}
