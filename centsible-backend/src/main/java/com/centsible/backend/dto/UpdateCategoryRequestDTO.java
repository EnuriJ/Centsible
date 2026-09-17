package com.centsible.backend.dto;

public class UpdateCategoryRequestDTO {

    private String categoryName;
    private boolean rememberRule;
    private String keyword;

    public UpdateCategoryRequestDTO() {
    }

    public UpdateCategoryRequestDTO(String categoryName, boolean rememberRule, String keyword) {
        this.categoryName = categoryName;
        this.rememberRule = rememberRule;
        this.keyword = keyword;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public boolean isRememberRule() {
        return rememberRule;
    }

    public void setRememberRule(boolean rememberRule) {
        this.rememberRule = rememberRule;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
