package com.app.xspendso.di

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkManager
import com.app.xspendso.auth.AuthManager
import com.app.xspendso.auth.BiometricAuthManager
import com.app.xspendso.data.PrefsManager
import com.app.xspendso.domain.usecase.*
import com.app.xspendso.sms.SmsReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthManager(
        @ApplicationContext context: Context,
        prefsManager: PrefsManager
    ): AuthManager {
        return AuthManager(context, prefsManager)
    }

    @Provides
    @Singleton
    fun provideBiometricAuthManager(@ApplicationContext context: Context): BiometricAuthManager {
        return BiometricAuthManager(context)
    }

    @Provides
    @Singleton
    fun provideSmsReader(@ApplicationContext context: Context): SmsReader {
        return SmsReader(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    fun provideGetMonthlyAnalyticsUseCase(): GetMonthlyAnalyticsUseCase = GetMonthlyAnalyticsUseCase()

    @Provides
    fun provideGetBudgetingStatusUseCase(): GetBudgetingStatusUseCase = GetBudgetingStatusUseCase()

    @Provides
    fun providePredictMonthEndSavingsUseCase(): PredictMonthEndSavingsUseCase = PredictMonthEndSavingsUseCase()

    @Provides
    fun provideGetMerchantAnalyticsUseCase(): GetMerchantAnalyticsUseCase = GetMerchantAnalyticsUseCase()

    @Provides
    fun provideGetBalanceHistoryUseCase(): GetBalanceHistoryUseCase = GetBalanceHistoryUseCase()

    @Provides
    fun provideGetMonthOverMonthComparisonUseCase(): GetMonthOverMonthComparisonUseCase = GetMonthOverMonthComparisonUseCase()

    @Provides
    fun provideGetAccountBreakdownUseCase(): GetAccountBreakdownUseCase = GetAccountBreakdownUseCase()

    @Provides
    fun provideGetSpendingTrendsUseCase(): GetSpendingTrendsUseCase = GetSpendingTrendsUseCase()

    @Provides
    fun provideGetSpendingByDayOfWeekUseCase(): GetSpendingByDayOfWeekUseCase = GetSpendingByDayOfWeekUseCase()

    @Provides
    fun provideDetectRecurringTransactionsUseCase(): DetectRecurringTransactionsUseCase = DetectRecurringTransactionsUseCase()

    @Provides
    fun provideExportReportUseCase(@ApplicationContext context: Context): ExportReportUseCase = ExportReportUseCase(context)
}
