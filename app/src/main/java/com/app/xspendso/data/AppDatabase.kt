package com.app.xspendso.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class, 
        CorrectionPattern::class, 
        BudgetEntity::class, 
        GoalEntity::class,
        CategorizationRule::class,
        ContactLedger::class,
        LoanTransaction::class
    ], 
    version = 10, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun correctionDao(): CorrectionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun ruleDao(): CategorizationRuleDao
    abstract fun peopleDao(): PeopleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migrate ContactLedger
                db.execSQL("ALTER TABLE contacts_ledger ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                
                // Migrate LoanTransaction
                db.execSQL("ALTER TABLE loan_transactions ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loan_transactions ADD COLUMN contactUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loan_transactions ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xspendso_database"
                )
                .addMigrations(MIGRATION_9_10)
                .fallbackToDestructiveMigration() // Use with caution in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
