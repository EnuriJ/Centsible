package com.centsible.backend.dto;

import java.util.List;

public class ImportResultDTO {

    private int imported;
    private int skipped;
    private List<String> errors;

    public ImportResultDTO(int imported, int skipped, List<String> errors) {
        this.imported = imported;
        this.skipped = skipped;
        this.errors = errors;
    }

    public int getImported() {
        return imported;
    }

    public void setImported(int imported) {
        this.imported = imported;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
