package com.app.xspendso

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.xspendso.data.AppDatabase
import com.app.xspendso.data.TransactionRepositoryImpl
import com.app.xspendso.domain.usecase.GetMonthlyAnalyticsUseCase
import com.app.xspendso.domain.usecase.ImportSmsUseCase
import com.app.xspendso.sms.SmsReader
import com.app.xspendso.ui.auth.LoginScreen
import com.app.xspendso.ui.auth.SignupScreen
import com.app.xspendso.ui.consent.ConsentScreen
import com.app.xspendso.ui.dashboard.DashboardScreen
import com.app.xspendso.ui.dashboard.DashboardViewModel
import com.app.xspendso.ui.navigation.Screen
import com.app.xspendso.ui.theme.XspendsoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XspendsoTheme {
                XpendsoApp(
                    hasSmsPermission = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
        }
    }
}

@Composable
fun XpendsoApp(
    hasSmsPermission: Boolean
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Simple Manual DI for scaffolding
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { TransactionRepositoryImpl(database.transactionDao()) }
    val smsReader = remember { SmsReader(context) }
    val importSmsUseCase = remember { ImportSmsUseCase(smsReader, repository) }
    val getMonthlyAnalyticsUseCase = remember { GetMonthlyAnalyticsUseCase() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Consent.route) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Screen.Consent.route) },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
            )
        }
        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = { navController.navigate(Screen.Consent.route) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(Screen.Consent.route) {
            ConsentScreen(
                onConsentGiven = {
                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                }
            )
        }
        composable(Screen.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(repository, importSmsUseCase, getMonthlyAnalyticsUseCase)
            )
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToTransactions = { /* Already handled via tabs in this implementation */ },
                onNavigateToAnalytics = { /* Already handled via tabs in this implementation */ }
            )
        }
    }
}