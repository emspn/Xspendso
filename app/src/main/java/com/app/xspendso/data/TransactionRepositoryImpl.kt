package com.app.xspendso.data

import com.app.xspendso.domain.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val patternDao: CorrectionDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val ruleDao: CategorizationRuleDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    override suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)

    override suspend fun insertTransactions(transactions: List<TransactionEntity>) = transactionDao.insertTransactions(transactions)

    override suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.deleteTransaction(transaction)

    override suspend fun deleteAllUserData() {
        transactionDao.deleteAllTransactions()
        patternDao.deleteAllPatterns()
        budgetDao.deleteAllBudgets()
        goalDao.deleteAllGoals()
        ruleDao.deleteAllRules()
    }

    override fun getAllPatterns(): Flow<List<CorrectionPattern>> = patternDao.getAllPatterns()

    override suspend fun insertPattern(pattern: CorrectionPattern) = patternDao.insertPattern(pattern)

    override suspend fun findMatchingPattern(amount: Double, timeBucket: Long, source: String): CorrectionPattern? {
        return patternDao.findMatchingPattern(amount, timeBucket, source)
    }

    override fun getBudgetForMonth(monthYear: String): Flow<BudgetEntity?> = budgetDao.getBudgetForMonth(monthYear)

    override suspend fun insertBudget(budget: BudgetEntity) = budgetDao.insertBudget(budget)

    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    override fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    override suspend fun insertGoal(goal: GoalEntity) = goalDao.insertGoal(goal)

    override suspend fun updateGoal(goal: GoalEntity) = goalDao.updateGoal(goal)

    override suspend fun deleteGoal(goal: GoalEntity) = goalDao.deleteGoal(goal)

    override fun getAllRules(): Flow<List<CategorizationRule>> = ruleDao.getAllRules()

    override suspend fun insertRule(rule: CategorizationRule) = ruleDao.insertRule(rule)

    override suspend fun deleteRule(rule: CategorizationRule) = ruleDao.deleteRule(rule)
}