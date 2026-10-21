package com.centsible.backend.service;

import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.CategoryRuleRepository;
import com.centsible.backend.repository.TransactionRepository;
import com.centsible.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final TransactionRepository transactionRepository;
    private final CategoryRuleRepository categoryRuleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public AccountService(TransactionRepository transactionRepository,
                          CategoryRuleRepository categoryRuleRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRuleRepository = categoryRuleRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Permanently removes a user and everything that belongs to them. Runs in one
     * transaction, so a failure part-way leaves the data untouched rather than
     * half-deleted. Order matters: transactions and rules reference categories, and
     * everything references the user.
     */
    @Transactional
    public void deleteAccountAndAllData(Long userId) {
        transactionRepository.deleteAllByUserId(userId);
        categoryRuleRepository.deleteAllByUserId(userId);
        categoryRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }
}
