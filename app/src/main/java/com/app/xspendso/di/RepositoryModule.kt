package com.app.xspendso.di

import com.app.xspendso.data.PeopleLedgerRepositoryImpl
import com.app.xspendso.data.TransactionRepositoryImpl
import com.app.xspendso.domain.PeopleLedgerRepository
import com.app.xspendso.domain.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindPeopleLedgerRepository(
        peopleLedgerRepositoryImpl: PeopleLedgerRepositoryImpl
    ): PeopleLedgerRepository
}
