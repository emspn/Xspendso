package com.app.xspendso.domain

import com.app.xspendso.data.*
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteAllUserData()
    
    // Correction Patterns
    fun getAllPatterns(): Flow<List<CorrectionPattern>>
    suspend fun insertPattern(pattern: CorrectionPattern)
    suspend fun findMatchingPattern(amount: Double, timeBucket: Long, source: String): CorrectionPattern?

    // Budgets
    fun getBudgetForMonth(monthYear: String): Flow<BudgetEntity?>
    suspend fun insertBudget(budget: BudgetEntity)
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    // Goals
    fun getAllGoals(): Flow<List<GoalEntity>>
    suspend fun insertGoal(goal: GoalEntity)
    suspend fun updateGoal(goal: GoalEntity)
    suspend fun deleteGoal(goal: GoalEntity)

    // Categorization Rules
    fun getAllRules(): Flow<List<CategorizationRule>>
    suspend fun insertRule(rule: CategorizationRule)
    suspend fun deleteRule(rule: CategorizationRule)
}
