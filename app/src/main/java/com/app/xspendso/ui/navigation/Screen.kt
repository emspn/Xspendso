package com.app.xspendso.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Consent : Screen("consent")
    object Dashboard : Screen("dashboard")
}