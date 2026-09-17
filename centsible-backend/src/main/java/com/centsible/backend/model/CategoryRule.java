package com.centsible.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "category_rules")
public class CategoryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Keyword to match against raw merchant/description strings (case-insensitive)
    @Column(nullable = false, unique = true)
    private String keyword;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private int priority = 0;

    public CategoryRule() {
    }

    public CategoryRule(String keyword, Category category) {
        this.keyword = keyword != null ? keyword.trim().toUpperCase() : "";
        this.category = category;
        this.priority = this.keyword.length(); // default: longer keywords match first
    }

    public CategoryRule(String keyword, Category category, int priority) {
        this.keyword = keyword != null ? keyword.trim().toUpperCase() : "";
        this.category = category;
        this.priority = priority;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword != null ? keyword.trim().toUpperCase() : "";
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
