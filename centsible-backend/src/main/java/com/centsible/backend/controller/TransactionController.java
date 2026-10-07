package com.centsible.backend.controller;

import com.centsible.backend.dto.ImportResultDTO;
import com.centsible.backend.dto.TransactionDTO;
import com.centsible.backend.dto.UpdateCategoryRequestDTO;
import com.centsible.backend.model.Category;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.model.User;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.TransactionRepository;
import com.centsible.backend.security.CustomUserDetails;
import com.centsible.backend.service.CategorizationService;
import com.centsible.backend.service.TransactionImportService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionImportService transactionImportService;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CategorizationService categorizationService;

    public TransactionController(TransactionImportService transactionImportService,
                                 TransactionRepository transactionRepository,
                                 CategoryRepository categoryRepository,
                                 CategorizationService categorizationService) {
        this.transactionImportService = transactionImportService;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.categorizationService = categorizationService;
    }

    // POST /api/transactions/upload (supports CSV and PDF statements scoped to current user)
    @PostMapping("/upload")
    public ImportResultDTO upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return transactionImportService.importStatement(file, userDetails.getUser());
    }

    // GET /api/transactions (returns all transactions for current user ordered by date descending)
    @GetMapping
    public List<TransactionDTO> listTransactions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return transactionRepository.findByUserId(userDetails.getUser().getId(), Sort.by(Sort.Direction.DESC, "date", "id")).stream()
                .map(t -> new TransactionDTO(
                        t.getId(),
                        t.getDate(),
                        t.getDescription(),
                        t.getAmount(),
                        t.getCategory() != null ? t.getCategory().getName() : "Uncategorized"
                ))
                .collect(Collectors.toList());
    }

    // PUT /api/transactions/{id}/category (updates category and optionally learns rule for current user)
    @PutMapping("/{id}/category")
    public ResponseEntity<TransactionDTO> updateCategory(
            @PathVariable Long id,
            @RequestBody UpdateCategoryRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser();
        return transactionRepository.findByIdAndUserId(id, user.getId()).map(t -> {
            Category category = categorizationService.getOrCreateCategory(request.getCategoryName(), user);
            t.setCategory(category);
            Transaction saved = transactionRepository.save(t);

            if (request.isRememberRule()) {
                String keyword = request.getKeyword();
                if (keyword == null || keyword.isBlank()) {
                    keyword = extractDefaultKeyword(t.getDescription());
                }
                categorizationService.learnRule(keyword, request.getCategoryName(), true, user);
            }

            return ResponseEntity.ok(new TransactionDTO(
                    saved.getId(),
                    saved.getDate(),
                    saved.getDescription(),
                    saved.getAmount(),
                    saved.getCategory() != null ? saved.getCategory().getName() : "Uncategorized"
                ));
        }).orElse(ResponseEntity.notFound().build());
    }

    private String extractDefaultKeyword(String description) {
        if (description == null || description.isBlank()) return "";
        String clean = description.replaceAll("^(PURCHASE|TRANSFER TO|PAYMENT TO)\\s+", "").trim();
        String[] words = clean.split("\\s+");
        if (words.length > 0 && !words[0].isBlank()) {
            return words[0];
        }
        return clean;
    }
}
