package com.app.xspendso.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Consent : Screen("consent")
    data object Dashboard : Screen("dashboard")
    data object Settings : Screen("settings")
    data object Analytics : Screen("analytics")
    data object CategorizationRules : Screen("categorization_rules")
    data object BudgetPlanner : Screen("budget_planner")
    data object People : Screen("people")
}
