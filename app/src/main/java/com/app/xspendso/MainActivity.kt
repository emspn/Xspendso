package com.app.xspendso

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.xspendso.auth.AuthManager
import com.app.xspendso.auth.BiometricAuthManager
import com.app.xspendso.data.AppDatabase
import com.app.xspendso.data.PrefsManager
import com.app.xspendso.data.TransactionRepositoryImpl
import com.app.xspendso.domain.usecase.*
import com.app.xspendso.sms.SmsReader
import com.app.xspendso.sms.SyncWorker
import com.app.xspendso.ui.auth.LoginScreen
import com.app.xspendso.ui.consent.ConsentScreen
import com.app.xspendso.ui.dashboard.DashboardHeader
import com.app.xspendso.ui.dashboard.DashboardScreen
import com.app.xspendso.ui.dashboard.DashboardViewModel
import com.app.xspendso.ui.insights.InsightsScreen
import com.app.xspendso.ui.navigation.Screen
import com.app.xspendso.ui.rules.RuleManagementScreen
import com.app.xspendso.ui.settings.SettingsScreen
import com.app.xspendso.ui.theme.XspendsoTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XspendsoTheme {
                XpendsoApp(
                    isInitiallyAuthorized = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XpendsoApp(
    isInitiallyAuthorized: Boolean
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { 
        TransactionRepositoryImpl(
            database.transactionDao(), 
            database.correctionDao(),
            database.budgetDao(),
            database.goalDao(),
            database.ruleDao()
        ) 
    }
    val prefsManager = remember { PrefsManager(context) }
    val smsReader = remember { SmsReader(context) }
    val detectRecurringTransactionsUseCase = remember { DetectRecurringTransactionsUseCase() }
    val syncLedgerUseCase = remember { 
        SyncLedgerUseCase(smsReader, repository, detectRecurringTransactionsUseCase, prefsManager) 
    }
    val getMonthlyAnalyticsUseCase = remember { GetMonthlyAnalyticsUseCase() }
    val getBudgetingStatusUseCase = remember { GetBudgetingStatusUseCase() }
    val predictMonthEndSavingsUseCase = remember { PredictMonthEndSavingsUseCase() }
    val exportReportUseCase = remember { ExportReportUseCase(context) }
    val getMerchantAnalyticsUseCase = remember { GetMerchantAnalyticsUseCase() }
    val getBalanceHistoryUseCase = remember { GetBalanceHistoryUseCase() }
    val getMonthOverMonthComparisonUseCase = remember { GetMonthOverMonthComparisonUseCase() }
    val getAccountBreakdownUseCase = remember { GetAccountBreakdownUseCase() }
    val authManager = remember { AuthManager(context) }
    val biometricAuthManager = remember { BiometricAuthManager(context) }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            repository, 
            syncLedgerUseCase, 
            getMonthlyAnalyticsUseCase,
            getBudgetingStatusUseCase,
            predictMonthEndSavingsUseCase,
            exportReportUseCase,
            getMerchantAnalyticsUseCase,
            getBalanceHistoryUseCase,
            getMonthOverMonthComparisonUseCase,
            getAccountBreakdownUseCase
        )
    )

    var isAuthenticated by remember { mutableStateOf(false) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            SyncWorker.schedulePeriodicSync(context)
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Consent.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(isInitiallyAuthorized) {
        if (isInitiallyAuthorized) {
            SyncWorker.schedulePeriodicSync(context)
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                scope.launch {
                    val success = authManager.signInWithGoogle(idToken)
                    if (success) {
                        navController.navigate(Screen.Consent.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val startDestination = remember {
        if (!authManager.isUserLoggedIn()) {
            Screen.Login.route
        } else if (!isInitiallyAuthorized) {
            Screen.Consent.route
        } else {
            Screen.Dashboard.route
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in listOf(Screen.Dashboard.route, Screen.Analytics.route)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            XpendsoTopBar(
                currentDestination = currentDestination,
                isAuthenticated = isAuthenticated,
                viewModel = dashboardViewModel,
                navController = navController,
                currencyFormatter = currencyFormatter
            )
        },
        bottomBar = {
            if (showBottomBar) {
                XpendsoBottomNavigation(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        },
        floatingActionButton = {
            if (currentDestination?.route == Screen.Dashboard.route && isAuthenticated) {
                FloatingActionButton(
                    onClick = { dashboardViewModel.setShowManualPaymentSheet(true) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Payment", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController, 
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        val signInIntent = authManager.getGoogleSignInClient().signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    }
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
                if (!isAuthenticated && biometricAuthManager.isBiometricAvailable() && prefsManager.isBiometricEnabled) {
                    LaunchedEffect(Unit) {
                        biometricAuthManager.authenticate(
                            activity = activity,
                            onSuccess = { isAuthenticated = true },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    isAuthenticated = true
                }

                if (isAuthenticated) {
                    DashboardScreen(viewModel = dashboardViewModel)
                }
            }
            composable(Screen.Analytics.route) {
                InsightsScreen(viewModel = dashboardViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    prefsManager = prefsManager,
                    onSignOut = {
                        authManager.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onDeleteData = {
                        scope.launch {
                            repository.deleteAllUserData()
                            Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onNavigateToRules = {
                        navController.navigate(Screen.CategorizationRules.route)
                    },
                    onForceReparse = {
                        dashboardViewModel.forceReparseAllData(prefsManager)
                        navController.popBackStack()
                        Toast.makeText(context, "Ledger repair started...", Toast.LENGTH_LONG).show()
                    }
                )
            }
            composable(Screen.CategorizationRules.route) {
                RuleManagementScreen(viewModel = dashboardViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XpendsoTopBar(
    currentDestination: NavDestination?,
    isAuthenticated: Boolean,
    viewModel: DashboardViewModel,
    navController: androidx.navigation.NavController,
    currencyFormatter: NumberFormat
) {
    when (currentDestination?.route) {
        Screen.Dashboard.route -> {
            if (isAuthenticated) {
                val searchQuery by viewModel.searchQuery.collectAsState()
                val totalSpent by viewModel.totalSpent.collectAsState()
                val isSyncing by viewModel.isSyncing.collectAsState()
                val timeFilter by viewModel.timeFilter.collectAsState()
                
                DashboardHeader(
                    searchQuery = searchQuery,
                    totalSpent = totalSpent,
                    isSyncing = isSyncing,
                    timeFilter = timeFilter,
                    currencyFormatter = currencyFormatter,
                    onSearchChange = { viewModel.onSearchQueryChange(it) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }
        }
        Screen.Analytics.route -> {
            CenterAlignedTopAppBar(
                title = { Text("Insights", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
        Screen.Settings.route -> {
            TopAppBar(
                title = { Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
        Screen.CategorizationRules.route -> {
            TopAppBar(
                title = { Text("Rules", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    }
}

@Composable
fun XpendsoBottomNavigation(
    navController: androidx.navigation.NavController,
    currentDestination: NavDestination?
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(Screen.Dashboard, "Home", Icons.Default.Home),
            Triple(Screen.Analytics, "Insights", Icons.Default.Insights)
        )
        items.forEach { (screen, label, icon) ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
