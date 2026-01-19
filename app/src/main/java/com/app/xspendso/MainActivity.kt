package com.app.xspendso

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.xspendso.auth.AuthManager
import com.app.xspendso.auth.BiometricAuthManager
import com.app.xspendso.data.PrefsManager
import com.app.xspendso.sms.SyncWorker
import com.app.xspendso.ui.auth.LoginScreen
import com.app.xspendso.ui.consent.ConsentScreen
import com.app.xspendso.ui.dashboard.DashboardHeader
import com.app.xspendso.ui.dashboard.DashboardScreen
import com.app.xspendso.ui.dashboard.DashboardViewModel
import com.app.xspendso.ui.insights.BudgetManagementScreen
import com.app.xspendso.ui.insights.InsightsScreen
import com.app.xspendso.ui.navigation.Screen
import com.app.xspendso.ui.people.PeopleScreen
import com.app.xspendso.ui.people.PeopleViewModel
import com.app.xspendso.ui.rules.RuleManagementScreen
import com.app.xspendso.ui.settings.SettingsScreen
import com.app.xspendso.ui.theme.XspendsoTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var authManager: AuthManager
    
    @Inject
    lateinit var prefsManager: PrefsManager
    
    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XspendsoTheme {
                XpendsoApp(
                    authManager = authManager,
                    prefsManager = prefsManager,
                    biometricAuthManager = biometricAuthManager,
                    isSmsAuthorized = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_SMS
                    ) == PackageManager.PERMISSION_GRANTED,
                    isContactsAuthorized = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XpendsoApp(
    authManager: AuthManager,
    prefsManager: PrefsManager,
    biometricAuthManager: BiometricAuthManager,
    isSmsAuthorized: Boolean,
    isContactsAuthorized: Boolean
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val peopleViewModel: PeopleViewModel = hiltViewModel()

    var isAuthenticated by remember { mutableStateOf(false) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.READ_SMS] == true
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        
        if (smsGranted) {
            SyncWorker.schedulePeriodicSync(context)
        }
        
        peopleViewModel.setContactPermissionGranted(contactsGranted)
        
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(Screen.Consent.route) { inclusive = true }
        }
    }

    LaunchedEffect(isSmsAuthorized) {
        if (isSmsAuthorized) {
            SyncWorker.schedulePeriodicSync(context)
        }
    }
    
    LaunchedEffect(isContactsAuthorized) {
        peopleViewModel.setContactPermissionGranted(isContactsAuthorized)
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
        } else if (!isSmsAuthorized) {
            Screen.Consent.route
        } else {
            Screen.Dashboard.route
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in listOf(Screen.Dashboard.route, Screen.Analytics.route, Screen.People.route)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            XpendsoTopBar(
                currentDestination = currentDestination,
                navController = navController
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
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_SMS,
                                Manifest.permission.READ_CONTACTS
                            )
                        )
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
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        peopleViewModel = peopleViewModel,
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
            }
            composable(Screen.Analytics.route) {
                InsightsScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToPlanner = { navController.navigate(Screen.BudgetPlanner.route) }
                )
            }
            composable(Screen.People.route) {
                PeopleScreen(viewModel = peopleViewModel)
            }
            composable(Screen.BudgetPlanner.route) {
                BudgetManagementScreen(
                    viewModel = dashboardViewModel,
                    onBack = { navController.popBackStack() }
                )
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
                            // Repository should be injected or handled through VM
                            // For now, keep it simple
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
    navController: androidx.navigation.NavController
) {
    when (currentDestination?.route) {
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
        // DashboardHeader moved to DashboardScreen for sticky search effect
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
            Triple(Screen.People, "People", Icons.Default.People),
            Triple(Screen.Dashboard, "Dashboard", Icons.Default.Home),
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
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
